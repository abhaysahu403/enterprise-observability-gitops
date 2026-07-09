# 🚀 Complete Command Reference - Enterprise Observability Platform

**One document with ALL commands to deploy, verify, and destroy the complete platform.**

⏱️ **Total Time**: 30-40 minutes from Git clone to fully operational  
💰 **Cost**: ~$250-300/month when running, ~$3-6/month after destruction  
📦 **What Remains**: Git repository + ECR images (instant redeployment capability)

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Deployment Commands](#deployment-30-40-minutes)
3. [Verification Commands](#verification-commands)
4. [Access & Monitoring](#access--monitoring)
5. [Destruction Commands](#destruction-commands)
6. [Post-Destruction Verification](#post-destruction-verification)
7. [Redeployment](#redeployment-from-scratch)
8. [Troubleshooting](#troubleshooting)

---

## ⚡ Prerequisites

### 1. Required Tools (One-Time Setup)

```bash
# Verify installations
aws --version          # AWS CLI v2+
terraform --version    # Terraform 1.0+
kubectl version        # kubectl 1.28+
git --version          # Git 2.0+

# Install if missing:
# - AWS CLI: https://aws.amazon.com/cli/
# - Terraform: https://www.terraform.io/downloads
# - kubectl: https://kubernetes.io/docs/tasks/tools/
```

### 2. AWS Credentials

```bash
# Configure AWS
aws configure
# Enter: Access Key ID, Secret Access Key, Region (us-east-1), Format (json)

# Verify
aws sts get-caller-identity
```

### 3. Dynatrace Account

- Sign up: https://www.dynatrace.com/trial/ (15-day free trial)
- Get your tenant URL: `https://YOUR_TENANT.live.dynatrace.com`
- Generate tokens (Settings → Integration → Dynatrace API):
  - **API Token** with: Read/Write configuration, Access problems, PaaS integration
  - **Data Ingest Token** with: Ingest metrics, logs, events, traces

---

## 🚀 DEPLOYMENT (30-40 minutes)

### Step 1: Clone Repository (1 min)

```bash
git clone https://github.com/abhaysahu403/enterprise-observability-demo-up.git
cd enterprise-observability-demo-up
```

### Step 2: Deploy Terraform Infrastructure (15-20 min)

```bash
cd terraform

# Initialize Terraform
terraform init

# Review plan
terraform plan -out=tfplan

# Deploy infrastructure
terraform apply tfplan
```

**✅ What gets created:**
- VPC with CIDR 10.0.0.0/16
- 2 Public subnets, 2 Private subnets
- Internet Gateway, 2 NAT Gateways
- EKS Cluster (Kubernetes 1.28+)
- 3 × t3.xlarge nodes (12 vCPU, 48GB RAM)
- IAM roles, Security groups
- EBS CSI driver

**⏱️ Expected time:** 15-20 minutes

```bash
# Configure kubectl to access cluster
aws eks update-kubeconfig --region us-east-1 --name enterprise-eks-cluster

# Verify nodes are ready
kubectl get nodes
```

**✅ Expected output:**
```
NAME                          STATUS   ROLES    AGE   VERSION
ip-10-0-1-xxx.ec2.internal    Ready    <none>   5m    v1.28.x
ip-10-0-2-xxx.ec2.internal    Ready    <none>   5m    v1.28.x
ip-10-0-3-xxx.ec2.internal    Ready    <none>   5m    v1.28.x
```

**📝 Expected:** 3 nodes in `Ready` state

### Step 3: Install Kubernetes Components (5 min)

```bash
# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/aws/deploy.yaml

# Wait for ingress controller to be ready
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s

# Install Metrics Server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

**✅ Verify:**
```bash
kubectl get pods -n ingress-nginx
kubectl get pods -n kube-system | grep metrics-server
```

### Step 4: Deploy Dynatrace Monitoring (5 min)

```bash
# Create Dynatrace namespace
kubectl create namespace dynatrace

# Create secret with your Dynatrace tokens (replace with YOUR tokens)
kubectl create secret generic enterprise-eks-cluster \
  --from-literal=apiToken=YOUR_API_TOKEN_HERE \
  --from-literal=dataIngestToken=YOUR_DATA_INGEST_TOKEN_HERE \
  -n dynatrace

# Install Dynatrace Operator
kubectl apply -f https://github.com/Dynatrace/dynatrace-operator/releases/latest/download/kubernetes.yaml

# Wait for operator to be ready
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/name=dynatrace-operator \
  -n dynatrace \
  --timeout=300s

# Deploy DynaKube (IMPORTANT: Update apiUrl in k8s/dynatrace/dynakube.yaml first!)
# Edit: apiUrl: "https://YOUR_TENANT.live.dynatrace.com/api"
kubectl apply -f k8s/dynatrace/dynakube.yaml
```

**✅ Verify Dynatrace:**
```bash
kubectl get pods -n dynatrace
```

**📝 Expected:** After 3-5 minutes:
```
NAME                                          READY   STATUS
dynatrace-operator-xxx                        1/1     Running
dynatrace-webhook-xxx                         1/1     Running
enterprise-eks-cluster-activegate-0           1/1     Running
dynatrace-oneagent-xxx                        1/1     Running  (3 pods - one per node)
dynatrace-oneagent-csi-driver-xxx             1/1     Running  (3 pods)
```
**Total:** 8 Dynatrace pods running

### Step 5: Deploy Application Stack (5 min)

```bash
# Return to project root
cd ..

# Create application namespace
kubectl create namespace enterprise-observability

# Deploy in order:
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml

# Deploy databases (wait for these to be ready first)
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/redis/

# Wait for databases
kubectl wait --for=condition=ready pod \
  -l app=postgres \
  -n enterprise-observability \
  --timeout=300s

kubectl wait --for=condition=ready pod \
  -l app=redis \
  -n enterprise-observability \
  --timeout=300s

# Deploy microservices
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
```

### Step 6: Deploy Monitoring Stack (Grafana + InfluxDB) (2 min)

```bash
# Deploy Grafana, InfluxDB, OpenTelemetry Collector
kubectl apply -f k8s/monitoring/
```

---

## ✅ VERIFICATION COMMANDS

### Check All Pods are Running

```bash
# Check application pods
kubectl get pods -n enterprise-observability
```

**📝 Expected output (all 1/1 READY, STATUS Running):**
```
NAME                                      READY   STATUS    RESTARTS
asset-service-xxx                         1/1     Running   0
authentication-service-xxx                1/1     Running   0
employee-service-xxx                      1/1     Running   0
frontend-xxx                              1/1     Running   0
gateway-xxx                               1/1     Running   0
helpdesk-service-xxx                      1/1     Running   0
leave-service-xxx                         1/1     Running   0
notification-service-xxx                  1/1     Running   0
payroll-service-xxx                       1/1     Running   0
postgres-xxx                              1/1     Running   0
redis-xxx                                 1/1     Running   0
```
**Total:** 11 application pods (all 1/1 Running)

```bash
# Check Dynatrace pods
kubectl get pods -n dynatrace
```

**📝 Expected:** 8 Dynatrace pods (1 operator, 1 webhook, 1 activegate, 3 oneagent, 3 csi-driver)

```bash
# Check monitoring pods
kubectl get pods -n monitoring
```

**📝 Expected output:**
```
NAME                              READY   STATUS    RESTARTS
grafana-xxx                       1/1     Running   0
influxdb-influxdb2-0              1/1     Running   0
otel-collector-xxx                1/1     Running   0
otel-collector-xxx                1/1     Running   0
```
**Total:** 4 monitoring pods (1 Grafana, 1 InfluxDB, 2 OTel Collectors)

### Verify Services and Ingress

```bash
# Check all services
kubectl get svc -n enterprise-observability

# Get Load Balancer URL
kubectl get ingress -n enterprise-observability

# Alternative command to get URL
LB_URL=$(kubectl get ingress ingress-nginx -n enterprise-observability -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo "Application URL: http://$LB_URL"
```

**📝 Expected:** Ingress should show an EXTERNAL-IP (AWS Load Balancer hostname)

### Test Application Health

```bash
# Get Load Balancer URL
LB_URL=$(kubectl get ingress ingress-nginx -n enterprise-observability -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

# Test health endpoint
curl -s http://$LB_URL/api/health

# Test employee API
curl -s http://$LB_URL/api/employees | jq
```

**📝 Expected:** Both commands return JSON responses with HTTP 200

### Complete Verification Checklist

```bash
# Run all checks at once
echo "=== NODE CHECK ==="
kubectl get nodes

echo "\n=== APPLICATION PODS ==="
kubectl get pods -n enterprise-observability

echo "\n=== DYNATRACE PODS ==="
kubectl get pods -n dynatrace

echo "\n=== MONITORING PODS ==="
kubectl get pods -n monitoring

echo "\n=== SERVICES ==="
kubectl get svc -n enterprise-observability

echo "\n=== INGRESS ==="
kubectl get ingress -n enterprise-observability

echo "\n=== LOAD BALANCER URL ==="
kubectl get ingress ingress-nginx -n enterprise-observability -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
echo ""
```

**✅ Platform is FULLY OPERATIONAL when:**
- [ ] 3 nodes in `Ready` state
- [ ] 11 application pods `1/1 Running`
- [ ] 8 Dynatrace pods `Running`
- [ ] 4 monitoring pods `1/1 Running`
- [ ] Ingress has EXTERNAL-IP
- [ ] curl commands return JSON data
- [ ] **Total: 23 pods running across all namespaces**

---

## 🌐 ACCESS & MONITORING

### Access Application UI

```bash
# Get URL
LB_URL=$(kubectl get ingress ingress-nginx -n enterprise-observability -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo "Open browser: http://$LB_URL"
```

**Login credentials:**
- Username: `admin`
- Password: `Password@123`

### Access Grafana

```bash
# Port-forward Grafana
kubectl port-forward -n monitoring svc/grafana 3000:80

# Open browser: http://localhost:3000
```

**Login credentials:**
- Username: `admin`
- Password: `GrafanaAdmin@Secure123`

**Test InfluxDB connection:**
1. Go to: **Explore**
2. Run query:
```flux
from(bucket: "metrics")
  |> range(start: -10m)
  |> filter(fn: (r) => r._measurement == "jvm_memory_used_bytes")
  |> limit(n: 10)
```

**✅ Expected:** Should see JVM memory metrics from services

### Access Dynatrace

```bash
# Open your Dynatrace tenant URL
# Example: https://abc12345.live.dynatrace.com
```

**Navigate to:**
1. **Services** → See all 8 microservices + gateway
2. **Kubernetes** → View cluster: `enterprise-eks-cluster`
3. **Problems** → Check Davis AI detections
4. **Distributed traces** → Click any service → View PurePaths

### Generate Test Traffic

```bash
# Generate traffic to populate metrics
LB_URL=$(kubectl get ingress ingress-nginx -n enterprise-observability -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

for i in {1..100}; do
  curl -s http://$LB_URL/api/employees > /dev/null
  curl -s http://$LB_URL/api/leaves > /dev/null
  curl -s http://$LB_URL/api/health > /dev/null
  sleep 0.2
done
```

**📝 After 2-3 minutes:** Grafana and Dynatrace should show increased activity

---

## 🗑️ DESTRUCTION COMMANDS

**⚠️ WARNING:** This will delete ALL AWS infrastructure except ECR images and Git repository.

### Step 1: Delete Kubernetes Namespaces

```bash
# Delete application namespace
kubectl delete namespace enterprise-observability

# Delete Dynatrace namespace
kubectl delete namespace dynatrace

# Delete monitoring namespace
kubectl delete namespace monitoring

# Delete ingress namespace
kubectl delete namespace ingress-nginx

# Wait for all namespaces to be fully deleted (5-10 min)
kubectl get namespaces
```

**📝 Important:** Wait until ALL namespaces are gone before proceeding!

### Step 2: Verify No Load Balancers Remain

```bash
# Check for any remaining Load Balancers
aws elbv2 describe-load-balancers --region us-east-1 --query "LoadBalancers[?contains(LoadBalancerName, 'k8s')].LoadBalancerName"

# If any exist, delete manually:
# aws elbv2 delete-load-balancer --load-balancer-arn <ARN> --region us-east-1
```

**📝 Expected:** No k8s-* load balancers should exist

### Step 3: Destroy Terraform Infrastructure

```bash
cd terraform

# Destroy everything
terraform destroy -auto-approve
```

**⏱️ Expected time:** 10-15 minutes

**✅ What gets deleted:**
- EKS Node Group (3 nodes terminated)
- EKS Cluster
- NAT Gateways (2)
- Internet Gateway
- Subnets (4)
- VPC
- Security Groups
- IAM Roles
- EBS Volumes

**✅ What is PRESERVED:**
- Git repository (all code)
- ECR repositories and images
- Terraform state files

---

## ✅ POST-DESTRUCTION VERIFICATION

### Verify Infrastructure is Gone

```bash
# Check EKS cluster is deleted
aws eks list-clusters --region us-east-1
# Expected: Should NOT show "enterprise-eks-cluster"

# Check EC2 instances are terminated
aws ec2 describe-instances --region us-east-1 \
  --filters "Name=tag:Name,Values=*enterprise-eks*" \
  --query "Reservations[].Instances[].State.Name"
# Expected: All "terminated" or empty

# Check NAT Gateways are deleted
aws ec2 describe-nat-gateways --region us-east-1 \
  --filter "Name=tag:Name,Values=*enterprise*" \
  --query "NatGateways[].State"
# Expected: All "deleted" or empty

# Check Load Balancers are deleted
aws elbv2 describe-load-balancers --region us-east-1 \
  --query "LoadBalancers[?contains(LoadBalancerName, 'k8s')].LoadBalancerName"
# Expected: Empty list

# Check VPC is deleted
aws ec2 describe-vpcs --region us-east-1 \
  --filters "Name=tag:Name,Values=*enterprise*"
# Expected: Empty or no matching VPCs
```

### Verify ECR Images are PRESERVED

```bash
# List ECR repositories (should still exist)
aws ecr describe-repositories --region us-east-1

# Verify images exist in gateway repository
aws ecr list-images --repository-name enterprise-observability/gateway --region us-east-1

# List all images across repositories
for repo in gateway authentication-service employee-service leave-service payroll-service asset-service helpdesk-service notification-service frontend; do
  echo "=== $repo ==="
  aws ecr list-images --repository-name enterprise-observability/$repo --region us-east-1 --query 'imageIds[].imageTag' --output text
done
```

**✅ Expected:** All ECR repositories exist with images

### Verify Git Repository

```bash
# Check Git status
git status

# Verify latest commit is pushed
git log -1

# Check GitHub remote
git remote -v
```

**✅ Expected:** Git repository is clean, all changes committed and pushed

### Cost Verification

**Before Destruction:**
- EKS Cluster: ~$73/month
- 3× t3.xlarge nodes: ~$150/month  
- NAT Gateways (2): ~$65/month
- Load Balancer: ~$20/month
- EBS Volumes: ~$10/month
- **Total: ~$318/month**

**After Destruction:**
- ECR Storage: ~$2-5/month (container images)
- S3 (if any): <$1/month
- **Total: ~$3-6/month**

💰 **Monthly Savings: ~$312**

```bash
# Check AWS billing after 24-48 hours
# Go to: https://console.aws.amazon.com/billing/home

# Verify these costs are ZERO:
# - EC2 instances
# - EKS cluster
# - NAT Gateway
# - Load Balancer
# - EBS volumes
```

---

## 🔄 REDEPLOYMENT FROM SCRATCH

After destruction, to redeploy the entire platform:

```bash
# 1. Clone repository (if on new machine)
git clone https://github.com/abhaysahu403/enterprise-observability-demo-up.git
cd enterprise-observability-demo-up

# 2. Deploy Terraform
cd terraform
terraform init
terraform apply -auto-approve

# 3. Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name enterprise-eks-cluster

# 4. Follow deployment steps above (Steps 3-6)
# Total time: 30-40 minutes
```

---

## 🛠️ TROUBLESHOOTING

### Issue: Pods stuck in Pending

```bash
# Check pod events
kubectl describe pod <pod-name> -n enterprise-observability

# Check PVC status
kubectl get pvc -n enterprise-observability

# Check node resources
kubectl top nodes

# Solution: Verify storage class exists
kubectl get storageclass
# Should show 'gp2' as default
```

### Issue: Frontend CrashLoopBackOff

```bash
# Check logs
kubectl logs -n enterprise-observability deployment/frontend

# Common cause: nginx DNS resolution failure
# Solution: Frontend nginx.conf has runtime resolver for gateway-service
# No action needed - should resolve automatically
```

### Issue: Ingress has no EXTERNAL-IP

```bash
# Wait 3-5 minutes for AWS Load Balancer provisioning
kubectl get ingress -n enterprise-observability -w

# Check ingress controller
kubectl get pods -n ingress-nginx

# Check AWS Load Balancers
aws elbv2 describe-load-balancers --region us-east-1
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

# Check datasource URL in Grafana:
# Should be: http://influxdb-influxdb2.monitoring.svc.cluster.local:8086
```

### Issue: Terraform destroy hangs

```bash
# Cancel with Ctrl+C
# Delete Load Balancers manually first
aws elbv2 describe-load-balancers --region us-east-1 \
  --query "LoadBalancers[?contains(LoadBalancerName, 'k8s')].LoadBalancerArn" \
  --output text | xargs -I {} aws elbv2 delete-load-balancer --load-balancer-arn {} --region us-east-1

# Wait 5 minutes, then retry
terraform destroy -auto-approve
```

### Issue: Cannot access application after deployment

```bash
# Verify all pods are Running
kubectl get pods -n enterprise-observability

# Get Load Balancer URL
kubectl get ingress -n enterprise-observability

# Test from within cluster
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://gateway.enterprise-observability.svc.cluster.local:8080/api/health

# If internal curl works but external doesn't, check AWS Security Groups
```

---

## 📊 QUICK REFERENCE

### Essential Commands

```bash
# Get all pods across all namespaces
kubectl get pods -A

# Get pod logs
kubectl logs -f <pod-name> -n <namespace>

# Describe pod (see events)
kubectl describe pod <pod-name> -n <namespace>

# Port-forward to access service locally
kubectl port-forward -n <namespace> svc/<service-name> <local-port>:<service-port>

# Restart deployment
kubectl rollout restart deployment/<deployment-name> -n <namespace>

# Scale deployment
kubectl scale deployment/<deployment-name> --replicas=<count> -n <namespace>

# Execute command in pod
kubectl exec -it <pod-name> -n <namespace> -- /bin/bash

# Get service endpoints
kubectl get endpoints -n <namespace>

# Watch resources
kubectl get pods -n <namespace> -w
```

### Expected Pod Counts

| Namespace | Pods | Details |
|-----------|------|---------|
| `enterprise-observability` | 11 | postgres, redis, 7 services, gateway, frontend |
| `dynatrace` | 8 | operator, webhook, activegate, 3×oneagent, 3×csi-driver |
| `monitoring` | 4 | grafana, influxdb, 2×otel-collector |
| `ingress-nginx` | 1 | nginx-ingress-controller |
| `kube-system` | ~10 | aws-node, coredns, kube-proxy, metrics-server, etc. |
| **Total** | **~34** | **All pods across cluster** |

### Useful Grafana Queries

```flux
# JVM Memory Usage
from(bucket: "metrics")
  |> range(start: -1h)
  |> filter(fn: (r) => r._measurement == "jvm_memory_used_bytes")
  |> filter(fn: (r) => r.service_name == "gateway")

# HTTP Request Count
from(bucket: "metrics")
  |> range(start: -1h)
  |> filter(fn: (r) => r._measurement == "http_server_requests_seconds_count")
  |> filter(fn: (r) => r.service_name == "employee-service")

# CPU Usage
from(bucket: "metrics")
  |> range(start: -1h)
  |> filter(fn: (r) => r._measurement == "process_cpu_usage")

# Thread Count
from(bucket: "metrics")
  |> range(start: -1h)
  |> filter(fn: (r) => r._measurement == "jvm_threads_live_threads")

# Garbage Collection
from(bucket: "metrics")
  |> range(start: -1h)
  |> filter(fn: (r) => r._measurement == "jvm_gc_pause_seconds_count")
```

---

## 📝 IMPORTANT NOTES

### Cost Management
- EKS cluster + nodes cost ~$250-300/month when running
- **ALWAYS destroy when not in use** to avoid charges
- ECR images cost ~$3-6/month after destruction
- Redeployment takes only 30-40 minutes from ECR images

### Security
- Change default passwords before production use
- Rotate Dynatrace tokens regularly
- Never commit secrets to Git
- Use AWS Secrets Manager in production

### Monitoring
- Dynatrace monitors everything automatically (zero config)
- Grafana requires manual dashboard creation
- OTel Collector scrapes metrics every 30 seconds
- InfluxDB retains metrics for 30 days (configurable)

### GitOps (Optional)
- Platform can be deployed via FluxCD for continuous deployment
- See `FLUXCD_QUICKSTART.md` for GitOps setup
- All manifests in `k8s/` are FluxCD-ready

### GitHub Actions (Optional)
- CI/CD pipelines in `.github/workflows/` directory
- Automatically builds and pushes to ECR on code changes
- Includes: Maven build, unit tests, SonarQube, Trivy scanning

---

## 📚 Additional Documentation

- **[RECOVERY_GUIDE.md](docs/RECOVERY_GUIDE.md)** - Detailed disaster recovery procedures
- **[VALIDATION_GUIDE.md](docs/VALIDATION_GUIDE.md)** - Testing and validation checklist
- **[GRAFANA_INFLUXDB_DEPLOYMENT.md](docs/GRAFANA_INFLUXDB_DEPLOYMENT.md)** - Monitoring stack details
- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Complete architecture documentation
- **[API_REFERENCE.md](docs/API_REFERENCE.md)** - API endpoints and examples
- **[CICD_PIPELINE.md](docs/CICD_PIPELINE.md)** - CI/CD workflow documentation

---

## 🎯 SUMMARY

This document provides **ALL commands** needed to:

✅ Deploy the complete platform (30-40 min)  
✅ Verify everything is working (23+ pods running)  
✅ Access applications and monitoring dashboards  
✅ Destroy infrastructure safely (preserve ECR + Git)  
✅ Verify destruction completed correctly  
✅ Redeploy from scratch anytime (same 30-40 min)  

**Result after destruction:**
- Zero AWS compute costs (~$3-6/month ECR storage only)
- Complete Git repository preserved
- All Docker images in ECR preserved
- Instant redeployment capability maintained

---

**Platform Status**: Production-ready, fully documented, instantly redeployable  
**Last Updated**: 2026-07-09  
**Repository**: https://github.com/abhaysahu403/enterprise-observability-demo-up

