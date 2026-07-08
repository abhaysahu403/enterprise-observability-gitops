#!/bin/bash

# FluxCD Bootstrap Script for Enterprise Observability Platform
# This script installs FluxCD and configures it to manage your Kubernetes deployments

set -e

echo "=========================================="
echo "FluxCD Bootstrap for Enterprise Platform"
echo "=========================================="
echo ""

# Check prerequisites
echo "🔍 Checking prerequisites..."

if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl not found. Please install kubectl first."
    exit 1
fi

if ! command -v flux &> /dev/null; then
    echo "❌ flux CLI not found. Installing Flux CLI..."
    echo ""
    echo "Run one of these based on your OS:"
    echo "  MacOS:   brew install fluxcd/tap/flux"
    echo "  Linux:   curl -s https://fluxcd.io/install.sh | sudo bash"
    echo "  Windows: choco install flux"
    echo ""
    exit 1
fi

echo "✅ kubectl found: $(kubectl version --client --short)"
echo "✅ flux found: $(flux --version)"
echo ""

# Verify cluster connectivity
echo "🔍 Verifying Kubernetes cluster connection..."
CONTEXT=$(kubectl config current-context)
echo "Current context: $CONTEXT"

if ! kubectl get nodes &> /dev/null; then
    echo "❌ Cannot connect to Kubernetes cluster. Please check your kubeconfig."
    exit 1
fi

echo "✅ Connected to cluster"
kubectl get nodes
echo ""

# Check if GitHub token is set
if [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ GITHUB_TOKEN environment variable not set."
    echo ""
    echo "Please create a GitHub Personal Access Token with these permissions:"
    echo "  ✓ repo (all)"
    echo "  ✓ workflow"
    echo "  ✓ admin:public_key"
    echo ""
    echo "Then export it:"
    echo "  export GITHUB_TOKEN=<your-token>"
    echo ""
    exit 1
fi

echo "✅ GitHub token is set"
echo ""

# Set GitHub details
GITHUB_USER="abhaysahu403"
GITHUB_REPO="enterprise-observability-gitops"
GITHUB_BRANCH="main"

echo "📋 Configuration:"
echo "  GitHub User: $GITHUB_USER"
echo "  Repository:  $GITHUB_REPO"
echo "  Branch:      $GITHUB_BRANCH"
echo "  Path:        ./k8s"
echo ""

read -p "Do you want to continue? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "Bootstrap cancelled."
    exit 0
fi

echo ""
echo "🚀 Bootstrapping FluxCD..."
echo ""

# Bootstrap Flux
flux bootstrap github \
  --owner=$GITHUB_USER \
  --repository=$GITHUB_REPO \
  --branch=$GITHUB_BRANCH \
  --path=./k8s \
  --personal \
  --components-extra=image-reflector-controller,image-automation-controller

echo ""
echo "✅ FluxCD bootstrap complete!"
echo ""

# Wait for Flux system to be ready
echo "⏳ Waiting for Flux system pods to be ready..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/part-of=flux -n flux-system --timeout=5m

echo ""
echo "✅ Flux system is ready!"
echo ""

# Show Flux components
echo "📦 Flux components:"
kubectl get pods -n flux-system
echo ""

# Show GitRepository status
echo "📚 Git repositories:"
flux get sources git
echo ""

# Show Kustomizations
echo "🔧 Kustomizations:"
flux get kustomizations
echo ""

echo "=========================================="
echo "✅ FluxCD Setup Complete!"
echo "=========================================="
echo ""
echo "FluxCD is now managing your deployments from:"
echo "  https://github.com/$GITHUB_USER/$GITHUB_REPO"
echo ""
echo "Useful commands:"
echo "  flux get all              # Show all Flux resources"
echo "  flux logs                 # Show Flux logs"
echo "  flux reconcile source git flux-system  # Force sync"
echo "  flux reconcile kustomization flux-system  # Force apply"
echo ""
echo "To test GitOps workflow:"
echo "  1. Edit k8s manifests in your repository"
echo "  2. Commit and push to GitHub"
echo "  3. Watch Flux apply changes automatically"
echo "  4. Monitor: kubectl get pods -n enterprise-observability -w"
echo ""
