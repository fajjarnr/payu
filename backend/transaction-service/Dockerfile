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

# Build the application (includes building shared libraries)
RUN mvn clean install -DskipTests -Dspring-boot.build-image.skip=true && \
    mv target/*.jar target/app.jar

####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="PayU Transaction Service - Transfers, BI-FAST, QRIS"
LABEL version="1.0.0"
LABEL org.opencontainers.image.title="payu-transaction-service"
LABEL org.opencontainers.image.description="PayU Transaction Service - Core banking for transfers, BI-FAST, QRIS payments with Saga pattern"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="PayU Indonesia"
LABEL org.opencontainers.image.url="https://payu.id"
LABEL org.opencontainers.image.source="https://github.com/payu/backend/transaction-service"
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.authors="PayU Backend Team"

# Use non-root user (185 is the default jboss user in UBI images)
USER 185

WORKDIR /deployments

# Copy the built artifact from build stage
COPY --from=build --chown=185 /build/target/app.jar /deployments/app.jar

# Expose the service port
EXPOSE 8003

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/deployments/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8003/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]
