####
# Build stage - Using Red Hat UBI9 OpenJDK 21 with Maven
####
FROM registry.access.redhat.com/ubi9/openjdk-21:1.20 AS build

USER root
WORKDIR /build

# Install Maven
RUN microdnf install -y maven && microdnf clean all

# Copy entire project (needed for shared library dependencies)
COPY . .

# Build the Quarkus application (fast-jar format for better caching)
RUN mvn clean package -DskipTests

####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="PayU Gateway Service - API Gateway"
LABEL version="1.0.0"
LABEL org.opencontainers.image.title="payu-gateway-service"
LABEL org.opencontainers.image.description="PayU Gateway Service - API Gateway with rate limiting, circuit breaker, routing"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="PayU Indonesia"
LABEL org.opencontainers.image.url="https://payu.id"
LABEL org.opencontainers.image.source="https://github.com/payu/backend/gateway-service"
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.authors="PayU Backend Team"

# Use non-root user (185 is the default jboss user in UBI images)
USER 185

WORKDIR /deployments

# Copy Quarkus fast-jar structure for better layer caching
COPY --from=build --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /build/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /build/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /build/target/quarkus-app/quarkus/ /deployments/quarkus/

# Expose the service port
EXPOSE 8080

# JVM configuration optimized for Gateway (API Gateway needs low latency)
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:MinRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=50 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseStringDeduplication \
    -Dquarkus.http.host=0.0.0.0 \
    -Dquarkus.http.idle-timeout=30s \
    -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Health check with shorter start period for faster failover
HEALTHCHECK --interval=15s --timeout=3s --start-period=20s --retries=3 \
    CMD curl -f http://localhost:8080/q/health || exit 1

# Run the Quarkus application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/quarkus-run.jar"]
