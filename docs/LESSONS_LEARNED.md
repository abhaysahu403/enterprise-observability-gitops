# Lessons Learned: Production Deployment Journey

This document captures real issues encountered during the deployment of this enterprise observability platform on AWS EKS with Dynatrace, and their solutions.

---

## 1. EKS Cluster Resource Sizing

### Problem
Initial cluster configuration with 2×t3.medium nodes (2 vCPU, 4GB RAM each) was insufficient:
- **Symptom**: Pods stuck in `Pending` state with errors:
  ```
  0/2 nodes are available
  Too many pods
  Insufficient cpu
  Insufficient memory
  ```
- **Root Cause**: 11 application pods + Dynatrace components (ActiveGate, OneAgent DaemonSet, CSI Driver, Operator) exceeded available resources
- **Impact**: Complete deployment failure; critical services couldn't schedule

### Solution
Upgraded to 3×t3.xlarge nodes:
```terraform
instance_type    = "t3.xlarge"  # Was: t3.medium
desired_capacity = 3            # Was: 2
min_size         = 3            # Was: 1
max_size         = 4            # Was: 2
disk_size        = 50           # Was: 30
```

**Result**: Cluster now has 12 vCPU and 48GB RAM total with healthy utilization (3-4% CPU, 14-21% memory per node).

### Lesson
**Always account for monitoring infrastructure in capacity planning.** Dynatrace OneAgent, ActiveGate, and CSI components consume significant resources. For production:
- Calculate: (Application pods × avg resource) + (Monitoring components) + (System overhead) + 30% buffer
- Monitor actual usage and adjust before adding services

---

## 2. PostgreSQL PVC Missing

### Problem
PostgreSQL pod stuck in `Pending` state:
- **Symptom**: 
  ```
  persistentvolumeclaim "postgres-pvc" not found
  ```
- **Root Cause**: PVC manifest existed but was never applied to cluster
- **Impact**: All database-dependent services failed to start

### Solution
```bash
kubectl apply -f k8s/postgres/pvc.yaml
```

Created 10GB PVC with gp2 storage class.

### Lesson
**Verify resource creation order.** Storage must exist before pods that reference it. Use Kubernetes init containers or startup dependencies to enforce ordering.

---

## 3. Application Startup Probe Failures

### Problem
Multiple microservices (employee-service, leave-service, payroll-service, notification-service) stuck in `CrashLoopBackOff`:
- **Symptom**: Pods restarting every 60-90 seconds
- **Logs showed**: 
  ```
  Started EmployeeServiceApplication in 145.23 seconds
  ```
  But Kubernetes killed the pod before completion.

### Investigation
```bash
kubectl logs employee-service-xxx -n enterprise-observability
```
Revealed:
1. Spring Boot applications starting successfully
2. Flyway migrations completing
3. Database connections working
4. **But liveness probes failing before full initialization**

### Root Cause
Original probe configuration:
```yaml
livenessProbe:
  initialDelaySeconds: 60
  timeoutSeconds: 1
readinessProbe:
  initialDelaySeconds: 30
  timeoutSeconds: 5
```

**Spring Boot + Dynatrace OneAgent takes 2-3 minutes to fully initialize:**
- OneAgent init container injection: +30s
- Spring context initialization: +60s
- Dynatrace instrumentation initialization: +30s
- Flyway migrations: +15-30s

Probes checked too early → timeout → Kubernetes killed pod → endless restart loop.

### Solution
Updated all deployment manifests:
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8082
  initialDelaySeconds: 150  # Was: 60
  periodSeconds: 10
  timeoutSeconds: 10        # Was: 1
  failureThreshold: 3
  
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8082
  initialDelaySeconds: 120  # Was: 30
  periodSeconds: 10
  timeoutSeconds: 10        # Was: 5
  failureThreshold: 3
