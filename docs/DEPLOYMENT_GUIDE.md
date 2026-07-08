# Complete Deployment Guide

## Prerequisites

### Required Tools
- AWS CLI (configured with credentials)
- kubectl
- Terraform >= 1.0
- eksctl
- Helm 3
- Docker (for local development)

### AWS Account Requirements
- Administrator or equivalent IAM permissions
- Service quotas for:
  - EKS clusters: 1
  - EC2 instances: 3+ t3.xlarge
  - EBS volumes: 50GB+
  - Elastic IPs: 2+

### Dynatrace Requirements
- Dynatrace tenant (trial or licensed)
- API Token with permissions:
  - `PaaS integration - Installer download`
  - `Write configuration`
  - `Read configuration`
  - `Access problem and event feed, metrics, and topology`
- Data Ingest Token with permissions:
  - `Ingest metrics`
  - `Ingest logs`
  - `Ingest events`

---

## Step 1: Clone and Configure

```bash
# Clone repository
git clone <repository-url>
cd enterprise-observability-demo-up

# Copy example environment file
cp .env.example .env

# Edit .env with your values
# - JWT_SECRET: Generate with: openssl rand -base64 32
# - POSTGRES_PASSWORD: Set a strong password
```

---

## Step 2: Build and Push Container Images

```bash
# Set your AWS account ID and region
export AWS_ACCOUNT_ID=<your-account-id>
export AWS_REGION=us-east-1

# Authenticate to ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Create ECR repositories
SERVICES="gateway authentication-service employee-service leave-service payroll-service asset-service helpdesk-service notification-service frontend"

for service in $SERVICES; do
  aws ecr create-repository \
    --repository-name enterprise-observability/$service \
    --region $AWS_REGION || echo "Repository $service already exists"
done

# Build and push images
# Option 1: Build all services at once
docker compose build

# Option 2: Build services individually
cd services/authentication-service
mvn clean package -DskipTests
docker build -t $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/enterprise-observability/auth-service:v1.0.0 .
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/enterprise-observability/auth-service:v1.0.0

# Repeat for all services...

# Build frontend
cd ../../frontend
docker build -t $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/enterprise-observability/frontend:v1.0.0 .
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/enterprise-observability/frontend:v1.0.0

# Build gateway
cd ../gateway
mvn clean package -DskipTests
docker build -t $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/enterprise-observability/gateway:v1.0.0 .
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/enterprise-observability/gateway:v1.0.0
```

---

## Step 3: Deploy Infrastructure with Terraform

```bash
cd terraform

# Initialize Terraform
terraform init

# Create terraform.tfvars
cat > terraform.tfvars <<EOF
aws_region         = "us-east-1"
cluster_name       = "enterprise-eks-cluster"
node_instance_type = "t3.xlarge"
desired_capacity   = 3
min_capacity       = 3
max_capacity       = 4
disk_size          = 50
EOF

# Plan infrastructure
terraform plan

# Apply (creates VPC, EKS cluster, node group)
terraform apply

# This takes 15-20 minutes
# Output will include:
# - cluster_endpoint
# - cluster_name
# - node_group_name

# Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name enterprise-eks-cluster

# Verify cluster access
kubectl get nodes
# Should show 3 nodes in Ready state
```

---

## Step 4: Install Cluster Add-ons

### 4.1 AWS Load Balancer Controller

```bash
# Download IAM policy
curl -o iam-policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/main/docs/install/iam_policy.json

# Create IAM policy
aws iam create-policy \
  --policy-name AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam-policy.json

# Create service account (replace <ACCOUNT_ID>)
eksctl create iamserviceaccount \
  --cluster=enterprise-eks-cluster \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --attach-policy-arn=arn:aws:iam::<ACCOUNT_ID>:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve \
  --region=us-east-1

# Add EKS chart repo
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=enterprise-eks-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller

# Verify installation
kubectl get deployment -n kube-system aws-load-balancer-controller
```

### 4.2 NGINX Ingress Controller

```bash
# Install NGINX Ingress
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/aws/deploy.yaml

# Verify installation
kubectl get pods -n ingress-nginx

# Wait for LoadBalancer to be provisioned (2-3 minutes)
kubectl get svc -n ingress-nginx
```

