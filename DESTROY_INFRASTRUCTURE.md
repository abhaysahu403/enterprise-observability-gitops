# 🗑️ Infrastructure Destruction Guide

## ⚠️ WARNING

**This guide will permanently delete ALL resources and data. There is NO undo.**

Make sure you have:
- ✅ Backed up any important data
- ✅ Exported Dynatrace dashboards/SLOs
- ✅ Saved screenshots for documentation
- ✅ Committed all code to GitHub
- ✅ Downloaded logs if needed

---

## Estimated Cost Savings

Destroying the infrastructure will stop the following costs:

| Resource | Cost per Hour | Cost per Month |
|----------|---------------|----------------|
| EKS Control Plane | $0.10 | ~$72 |
| 3x t3.large EC2 (nodes) | $0.25 | ~$180 |
| Network Load Balancer | $0.02 | ~$15 |
| EBS Volumes (120GB) | - | ~$12 |
| ECR Storage (minimal) | - | ~$5 |
| **Total** | **~$0.37/hour** | **~$284/month** |

---

## Destruction Process (Step-by-Step)

### ⏱️ Total Time: ~20-30 minutes

---

## Step 1: Delete Kubernetes Resources (10 min)

### Why This Step is Critical
Before running `terraform destroy`, you MUST delete all Kubernetes resources that created AWS resources (LoadBalancers, EBS volumes). If you skip this, Terraform will fail because AWS resources will still be in use.

### 1.1 Delete All Application Resources

```bash
# Delete enterprise-observability namespace
kubectl delete namespace enterprise-observability --wait=true

# This deletes:
# - All deployments (gateway, microservices, frontend)
# - All services (including LoadBalancers)
# - All persistent volume claims
# - All configmaps and secrets
```

**Wait for completion** (this can take 3-5 minutes):
```bash
# Check if namespace is fully deleted
kubectl get namespace enterprise-observability
# Should return: Error from server (NotFound)
```

### 1.2 Delete Dynatrace

```bash
# Delete Dynatrace namespace
kubectl delete namespace dynatrace --wait=true

# This deletes:
# - Dynatrace Operator
# - OneAgent DaemonSet
# - ActiveGate Deployment
```

**Wait for completion**:
```bash
kubectl get namespace dynatrace
# Should return: Error from server (NotFound)
```

### 1.3 Delete FluxCD

```bash
# Delete flux-system namespace
kubectl delete namespace flux-system --wait=true

# This deletes:
# - FluxCD controllers
# - GitRepository resources
# - Kustomization resources
```

**Wait for completion**:
```bash
kubectl get namespace flux-system
# Should return: Error from server (NotFound)
```

### 1.4 Delete Ingress Controller

```bash
# Delete ingress-nginx namespace
kubectl delete namespace ingress-nginx --wait=true

# This deletes:
# - NGINX Ingress Controller
# - LoadBalancer service (CRITICAL!)
```

**Wait for completion**:
```bash
kubectl get namespace ingress-nginx
# Should return: Error from server (NotFound)
```

### 1.5 Verify No LoadBalancers Remain

**⚠️ CRITICAL CHECK:**
```bash
# Check for any remaining LoadBalancer services
kubectl get svc --all-namespaces | grep LoadBalancer

# Expected output: No resources found (or empty)
```

If you still see LoadBalancers:
```bash
# Force delete them
kubectl delete svc <service-name> -n <namespace> --grace-period=0 --force
```

### 1.6 Verify No Persistent Volumes Remain

```bash
# Check for PVCs
kubectl get pvc --all-namespaces

# If any exist, delete them
kubectl delete pvc --all --all-namespaces
```

---

## Step 2: Delete AWS Load Balancers Manually (5 min)

Even after deleting services, sometimes AWS LoadBalancers don't get cleaned up properly.

### 2.1 Check for Orphaned Load Balancers

```bash
# List all load balancers
aws elbv2 describe-load-balancers --region us-east-1

# Look for load balancers with tag:
# kubernetes.io/cluster/enterprise-eks-cluster = owned
```

### 2.2 Delete Orphaned Load Balancers (if any)