```

**Result**: All 11 application pods now start successfully (1/1 READY).

### Lesson
**Always measure actual startup time in the target environment before setting probe timings.** Formula:
```
initialDelaySeconds = (measured startup time) + (30s buffer)
```

For instrumented applications (APM, tracing, profiling agents), add extra buffer for agent initialization.

---

## 4. Dynatrace OneAgent DaemonSet CrashLoopBackOff

### Problem
2 out of 3 OneAgent pods in CrashLoopBackOff:
- **Symptom**:
  ```
  openssl process was terminated externally via SIGKILL
  ```
  During installer signature verification
- **Impact**: Partial monitoring coverage; some nodes not instrumented

### Investigation
```bash
kubectl logs dynatrace-oneagent-xxx -n dynatrace
```
Showed signature verification process being killed mid-execution.

### Root Cause
Original DynaKube resource configuration:
```yaml
initResources:
  limits:
    cpu: 100m
    memory: 128Mi
```

Signature verification is CPU and memory intensive. The process exceeded limits and was killed by Kubernetes OOMKiller.

### Solution
Updated `k8s/dynatrace/dynakube.yaml`:
```yaml
initResources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 500m      # Was: 100m
    memory: 512Mi  # Was: 128Mi
    
oneAgentResources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 300m
    memory: 512Mi  # Was: 256Mi
```

Applied and forced pod recreation:
```bash
kubectl apply -f k8s/dynatrace/dynakube.yaml
kubectl delete pods -l app.kubernetes.io/name=dynatrace-oneagent -n dynatrace
```

**Result**: All 3 OneAgent pods now running successfully (1/1).

### Lesson
**Monitor init container resource usage separately from main containers.** Init containers often have different resource profiles (CPU spikes for signature verification, decompression, network downloads). Set limits generously for init containers—they're temporary and won't impact steady-state resource usage.

---

## 5. Ingress 404 Errors

### Problem
Accessing application via LoadBalancer URL returned 404:
- **Symptom**: `http://k8s-ingressn-xxx.elb.us-east-1.amazonaws.com` → 404 Not Found
- **Root Cause**: Ingress configured with:
  1. Host restriction: `host: enterprise.example.com`
  2. SSL redirect enabled
  3. TLS configuration present

Requests to direct IP/hostname didn't match the host rule.

### Solution
Updated `k8s/ingress/ingress.yaml`:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ingress-nginx
  namespace: enterprise-observability
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "false"  # Added
spec:
  ingressClassName: nginx
  rules:
    - host: "*"  # Was: enterprise.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend-service
                port:
                  number: 3000
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: gateway-service
                port:
                  number: 8080
  # Removed tls section
```

**Result**: Application accessible via both IP addresses:
- http://34.195.29.70
- http://34.237.232.204
- http://k8s-ingressn-xxx.elb.us-east-1.amazonaws.com

### Lesson
**For demo/dev environments, use wildcard host matching.** For production:
1. Register a domain
2. Configure Route53 or external DNS
3. Add cert-manager for automatic TLS
4. Use specific host rules with TLS enabled

---

## 6. Davis AI Not Detecting Problems (The Big One!)

### Problem
Generated thousands of failed requests with service scaled to 0 replicas, but **Davis AI created no problems**.

### Attempts That Failed

**Attempt 1: Scale authentication-service to 0**
```bash
kubectl scale deployment authentication-service --replicas=0
```
Generated 500+ failed requests over 5 minutes. Result: **No problem detected.**

**Attempt 2: Scale 3 services simultaneously**
```bash
kubectl scale deployment gateway --replicas=0
kubectl scale deployment employee-service --replicas=0
kubectl scale deployment authentication-service --replicas=0
```
Generated 15,000+ failed requests at 50 req/sec. Result: **No problem detected.**

### Investigation
Checked Kubernetes events:
```bash
kubectl get events -n enterprise-observability --sort-by='.lastTimestamp'
```

Output showed:
```
7m55s  Normal  ScalingReplicaSet  deployment/gateway
       Scaled down replica set gateway-xxx to 0 from 1
```

**Key insight**: Event type was `Normal`, reason was `ScalingReplicaSet`.

### Root Cause
**Davis AI distinguishes between expected and unexpected failures:**
- `kubectl scale --replicas=0` generates a `Normal` Kubernetes event
- Davis AI interprets this as **intentional administrative action**
- Therefore, **no problem is created**

This is by design! Davis AI is smart enough to ignore intentional operations.

### Solution
Use **unexpected failure scenarios** instead:
```bash
# ✅ This triggers Davis AI:
kubectl set image deployment/gateway \
  gateway=nginx:invalid-image-404 \
  -n enterprise-observability