### 4.3 Metrics Server

```bash
# Install Metrics Server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Verify installation
kubectl get deployment metrics-server -n kube-system

# Test metrics
kubectl top nodes
```

---

## Step 5: Deploy Dynatrace

### 5.1 Get Dynatrace Tokens

1. Log into your Dynatrace tenant
2. Go to **Settings → Integration → Dynatrace API**
3. Generate **API Token** with scopes:
   - PaaS integration - Installer download
   - Write configuration
   - Read configuration
   - Access problem and event feed, metrics, and topology
4. Generate **Data Ingest Token** with scopes:
   - Ingest metrics
   - Ingest logs
   - Ingest events

### 5.2 Install Dynatrace Operator

```bash
# Create Dynatrace namespace
kubectl create namespace dynatrace
kubectl apply -f k8s/dynatrace/namespace.yaml

# Create secret with your tokens
kubectl create secret generic enterprise-eks-cluster \
  --from-literal=apiToken=<YOUR_API_TOKEN> \
  --from-literal=dataIngestToken=<YOUR_DATA_INGEST_TOKEN> \
  -n dynatrace

# Install Dynatrace Operator
kubectl apply -f https://github.com/Dynatrace/dynatrace-operator/releases/latest/download/kubernetes.yaml

# Wait for operator to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=dynatrace-operator -n dynatrace --timeout=300s
```

### 5.3 Deploy DynaKube

```bash
# Edit k8s/dynatrace/dynakube.yaml
# Replace apiUrl with your tenant URL:
# apiUrl: https://YOUR_TENANT.live.dynatrace.com/api

# Apply DynaKube
kubectl apply -f k8s/dynatrace/dynakube.yaml

# Monitor deployment (takes 3-5 minutes)
kubectl get pods -n dynatrace -w

# Expected final state:
# dynatrace-operator-xxx                    1/1     Running
# dynatrace-webhook-xxx                     1/1     Running
# enterprise-eks-cluster-activegate-0       1/1     Running
# dynatrace-oneagent-xxx (3 pods)           1/1     Running
# dynatrace-oneagent-csi-driver-xxx (3)     1/1     Running

# Check DynaKube status
kubectl get dynakube -n dynatrace
```

---

## Step 6: Deploy Application

### 6.1 Create Application Namespace

```bash
kubectl create namespace enterprise-observability
```

### 6.2 Update Image References

```bash
# Update all deployment YAMLs with your ECR URLs
# Replace <ACCOUNT_ID> in all files under k8s/

# Automated replacement:
export AWS_ACCOUNT_ID=<your-account-id>

find k8s -name "deployment.yaml" -type f -exec sed -i \
  "s/039612843833/$AWS_ACCOUNT_ID/g" {} \;
```

### 6.3 Deploy ConfigMap

```bash
kubectl apply -f k8s/configmap.yaml
```

### 6.4 Deploy PostgreSQL

```bash
# Create PVC
kubectl apply -f k8s/postgres/pvc.yaml

# Deploy PostgreSQL
kubectl apply -f k8s/postgres/deployment.yaml
kubectl apply -f k8s/postgres/service.yaml

# Wait for PostgreSQL to be ready
kubectl wait --for=condition=ready pod -l app=postgres \
  -n enterprise-observability --timeout=300s

# Verify databases are created
kubectl exec -it $(kubectl get pod -l app=postgres -n enterprise-observability -o jsonpath='{.items[0].metadata.name}') \
  -n enterprise-observability -- psql -U enterprise -c "\l"

# Should show: auth_db, employee_db, leave_db, payroll_db, asset_db, helpdesk_db, notification_db
```

### 6.5 Deploy Redis

```bash
kubectl apply -f k8s/redis/deployment.yaml
kubectl apply -f k8s/redis/service.yaml

# Wait for Redis to be ready
kubectl wait --for=condition=ready pod -l app=redis \
  -n enterprise-observability --timeout=180s
```

### 6.6 Deploy Microservices

