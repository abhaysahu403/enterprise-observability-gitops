# Complete Disaster Recovery Guide

## Purpose

This guide allows you to recreate the **entire Nimbus platform** from scratch using only this Git repository. Whether 6 months or 6 years from now, these instructions will deploy a fully operational system on AWS EKS with complete observability.

⏱️ **Estimated Total Time**: 30-40 minutes from zero to fully operational

---

## Prerequisites

### Required Tools
```bash
# Verify you have these installed
aws --version          # AWS CLI v2+
terraform --version    # Terraform 1.0+
kubectl version        # kubectl 1.28+
git --version          # Git 2.0+
docker --version       # Docker 20.0+ (for local testing)
```

### Required Accounts
1. **AWS Account** with admin access
2. **Dynatrace Tenant** (trial: https://www.dynatrace.com/trial/)
3. **GitHub Account** (for CI/CD, optional for recovery)

### AWS Credentials
```bash
# Configure AWS CLI
aws configure
# Enter: Access Key, Secret Key, Region (us-east-1), Format (json)

# Verify
aws sts get-caller-identity
```

---

## Phase 1: Clone Repository (1 minute)

```bash
git clone https://github.com/abhaysahu403/enterprise-observability-demo-up.git
cd enterprise-observability-demo-up
```

---

## Phase 2: Deploy Infrastructure (15-20 minutes)

### Step 1: Configure Terraform Variables

```bash
cd terraform

# Create terraform.tfvars (or use existing)
cat > terraform.tfvars <<EOF
aws_region = "us-east-1"
cluster_name = "enterprise-eks-cluster"
node_instance_type = "t3.xlarge"
desired_capacity = 3
min_capacity = 3
max_capacity = 4
EOF
```

### Step 2: Initialize and Deploy Terraform
```bash
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

⏱️ **Expected Time**: 15-20 minutes (EKS cluster creation is slow)

**What gets created:**
- VPC with CIDR 10.0.0.0/16
- 2 Public subnets (10.0.1.0/24, 10.0.2.0/24)
- 2 Private subnets (10.0.3.0/24, 10.0.4.0/24)
- Internet Gateway
- NAT Gateways (2)
- EKS Cluster
- EKS Node Group (3 t3.xlarge nodes)
- IAM roles and policies
- Security groups

### Step 3: Configure kubectl
```bash
aws eks update-kubeconfig --region us-east-1 --name enterprise-eks-cluster

# Verify
kubectl get nodes
# Should show 3 nodes in Ready state
```

---

## Phase 3: Install Kubernetes Components (5 minutes)

### Step 1: Install AWS Load Balancer Controller
```bash
# Download IAM policy
curl -o iam-policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/main/docs/install/iam_policy.json

# Create policy
aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://iam-policy.json

# Get your AWS account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Create service account (requires eksctl)
eksctl create iamserviceaccount \
  --cluster=enterprise-eks-cluster \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --attach-policy-arn=arn:aws:iam::${ACCOUNT_ID}:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve \
  --region=us-east-1

# Add Helm repo and install controller
helm repo add eks https://aws.github.io/eks-charts
helm repo update
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=enterprise-eks-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

### Step 2: Install NGINX Ingress Controller
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/aws/deploy.yaml

# Wait for Load Balancer to be created (2-3 minutes)
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s
```

### Step 3: Install Metrics Server
```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

---

## Phase 4: Deploy Dynatrace (5 minutes)

### Step 1: Get Dynatrace Tokens

Go to your Dynatrace tenant: **Settings → Integration → Dynatrace API**

Create two tokens:

**API Token** with permissions:
- Read configuration
- Write configuration
- Access problem and event feed
- PaaS integration - Installer download

**Data Ingest Token** with permissions:
- Ingest metrics
- Ingest logs
- Ingest events
- Ingest OpenTelemetry traces

### Step 2: Install Dynatrace Operator
```bash
# Create namespace
kubectl create namespace dynatrace

# Create secret with your tokens
kubectl create secret generic enterprise-eks-cluster \
  --from-literal=apiToken=YOUR_API_TOKEN_HERE \
  --from-literal=dataIngestToken=YOUR_DATA_INGEST_TOKEN_HERE \
  -n dynatrace

# Install Dynatrace Operator
kubectl apply -f https://github.com/Dynatrace/dynatrace-operator/releases/latest/download/kubernetes.yaml

# Wait for operator to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=dynatrace-operator -n dynatrace --timeout=300s
```

### Step 3: Deploy DynaKube

Update the DynaKube manifest with your Dynatrace URL:

```bash
# Edit k8s/dynatrace/dynakube.yaml
# Change apiUrl: "https://YOUR_TENANT.live.dynatrace.com/api"
vim k8s/dynatrace/dynakube.yaml

# Deploy
kubectl apply -f k8s/dynatrace/dynakube.yaml

# Verify Dynatrace components
kubectl get pods -n dynatrace
# Should see: operator, webhook, activegate, oneagent pods
```

---

## Phase 5: Deploy Application Stack (5 minutes)

### Step 1: Deploy via FluxCD (Recommended)
```bash
# Install Flux CLI (if not installed)
curl -s https://fluxcd.io/install.sh | sudo bash

# Bootstrap Flux (connects to your GitHub repo)
flux bootstrap github \
  --owner=abhaysahu403 \
  --repository=enterprise-observability-gitops \
  --path=k8s \
  --personal \
  --token-auth

# Flux will automatically deploy everything in k8s/ folder
# Wait 2-3 minutes for all resources to be created

# Watch deployment
flux get kustomizations --watch
```

### Step 2: Deploy Manually (Alternative)
```bash
# Create namespace
kubectl create namespace enterprise-observability

# Deploy in order
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml

# Deploy databases
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/redis/

# Wait for databases to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n enterprise-observability --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n enterprise-observability --timeout=300s

# Deploy services
kubectl apply -f k8s/auth-service/
kubectl apply -f k8s/employee-service/
kubectl apply -f k8s/leave-service/
kubectl apply -f k8s/payroll-service/
kubectl apply -f k8s/asset-service/
kubectl apply -f k8s/helpdesk-service/
kubectl apply -f k8s/notification-service/

# Deploy gateway and frontend
kubectl apply -f k8s/gateway/
kubectl apply -f k8s/frontend/

# Deploy ingress
kubectl apply -f k8s/ingress/

# Deploy monitoring stack (Grafana + InfluxDB)
kubectl apply -f k8s/monitoring/
```

---

## Phase 6: Verify Deployment (2 minutes)

### Check All Pods are Running
```bash
kubectl get pods -n enterprise-observability

# Expected output (all 1/1 READY):
# NAME                                      READY   STATUS
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
```

### Check Dynatrace Components
```bash
kubectl get pods -n dynatrace

# Expected output:
# dynatrace-operator-xxx                    1/1     Running
# dynatrace-webhook-xxx                     1/1     Running
# enterprise-eks-cluster-activegate-0       1/1     Running
# dynatrace-oneagent-xxx                    1/1     Running  (3 pods - one per node)
# dynatrace-oneagent-csi-driver-xxx         1/1     Running  (3 pods)
```

### Check Monitoring Stack
```bash
kubectl get pods -n monitoring

# Expected output:
# grafana-xxx                               1/1     Running
# influxdb-influxdb2-0                      1/1     Running
# otel-collector-xxx                        1/1     Running  (2 pods)
```

### Get Application URL
```bash
# Get the Load Balancer URL
kubectl get ingress -n enterprise-observability

# Or use this command
LB_URL=$(kubectl get ingress ingress-nginx -n enterprise-observability -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo "Application URL: http://$LB_URL"

# Access the application
echo "Login: admin / Password@123"
```

---

## Phase 7: Access Monitoring Dashboards

### Access Grafana
```bash
# Port-forward Grafana
kubectl port-forward -n monitoring svc/grafana 3000:80

# Open browser: http://localhost:3000
# Login: admin / GrafanaAdmin@Secure123

# Test datasource: Connections → Data sources → InfluxDB → Save & test
# View metrics: Explore → Run query:
# from(bucket: "metrics") |> range(start: -1h) |> limit(n: 10)
```

### Access Dynatrace
```bash
# Go to your Dynatrace tenant URL
# Example: https://abc12345.live.dynatrace.com

# Navigate to:
# - Services → See all microservices
# - Kubernetes → View cluster health
# - Problems → Check Davis AI detections
# - Dashboards → View custom dashboards
```

---

## Phase 8: Verify Full Functionality

### Test Application
```bash
# Generate test traffic
curl http://$LB_URL/api/health
curl http://$LB_URL/api/employees
curl http://$LB_URL/api/leaves

# Login to UI and test:
# - Employee management
# - Leave applications
# - Payroll viewing
# - Asset tracking
# - Helpdesk tickets
```

### Verify Distributed Tracing
1. Open Dynatrace
2. Go to **Services**
3. Click on any service (e.g., **gateway**)
4. View **Service flow** to see distributed traces
5. Click on any trace to see the complete call chain

### Verify Metrics in Grafana
1. Port-forward: `kubectl port-forward -n monitoring svc/grafana 3000:80`
2. Open: http://localhost:3000
3. Go to **Explore**
4. Run query:
   ```flux
   from(bucket: "metrics")
     |> range(start: -1h)
     |> filter(fn: (r) => r._measurement == "jvm_memory_used_bytes")
     |> filter(fn: (r) => r.service_name == "gateway")
   ```
5. Should see JVM memory metrics

---

## ✅ Success Criteria

Your platform is fully operational when:

- [ ] All 11 application pods are Running (1/1)
- [ ] All 8 Dynatrace pods are Running
- [ ] All 4 monitoring pods are Running (Grafana, InfluxDB, 2x OTel Collector)
- [ ] Application accessible via Load Balancer URL
- [ ] Can login and navigate the UI
- [ ] Dynatrace shows all services with PurePaths
- [ ] Grafana shows metrics from all services
- [ ] No pods in CrashLoopBackOff or Error state

---

## Troubleshooting Common Issues

### Issue: Pods stuck in Pending
```bash
# Check PVC status
kubectl get pvc -n enterprise-observability

# If PVC is Pending, check storage class
kubectl get storageclass

# Ensure gp2 storage class exists (should be default on EKS)
```

### Issue: Frontend shows "Cannot connect to backend"
```bash
# Check gateway is running
kubectl get pods -n enterprise-observability -l app=gateway

# Check ingress has Load Balancer
kubectl get ingress -n enterprise-observability

# May take 3-5 minutes for LB to become active
```

### Issue: Dynatrace OneAgent not injecting
```bash
# Check webhook is running
kubectl get pods -n dynatrace -l app.kubernetes.io/name=dynatrace-webhook

# Restart application pods to trigger injection
kubectl rollout restart deployment -n enterprise-observability
```

### Issue: Grafana shows "No data"
```bash
# Verify OTel Collector is scraping
kubectl logs -n monitoring deployment/otel-collector | grep "scrape"

# Verify InfluxDB has data
kubectl exec -n monitoring influxdb-influxdb2-0 -- influx query \
  'from(bucket:"metrics") |> range(start: -5m) |> limit(n:5)' \
  --token "enterprise-platform-influxdb-token-2024-super-secret" \
  --org enterprise-observability

# Check datasource URL in Grafana points to:
# http://influxdb-influxdb2.monitoring.svc.cluster.local:8086
```

---

## Recovery Time Summary

| Phase | Task | Time |
|-------|------|------|
| 1 | Clone repository | 1 min |
| 2 | Deploy Terraform infrastructure | 15-20 min |
| 3 | Install Kubernetes components | 5 min |
| 4 | Deploy Dynatrace | 5 min |
| 5 | Deploy applications | 5 min |
| 6 | Verify deployment | 2 min |
| 7 | Access monitoring | 2 min |
| **Total** | **End-to-end recovery** | **30-40 min** |

---

## Next Steps

After successful recovery:

1. **Configure Slack Alerts**: Set up Dynatrace workflows for Slack notifications
2. **Create Dashboards**: Build custom Grafana dashboards
3. **Set up CI/CD**: Configure GitHub Actions for automated deployments
4. **Enable GitOps**: Connect FluxCD to your Git repository
5. **Test Alerting**: Trigger test problems to verify Davis AI detection

---

## Important Notes

⚠️ **Cost Management**:
- This infrastructure costs ~$200-300/month when running
- See `docs/DESTROY_GUIDE.md` for safe teardown
- Remember to destroy when not in use!

🔐 **Security**:
- Change default passwords before production use
- Rotate Dynatrace tokens regularly
- Use AWS Secrets Manager for sensitive data in production
- Enable network policies and pod security policies

📊 **Monitoring**:
- Dynatrace monitors everything automatically
- Grafana requires manual dashboard creation
- OTel Collector scrapes metrics every 30 seconds
- InfluxDB retains metrics for 30 days (configurable)

---

**Last Updated**: 2026-07-09  
**Tested On**: AWS EKS 1.28, Kubernetes 1.28, Dynatrace OneAgent 1.283
