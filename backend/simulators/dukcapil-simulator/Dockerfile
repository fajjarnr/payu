####
# Build stage
####
FROM registry.access.redhat.com/ubi9/openjdk-21:1.24-2 AS build

USER root
WORKDIR /build

# Install Maven
RUN microdnf install -y maven && microdnf clean all

# Copy backend parent POM and simulator source
COPY pom.xml .
COPY simulators/dukcapil-simulator/pom.xml simulators/dukcapil-simulator/
COPY simulators/dukcapil-simulator/src simulators/dukcapil-simulator/src

# Build the Quarkus application
RUN mvn package -DskipTests -Dquarkus.package.jar.type=fast-jar -f simulators/dukcapil-simulator/pom.xml

####
# Runtime stage
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="PayU dukcapil-simulator"
LABEL version="1.0.0"

# Install curl
USER root
RUN microdnf install -y curl-minimal && microdnf clean all

# Use non-root user
USER 185
WORKDIR /deployments

# Copy Quarkus fast-jar structure
COPY --from=build --chown=185 /build/simulators/dukcapil-simulator/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /build/simulators/dukcapil-simulator/target/quarkus-app/quarkus-run.jar /deployments/quarkus-run.jar
COPY --from=build --chown=185 /build/simulators/dukcapil-simulator/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /build/simulators/dukcapil-simulator/target/quarkus-app/quarkus/ /deployments/quarkus/

# Expose port
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

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/quarkus-run.jar"]
