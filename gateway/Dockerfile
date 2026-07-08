# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY shared-library shared-library
COPY gateway gateway
COPY services services

RUN mvn -q -pl shared-library -am install -DskipTests
RUN mvn -q -pl gateway -am package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-jammy

# OCI Labels
LABEL org.opencontainers.image.title="Enterprise Observability Platform - API Gateway"
LABEL org.opencontainers.image.description="Spring Cloud Gateway for routing and authentication"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="Enterprise Platform"
LABEL org.opencontainers.image.authors="Platform Team"

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /workspace/gateway/target/gateway.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
