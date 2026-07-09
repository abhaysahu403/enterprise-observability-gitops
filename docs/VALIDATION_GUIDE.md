# Platform Validation & Destruction Guide

## Purpose

This guide helps you:
1. ✅ Verify the repository contains everything needed for deployment
2. ✅ Test that deployment works from scratch
3. ✅ Validate all components are operational
4. ✅ Safely destroy Terraform infrastructure
5. ✅ Confirm only Git repository remains (preserving ECR images)

---

## Pre-Destruction Checklist

Before destroying infrastructure, verify:

### 1. All Code is Committed
```bash
cd /path/to/enterprise-observability-demo-up

# Check git status
git status

# Should show: "nothing to commit, working tree clean"
# If not, commit remaining changes:
git add .
git commit -m "final: ready for archival"
git push origin main
```

### 2. All Images are in ECR
```bash
# List all ECR repositories
aws ecr describe-repositories --region us-east-1

# Verify images exist for all services:
# - gateway
# - authentication-service
# - employee-service
# - leave-service
# - payroll-service
# - asset-service
# - helpdesk-service
# - notification-service
# - frontend

# List images in each repository
aws ecr list-images --repository-name enterprise-observability/gateway --region us-east-1
# Repeat for all services
```

### 3. Documentation is Complete
```bash
# Verify these files exist:
ls docs/

# Should include:
# - ARCHITECTURE.md
# - DEPLOYMENT_GUIDE.md
# - RECOVERY_GUIDE.md
# - VALIDATION_GUIDE.md (this file)
# - GRAFANA_INFLUXDB_DEPLOYMENT.md
# - CICD_PIPELINE.md
# - API_REFERENCE.md
# - screenshots/
```

### 4. GitHub Repository is Updated
```bash
# Verify latest commit is pushed
git log -1

# Check GitHub:
# https://github.com/abhaysahu403/enterprise-observability-demo-up
# Latest commit should match local
```

---

## Validation Test: Deploy from Scratch

### Phase 1: Deploy Infrastructure (15 min)
```bash
cd terraform

terraform init
terraform plan -out=tfplan
terraform apply tfplan

# Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name enterprise-eks-cluster

# Verify nodes
kubectl get nodes
# Should show 3 nodes in Ready state
```

### Phase 2: Install Prerequisites (5 min)
```bash
# Install AWS Load Balancer Controller (simplified - using manifest)
kubectl apply -k "github.com/aws/eks-charts/stable/aws-load-balancer-controller/crds?ref=master"

# Install NGINX Ingress
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/aws/deploy.yaml

# Install Metrics Server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### Phase 3: Deploy Dynatrace (5 min)
```bash
# Create Dynatrace namespace
kubectl create namespace dynatrace

# Create secret (replace with your tokens)
kubectl create secret generic enterprise-eks-cluster \
  --from-literal=apiToken=YOUR_API_TOKEN \
  --from-literal=dataIngestToken=YOUR_DATA_INGEST_TOKEN \
  -n dynatrace

# Install Dynatrace Operator
kubectl apply -f https://github.com/Dynatrace/dynatrace-operator/releases/latest/download/kubernetes.yaml

# Deploy DynaKube (update apiUrl in k8s/dynatrace/dynakube.yaml first)
kubectl apply -f k8s/dynatrace/dynakube.yaml
```

### Phase 4: Deploy Applications (5 min)
```bash
# Create namespace
kubectl create namespace enterprise-observability

# Deploy all resources
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/redis/
kubectl apply -f k8s/auth-service/
kubectl apply -f k8s/employee-service/
kubectl apply -f k8s/leave-service/
kubectl apply -f k8s/payroll-service/
kubectl apply -f k8s/asset-service/
kubectl apply -f k8s/helpdesk-service/
kubectl apply -f k8s/notification-service/
kubectl apply -f k8s/gateway/
kubectl apply -f k8s/frontend/
kubectl apply -f k8s/ingress/

