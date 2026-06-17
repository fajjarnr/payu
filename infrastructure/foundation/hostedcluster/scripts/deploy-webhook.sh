#!/bin/bash
set -euo pipefail

# deploy-webhook.sh
# Automates the creation of self-signed TLS certs, deploys the webhook service,
# and configures the MutatingWebhookConfiguration with the correct CA bundle.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEBHOOK_DIR="${SCRIPT_DIR}/../webhook"
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "${TEMP_DIR}"' EXIT

echo "=== Deploying HCP Audience Fixer Mutating Webhook ==="

# 1. Ensure Namespace Exists
oc create namespace payu-system --insecure-skip-tls-verify=true 2>/dev/null || true

# 2. Generate TLS Certificates
echo "Generating self-signed TLS certificates..."
cat > "${TEMP_DIR}/openssl.cnf" <<EOF
[req]
req_extensions = v3_req
distinguished_name = req_distinguished_name
prompt = no
[req_distinguished_name]
CN = hcp-audience-fixer.payu-system.svc
[v3_req]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
subjectAltName = @alt_names
[alt_names]
DNS.1 = hcp-audience-fixer.payu-system.svc
DNS.2 = hcp-audience-fixer.payu-system.svc.cluster.local
EOF

openssl genrsa -out "${TEMP_DIR}/tls.key" 2048
openssl req -new -key "${TEMP_DIR}/tls.key" -out "${TEMP_DIR}/tls.csr" -config "${TEMP_DIR}/openssl.cnf"
openssl x509 -req -in "${TEMP_DIR}/tls.csr" -signkey "${TEMP_DIR}/tls.key" -out "${TEMP_DIR}/tls.crt" -days 3650 -extensions v3_req -extfile "${TEMP_DIR}/openssl.cnf"

# 3. Create TLS Secret
echo "Recreating TLS Secret..."
oc delete secret hcp-audience-fixer-tls -n payu-system --insecure-skip-tls-verify=true 2>/dev/null || true
oc create secret tls hcp-audience-fixer-tls \
  --cert="${TEMP_DIR}/tls.crt" \
  --key="${TEMP_DIR}/tls.key" \
  -n payu-system --insecure-skip-tls-verify=true

# 4. Apply manifests (ConfigMap, Deployment, Service)
echo "Applying webhook deployment manifests..."
oc apply -f "${WEBHOOK_DIR}/manifests.yaml" --insecure-skip-tls-verify=true

# 5. Extract CA Bundle and apply MutatingWebhookConfiguration
echo "Injecting CA bundle into MutatingWebhookConfiguration..."
CA_BUNDLE=$(base64 < "${TEMP_DIR}/tls.crt" | tr -d '\n')

sed "s/\${CA_BUNDLE}/${CA_BUNDLE}/g" "${WEBHOOK_DIR}/mutating-webhook-configuration.yaml" > "${TEMP_DIR}/mutating-webhook-configuration-active.yaml"

oc apply -f "${TEMP_DIR}/mutating-webhook-configuration-active.yaml" --insecure-skip-tls-verify=true

# 6. Restart deployment to ensure it uses the new certificates
echo "Rollout restarting hcp-audience-fixer..."
oc rollout restart deployment/hcp-audience-fixer -n payu-system --insecure-skip-tls-verify=true
oc rollout status deployment/hcp-audience-fixer -n payu-system --insecure-skip-tls-verify=true

echo "=== Webhook Successfully Deployed & Active ==="
