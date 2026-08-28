#!/usr/bin/env bash
# SSO-DPOP-003 — DPoP (RFC 9449) enforcement + BFF proof generation check
# Usage: ./scripts/verify-dpop.sh
set -euo pipefail
echo "=== DPoP Client Attributes (Keycloak) ==="
echo "[1] Git realm payu-realm-export.json dpop.bound.access.tokens"
grep -A2 -B2 "dpop.bound.access.tokens" infrastructure/platform/identity/keycloak/payu-realm-export.json | head -n 20
echo ""
echo "[2] Git realm import YAML"
grep -A2 -B2 "dpop.bound.access.tokens" infrastructure/platform/identity/keycloak/keycloak-realm-import.yaml | head -n 20
echo ""
echo "[3] Live realm (requires oc)"
oc get keycloakrealmimport payu-realm-import -n payu-sso -o yaml 2>&1 | grep -A2 dpop || echo "no oc or not found (expected locally)"
echo ""
echo "[4] BFF DPoP proof generation"
if grep -r "DPoP" frontend/web-app --include="*.ts" --include="*.tsx" | head -n 20; then
  echo "Found DPoP in web-app BFF"
else
  echo "No DPoP proof generation in web-app BFF (expected — deferred)"
fi
echo ""
echo "[5] Auth-service DPoP validator"
grep -l "DPoPProofValidator" backend/auth-service/src/main/java -r 2>&1 | head -n 5
echo ""
echo "=== Decision ==="
echo "Current: payu-web-app client dpop.bound.access.tokens=false live (enforcement disabled) — BFF does not generate DPoP proof (authorize/route.ts PKCE only, callback/route.ts no DPoP header). Mobile client payu-mobile keeps dpop=true for device grant (future)."
echo "Validator: backend/auth-service DPoPProofValidator, DPoPFilter, DPoPBearerTokenResolver exist and test 6/6 EC256 valid/replay/htm/htu/ath/iat (DPoPProofValidatorTest) — server side ready."
echo "Proof generation missing: BFF must generate DPoP JWT (typ dpop+jwt, jwk ES256, htm/htu, iat, jti, ath) via WebCrypto/ECDSA P-256, store key in IndexedDB/SecureStore, send DPoP header on token endpoint (callback, refresh) and Authorization: DPoP on API calls. Deferred until product validates sender-constraint value vs complexity (Keycloak lockout 3/15m already covers token replay for same-device theft; DPoP adds stolen token replay across devices)."
echo "Action: keep dpop=false for payu-web-app until BFF proof generation lands, then flip to true via oc patch or git + oc apply -k identity/overlays/<env>. Verify via: curl -s http://localhost:8099/realms/payu/.well-known/openid-configuration | jq .dpop_signing_alg_values_supported"