```bash
# Get load balancer ARN
aws elbv2 describe-load-balancers --region us-east-1 \
  --query "LoadBalancers[?contains(LoadBalancerName, 'k8s')].LoadBalancerArn" \
  --output text

# Delete the load balancer
aws elbv2 delete-load-balancer --load-balancer-arn <ARN> --region us-east-1
```

### 2.3 Wait for Load Balancers to be Deleted

```bash
# Check status
aws elbv2 describe-load-balancers --region us-east-1 | grep -i k8s

# Wait until no results are returned
```

---

## Step 3: Delete EBS Volumes (2 min)

### 3.1 Find EBS Volumes

```bash
# List volumes tagged with the cluster
aws ec2 describe-volumes --region us-east-1 \
  --filters "Name=tag:kubernetes.io/cluster/enterprise-eks-cluster,Values=owned" \
  --query "Volumes[].VolumeId" --output text
```

### 3.2 Delete EBS Volumes

```bash
# For each volume ID, delete it
aws ec2 delete-volume --volume-id <volume-id> --region us-east-1

# If volume is in use, force detach first
aws ec2 detach-volume --volume-id <volume-id> --force --region us-east-1
# Then delete
aws ec2 delete-volume --volume-id <volume-id> --region us-east-1
```

---

## Step 4: Destroy Terraform Infrastructure (10 min)

### 4.1 Navigate to Terraform Directory

```bash
cd terraform
```

### 4.2 Review What Will Be Destroyed

```bash
terraform plan -destroy
```

**This will show:**
- EKS cluster deletion
- VPC deletion (subnets, route tables, internet gateway, NAT gateways)
- Security groups deletion
- IAM roles and policies deletion
- All networking components

### 4.3 Destroy Infrastructure

```bash
# Destroy everything
terraform destroy -auto-approve
```

**Expected output:**
```
...
Destroy complete! Resources: 45 destroyed.
```

### 4.4 Handle Errors (if any)

**Common Error: ENI still attached**
```bash
# Find and delete network interfaces
aws ec2 describe-network-interfaces --region us-east-1 \
  --filters "Name=vpc-id,Values=<vpc-id>" \
  --query "NetworkInterfaces[].NetworkInterfaceId" --output text

# Delete each ENI
aws ec2 delete-network-interface --network-interface-id <eni-id> --region us-east-1
```

**Common Error: Security group in use**
```bash
# List security groups
aws ec2 describe-security-groups --region us-east-1 \
  --filters "Name=vpc-id,Values=<vpc-id>"

# Delete security groups (delete dependent ones first)
aws ec2 delete-security-group --group-id <sg-id> --region us-east-1
```

### 4.5 Retry Terraform Destroy

```bash
# If errors occurred, retry
terraform destroy -auto-approve
```

---

## Step 5: Delete ECR Repositories (3 min)

### 5.1 List All Repositories

```bash
aws ecr describe-repositories --region us-east-1 \
  --query "repositories[?contains(repositoryName, 'enterprise-observability')].repositoryName" \
  --output text
```

### 5.2 Delete Each Repository

```bash
# Delete all images and repository
aws ecr delete-repository \
  --repository-name enterprise-observability/gateway \
  --force \
  --region us-east-1

aws ecr delete-repository \
  --repository-name enterprise-observability/authentication-service \
  --force \
  --region us-east-1

aws ecr delete-repository \
  --repository-name enterprise-observability/employee-service \
  --force \
  --region us-east-1

aws ecr delete-repository \
  --repository-name enterprise-observability/leave-service \
  --force \
  --region us-east-1

aws ecr delete-repository \
  --repository-name enterprise-observability/payroll-service \
  --force \
  --region us-east-1

aws ecr delete-repository \
  --repository-name enterprise-observability/asset-service \
  --force \
  --region us-east-1

aws ecr delete-repository \
  --repository-name enterprise-observability/notification-service \
  --force \
  --region us-east-1

aws ecr delete-repository \
  --repository-name enterprise-observability/helpdesk-service \
  --force \
  --region us-east-1

aws ecr delete-repository \
  --repository-name enterprise-observability/frontend \
  --force \
  --region us-east-1
```

### 5.3 Verify Deletion

