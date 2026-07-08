# 🚀 Quick Start Guide - Run the Complete Project

This guide will help you recreate the entire Enterprise Observability Platform from scratch.

---

## ⚡ Prerequisites (One-Time Setup)

### 1. Install Required Tools

```bash
# AWS CLI
# Download from: https://aws.amazon.com/cli/

# kubectl
# Download from: https://kubernetes.io/docs/tasks/tools/

# Terraform
# Download from: https://www.terraform.io/downloads

# Flux CLI (Windows)
choco install flux
# Or download from: https://fluxcd.io/flux/installation/

# Docker
# Download from: https://www.docker.com/products/docker-desktop
```

### 2. Configure AWS Credentials

```bash
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Default region: us-east-1
# Default output format: json
```

### 3. Clone the Repository

```bash
git clone https://github.com/abhaysahu403/enterprise-observability-gitops.git
cd enterprise-observability-gitops
```

---

## 🏗️ Step-by-Step Deployment (30-45 minutes)

### Step 1: Create Terraform Variables (5 min)

Create `terraform/terraform.tfvars`:

```hcl
aws_region          = "us-east-1"
cluster_name        = "enterprise-eks-cluster"
cluster_version     = "1.31"
vpc_cidr            = "10.0.0.0/16"
private_subnet_cidrs = ["10.0.1.0/24", "10.0.2.0/24"]
public_subnet_cidrs  = ["10.0.3.0/24", "10.0.4.0/24"]
node_desired_size   = 3
node_min_size       = 2
node_max_size       = 5
node_instance_types = ["t3.large"]
```

### Step 2: Deploy AWS Infrastructure with Terraform (15-20 min)

```bash
cd terraform

# Initialize Terraform
terraform init

# Review the plan
terraform plan -out=tfplan

# Apply the infrastructure
terraform apply tfplan

# Save the outputs
terraform output > ../outputs.txt

# Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name enterprise-eks-cluster
```

**Verify:**
```bash
kubectl get nodes
# You should see 3 nodes in Ready state
```

### Step 3: Build and Push Docker Images (10 min)

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Get your AWS account ID
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Create ECR repositories
aws ecr create-repository --repository-name enterprise-observability/gateway --region us-east-1
aws ecr create-repository --repository-name enterprise-observability/authentication-service --region us-east-1
aws ecr create-repository --repository-name enterprise-observability/employee-service --region us-east-1
aws ecr create-repository --repository-name enterprise-observability/leave-service --region us-east-1
aws ecr create-repository --repository-name enterprise-observability/payroll-service --region us-east-1
aws ecr create-repository --repository-name enterprise-observability/asset-service --region us-east-1
aws ecr create-repository --repository-name enterprise-observability/notification-service --region us-east-1
aws ecr create-repository --repository-name enterprise-observability/helpdesk-service --region us-east-1
aws ecr create-repository --repository-name enterprise-observability/frontend --region us-east-1

# Build and push images (run from project root)
cd gateway
mvn clean package -DskipTests
docker build -t ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/enterprise-observability/gateway:v1.0.0 .
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/enterprise-observability/gateway:v1.0.0
cd ..

# Repeat for all services:
# - authentication-service
# - employee-service
# - leave-service
# - payroll-service
# - asset-service
# - notification-service
# - helpdesk-service

# Build frontend
cd frontend
npm install
docker build -t ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/enterprise-observability/frontend:v1.0.0 .
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/enterprise-observability/frontend:v1.0.0
cd ..
```

### Step 4: Install Dynatrace (5 min)

```bash
# Create Dynatrace namespace
kubectl create namespace dynatrace

# Install Dynatrace Operator
kubectl apply -f https://github.com/Dynatrace/dynatrace-operator/releases/latest/download/kubernetes.yaml

# Create Dynakube secret (replace with your values)
kubectl create secret generic dynakube --from-literal="apiToken=<YOUR_API_TOKEN>" --from-literal="dataIngestToken=<YOUR_DATA_INGEST_TOKEN>" -n dynatrace

# Deploy Dynakube
kubectl apply -f k8s/dynatrace/dynakube.yaml
```

**Wait for Dynatrace to be ready:**
```bash
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=dynatrace-operator -n dynatrace --timeout=5m
```

### Step 5: Create Kubernetes Secrets (2 min)

Update `k8s/secrets.yaml` with your values:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: enterprise-secrets
  namespace: enterprise-observability
type: Opaque
stringData:
  JWT_SECRET: "your-jwt-secret-minimum-32-characters-long"
  DB_PASSWORD: "your-database-password"
  DYNATRACE_API_TOKEN: "your-dynatrace-api-token"
  DYNATRACE_TENANT_URL: "https://your-tenant.live.dynatrace.com"
```

```bash
kubectl apply -f k8s/secrets.yaml
```

