####
# Build stage - Using Red Hat UBI9 OpenJDK 21 with Maven
####
FROM registry.access.redhat.com/ubi9/openjdk-21:1.20 AS build

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

# Build the application
RUN mvn package -DskipTests -Dspring-boot.build-image.skip=true && \
    mv target/*.jar target/app.jar

####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20

LABEL maintainer="backend-team@payu.id"
LABEL description="PayU Wallet Service - Balance Management, Ledger"
LABEL version="1.0.0"

# Use non-root user (185 is the default jboss user in UBI images)
USER 185

WORKDIR /deployments

# Copy the built artifact from build stage
COPY --from=build --chown=185 /build/target/app.jar /deployments/app.jar

# Expose the service port
EXPOSE 8004

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/deployments/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8004/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]