```

This causes:
- `ImagePullBackOff` errors
- `Warning` events (not `Normal`)
- Kubernetes reports: `Failed to pull image: not found`

Within 3-4 minutes:
- **Davis AI detected the problem** ✅
- **Created Problem P-2607468 (SEV-3)** ✅
- **Triggered Slack workflow automatically** ✅
- **Sent alert to Slack channel** ✅

### Lesson
**When testing monitoring and alerting systems:**
- ❌ Don't use `kubectl scale --replicas=0` → Treated as intentional
- ❌ Don't use `kubectl delete deployment` → Treated as intentional
- ✅ Use crash scenarios:
  - Invalid container images → ImagePullBackOff
  - Memory/CPU stress → OOMKilled / CPU throttling
  - Liveness probe failures → CrashLoopBackOff
  - Application panics/exits → Container restarts

**Davis AI is context-aware.** It analyzes:
- Event types (Normal vs. Warning vs. Error)
- Event reasons (ScalingReplicaSet vs. BackOff vs. FailedScheduling)
- Change metadata (deployment spec changes, manual interventions)
- Temporal patterns (gradual vs. sudden, recurring vs. one-time)

---

## 7. Service Discovery in Kubernetes

### Problem
Services couldn't communicate even though pods were running:
- **Symptom**: `Connection refused` errors in logs
- **Root Cause**: Hardcoded service URLs instead of Kubernetes DNS

### Solution
Use Kubernetes service DNS:
```yaml
# ❌ Wrong:
AUTH_SERVICE_URL: "http://localhost:8081"

# ✅ Right:
AUTH_SERVICE_URL: "http://authentication-service.enterprise-observability.svc.cluster.local:8081"

# ✅ Short form (same namespace):
AUTH_SERVICE_URL: "http://authentication-service:8081"
```

### Lesson
**Use Kubernetes DNS for all inter-service communication:**
- Format: `<service-name>.<namespace>.svc.cluster.local:<port>`
- Short form works within same namespace: `<service-name>:<port>`
- Never use pod IPs (they change on restart)
- Never use localhost in containerized environments

---

## 8. ConfigMap Updates Not Propagating

### Problem
Updated ConfigMap but services still used old values:
- **Root Cause**: Pods don't automatically reload ConfigMaps
- **Impact**: Configuration changes required manual pod restarts

### Solution
```bash
# After updating ConfigMap:
kubectl apply -f k8s/configmap.yaml

# Force pod recreation:
kubectl rollout restart deployment -n enterprise-observability
```

Or use a ConfigMap hash annotation:
```yaml
spec:
  template:
    metadata:
      annotations:
        checksum/config: {{ include (print $.Template.BasePath "/configmap.yaml") . | sha256sum }}
```

### Lesson
**ConfigMaps are mounted at pod creation time, not updated live.** Options:
1. Manual rollout restart after ConfigMap changes
2. Use ConfigMap hash annotations to trigger automatic rollouts
3. Use tools like Reloader (https://github.com/stakater/Reloader)
4. For frequently changing config, use external config services (Spring Cloud Config, etcd)

---

## 9. Terraform State Locking

### Problem
Multiple team members running Terraform simultaneously caused state corruption.

### Solution
Configure S3 backend with DynamoDB locking:
```hcl
terraform {
  backend "s3" {
    bucket         = "my-terraform-state-bucket"
    key            = "enterprise-eks/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "terraform-lock-table"
    encrypt        = true
  }
}
```

### Lesson
**Always use remote state with locking for team environments.** Benefits:
- Prevents concurrent modifications
- Enables state sharing
- Provides state versioning
- Enables automated CI/CD

---

## 10. Resource Requests vs Limits

### Problem
Pods being OOMKilled even with "plenty" of node memory available.

### Root Cause
Misunderstood difference between requests and limits:
- **Requests**: Guaranteed resources; used for scheduling decisions
- **Limits**: Maximum allowed; pod killed if exceeded

Set requests too low → Multiple pods scheduled to same node → Total usage exceeds node capacity → OOMKiller activates.

### Solution
Set requests and limits based on actual usage:
```yaml
resources:
  requests:
    memory: "256Mi"  # Guaranteed minimum
    cpu: "100m"
  limits:
    memory: "512Mi"  # Maximum allowed (2× requests)
    cpu: "500m"      # Maximum allowed (5× requests)