### Step 6: Bootstrap FluxCD (3 min)

```bash
# Set GitHub token
export GITHUB_TOKEN="your-github-personal-access-token"

# Bootstrap Flux
flux bootstrap github \
  --owner=abhaysahu403 \
  --repository=enterprise-observability-gitops \
  --branch=main \
  --path=./k8s \
  --personal

# Wait for Flux to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/part-of=flux -n flux-system --timeout=5m
```

**Verify Flux:**
```bash
flux check
flux get all
```

### Step 7: Deploy Applications via GitOps (5 min)

FluxCD will automatically deploy all applications from the Git repository.

**Watch the deployment:**
```bash
kubectl get pods -n enterprise-observability -w
```

**Wait for all pods to be Running:**
```bash
kubectl wait --for=condition=ready pod --all -n enterprise-observability --timeout=10m
```

### Step 8: Access the Application

```bash
# Get the Load Balancer URL
kubectl get svc frontend -n enterprise-observability

# Access the application
# Open browser: http://<EXTERNAL-IP>
```

---

## ✅ Verification Checklist

```bash
# 1. Check EKS nodes
kubectl get nodes
# Expected: 3 nodes in Ready state

# 2. Check Flux status
flux get all
# Expected: GitRepository and Kustomization both Ready=True

# 3. Check all deployments
kubectl get deployments -n enterprise-observability
# Expected: All deployments with READY 1/1 or more

# 4. Check Dynatrace
kubectl get pods -n dynatrace
# Expected: dynakube-activegate and oneagent pods running

# 5. Check services
kubectl get svc -n enterprise-observability
# Expected: All services with ClusterIP or LoadBalancer

# 6. Access Dynatrace Dashboard
# URL: https://your-tenant.live.dynatrace.com
# Navigate to: Kubernetes → Clusters → enterprise-eks-cluster

# 7. Test the application
curl http://<FRONTEND-URL>/health
# Expected: HTTP 200 OK
```

---

## 🧪 Test the Platform

### 1. Generate Traffic

```bash
# Get gateway URL
GATEWAY_URL=$(kubectl get svc gateway -n enterprise-observability -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

# Generate continuous traffic
while true; do
  curl -s http://$GATEWAY_URL/api/employees > /dev/null
  curl -s http://$GATEWAY_URL/api/health > /dev/null
  sleep 1
done
```

### 2. Test GitOps Workflow

```bash
# Edit a deployment
vim k8s/gateway/deployment.yaml
# Change: replicas: 1 → replicas: 3

# Commit and push
git add k8s/gateway/deployment.yaml
git commit -m "Scale gateway to 3 replicas"
git push origin main

# Watch Flux apply the change
flux reconcile kustomization flux-system --with-source
kubectl get pods -n enterprise-observability -w
```

### 3. Test Self-Healing

```bash
# Delete a pod
kubectl delete pod -l app=gateway -n enterprise-observability

# Watch Kubernetes recreate it
kubectl get pods -n enterprise-observability -w
```

### 4. Test Davis AI Problem Detection

```bash
# Scale authentication-service to 0 (simulate failure)
kubectl scale deployment authentication-service --replicas=0 -n enterprise-observability

# Generate traffic to trigger errors
for i in {1..100}; do
  curl -s http://$GATEWAY_URL/api/employees > /dev/null
done

# Check Dynatrace for Problem detection
# Davis AI should detect the issue and create a Problem

# Restore the service
kubectl scale deployment authentication-service --replicas=1 -n enterprise-observability
```

---

## 📊 Access Monitoring Dashboards

### Dynatrace
- URL: `https://your-tenant.live.dynatrace.com`
- Navigate to: **Kubernetes → Workloads**
- View: Traces, Metrics, Logs, Problems

### Kubernetes Dashboard
```bash
kubectl proxy
# Access: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
```

---

## 🔧 Useful Commands

### Kubernetes Operations

```bash
# View all resources
kubectl get all -n enterprise-observability

# View logs
kubectl logs -f deployment/gateway -n enterprise-observability

# Describe a pod
kubectl describe pod <pod-name> -n enterprise-observability

# Execute into a pod
kubectl exec -it <pod-name> -n enterprise-observability -- /bin/bash

# Port forward
kubectl port-forward svc/gateway 8080:8080 -n enterprise-observability
```

### Flux Operations

```bash
# View all Flux resources
flux get all

# Force reconciliation
flux reconcile kustomization flux-system --with-source

# View Flux logs
flux logs --follow

# Suspend/Resume
flux suspend kustomization flux-system
flux resume kustomization flux-system
```

### Terraform Operations

```bash
# View current state
terraform show

# View outputs
terraform output

# Refresh state
terraform refresh

# View specific resource
terraform state show aws_eks_cluster.main
```

