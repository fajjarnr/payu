#!/bin/bash
set -e

REGISTRY="default-route-openshift-image-registry.apps.payu.ocp.fajjjar.my.id"
NAMESPACE="payu-dev"
TAG="1.8.55"

# List of services that we actually modified and need to rebuild and redeploy
SERVICES=(
  account-service
  auth-service
  cms-service
  compliance-service
  dispute-service
  fx-service
  lending-service
  promotion-service
  support-service
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
    grep -oP 'image:.*:\K[a-zA-Z0-9.-]+' "$yaml" | head -n 1
  else
    echo "latest"
  fi
}

echo "Logging podman into OCP registry..."
podman login -u kubeadmin -p "$(oc whoami -t)" --tls-verify=false "${REGISTRY}"

for svc in "${SERVICES[@]}"; do
  dir=$(get_dir "$svc")
  if [ -z "$dir" ]; then
    echo "WARNING: Directory not found for ${svc}, skipping"
    continue
  fi
  
  TAG=$(get_tag "$svc")
  if [ -z "$TAG" ]; then
    TAG="latest"
  fi
  
  image="${REGISTRY}/${NAMESPACE}/${svc}:${TAG}"
  
  echo "=== Building ${svc} (Tag: ${TAG}, Directory: ${dir}) ==="
  
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
  
  echo "  Rolling out restart for deployment/${svc}..."
  oc rollout restart deployment/${svc} -n ${NAMESPACE}
  
  echo "  Done: ${svc}"
  echo ""
done

echo "All modified images built, pushed, and rollout restarted!"
