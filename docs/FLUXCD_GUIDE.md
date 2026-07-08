# FluxCD GitOps Implementation Guide

## Overview

This guide covers the FluxCD GitOps setup for the Enterprise Observability Platform. FluxCD automatically syncs Kubernetes manifests from Git to your EKS cluster, enabling true GitOps workflows.

## Architecture

```
GitHub Repository (Source of Truth)
        ↓
    FluxCD Controllers
    ├── Source Controller (Git sync)
    ├── Kustomize Controller (Apply manifests)
    ├── Helm Controller (Helm releases)
    └── Notification Controller (Alerts)
        ↓
    AWS EKS Cluster
        ↓
    Enterprise Observability Platform
```

## Prerequisites

### 1. Install Flux CLI

**MacOS:**
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

Verify installation:
```bash
flux --version
```

### 2. GitHub Personal Access Token

Create a token at: https://github.com/settings/tokens

Required permissions:
- ✅ `repo` (all)
- ✅ `workflow`
- ✅ `admin:public_key`

Export the token:
```bash
export GITHUB_TOKEN=<your-token>
export GITHUB_USER=abhaysahu403
```

### 3. Kubernetes Cluster Access

Verify you can access your EKS cluster:
```bash
kubectl config current-context
kubectl get nodes
```

## Installation

### Option 1: Automated Bootstrap (Recommended)

Run the bootstrap script:
```bash
cd /path/to/enterprise-observability-demo-up
chmod +x flux-bootstrap.sh
./flux-bootstrap.sh
```

### Option 2: Manual Bootstrap

```bash
flux bootstrap github \
  --owner=abhaysahu403 \
  --repository=enterprise-observability-gitops \
  --branch=main \
  --path=./k8s \
  --personal \
  --components-extra=image-reflector-controller,image-automation-controller
```

### What Happens During Bootstrap?

1. **Flux installs itself** into the `flux-system` namespace
2. **Creates a deploy key** in your GitHub repository
3. **Installs controllers:**
   - `source-controller` - Fetches artifacts from Git
   - `kustomize-controller` - Applies Kustomize manifests
   - `helm-controller` - Manages Helm releases
   - `notification-controller` - Sends alerts
   - `image-reflector-controller` - Scans container registries
   - `image-automation-controller` - Updates image tags

4. **Creates GitRepository** resource pointing to your repo
5. **Creates Kustomization** resource that applies manifests from `./k8s`

## Verification

### Check Flux System

```bash
# Check Flux components
flux check

# View all Flux resources
flux get all

# Check pods
kubectl get pods -n flux-system
```

Expected output:
```
NAME                                       READY   STATUS
source-controller-xxx                      1/1     Running
kustomize-controller-xxx                   1/1     Running
helm-controller-xxx                        1/1     Running
notification-controller-xxx                1/1     Running
image-reflector-controller-xxx             1/1     Running
image-automation-controller-xxx            1/1     Running
```

### Check GitRepository Status

```bash
flux get sources git
```

Expected output:
```
NAME        READY   MESSAGE
flux-system True    stored artifact for revision 'main@sha1:xxxxx'
```

### Check Kustomization Status

```bash
flux get kustomizations
```

Expected output:
```
NAME        READY   MESSAGE
flux-system True    Applied revision: main@sha1:xxxxx
```

## GitOps Workflow

### How It Works

1. **You edit** Kubernetes manifests in the `k8s/` directory
2. **Commit and push** to GitHub
3. **Flux detects** the change (polls every 1 minute by default)
4. **Flux applies** changes to the cluster automatically
5. **Dynatrace monitors** the deployment via OpenTelemetry

### Example: Scale Gateway Service

#### 1. Edit the Deployment

```bash
# Edit k8s/gateway/deployment.yaml
vim k8s/gateway/deployment.yaml
```

Change:
```yaml
spec:
  replicas: 1  # Change this
```

To:
```yaml
spec:
  replicas: 3  # New value
```

#### 2. Commit and Push

```bash
git add k8s/gateway/deployment.yaml
git commit -m "Scale gateway to 3 replicas"
git push gitops main
```

#### 3. Watch Flux Sync

```bash
# Force immediate sync (optional)
flux reconcile source git flux-system
flux reconcile kustomization flux-system

# Watch pods scale
kubectl get pods -n enterprise-observability -w
```

#### 4. Verify in Dynatrace

- Open Dynatrace dashboard
- Navigate to Kubernetes → Workloads → gateway
- See replica count change from 1 → 3
- View deployment events in Davis AI

### Self-Healing Demo

Flux automatically reverts manual changes that aren't in Git:

```bash
# Manually scale (outside of Git)
kubectl scale deployment gateway -n enterprise-observability --replicas=5

# Watch Flux revert it back to Git state (3 replicas)
watch kubectl get deployment gateway -n enterprise-observability
```