---

## 🗑️ Destroy Infrastructure (When Done)

**⚠️ WARNING: This will delete ALL resources and data!**

```bash
# Step 1: Delete Kubernetes resources
kubectl delete namespace enterprise-observability --wait=false
kubectl delete namespace dynatrace --wait=false
kubectl delete namespace flux-system --wait=false
kubectl delete namespace ingress-nginx --wait=false

# Step 2: Wait for LoadBalancers to be deleted (important!)
kubectl get svc --all-namespaces | grep LoadBalancer
# Wait until no LoadBalancer services remain

# Step 3: Destroy Terraform infrastructure
cd terraform
terraform destroy -auto-approve

# Step 4: Delete ECR repositories
aws ecr delete-repository --repository-name enterprise-observability/gateway --force --region us-east-1
aws ecr delete-repository --repository-name enterprise-observability/authentication-service --force --region us-east-1
aws ecr delete-repository --repository-name enterprise-observability/employee-service --force --region us-east-1
aws ecr delete-repository --repository-name enterprise-observability/leave-service --force --region us-east-1
aws ecr delete-repository --repository-name enterprise-observability/payroll-service --force --region us-east-1
aws ecr delete-repository --repository-name enterprise-observability/asset-service --force --region us-east-1
aws ecr delete-repository --repository-name enterprise-observability/notification-service --force --region us-east-1
aws ecr delete-repository --repository-name enterprise-observability/helpdesk-service --force --region us-east-1
aws ecr delete-repository --repository-name enterprise-observability/frontend --force --region us-east-1

# Step 5: Verify all resources are deleted
aws eks list-clusters --region us-east-1
# Should show no cluster

aws ec2 describe-vpcs --filters "Name=tag:Name,Values=enterprise-eks-vpc" --region us-east-1
# Should show no VPCs
```

---

## 📝 Important Notes

1. **Cost Optimization**
   - The EKS cluster costs ~$0.10/hour ($72/month)
   - EC2 instances (3 x t3.large) cost ~$0.25/hour ($180/month)
   - Total estimated cost: ~$250/month
   - **Always destroy when not in use!**

2. **Secrets Management**
   - Never commit secrets to Git
   - Use AWS Secrets Manager or Sealed Secrets for production
   - Update `k8s/secrets.yaml` with your values before deployment

3. **GitHub Token**
   - Required for FluxCD bootstrap
   - Needs `repo`, `workflow`, `admin:public_key` scopes
   - Create at: https://github.com/settings/tokens

4. **Dynatrace Trial**
   - Free 15-day trial available
   - Sign up at: https://www.dynatrace.com/trial/

5. **State Management**
   - Terraform state is local (not recommended for production)
   - For production, use S3 backend for state storage

---

## 🆘 Troubleshooting

### Issue: EKS nodes not joining cluster

```bash
# Check node group status
aws eks describe-nodegroup --cluster-name enterprise-eks-cluster --nodegroup-name <nodegroup-name> --region us-east-1

# Check IAM role permissions
aws iam get-role --role-name <node-role-name>
```

### Issue: Pods stuck in Pending state

```bash
# Check pod events
kubectl describe pod <pod-name> -n enterprise-observability

# Check node resources
kubectl top nodes

# Check PVC status
kubectl get pvc -n enterprise-observability
```

### Issue: Flux not syncing

```bash
# Check Flux logs
flux logs --kind=Kustomization --name=flux-system

# Check Git authentication
kubectl get secret flux-system -n flux-system -o yaml

# Force reconciliation
flux reconcile source git flux-system
flux reconcile kustomization flux-system
```

### Issue: Dynatrace not reporting data

```bash
# Check Dynatrace pods
kubectl get pods -n dynatrace

# Check Dynatrace logs
kubectl logs -l app.kubernetes.io/name=dynatrace-operator -n dynatrace

# Verify tokens
kubectl get secret dynakube -n dynatrace -o yaml
```

---

## 📚 Additional Resources

- [Project README](README.md) - Complete project documentation
- [Deployment Guide](docs/DEPLOYMENT_GUIDE.md) - Detailed deployment steps
- [FluxCD Guide](FLUXCD_QUICKSTART.md) - GitOps workflow
- [API Reference](docs/API_REFERENCE.md) - API endpoints
- [Architecture](README.md#architecture) - System architecture diagrams

---

## ⏱️ Estimated Times

| Task | Time |
|------|------|
| Prerequisites setup | 30 min (one-time) |
| Terraform infrastructure | 15-20 min |
| Build and push images | 10 min |
| Install Dynatrace | 5 min |
| Bootstrap FluxCD | 3 min |
| Deploy applications | 5 min |
| **Total** | **40-45 min** |

---

**🎉 You're all set! Your Enterprise Observability Platform is now running!**
