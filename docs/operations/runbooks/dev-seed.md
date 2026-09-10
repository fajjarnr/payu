# Dev Seed Runbook — Money Journey Fixtures (RELAY-010)

Fresh `payu-dev` must run the money journey without manual SQL. Three layers,
each with an owner file. Live proof 2026-09-10: applied as app role `payu`
(RLS active), re-run `INSERT 0 0` idempotent.

## 1. Wallets — automatic via Flyway

`backend/wallet-service/.../db/migration/V124__seed_dev_journey_wallets.sql`
(SYSTEM bypass + `ON CONFLICT (account_id) DO NOTHING`, no ledger rows —
ledger is created by transfers, never hand-seeded). Runs on every
wallet-service boot/migrate. Balances mirror `db/seed/wallet-test-data.sql`
(which stays as local-compose reference only).

Verify (tenant GUC required — bare SELECT returns 0 rows by design):

```bash
oc exec -n payu-dev payu-database-1 -c postgres -- \
  env PGPASSWORD="$(oc get secret payu-database-app -n payu-dev -o jsonpath='{.data.password}' | base64 -d)" \
  psql -h localhost -U payu -d payu_wallet \
  -c "SET app.tenant_id='default'; SELECT account_id, balance FROM wallets WHERE account_id LIKE '750e%' ORDER BY 1;"
```

## 2. Users/Profiles/Accounts — automatic via Flyway

`backend/account-service/.../db/migration/V113__seed_dev_journey_users.sql`
(closes FE-AUDIT-007: `User` rows linked by Keycloak sub so
ownership-gated endpoints stop 403). Notes:

- Tenant context is `default`, NOT SYSTEM — `users`/`accounts` policies
  (V107/V108) have no SYSTEM escape hatch (`profiles` V112 does).
- `profiles` has no `user_id`/`created_at` columns (V10) — `id` shares the User PK.
- Plaintext PII is read-safe (`decryptFromDatabase` passes non-`ENC()` through;
  re-encrypted on next app write).

## 3. Keycloak realm users — import CR at create, Admin REST after

`infrastructure/platform/identity/keycloak/keycloak-realm-import.yaml` seeds
users only when the realm is created (RHBK never updates existing realms —
L-377). For a live realm, re-sync via Admin REST (full-representation
roundtrips, never partial PATCH):

1. `PUT /admin/realms/payu/users/profile` with `unmanagedAttributePolicy:
   ENABLED` (the field lives in UPConfig — `PUT /admin/realms/payu` 400s).
2. `GET` each user → set `attributes` → `PUT` back; create missing users per
   the import file; attach realm roles.
3. Leave foreign users (e.g. `probe1`) alone unless the owner says otherwise.

Live proof 2026-09-10 (RELAY-005): policy `None→ENABLED`, `customer1/2`
attrs+roles restored, `admin`+`backoffice` created 201.
