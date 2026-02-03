####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="PayU dukcapil-simulator"
LABEL version="1.0.0"

# Install curl for health checks (MUST BE ROOT)
USER root
RUN microdnf install -y curl-minimal && microdnf clean all

# Use non-root user
USER 185

WORKDIR /deployments

# Copy built artifact from host build (Quarkus fast-jar)
COPY target/quarkus-app/ /deployments/

# Expose the service port
EXPOSE 8080

# JVM configuration
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -Dquarkus.http.host=0.0.0.0 \
    -Dquarkus.http.port=8080 \
    -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/q/health || exit 1

# Volume mounts for temporary files and logs
VOLUME ["/deployments/tmp", "/deployments/logs"]

# Run the Quarkus application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/quarkus-run.jar"]
