# PayU E2E env shim — sourced by scripts/e2e/*.sh (PROMO-2026-08).
# Usage: PAYU_ENV=payu-uat bash scripts/e2e/wallet-balance.sh
# Defaults preserve the historical payu-dev behavior byte-for-byte.
PAYU_ENV="${PAYU_ENV:-payu-dev}"
case "$PAYU_ENV" in
  payu-dev)    SSO_HOST="sso-dev.apps.payu.ocp.fajjjar.my.id"; APP_HOST="payu-dev.apps.payu.ocp.fajjjar.my.id" ;;
  payu-sit)    SSO_HOST="sso-sit.apps.fajjjar.my.id"; APP_HOST="app-sit.apps.fajjjar.my.id" ;;
  payu-uat)    SSO_HOST="sso-uat.apps.fajjjar.my.id"; APP_HOST="app-uat.apps.fajjjar.my.id" ;;
  payu-preprod) SSO_HOST="sso-preprod.apps.fajjjar.my.id"; APP_HOST="app-preprod.apps.fajjjar.my.id" ;;
  *) echo "ERROR: unknown PAYU_ENV=$PAYU_ENV (want payu-dev|payu-sit|payu-uat|payu-preprod)" >&2; exit 1 ;;
esac
if [ "$PAYU_ENV" = "payu-dev" ]; then
  E2E_SECRET_FILE="${E2E_SECRET_FILE:-/tmp/client-secret.txt}"
else
  E2E_SECRET_FILE="${E2E_SECRET_FILE:-/tmp/client-secret-${PAYU_ENV}.txt}"
fi
