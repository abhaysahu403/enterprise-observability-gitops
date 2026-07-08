# Screenshot Capture Guide

This guide helps you capture high-quality screenshots for your README and documentation.

---

## Required Screenshots

### 1. Application Dashboard
**File:** `screenshots/dashboard.png`

**What to capture:**
- Login to application: http://<LOAD_BALANCER_URL>
- Navigate to main dashboard
- Show employee list or dashboard overview
- Capture full browser window

**Tips:**
- Use clean test data
- Show multiple widgets/cards
- Ensure responsive layout is visible

---

### 2. Dynatrace Distributed Tracing
**File:** `screenshots/dynatrace-tracing.png`

**What to capture:**
1. Login to Dynatrace
2. Navigate to **Distributed Traces**
3. Find a trace that spans multiple services
4. Click to view full trace details
5. Capture the waterfall view showing all service calls

**Tips:**
- Look for traces with 5+ service spans
- Show timing information
- Include service names and response times
- Capture a successful trace (not errors)

---

### 3. Davis AI Problem Detection
**File:** `screenshots/davis-problem.png`

**What to capture:**
1. Go to Dynatrace → **Problems**
2. Select an active or recently closed problem
3. Show the problem details page with:
   - Problem title and severity
   - Affected entities
   - Root cause analysis
   - Timeline
   - Impact information

**Tips:**
- Use a real problem from the alert testing
- Show the ImagePullBackOff problem you created
- Include Problem ID and timestamps
- Show Davis AI's analysis section

---

### 4. Slack Alert Integration
**File:** `screenshots/slack-alert.png`

**What to capture:**
- Your Slack channel with Dynatrace alerts
- Show a complete alert message with:
  - Problem title
  - Severity
  - Affected service
  - Cluster and namespace info
  - Description
  - Problem ID
  - Timestamp

**Tips:**
- Use a real alert from your testing
- Show the full formatted message
- Include channel name in screenshot
- Show that it's an automated message (bot badge)

---

### 5. Kubernetes Monitoring
**File:** `screenshots/kubernetes-monitoring.png`

**What to capture:**
1. In Dynatrace, go to **Kubernetes**
2. Select your cluster: `enterprise-eks-cluster`
3. Show the cluster overview with:
   - Node health and resources
   - Namespace metrics
   - Pod status
   - Container metrics
   - Workload health

**Tips:**
- Show multiple metrics (CPU, memory, pod count)
- Include namespace: enterprise-observability
- Display healthy state (green indicators)
- Capture time range selector showing recent data

---

### 6. Service Dependencies
**File:** `screenshots/service-dependencies.png`

**What to capture:**
1. Go to **Services** in Dynatrace
2. Select **gateway** or **employee-service**
3. Click on **Service flow** or **Dependencies** tab
4. Show the automatic topology map

**Tips:**
- Capture service-to-service relationships
- Show database connections
- Include Redis cache connections
- Display request counts or response times on edges
- Use the graphical service flow view

---

### 7. Service List (Optional)
**File:** `screenshots/service-list.png`

**What to capture:**
- Dynatrace **Services** page showing all detected services:
  - gateway
  - authentication-service
  - employee-service
  - leave-service
  - payroll-service
  - asset-service
  - helpdesk-service
  - notification-service
  - postgres-service
  - redis-service

---

### 8. Kubernetes Workloads (Optional)
**File:** `screenshots/kubernetes-workloads.png`

**What to capture:**
- Dynatrace **Kubernetes** → **Workloads**
- Show all deployments with health status
- Include replica counts and resource usage

---

### 9. Application Architecture (Optional)
**File:** `screenshots/architecture-diagram.png`

**What to create:**
- Use draw.io, Lucidchart, or similar tool
- Create visual representation of:
  - Frontend → NGINX Ingress → Gateway → Services → Databases
  - Dynatrace OneAgent injection
  - Davis AI monitoring
  - Slack alerting workflow

---

### 10. Infrastructure View (Optional)
**File:** `screenshots/infrastructure.png`

**What to capture:**
- AWS Console showing:
  - EKS cluster
  - EC2 instances (nodes)
  - Load balancers
- Or Terraform state output
- Or `kubectl get nodes -o wide`

---

## Screenshot Best Practices

### Resolution
- **Minimum**: 1920×1080
- **Recommended**: 2560×1440 or higher
- Use high DPI displays when possible

### Format
- **Recommended**: PNG (lossless)
- Avoid JPEG (compression artifacts)

### Content
- Remove sensitive information:
  - API tokens
  - Account IDs (if not public)
  - Personal email addresses
  - Internal IP addresses (if sensitive)
- Keep:
  - Service names
  - Cluster names
  - Namespace names
  - Timestamps
  - Metrics and graphs

### Framing
- Include enough context (URLs, navigation breadcrumbs)
- Remove unnecessary browser UI (bookmarks bar, extensions)
- Keep the application/service name visible
- Show legends for graphs
- Include timestamps for time-series data

