# Grafana + InfluxDB Integration Guide

## ✅ **DEPLOYMENT STATUS: COMPLETED**

**Date:** July 9, 2026  
**Status:** All components operational and metrics flowing ✅

### Current State
- ✅ **InfluxDB StatefulSet:** 1/1 pods running (20GB PVC on gp2)
- ✅ **Grafana Deployment:** 1/1 pods running (10GB PVC on gp2)  
- ✅ **OTel Collector Deployment:** 2/2 pods running
- ✅ **Prometheus Scraping:** All 8 Spring Boot services configured with static targets
- ✅ **Metrics Pipeline:** No scraping errors, no export errors
- ✅ **InfluxDB Connection:** Healthy and receiving data
- ✅ **Grafana Datasource:** InfluxDB configured and connected
- ✅ **Dashboards:** Pre-configured dashboards loaded

### Access Information
```bash
# Access Grafana
kubectl port-forward -n monitoring svc/grafana 3000:80
# URL: http://localhost:3000
# Username: admin
# Password: GrafanaAdmin@Secure123
```

---

## Overview

This guide explains how to deploy **Grafana** and **InfluxDB** alongside the existing **Dynatrace** observability stack. Both monitoring solutions work in parallel without interfering with each other.

### Architecture

```
Spring Boot Services (with /actuator/prometheus)
        │
        ├─────────────────────────────────────┐
        │                                     │
        ▼                                     ▼
   Dynatrace OneAgent              OpenTelemetry Collector
   (Auto-instrumentation)          (Prometheus Scraper)
        │                                     │
        ▼                                     ▼
   Dynatrace Platform                    InfluxDB
        │                                     │
        ▼                                     ▼
   Dynatrace Dashboards                 Grafana Dashboards
```

**Key Points:**
- ✅ **Dynatrace OneAgent** continues auto-instrumenting everything (UNCHANGED)
- ✅ **OTel Collector** scrapes `/actuator/prometheus` endpoints from services
- ✅ **No overlap** - OneAgent and OTel Collector collect independently
- ✅ **No duplicate data** - Each system has its own telemetry path

### Key Features

✅ **Independent Monitoring** — Dynatrace OneAgent and OTel Collector work separately  
✅ **No Overlap** — OneAgent auto-instruments, OTel scrapes Prometheus metrics  
✅ **No Impact on Dynatrace** — OneAgent continues unchanged  
✅ **GitOps Deployment** — Entirely managed through FluxCD  
✅ **Persistent Storage** — Both InfluxDB (20GB) and Grafana (10GB) use AWS EBS gp3  
✅ **Auto-Provisioned** — Datasources and dashboards created automatically  
✅ **Clean Separation** — Monitoring namespace isolated from application namespace  

---

## Prerequisites

- EKS cluster running with FluxCD installed
- Dynatrace already deployed and working
- `kubectl` configured for your cluster
- FluxCD syncing from your Git repository

---

## Deployment Steps

### 1. Verify Services Expose Prometheus Metrics

First, check that your services expose `/actuator/prometheus`:

```bash
# Port-forward to gateway
kubectl port-forward -n enterprise-observability deployment/gateway 8080:8080

# Test Prometheus endpoint
curl http://localhost:8080/actuator/prometheus

# You should see metrics like:
# jvm_memory_used_bytes{area="heap"} 123456789
# http_server_requests_seconds_count{method="GET"} 42
```

All services already have `micrometer-registry-prometheus` dependency, so this should work! ✅

### 2. Commit and Push to Git

```bash
git add k8s/monitoring/ docs/GRAFANA_INFLUXDB_DEPLOYMENT.md
git commit -m "Add Grafana + InfluxDB monitoring stack [skip ci]"
git push gitops main
```

### 3. Wait for FluxCD to Deploy

FluxCD will automatically:
1. Create the `monitoring` namespace
2. Install InfluxDB via Helm (with 20GB persistent volume)
3. Install Grafana via Helm (with 10GB persistent volume)
4. Deploy OpenTelemetry Collector (2 replicas)
5. Provision datasources automatically
6. Load dashboards automatically

Check deployment progress:

