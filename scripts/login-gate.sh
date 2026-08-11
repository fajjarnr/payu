#!/usr/bin/env bash
# ============================================================================
# LOGIN-006 release gate — browser login E2E (BFF → gateway → auth → Keycloak),
# fail-closed: ANY failed Playwright assertion fails the gate with non-zero exit.
#
# Runs the minimal login chain from login-gate-compose.yml with PUBLIC images
# only (no Red Hat registry auth), so it works on CI runners and local hosts.
#
# Usage:
#   ./scripts/login-gate.sh            # build + start + run gate (default)
#   ./scripts/login-gate.sh --skip-build
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT/infrastructure/local/podman/login-gate-compose.yml"
GATE_COMPOSE="podman compose -f $COMPOSE_FILE"
SKIP_BUILD="${1:-}"

# Prefer docker on GitHub runners, podman locally
if command -v podman >/dev/null 2>&1 && [ -z "${GITHUB_ACTIONS:-}" ]; then
  COMPOSE="podman compose -f $COMPOSE_FILE"
  CONTAINER_RUNTIME="podman"
else
  COMPOSE="docker compose -f $COMPOSE_FILE"
  CONTAINER_RUNTIME="docker"
fi

echo "==> LOGIN gate ($CONTAINER_RUNTIME compose)"
echo "    Hosts entry required for the browser to reach Keycloak at http://keycloak:8099"

# The browser resolves `keycloak` through the runner's /etc/hosts (CI runners
# allow this as root). Idempotent: never duplicates the entry.
if ! grep -q ' keycloak$' /etc/hosts; then
  if [ "$(id -u)" -eq 0 ]; then
    echo "127.0.0.1 keycloak" >> /etc/hosts
  else
    echo "!! /etc/hosts entry missing and not root — add: 127.0.0.1 keycloak" >&2
    exit 1
  fi
fi

cleanup() {
  echo "==> Stopping gate stack"
  $COMPOSE down --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

if [ "$SKIP_BUILD" != "--skip-build" ]; then
  echo "==> Packaging auth-service and gateway-service"
  (cd "$ROOT/backend" && mvn -q -pl auth-service,gateway-service -am package -DskipTests)
fi

echo "==> Starting gate stack"
$COMPOSE up -d --build --wait --wait-timeout 600

echo "==> Verifying the live chain (fail-closed)"
AUTH_LIVENESS=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8002/actuator/health/liveness || true)
GW_LIVENESS=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/q/health/live || true)
APP_HEALTH=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:3001/api/health || true)
echo "    auth liveness=$AUTH_LIVENESS gateway liveness=$GW_LIVENESS web-app health=$APP_HEALTH"
[ "$AUTH_LIVENESS" = "200" ] || { echo "!! auth-service not ready"; exit 1; }
[ "$GW_LIVENESS" = "200" ] || { echo "!! gateway not ready"; exit 1; }
[ "$APP_HEALTH" = "200" ] || { echo "!! web-app not ready"; exit 1; }

# Negative check first: unauthenticated callback must NOT exchange (state gate)
STATE_CODE=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:3001/api/auth/callback?code=stale&state=wrong")
echo "    unauthenticated callback status=$STATE_CODE (expect 307 → /login)"
[ "$STATE_CODE" = "307" ] || { echo "!! callback did not fail closed"; exit 1; }

echo "==> Running Playwright login E2E (14 tests)"
(
  cd "$ROOT/frontend/web-app"
  npx playwright test e2e/login-flow.spec.ts --reporter=line
)

echo "==> LOGIN gate GREEN"
