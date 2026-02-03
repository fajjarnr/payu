####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="PayU compliance-service"
LABEL version="1.0.0"

# Install curl for health checks (MUST BE ROOT)
USER root
RUN microdnf install -y curl-minimal && microdnf clean all

# Use non-root user
USER 185

WORKDIR /deployments

# Copy the built artifact from the target directory
COPY target/*.jar /deployments/app.jar

# Volume mounts
VOLUME ["/deployments/tmp", "/deployments/logs"]

# Expose the service port
EXPOSE 8080

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
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]