```

Monitor actual usage:
```bash
kubectl top pods -n enterprise-observability
```

### Lesson
**Set requests = typical usage, limits = peak usage:**
1. Start with conservative requests
2. Monitor actual usage for 1-2 weeks
3. Adjust requests to P75 usage
4. Set limits to P99 usage + 20%
5. For predictable workloads, requests ≈ limits (guaranteed QoS)
6. For variable workloads, limits = 2-3× requests (burstable QoS)

---

## Key Takeaways

1. **Capacity Planning**: Always account for monitoring infrastructure (Dynatrace, Prometheus, etc.) in cluster sizing. Add 30-50% buffer.

2. **Probe Timings**: Measure actual startup time in target environment. Add generous buffers for instrumented applications.

3. **Resource Limits**: Monitor actual usage before setting limits. Init containers often need higher limits than main containers.

4. **Alert Testing**: Use unexpected failure scenarios (crashes, OOM, ImagePullBackOff), not intentional operations (scale-down, deletions).

5. **Troubleshooting Approach**: 
   - Always check logs first: `kubectl logs <pod> -n <namespace>`
   - Then events: `kubectl get events -n <namespace> --sort-by='.lastTimestamp'`
   - Then describe: `kubectl describe pod <pod> -n <namespace>`
   - Never guess—investigate systematically

6. **Documentation**: Document deployment steps, issues, and solutions in real-time. Future you will thank present you.

7. **Iterative Improvement**: Infrastructure rarely works perfectly on first deployment. Plan for multiple iterations of sizing, tuning, and optimization.

---

## Metrics

**Initial Deployment Issues**: 10
**Time to First Successful Deployment**: 4 hours
**Time to Stable Dynatrace Integration**: 2 hours
**Time to Working Alerting**: 3 hours (due to Davis AI learning curve)

**Total Time Investment**: ~12 hours

**Final State**: ✅ Production-ready platform with full observability and alerting

---

## Tools That Helped

- `kubectl describe pod` - Most useful command for debugging
- `kubectl logs -f` - Real-time log streaming
- `kubectl get events --sort-by='.lastTimestamp'` - Timeline view
- `kubectl top nodes/pods` - Resource usage visibility
- Dynatrace UI - Service dependency mapping and distributed tracing
- AWS CloudWatch - EKS control plane logs

---

## Recommended Pre-Flight Checklist

Before deploying similar infrastructure:

- [ ] Verify AWS quotas (EC2 instances, VPCs, EIPs)
- [ ] Size cluster for applications + monitoring + 30% buffer
- [ ] Measure application startup time in target environment
- [ ] Set probe initialDelaySeconds > measured startup time + 30s
- [ ] Verify PVCs before deploying stateful workloads
- [ ] Test Dynatrace token permissions
- [ ] Configure Slack app before creating workflows
- [ ] Document all environment-specific values (tokens, URLs, IDs)
- [ ] Create runbook for common issues
- [ ] Set up monitoring for the monitoring (meta-monitoring)

---

## Future Improvements

1. **GitOps**: Implement FluxCD or ArgoCD for declarative deployments
2. **Auto-scaling**: Configure HPA based on custom metrics from Dynatrace
3. **Multi-region**: Deploy to multiple regions with global load balancing
4. **Backup/Restore**: Implement Velero for disaster recovery
5. **Cost Optimization**: Use Spot instances for non-critical workloads
6. **Security**: Implement Pod Security Standards, Network Policies, OPA policies
7. **Observability++**: Add custom Dynatrace dashboards, SLO monitoring, business KPIs
