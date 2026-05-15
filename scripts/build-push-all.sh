#!/bin/bash
# Build and push all service images to OpenShift internal registry
set -e

REGISTRY="default-route-openshift-image-registry.apps.payu.ocp.fajjjar.my.id"
NAMESPACE="payu-dev"
TAG="latest"

# Java Spring Boot services (have target/*.jar)
SPRING_SERVICES=(
  account-service
  auth-service
  backoffice-service
  billing-service
  cms-service
  compliance-service
  dispute-service
  fx-service
  integration-service
  investment-service
  kyc-service
  lending-service
  partner-service
  product-catalog-service
  promotion-service
  statement-service
  support-service
  transaction-service
  wallet-service
)

# Quarkus services (have target/quarkus-app/)
QUARKUS_SERVICES=(
  api-portal-service
  gateway-service
  notification-service
)

# Simulators
SIMULATORS=(
  bi-fast-simulator
  biller-simulator
  dukcapil-simulator
  qris-simulator
)

build_and_push() {
  local svc=$1
  local dir=$2
  local image="${REGISTRY}/${NAMESPACE}/${svc}:${TAG}"
  
  echo "=== Building ${svc} ==="
  if [ -f "${dir}/Containerfile" ]; then
    podman build --tls-verify=false -t "${image}" -f "${dir}/Containerfile" "${dir}" 2>&1 | tail -5
  elif [ -f "${dir}/Dockerfile" ]; then
    podman build --tls-verify=false -t "${image}" -f "${dir}/Dockerfile" "${dir}" 2>&1 | tail -5
  else
    echo "  WARN: No Containerfile/Dockerfile found for ${svc}, skipping"
    return 0
  fi
  
  echo "  Pushing ${image}..."
  podman push --tls-verify=false "${image}" 2>&1 | tail -3
  echo "  Done: ${svc}"
  echo ""
}

echo "============================================"
echo "Building PayU services -> ${REGISTRY}/${NAMESPACE}"
echo "============================================"
echo ""

# Build Spring Boot services
for svc in "${SPRING_SERVICES[@]}"; do
  build_and_push "${svc}" "backend/${svc}"
done

# Build Quarkus services
for svc in "${QUARKUS_SERVICES[@]}"; do
  build_and_push "${svc}" "backend/${svc}"
done

# Build Simulators
for svc in "${SIMULATORS[@]}"; do
  build_and_push "${svc}" "backend/simulators/${svc}"
done

echo "============================================"
echo "All backend services built and pushed!"
echo "============================================"
