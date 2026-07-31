#!/bin/bash
# ============================================
# PayU E2E Test Fixture Bootstrap
# Creates test wallet + pocket rows for the customer1 Keycloak user
# so that the cards CRUD E2E test can run end-to-end.
#
# Run once after a fresh cluster bootstrap:
#   PG=$(oc get pod -n payu-dev -l app.kubernetes.io/name=payu-postgres \
#        -o name --field-selector=status.phase=Running | head -1)
#   oc rsh -n payu-dev $PG psql -U payu -d payu_wallet -f scripts/e2e/wallet-bootstrap.sql
#
# The customer1 user_id (Keycloak subject claim) is the lookup key —
# CardService.createVirtualCard resolves the wallet by
#   walletPersistencePort.findByAccountId(accountId)
# which maps to `wallets.account_id` in the V1 schema.
# ============================================

\set ON_ERROR_STOP on

-- customer1 is the default test user created in Keycloak payu realm
-- password: customer1-test-pass  (reset before first E2E run via Admin API)
\set acct_id '\'7753193d-b7e7-4e1e-bcb8-f9e4612e9207\''
\set wallet_id '\'33333333-3333-3333-3333-333333333333\''

-- Old schema: V1__create_wallet_tables.sql (Optional<Wallet> lookup)
INSERT INTO wallets (id, account_id, balance, reserved_balance, currency, status, version, created_at, updated_at)
VALUES (:'wallet_id', :'acct_id', 10000000.0000, 0.0000, 'IDR', 'ACTIVE', 0, NOW(), NOW())
ON CONFLICT (account_id) DO NOTHING
RETURNING id, account_id, balance, status;

-- New schema: V3.1__create_pockets_table.sql (List<Pocket> lookup)
INSERT INTO pockets (id, account_id, name, description, currency, balance, status, created_at, updated_at)
VALUES (gen_random_uuid(), :'acct_id', 'E2E Test Pocket', 'Bootstrapped for E2E test', 'IDR', 10000000.00, 'ACTIVE', NOW(), NOW())
RETURNING id, account_id, name, balance;