```bash
aws ecr describe-repositories --region us-east-1 | grep enterprise-observability
# Should return empty
```

---

## Step 6: Clean Up Local Files (1 min)

### 6.1 Remove Terraform State Files

```bash
cd terraform

# Remove state files (already in .gitignore)
rm -f terraform.tfstate
rm -f terraform.tfstate.backup
rm -f tfplan
rm -rf .terraform/
```

### 6.2 Remove kubectl Context

```bash
# Remove EKS cluster from kubeconfig
kubectl config delete-context arn:aws:eks:us-east-1:<account-id>:cluster/enterprise-eks-cluster

# Or reset kubeconfig
kubectl config use-context <previous-context>
```

---

## Step 7: Verify Complete Deletion (2 min)

### 7.1 Verify EKS Cluster is Gone

```bash
aws eks list-clusters --region us-east-1
# Should NOT show enterprise-eks-cluster
```

### 7.2 Verify VPC is Gone

```bash
aws ec2 describe-vpcs --region us-east-1 \
  --filters "Name=tag:Name,Values=enterprise-eks-vpc"
# Should return empty
```

### 7.3 Verify No EC2 Instances

```bash
aws ec2 describe-instances --region us-east-1 \
  --filters "Name=tag:kubernetes.io/cluster/enterprise-eks-cluster,Values=owned" \
  --query "Reservations[].Instances[].InstanceId"
# Should return empty
```

### 7.4 Verify No Load Balancers

```bash
aws elbv2 describe-load-balancers --region us-east-1 | grep k8s
# Should return empty
```

### 7.5 Verify No EBS Volumes

```bash
aws ec2 describe-volumes --region us-east-1 \
  --filters "Name=tag:kubernetes.io/cluster/enterprise-eks-cluster,Values=owned"
# Should return empty
```

---

## Step 8: Optional - Clean Up Dynatrace (1 min)

### 8.1 Delete Kubernetes Cluster from Dynatrace

1. Login to Dynatrace
2. Go to **Settings → Cloud and virtualization → Kubernetes**
3. Find `enterprise-eks-cluster`
4. Click **Delete**

### 8.2 Revoke API Tokens (Optional)

1. Go to **Settings → Integration → Dynatrace API**
2. Find tokens created for this project
3. **Revoke** them

---

## Automated Destruction Script

Save this as `destroy-all.sh`:

```bash
#!/bin/bash

set -e

echo "=========================================="
echo "⚠️  INFRASTRUCTURE DESTRUCTION SCRIPT"
echo "=========================================="
echo ""
echo "This will DELETE ALL resources!"
echo ""
read -p "Are you ABSOLUTELY sure? Type 'yes' to continue: " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Destruction cancelled."
    exit 0
fi

echo ""
echo "Step 1: Deleting Kubernetes namespaces..."
kubectl delete namespace enterprise-observability --wait=true || true
kubectl delete namespace dynatrace --wait=true || true
kubectl delete namespace flux-system --wait=true || true
kubectl delete namespace ingress-nginx --wait=true || true

echo ""
echo "Step 2: Waiting for LoadBalancers to be deleted..."
sleep 60

echo ""
echo "Step 3: Destroying Terraform infrastructure..."
cd terraform
terraform destroy -auto-approve

echo ""
echo "Step 4: Deleting ECR repositories..."
cd ..

REPOS=(
  "enterprise-observability/gateway"
  "enterprise-observability/authentication-service"
  "enterprise-observability/employee-service"
  "enterprise-observability/leave-service"
  "enterprise-observability/payroll-service"
  "enterprise-observability/asset-service"
  "enterprise-observability/notification-service"
  "enterprise-observability/helpdesk-service"
  "enterprise-observability/frontend"
)

for repo in "${REPOS[@]}"; do
  echo "Deleting $repo..."
  aws ecr delete-repository --repository-name "$repo" --force --region us-east-1 || true
done

echo ""
echo "Step 5: Verifying deletion..."
aws eks list-clusters --region us-east-1
aws ec2 describe-vpcs --region us-east-1 --filters "Name=tag:Name,Values=enterprise-eks-vpc"

echo ""
echo "=========================================="
echo "✅ Infrastructure destruction complete!"
echo "=========================================="
echo ""
echo "Cost savings: ~$284/month"
echo ""
```

