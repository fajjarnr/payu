#!/bin/bash
set -e

echo "Creating databases in payu-database-1..."

DBS=(
  payu_auth
  payu_transaction
  payu_wallet
  payu_notification
  payu_billing
  keycloak
  payu_kyc
  payu_analytics
  payu_compliance
  payu_bifast
  payu_dukcapil
  payu_qris
  payu_investment
  payu_lending
  payu_loan_origination
  payu_biller
  payu_va_simulator
  payu_backoffice
  payu_partner
  payu_promotion
  payu_support
  payu_statement
  payu_fx
  payu_cms
  payu_abtesting
  payu_dispute
  payu_integration
  payu_products
  payu_gateway
  payu_api_portal
  payu_sonarqube
)

for db in "${DBS[@]}"; do
  echo "Ensuring DB: ${db}"
  oc exec payu-database-1 -n payu-dev -c postgres -- psql -U postgres -tc "SELECT 1 FROM pg_database WHERE datname = '${db}'" | grep -q 1 || \
  oc exec payu-database-1 -n payu-dev -c postgres -- psql -U postgres -c "CREATE DATABASE ${db} OWNER payu;" || true
  oc exec payu-database-1 -n payu-dev -c postgres -- psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE ${db} TO payu;" || true
done

echo "All databases initialized successfully!"
