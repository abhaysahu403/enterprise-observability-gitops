# Enterprise Observability - GitOps with FluxCD

This repository contains the GitOps configuration for the Enterprise Observability Platform running on AWS EKS with Dynatrace monitoring.

![FluxCD](https://img.shields.io/badge/FluxCD-v2.x-blue)
![Kubernetes](https://img.shields.io/badge/Kubernetes-1.28+-blue)
![GitOps](https://img.shields.io/badge/GitOps-Enabled-green)

---

## 🎯 What is GitOps?

GitOps is a way of managing Kubernetes deployments where:
- **Git is the single source of truth** for declarative infrastructure and applications
- **Automated delivery** pipelines roll out changes to your infrastructure
- **Pull-based deployment** where agents pull changes from Git (not push from CI)
- **Self-healing** where the cluster state matches Git automatically

---

## 🏗️ Repository Structure

```
enterprise-observability-gitops/
├── clusters/
│   └── production/              # Production cluster config
│       ├── flux-system/         # Flux controllers (auto-generated)
│       ├── infrastructure/      # Infrastructure resources
│       │   ├── namespace.yaml
│       │   ├── configmap.yaml
│       │   ├── postgres.yaml
│       │   └── redis.yaml
│       └── apps/                # Application deployments
│           ├── kustomization.yaml
│           ├── gateway.yaml
│           ├── authentication.yaml
│           ├── employee.yaml
│           ├── leave.yaml
│           ├── payroll.yaml
│           ├── asset.yaml
│           ├── helpdesk.yaml
│           ├── notification.yaml
│           └── frontend.yaml
├── apps/                        # Application manifests
│   ├── gateway/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── kustomization.yaml
│   ├── authentication/
│   └── ... (all services)
├── infrastructure/              # Infrastructure manifests
│   ├── namespace/
│   ├── configmap/
│   └── databases/
└── scripts/                     # Helper scripts
    ├── bootstrap-flux.sh
    ├── demo-scale.sh
    └── demo-update.sh
```

---

## 🚀 Quick Start

### Prerequisites

- AWS EKS cluster running (`enterprise-eks-cluster`)
- kubectl configured to access the cluster
- GitHub Personal Access Token with `repo`, `workflow`, `admin:public_key` scopes
- Flux CLI installed

### 1. Install Flux CLI

**Mac:**
```bash
brew install fluxcd/tap/flux
```

**Linux:**
```bash
curl -s https://fluxcd.io/install.sh | sudo bash
```

**Windows:**
```powershell
choco install flux
```

**Verify installation:**
```bash
flux --version
```

### 2. Verify Cluster Access

```bash
# Check current context
kubectl config current-context
# Should output: arn:aws:eks:us-east-1:...:cluster/enterprise-eks-cluster

# Check nodes
kubectl get nodes
# Should show 3 nodes in Ready state
```

### 3. Export GitHub Credentials

```bash
export GITHUB_TOKEN=<your-personal-access-token>
export GITHUB_USER=abhaysahu403
```

### 4. Bootstrap Flux

```bash
flux bootstrap github \
  --owner=$GITHUB_USER \
  --repository=enterprise-observability-gitops \
  --branch=main \
  --path=clusters/production \
  --personal
```

This command:
- ✅ Installs Flux controllers in your cluster
- ✅ Creates deploy keys for the repository
- ✅ Commits Flux manifests to `clusters/production/flux-system/`
- ✅ Configures Flux to watch this repository

**Expected output:**
```
► connecting to github.com
✔ repository "https://github.com/abhaysahu403/enterprise-observability-gitops" created
► cloning branch "main" from Git repository "https://github.com/abhaysahu403/enterprise-observability-gitops.git"
✔ cloned repository
► generating component manifests
✔ generated component manifests
► installing components in "flux-system" namespace
✔ installed components
✔ reconciled components
► determining if source secret "flux-system/flux-system" exists
► generating source secret
✔ reconciled source secret
► applying source secret "flux-system/flux-system"
✔ reconciled source configuration
✔ bootstrap finished
```

### 5. Verify Flux Installation

```bash
# Check Flux pods
kubectl get pods -n flux-system

# Expected output:
# NAME                                      READY   STATUS
# source-controller-xxx                     1/1     Running
# kustomize-controller-xxx                  1/1     Running
# helm-controller-xxx                       1/1     Running
# notification-controller-xxx               1/1     Running

# Check Flux sources
flux get sources git

# Check Flux kustomizations
flux get kustomizations
```

---

## 📋 GitOps Workflow

### How Flux Works

```
┌─────────────┐
│   GitHub    │
│  Repository │
└──────┬──────┘
       │
       │ 1. Flux pulls changes every 1 minute
       │
       ▼
┌─────────────┐
│    Flux     │
│ Controllers │
└──────┬──────┘
       │
       │ 2. Flux applies changes to cluster
       │
       ▼
┌─────────────┐
│  EKS Cluster│
│  Resources  │
└─────────────┘
```

### Making Changes

**Option 1: GitHub UI (Easiest for demos)**
1. Navigate to repository on GitHub
2. Click on file to edit (e.g., `apps/gateway/deployment.yaml`)
3. Click "Edit" (pencil icon)
4. Make changes (e.g., change replicas: 1 → 3)
5. Commit directly to main branch
6. Wait ~60 seconds
7. Verify with: `kubectl get pods -n enterprise-observability -w`

**Option 2: Local Git (For bulk changes)**
```bash
# Clone repository
git clone https://github.com/abhaysahu403/enterprise-observability-gitops.git
cd enterprise-observability-gitops

# Make changes
vim apps/gateway/deployment.yaml
# Change replicas: 1 → 3

# Commit and push
git add .
git commit -m "Scale gateway to 3 replicas"
git push

# Watch Flux sync
flux get kustomizations --watch

# Watch pods update
kubectl get pods -n enterprise-observability -w
```

---

## 🎬 Demo Scenarios

### Demo 1: Automatic Scaling

**Objective:** Show GitOps auto-scaling without kubectl

**Steps:**
```bash
# 1. Show current state
kubectl get deployment gateway -n enterprise-observability
# DESIRED: 1  CURRENT: 1  READY: 1

# 2. Edit in GitHub
# Navigate to: apps/gateway/deployment.yaml
# Change: replicas: 1 → replicas: 3
# Commit to main branch

# 3. Watch Flux detect and apply
flux get kustomizations --watch
# Wait for "applied revision" message

# 4. Verify scaling happened automatically
kubectl get deployment gateway -n enterprise-observability
# DESIRED: 3  CURRENT: 3  READY: 3

# 5. No kubectl apply was executed!
```

**Time:** ~90 seconds from commit to fully scaled

### Demo 2: Image Update (Rolling Update)

**Objective:** Show zero-downtime deployment via GitOps

**Steps:**
```bash
# 1. Show current image
kubectl get deployment gateway -n enterprise-observability -o jsonpath='{.spec.template.spec.containers[0].image}'
# Output: ...gateway:v1.0.0

# 2. Edit in GitHub
# Navigate to: apps/gateway/deployment.yaml
# Change: image: ...gateway:v1.0.0 → ...gateway:v1.0.1
# Commit to main

# 3. Watch rolling update
kubectl rollout status deployment/gateway -n enterprise-observability

# 4. Verify new image
kubectl get deployment gateway -n enterprise-observability -o jsonpath='{.spec.template.spec.containers[0].image}'
# Output: ...gateway:v1.0.1
```

**Time:** ~2-3 minutes for rolling update

### Demo 3: Self-Healing (Drift Detection)

**Objective:** Show Flux automatically fixing manual changes

**Steps:**
```bash
# 1. Manually delete deployment (simulate accident)
kubectl delete deployment gateway -n enterprise-observability
# deployment.apps "gateway" deleted

# 2. Show it's gone
kubectl get deployment gateway -n enterprise-observability
# Error from server (NotFound): deployments.apps "gateway" not found

# 3. Wait ~60 seconds for Flux to detect drift

# 4. Flux automatically recreates it
kubectl get deployment gateway -n enterprise-observability
# NAME      READY   UP-TO-DATE   AVAILABLE   AGE
# gateway   1/1     1            1           30s

# 5. Self-healing complete!
```

**Time:** ~60-90 seconds for self-healing

### Demo 4: Rollback

**Objective:** Show instant rollback with git revert

**Steps:**
```bash
# 1. Make a breaking change
# Edit apps/gateway/deployment.yaml
# Change image to invalid tag: ...gateway:v999.broken
# Commit to main

# 2. Watch deployment fail
kubectl get pods -n enterprise-observability | grep gateway
# gateway-xxx   0/1     ImagePullBackOff

# 3. Rollback with git
git log --oneline
# Get commit hash before breaking change
git revert <commit-hash>
git push

# 4. Flux automatically applies rollback
kubectl get pods -n enterprise-observability | grep gateway
# gateway-xxx   1/1     Running

# 5. Service restored!
```

**Time:** ~90 seconds from git revert to healthy

---

## 🔧 Common Operations

### Check Flux Status
```bash
# Overall status
flux check

# Git sources
flux get sources git

# Kustomizations
flux get kustomizations

# Helm releases
flux get helmreleases

# All resources
flux get all
```

### Force Reconciliation
```bash
# Force immediate sync (don't wait 1 minute)
flux reconcile kustomization flux-system --with-source

# Force specific app
flux reconcile kustomization apps
```

### View Logs
```bash
# Source controller logs
kubectl logs -n flux-system deployment/source-controller -f

# Kustomize controller logs
kubectl logs -n flux-system deployment/kustomize-controller -f

# All Flux logs
flux logs --all-namespaces --follow
```

### Suspend/Resume
```bash
# Suspend reconciliation (for maintenance)
flux suspend kustomization apps

# Resume reconciliation
flux resume kustomization apps
```

---

## 🚨 Troubleshooting

### Flux Not Syncing

**Problem:** Changes committed but not applied

**Check:**
```bash
# 1. Verify Flux is running
kubectl get pods -n flux-system

# 2. Check source status
flux get sources git
# Should show: "stored artifact for revision 'main@sha1:...'"

# 3. Check kustomization status
flux get kustomizations
# Should show: "Applied revision: main@sha1:..."

# 4. Check for errors
flux logs --all-namespaces --level=error
```

**Common causes:**
- Invalid YAML syntax
- Missing kustomization.yaml
- Resource conflicts
- Namespace doesn't exist

**Solution:**
```bash
# Validate locally first
kubectl apply --dry-run=client -f apps/gateway/

# Force reconciliation
flux reconcile kustomization apps --with-source
```

### Authentication Issues

**Problem:** Flux can't access repository

**Check:**
```bash
# Verify deploy key
flux get sources git

# Check secret
kubectl get secret -n flux-system flux-system -o yaml
```

**Solution:**
```bash
# Re-bootstrap
flux bootstrap github \
  --owner=$GITHUB_USER \
  --repository=enterprise-observability-gitops \
  --branch=main \
  --path=clusters/production \
  --personal
```

### Resource Not Created

**Problem:** Manifest committed but resource not in cluster

**Check:**
```bash
# 1. Check if kustomization references it
cat clusters/production/apps/kustomization.yaml

# 2. Check Flux logs
flux logs --all-namespaces | grep -i error

# 3. Validate manifest
kubectl apply --dry-run=client -f apps/gateway/deployment.yaml
```

---

## 📊 Monitoring Flux

### Health Checks
```bash
# Check Flux components
flux check

# Get status of all resources
flux get all

# Watch for changes
flux get kustomizations --watch
```

### Notifications (Optional)

Configure Slack/Discord/etc notifications:

```yaml
# clusters/production/flux-system/notification.yaml
apiVersion: notification.toolkit.fluxcd.io/v1beta1
kind: Alert
metadata:
  name: slack-alert
  namespace: flux-system
spec:
  providerRef:
    name: slack
  eventSeverity: info
  eventSources:
    - kind: Kustomization
      name: '*'
```

---

## 🔐 Security Best Practices

1. **Never commit secrets to Git**
   - Use Sealed Secrets or External Secrets Operator
   - Keep `.gitignore` comprehensive

2. **Use branch protection**
   - Require PR reviews for main branch
   - Enable status checks

3. **Enable GitHub deploy keys**
   - Flux uses read-only deploy keys
   - More secure than personal tokens

4. **Implement RBAC**
   - Limit Flux service account permissions
   - Use separate service accounts per namespace

5. **Enable webhook receivers**
   - Faster sync on commit (instead of 1min polling)
   - Reduces Git API calls

---

## 📚 Additional Resources

- **Flux Documentation**: https://fluxcd.io/docs/
- **GitOps Principles**: https://opengitops.dev/
- **Kustomize Guide**: https://kustomize.io/
- **Main Application Repo**: https://github.com/abhaysahu403/enterprise-observability-demo-up

---

## 🤝 Contributing

To add new applications or modify existing ones:

1. Fork this repository
2. Create a feature branch
3. Add/modify manifests in `apps/` directory
4. Add reference in `clusters/production/apps/kustomization.yaml`
5. Test locally with `kubectl apply --dry-run=client -k apps/your-app/`
6. Create pull request
7. After merge, Flux automatically deploys

---

## 📝 License

MIT License - See LICENSE file for details

---

## 💬 Support

For issues or questions:
- Create an issue in this repository
- Check Flux logs: `flux logs --all-namespaces`
- Review Flux documentation
- Check Kubernetes events: `kubectl get events -n enterprise-observability`
