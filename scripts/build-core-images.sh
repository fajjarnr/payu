#!/bin/bash
set -e

# Services that successfully built JARs
SERVICES=(
    "account-service"
    "auth-service"
    "transaction-service"
    "wallet-service"
    "lending-service"
    "investment-service"
    "statement-service"
    "support-service"
    "partner-service"
)

echo "🚀 Starting Batch Container Build..."

for svc in "${SERVICES[@]}"; do
    echo "------------------------------------------------"
    echo "🐳 Building: $svc"
    cd "/home/ubuntu/payu/backend/$svc"
    
    # Check if JAR exists
    if ls target/*.jar 1> /dev/null 2>&1; then
        podman build -t "payu-$svc:latest" .
        echo "✅ Built payu-$svc:latest"
    else
        echo "❌ JAR not found for $svc, skipping..."
    fi
done

echo "------------------------------------------------"
echo "🎉 Batch Build Complete!"
podman images | grep payu-
