
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

# Build the Quarkus application (fast-jar format)
RUN mvn package -DskipTests

####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20

LABEL maintainer="backend-team@payu.id"
LABEL description="PayU Partner Service - SNAP BI Standard & Merchant Integration"
LABEL version="1.0.0"

# Use non-root user (185 is the default jboss user in UBI images)
USER 185

WORKDIR /deployments

# Copy Quarkus fast-jar structure
COPY --from=build --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /build/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /build/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /build/target/quarkus-app/quarkus/ /deployments/quarkus/

# Expose the service port
EXPOSE 8080

# JVM configuration for Quarkus
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -Dquarkus.http.host=0.0.0.0 \
    -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Health check using Quarkus health endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/q/health || exit 1

# Run the Quarkus application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/quarkus-run.jar"]
