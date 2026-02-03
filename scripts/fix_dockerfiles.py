import os

# Define all backend services
services = [
    'account-service', 'auth-service', 'transaction-service', 'wallet-service',
    'compliance-service', 'investment-service', 'lending-service', 'billing-service',
    'notification-service', 'backoffice-service', 'partner-service', 'promotion-service',
    'support-service', 'statement-service',
    'simulators/bi-fast-simulator', 'simulators/dukcapil-simulator', 'simulators/qris-simulator',
    'api-portal-service', 'kyc-service', 'analytics-service'
]

# Spring Boot Template
springboot_template = """####
# Build stage - Using Red Hat UBI9 OpenJDK 21 with Maven
####
FROM registry.access.redhat.com/ubi9/openjdk-21:1.24-2 AS build

USER root
WORKDIR /build

# Install Maven
RUN microdnf install -y maven && microdnf clean all

# Copy entire project (needed for parent pom/shared dependencies)
COPY . .

# Build the application
RUN mvn package -DskipTests -Dspring-boot.build-image.skip=true -pl :{service_name} -am && \\
    mv {service_path}/target/*.jar {service_path}/target/app.jar

####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="PayU {service_name}"
LABEL version="1.0.0"
LABEL id.payu.service.tier="core"

# Install curl for health checks (MUST BE ROOT)
USER root
RUN microdnf install -y curl-minimal && microdnf clean all

# Use non-root user (185 is the default jboss user in UBI images)
USER 185

WORKDIR /deployments

# Copy the built artifact from build stage
COPY --from=build --chown=185 /build/{service_path}/target/app.jar /deployments/app.jar

# Volume mounts for temporary files and logs
VOLUME ["/deployments/tmp", "/deployments/logs"]

# Expose the service port
EXPOSE {port}

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \\
    -XX:MaxRAMPercentage=75.0 \\
    -XX:InitialRAMPercentage=50.0 \\
    -XX:+UseG1GC \\
    -XX:MaxGCPauseMillis=200 \\
    -XX:+HeapDumpOnOutOfMemoryError \\
    -XX:HeapDumpPath=/deployments/heapdump.hprof \\
    -Djava.security.egd=file:/dev/./urandom"

# Health check (Actuator)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \\
    CMD curl -f http://localhost:{port}/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]
"""

# Quarkus Simulator Template
quarkus_template = """####
# Build stage - Using Red Hat UBI9 OpenJDK 21 with Maven
####
FROM registry.access.redhat.com/ubi9/openjdk-21:1.24-2 AS build

USER root
WORKDIR /build

# Install Maven
RUN microdnf install -y maven && microdnf clean all

# Copy entire project (needed for parent pom/shared dependencies)
COPY . .

# Build the Quarkus application
RUN mvn package -DskipTests -Dquarkus.package.jar.type={package_type} -pl :{service_name} -am

####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="PayU {service_name}"
LABEL version="1.0.0"

# Install curl for health checks (MUST BE ROOT)
USER root
RUN microdnf install -y curl-minimal && microdnf clean all

# Use non-root user (185 is the default jboss user in UBI images)
USER 185

WORKDIR /deployments

# Copy built artifact from build stage
{copy_steps}

# Expose the service port
EXPOSE {port}

# JVM configuration
ENV JAVA_OPTS="-XX:+UseContainerSupport \\
    -XX:MaxRAMPercentage=75.0 \\
    -XX:+UseG1GC \\
    -XX:MaxGCPauseMillis=100 \\
    -Dquarkus.http.host=0.0.0.0 \\
    -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \\
    CMD curl -f http://localhost:{port}/q/health || exit 1

# Volume mounts for temporary files and logs
VOLUME ["/deployments/tmp", "/deployments/logs"]

# Run the Quarkus application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/{jar_name}"]
"""

# Python Template
python_template = """####
# Build stage - UBI9 Python 3.11
####
FROM registry.access.redhat.com/ubi9/python-311:latest AS builder

USER root
WORKDIR /app

# Install build dependencies
RUN dnf install -y gcc g++ glibc-devel && dnf clean all

# Copy entire project
COPY . .

# Install dependencies from the specific service directory
RUN pip install --user --no-cache-dir -r {service_path}/requirements.txt

####
# Runtime stage - UBI9 Minimal
####
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.3

# Install runtime dependencies
RUN microdnf install -y curl-minimal && microdnf clean all

WORKDIR /app

# Copy Python packages and source
COPY --from=builder /root/.local /root/.local
COPY --from=builder /app/{service_path}/src /app/src

ENV PATH=/root/.local/bin:$PATH
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# Create directories and set permissions
RUN mkdir -p /app/tmp /app/logs /app/models /app/uploads && \\
    chown -R 185:0 /app

USER 185

# Expose service port
EXPOSE {port}

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \\
    CMD curl -f http://localhost:{port}/health || exit 1

# Run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "{port}", "--workers", "4"]
"""

# Port mapping
ports = {
    'account-service': 8001,
    'auth-service': 8002,
    'transaction-service': 8003,
    'wallet-service': 8004,
    'compliance-service': 8087,
    'investment-service': 8080,
    'lending-service': 8087,
    'billing-service': 8080,
    'notification-service': 8080,
    'backoffice-service': 8080,
    'partner-service': 8080,
    'promotion-service': 8080,
    'support-service': 8080,
    'statement-service': 8015,
    'simulators/bi-fast-simulator': 8090,
    'simulators/dukcapil-simulator': 8091,
    'simulators/qris-simulator': 8092,
    'api-portal-service': 8099,
    'kyc-service': 8007,
    'analytics-service': 8008
}

for service in services:
    dockerfile_path = f"backend/{service}/Dockerfile"
    if os.path.exists(dockerfile_path):
        port = ports.get(service, 8080)
        service_name = service.split('/')[-1]
        
        if 'simulator' in service:
            copy_steps = f"COPY --from=build --chown=185 /build/{service}/target/*-runner.jar /deployments/app.jar"
            content = quarkus_template.format(
                service_path=service, service_name=service_name, port=port,
                package_type="uber-jar", jar_name="app.jar", copy_steps=copy_steps
            )
        elif service == 'api-portal-service':
            copy_steps = f"""COPY --from=build --chown=185 /build/{service}/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /build/{service}/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /build/{service}/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /build/{service}/target/quarkus-app/quarkus/ /deployments/quarkus/"""
            content = quarkus_template.format(
                service_path=service, service_name=service_name, port=port,
                package_type="fast-jar", jar_name="quarkus-run.jar", copy_steps=copy_steps
            )
        elif service in ['kyc-service', 'analytics-service']:
            content = python_template.format(service_path=service, port=port)
        else:
            content = springboot_template.format(service_path=service, service_name=service_name, port=port)
            
        with open(dockerfile_path, 'w') as f:
            f.write(content)
        print(f"Updated {dockerfile_path}")
