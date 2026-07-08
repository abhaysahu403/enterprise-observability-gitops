# FluxCD Quick Start Guide

## 🚀 Quick Setup (5 Minutes)

### Step 1: Install Flux CLI

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

### Step 2: Set Environment Variables

```bash
export GITHUB_TOKEN=<your-personal-access-token>
export GITHUB_USER=abhaysahu403
```

**Create GitHub Token:** https://github.com/settings/tokens
- Required scopes: `repo`, `workflow`, `admin:public_key`

### Step 3: Bootstrap FluxCD

```bash
cd /path/to/enterprise-observability-demo-up
chmod +x flux-bootstrap.sh
./flux-bootstrap.sh
```

**Or manually:**
```bash
flux bootstrap github \
  --owner=abhaysahu403 \
  --repository=enterprise-observability-gitops \
  --branch=main \
  --path=./k8s \
  --personal
```

### Step 4: Verify Installation

```bash
# Check Flux components
flux check

# View all resources
flux get all

# Check pods
kubectl get pods -n flux-system
```

---

## 📋 Daily Operations

### Make a Change (GitOps Style)

```bash
# 1. Edit manifest
vim k8s/gateway/deployment.yaml

# 2. Commit and push
git add k8s/gateway/deployment.yaml
git commit -m "Scale gateway to 3 replicas"
git push gitops main

# 3. Watch Flux apply automatically
flux reconcile kustomization flux-system --with-source
kubectl get pods -n enterprise-observability -w
```

### Force Sync from Git

```bash
flux reconcile source git flux-system
flux reconcile kustomization flux-system
```

### View Logs

```bash
flux logs --follow
```

### Rollback

```bash
# Option 1: Revert commit
git revert HEAD
git push gitops main

# Option 2: Reset to previous state
git reset --hard HEAD~1
git push gitops main --force
```

---

## 🎯 Common Commands

| Task | Command |
|------|---------|
| View all Flux resources | `flux get all` |
| Check Git sync status | `flux get sources git` |
| Check manifest apply status | `flux get kustomizations` |
| Force sync | `flux reconcile kustomization flux-system --with-source` |
| View logs | `flux logs --follow` |
| Suspend reconciliation | `flux suspend kustomization flux-system` |
| Resume reconciliation | `flux resume kustomization flux-system` |
| Watch pods | `kubectl get pods -n enterprise-observability -w` |
| View events | `kubectl get events -n flux-system --sort-by='.lastTimestamp'` |

---

## 🔥 GitOps Demo

Run the interactive demo:
```bash
chmod +x flux-demo.sh
./flux-demo.sh
```

The demo shows:
1. Current deployment state
2. GitOps-based scaling
3. Self-healing (automatic drift correction)
4. Rollback via Git history
5. Traffic generation for Dynatrace
6. Monitoring FluxCD operations

---

## 🐛 Troubleshooting

### Flux Not Syncing?

```bash
# Check status
flux get all

# Check logs for errors
flux logs --kind=Kustomization --name=flux-system

# Force reconcile
flux reconcile source git flux-system --with-source
```

### Authentication Issues?

```bash
# Check deploy key in GitHub
# Go to: Settings → Deploy Keys

# Recreate if needed
kubectl delete secret flux-system -n flux-system
flux bootstrap github --force ...
```

### Deployment Stuck?

```bash
# Check events
kubectl get events -n enterprise-observability --sort-by='.lastTimestamp'

# Check pod status
kubectl get pods -n enterprise-observability
kubectl describe pod <pod-name> -n enterprise-observability
```

---

## 📚 Full Documentation

For detailed information, see:
- **[FLUXCD_GUIDE.md](docs/FLUXCD_GUIDE.md)** - Complete FluxCD guide
- **[DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)** - Infrastructure setup
- **[README.md](README.md)** - Project overview

---

## 🎓 GitOps Principles

1. **Git is the source of truth** - All changes go through Git
2. **No manual kubectl apply** - Let Flux manage deployments
3. **Declarative** - Describe desired state, not commands
4. **Automated** - Flux continuously syncs Git → Cluster
5. **Auditable** - Full change history in Git

---

## ✅ Success Checklist

- [ ] Flux CLI installed
- [ ] GitHub token created and exported
- [ ] Flux bootstrapped successfully
- [ ] `flux check` passes
- [ ] `flux get all` shows Ready=True
- [ ] Pods running in `flux-system` namespace
- [ ] Can make a change and see Flux apply it
- [ ] Dynatrace shows deployment events

---

## 🔗 Quick Links

- **GitHub Repository:** https://github.com/abhaysahu403/enterprise-observability-gitops
- **FluxCD Docs:** https://fluxcd.io/docs/
- **Dynatrace Dashboard:** (Your Dynatrace URL)
- **EKS Cluster:** `enterprise-eks-cluster`

---

## 💡 Pro Tips

1. **Use branches** for testing changes before merging to main
2. **Enable notifications** to Slack for Flux events
3. **Monitor Flux logs** during deployments
4. **Use Git tags** for release tracking
5. **Test rollbacks** in staging first

---

## 🆘 Need Help?

```bash
# Check Flux documentation
flux --help

# Check specific command
flux reconcile --help

# View Flux system status
flux check --verbose

# Export current config
flux export kustomization flux-system
```

---

**That's it! You now have GitOps enabled for your Enterprise Observability Platform! 🎉**