```bash
# Watch FluxCD reconciliation
flux get kustomizations -n flux-system

# Check HelmReleases
flux get helmreleases -n monitoring

# Check pods
kubectl get pods -n monitoring

# Expected output:
# NAME                              READY   STATUS    RESTARTS   AGE
# influxdb-0                        1/1     Running   0          5m
# grafana-xxxxxxxxxx-xxxxx          1/1     Running   0          5m
# otel-collector-xxxxxxxxxx-xxxxx   1/1     Running   0          5m
# otel-collector-xxxxxxxxxx-xxxxx   1/1     Running   0          5m
```

### 4. Verify InfluxDB

```bash
# Check InfluxDB is running
kubectl get pods -n monitoring | grep influxdb

# Check PVC is bound
kubectl get pvc -n monitoring | grep influxdb

# Check InfluxDB logs
kubectl logs -n monitoring influxdb-0
```

### 5. Verify Grafana

```bash
# Check Grafana is running
kubectl get pods -n monitoring | grep grafana

# Check PVC is bound
kubectl get pvc -n monitoring | grep grafana

# Get Grafana password
kubectl get secret -n monitoring grafana -o jsonpath="{.data.admin-password}" | base64 --decode
# Default: GrafanaAdmin@Secure123
```

### 7. Verify OpenTelemetry Collector

```bash
# Check OTel Collector pods
kubectl get pods -n monitoring | grep otel-collector

# Check OTel Collector logs
kubectl logs -n monitoring deployment/otel-collector

# You should see:
# - "Everything is ready. Begin running and processing data."
# - Successful connections to InfluxDB
# - Prometheus scraper discovering Spring Boot services
```

**Note:** OTel Collector does NOT export to Dynatrace. Dynatrace OneAgent handles all Dynatrace monitoring separately.

### 8. Access Grafana

#### Option A: Port Forward (Quick Test)

```bash
kubectl port-forward -n monitoring svc/grafana 3000:80
```

Then open: http://localhost:3000

Login:
- **Username:** `admin`
- **Password:** `GrafanaAdmin@Secure123`

#### Option B: Through Ingress (Production)

The Grafana ingress is already configured. Get the Load Balancer URL:

```bash
kubectl get ingress -n monitoring

# Access via: http://<LOAD_BALANCER_URL>/grafana
```

### 9. Verify Datasources

After logging into Grafana:

1. Go to **Configuration → Data Sources**
2. You should see:
   - ✅ **InfluxDB** (default) — Connected to `http://influxdb.monitoring.svc.cluster.local:8086`
   - ✅ **Prometheus** — Connected to OpenTelemetry Collector

3. Click **Test** on each datasource — both should return success.

### 10. View Dashboards

Pre-configured dashboards are automatically loaded:

1. Go to **Dashboards → Browse**
2. Open folder **"Enterprise Observability"**
3. Available dashboards:
   - **Enterprise Platform - JVM Metrics**
   - **Enterprise Platform - API Gateway**
   - **Enterprise Platform - Kubernetes Cluster**
   - **Enterprise Platform - Employee Service**
   - **Enterprise Platform - PostgreSQL**

---

## Add Prometheus Annotations to Services

To enable OTel Collector to scrape metrics, add annotations to each service deployment.

**Example for Gateway:**

```yaml
# k8s/gateway/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway
  namespace: enterprise-observability
spec:
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
        - name: gateway
          # ... rest stays unchanged
```

**Apply to all services:**
- ✅ gateway
- ✅ authentication-service (port: 8081)
- ✅ employee-service (port: 8082)
- ✅ leave-service (port: 8083)
- ✅ payroll-service (port: 8084)
- ✅ asset-service (port: 8085)
- ✅ helpdesk-service (port: 8086)
- ✅ notification-service (port: 8087)

**That's it!** Just 3 annotation lines per service. No code changes needed.

---

## Verify Independent Monitoring

### Check InfluxDB Data

```bash
# Port forward to InfluxDB
kubectl port-forward -n monitoring svc/influxdb 8086:8086

# Open InfluxDB UI: http://localhost:8086
# Login with:
#   Username: admin
#   Password: InfluxDB@SecurePassword123
#   Organization: enterprise-observability
#   Bucket: metrics

# Run query to verify data:
from(bucket: "metrics")
  |> range(start: -1h)
  |> filter(fn: (r) => r._measurement == "jvm_memory_used_bytes")
  |> limit(n: 10)
```

### Check Dynatrace Still Works Independently

1. Open Dynatrace UI
2. Go to **Services**
3. Verify all microservices still visible
4. Check traces still working
5. Verify Davis AI still detecting problems
6. Check OneAgent still running:
   ```bash
   kubectl get pods -n dynatrace | grep oneagent
   ```