### Editing
- Add arrows or highlights if needed (use red/yellow)
- Add text annotations if explaining complex flows
- Crop to remove empty space
- Don't over-edit or add fake data

---

## Capture Commands

### kubectl Commands for Terminal Screenshots
```bash
# Show all pods
kubectl get pods -n enterprise-observability

# Show deployments
kubectl get deployments -n enterprise-observability

# Show services
kubectl get svc -n enterprise-observability

# Show Dynatrace components
kubectl get pods -n dynatrace

# Show ingress
kubectl get ingress -n enterprise-observability

# Show nodes with resource usage
kubectl top nodes

# Show pod resource usage
kubectl top pods -n enterprise-observability
```

### Generate Traffic for Screenshots
```bash
# Get application URL
LB_URL=$(kubectl get ingress ingress-nginx -n enterprise-observability \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

# Login
TOKEN=$(curl -X POST "http://$LB_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Password@123"}' \
  | jq -r '.token')

# Generate diverse traffic
for i in {1..20}; do
  curl -H "Authorization: Bearer $TOKEN" "http://$LB_URL/api/employees"
  curl -H "Authorization: Bearer $TOKEN" "http://$LB_URL/api/leaves"
  curl -H "Authorization: Bearer $TOKEN" "http://$LB_URL/api/assets"
  curl -H "Authorization: Bearer $TOKEN" "http://$LB_URL/api/payroll/records"
  sleep 1
done
```

---

## Screenshot Checklist

Before publishing your documentation:

- [ ] All required screenshots captured
- [ ] Screenshots are high resolution (1920×1080 minimum)
- [ ] Sensitive information removed or redacted
- [ ] Files saved in PNG format
- [ ] Files named according to guide
- [ ] Files placed in `docs/screenshots/` directory
- [ ] README.md updated with correct image paths
- [ ] Images display correctly in GitHub preview
- [ ] Alt text added for accessibility
- [ ] File sizes optimized (< 2MB per image)

---

## Tools

### Screenshot Capture
- **Windows**: Snipping Tool, Win+Shift+S, ShareX
- **Mac**: Cmd+Shift+4, Cmd+Shift+3
- **Linux**: Flameshot, Spectacle, gnome-screenshot
- **Browser**: Firefox/Chrome DevTools screenshot feature

### Image Editing
- **Free**: GIMP, Paint.NET, Krita
- **Online**: Photopea, Pixlr
- **Lightweight**: IrfanView, XnView
- **Annotation**: Greenshot, Snagit

### Image Optimization
```bash
# Install ImageMagick
# Windows: choco install imagemagick
# Mac: brew install imagemagick
# Linux: apt install imagemagick

# Optimize PNG
mogrify -strip -resize 1920x1080 -quality 85 screenshot.png

# Compress all screenshots
cd docs/screenshots
for img in *.png; do
  convert "$img" -strip -resize 1920x1080 -quality 85 "optimized_$img"
done
```

---

## Alternative: Use Existing Screenshots

If you don't have a deployed environment yet, you can:

1. Use placeholder images temporarily
2. Add TODO markers in README
3. Reference Dynatrace demo environment screenshots
4. Use architecture diagrams instead of live screenshots
5. Add "Coming Soon" badges

Example placeholder:
```markdown
![Screenshot Coming Soon](https://via.placeholder.com/1920x1080/333/fff?text=Screenshot+Coming+Soon)
```

---

## Publishing Checklist

Before pushing to GitHub:

- [ ] All screenshots in `docs/screenshots/` directory
- [ ] Image paths in README.md are relative: `docs/screenshots/image.png`
- [ ] Images render correctly in local README preview
- [ ] Total size of screenshots < 20MB
- [ ] Images are in PNG format
- [ ] No sensitive information in screenshots
- [ ] README.md mentions what screenshots show

---

## Example Screenshot Section in README

```markdown
## 📸 Platform Screenshots

### Application Dashboard
![Dashboard](docs/screenshots/dashboard.png)
*Enterprise dashboard built with React + Material-UI*

### Dynatrace Distributed Tracing
![Distributed Tracing](docs/screenshots/dynatrace-tracing.png)
*End-to-end request tracing across all microservices*

### Davis AI Problem Detection
![Davis Problem](docs/screenshots/davis-problem.png)
*Automatic anomaly detection with root cause analysis - Problem P-2607468*

### Slack Alert Notification
![Slack Alert](docs/screenshots/slack-alert.png)
*Real-time alert delivered to Slack channel via Dynatrace Workflow*
```

---

## Need Help?

If you encounter issues capturing screenshots:
1. Check Dynatrace tenant access and permissions
2. Verify services are running: `kubectl get pods -n enterprise-observability`
3. Generate traffic before capturing monitoring screenshots
4. Wait 5-10 minutes for Dynatrace to collect and display data
5. Ensure browser zoom is 100% for clean screenshots
