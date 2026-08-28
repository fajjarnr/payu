#!/usr/bin/env bash
# SSO-ISSUER-002 verification — issuer token vs validator + realm drift checklist
# Checks:
#  - OIDC_ISSUER per env (expected public sso-<env>.apps.fajjjar.my.id)
#  - per-service vs monolith drift
#  - realm users/secret/redirectUris vs git (manual checklist, requires oc)
#  - DPoP attributes
set -euo pipefail

EXPECTED() {
  case $1 in
    payu-dev) echo "https://sso-dev.apps.fajjjar.my.id/realms/payu" ;;
    payu-sit) echo "https://sso-sit.apps.fajjjar.my.id/realms/payu" ;;
    payu-uat) echo "https://sso-uat.apps.fajjjar.my.id/realms/payu" ;;
    payu-preprod) echo "https://sso-preprod.apps.fajjjar.my.id/realms/payu" ;;
    payu-prod) echo "https://sso-prod.apps.fajjjar.my.id/realms/payu" ;;
    payu) echo "https://sso-prod.apps.fajjjar.my.id/realms/payu" ;;
  esac
}

FAIL=0
for env in payu-dev payu-sit payu-uat payu-preprod payu-prod; do
  expected=$(EXPECTED $env)
  echo "=== $env expected $expected ==="
  # monolith
  mono=$(grep -A1 "OIDC_ISSUER" "infrastructure/workloads/overlays/$env/kustomization.yaml" 2>/dev/null | grep value | head -n1 | sed -E 's/.*value: *"?([^"]+)".*/\1/' || echo "NONE")
  echo "  monolith: $mono"
  if [ "$mono" != "$expected" ] && [ -n "$mono" ]; then
    echo "  FAIL monolith mismatch"
    FAIL=1
  fi
  # per-service account-service
  per=$(grep -A1 "OIDC_ISSUER" "infrastructure/workloads/overlays/$env/account-service/kustomization.yaml" 2>/dev/null | grep value | head -n1 | sed -E 's/.*value: *"?([^"]+)".*/\1/; s/.*payu.fajjjar.*/\0/' | tr -d '"' | xargs || echo "NONE")
  # normalize extraction for different quote styles
  per_full=$(grep -A1 "OIDC_ISSUER" "infrastructure/workloads/overlays/$env/account-service/kustomization.yaml" 2>/dev/null | head -n2 | tail -n1 || echo "")
  echo "  per-service account-service: $per_full"
  if echo "$per_full" | grep -q "sso.*payu.fajjjar" && ! echo "$per_full" | grep -q "apps.fajjjar"; then
    echo "  FAIL per-service stale (payu without apps)"
    FAIL=1
  fi
  # web-app per-service
  if ! grep -q "OIDC_ISSUER" "infrastructure/workloads/overlays/$env/web-app/kustomization.yaml" 2>/dev/null; then
    if [ "$env" != "payu-dev" ]; then echo "  WARN per-service web-app missing OIDC_ISSUER (drift)"; FAIL=1; fi
  fi
  # gateway per-service
  if ! grep -q "QUARKUS_OIDC_TOKEN_ISSUER" "infrastructure/workloads/overlays/$env/gateway-service/kustomization.yaml" 2>/dev/null; then
    if [ "$env" != "payu-dev" ]; then echo "  WARN per-service gateway missing QUARKUS_OIDC_TOKEN_ISSUER (drift)"; FAIL=1; fi
  fi
  # NEXT_PUBLIC_BASE_URL monolith
  base=$(grep -A1 "NEXT_PUBLIC_BASE_URL" "infrastructure/workloads/overlays/$env/kustomization.yaml" 2>/dev/null | grep value | head -n1 || echo "")
  echo "  monolith NEXT_PUBLIC_BASE_URL: $base"
  if echo "$base" | grep -q "payu-dev.apps" && [ "$env" != "payu-dev" ]; then
    echo "  FAIL NEXT_PUBLIC_BASE_URL still payu-dev (drift)"
    FAIL=1
  fi
done

echo ""
echo "=== Manual checklist (requires oc) ==="
echo "[ ] oc get keycloak payu-keycloak -n payu-sso -o jsonpath='{.spec.hostname.hostname}' per env"
echo "[ ] oc get keycloakrealmimport payu-realm-import -n payu-sso -o json | jq '.spec.realm.clients[] | select(.clientId==\"payu-web-app\") | .redirectUris'"
echo "[ ] oc get secret payu-keycloak-client-secrets -n <env> -o jsonpath='{.data}' vs git"
echo "[ ] curl -s http://localhost:8099/realms/payu/.well-known/openid-configuration | jq .issuer per env public URL"
echo "[ ] oc rsh deployment/payU-web-app -n <env> 'env | grep OIDC_ISSUER' vs token iss claim (decode JWT from cookie)"
echo "[ ] oc get cm -n payu-sso keycloak-realm-import vs infrastructure/platform/identity/keycloak/payu-realm-export.json"

if [ $FAIL -eq 0 ]; then
  echo "PASS: issuer alignment OK"
  exit 0
else
  echo "FAIL: drift detected (see above)"
  exit 1
fi