```bash
# Deploy all services
kubectl apply -f k8s/authentication-service/
kubectl apply -f k8s/employee-service/
kubectl apply -f k8s/leave-service/
kubectl apply -f k8s/payroll-service/
kubectl apply -f k8s/asset-service/
kubectl apply -f k8s/helpdesk-service/
kubectl apply -f k8s/notification-service/

# Wait for services to be ready (takes 3-5 minutes due to Dynatrace injection)
kubectl get pods -n enterprise-observability -w

# All pods should eventually show 1/1 READY
```

### 6.7 Deploy Gateway and Frontend

```bash
kubectl apply -f k8s/gateway/
kubectl apply -f k8s/frontend/

# Wait for deployment
kubectl wait --for=condition=ready pod -l app=gateway \
  -n enterprise-observability --timeout=300s
kubectl wait --for=condition=ready pod -l app=frontend \
  -n enterprise-observability --timeout=180s
```

### 6.8 Deploy Ingress

```bash
kubectl apply -f k8s/ingress/ingress.yaml

# Get LoadBalancer URL (wait 2-3 minutes for provisioning)
kubectl get ingress -n enterprise-observability

# Test access
LB_URL=$(kubectl get ingress ingress-nginx -n enterprise-observability \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

echo "Application URL: http://$LB_URL"

# Test in browser
curl -I http://$LB_URL
```

---

## Step 7: Configure Dynatrace Alerting

### 7.1 Enable Problem Detection

1. Login to Dynatrace tenant
2. Go to **Settings → Anomaly detection → Services**
3. Enable **"Detect increases in failure rate"**
4. Set sensitivity: **High**
5. Set threshold: **> 10% failure rate**
6. Click **Save**

### 7.2 Enable Kubernetes Monitoring

1. Go to **Settings → Anomaly detection → Kubernetes**
2. Enable:
   - ✅ Detect pod availability issues
   - ✅ Detect deployment stuck
   - ✅ Detect container restart loops
3. Click **Save**

### 7.3 Create Slack Workflow

1. **Create Slack App:**
   - Go to https://api.slack.com/apps
   - Click **Create New App** → From scratch
   - Name: "Dynatrace Alerts"
   - Choose your workspace
   - Go to **OAuth & Permissions**
   - Add Bot Token Scopes:
     - `chat:write`
     - `chat:write.public`
   - Click **Install to Workspace**
   - Copy **Bot User OAuth Token**

2. **Add Slack to Dynatrace:**
   - Go to **Settings → Integration → Slack**
   - Click **Add connection**
   - Paste Bot Token
   - Click **Save**

3. **Create Workflow:**
   - Go to **Workflows → Create Workflow**
   - Name: "Slack Alert on Problem"
   - Trigger: **Problem**
   - Filter: Category = **Error**
   - Add Action: **Send Slack message**
   - Select Channel
   - Message template:
   ```
   🚨 *Dynatrace Problem Alert*
   ━━━━━━━━━━━━━━━━━━━━━━
   *Problem*: {{ .title }}
   *Severity*: {{ .severity }}
   *Status*: {{ .status }}
   *Affected*: {{ .affectedEntities[0].name }}
   *Cluster*: enterprise-eks-cluster
   *Namespace*: enterprise-observability
   
   *Description*: {{ .description }}
   *Display ID*: {{ .displayId }}
   *Started*: {{ .startTime }}
   ━━━━━━━━━━━━━━━━━━━━━━
   Generated automatically by Dynatrace Workflow
   ```
   - Click **Deploy**

---

## Step 8: Verify Complete Deployment

```bash
# Check all application pods
kubectl get pods -n enterprise-observability

# Expected output (all 1/1 READY):
# asset-service-xxx                         1/1     Running
# authentication-service-xxx                1/1     Running
# employee-service-xxx                      1/1     Running
# frontend-xxx                              1/1     Running
# gateway-xxx                               1/1     Running
# helpdesk-service-xxx                      1/1     Running
# leave-service-xxx                         1/1     Running
# notification-service-xxx                  1/1     Running
# payroll-service-xxx                       1/1     Running
# postgres-xxx                              1/1     Running
# redis-xxx                                 1/1     Running

# Check Dynatrace pods
kubectl get pods -n dynatrace

# Check services
kubectl get svc -n enterprise-observability

# Check ingress
kubectl get ingress -n enterprise-observability

# Get application URL
echo "Application: http://$(kubectl get ingress ingress-nginx -n enterprise-observability -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"

# Test application
curl http://$LB_URL/api/health
```

