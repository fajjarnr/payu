#!/usr/bin/env bash
# Build and push the exact images referenced by the rendered dev overlay.
set -euo pipefail

OVERLAY="${OVERLAY:-infrastructure/workloads/overlays/payu-dev}"
NAMESPACE="${NAMESPACE:-payu-dev}"
TLS_VERIFY="${TLS_VERIFY:-false}"

if [ ! -d "$OVERLAY" ]; then
  echo "ERROR: overlay not found: $OVERLAY" >&2
  exit 1
fi

REGISTRY="${REGISTRY:-$(oc get route default-route -n openshift-image-registry -o jsonpath='{.spec.host}' 2>/dev/null || true)}"
if [ -z "$REGISTRY" ]; then
  echo "ERROR: image registry default route is missing" >&2
  echo "Run: oc patch configs.imageregistry.operator.openshift.io/cluster --type=merge -p '{\"spec\":{\"defaultRoute\":true}}'" >&2
  exit 1
fi

get_dir() {
  local svc="$1"
  if [ -f "backend/${svc}/Containerfile" ]; then
    printf 'backend/%s\n' "$svc"
  elif [ -f "backend/simulators/${svc}/Containerfile" ]; then
    printf 'backend/simulators/%s\n' "$svc"
  elif [ -f "frontend/${svc}/Containerfile" ]; then
    printf 'frontend/%s\n' "$svc"
  else
    return 1
  fi
}

to_push_ref() {
  local image="$1"
  local path="${image#*/}"
  local ns="${path%%/*}"
  local repo_tag="${path#*/}"
  printf '%s/%s/%s\n' "$REGISTRY" "$ns" "$repo_tag"
}

podman login -u "$(oc whoami)" -p "$(oc whoami -t)" --tls-verify="$TLS_VERIFY" "$REGISTRY" >/dev/null

mapfile -t images < <(oc kustomize "$OVERLAY" | awk '/^[[:space:]]*image: /{print $2}' | sort -u)

if [ "${#images[@]}" -eq 0 ]; then
  echo "ERROR: no images found in rendered overlay: $OVERLAY" >&2
  exit 1
fi

for image in "${images[@]}"; do
  path="${image#*/}"
  repo_tag="${path#*/}"
  svc="${repo_tag%%:*}"
  tag="${repo_tag##*:}"
  dir="$(get_dir "$svc")"
  push_image="$(to_push_ref "$image")"

  echo "==> ${svc}:${tag}"
  podman build --tls-verify="$TLS_VERIFY" \
    --format=docker \
    --build-arg "APP_VERSION=${tag}" \
    -f "${dir}/Containerfile" \
    -t "$push_image" \
    "$dir"
  podman push --tls-verify="$TLS_VERIFY" "$push_image"
done

echo "Built and pushed ${#images[@]} image(s) for ${NAMESPACE}."
