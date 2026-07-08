#!/bin/bash

# FluxCD GitOps Demo Script
# Demonstrates automatic deployment and scaling through GitOps

set -e

echo "=========================================="
echo "FluxCD GitOps Workflow Demo"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

NAMESPACE="enterprise-observability"

echo -e "${BLUE}This demo will show:${NC}"
echo "  1. How Flux automatically syncs from GitHub"
echo "  2. GitOps-based scaling (edit replicas → commit → auto-deploy)"
echo "  3. Self-healing (manual changes reverted by Flux)"
echo "  4. Rollback using Git history"
echo ""

read -p "Press Enter to start the demo..."
echo ""

# Demo 1: Check current state
echo -e "${GREEN}=== Demo 1: Current Deployment State ===${NC}"
echo ""
echo "Let's check the current gateway deployment:"
kubectl get deployment gateway -n $NAMESPACE -o wide
echo ""

CURRENT_REPLICAS=$(kubectl get deployment gateway -n $NAMESPACE -o jsonpath='{.spec.replicas}')
echo "Current replicas: $CURRENT_REPLICAS"
echo ""

read -p "Press Enter to continue..."
echo ""

# Demo 2: Scale via GitOps
echo -e "${GREEN}=== Demo 2: GitOps Scaling (Manual Edit Required) ===${NC}"
echo ""
echo "To scale the gateway from $CURRENT_REPLICAS to 3 replicas using GitOps:"
echo ""
echo -e "${YELLOW}1. Edit the file:${NC}"
echo "   k8s/gateway/deployment.yaml"
echo ""
echo -e "${YELLOW}2. Change:${NC}"
echo "   replicas: $CURRENT_REPLICAS"
echo "   to"
echo "   replicas: 3"
echo ""
echo -e "${YELLOW}3. Commit and push:${NC}"
echo "   git add k8s/gateway/deployment.yaml"
echo "   git commit -m 'Scale gateway to 3 replicas'"
echo "   git push gitops main"
echo ""
echo -e "${YELLOW}4. Watch Flux sync automatically:${NC}"
echo "   flux reconcile kustomization flux-system --with-source"
echo "   kubectl get pods -n $NAMESPACE -w"
echo ""
echo "Flux will detect the change within 1 minute and scale automatically!"
echo ""

read -p "Have you pushed the change? Press Enter to check status..."
echo ""

# Force Flux to reconcile
echo "🔄 Forcing Flux to reconcile..."
flux reconcile source git flux-system
flux reconcile kustomization flux-system
echo ""

# Wait for reconciliation
echo "⏳ Waiting for reconciliation (30 seconds)..."
sleep 30
echo ""

# Check new state
NEW_REPLICAS=$(kubectl get deployment gateway -n $NAMESPACE -o jsonpath='{.spec.replicas}')
echo "New replicas: $NEW_REPLICAS"
kubectl get pods -n $NAMESPACE -l app=gateway
echo ""

read -p "Press Enter to continue..."
echo ""

# Demo 3: Self-Healing
echo -e "${GREEN}=== Demo 3: Self-Healing (Flux Reverts Manual Changes) ===${NC}"
echo ""
echo "Now let's try to manually scale the deployment and see Flux revert it:"
echo ""

echo "🔧 Manually scaling gateway to 5 replicas (outside of Git)..."
kubectl scale deployment gateway -n $NAMESPACE --replicas=5
echo ""

echo "Current state (manually changed):"
kubectl get deployment gateway -n $NAMESPACE
echo ""

echo "⏳ Waiting for Flux to detect drift and revert (within 1 minute)..."
echo "Watch how Flux automatically reverts to Git state (3 replicas):"
echo ""

# Watch for 60 seconds
for i in {1..6}; do
    sleep 10
    CURRENT=$(kubectl get deployment gateway -n $NAMESPACE -o jsonpath='{.spec.replicas}')
    echo "After ${i}0s: replicas=$CURRENT"
    if [ "$CURRENT" -eq 3 ]; then
        echo ""
        echo -e "${GREEN}✅ Flux detected drift and reverted to Git state!${NC}"
        break
    fi
done
echo ""

read -p "Press Enter to continue..."
echo ""

# Demo 4: Rollback
echo -e "${GREEN}=== Demo 4: Rollback via Git History ===${NC}"
echo ""
echo "To rollback to previous state:"
echo ""
echo -e "${YELLOW}1. View Git history:${NC}"
echo "   git log --oneline k8s/gateway/deployment.yaml"
echo ""
echo -e "${YELLOW}2. Revert the commit:${NC}"
echo "   git revert HEAD"
echo "   # or"
echo "   git reset --hard HEAD~1"
echo "   git push gitops main --force"
echo ""
echo -e "${YELLOW}3. Flux automatically applies the rollback!${NC}"
echo ""
echo "This is the power of GitOps - your Git history is your deployment history!"
echo ""

read -p "Press Enter to continue..."
echo ""

# Demo 5: Traffic Generation + Monitoring
echo -e "${GREEN}=== Demo 5: Generate Traffic for Observability ===${NC}"
echo ""
echo "Generate traffic to see OpenTelemetry traces in Dynatrace:"
echo ""

# Get gateway service
GATEWAY_URL=$(kubectl get svc gateway -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "localhost:8080")

echo "Gateway URL: http://$GATEWAY_URL"
echo ""
echo "Run these commands to generate traffic:"
echo ""
echo -e "${YELLOW}# Continuous traffic:${NC}"
echo "while true; do"
echo "  curl -s http://$GATEWAY_URL/api/employees > /dev/null"
echo "  curl -s http://$GATEWAY_URL/api/health > /dev/null"
echo "  sleep 1"
echo "done"
echo ""

read -p "Press Enter to show monitoring info..."
echo ""

# Demo 6: Monitoring
echo -e "${GREEN}=== Demo 6: Monitor FluxCD Operations ===${NC}"
echo ""

echo -e "${YELLOW}Git Repository Status:${NC}"
flux get sources git
echo ""

echo -e "${YELLOW}Kustomization Status:${NC}"
flux get kustomizations
echo ""

echo -e "${YELLOW}Recent Flux Events:${NC}"
kubectl get events -n flux-system --sort-by='.lastTimestamp' | tail -10
echo ""

echo -e "${YELLOW}Pod Status:${NC}"
kubectl get pods -n $NAMESPACE
echo ""

echo "=========================================="
echo "Demo Complete!"
echo "=========================================="
echo ""
echo -e "${GREEN}Key Takeaways:${NC}"
echo "  ✅ Flux automatically syncs from Git every 1 minute"
echo "  ✅ All changes must go through Git (GitOps principles)"
echo "  ✅ Flux reverts manual changes (self-healing)"
echo "  ✅ Rollback = revert Git commit"
echo "  ✅ Full audit trail in Git history"
echo ""
echo -e "${YELLOW}Useful Commands:${NC}"
echo "  flux get all                           # Show all Flux resources"
echo "  flux logs --follow                     # Stream Flux logs"
echo "  flux reconcile kustomization flux-system --with-source  # Force sync"
echo "  kubectl get pods -n $NAMESPACE -w      # Watch pods"
echo "  kubectl get events -n $NAMESPACE       # Show events"
echo ""
