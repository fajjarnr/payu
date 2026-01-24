# Statement Service Dockerfile
# Multi-stage build using Red Hat UBI9

# ============================================
# Builder Stage
# ============================================
FROM registry.access.redhat.com/ubi9/openjdk-21:1.20 AS builder

LABEL maintainer="PayU Engineering <engineering@payu.id>"
LABEL description="PayU Statement Service - E-Statement PDF Generation"

# Set working directory
WORKDIR /build

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -q

# ============================================
# Runtime Stage
# ============================================
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20

LABEL maintainer="PayU Engineering <engineering@payu.id>"
LABEL description="PayU Statement Service - Runtime"

# Create non-root user
RUN groupadd -r payu -g 185 && \
    useradd -r -g payu -u 185 -m -d /home/payu -s /sbin/nologin -c "PayU Statement Service" payu

# Set working directory
WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/target/statement-service-*.jar app.jar

# Change ownership
RUN chown -R payu:payu /app

# Switch to non-root user
USER payu

# Expose port
EXPOSE 8015

# Health check
HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=30s \
    CMD curl -f http://localhost:8015/actuator/health/liveness || exit 1

# JVM tuning for production
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
