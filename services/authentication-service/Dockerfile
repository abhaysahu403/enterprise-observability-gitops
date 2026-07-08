# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy the whole multi-module project so the shared-library can be resolved
COPY pom.xml .
COPY shared-library shared-library
COPY gateway gateway
COPY services services

# Build shared-library first, then this service (offline-friendly layer caching)
RUN mvn -q -pl shared-library -am install -DskipTests
RUN mvn -q -pl services/authentication-service -am package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-jammy

# OCI Labels
LABEL org.opencontainers.image.title="Enterprise Observability Platform - Authentication Service"
LABEL org.opencontainers.image.description="User authentication and authorization service with JWT"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="Enterprise Platform"
LABEL org.opencontainers.image.authors="Platform Team"

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /workspace/services/authentication-service/target/authentication-service.jar app.jar

EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8081/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