**Make it executable and run:**
```bash
chmod +x destroy-all.sh
./destroy-all.sh
```

---

## Troubleshooting

### Issue: Terraform destroy fails with "resource still in use"

**Solution:**
```bash
# Find the resource
aws ec2 describe-<resource-type> --region us-east-1

# Delete it manually
aws ec2 delete-<resource-type> --<resource-id> <id> --region us-east-1

# Retry terraform destroy
terraform destroy -auto-approve
```

### Issue: LoadBalancer won't delete

**Solution:**
```bash
# Force delete the service
kubectl delete svc <service-name> -n <namespace> --grace-period=0 --force

# Wait 5 minutes, then check AWS console
aws elbv2 describe-load-balancers --region us-east-1

# If still there, delete via AWS CLI
aws elbv2 delete-load-balancer --load-balancer-arn <arn> --region us-east-1
```

### Issue: VPC deletion fails

**Solution:**
```bash
# Check for remaining ENIs
aws ec2 describe-network-interfaces --region us-east-1 \
  --filters "Name=vpc-id,Values=<vpc-id>"

# Delete each ENI
aws ec2 delete-network-interface --network-interface-id <eni-id> --region us-east-1

# Check for remaining security groups
aws ec2 describe-security-groups --region us-east-1 \
  --filters "Name=vpc-id,Values=<vpc-id>"

# Delete security groups (dependencies first)
aws ec2 delete-security-group --group-id <sg-id> --region us-east-1
```

### Issue: EKS cluster stuck in "DELETING" state

**Solution:**
```bash
# Check node groups
aws eks list-nodegroups --cluster-name enterprise-eks-cluster --region us-east-1

# If any exist, delete them
aws eks delete-nodegroup \
  --cluster-name enterprise-eks-cluster \
  --nodegroup-name <nodegroup-name> \
  --region us-east-1

# Wait 10 minutes, cluster should delete
```

---

## Post-Destruction Checklist

- [ ] EKS cluster deleted
- [ ] VPC and subnets deleted
- [ ] EC2 instances terminated
- [ ] Load Balancers deleted
- [ ] EBS volumes deleted
- [ ] ECR repositories deleted
- [ ] Security groups deleted
- [ ] IAM roles deleted (check manually)
- [ ] NAT gateways deleted
- [ ] Elastic IPs released
- [ ] CloudWatch log groups deleted (optional)
- [ ] AWS bill shows $0 for EKS/EC2

---

## Estimated Costs After Destruction

| Service | Cost |
|---------|------|
| S3 (Terraform state) | ~$0.01/month |
| ECR (if not deleted) | ~$0.10/month per GB |
| CloudWatch Logs | ~$0.50/month |
| **Total** | **< $1/month** |

---

## Recreation Time

If you need to recreate the infrastructure:

**From GitHub repository:**
```bash
git clone https://github.com/abhaysahu403/enterprise-observability-gitops.git
cd enterprise-observability-gitops

# Follow RUN_PROJECT.md
# Total time: 40-45 minutes
```

All your code, configurations, and documentation are safely stored in GitHub!

---

## Final Notes

- **Terraform state files** are NOT in GitHub (excluded by .gitignore)
- **You can always recreate** the infrastructure using `RUN_PROJECT.md`
- **All Docker images** are in ECR (or can be rebuilt)
- **All configurations** are in Git
- **Screenshots** are saved for documentation

**Your work is safe! You can destroy the infrastructure anytime without losing your project.**

---

## Quick Destruction (One Command)

If you're in a hurry:

```bash
# Delete everything
kubectl delete namespace enterprise-observability dynatrace flux-system ingress-nginx --wait=false
sleep 120
cd terraform && terraform destroy -auto-approve
cd .. && for repo in gateway authentication-service employee-service leave-service payroll-service asset-service notification-service helpdesk-service frontend; do aws ecr delete-repository --repository-name enterprise-observability/$repo --force --region us-east-1; done
```

**⚠️ Use with caution! This deletes everything immediately.**
