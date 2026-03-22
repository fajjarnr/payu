#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
# PayU Build & Push All Services v1.7.1
# Triggers payu-build-pipeline for each of the 23 microservices
# ═══════════════════════════════════════════════════════════════════
set -euo pipefail

TAG="${1:-v1.7.1}"
NAMESPACE="payu-cicd"
GIT_URL="https://github.com/fajjarnr/payu.git"
GIT_REV="main"

# Service definitions: name:type
SERVICES=(
  "account-service:spring-boot"
  "auth-service:spring-boot"
  "transaction-service:spring-boot"
  "wallet-service:spring-boot"
  "investment-service:spring-boot"
  "lending-service:spring-boot"
  "fx-service:spring-boot"
  "statement-service:spring-boot"
  "backoffice-service:spring-boot"
  "partner-service:spring-boot"
  "promotion-service:spring-boot"
  "support-service:spring-boot"
  "compliance-service:spring-boot"
  "cms-service:spring-boot"
  "dispute-service:spring-boot"
  "integration-service:spring-boot"
  "product-catalog-service:spring-boot"
  "billing-service:quarkus"
  "notification-service:quarkus"
  "gateway-service:quarkus"
  "api-portal-service:quarkus"
  "kyc-service:python"
  "analytics-service:python"
)

echo "═══════════════════════════════════════════════════"
echo "  PayU Build Pipeline — Tag: ${TAG}"
echo "  Services: ${#SERVICES[@]}"
echo "═══════════════════════════════════════════════════"

for entry in "${SERVICES[@]}"; do
  SVC_NAME="${entry%%:*}"
  SVC_TYPE="${entry##*:}"
  RUN_NAME="build-${SVC_NAME}-${TAG//\./-}-$(date +%s)"

  echo ""
  echo "▶ Triggering build: ${SVC_NAME} (${SVC_TYPE}) → ${TAG}"

  cat <<YAML | oc apply -n "${NAMESPACE}" -f -
apiVersion: tekton.dev/v1
kind: PipelineRun
metadata:
  name: ${RUN_NAME}
  namespace: ${NAMESPACE}
  labels:
    app: payu
    service: ${SVC_NAME}
    version: ${TAG}
spec:
  pipelineRef:
    name: payu-build-pipeline
  params:
    - name: service-name
      value: "${SVC_NAME}"
    - name: git-url
      value: "${GIT_URL}"
    - name: git-revision
      value: "${GIT_REV}"
    - name: service-type
      value: "${SVC_TYPE}"
    - name: build-image-tag
      value: "${TAG}"
  workspaces:
    - name: source
      volumeClaimTemplate:
        spec:
          accessModes:
            - ReadWriteOnce
          resources:
            requests:
              storage: 2Gi
  taskRunTemplate:
    serviceAccountName: pipeline
YAML

  echo "  ✓ PipelineRun created: ${RUN_NAME}"
  # Small delay to avoid API flooding
  sleep 1
done

echo ""
echo "═══════════════════════════════════════════════════"
echo "  All ${#SERVICES[@]} PipelineRuns triggered!"
echo "  Monitor: oc get pipelinerun -n ${NAMESPACE} -w"
echo "═══════════════════════════════════════════════════"