# Deploy monitoring stack
kubectl apply -f k8s/monitoring/
```

### Phase 5: Verify Deployment (5 min)
```bash
# Wait for all pods to be ready (may take 3-5 minutes)
kubectl wait --for=condition=ready pod --all -n enterprise-observability --timeout=600s

# Check pod status
kubectl get pods -n enterprise-observability

# Should see all pods 1/1 Running:
# - postgres-xxx
# - redis-xxx
# - authentication-service-xxx
# - employee-service-xxx
# - leave-service-xxx
# - payroll-service-xxx
# - asset-service-xxx
# - helpdesk-service-xxx
# - notification-service-xxx
# - gateway-xxx
# - frontend-xxx

# Check Dynatrace
kubectl get pods -n dynatrace
# Should see operator, webhook, activegate, oneagent pods running

# Check monitoring
kubectl get pods -n monitoring
# Should see grafana, influxdb, otel-collector pods running

# Get application URL
kubectl get ingress -n enterprise-observability
```

### Phase 6: Test Application (5 min)
```bash
# Get Load Balancer URL
LB_URL=$(kubectl get ingress ingress-nginx -n enterprise-observability -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

# Test health endpoints
curl -s http://$LB_URL/api/health | jq
curl -s http://$LB_URL/api/employees | jq

# Test UI access
echo "Open browser: http://$LB_URL"
echo "Login: admin / Password@123"

# Verify you can:
# - Login successfully
# - View employees list
# - Create a leave application
# - View dashboard metrics
```

### Phase 7: Test Monitoring (5 min)

#### Test Dynatrace
```bash
# 1. Open your Dynatrace tenant
# 2. Go to Services
# 3. Verify you see all 8 microservices + gateway
# 4. Click on gateway → View PurePaths
# 5. Generate traffic and verify traces appear
```

#### Test Grafana
```bash
# Port-forward Grafana
kubectl port-forward -n monitoring svc/grafana 3000:80 &

# Open browser: http://localhost:3000
# Login: admin / GrafanaAdmin@Secure123

# Go to Explore and run:
from(bucket: "metrics")
  |> range(start: -10m)
  |> filter(fn: (r) => r._measurement == "jvm_memory_used_bytes")
  |> limit(n: 10)

# Should see data from services
```

### Phase 8: Generate Test Data (2 min)
```bash
# Generate traffic to populate metrics
for i in {1..100}; do
  curl -s http://$LB_URL/api/employees > /dev/null
  curl -s http://$LB_URL/api/leaves > /dev/null
  sleep 0.1
done

# Wait 2 minutes, then check Grafana again
# Metrics should show increased activity
```

---

## ✅ Validation Checklist

Mark each item as complete:

- [ ] Terraform deployed successfully (3 nodes Running)
- [ ] All 11 application pods are 1/1 Running
- [ ] All 8 Dynatrace pods are Running
- [ ] All 4 monitoring pods are Running
- [ ] Load Balancer URL is accessible
- [ ] Can login to application UI
- [ ] Can view employees, create leave applications
- [ ] Dynatrace shows all services with traces
- [ ] Grafana shows metrics from all services
- [ ] No pods in Error or CrashLoopBackOff state

**If ALL checkboxes are marked, the platform is VALIDATED ✅**

---

## Safe Destruction Procedure

⚠️ **ONLY proceed if validation is complete and repository is pushed to GitHub**

### Step 1: Document Current State
```bash
# Capture final state before destruction
kubectl get all -A > final-state.txt
kubectl get pv > final-pvs.txt
kubectl get pvc -A > final-pvcs.txt

# Commit these for reference
git add final-*.txt
git commit -m "docs: capture final state before destruction"
git push origin main
```

### Step 2: Delete Application Workloads (Optional)
```bash
# This is optional - Terraform destroy will handle it
# But doing this first is cleaner

kubectl delete namespace enterprise-observability
kubectl delete namespace dynatrace
kubectl delete namespace monitoring

# Wait for termination
kubectl wait --for=delete namespace/enterprise-observability --timeout=300s
kubectl wait --for=delete namespace/dynatrace --timeout=300s
kubectl wait --for=delete namespace/monitoring --timeout=300s
```

### Step 3: Verify No PVCs Remain
```bash
# Check for any remaining PVCs
kubectl get pvc -A

# If any exist, delete them
kubectl delete pvc --all -n enterprise-observability
kubectl delete pvc --all -n monitoring
```

### Step 4: Destroy Terraform Infrastructure
```bash
cd terraform

# Destroy everything
terraform destroy -auto-approve

# This will delete:
# - EKS Node Group
# - EKS Cluster
# - NAT Gateways
# - Internet Gateway
# - Subnets
# - VPC
# - Security Groups
# - IAM Roles
# - EBS Volumes (if any attached to nodes)
```

⏱️ **Expected Time**: 10-15 minutes

### Step 5: Verify AWS Cleanup
```bash
# Verify EKS cluster is gone
aws eks list-clusters --region us-east-1
# Should NOT show enterprise-eks-cluster

# Verify EC2 instances are terminated
aws ec2 describe-instances --region us-east-1 \
  --filters "Name=tag:Name,Values=enterprise-eks-cluster*" \
  --query "Reservations[].Instances[].State.Name"
# Should show all "terminated"

# Verify NAT Gateways are deleted
aws ec2 describe-nat-gateways --region us-east-1 \
  --filter "Name=tag:Name,Values=*enterprise*"
# Should show empty or "deleted" state

# Verify Load Balancers are deleted
aws elbv2 describe-load-balancers --region us-east-1
# Should NOT show k8s-* load balancers

# Verify VPC is deleted
aws ec2 describe-vpcs --region us-east-1 \
  --filters "Name=tag:Name,Values=enterprise-vpc"
# Should show empty
```

### Step 6: Check for Remaining Resources
```bash
# Check for any EBS volumes
aws ec2 describe-volumes --region us-east-1 \
  --filters "Name=tag:kubernetes.io/cluster/enterprise-eks-cluster,Values=owned"
# Should be empty - if any exist, delete them:
# aws ec2 delete-volume --volume-id vol-xxxxx --region us-east-1

# Check for any remaining security groups
aws ec2 describe-security-groups --region us-east-1 \
  --filters "Name=group-name,Values=*enterprise-eks-cluster*"
# Should be empty
```

### Step 7: Verify ECR Images are PRESERVED
```bash
# Verify ECR repositories still exist
aws ecr describe-repositories --region us-east-1

# Should show all repositories:
# - enterprise-observability/gateway
# - enterprise-observability/authentication-service
# - enterprise-observability/employee-service
# (and all others)

# Verify images are present
aws ecr list-images --repository-name enterprise-observability/gateway --region us-east-1
# Should show images with tags
```

### Step 8: Final Cost Verification
```bash
# Check AWS Cost Explorer for running resources
# Go to: https://console.aws.amazon.com/cost-management/home

# Verify these costs are ZERO after 24 hours:
# - EC2 instances
# - EKS cluster
# - NAT Gateway
# - Load Balancer
# - EBS volumes

# Expected remaining costs:
# - ECR storage (~$1-5/month for container images) - KEEP THESE
# - S3 (if any) - minimal
```

---

## Post-Destruction Verification

### What Should EXIST:
✅ Git repository with all code  
✅ ECR container images (all services)  
✅ Documentation (README, all docs/)  
✅ Terraform state files (for reference)  
✅ GitHub repository  

### What Should NOT EXIST:
❌ EKS cluster  
❌ EC2 instances (worker nodes)  
❌ NAT Gateways  
❌ Load Balancers  
❌ Running pods  
❌ EBS volumes  
❌ VPC (enterprise-vpc)  

---

## Recovery from This Point

After destruction, to redeploy the entire platform:

```bash
# 1. Clone repository
git clone https://github.com/abhaysahu403/enterprise-observability-demo-up.git
cd enterprise-observability-demo-up

# 2. Deploy (follow RECOVERY_GUIDE.md)
cd terraform
terraform init
terraform apply

# 3. Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name enterprise-eks-cluster

# 4. Deploy everything (follow steps in RECOVERY_GUIDE.md)
# Total time: 30-40 minutes
```

---

## Estimated Monthly Costs

### When Running (Before Destruction):
- EKS Cluster: ~$73/month
- 3x t3.xlarge nodes: ~$150/month
- NAT Gateways: ~$65/month (2 gateways)
- Load Balancer: ~$20/month
- EBS Volumes: ~$10/month
- **Total: ~$318/month**

### After Destruction:
- ECR Storage: ~$2-5/month (container images)
- S3 (if any): <$1/month
- **Total: ~$3-6/month**

💰 **Savings**: ~$312/month after destruction while preserving ability to redeploy!

---

## Troubleshooting Destruction Issues

### Issue: Terraform destroy hangs
```bash
# Cancel with Ctrl+C
# Check what's blocking
terraform destroy | grep "Still destroying"

# Common blockers:
# - Load Balancers (delete manually)
# - Security groups (wait for LBs to delete first)
# - ENIs (elastic network interfaces)

# Force delete Load Balancers
aws elbv2 describe-load-balancers --region us-east-1 --query "LoadBalancers[?contains(LoadBalancerName, 'k8s')].LoadBalancerArn" --output text | xargs -I {} aws elbv2 delete-load-balancer --load-balancer-arn {} --region us-east-1

# Retry destroy
terraform destroy -auto-approve
```

### Issue: VPC deletion fails
```bash
# VPC may have dependent resources
# Delete in order:
# 1. NAT Gateways
aws ec2 describe-nat-gateways --filter "Name=vpc-id,Values=vpc-xxxxx" --query "NatGateways[].NatGatewayId" --output text | xargs -I {} aws ec2 delete-nat-gateway --nat-gateway-id {}

# 2. Internet Gateway
aws ec2 describe-internet-gateways --filters "Name=attachment.vpc-id,Values=vpc-xxxxx" --query "InternetGateways[].InternetGatewayId" --output text | xargs -I {} aws ec2 detach-internet-gateway --internet-gateway-id {} --vpc-id vpc-xxxxx
aws ec2 delete-internet-gateway --internet-gateway-id igw-xxxxx

# 3. Retry Terraform destroy
terraform destroy -auto-approve
```

### Issue: Security group dependency errors
```bash
# Wait 5-10 minutes after destroying EKS
# Security groups have dependencies that take time to clear
# Then retry:
terraform destroy -auto-approve
```

---

## Final Checklist Before Marking Complete

- [ ] All validation tests passed
- [ ] Git repository is pushed to GitHub
- [ ] ECR images are confirmed present
- [ ] Documentation is complete
- [ ] Terraform destroy completed successfully
- [ ] AWS Console shows no EKS cluster
- [ ] AWS Console shows no EC2 instances (worker nodes)
- [ ] AWS Console shows no NAT Gateways
- [ ] AWS Console shows no k8s Load Balancers
- [ ] ECR repositories still exist with images
- [ ] Cost Explorer shows costs dropping to ~$3-6/month
- [ ] Can clone repository and redeploy using RECOVERY_GUIDE.md

✅ **If all checkboxes are marked, destruction is COMPLETE and SAFE!**

---

**Platform Status**: Ready for long-term archival  
**Recovery Time**: 30-40 minutes from Git clone  
**Monthly Storage Cost**: ~$3-6 (ECR images only)  
**Last Validated**: 2026-07-09
