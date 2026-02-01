#!/bin/bash
set -e

# Configuration
BACKEND_DIR="/home/ubuntu/payu/backend"
TEMPLATE_FILE="$BACKEND_DIR/Containerfile.template"

# Create Template with Placeholders
cat > "$TEMPLATE_FILE" <<EOF
####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
# Note: Expects pre-built JAR from host (mvn package must be run first)
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Metadata labels
LABEL maintainer="backend-team@payu.id"
LABEL description="{{DESCRIPTION}}"
LABEL version="1.0.0"
LABEL org.opencontainers.image.title="payu-{{SERVICE_NAME}}"
LABEL org.opencontainers.image.description="{{DESCRIPTION}}"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="PayU Indonesia"
LABEL org.opencontainers.image.source="https://github.com/payu/backend/{{SERVICE_NAME}}"
LABEL id.payu.service.tier="core"

# Use non-root user (185 is the default jboss user in UBI images)
USER 185

WORKDIR /deployments

# Copy the pre-built artifact from host (requires mvn clean package run locally first)
COPY --chown=185 target/*.jar /deployments/app.jar

# Volume mounts for temporary files and logs
VOLUME ["/deployments/tmp", "/deployments/logs"]

# Expose the service port
EXPOSE {{PORT}}

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java \$JAVA_OPTS -jar /deployments/app.jar"]
EOF

echo "✅ Created template at $TEMPLATE_FILE"

# Function to detect port from application.yml
get_port() {
    local service_dir=$1
    local port_yml=$(grep "port:" "$service_dir/src/main/resources/application.yml" 2>/dev/null | head -n 1 | awk '{print $2}' | tr -d '"')
    
    if [ -z "$port_yml" ]; then
        # Fallback: Try to grep EXPOSE from existing Containerfile
        local port_docker=$(grep "EXPOSE" "$service_dir/Containerfile" 2>/dev/null | awk '{print $2}')
        echo "${port_docker:-8080}"
    else
        echo "$port_yml"
    fi
}

# Function to process a service
process_service() {
    local service_dir=$1
    local service_name=$(basename "$service_dir")
    
    echo "------------------------------------------------"
    echo "📦 Processing: $service_name"
    
    # 1. Detect Port
    local port=$(get_port "$service_dir")
    echo "   Running on Port: $port"
    
    # 2. Define Description
    local description="PayU $service_name Service"
    
    # 3. Create new Containerfile from template
    sed -e "s|{{SERVICE_NAME}}|$service_name|g" \
        -e "s|{{DESCRIPTION}}|$description|g" \
        -e "s|{{PORT}}|$port|g" \
        "$TEMPLATE_FILE" > "$service_dir/Containerfile.new"
        
    mv "$service_dir/Containerfile.new" "$service_dir/Containerfile"
    echo "   ✅ Containerfile updated"
}

# List of Java Services to standardize
# Excludes: lending-service (already done), python services (analytics, kyc)
SERVICES=(
    "account-service"
    "auth-service"
    "transaction-service"
    "wallet-service"
    "investment-service"
    "statement-service"
    "support-service"
    "partner-service"
    "promotion-service"
    "compliance-service"
    "billing-service"
    "notification-service"
    "gateway-service"
    "api-portal-service"
    "cms-service"
    "ab-testing-service"
    "fx-service"
    "backoffice-service"
)

# Execute loop
for svc in "${SERVICES[@]}"; do
    if [ -d "$BACKEND_DIR/$svc" ]; then
        process_service "$BACKEND_DIR/$svc"
    else
        echo "⚠️  Skipping $svc (Directory not found)"
    fi
done

echo "------------------------------------------------"
echo "🎉 Standardization Complete!"
rm "$TEMPLATE_FILE"
