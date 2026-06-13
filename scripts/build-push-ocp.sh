#!/bin/bash
# Build and push all service images to OpenShift internal registry with correct tags
# usage: ./scripts/build-push-ocp.sh
set -e

REGISTRY="default-route-openshift-image-registry.apps.payu.ocp.fajjjar.my.id"
NAMESPACE="payu-dev"

SERVICES=(
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
  api-portal-service
  gateway-service
  notification-service
  analytics-service
  web-app
  bi-fast-simulator
  biller-simulator
  dukcapil-simulator
  qris-simulator
)

get_dir() {
  local svc=$1
  if [ -d "backend/${svc}" ]; then
    echo "backend/${svc}"
  elif [ -d "backend/simulators/${svc}" ]; then
    echo "backend/simulators/${svc}"
  elif [ -d "frontend/${svc}" ]; then
    echo "frontend/${svc}"
  else
    echo ""
  fi
}

get_tag() {
  local svc=$1
  local yaml=""
  if [ -d "infrastructure/workloads/base/${svc}" ]; then
    yaml="infrastructure/workloads/base/${svc}/deployment.yaml"
  elif [ -f "infrastructure/workloads/base/${svc}.yaml" ]; then
    yaml="infrastructure/workloads/base/${svc}.yaml"
  fi
  
  if [ -f "$yaml" ]; then
    # Extract tag from image: ...:tag line
    grep -oP 'image:.*:\K[a-zA-Z0-9.-]+' "$yaml" | head -n 1
  else
    echo "latest"
  fi
}

echo "============================================"
echo "Starting PayU Build & Push to OCP Registry"
echo "Registry: ${REGISTRY}/${NAMESPACE}"
echo "============================================"
echo ""

# Make sure we're logged in
if ! podman login --get-login "${REGISTRY}" &>/dev/null; then
  echo "Logging podman into OCP registry..."
  podman login -u kubeadmin -p "$(oc whoami -t)" --tls-verify=false "${REGISTRY}"
fi

for svc in "${SERVICES[@]}"; do
  dir=$(get_dir "$svc")
  if [ -z "$dir" ]; then
    echo "WARNING: Directory not found for ${svc}, skipping"
    continue
  fi
  
  tag=$(get_tag "$svc")
  if [ -z "$tag" ]; then
    tag="latest"
  fi
  
  image="${REGISTRY}/${NAMESPACE}/${svc}:${tag}"
  
  echo "=== Building ${svc} (Tag: ${tag}, Directory: ${dir}) ==="
  
  if [ -f "${dir}/Containerfile" ]; then
    podman build --tls-verify=false -t "${image}" -f "${dir}/Containerfile" "${dir}"
  elif [ -f "${dir}/Dockerfile" ]; then
    podman build --tls-verify=false -t "${image}" -f "${dir}/Dockerfile" "${dir}"
  else
    echo "WARNING: No Containerfile or Dockerfile found for ${svc}, skipping"
    continue
  fi
  
  echo "  Pushing ${image}..."
  podman push --tls-verify=false "${image}"
  echo "  Done: ${svc}"
  echo ""
done

echo "============================================"
echo "All images built and pushed successfully!"
echo "============================================"
