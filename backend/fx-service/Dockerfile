####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
# Note: JAR must be built locally first with:
# mvn -f backend/fx-service/pom.xml clean package -DskipTests
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="PayU FX Service - Foreign Exchange rates and conversion"
LABEL version="1.0.0"
LABEL org.opencontainers.image.title="payu-fx-service"
LABEL org.opencontainers.image.description="PayU FX Service - Currency exchange rates, multi-currency conversion"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="PayU Indonesia"
LABEL id.payu.service.tier="financial"
LABEL id.payu.compliance.pci-dss="required"

# Install curl for health checks (use curl-minimal to avoid conflicts with UBI micro image)
USER root
RUN microdnf install -y curl-minimal && microdnf clean all

# Use non-root user
USER 185

WORKDIR /deployments

# Copy the built artifact from host
COPY target/*.jar /deployments/app.jar

# Volume mounts for temporary files and logs
VOLUME ["/deployments/tmp", "/deployments/logs"]

# Expose the service port
# Expose the service port
EXPOSE 8080

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/deployments/heapdump.hprof"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/fx-api/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]
