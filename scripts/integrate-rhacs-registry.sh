#!/usr/bin/env bash
set -eo pipefail

echo "=========================================================================="
echo "🛡️  Setting up RHACS Integration for OpenShift Internal Image Registry "
echo "=========================================================================="

# 1. Retrieve RHACS Central Password and URL
echo "🔄 Retrieving ACS Central credentials..."
ROX_PASSWORD=$(oc get secret central-htpasswd -n stackrox -o jsonpath='{.data.password}' | base64 -d)
ROX_CENTRAL_URL="https://$(oc get route central -n stackrox -o jsonpath='{.spec.host}')"

# 2. Grant image-puller right to the pipeline SA (already done, but making it idempotent)
echo "🔑 Ensuring pipeline serviceaccount has image-puller rights..."
oc adm policy add-cluster-role-to-user system:image-puller system:serviceaccount:payu-cicd:pipeline || true
oc adm policy add-cluster-role-to-user registry-viewer system:serviceaccount:payu-cicd:pipeline || true

# 3. Create a long-lived SA token from the pipeline SA for RHACS to use
echo "🪙 Generating long-lived access token for internal registry..."
TOKEN=$(oc create token pipeline -n payu-cicd --duration=8760h)

# 4. Push Image Integration to RHACS Central
echo "📡 Configuring Image Integration in Central API..."
# Use a static ID so we PUT (update) instead of POST (create duplicates)
INTEGRATION_ID="6da0559c-f595-45ea-95e2-d062e3f48849"

curl -sk -X PUT -u "admin:${ROX_PASSWORD}" -H "Content-Type: application/json" \
  "${ROX_CENTRAL_URL}/v1/imageintegrations/${INTEGRATION_ID}" -d '{
  "id": "'"${INTEGRATION_ID}"'",
  "name": "OpenShift Internal Registry",
  "type": "docker",
  "categories": ["REGISTRY"],
  "docker": {
    "endpoint": "image-registry.openshift-image-registry.svc:5000",
    "username": "serviceaccount",
    "password": "'"${TOKEN}"'",
    "insecure": true
  },
  "skipTestIntegration": true
}' > /dev/null

echo ""
echo "✅ Successfully configured OpenShift Internal Registry integration in RHACS."