**Both monitoring systems should work independently with NO interference!**

---

## Troubleshooting

### InfluxDB Pod Not Starting

```bash
# Check PVC
kubectl get pvc -n monitoring

# If PVC is pending, check storage class
kubectl get storageclass

# If gp3 doesn't exist, change to gp2 in helmrelease.yaml
```

### Grafana Can't Connect to InfluxDB

```bash
# Check InfluxDB service
kubectl get svc -n monitoring influxdb

# Test connectivity from Grafana pod
kubectl exec -n monitoring deployment/grafana -- curl -v http://influxdb.monitoring.svc.cluster.local:8086/health
```

### OTel Collector Not Receiving Metrics

```bash
# Check collector logs
kubectl logs -n monitoring deployment/otel-collector -f

# Check if services can reach collector
kubectl exec -n enterprise-observability deployment/gateway -- curl -v http://otel-collector.monitoring.svc.cluster.local:4318
```

### No Data in Grafana Dashboards

```bash
# Check InfluxDB has data
kubectl exec -n monitoring influxdb-0 -- influx query 'from(bucket:"metrics") |> range(start:-1h) |> limit(n:10)'

# Check OTel Collector is exporting
kubectl logs -n monitoring deployment/otel-collector | grep "influxdb"
```

---

## Cleanup (Optional)

To remove Grafana + InfluxDB while keeping Dynatrace:

```bash
# Delete monitoring namespace
kubectl delete namespace monitoring

# Remove from Git
git rm -r k8s/monitoring/
git commit -m "Remove Grafana + InfluxDB"
git push gitops main
```

**Dynatrace will continue working normally.**

---

## Next Steps

1. ✅ Deploy monitoring stack
2. ✅ Verify dual export working
3. ✅ View dashboards in Grafana
4. ⬜ Configure alerts in Grafana
5. ⬜ Add Slack notifications from Grafana
6. ⬜ Create custom business dashboards
7. ⬜ Set up Grafana SLOs

---

## Quick Verification Commands

Run these commands to verify the deployment:

```bash
# 1. Check all pods are running
kubectl get pods -n monitoring
# Expected: 4 pods running (2 otel-collector, 1 grafana, 1 influxdb)

# 2. Verify scraping is working (no "Failed to scrape" errors)
kubectl logs -n monitoring deployment/otel-collector --tail=50 | grep -i "failed"
# Expected: No output (no failures)

# 3. Verify InfluxDB connection (no errors)
kubectl logs -n monitoring deployment/otel-collector --tail=50 | grep -i "influxdb.*error"
# Expected: No output (no errors)

# 4. Verify all 8 scrape jobs are configured
kubectl logs -n monitoring deployment/otel-collector --tail=100 | grep "Scrape job added"
# Expected: 8 lines showing gateway, auth, employee, leave, payroll, asset, helpdesk, notification

# 5. Check Prometheus annotations on services
kubectl get pods -n enterprise-observability -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.metadata.annotations.prometheus\.io/scrape}{"\n"}{end}' | grep -v "^$"
# Expected: All pods showing "true"

# 6. Test Prometheus endpoint manually
kubectl exec -n enterprise-observability deployment/gateway -- curl -s http://localhost:8080/actuator/prometheus | head -20
# Expected: Prometheus metrics output (jvm_memory_used_bytes, etc.)

# 7. Access Grafana
kubectl port-forward -n monitoring svc/grafana 3000:80
# Then open: http://localhost:3000
# Login: admin / GrafanaAdmin@Secure123
```

---

## Summary

You now have **two complete observability stacks**:

| Feature | Dynatrace | Grafana + InfluxDB |
|---|---|---|
| **Automatic Instrumentation** | ✅ OneAgent | ❌ Manual |
| **AI Root Cause Analysis** | ✅ Davis AI | ❌ |
| **Custom Dashboards** | ✅ | ✅ |
| **Log Monitoring** | ✅ | ⬜ (can add) |
| **APM** | ✅ | ⬜ (via OTel) |
| **Kubernetes Monitoring** | ✅ ActiveGate | ✅ |
| **Cost** | 💰💰💰 | Free (OSS) |
| **Flexibility** | Medium | High |

**Use Both:**
- **Dynatrace** for production monitoring, AI detection, automatic insights
- **Grafana** for custom visualizations, development metrics, cost-effective monitoring

Both work together without conflicts! 🎉
