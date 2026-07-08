# FluxCD GitOps Setup Instructions

## 📦 What You Have

In the `gitops-flux/` directory, you have:
- `.gitignore` - Proper gitignore for secrets and sensitive files
- `README.md` - Complete documentation for the GitOps repository
- `SETUP_INSTRUCTIONS.md` - This file

## 🚀 Step-by-Step Setup

### Step 1: Copy Files to Your GitOps Repository

```bash
# Navigate to your local clone of the GitOps repo
cd /path/to/enterprise-observability-gitops

# Copy the files from gitops-flux directory
cp ../enterprise-observability-demo-up/gitops-flux/.gitignore .
cp ../enterprise-observability-demo-up/gitops-flux/README.md .

# Commit and push
git add .
git commit -m "Add GitOps documentation and gitignore"
git push origin main
```

### Step 2: Create Directory Structure

```bash
# Create directory structure
mkdir -p clusters/production/{flux-system,infrastructure,apps}
mkdir -p apps/{gateway,authentication,employee,leave,payroll,asset,helpdesk,notification,frontend}
mkdir -p infrastructure/{namespace,configmap,databases/{postgres,redis}}
mkdir -p scripts
```

### Step 3: Copy Kubernetes Manifests

```bash
# From your main project, copy all k8s manifests
cd ../enterprise-observability-demo-up

# Copy deployments and services for each app
for service in gateway authentication-service employee-service leave-service payroll-service asset-service helpdesk-service notification-service frontend; do
  app_name=$(echo $service | sed 's/-service//')
  mkdir -p ../enterprise-observability-gitops/apps/$app_name
  
  if [ -f "k8s/$service/deployment.yaml" ]; then
    cp k8s/$service/deployment.yaml ../enterprise-observability-gitops/apps/$app_name/
  fi
  
  if [ -f "k8s/$service/service.yaml" ]; then
    cp k8s/$service/service.yaml ../enterprise-observability-gitops/apps/$app_name/
  fi
done

# Copy infrastructure
cp k8s/configmap.yaml ../enterprise-observability-gitops/infrastructure/configmap/
cp k8s/postgres/*.yaml ../enterprise-observability-gitops/infrastructure/databases/postgres/
cp k8s/redis/*.yaml ../enterprise-observability-gitops/infrastructure/databases/redis/
```

### Step 4: Install Flux CLI

**On Windows:**
```powershell
choco install flux
```

**On Mac:**
```bash
brew install fluxcd/tap/flux
```

**On Linux:**
```bash
curl -s https://fluxcd.io/install.sh | sudo bash
```

**Verify:**
```bash
flux --version
```

### Step 5: Set Environment Variables

```bash
export GITHUB_TOKEN=<your-personal-access-token>
export GITHUB_USER=abhaysahu403
```

### Step 6: Bootstrap Flux

```bash
# Make sure kubectl is configured for your EKS cluster
kubectl config current-context

# Bootstrap Flux
flux bootstrap github \
  --owner=$GITHUB_USER \
  --repository=enterprise-observability-gitops \
  --branch=main \
  --path=clusters/production \
  --personal
```

This will:
- Install Flux controllers in `flux-system` namespace
- Create deploy key in your GitHub repo
- Create `clusters/production/flux-system/` directory with Flux manifests
- Start watching the repository

### Step 7: Create Flux Kustomizations

I'll provide these files in the next message since disk space is limited. You'll need to create:

1. `clusters/production/apps/kustomization.yaml` - References all apps
2. `apps/gateway/kustomization.yaml` - Gateway app kustomization
3. Similar kustomization.yaml for each service
4. Flux GitRepository and Kustomization resources

### Step 8: Commit and Push

```bash
cd ../enterprise-observability-gitops
git add .
git commit -m "Add FluxCD GitOps configuration"
git push origin main
```

### Step 9: Verify Deployment

```bash
# Watch Flux sync
flux get kustomizations --watch

# Check if apps are deployed
kubectl get pods -n enterprise-observability

# Check Flux logs
flux logs --all-namespaces --follow
```

---

## 🎯 Quick Reference Commands

```bash
# Force sync immediately
flux reconcile kustomization flux-system --with-source

# Check status
flux get all

# View logs
flux logs --all-namespaces | grep -i error

# Suspend/Resume
flux suspend kustomization apps
flux resume kustomization apps
```

---

## ⚠️ Important Notes

1. **Disk Space**: Due to limited disk space on your Windows machine, I'm creating files incrementally
2. **Manual Steps**: You'll need to manually copy manifests from k8s/ to apps/
3. **GitHub Token**: Keep your token secure, never commit it
4. **Testing**: Test each kustomization locally before committing:
   ```bash
   kubectl apply --dry-run=client -k apps/gateway/
   ```

---

## 📋 Next Steps

Once basic structure is in place, I'll provide:
1. All Flux Kustomization resources
2. GitRepository resources
3. Helper scripts for common operations
4. Demo scripts for showing GitOps in action