Within 1 minute, Flux will detect the drift and revert to the Git-defined state.

### Rollback Using Git

To rollback a deployment:

```bash
# Option 1: Revert specific commit
git log --oneline k8s/gateway/deployment.yaml
git revert <commit-hash>
git push gitops main

# Option 2: Reset to previous commit
git reset --hard HEAD~1
git push gitops main --force

# Flux automatically applies the rollback
```

## Repository Structure

```
enterprise-observability-gitops/
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secrets.yaml
│   ├── gateway/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── auth-service/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── employee-service/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── fluxcd/
│   │   ├── gotk-components.yaml
│   │   ├── gotk-sync.yaml
│   │   └── kustomization.yaml
│   └── ...
├── terraform/
├── frontend/
├── gateway/
└── docs/
```

## Flux Configuration

### GitRepository Resource

Located at: `k8s/fluxcd/gotk-sync.yaml`

```yaml
apiVersion: source.toolkit.fluxcd.io/v1
kind: GitRepository
metadata:
  name: flux-system
  namespace: flux-system
spec:
  interval: 1m  # Check for changes every 1 minute
  ref:
    branch: main
  url: https://github.com/abhaysahu403/enterprise-observability-gitops
```

### Kustomization Resource

```yaml
apiVersion: kustomize.toolkit.fluxcd.io/v1
kind: Kustomization
metadata:
  name: enterprise-observability
  namespace: flux-system
spec:
  interval: 10m  # Reconcile every 10 minutes
  path: ./k8s    # Path to manifests
  prune: true    # Delete resources removed from Git
  sourceRef:
    kind: GitRepository
    name: flux-system
  targetNamespace: enterprise-observability
```

## Monitoring FluxCD

### View Flux Logs

```bash
# All Flux logs
flux logs --follow

# Specific controller
flux logs --kind=Kustomization --name=flux-system
```

### Check Reconciliation

```bash
# Last reconciliation time
flux get kustomizations

# Force reconciliation
flux reconcile kustomization flux-system --with-source
```

### View Events

```bash
# Flux system events
kubectl get events -n flux-system --sort-by='.lastTimestamp'

# Application events
kubectl get events -n enterprise-observability --sort-by='.lastTimestamp'
```

## Demo Scenarios

Run the interactive demo:
```bash
chmod +x flux-demo.sh
./flux-demo.sh
```

The demo shows:
1. ✅ Current deployment state
2. ✅ GitOps-based scaling
3. ✅ Self-healing (drift correction)
4. ✅ Rollback via Git history
5. ✅ Traffic generation for observability
6. ✅ Monitoring FluxCD operations

## Integration with Dynatrace

### OpenTelemetry + FluxCD

When Flux deploys changes:
1. **Deployment events** are captured by Dynatrace
2. **OpenTelemetry traces** show request flow
3. **Davis AI** correlates deployment with issues
4. **Slack alerts** notify team of problems

### Dynatrace Dashboard

View FluxCD deployments in Dynatrace:
- Navigate to: **Kubernetes → Workloads**
- Select: **enterprise-observability** namespace
- See: Deployment history, resource usage, events

### Davis AI Problem Detection

Davis AI automatically detects:
- ❌ Failed deployments (ImagePullBackOff)
- ❌ Increased error rates after deployment
- ❌ Resource exhaustion
- ❌ Service degradation

Davis AI **does NOT** alert on:
- ✅ Intentional scaling to 0 replicas
- ✅ Planned maintenance
- ✅ Manual pod deletions

## Troubleshooting

### Flux Not Syncing

```bash
# Check Flux system status
flux check

# Check Git repository
flux get sources git

# Check for reconciliation errors
flux logs --kind=Kustomization --name=flux-system
```

### Authentication Issues

```bash
# Verify deploy key in GitHub
# Settings → Deploy Keys → flux-system

# Recreate secret if needed
kubectl delete secret flux-system -n flux-system
flux bootstrap github --force ...
```

### Reconciliation Stuck

```bash
# Suspend reconciliation
flux suspend kustomization flux-system

# Fix issues in Git

# Resume reconciliation
flux resume kustomization flux-system
```

### View Detailed Status

```bash
# Describe GitRepository
kubectl describe gitrepository flux-system -n flux-system

# Describe Kustomization
kubectl describe kustomization flux-system -n flux-system
```

## Useful Commands

### Flux Operations

```bash
# View all Flux resources
flux get all

# Force sync from Git
flux reconcile source git flux-system

# Force apply manifests
flux reconcile kustomization flux-system

# View logs
flux logs --follow

# Suspend/resume
flux suspend kustomization flux-system
flux resume kustomization flux-system

# Export current configuration
flux export source git flux-system
flux export kustomization flux-system
```

