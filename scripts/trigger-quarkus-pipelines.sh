#!/bin/bash
# Trigger PayU Quarkus Services Pipelines (including Simulators)
# Semantic Versioning: v1.8.55
# Decoupled Service Name (Image) from Service Path (Context)

NAMESPACE="payu-cicd"
GIT_URL="https://github.com/fajjarnr/payu.git"
GIT_REVISION="main"
IMAGE_TAG="v1.8.55"

# Format: "service-name|service-path"
SERVICES=(
  "gateway-service|gateway-service"
  "notification-service|notification-service"
  "api-portal-service|api-portal-service"
  "bi-fast-simulator|simulators/bi-fast-simulator"
  "biller-simulator|simulators/biller-simulator"
  "qris-simulator|simulators/qris-simulator"
  "dukcapil-simulator|simulators/dukcapil-simulator"
  "va-simulator|simulators/va-simulator"
)

echo "🚀 Triggering Quarkus Services Pipelines for $IMAGE_TAG..."

for ENTRY in "${SERVICES[@]}"; do
  SVC_NAME=$(echo "$ENTRY" | cut -d'|' -f1)
  SVC_PATH=$(echo "$ENTRY" | cut -d'|' -f2)
  
  # Safe Name for Tekton Run
  SAFE_NAME=$(echo "$SVC_NAME" | sed 's/\//-/g')
  
  echo "--- Triggering $SVC_NAME (Path: $SVC_PATH) ---"
  
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
      value: "${SVC_NAME}"
    - name: service-path
      value: "${SVC_PATH}"
    - name: service-base-dir
      value: "backend"
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
