#!/bin/bash
set -euo pipefail
# promote-service.sh — per ADR-0066 polyrepo promotion via Tekton pipelineRun with target-env
# Usage: ./scripts/promote-service.sh <service> <target-env> <image-tag>
# Example: ./scripts/promote-service.sh transaction-service sit 1.18.15
# Ponytail: wraps oc create pipelinerun with image-tag + target-env per payu-service-pipeline.yaml; logs to reports/
SERVICE=${1:-}
TARGET_ENV=${2:-}
IMAGE_TAG=${3:-$(jq -r .version package.json)}
if [[ -z "$SERVICE" || -z "$TARGET_ENV" ]]; then
  echo "Usage: $0 <service> <target-env> [image-tag]"
  echo "  service: account-service|transaction-service|...|web-app"
  echo "  target-env: dev|sit|uat|preprod|prod"
  exit 1
fi
PIPELINE="payu-${SERVICE}-pipeline"
RUN_NAME="${SERVICE}-promote-${TARGET_ENV}-$(date +%s)"
REPORT_DIR="reports/promote"
mkdir -p "$REPORT_DIR"
echo "[promote] $SERVICE → $TARGET_ENV @ $IMAGE_TAG via $PIPELINE"
# Use yaml manifest re-apply, not oc patch (repo rule)
cat <<EOF | oc apply -f -
apiVersion: tekton.dev/v1
kind: PipelineRun
metadata:
  name: $RUN_NAME
  namespace: payu-cicd
  labels:
    app.kubernetes.io/part-of: payu
    payu.io/service: $SERVICE
    payu.io/target-env: $TARGET_ENV
spec:
  pipelineRef:
    name: $PIPELINE
  params:
    - name: service-name
      value: $SERVICE
    - name: image
      value: image-registry.openshift-image-registry.svc:5000/payu-${TARGET_ENV}/$SERVICE
    - name: image-tag
      value: $IMAGE_TAG
    - name: target-env
      value: $TARGET_ENV
  workspaces:
    - name: source
      volumeClaimTemplate:
        spec:
          accessModes: [ReadWriteOnce]
          resources:
            requests:
              storage: 5Gi
    - name: dockerconfig
      secret:
        secretName: redhat-registry-pull
    - name: git-ssh
      secret:
        secretName: git-ssh-credentials
        optional: true
EOF
echo "[promote] created PipelineRun $RUN_NAME in payu-cicd"
oc get pipelinerun -n payu-cicd "$RUN_NAME" -o yaml | tee "$REPORT_DIR/${RUN_NAME}.yaml"
echo "[promote] tail logs: oc logs -f -n payu-cicd pipelinerun/$RUN_NAME"
# Also handle payu-cid alias namespace if exists (user asked payu-cid)
if oc get ns payu-cid >/dev/null 2>&1; then
  echo "[promote] payu-cid alias exists, also created in payu-cicd (real)"
fi
