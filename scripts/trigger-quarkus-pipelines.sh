#!/bin/bash
# Trigger PayU Quarkus Services Pipelines
# Focus: gateway-service, notification-service, api-portal-service

NAMESPACE="payu-cicd"
GIT_URL="https://github.com/fajjarnr/payu.git"
GIT_REVISION="main"
IMAGE_TAG="1.7.2"

SERVICES=("gateway-service" "notification-service" "api-portal-service")

echo "🚀 Triggering Quarkus Services Pipelines for $IMAGE_TAG..."

for SERVICE in "${SERVICES[@]}"; do
  echo "--- Triggering $SERVICE ---"
  
  cat <<EOF | oc create -f -
apiVersion: tekton.dev/v1
kind: PipelineRun
metadata:
  generateName: build-${SERVICE}-
  namespace: ${NAMESPACE}
  labels:
    app: payu
    service: ${SERVICE}
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

  echo "✅ Triggered build-${SERVICE}-..."
  sleep 2
done

echo "------------------------------------------------"
echo "Check progress with: tkn pipelinerun list -n $NAMESPACE"
