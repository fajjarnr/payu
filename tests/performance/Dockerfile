# Dockerfile for Gatling Performance Tests
# Multi-stage build for optimal image size

FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Scala
ENV SCALA_VERSION=2.13.14
RUN apk add --no-cache curl bash && \
    curl -L https://github.com/scala/scala/releases/download/v${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz | tar -xz -C /opt && \
    mv /opt/scala-${SCALA_VERSION} /opt/scala

ENV PATH="/opt/scala/bin:${PATH}"

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the project
RUN mvn package -DskipTests -B

# Final stage
FROM eclipse-temurin:21-jre-alpine

# Install bash and curl
RUN apk add --no-cache bash curl

WORKDIR /gatling

# Copy built artifacts from builder
COPY --from=builder /build/target/lib ./lib
COPY --from=builder /build/target/gatling ./gatling
COPY --from=builder /build/src/test/resources ./resources

# Set environment variables
ENV GATLING_HOME=/gatling
ENV JAVA_OPTS="-Xms1024m -Xmx2048m -XX:+UseG1GC"

# Create results directory
RUN mkdir -p /gatling/results

# Default command (can be overridden)
CMD ["sh", "-c", "java $JAVA_OPTS -cp lib/*:$(find . -name '*.jar' | tr '\n' ':') io.gatling.app.Gatling -s id.payu.simulations.AllServicesSimulation -rf /gatling/results"]

# Labels
LABEL maintainer="qa-team@payu.id"
LABEL description="PayU Performance Load Testing with Gatling"
LABEL version="1.0.0"
