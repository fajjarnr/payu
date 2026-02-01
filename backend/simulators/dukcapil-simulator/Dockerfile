####
# Build stage - Using Red Hat UBI9 OpenJDK 21 with Maven
####
FROM registry.access.redhat.com/ubi9/openjdk-21:1.24-2 AS build

USER root
WORKDIR /build

# Install Maven
RUN microdnf install -y maven && microdnf clean all

# Copy pom.xml first for dependency caching
COPY pom.xml ./

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the Quarkus application (uber-jar format for simulator)
RUN mvn package -DskipTests -Dquarkus.package.jar.type=uber-jar

####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="PayU Dukcapil Simulator - NIK Verification, Face Matching"
LABEL version="1.0.0"
LABEL org.opencontainers.image.title="payu-dukcapil-simulator"
LABEL org.opencontainers.image.description="PayU Dukcapil Simulator - Simulates Dukcapil NIK verification and face matching for testing"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="PayU Indonesia"
LABEL org.opencontainers.image.url="https://payu.id"
LABEL org.opencontainers.image.source="https://github.com/payu/backend/simulators/dukcapil-simulator"
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.authors="PayU Backend Team"
LABEL id.payu.service="dukcapil-simulator"
LABEL id.payu.tier="simulator"
LABEL id.payu.runtime="quarkus-native"

# Use non-root user (185 is the default jboss user in UBI images)
USER 185

# Install curl for health checks and debugging
RUN microdnf install -y curl && microdnf clean all

WORKDIR /deployments

# Copy the uber-jar from build stage
COPY --from=build --chown=185 /build/target/*-runner.jar /deployments/app.jar

# Expose the service port
EXPOSE 8091

# JVM configuration
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -Dquarkus.http.host=0.0.0.0 \
    -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8091/q/health || exit 1

# Volume mounts for temporary files and logs
VOLUME ["/deployments/tmp", "/deployments/logs"]

# Run the Quarkus application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]
