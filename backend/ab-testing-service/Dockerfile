####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="PayU A/B Testing Service - Feature experimentation and A/B testing"
LABEL version="1.0.0"
LABEL org.opencontainers.image.title="payu-ab-testing-service"
LABEL org.opencontainers.image.description="PayU A/B Testing Service - UI/Feature experimentation framework"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="PayU Indonesia"
LABEL id.payu.service.tier="platform"

# Install curl for health checks
USER root
RUN microdnf install -y curl-minimal && microdnf clean all

# Use non-root user
USER 185

WORKDIR /deployments

# Copy the pre-built artifact
COPY target/*.jar /deployments/app.jar

# Volume mounts for temporary files and logs
VOLUME ["/deployments/tmp", "/deployments/logs"]

# Expose the service port
EXPOSE 8080

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]