---

## Step 9: Test Dynatrace Monitoring

### 9.1 Generate Traffic

```bash
# Login and generate traffic
LB_URL="http://$(kubectl get ingress ingress-nginx -n enterprise-observability -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"

# Get auth token
TOKEN=$(curl -X POST "$LB_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Password@123"}' \
  | jq -r '.token')

# Generate requests
for i in {1..50}; do
  curl -H "Authorization: Bearer $TOKEN" "$LB_URL/api/employees"
  curl -H "Authorization: Bearer $TOKEN" "$LB_URL/api/leaves"
  curl -H "Authorization: Bearer $TOKEN" "$LB_URL/api/assets"
  sleep 0.5
done
```

### 9.2 Verify in Dynatrace

1. Go to **Services**
2. Find your services (gateway, employee-service, etc.)
3. Check distributed traces
4. Verify metrics are flowing

### 9.3 Test Alert Workflow

```bash
# Create a crash (not scale-down!)
kubectl set image deployment/gateway \
  gateway=nginx:invalid-image-404 \
  -n enterprise-observability

# Generate failed requests
for i in {1..100}; do
  curl "$LB_URL/api/employees"
  sleep 0.1
done

# Wait 2-5 minutes, then check:
# 1. Dynatrace → Problems (should show new problem)
# 2. Slack channel (should receive alert)

# Fix the service
kubectl set image deployment/gateway \
  gateway=$AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/enterprise-observability/gateway:v1.0.0 \
  -n enterprise-observability

# Problem should auto-resolve
```

---

## Step 10: Cleanup (Optional)

```bash
# Delete application
kubectl delete namespace enterprise-observability

# Delete Dynatrace
kubectl delete namespace dynatrace

# Delete ingress controller
kubectl delete namespace ingress-nginx

# Destroy infrastructure
cd terraform
terraform destroy

# This will:
# - Delete EKS cluster
# - Delete node group
# - Delete VPC and subnets
# - Delete security groups
# - Delete IAM roles

# Delete ECR repositories
SERVICES="gateway authentication-service employee-service leave-service payroll-service asset-service helpdesk-service notification-service frontend"

for service in $SERVICES; do
  aws ecr delete-repository \
    --repository-name enterprise-observability/$service \
    --region us-east-1 \
    --force
done
```

---

## Estimated Costs

**AWS Infrastructure (us-east-1):**
- EKS Cluster: $0.10/hour (~$73/month)
- 3 × t3.xlarge nodes: $0.1664/hour each (~$360/month total)
- Network Load Balancer: $0.0225/hour (~$16/month)
- EBS volumes: $0.10/GB-month (~$5/month for 50GB)
- Data transfer: Variable

**Total estimated cost: ~$454/month** (running 24/7)

**Cost optimization:**
- Run during business hours only: ~$150/month
- Use t3.large instead of t3.xlarge: ~$250/month
- Use Spot instances: ~50% savings

**Dynatrace:**
- Trial: Free for 15 days
- Full-stack monitoring: Pricing based on host units and usage

---

## Next Steps

1. **Configure CI/CD**: Set up GitHub Actions or GitLab CI
2. **Add FluxCD**: Implement GitOps for deployments
3. **Configure Backups**: Set up Velero for cluster backups
4. **Add Monitoring Dashboards**: Create custom Dynatrace dashboards
5. **Implement Auto-scaling**: Configure HPA for services
6. **Add TLS**: Configure cert-manager and Let's Encrypt
7. **Multi-environment**: Create dev, staging, prod environments

---

## Support

For issues or questions:
1. Check the [Troubleshooting Guide](../README.md#troubleshooting)
2. Review Kubernetes events: `kubectl get events -n enterprise-observability`
3. Check pod logs: `kubectl logs <pod-name> -n enterprise-observability`
4. Review Dynatrace documentation
