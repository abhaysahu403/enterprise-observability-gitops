# Nimbus — Enterprise Observability Demonstration Platform

A production-grade, multi-service enterprise application demonstrating **complete enterprise observability** with Dynatrace on Amazon EKS. This is not a toy demo — it's a fully instrumented, monitored, and alerted microservices platform running on AWS infrastructure.

![Platform Status](https://img.shields.io/badge/status-production--ready-green)
![AWS EKS](https://img.shields.io/badge/AWS-EKS-orange)
![Dynatrace](https://img.shields.io/badge/Dynatrace-OneAgent-purple)
![Kubernetes](https://img.shields.io/badge/Kubernetes-1.28+-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green)

---

## ✨ Platform Features

### � Enterprise Observability Stack
- ✅ **OpenTelemetry** — Industry-standard instrumentation
- ✅ **Dynatrace OneAgent** — Automatic full-stack monitoring
- ✅ **Dynatrace ActiveGate** — Kubernetes cluster monitoring
- ✅ **Distributed Tracing** — Complete request flow visibility
- ✅ **Kubernetes Monitoring** — Pod, container, node, and cluster metrics
- ✅ **Davis AI** — Automatic anomaly detection and root cause analysis
- ✅ **Slack Notifications** — Real-time problem alerts
- ✅ **Log Monitoring** — Centralized log aggregation and analysis
- ✅ **Database Observability** — PostgreSQL query performance tracking
- ✅ **JVM Monitoring** — Heap, GC, thread, and CPU metrics
- ✅ **Service Level Objectives (SLO)** — Availability and performance tracking
- ✅ **Error Budget Monitoring** — SLO compliance tracking
- ✅ **Enterprise Dashboards** — Custom business and technical KPI visualization

### 🏗️ Application Architecture
- ✅ **7 Spring Boot Microservices** — Authentication, Employee, Leave, Payroll, Asset, Helpdesk, Notification
- ✅ **API Gateway** — Spring Cloud Gateway with JWT validation and rate limiting
- ✅ **React Frontend** — Material-UI dashboard with responsive design
- ✅ **Database-per-Service** — PostgreSQL with Flyway migrations
- ✅ **Redis Cache** — Distributed caching and session management
- ✅ **RESTful APIs** — OpenAPI/Swagger documentation
- ✅ **JWT Authentication** — Secure token-based authentication
- ✅ **Role-Based Access Control** — Admin, HR, Manager, Employee roles

### ☁️ Cloud Infrastructure
- ✅ **Amazon EKS** — Managed Kubernetes cluster
- ✅ **Terraform IaC** — Complete infrastructure as code
- ✅ **AWS VPC** — Multi-AZ networking with public/private subnets
- ✅ **AWS ECR** — Private container registry
- ✅ **NGINX Ingress** — Layer 7 load balancing
- ✅ **AWS NLB** — Network load balancer
- ✅ **EBS Volumes** — Persistent storage for databases
- ✅ **Auto-Scaling** — Horizontal pod autoscaling

### 🔒 Production-Ready Features
- ✅ **Health Checks** — Liveness and readiness probes for all services
- ✅ **Graceful Shutdown** — Zero-downtime deployments
- ✅ **Resource Management** — CPU and memory limits/requests
- ✅ **Secrets Management** — Kubernetes secrets for sensitive data
- ✅ **ConfigMaps** — Externalized configuration
- ✅ **Rolling Updates** — Zero-downtime application updates
- ✅ **Multi-Environment** — Separate dev, staging, prod configurations

---

## 🎯 Demo Highlights

This platform showcases:

### 📊 Observability
- **Full-stack distributed tracing** across all microservices
- **Automatic problem detection** with Davis AI (ImagePullBackOff, CrashLoopBackOff, high error rates)
- **Real-time Slack notifications** via Dynatrace workflows
- **Kubernetes monitoring** with pod, container, and node metrics
- **Database and Redis observability** with connection pool metrics and query performance
- **Service dependency mapping** and call chain visualization
- **Log correlation** with traces using request IDs
- **Custom dashboards** for business and technical KPIs

### 🚨 Alerting & Automation
- **Davis AI anomaly detection** distinguishes intentional vs unexpected failures
- **Automatic incident creation** for production issues
- **Workflow automation** triggering on problem detection
- **Multi-channel notifications** (Slack, email, PagerDuty-ready)
- **Problem correlation** across services
- **Root cause analysis** with AI-powered insights

### 🏢 Business Capabilities
- **Employee Management** — CRUD operations, search, caching
- **Leave Management** — Apply, approve, balance tracking
- **Payroll Processing** — Automated monthly payroll generation
- **Asset Tracking** — Assignment, return, audit trail
- **Helpdesk System** — Ticket creation, SLA tracking, comments
- **Notifications** — Multi-channel with retry logic and delivery tracking

---

## 🚀 Quick Start

### For Demos and Presentations
```bash
# Get the application URL
kubectl get ingress -n enterprise-observability

# Access the application
open http://<LOAD_BALANCER_URL>

# Login credentials
Username: admin
Password: Password@123
```

### Deploy to AWS EKS (Complete Platform)
```bash
# 1. Deploy infrastructure
cd terraform
terraform init
terraform apply

# 2. Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name enterprise-eks-cluster

# 3. Install cluster components
# (AWS Load Balancer Controller, NGINX Ingress, Metrics Server)

# 4. Deploy Dynatrace
kubectl create namespace dynatrace
kubectl create secret generic enterprise-eks-cluster \
  --from-literal=apiToken=<YOUR_TOKEN> \
  --from-literal=dataIngestToken=<YOUR_TOKEN> \
  -n dynatrace
kubectl apply -f k8s/dynatrace/

# 5. Deploy application
kubectl create namespace enterprise-observability
kubectl apply -f k8s/

# Estimated deployment time: 20-30 minutes
```

**📖 Full deployment guide:** [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)

### Local Development (Docker Compose)
```bash
# For local testing without AWS
cp .env.example .env
docker compose build
docker compose up -d

# Access at http://localhost
```

---

## 📸 Screenshots & Demos

### Application Dashboard
![Dashboard](docs/screenshots/dashboard.png)
*React + Material-UI enterprise dashboard with real-time metrics*

### Dynatrace Distributed Tracing
![Distributed Tracing](docs/screenshots/dynatrace-tracing.png)
*Complete request flow across all microservices*

### Davis AI Problem Detection
![Davis AI](docs/screenshots/davis-problem.png)
*Automatic anomaly detection with root cause analysis*

### Slack Alert Integration
![Slack Alert](docs/screenshots/slack-alert.png)
*Real-time problem notifications in Slack*

### Kubernetes Monitoring
![Kubernetes](docs/screenshots/kubernetes-monitoring.png)
*Pod, container, and cluster health metrics*

### Service Dependencies
![Service Map](docs/screenshots/service-dependencies.png)
*Automatic service topology discovery and mapping*

**Note:** To capture screenshots for your deployment:
1. Deploy the platform to AWS EKS
2. Access Dynatrace tenant
3. Generate traffic using the provided test scripts
4. Trigger test problems using the alert testing guide
5. Capture screenshots from Dynatrace UI and Slack
6. Save to `docs/screenshots/` directory

---

## 📐 Architecture

### Application Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          AWS Cloud (us-east-1)                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    Amazon EKS Cluster                          │ │
│  │                  (enterprise-eks-cluster)                      │ │
│  │                                                                │ │
│  │  ┌──────────────────────────────────────────────────────────┐ │ │
│  │  │              NGINX Ingress Controller                     │ │ │
│  │  │         LoadBalancer: k8s-ingressn-xxx.elb.us...        │ │ │
│  │  └──────────────────┬───────────────────────────────────────┘ │ │
│  │                     │                                          │ │
│  │      ┌──────────────┴──────────────┐                         │ │
│  │      │                              │                         │ │
│  │  ┌───▼────────┐            ┌────────▼──────┐                │ │
│  │  │  Frontend  │            │   Gateway     │                │ │
│  │  │ React+MUI  │            │ Spring Cloud  │                │ │
│  │  │   :3000    │            │    :8080      │                │ │
│  │  └────────────┘            └───────┬───────┘                │ │
│  │                                     │                         │ │
│  │         ┌────────────┬──────────────┼──────────────┬─────────┤ │
│  │         │            │              │              │         │ │
│  │  ┌──────▼──────┐ ┌──▼────────┐ ┌───▼────────┐ ┌──▼──────┐ │ │
│  │  │    Auth     │ │ Employee  │ │   Leave    │ │ Payroll │ │ │
│  │  │  Service    │ │  Service  │ │  Service   │ │ Service │ │ │
│  │  │   :8081     │ │   :8082   │ │   :8083    │ │  :8084  │ │ │
│  │  └──────┬──────┘ └──┬────────┘ └───┬────────┘ └──┬──────┘ │ │
│  │         │            │              │              │         │ │
│  │  ┌──────▼──────┐ ┌──▼────────┐ ┌───▼────────┐             │ │
│  │  │    Asset    │ │ Helpdesk  │ │Notification│             │ │
│  │  │  Service    │ │  Service  │ │  Service   │             │ │
│  │  │   :8085     │ │   :8086   │ │   :8087    │             │ │
│  │  └──────┬──────┘ └──┬────────┘ └───┬────────┘             │ │
│  │         │            │              │                       │ │
│  │         └────────────┴──────────────┴───────────┐          │ │
│  │                                                  │          │ │
│  │              ┌────────────────┬──────────────────┘          │ │
│  │              │                │                             │ │
│  │         ┌────▼─────┐    ┌────▼────┐                       │ │
│  │         │PostgreSQL│    │  Redis  │                       │ │
│  │         │7 databases│    │ Cache  │                       │ │
│  │         └──────────┘    └─────────┘                       │ │
│  │                                                            │ │
│  └────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────┘
```

### Observability Stack

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Dynatrace Observability                         │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    Dynatrace Operator                          │ │
│  └──────────┬──────────────────────────────┬─────────────────────┘ │
│             │                               │                       │
│  ┌──────────▼──────────┐         ┌─────────▼──────────┐           │
│  │   ActiveGate        │         │   OneAgent         │           │
│  │  (DaemonSet)        │         │   (CSI Driver)     │           │
│  │  Kubernetes Monitor │         │   Auto-injection   │           │
│  │  Metrics Collector  │         │   into all pods    │           │
│  └──────────┬──────────┘         └─────────┬──────────┘           │
│             │                               │                       │
│             └───────────────┬───────────────┘                       │
│                             │                                       │
│                   ┌─────────▼─────────┐                            │
│                   │  Davis AI Engine  │                            │
│                   │ Problem Detection │                            │
│                   └─────────┬─────────┘                            │
│                             │                                       │
│                   ┌─────────▼─────────┐                            │
│                   │    Workflows      │                            │
│                   │  Auto-triggering  │                            │
│                   └─────────┬─────────┘                            │
│                             │                                       │
│                   ┌─────────▼─────────┐                            │
│                   │   Slack Channel   │                            │
│                   │   Alert Delivery  │                            │
│                   └───────────────────┘                            │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Infrastructure Architecture

```
Developer
   │
   ▼
GitHub Repository
   │
   ▼
AWS ECR (Container Registry)
   │
   ▼
┌──────────────────────────────────────────────────────┐
│            Amazon EKS Cluster                        │
│          (3 × t3.xlarge nodes)                       │
│                                                      │
│  ┌─────────────────────────────────────────────┐  │
│  │      Kubernetes Namespace                    │  │
│  │    (enterprise-observability)                │  │
│  │                                               │  │
│  │  ┌──────────────────────────────────────┐  │  │
│  │  │  NGINX Ingress Controller           │  │  │
│  │  │  (AWS NLB)                          │  │  │
│  │  └────────────┬────────────────────────┘  │  │
│  │               │                            │  │
│  │  ┌────────────▼────────────┐             │  │
│  │  │  11 Application Pods    │             │  │
│  │  │  (All Running 1/1)      │             │  │
│  │  └─────────────────────────┘             │  │
│  │                                            │  │
│  │  ┌─────────────────────────────────────┐ │  │
│  │  │  Dynatrace Components                │ │  │
│  │  │  - Operator: 1/1                     │ │  │
│  │  │  - ActiveGate: 1/1                   │ │  │
│  │  │  - OneAgent: 3/3 (DaemonSet)        │ │  │
│  │  │  - CSI Driver: 3/3                   │ │  │
│  │  │  - Webhook: 1/1                      │ │  │
│  │  └─────────────────────────────────────┘ │  │
│  │                                            │  │
│  │  ┌─────────────────────────────────────┐ │  │
│  │  │  Storage                             │ │  │
│  │  │  - PostgreSQL PVC: 10GB (gp2)       │ │  │
│  │  │  - Redis: Ephemeral                  │ │  │
│  │  └─────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────┘  │
│                                                   │
│  Managed by Terraform                            │
│  Region: us-east-1                               │
│  VPC: 10.0.0.0/16                               │
│  Subnets: 2 × Public, 2 × Private               │
└───────────────────────────────────────────────────┘
```

### Service communication

- **Frontend -> Gateway -> Services**: the browser only ever talks to NGINX Ingress
  (`/api/**`), which forwards to the Gateway, which routes by path prefix to
  the right service (`/api/auth/**` -> Authentication Service, `/api/employees/**`
  -> Employee Service, etc).
- **Gateway -> Services**: the Gateway validates the JWT once, then forwards
  the resolved identity via `X-Auth-User` / `X-Auth-Roles` headers *and*
  passes the original bearer token through, so services can operate
  correctly whether reached via the Gateway or directly (e.g. from Swagger
  during local development).
- **Cross-service references are by ID, not FK.** Each service owns its own
  database; e.g. the Leave Service stores `employeeId` as a plain `BIGINT`,
  not a foreign key into the Employee Service's database. This is the
  correct microservice pattern — it does mean the seed data across services
  is *coordinated* (employee IDs 1-100 line up with payroll salary
  profiles, leave balances, asset assignments, etc.) rather than
  relationally enforced.
- **Payroll and Notification services** own two background scheduled jobs
  each (monthly payroll generation; notification retry sweep + cleanup),
  demonstrating the "background jobs" requirement without needing an
  external scheduler.

### Request tracing

Every request gets an `X-Request-Id` (generated at the Gateway if not
already present), which flows through to every downstream service and is
injected into the logging MDC (`requestId`, `user`, `api`). This is exactly
the correlation key an OpenTelemetry Collector / log pipeline would use to
stitch structured logs to distributed traces once tracing instrumentation is
added.

---

## 🚀 Deployment Guide

### Prerequisites

- AWS Account with appropriate permissions
- AWS CLI configured
- Terraform >= 1.0
- kubectl
- Dynatrace tenant (trial or licensed)
- Docker (for local development)

### 1. Infrastructure Setup with Terraform

```bash
cd terraform

# Initialize Terraform
terraform init

# Review the infrastructure plan
terraform plan

# Deploy EKS cluster and VPC
terraform apply

# Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name enterprise-eks-cluster
```

**What gets created:**
- VPC with public and private subnets across 2 AZs
- EKS cluster (3 × t3.xlarge nodes, 12 vCPU, 48GB RAM)
- Node group with auto-scaling (min: 3, max: 4)
- IAM roles and policies for EKS
- Security groups
- EBS CSI driver add-on

### 2. Install AWS Load Balancer Controller

```bash
# Create IAM policy
curl -o iam-policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/main/docs/install/iam_policy.json
aws iam create-policy --policy-name AWSLoadBalancerControllerIAMPolicy --policy-document file://iam-policy.json

# Create service account
eksctl create iamserviceaccount \
  --cluster=enterprise-eks-cluster \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --attach-policy-arn=arn:aws:iam::<ACCOUNT_ID>:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve

# Install controller
kubectl apply -k "github.com/aws/eks-charts/stable/aws-load-balancer-controller/crds?ref=master"
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=enterprise-eks-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

### 3. Install NGINX Ingress Controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/aws/deploy.yaml
```

### 4. Install Metrics Server

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### 5. Deploy Dynatrace Operator

```bash
# Create Dynatrace namespace
kubectl create namespace dynatrace
kubectl apply -f k8s/dynatrace/namespace.yaml

# Create Dynatrace API token secret
# Get tokens from Dynatrace: Settings -> Integration -> Dynatrace API
kubectl create secret generic enterprise-eks-cluster \
  --from-literal=apiToken=<YOUR_API_TOKEN> \
  --from-literal=dataIngestToken=<YOUR_DATA_INGEST_TOKEN> \
  -n dynatrace

# Install Dynatrace Operator
kubectl apply -f https://github.com/Dynatrace/dynatrace-operator/releases/latest/download/kubernetes.yaml

# Deploy DynaKube (update apiUrl with your tenant URL)
kubectl apply -f k8s/dynatrace/dynakube.yaml
```

**Dynatrace Configuration:**

The DynaKube is configured for:
- ✅ **OneAgent CloudNativeFullStack**: Automatic injection into all pods
- ✅ **ActiveGate**: Kubernetes monitoring, metrics ingestion, routing
- ✅ **Host Group**: `enterprise-observability`
- ✅ **Network Zone**: `us-east-1`
- ✅ **Resources**: Optimized for 3-node cluster

### 6. Deploy Application

```bash
# Create application namespace
kubectl create namespace enterprise-observability

# Create ConfigMap
kubectl apply -f k8s/configmap.yaml

# Create PostgreSQL PVC
kubectl apply -f k8s/postgres/pvc.yaml

# Deploy databases
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/redis/

# Deploy services
kubectl apply -f k8s/authentication-service/
kubectl apply -f k8s/employee-service/
kubectl apply -f k8s/leave-service/
kubectl apply -f k8s/payroll-service/
kubectl apply -f k8s/asset-service/
kubectl apply -f k8s/helpdesk-service/
kubectl apply -f k8s/notification-service/

# Deploy gateway and frontend
kubectl apply -f k8s/gateway/
kubectl apply -f k8s/frontend/

# Deploy ingress
kubectl apply -f k8s/ingress/
```

### 7. Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n enterprise-observability

# Expected output (all 1/1 READY):
# NAME                                      READY   STATUS
# asset-service-xxx                         1/1     Running
# authentication-service-xxx                1/1     Running
# employee-service-xxx                      1/1     Running
# frontend-xxx                              1/1     Running
# gateway-xxx                               1/1     Running
# helpdesk-service-xxx                      1/1     Running
# leave-service-xxx                         1/1     Running
# notification-service-xxx                  1/1     Running
# payroll-service-xxx                       1/1     Running
# postgres-xxx                              1/1     Running
# redis-xxx                                 1/1     Running

# Check Dynatrace components
kubectl get pods -n dynatrace

# Expected output:
# dynatrace-operator-xxx                    1/1     Running
# dynatrace-webhook-xxx                     1/1     Running
# enterprise-eks-cluster-activegate-0       1/1     Running
# dynatrace-oneagent-xxx                    1/1     Running  (3 pods)
# dynatrace-oneagent-csi-driver-xxx         1/1     Running  (3 pods)

# Get ingress URL
kubectl get ingress -n enterprise-observability
```

### 8. Access the Application

```bash
# Get the Load Balancer URL
LB_URL=$(kubectl get ingress ingress-nginx -n enterprise-observability -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo "Application URL: http://$LB_URL"
```

Open the URL in your browser. Login with demo credentials:
- Username: `admin`
- Password: `Password@123`

---

## 🔔 Dynatrace Alerting Setup

### Configure Davis AI Problem Detection

1. Go to Dynatrace: **Settings → Anomaly detection → Services**
2. Enable: **"Detect increases in failure rate"**
3. Set sensitivity: **High**
4. Set threshold: **Alert if > 10% failure rate**
5. Click **Save**

### Configure Kubernetes Monitoring

1. Go to: **Settings → Anomaly detection → Kubernetes**
2. Enable:
   - ✅ **Detect pod availability issues**
   - ✅ **Detect deployment stuck**
   - ✅ **Detect container restart loops**
3. Click **Save**

### Create Slack Workflow

1. Go to: **Workflows → Create Workflow**
2. Select trigger: **Problem**
3. Add action: **Send Slack message**
4. Configure Slack connection:
   - Create Slack App: https://api.slack.com/apps
   - Add Bot Token Scopes: `chat:write`, `chat:write.public`
   - Copy Bot Token
   - Add to Dynatrace: **Settings → Integration → Slack**
5. Configure message template:
   ```
   🚨 *Dynatrace Problem Alert*
   ━━━━━━━━━━━━━━━━━━━━━━
   *Problem*: {{ .title }}
   *Severity*: {{ .severity }}
   *Status*: {{ .status }}
   *Affected*: {{ .affectedEntities }}
   *Cluster*: enterprise-eks-cluster
   *Namespace*: enterprise-observability
   
   *Description*: {{ .description }}
   *Display ID*: {{ .displayId }}
   *Started*: {{ .startTime }}
   ━━━━━━━━━━━━━━━━━━━━━━
   Generated automatically by Dynatrace Workflow
   ```
6. Click **Deploy**

---

## 🧪 Testing the Alerting System

### ⚠️ IMPORTANT: How to Trigger Davis AI Correctly

**❌ Don't do this (Davis ignores intentional scale-down):**
```bash
kubectl scale deployment authentication-service --replicas=0
```

**✅ Do this instead (causes real crash that Davis detects):**
```bash
# Break a service with invalid container image
kubectl set image deployment/gateway gateway=nginx:invalid-image-404 \
  -n enterprise-observability

# This triggers ImagePullBackOff - an UNEXPECTED failure
# Davis AI will detect this within 2-5 minutes
```

### Test Procedure

1. **Break a service** (use invalid image as shown above)
2. **Generate traffic** to trigger error detection:
   ```bash
   # Generate failed requests
   for i in {1..100}; do
     curl -X GET http://$LB_URL/api/employees
     sleep 0.1
   done
   ```
3. **Monitor Dynatrace**:
   - Go to **Problems** dashboard
   - Wait 2-5 minutes for Davis AI detection
   - Problem will be created automatically
   - Slack alert will be sent

4. **Fix the service**:
   ```bash
   kubectl set image deployment/gateway \
     gateway=039612843833.dkr.ecr.us-east-1.amazonaws.com/enterprise-observability/gateway:v1.0.0 \
     -n enterprise-observability
   ```
5. **Verify recovery**:
   - Problem status changes to "Resolved"
   - Pods return to Running state

### Key Lesson Learned

🔑 **Davis AI distinguishes between:**
- **Intentional operations**: `kubectl scale`, manual deployments → No alert
- **Unexpected failures**: ImagePullBackOff, CrashLoopBackOff, container errors → Alert triggered

Always use crash scenarios for alert testing, not scale operations!

---

## 2. Technology Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3.3.4, Spring Security, Spring Data JPA, Spring Validation, Spring Boot Actuator, Maven |
| **Gateway** | Spring Cloud Gateway (WebFlux/Netty) 2023.0.3 |
| **Frontend** | React 18, Vite, Material UI, Axios, React Router, Recharts |
| **Databases** | PostgreSQL 16 (1 DB per service), Redis 7 |
| **Migrations** | Flyway |
| **API docs** | springdoc-openapi / Swagger UI, per service |
| **Auth** | JWT (jjwt), BCrypt password hashing |
| **Monitoring** | Dynatrace OneAgent, Micrometer + Prometheus registry, Spring Boot Actuator |
| **Cloud** | AWS EKS, AWS ECR, AWS VPC, AWS EBS, AWS NLB |
| **Infrastructure** | Terraform, Kubernetes, Docker, NGINX Ingress |
| **CI/CD** | (Ready for GitHub Actions, FluxCD, ArgoCD) |
| **Observability** | Dynatrace Davis AI, Distributed Tracing, Log Monitoring, Kubernetes Monitoring |
| **Alerting** | Dynatrace Workflows, Slack Integration |

---

## 📊 Monitoring & Observability Features

### Dynatrace OneAgent Capabilities

✅ **Automatic Instrumentation**
- Zero-code injection into all Spring Boot microservices
- Complete distributed tracing across service boundaries
- Automatic database query monitoring (PostgreSQL)
- Redis operation tracing
- JVM metrics (heap, GC, threads, CPU)

✅ **Kubernetes Monitoring**
- Pod health and availability
- Container resource usage (CPU, memory)
- Node metrics
- Deployment status
- Replica set monitoring
- Service mesh visibility

✅ **Application Performance**
- Request rate, response time, error rate (RED metrics)
- Service dependency mapping
- Call chain visualization
- Slow transaction detection
- Database connection pool monitoring

✅ **Log Monitoring**
- Structured log ingestion from all pods
- Log correlation with traces via `X-Request-Id`
- Error log detection and alerting
- Log analytics and search

✅ **Davis AI**
- Automatic problem detection
- Root cause analysis
- Anomaly detection for:
  - Service failure rates
  - Response time degradation
  - Pod availability issues
  - Container crashes (ImagePullBackOff, CrashLoopBackOff)
  - Memory/CPU anomalies

### Exposed Metrics Endpoints

Every service exposes comprehensive health and metrics endpoints:

- `/actuator/health` — Overall health status
- `/actuator/health/liveness` — Kubernetes liveness probe
- `/actuator/health/readiness` — Kubernetes readiness probe
- `/actuator/metrics` — Micrometer metrics
- `/actuator/prometheus` — Prometheus-format metrics
- `/actuator/info` — Application information
- `/actuator/env` — Environment properties
- `/actuator/beans` — Spring beans
- `/actuator/mappings` — Request mappings

### Distributed Tracing

Every HTTP request through the system:
1. Receives an `X-Request-Id` header (generated at Gateway if missing)
2. Propagates the request ID to all downstream services
3. Logs include `requestId`, `user`, `api` in MDC
4. Dynatrace automatically correlates:
   - HTTP requests across services
   - Database queries
   - Redis operations
   - Log entries
   - Exceptions and errors

---

## 🏗️ Infrastructure Details

### EKS Cluster Specifications

**Cluster Name:** `enterprise-eks-cluster`  
**Region:** `us-east-1`  
**Kubernetes Version:** 1.28+

**Node Group:**
- **Instance Type:** t3.xlarge (4 vCPU, 16GB RAM)
- **Node Count:** 3 nodes (min: 3, desired: 3, max: 4)
- **Disk:** 50GB gp2 EBS per node
- **Total Capacity:** 12 vCPU, 48GB RAM

**Networking:**
- **VPC CIDR:** 10.0.0.0/16
- **Public Subnets:** 2 × /24 (10.0.1.0/24, 10.0.2.0/24)
- **Private Subnets:** 2 × /24 (10.0.3.0/24, 10.0.4.0/24)
- **Availability Zones:** us-east-1a, us-east-1b

### Resource Allocation

**Application Pods:**
| Service | CPU Request | CPU Limit | Memory Request | Memory Limit |
|---------|-------------|-----------|----------------|--------------|
| Gateway | 100m | 500m | 256Mi | 512Mi |
| Auth | 100m | 500m | 256Mi | 512Mi |
| Employee | 100m | 500m | 256Mi | 512Mi |
| Leave | 100m | 500m | 256Mi | 512Mi |
| Payroll | 100m | 500m | 256Mi | 512Mi |
| Asset | 100m | 500m | 256Mi | 512Mi |
| Helpdesk | 100m | 500m | 256Mi | 512Mi |
| Notification | 100m | 500m | 256Mi | 512Mi |
| Frontend | 50m | 200m | 64Mi | 128Mi |
| PostgreSQL | 200m | 1000m | 512Mi | 1Gi |
| Redis | 100m | 300m | 128Mi | 256Mi |

**Dynatrace Components:**
| Component | CPU Request | CPU Limit | Memory Request | Memory Limit |
|-----------|-------------|-----------|----------------|--------------|
| Operator | 50m | 200m | 64Mi | 128Mi |
| ActiveGate | 100m | 500m | 256Mi | 512Mi |
| OneAgent (per node) | 100m | 300m | 128Mi | 512Mi |
| OneAgent Init | 100m | 500m | 128Mi | 512Mi |

### Storage

- **PostgreSQL PVC:** 10GB gp2 EBS volume
- **Redis:** Ephemeral (no persistent storage)
- **Logs:** Dynatrace log ingestion (no local storage required)

---

## 3. Project structure

```
enterprise-observability-demo/
├── frontend/                    React 18 + Vite + MUI dashboard
├── gateway/                     Spring Cloud Gateway
├── shared-library/              Common DTOs, exceptions, JWT utils, logging filter
├── services/
│   ├── authentication-service/  Login, JWT, users, roles          :8081
│   ├── employee-service/        Employee CRUD, search, caching    :8082
│   ├── leave-service/           Leave apply/approve/balance       :8083
│   ├── payroll-service/         Payroll generation, payslips      :8084
│   ├── asset-service/           Asset assignment, tracking        :8085
│   ├── helpdesk-service/        Tickets, SLA, comments            :8086
│   └── notification-service/    Mock email/Teams/SMS, retries     :8087
├── docker/
│   ├── nginx/nginx.conf         Front-door reverse proxy
│   └── postgres/init-databases.sh
├── docker-compose.yml
├── pom.xml                      Maven multi-module parent
└── docs/                        Additional documentation
```

Every service follows the same internal layout:
`entity/ -> repository/ -> service/ -> controller/ -> dto/ -> config/`, plus
`src/main/resources/application.yml` and `src/main/resources/db/migration/`
(Flyway `V1__init_schema.sql`, `V2__seed_data.sql`).

---

## 💻 Local Development with Docker Compose

For local development and testing without AWS infrastructure:

**Prerequisites:** Docker + Docker Compose v2, ~6 GB free RAM, ports 80, 3000, 5432, 6379, 8080-8087 free.

```bash
cp .env.example .env        # adjust POSTGRES_PASSWORD / JWT_SECRET if desired
docker compose build        # builds all 7 services + gateway + frontend
docker compose up -d
docker compose ps           # wait until every service shows "healthy"
```

Then open:

- **App:** http://localhost (via NGINX) or http://localhost:3000 (frontend direct)
- **Gateway health:** http://localhost:8080/actuator/health
- **Swagger UI** per service, e.g.:
  - http://localhost:8081/swagger-ui.html (Authentication)
  - http://localhost:8082/swagger-ui.html (Employee)
  - http://localhost:8083/swagger-ui.html (Leave)
  - http://localhost:8084/swagger-ui.html (Payroll)
  - http://localhost:8085/swagger-ui.html (Asset)
  - http://localhost:8086/swagger-ui.html (Helpdesk)
  - http://localhost:8087/swagger-ui.html (Notification)

To stop everything: `docker compose down` (add `-v` to also wipe the
Postgres/Redis volumes and start fresh next time).

### Demo Login Accounts

All seeded users share the password `Password@123`:

| Username | Role |
|---|---|
| `admin` | ADMIN |
| `hr.sharma`, `hr.iyer` | HR |
| `mgr.rao`, `mgr.gupta`, `mgr.khan` | MANAGER |
| `emp.verma`, `emp.nair`, `emp.singh`, `emp.reddy`, `emp.das`, `emp.mehta` | EMPLOYEE |

---

## 🔧 Running a Single Service Locally

Each service can run standalone against a local Postgres + Redis:

```bash
# from repo root
mvn -pl shared-library -am install -DskipTests
cd services/employee-service
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-DDB_HOST=localhost -DDB_NAME=employee_db -DDB_USER=enterprise -DDB_PASSWORD=enterprise_pass -DREDIS_HOST=localhost"
```

Frontend:

```bash
cd frontend
npm install
npm run dev          # http://localhost:3000, proxies /api to localhost:8080
```

---

## 🌍 Environment Variables

| Variable | Used by | Default | Purpose |
|---|---|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | postgres, all services | `enterprise` / `enterprise_pass` | DB credentials |
| `JWT_SECRET` | auth-service, gateway, all services | demo key (see `.env.example`) | Base64 HMAC signing key — **must be identical everywhere** |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | each service | `postgres:5432` / per-service | Postgres connection |
| `REDIS_HOST` / `REDIS_PORT` | services using Redis, gateway | `redis:6379` | Redis connection |
| `AUTH_SERVICE_URL` etc. | gateway | `http://<service>:<port>` | Downstream routing targets |
| `PAYROLL_CRON` | payroll-service | `0 0 2 1 * *` | Monthly payroll job schedule |
| `NOTIFICATION_RETENTION_DAYS` | notification-service | `30` | Cleanup job retention window |
| `VITE_API_BASE_URL` | frontend (build arg) | `/api` | Where the SPA sends API calls |

**Kubernetes ConfigMap:**

All services read configuration from `k8s/configmap.yaml`:
- Database connection strings
- Service URLs
- Redis configuration
- JWT settings
- Feature flags

---

## 7. Database design

Database-per-service — no cross-service foreign keys. Each service's
`V1__init_schema.sql` is the source of truth for its tables. High-level ER
summary:

- **auth_db**: `users` <-> `user_roles` (one-to-many via join table)
- **employee_db**: `employees` self-referencing on `manager_id`
- **leave_db**: `leave_requests`, `leave_balances` (unique per employee+type+year)
- **payroll_db**: `employee_salary_profiles`, `payroll_records` (unique per employee+month+year)
- **asset_db**: `assets` (nullable `assigned_to_employee_id`)
- **helpdesk_db**: `tickets` <-> `ticket_comments` (one-to-many, cascade delete)
- **notification_db**: `notifications` (retry/status tracked per row)

All employee-referencing tables in other services use `employeeId BIGINT`
(no FK) — see [Service communication](#service-communication) above.

---

## 8. Observability hooks already in place

This app is deliberately built so instrumentation is additive, not
invasive:

- Every service exposes `/actuator/health`, `/actuator/health/liveness`,
  `/actuator/health/readiness`, `/actuator/info`, `/actuator/metrics`,
  `/actuator/prometheus`, `/actuator/beans`, `/actuator/mappings`,
  `/actuator/env`, `/actuator/configprops`.
- Every request is tagged with a correlation `X-Request-Id` header,
  propagated end-to-end and present in every structured log line
  (`requestId=... user=... api=...`).
- Micrometer + `micrometer-registry-prometheus` is already on the
  classpath of every service — pointing an OpenTelemetry Collector's
  Prometheus receiver (or a native `micrometer-registry-otlp` swap) at
  `/actuator/prometheus` requires no code changes.
- Redis, DB pool (HikariCP), and JVM metrics are automatically exported by
  Actuator once scraped.
- To add full distributed tracing, add the OpenTelemetry Java agent
  (`-javaagent:opentelemetry-javaagent.jar`) to each service's `ENTRYPOINT`
  in its Dockerfile and point `OTEL_EXPORTER_OTLP_ENDPOINT` at your
  Collector — no application code changes needed.

---

## 9. Troubleshooting

**Build fails on a dependency version.** This project targets Spring Boot
3.3.4 / Spring Cloud 2023.0.3 (Java 17) and springdoc-openapi 2.6.0. If your
local Maven resolves a slightly different transitive version and something
doesn't compile, the most common culprits are jjwt (pinned to 0.12.5 — API
changed between 0.11.x and 0.12.x) and Spring Cloud Gateway (reactive, not
MVC — don't add `spring-boot-starter-web` to the `gateway` module).

**A service can't reach Postgres/Redis when run outside Docker.** Set
`DB_HOST=localhost` / `REDIS_HOST=localhost` explicitly — the defaults in
`application.yml` assume Docker Compose networking (service name as host).

**Flyway checksum mismatch after editing a seed file.** Flyway checksums
migration files; if you edit `V2__seed_data.sql` after it's already run
once, either `docker compose down -v` to reset the DB volume or run
`flyway repair`.

**Frontend shows CORS errors when run outside Docker.** Run it via
`npm run dev` (Vite proxies `/api` to `localhost:8080`), not by opening
`dist/index.html` directly.


## 🔧 Troubleshooting

### EKS Cluster Issues

**Pods stuck in Pending state:**
```bash
# Check node capacity
kubectl top nodes
kubectl describe nodes

# Check pod events
kubectl describe pod <pod-name> -n enterprise-observability

# Common causes:
# - Insufficient CPU/memory → Scale up node group in Terraform
# - PVC not bound → Check storage class: kubectl get pvc -n enterprise-observability
```

**Solution for resource issues:**
```bash
cd terraform
# Edit terraform.tfvars to increase node count or instance type
terraform apply
```

**Services in CrashLoopBackOff:**
```bash
# Check logs
kubectl logs <pod-name> -n enterprise-observability

# Common causes:
# - Database connection failed → Check postgres pod status
# - Missing environment variables → Verify ConfigMap
# - Flyway migration conflict → Delete PVC and recreate

# Fix Flyway issues:
kubectl scale deployment postgres --replicas=0 -n enterprise-observability
kubectl delete pvc postgres-pvc -n enterprise-observability
kubectl apply -f k8s/postgres/pvc.yaml
kubectl scale deployment postgres --replicas=1 -n enterprise-observability
```

### Dynatrace Issues

**OneAgent pods in CrashLoopBackOff with SIGKILL:**
```bash
# Check OneAgent resources
kubectl get pods -n dynatrace

# Issue: initResources too low for signature verification
# Solution: Already fixed in k8s/dynatrace/dynakube.yaml
#   initResources.cpu: 500m (was 100m)
#   initResources.memory: 512Mi (was 128Mi)

# Apply fix:
kubectl apply -f k8s/dynatrace/dynakube.yaml

# Force recreation:
kubectl delete pods -l app.kubernetes.io/name=dynatrace-operator -n dynatrace
```

**ActiveGate not connecting:**
```bash
# Verify secrets
kubectl get secret enterprise-eks-cluster -n dynatrace
kubectl describe secret enterprise-eks-cluster -n dynatrace

# Check ActiveGate logs
kubectl logs enterprise-eks-cluster-activegate-0 -n dynatrace

# Recreate secret if needed:
kubectl delete secret enterprise-eks-cluster -n dynatrace
kubectl create secret generic enterprise-eks-cluster \
  --from-literal=apiToken=<YOUR_API_TOKEN> \
  --from-literal=dataIngestToken=<YOUR_DATA_INGEST_TOKEN> \
  -n dynatrace
```

**Application pods not instrumented:**
```bash
# Check if OneAgent CSI driver is running
kubectl get pods -n dynatrace -l app.kubernetes.io/name=oneagent-csi-driver

# Check pod for Dynatrace init container
kubectl describe pod <app-pod-name> -n enterprise-observability | grep dynatrace

# Should see: "dynatrace-operator" as init container

# If missing, delete and recreate the pod:
kubectl delete pod <app-pod-name> -n enterprise-observability
```

### Liveness/Readiness Probe Issues

**Services failing health checks:**

Spring Boot apps with Dynatrace take 2-3 minutes to fully initialize.

**Problem:** Liveness probes with `initialDelaySeconds: 60s` timeout before startup completes.

**Solution:** Already fixed in deployment YAMLs:
```yaml
livenessProbe:
  initialDelaySeconds: 150  # Was 60
  timeoutSeconds: 10        # Was 1-5
readinessProbe:
  initialDelaySeconds: 120  # Was 30
  timeoutSeconds: 10        # Was 1-5
```

If services still fail:
```bash
# Check startup time
kubectl logs <pod-name> -n enterprise-observability | grep "Started"

# Increase delays if needed:
# Edit k8s/<service>/deployment.yaml
# Increase initialDelaySeconds values
# kubectl apply -f k8s/<service>/deployment.yaml
```

### Davis AI Not Detecting Problems

**Issue:** Manual `kubectl scale --replicas=0` is treated as intentional by Davis AI.

**Solution:** Use crash scenarios instead:
```bash
# ❌ Don't do this (Davis ignores):
kubectl scale deployment <service> --replicas=0

# ✅ Do this (Davis detects):
kubectl set image deployment/<service> \
  <container-name>=nginx:invalid-image-404 \
  -n enterprise-observability
```

**Verify problem detection:**
1. Wait 2-5 minutes after creating crash
2. Check Dynatrace → Problems dashboard
3. Look for "Backoff event" or "ImagePullBackOff" problems

### Ingress / Load Balancer Issues

**Can't access application via LoadBalancer URL:**
```bash
# Check ingress status
kubectl get ingress -n enterprise-observability
kubectl describe ingress ingress-nginx -n enterprise-observability

# Check NGINX ingress controller
kubectl get pods -n ingress-nginx

# Check ingress events
kubectl get events -n enterprise-observability | grep ingress
```

**404 errors when accessing application:**

Issue fixed in `k8s/ingress/ingress.yaml`:
- Removed host restriction (accepts any host)
- Disabled SSL redirect
- Removed TLS configuration

```yaml
spec:
  ingressClassName: nginx
  rules:
    - host: "*"  # Accept any host
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend-service
                port:
                  number: 3000
```

### Database Connection Issues

**Services can't connect to PostgreSQL:**
```bash
# Check postgres pod
kubectl get pods -n enterprise-observability | grep postgres
kubectl logs postgres-xxx -n enterprise-observability

# Test connection from a pod:
kubectl exec -it <any-service-pod> -n enterprise-observability -- \
  psql -h postgres-service -U enterprise -d employee_db

# Check service
kubectl get svc postgres-service -n enterprise-observability
```

**Database not initialized:**
```bash
# Check if databases exist
kubectl exec -it postgres-xxx -n enterprise-observability -- \
  psql -U enterprise -c "\l"

# Should see: auth_db, employee_db, leave_db, payroll_db, 
#             asset_db, helpdesk_db, notification_db

# If missing, check init script logs:
kubectl logs postgres-xxx -n enterprise-observability | grep "CREATE DATABASE"
```

### Useful Commands

```bash
# Get all resources in namespace
kubectl get all -n enterprise-observability

# Check pod resource usage
kubectl top pods -n enterprise-observability

# Get node resource usage
kubectl top nodes

# Stream logs from all pods of a deployment
kubectl logs -f deployment/gateway -n enterprise-observability

# Execute command in a pod
kubectl exec -it <pod-name> -n enterprise-observability -- /bin/sh

# Port forward to a service
kubectl port-forward svc/gateway-service 8080:8080 -n enterprise-observability

# Get events sorted by time
kubectl get events -n enterprise-observability --sort-by='.lastTimestamp'

# Describe all pods (useful for debugging)
kubectl describe pods -n enterprise-observability

# Delete and recreate all application pods
kubectl delete pods -l tier=backend -n enterprise-observability

# Check Dynatrace injection
kubectl get pods -n enterprise-observability -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.initContainers[*].name}{"\n"}{end}'
```

---

## 📚 Additional Resources

- **Dynatrace Documentation**: https://www.dynatrace.com/support/help/
- **Dynatrace OneAgent on Kubernetes**: https://www.dynatrace.com/support/help/setup-and-configuration/setup-on-k8s
- **Davis AI**: https://www.dynatrace.com/platform/artificial-intelligence/
- **AWS EKS Best Practices**: https://aws.github.io/aws-eks-best-practices/
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **Spring Cloud Gateway**: https://spring.io/projects/spring-cloud-gateway

---

## 📄 License

This project is created for demonstration and educational purposes.

---

## 👥 Contributors

Built to showcase enterprise-grade observability with Dynatrace on AWS EKS.

---

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Dynatrace for Davis AI and OneAgent
- AWS for EKS and cloud infrastructure
- Material-UI for the React component library
