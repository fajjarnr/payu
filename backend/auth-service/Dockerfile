####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="PayU auth-service"
LABEL version="1.0.0"
LABEL id.payu.service.tier="core"

# Install curl for health checks (MUST BE ROOT)
USER root
RUN microdnf install -y curl-minimal && microdnf clean all

# Use non-root user (185 is the default jboss user in UBI images)
USER 185

WORKDIR /deployments

# Copy the built artifact from the target directory
# This assumes the build was done on the host
COPY target/*.jar /deployments/app.jar

# Volume mounts for temporary files and logs
VOLUME ["/deployments/tmp", "/deployments/logs"]

# Expose the service port
EXPOSE 8002

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/deployments/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# Health check (Actuator)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8002/actuator/health/liveness || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]
