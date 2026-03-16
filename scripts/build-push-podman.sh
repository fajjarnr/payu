#!/bin/bash
# Build and push all PayU service images via Podman
set -e

REGISTRY="${1:-default-route-openshift-image-registry.apps.payu.ocp.fajjjar.my.id}"
NS="payu-dev"
TAG="${2:-1.4.0}"
BACKEND="/home/ubuntu/payu/backend"

# Java services (Spring Boot + Quarkus)
JAVA_SERVICES=(
  account-service
  auth-service
  wallet-service
  transaction-service
  investment-service
  lending-service
  statement-service
  notification-service
  gateway-service
  backoffice-service
  partner-service
  promotion-service
  support-service
  compliance-service
  fx-service
  cms-service
  api-portal-service
  billing-service
  dispute-service
  integration-service
  product-catalog-service
)

# Python services
PYTHON_SERVICES=(
  kyc-service
  analytics-service
)

echo "=== Building Java Service Images ==="
for svc in "${JAVA_SERVICES[@]}"; do
  echo "--- Building $svc ---"
  IMG="$REGISTRY/$NS/$svc:$TAG"
  # Check JAR exists
  JAR=$(find "$BACKEND/$svc/target" -name "*.jar" ! -name "*-sources.jar" ! -name "*original*" 2>/dev/null | head -1)
  if [ -z "$JAR" ]; then
    echo "SKIP: No JAR found for $svc"
    continue
  fi
  CF="$BACKEND/$svc/Containerfile"
  podman build --tls-verify=false -f "$CF" -t "$IMG" "$BACKEND/$svc" 2>&1 | tail -3
  echo "BUILT: $IMG"
done

echo ""
echo "=== Building Python Service Images ==="
for svc in "${PYTHON_SERVICES[@]}"; do
  echo "--- Building $svc ---"
  IMG="$REGISTRY/$NS/$svc:$TAG"
  CF="$BACKEND/$svc/Containerfile"
  podman build --tls-verify=false -f "$CF" -t "$IMG" "$BACKEND/$svc" 2>&1 | tail -3
  echo "BUILT: $IMG"
done

echo ""
echo "=== Pushing All Images ==="
for svc in "${JAVA_SERVICES[@]}" "${PYTHON_SERVICES[@]}"; do
  IMG="$REGISTRY/$NS/$svc:$TAG"
  echo "Pushing $svc..."
  podman push --tls-verify=false "$IMG" 2>&1 | tail -2
done

echo ""
echo "=== All Done ==="
podman images | grep "$NS" | wc -l
echo "images built and pushed"
