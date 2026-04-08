#!/bin/bash
# Trigger PayU Microservices Pipelines
# Triggers both backend (Spring Boot, Quarkus, Python) and frontend (Next.js)

NAMESPACE="payu-cicd"
GIT_URL="https://github.com/fajjarnr/payu.git"
GIT_REVISION="main"
IMAGE_TAG="v1.7.8"

log() { echo "=== $1 ==="; }

trigger_pipeline() {
  local svc_name=$1
  local svc_path=$2
  local svc_type=$3
  local base_dir=${4:-backend}
  
  local safe_name=$(echo "$svc_name" | sed 's/\//-/g')
  
  echo "Triggering $svc_name (Type: $svc_type, Path: $base_dir/$svc_path)..."
  
  cat <<EOF | oc create -f -
apiVersion: tekton.dev/v1
kind: PipelineRun
metadata:
  generateName: build-${safe_name}-
  namespace: ${NAMESPACE}
  labels:
    app: payu
    service: ${safe_name}
spec:
  pipelineRef:
    name: payu-build-pipeline
  params:
    - name: service-name
      value: "${svc_name}"
    - name: service-path
      value: "${svc_path}"
    - name: service-base-dir
      value: "${base_dir}"
    - name: git-url
      value: "${GIT_URL}"
    - name: git-revision
      value: "${GIT_REVISION}"
    - name: service-type
      value: "${svc_type}"
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
  sleep 2
}

log "Deploying Backend Services (Spring Boot)"
for svc in account-service auth-service transaction-service wallet-service investment-service lending-service fx-service statement-service backoffice-service partner-service promotion-service support-service compliance-service cms-service dispute-service integration-service product-catalog-service; do
  trigger_pipeline "$svc" "$svc" "spring-boot" "backend"
done

log "Deploying Backend Services (Quarkus)"
for svc in billing-service notification-service gateway-service api-portal-service; do
  trigger_pipeline "$svc" "$svc" "quarkus" "backend"
done

log "Deploying Required Simulator"
trigger_pipeline "biller-simulator" "simulators/biller-simulator" "quarkus" "backend"

log "Deploying Backend Services (Python/FastAPI)"
for svc in kyc-service analytics-service; do
  trigger_pipeline "$svc" "$svc" "python" "backend"
done

log "Deploying Frontend Application (Next.js)"
trigger_pipeline "web-app" "web-app" "nextjs" "frontend"

echo "All services triggered successfully! Run 'tkn pr ls -n payu-cicd' to monitor."
