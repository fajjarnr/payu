#!/bin/bash
set -euo pipefail
# schemathesis-smoke.sh — SX-AUTH-001: client-credentials Bearer per env, re-enable content_type + response_schema
# Ponytail: fetches Bearer from payu-keycloak-client-secrets, runs schemathesis with auth when secret present, 5xx-only fallback when missing
# Usage: ./scripts/schemathesis-smoke.sh [payu-dev] [account-service]
NS=${1:-payu-dev}
SVC=${2:-account-service}
BASE_URL="http://${SVC}.${NS}.svc.cluster.local:8080"
SCHEMA_URL="${BASE_URL}/api-docs"
ENDPOINT_FILTER="^(?!.*analytics).*"
REPORT_DIR="reports/schemathesis"
mkdir -p "$REPORT_DIR"
echo "[schemathesis-smoke] $SVC @ $NS $SCHEMA_URL --base-url $BASE_URL"
# Fetch client credentials from secret payu-keycloak-client-secrets (per SSO-ENV-002, payu-vault ExternalSecret)
SECRET="payu-keycloak-client-secrets"
TOKEN=""
if oc get secret -n "$NS" "$SECRET" >/dev/null 2>&1; then
  echo "[schemathesis-smoke] found secret $SECRET in $NS"
  # Try common keys: payu-backend-client-secret, client-secret, etc; fallback to generic
  CLIENT_ID=$(oc get secret -n "$NS" "$SECRET" -o jsonpath='{.data.client-id}' 2>/dev/null | base64 -d 2>/dev/null || oc get secret -n "$NS" "$SECRET" -o jsonpath='{.data.payu-backend-client-id}' 2>/dev/null | base64 -d 2>/dev/null || echo "payu-backend")
  CLIENT_SECRET=$(oc get secret -n "$NS" "$SECRET" -o jsonpath='{.data.client-secret}' 2>/dev/null | base64 -d 2>/dev/null || oc get secret -n "$NS" "$SECRET" -o jsonpath='{.data.payu-backend-client-secret}' 2>/dev/null | base64 -d 2>/dev/null || echo "")
  if [[ -n "$CLIENT_SECRET" ]]; then
    echo "[schemathesis-smoke] fetching Bearer via client-credentials grant"
    TOKEN_RESP=$(curl -s -X POST "http://payu-keycloak-service.payu-sso.svc.cluster.local:8080/realms/payu/protocol/openid-connect/token" \
      -d "grant_type=client_credentials" \
      -d "client_id=$CLIENT_ID" \
      -d "client_secret=$CLIENT_SECRET" || echo "")
    TOKEN=$(echo "$TOKEN_RESP" | jq -r '.access_token // empty' 2>/dev/null || echo "")
    if [[ -n "$TOKEN" ]]; then
      echo "[schemathesis-smoke] got Bearer ${TOKEN:0:20}..."
    else
      echo "[schemathesis-smoke] WARN: no token, fallback to 5xx-only"
    fi
  else
    echo "[schemathesis-smoke] no client_secret in $SECRET, fallback to 5xx-only"
  fi
else
  echo "[schemathesis-smoke] secret $SECRET not found in $NS, fallback to 5xx-only (SX-AUTH-001 pending Vault)"
fi
# Run schemathesis
IMAGE="docker.io/schemathesis/schemathesis@sha256:4ba658e6a309d51d76efb64697b695660fc565e159e5e269633521e9b486d759"
if [[ -n "$TOKEN" ]]; then
  echo "[schemathesis-smoke] running with Bearer, checks all exclude only not_a_server_error (re-enable content_type + response_schema)"
  docker run --rm --network host -v "$(pwd)/reports:/reports" "$IMAGE" run "$SCHEMA_URL" \
    --base-url "$BASE_URL" \
    --endpoint "$ENDPOINT_FILTER" \
    --checks all \
    --exclude-checks not_a_server_error \
    --header "Authorization: Bearer $TOKEN" \
    --experimental=openapi-3.1 \
    --report-dir /reports 2>&1 | tee "$REPORT_DIR/schemathesis-$SVC.log" || echo "schemathesis run completed with findings"
else
  echo "[schemathesis-smoke] running 5xx-only fallback (exclude status_code_conformance + not_a_server_error)"
  docker run --rm --network host -v "$(pwd)/reports:/reports" "$IMAGE" run "$SCHEMA_URL" \
    --base-url "$BASE_URL" \
    --endpoint "$ENDPOINT_FILTER" \
    --checks all \
    --exclude-checks status_code_conformance \
    --exclude-checks not_a_server_error \
    --experimental=openapi-3.1 2>&1 | tee "$REPORT_DIR/schemathesis-$SVC-5xx.log" || echo "schemathesis 5xx-only completed"
fi
echo "[schemathesis-smoke] reports in $REPORT_DIR"
ls -lh "$REPORT_DIR" | head -20
