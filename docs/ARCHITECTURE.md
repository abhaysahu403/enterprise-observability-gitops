# Enterprise Observability Platform - Architecture Documentation

## Table of Contents
- [System Overview](#system-overview)
- [Architecture Diagrams](#architecture-diagrams)
- [Component Details](#component-details)
- [Data Flow](#data-flow)
- [Technology Stack](#technology-stack)
- [Network Architecture](#network-architecture)
- [Security Architecture](#security-architecture)
- [Scalability & High Availability](#scalability--high-availability)

---

## System Overview

The Enterprise Observability Platform is a cloud-native, microservices-based application running on AWS EKS (Elastic Kubernetes Service). It demonstrates enterprise-grade observability using Dynatrace, OpenTelemetry, and GitOps practices.

### Key Characteristics
- **Microservices Architecture**: 7 independent Spring Boot services
- **Cloud-Native**: Containerized, orchestrated by Kubernetes
- **Observable**: Full-stack monitoring with Dynatrace OneAgent
- **GitOps-Driven**: Continuous deployment via FluxCD
- **Production-Ready**: Health checks, resource limits, auto-scaling

---

## Architecture Diagrams

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          GitHub Repository                               │
│                  (Source of Truth for GitOps)                           │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           FluxCD                                         │
│              (Continuous Deployment Controller)                         │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        AWS EKS Cluster                                   │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │                    Kubernetes Namespaces                        │    │
│  │                                                                 │    │
│  │  ┌─────────────────────────────────────────────────────────┐  │    │
│  │  │         enterprise-observability namespace              │  │    │
│  │  │                                                          │  │    │
│  │  │  ┌──────────────┐      ┌──────────────┐               │  │    │
│  │  │  │   Gateway    │◄─────┤   Frontend   │               │  │    │
│  │  │  │  (API GW)    │      │   (React)    │               │  │    │
│  │  │  └──────┬───────┘      └──────────────┘               │  │    │
│  │  │         │                                               │  │    │
│  │  │         ├───────────┬───────────┬───────────┐          │  │    │
│  │  │         ▼           ▼           ▼           ▼          │  │    │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │  │    │
│  │  │  │   Auth   │ │Employee  │ │  Leave   │ │ Payroll  │ │  │    │
│  │  │  │ Service  │ │ Service  │ │ Service  │ │ Service  │ │  │    │
│  │  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ │  │    │
│  │  │       │            │            │            │         │  │    │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐              │  │    │
│  │  │  │  Asset   │ │Notification│ │Helpdesk  │              │  │    │
│  │  │  │ Service  │ │  Service   │ │ Service  │              │  │    │
│  │  │  └────┬─────┘ └─────┬──────┘ └────┬─────┘              │  │    │
│  │  │       │             │             │                     │  │    │
│  │  │       └─────────────┼─────────────┘                     │  │    │
│  │  │                     ▼                                   │  │    │
│  │  │         ┌──────────────────────┐                        │  │    │
│  │  │         │     PostgreSQL       │                        │  │    │
│  │  │         │  (Database per       │                        │  │    │
│  │  │         │   Service)           │                        │  │    │
│  │  │         └──────────────────────┘                        │  │    │
│  │  │                     ▲                                   │  │    │
│  │  │         ┌───────────┴───────────┐                       │  │    │
│  │  │         │       Redis           │                       │  │    │
│  │  │         │   (Cache & Sessions)  │                       │  │    │
│  │  │         └───────────────────────┘                       │  │    │
│  │  └─────────────────────────────────────────────────────────┘  │    │
│  │                                                                │    │
│  │  ┌─────────────────────────────────────────────────────────┐  │    │
│  │  │            Dynatrace Namespace                          │  │    │
│  │  │  ┌──────────────┐      ┌──────────────┐               │  │    │
│  │  │  │  OneAgent    │      │ ActiveGate   │               │  │    │
│  │  │  │  (Monitoring)│      │  (Cluster)   │               │  │    │
│  │  │  └──────────────┘      └──────────────┘               │  │    │
│  │  └─────────────────────────────────────────────────────────┘  │    │
│  │                                                                │    │
│  │  ┌─────────────────────────────────────────────────────────┐  │    │
│  │  │            flux-system Namespace                        │  │    │
│  │  │  ┌────────────┐ ┌────────────┐ ┌────────────┐         │  │    │
│  │  │  │  Source    │ │ Kustomize  │ │    Helm    │         │  │    │
│  │  │  │Controller  │ │ Controller │ │ Controller │         │  │    │
│  │  │  └────────────┘ └────────────┘ └────────────┘         │  │    │
│  │  └─────────────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Dynatrace SaaS Platform                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │   Davis AI   │  │  Dashboards  │  │     SLOs     │                 │
│  │  (Problems)  │  │   (Metrics)  │  │   (Targets)  │                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
└─────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Slack Notifications                                │
│                  (Real-time Problem Alerts)                             │
└─────────────────────────────────────────────────────────────────────────┘
```

### Network Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          AWS Region (us-east-1)                         │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │                    VPC (10.0.0.0/16)                            │    │
│  │                                                                 │    │
│  │  ┌───────────────────────────┐  ┌───────────────────────────┐ │    │
│  │  │  Public Subnet (AZ-1)     │  │  Public Subnet (AZ-2)     │ │    │
│  │  │     10.0.3.0/24           │  │     10.0.4.0/24           │ │    │
│  │  │                           │  │                           │ │    │
│  │  │  ┌─────────────────────┐  │  │  ┌─────────────────────┐  │ │    │
│  │  │  │   NAT Gateway       │  │  │  │   NAT Gateway       │  │ │    │
│  │  │  └─────────────────────┘  │  │  └─────────────────────┘  │ │    │
│  │  │                           │  │                           │ │    │
│  │  │  ┌─────────────────────┐  │  │  ┌─────────────────────┐  │ │    │
│  │  │  │  Network Load       │  │  │  │  Network Load       │  │ │    │
│  │  │  │  Balancer           │  │  │  │  Balancer           │  │ │    │
│  │  │  └─────────────────────┘  │  │  └─────────────────────┘  │ │    │
│  │  └───────────────────────────┘  └───────────────────────────┘ │    │
│  │                  │                           │                 │    │
│  │                  │    Internet Gateway       │                 │    │
│  │                  └───────────┬───────────────┘                 │    │
│  │                              │                                 │    │
│  │  ┌───────────────────────────┐  ┌───────────────────────────┐ │    │
│  │  │  Private Subnet (AZ-1)    │  │  Private Subnet (AZ-2)    │ │    │
│  │  │     10.0.1.0/24           │  │     10.0.2.0/24           │ │    │
│  │  │                           │  │                           │ │    │
│  │  │  ┌─────────────────────┐  │  │  ┌─────────────────────┐  │ │    │
│  │  │  │   EKS Worker Node   │  │  │  │   EKS Worker Node   │  │ │    │
│  │  │  │    (t3.large)       │  │  │  │    (t3.large)       │  │ │    │
│  │  │  │                     │  │  │  │                     │  │ │    │
│  │  │  │  ┌──────────────┐   │  │  │  │  ┌──────────────┐   │  │ │    │
│  │  │  │  │  Microservice│   │  │  │  │  │  Microservice│   │  │ │    │
│  │  │  │  │     Pods     │   │  │  │  │  │     Pods     │   │  │ │    │
│  │  │  │  └──────────────┘   │  │  │  │  └──────────────┘   │  │ │    │
│  │  │  └─────────────────────┘  │  │  └─────────────────────┘  │ │    │
│  │  │                           │  │                           │ │    │
│  │  │  ┌─────────────────────┐  │  │  ┌─────────────────────┐  │ │    │
│  │  │  │   EKS Worker Node   │  │  │  │   EKS Worker Node   │  │ │    │
│  │  │  │    (t3.large)       │  │  │  │    (t3.large)       │  │ │    │
│  │  │  └─────────────────────┘  │  │  └─────────────────────┘  │ │    │
│  │  └───────────────────────────┘  └───────────────────────────┘ │    │
│  │                                                                 │    │
│  │  ┌──────────────────────────────────────────────────────────┐  │    │
│  │  │              EKS Control Plane                            │  │    │
│  │  │          (Managed by AWS in AWS VPC)                      │  │    │
│  │  └──────────────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │                    Amazon ECR                                   │    │
│  │              (Private Container Registry)                       │    │
│  └────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

### Observability Data Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Application Layer                             │
│                                                                      │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐         │
│  │  Service A   │───▶│  Service B   │───▶│  Service C   │         │
│  │              │    │              │    │              │         │
│  │ OpenTelemetry│    │ OpenTelemetry│    │ OpenTelemetry│         │
│  │   SDK        │    │   SDK        │    │   SDK        │         │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘         │
│         │                   │                   │                  │
│         │  Traces, Metrics, Logs                │                  │
│         └───────────────────┼───────────────────┘                  │
└─────────────────────────────┼──────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Dynatrace OneAgent                                │
│              (Injected into every pod via Operator)                 │
│                                                                      │
│  • Automatic code instrumentation                                   │
│  • Process monitoring                                               │
│  • Network monitoring                                               │
│  • Log injection with context                                       │
└─────────────────────────────┬───────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Dynatrace ActiveGate                              │
│              (Cluster monitoring & data routing)                    │
│                                                                      │
│  • Kubernetes API monitoring                                        │
│  • Cluster events                                                   │
│  • Resource metrics (CPU, Memory, Disk)                             │
│  • Network topology                                                 │
└─────────────────────────────┬───────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  Dynatrace SaaS Platform                             │
│                                                                      │
│  ┌────────────────────┐  ┌────────────────────┐                    │
│  │   Data Ingestion   │  │   Data Processing  │                    │
│  │   - Traces         │  │   - Correlation    │                    │
│  │   - Metrics        │  │   - Aggregation    │                    │
│  │   - Logs           │  │   - AI Analysis    │                    │
│  │   - Events         │  └────────────────────┘                    │
│  └────────────────────┘                                             │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │                     Davis AI Engine                         │    │
│  │                                                             │    │
│  │  • Anomaly Detection                                        │    │
│  │  • Root Cause Analysis                                      │    │
│  │  • Problem Correlation                                      │    │
│  │  • Impact Analysis                                          │    │
│  └────────────────────────────────────────────────────────────┘    │
│                                ▼                                     │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │                   Problem Detection                         │    │
│  │                                                             │    │
│  │  • ImagePullBackOff detected                               │    │
│  │  • High error rate detected                                │    │
│  │  • Slow response time detected                             │    │
│  │  • Resource saturation detected                            │    │
│  └────────────────────────────────────────────────────────────┘    │
└─────────────────────────────┬───────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Dynatrace Workflow                                │
│              (Automated Problem Response)                           │
│                                                                      │
│  Trigger: Problem Detected                                          │
│  Action:  Send Slack Notification                                   │
│                                                                      │
│  Payload:                                                            │
│  - Problem Title                                                     │
│  - Severity                                                          │
│  - Affected Services                                                 │
│  - Root Cause                                                        │
│  - Link to Dynatrace                                                 │
└─────────────────────────────┬───────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Slack Channel                                    │
│              Real-time team notification                            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Component Details

### Microservices

#### 1. Gateway Service
- **Technology**: Spring Cloud Gateway
- **Port**: 8080
- **Responsibilities**:
  - API routing to backend services
  - JWT token validation
  - Rate limiting
  - Request/response logging
  - CORS handling
- **Dependencies**: Redis (for rate limiting)

#### 2. Authentication Service
- **Technology**: Spring Boot + Spring Security
- **Port**: 8081
- **Responsibilities**:
  - User authentication (login/logout)
  - JWT token generation
  - Password encryption (BCrypt)
  - User session management
- **Database**: PostgreSQL (users, roles, permissions)

#### 3. Employee Service
- **Technology**: Spring Boot + JPA
- **Port**: 8082
- **Responsibilities**:
  - Employee CRUD operations
  - Employee search and filtering
  - Department management
  - Designation management
- **Database**: PostgreSQL (employees, departments, designations)
- **Cache**: Redis (employee data)

#### 4. Leave Service
- **Technology**: Spring Boot + JPA
- **Port**: 8083
- **Responsibilities**:
  - Leave application
  - Leave approval workflow
  - Leave balance tracking
  - Leave history
- **Database**: PostgreSQL (leaves, leave_balances)

#### 5. Payroll Service
- **Technology**: Spring Boot + JPA
- **Port**: 8084
- **Responsibilities**:
  - Payroll generation
  - Salary calculation
  - Tax computation
  - Payslip generation
- **Database**: PostgreSQL (payroll, salary_components)

#### 6. Asset Service
- **Technology**: Spring Boot + JPA
- **Port**: 8085
- **Responsibilities**:
  - Asset inventory management
  - Asset assignment/return
  - Asset tracking
  - Maintenance scheduling
- **Database**: PostgreSQL (assets, assignments)

#### 7. Notification Service
- **Technology**: Spring Boot + JPA
- **Port**: 8086
- **Responsibilities**:
  - Multi-channel notifications (email, SMS, push)
  - Notification templates
  - Delivery tracking
  - Retry logic
- **Database**: PostgreSQL (notifications, templates)

#### 8. Helpdesk Service
- **Technology**: Spring Boot + JPA
- **Port**: 8087
- **Responsibilities**:
  - Ticket creation
  - Ticket assignment
  - SLA tracking
  - Comment threads
- **Database**: PostgreSQL (tickets, comments)

### Frontend

#### React Application
- **Technology**: React 18, Material-UI, React Router
- **Port**: 80 (nginx)
- **Features**:
  - Employee dashboard
  - Leave management UI
  - Ticket creation
  - Responsive design
  - Dark/Light theme
- **Build**: Vite bundler

### Infrastructure Components

#### PostgreSQL
- **Version**: 14
- **Deployment**: StatefulSet with persistent volume
- **Databases**: One per microservice
- **Backup**: EBS snapshots

#### Redis
- **Version**: 7-alpine
- **Deployment**: Deployment with persistent volume
- **Use Cases**:
  - Session storage
  - Rate limiting
  - Employee data caching

#### NGINX Ingress Controller
- **Version**: 1.9.4
- **Responsibilities**:
  - Layer 7 load balancing
  - SSL termination (optional)
  - Path-based routing

---

## Technology Stack

### Backend
| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.3.4 |
| API Gateway | Spring Cloud Gateway | 4.1.5 |
| Security | Spring Security | 6.3.3 |
| ORM | Spring Data JPA | 3.3.4 |
| Database | PostgreSQL | 14 |
| Cache | Redis | 7 |
| Build Tool | Maven | 3.9.x |
| OpenTelemetry | Java Agent | 1.32.0 |

### Frontend
| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | React | 18.2.0 |
| UI Library | Material-UI | 5.14.x |
| Routing | React Router | 6.26.x |
| State Management | Context API | - |
| Build Tool | Vite | 5.4.x |
| HTTP Client | Axios | 1.7.x |

### Infrastructure
| Component | Technology | Version |
|-----------|-----------|---------|
| Cloud Provider | AWS | - |
| Container Orchestration | Kubernetes (EKS) | 1.31 |
| IaC | Terraform | 1.5.x |
| Container Runtime | containerd | 1.7.x |
| Load Balancer | AWS NLB | - |
| Ingress | NGINX Ingress | 1.9.4 |
| Container Registry | Amazon ECR | - |

### Observability
| Component | Technology | Version |
|-----------|-----------|---------|
| APM | Dynatrace OneAgent | Latest |
| Cluster Monitoring | Dynatrace ActiveGate | Latest |
| AI Engine | Davis AI | SaaS |
| Instrumentation | OpenTelemetry | 1.32.0 |
| Log Management | Dynatrace Logs | SaaS |
| Dashboards | Dynatrace | SaaS |

### GitOps
| Component | Technology | Version |
|-----------|-----------|---------|
| CD Tool | FluxCD | 2.2.3 |
| Source Control | GitHub | - |
| Image Scanning | Dynatrace | - |

---

## Data Flow

### User Request Flow

1. **User** → Frontend (React App)
2. **Frontend** → Gateway Service (JWT token in header)
3. **Gateway** → Validates JWT → Routes to backend service
4. **Backend Service** → Processes request → Queries PostgreSQL/Redis
5. **Backend Service** → Returns response to Gateway
6. **Gateway** → Returns response to Frontend
7. **Frontend** → Displays result to user

### Distributed Tracing Flow

1. **Gateway** generates trace ID and span ID
2. **Gateway** propagates context via HTTP headers (W3C Trace Context)
3. **Backend Service** extracts context and creates child span
4. **Backend Service** propagates to database call
5. All spans sent to Dynatrace via OneAgent
6. Dynatrace correlates spans into complete trace

### Problem Detection Flow

1. **Deployment fails** (ImagePullBackOff)
2. **OneAgent** detects pod events
3. **ActiveGate** monitors cluster state
4. **Data sent** to Dynatrace SaaS
5. **Davis AI** analyzes anomaly
6. **Problem created** with root cause
7. **Workflow triggered** automatically
8. **Slack notification** sent to team

---

## Network Architecture

### VPC Design
- **CIDR**: 10.0.0.0/16
- **Public Subnets**: 10.0.3.0/24, 10.0.4.0/24 (2 AZs)
- **Private Subnets**: 10.0.1.0/24, 10.0.2.0/24 (2 AZs)

### Traffic Flow
1. **Internet** → **AWS NLB** (public subnet)
2. **NLB** → **NGINX Ingress** (private subnet)
3. **NGINX** → **Gateway Service** (ClusterIP)
4. **Gateway** → **Backend Services** (ClusterIP)
5. **Backend** → **PostgreSQL/Redis** (ClusterIP)

### Security Groups
- **EKS Nodes**: Allow 443 from EKS control plane, all traffic within VPC
- **Load Balancer**: Allow 80/443 from internet
- **Database**: Allow 5432 only from application pods

---

## Security Architecture

### Authentication & Authorization
- **JWT-based authentication** with 24-hour expiry
- **Role-Based Access Control (RBAC)**:
  - ADMIN: Full access
  - HR: Employee, leave, payroll management
  - MANAGER: Team leave approval
  - EMPLOYEE: Self-service operations

### Secrets Management
- **Kubernetes Secrets** for sensitive data
- **Environment variables** injected into pods
- **No hardcoded credentials** in code or Docker images

### Network Security
- **Private subnets** for all application workloads
- **NAT Gateway** for outbound internet access
- **Security groups** restrict inter-service communication
- **Network policies** (optional) for pod-to-pod security

### Dynatrace Security
- **OneAgent** runs with minimal privileges
- **API tokens** stored as Kubernetes secrets
- **TLS encryption** for data in transit
- **RBAC** in Dynatrace for user access

---

## Scalability & High Availability

### Horizontal Scaling
- **HPA (Horizontal Pod Autoscaler)** configured for all services
- **Target CPU**: 70%
- **Min replicas**: 1
- **Max replicas**: 5

### High Availability
- **Multi-AZ deployment**: Nodes across 2 availability zones
- **Pod anti-affinity**: Spread pods across nodes
- **Health checks**: Liveness and readiness probes
- **Rolling updates**: Zero-downtime deployments

### Database HA
- **PostgreSQL**: Single instance with EBS persistence (can be upgraded to RDS for HA)
- **Redis**: Single instance with persistence (can be upgraded to ElastiCache)

### Disaster Recovery
- **EBS snapshots** for persistent volumes
- **Terraform state** for infrastructure recreation
- **GitOps repository** for application state
- **Dynatrace configuration** backed up

---

## Performance Characteristics

### Expected Performance
| Metric | Target |
|--------|--------|
| API Response Time (p95) | < 200ms |
| API Response Time (p99) | < 500ms |
| Throughput | 1000 req/sec |
| Error Rate | < 0.1% |
| Availability | 99.9% |

### Resource Usage
| Component | CPU (request/limit) | Memory (request/limit) |
|-----------|---------------------|------------------------|
| Gateway | 100m / 500m | 256Mi / 512Mi |
| Microservices | 100m / 500m | 256Mi / 512Mi |
| Frontend | 50m / 200m | 128Mi / 256Mi |
| PostgreSQL | 250m / 1000m | 512Mi / 1Gi |
| Redis | 100m / 500m | 256Mi / 512Mi |

---

## Future Enhancements

### Planned Improvements
1. **Service Mesh**: Istio for advanced traffic management
2. **API Gateway**: Kong or AWS API Gateway for rate limiting
3. **Secrets**: AWS Secrets Manager or HashiCorp Vault
4. **Database**: Amazon RDS for managed PostgreSQL with Multi-AZ
5. **Cache**: Amazon ElastiCache for managed Redis
6. **Monitoring**: Custom Dynatrace dashboards per service
7. **CI/CD**: GitHub Actions for automated testing and builds
8. **Security**: Falco for runtime security monitoring
9. **Backup**: Velero for Kubernetes backups
10. **Cost Optimization**: Spot instances for non-critical workloads

---

## References

- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Dynatrace Kubernetes Monitoring](https://www.dynatrace.com/technologies/kubernetes-monitoring/)
- [FluxCD Documentation](https://fluxcd.io/docs/)
- [Spring Boot Best Practices](https://spring.io/guides)
- [OpenTelemetry Specification](https://opentelemetry.io/docs/)
