#!/usr/bin/env bash
# Vault DR Restore Script for PayU Lab
# Usage: ./scripts/vault-dr-restore.sh
#
# Simulates disaster recovery for Vault in dev mode (inmem storage).
# After Vault pod restart, all secrets are lost. This script re-seeds them.
# ============================================================

set -euo pipefail

NAMESPACE="payu-dev"
VAULT_POD=$(oc get pods -n "$NAMESPACE" -l app.kubernetes.io/name=vault -o jsonpath='{.items[0].metadata.name}')
VAULT_ADDR="http://127.0.0.1:8200"
VAULT_TOKEN="payu-root-token-2026-lab"

echo "=== PayU Vault DR Restore ==="
echo "Namespace: $NAMESPACE"
echo "Pod: $VAULT_POD"
echo ""

random_alnum() {
  tr -dc 'A-Za-z0-9' </dev/urandom | head -c "$1"
}

random_b64() {
  head -c "$1" /dev/urandom | base64 | tr -d '\n'
}

oc exec -n "$NAMESPACE" "$VAULT_POD" -- /bin/sh -c "
export VAULT_ADDR='$VAULT_ADDR'
export VAULT_TOKEN='$VAULT_TOKEN'

echo 'Enabling KV-v2...'
vault secrets enable -path=secret kv-v2 2>/dev/null || true

POSTGRES_HOST='payu-postgres-primary.payu-dev.svc.cluster.local'
POSTGRES_PORT='5432'
POSTGRES_USERNAME='payu'
POSTGRES_PASSWORD='\$(random_alnum 32)'
POSTGRES_DATABASE='payu'
KEYCLOAK_DATABASE='keycloak'
KEYCLOAK_ADMIN_USERNAME='admin'
KEYCLOAK_ADMIN_PASSWORD='\$(random_alnum 32)'
KEYCLOAK_CLIENT_SECRET='\$(random_alnum 48)'
JWT_SECRET='\$(random_alnum 64)'
AES_ENCRYPTION_KEY='\$(random_b64 32)'
SECRET_KEY='\$(random_b64 32)'

echo 'Seeding db-credentials...'
vault kv put secret/payu/db-credentials \
  username=\"\$POSTGRES_USERNAME\" \
  password=\"\$POSTGRES_PASSWORD\" \
  host=\"\$POSTGRES_HOST\" \
  port=\"\$POSTGRES_PORT\" \
  database=\"\$POSTGRES_DATABASE\"

echo 'Seeding jwt-secret...'
vault kv put secret/payu/jwt-secret JWT_SECRET=\"\$JWT_SECRET\"

echo 'Seeding encryption-keys...'
vault kv put secret/payu/encryption-keys \
  AES_ENCRYPTION_KEY=\"\$AES_ENCRYPTION_KEY\" \
  SECRET_KEY=\"\$SECRET_KEY\"

echo 'Seeding keycloak-credentials...'
vault kv put secret/payu/keycloak-credentials \
  KEYCLOAK_CLIENT_SECRET=\"\$KEYCLOAK_CLIENT_SECRET\" \
  KEYCLOAK_ADMIN_USERNAME=\"\$KEYCLOAK_ADMIN_USERNAME\" \
  KEYCLOAK_ADMIN_PASSWORD=\"\$KEYCLOAK_ADMIN_PASSWORD\"

echo 'Seeding keycloak-db...'
vault kv put secret/payu/keycloak-db \
  POSTGRES_DATABASE=\"\$KEYCLOAK_DATABASE\" \
  POSTGRES_EXTERNAL_ADDRESS=\"\$POSTGRES_HOST\" \
  POSTGRES_EXTERNAL_PORT=\"\$POSTGRES_PORT\" \
  POSTGRES_HOST=\"\$POSTGRES_HOST\" \
  POSTGRES_USERNAME=\"\$POSTGRES_USERNAME\" \
  POSTGRES_PASSWORD=\"\$POSTGRES_PASSWORD\" \
  POSTGRES_SUPERUSER=\"false\" \
  SSLMODE=\"prefer\"

echo 'Vault DR restore complete.'
"

echo ""
echo "=== Forcing ExternalSecrets reconciliation ==="
for es in db-credentials encryption-keys jwt-secret keycloak-credentials keycloak-db-secret; do
  oc annotate externalsecret "$es" -n "$NAMESPACE" force-sync="$(date +%s)" --overwrite
done

echo ""
echo "=== DR Drill Summary ==="
echo "Time: $(date -Iseconds)"
echo "All 5 secrets re-seeded in Vault."
echo "ExternalSecrets annotated for forced reconciliation."
echo "Verify with: oc get externalsecrets -n $NAMESPACE"