### Kubernetes Operations

```bash
# Watch deployments
kubectl get deployments -n enterprise-observability -w

# Watch pods
kubectl get pods -n enterprise-observability -w

# View events
kubectl get events -n enterprise-observability --sort-by='.lastTimestamp'

# View logs
kubectl logs -n enterprise-observability deployment/gateway -f
```

## Advanced Features

### Image Automation

Automatically update image tags when new images are pushed:

```yaml
apiVersion: image.toolkit.fluxcd.io/v1beta1
kind: ImageRepository
metadata:
  name: gateway
  namespace: flux-system
spec:
  image: <account-id>.dkr.ecr.us-east-1.amazonaws.com/gateway
  interval: 1m

---
apiVersion: image.toolkit.fluxcd.io/v1beta1
kind: ImagePolicy
metadata:
  name: gateway
  namespace: flux-system
spec:
  imageRepositoryRef:
    name: gateway
  policy:
    semver:
      range: '>=1.0.0'

---
apiVersion: image.toolkit.fluxcd.io/v1beta1
kind: ImageUpdateAutomation
metadata:
  name: flux-system
  namespace: flux-system
spec:
  git:
    commit:
      author:
        email: fluxcdbot@users.noreply.github.com
        name: fluxcdbot
  sourceRef:
    kind: GitRepository
    name: flux-system
  update:
    path: ./k8s
    strategy: Setters
```

### Notifications

Send alerts to Slack:

```yaml
apiVersion: notification.toolkit.fluxcd.io/v1beta1
kind: Provider
metadata:
  name: slack
  namespace: flux-system
spec:
  type: slack
  channel: devops-alerts
  secretRef:
    name: slack-webhook

---
apiVersion: notification.toolkit.fluxcd.io/v1beta1
kind: Alert
metadata:
  name: flux-system
  namespace: flux-system
spec:
  providerRef:
    name: slack
  eventSeverity: info
  eventSources:
    - kind: GitRepository
      name: flux-system
    - kind: Kustomization
      name: flux-system
```

## Best Practices

### 1. Git as Single Source of Truth
- ✅ All changes go through Git
- ❌ Never `kubectl apply` directly in production
- ✅ Use branches and PRs for changes

### 2. Environment Separation
```
├── k8s/
│   ├── base/           # Common resources
│   ├── dev/            # Dev overrides
│   ├── staging/        # Staging overrides
│   └── production/     # Production overrides
```

### 3. Security
- 🔒 Use encrypted secrets (SealedSecrets or SOPS)
- 🔒 Limit Flux permissions via RBAC
- 🔒 Use separate GitHub deploy keys per environment

### 4. Monitoring
- 📊 Monitor Flux system metrics
- 📊 Alert on reconciliation failures
- 📊 Track deployment frequency

### 5. Testing
- ✅ Test changes in dev/staging first
- ✅ Use GitOps for rollback (don't panic revert)
- ✅ Validate manifests before commit

## Comparison: FluxCD vs ArgoCD

| Feature | FluxCD | ArgoCD |
|---------|--------|--------|
| **UI** | CLI-based | Rich Web UI |
| **Sync** | Pull-based | Pull-based |
| **Multi-Tenancy** | Via namespaces | Built-in |
| **Helm Support** | ✅ Native | ✅ Native |
| **Kustomize** | ✅ Native | ✅ Native |
| **Image Automation** | ✅ Built-in | ❌ Requires external tool |
| **GitOps** | ✅ Yes | ✅ Yes |
| **Complexity** | Lower | Higher |

**Choose FluxCD if:**
- You prefer CLI over UI
- You want image automation built-in
- You need lightweight solution

**Choose ArgoCD if:**
- You prefer web UI
- You need multi-tenancy features
- You want visualization

## Summary

FluxCD provides:
- ✅ **GitOps automation** - Changes in Git automatically applied
- ✅ **Self-healing** - Drift detection and correction
- ✅ **Audit trail** - Full history in Git
- ✅ **Rollback** - Simple Git revert
- ✅ **Observability** - Integration with Dynatrace

The combination of FluxCD + OpenTelemetry + Dynatrace provides complete observability into deployments and their impact on system health.

## Next Steps

1. ✅ Install Flux using bootstrap script
2. ✅ Run the GitOps demo
3. ✅ Make a change and watch it deploy
4. ✅ Verify in Dynatrace dashboard
5. ✅ Set up Slack notifications
6. ✅ Configure image automation

## References

- [FluxCD Official Docs](https://fluxcd.io/docs/)
- [GitOps Principles](https://www.gitops.tech/)
- [Dynatrace Kubernetes Monitoring](https://www.dynatrace.com/technologies/kubernetes-monitoring/)
