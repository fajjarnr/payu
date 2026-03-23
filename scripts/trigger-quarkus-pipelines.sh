#!/bin/bash
# Trigger PayU Quarkus Services Pipelines (including Simulators)
# Semantic Versioning: v1.7.2

NAMESPACE="payu-cicd"
GIT_URL="https://github.com/fajjarnr/payu.git"
GIT_REVISION="main"
IMAGE_TAG="1.7.2"

# Core Services
CORE_SERVICES=("gateway-service" "notification-service" "api-portal-service")

# Simulators
SIMULATORS=("simulators/bi-fast-simulator" "simulators/biller-simulator" "simulators/qris-simulator" "simulators/dukcapil-simulator" "simulators/va-simulator")

SERVICES=("${CORE_SERVICES[@]}" "${SIMULATORS[@]}")

echo "🚀 Triggering Quarkus Services Pipelines for $IMAGE_TAG..."

for SERVICE in "${SERVICES[@]}"; do
  # Replace slash with dash for valid Tekton resource naming
  SAFE_NAME=$(echo "$SERVICE" | sed 's/\//-/g')
  
  echo "--- Triggering $SERVICE (named: build-$SAFE_NAME) ---"
  
  cat <<EOF | oc create -f -
apiVersion: tekton.dev/v1
kind: PipelineRun
metadata:
  generateName: build-${SAFE_NAME}-
  namespace: ${NAMESPACE}
  labels:
    app: payu
    service: ${SAFE_NAME}
spec:
  pipelineRef:
    name: payu-build-pipeline
  params:
    - name: service-name
      value: "${SERVICE}"
    - name: git-url
      value: "${GIT_URL}"
    - name: git-revision
      value: "${GIT_REVISION}"
    - name: service-type
      value: "quarkus"
    - name: build-image-tag
      value: "${IMAGE_TAG}"
  workspaces:
    - name: source
      volumeClaimTemplate:
        spec:
          accessModes:
            - ReadWriteOnce
          resources:
            requests:
              storage: 2Gi
          storageClassName: gp3-csi
EOF

  echo "✅ Triggered build-${SAFE_NAME}-..."
  sleep 1
done

echo "------------------------------------------------"
echo "Check progress with: tkn pipelinerun list -n $NAMESPACE"
