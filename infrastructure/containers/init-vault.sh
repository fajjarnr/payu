#!/bin/bash

# Vault Initialization Script
# This script configures Vault with the PayU secrets

set -e

VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="${VAULT_TOKEN:?ERROR: VAULT_TOKEN must be set}"

# Read credentials from environment (fail if not set)
DB_PASSWORD="${DB_PASSWORD:?ERROR: DB_PASSWORD must be set}"
DB_USERNAME="${DB_USERNAME:-payu}"
KEYCLOAK_CLIENT_SECRET="${KEYCLOAK_CLIENT_SECRET:?ERROR: KEYCLOAK_CLIENT_SECRET must be set}"
KEYCLOAK_ADMIN_USER="${KEYCLOAK_ADMIN_USER:?ERROR: KEYCLOAK_ADMIN_USER must be set}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:?ERROR: KEYCLOAK_ADMIN_PASSWORD must be set}"
GRAFANA_ADMIN_USER="${GRAFANA_ADMIN_USER:-admin}"
GRAFANA_ADMIN_PASSWORD="${GRAFANA_ADMIN_PASSWORD:?ERROR: GRAFANA_ADMIN_PASSWORD must be set}"

export VAULT_ADDR
export VAULT_TOKEN

echo "Initializing Vault for PayU..."

# Enable KV secrets engine v2
vault secrets enable -path=secret kv-v2 2>/dev/null || echo "KV v2 already enabled"

# Database secrets
echo "Writing database secrets..."
vault kv put secret/account-service/db \
    url="jdbc:postgresql://postgres:5432/payu_account" \
    username="$DB_USERNAME" \
    password="$DB_PASSWORD"

vault kv put secret/auth-service/db \
    url="jdbc:postgresql://postgres:5432/payu_auth" \
    username="$DB_USERNAME" \
    password="$DB_PASSWORD"

vault kv put secret/transaction-service/db \
    url="jdbc:postgresql://postgres:5432/payu_transaction" \
    username="$DB_USERNAME" \
    password="$DB_PASSWORD"

vault kv put secret/wallet-service/db \
    url="jdbc:postgresql://postgres:5432/payu_wallet" \
    username="$DB_USERNAME" \
    password="$DB_PASSWORD"

# Keycloak secrets
echo "Writing Keycloak secrets..."
vault kv put secret/auth-service/keycloak \
    server-url="http://keycloak:8080" \
    realm="payu" \
    client-id="auth-service" \
    client-secret="$KEYCLOAK_CLIENT_SECRET" \
    admin-username="$KEYCLOAK_ADMIN_USER" \
    admin-password="$KEYCLOAK_ADMIN_PASSWORD"

# Kafka secrets
echo "Writing Kafka secrets..."
vault kv put secret/common/kafka \
    bootstrap-servers="kafka:29092"

# Redis secrets
echo "Writing Redis secrets..."
vault kv put secret/gateway-service/redis \
    hosts="redis://redis:6379" \
    password=""

# Grafana secrets
echo "Writing Grafana secrets..."
vault kv put secret/common/grafana \
    admin-user="$GRAFANA_ADMIN_USER" \
    admin-password="$GRAFANA_ADMIN_PASSWORD"

echo "Vault initialization complete!"
echo ""
echo "Available secrets:"
echo "  secret/account-service/db"
echo "  secret/auth-service/db"
echo "  secret/transaction-service/db"
echo "  secret/wallet-service/db"
echo "  secret/auth-service/keycloak"
echo "  secret/common/kafka"
echo "  secret/gateway-service/redis"
echo "  secret/common/grafana"
