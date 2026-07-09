# Complete Platform Architecture

## Table of Contents
1. [System Overview](#system-overview)
2. [Application Architecture](#application-architecture)
3. [Infrastructure Architecture](#infrastructure-architecture)
4. [Observability Architecture](#observability-architecture)
5. [CI/CD Architecture](#cicd-architecture)
6. [GitOps Architecture](#gitops-architecture)
7. [Security Architecture](#security-architecture)
8. [Network Architecture](#network-architecture)
9. [Data Flow](#data-flow)
10. [Technology Stack](#technology-stack)

---

## System Overview

**Nimbus** is a production-grade enterprise observability demonstration platform running on AWS EKS. It showcases a complete microservices architecture with dual observability stacks:

- **Commercial Observability**: Dynatrace OneAgent with AI-powered monitoring
- **Open-Source Observability**: Grafana + InfluxDB + OpenTelemetry

### Key Metrics
- **Microservices**: 8 Spring Boot services + API Gateway + React Frontend
- **Infrastructure**: AWS EKS cluster with 3 t3.xlarge nodes
- **Observability Agents**: Dynatrace OneAgent (3 pods) + OTel Collector (2 pods)
- **Databases**: PostgreSQL (7 databases) + Redis cache
- **CI/CD**: GitHub Actions + FluxCD GitOps
- **Security**: SonarCloud + OWASP + Trivy scanning

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           AWS Cloud (us-east-1)                          │
│                                                                          │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │                      Amazon EKS Cluster                           │ │
│  │                                                                   │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │ │
│  │  │   Worker     │  │   Worker     │  │   Worker     │          │ │
│  │  │   Node 1     │  │   Node 2     │  │   Node 3     │          │ │
│  │  │  t3.xlarge   │  │  t3.xlarge   │  │  t3.xlarge   │          │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘          │ │
│  │                                                                   │ │
│  └───────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │                    Observability Layer                            │ │
│  │  • Dynatrace: OneAgent + ActiveGate + Operator                   │ │
│  │  • Grafana + InfluxDB + OpenTelemetry Collector                  │ │
│  └───────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---
