# Dev Seed Runbook — Money Journey Fixtures (RELAY-010)

Fresh `payu-dev` must run the money journey without manual SQL. Three layers,
each with an owner file. Live proof 2026-09-10: applied as app role `payu`
(RLS active), re-run `INSERT 0 0` idempotent.

**Status 2026-10-20 (fresh cluster `payu-drrpz`):** layer 1 (wallets) dan layer 2
(users/profiles/accounts) **otomatis** via Flyway. Layer 3 (realm Keycloak) **tetap
MANUAL** karena dev overlay menghapus `KeycloakRealmImport/payu-realm-import`
(L-377) -> realm harus di-import sekali lewat Admin REST API, lalu diverifikasi.
Panduan lengkap + perintah yang sudah terverifikasi live ada di bagian 4-7.

---

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

Verified live 2026-10-20 (cluster baru): 5 row, semua `ACTIVE`, saldo
`10000000.0000` / `5000000.0000` / `3000000.0000` / `5000000.0000` / `0.0000`.

## 2. Users/Profiles/Accounts — automatic via Flyway

`backend/account-service/.../db/migration/V113__seed_dev_journey_users.sql`
(closes FE-AUDIT-007: `User` rows linked by Keycloak sub so
ownership-gated endpoints stop 403). Notes:

- Tenant context is `default`, NOT SYSTEM — `users`/`accounts` policies
  (V107/V108) have no SYSTEM escape hatch (`profiles` V112 does).
- `profiles` has no `user_id`/`created_at` columns (V10) — `id` shares the User PK.
- Plaintext PII is read-safe (`decryptFromDatabase` passes non-`ENC()` through;
  re-encrypted on next app write).

Verify (tenant GUC wajib: tanpa itu 0 row, itu RLS by design):

```bash
oc exec -n payu-dev payu-database-1 -c postgres -- \
  env PGPASSWORD="$(oc get secret payu-database-app -n payu-dev -o jsonpath='{.data.password}' | base64 -d)" \
  psql -h localhost -U payu -d payu_account \
  -c "SET app.tenant_id='default'; SELECT u.username, u.external_id, a.id AS account_id, a.account_number FROM users u JOIN accounts a ON a.user_id = u.id ORDER BY 1;"
```

Verified live 2026-10-20: 3 baris (customer1, customer2, admin). `external_id`
**harus sama persis** dengan `sub` Keycloak (lihat bagian 4): kalau berbeda,
tiap endpoint yang digate ownership akan 403.

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

---

## 4. Fresh-cluster: import realm `payu` (MANUAL, wajib)

Dev overlay (`infrastructure/platform/identity/overlays/dev/kustomization.yaml`)
secara eksplisit **menghapus** `KeycloakRealmImport/payu-realm-import` dengan
alasan "realm lives in the shared keycloak DB, never re-import over live state
(L-377); fresh envs import once manually". Jadi di cluster baru **tidak ada**
realm `payu` sampai di-import manual.

### Kenapa sub HARUS dipin (bukan acak)

`V113__seed_dev_journey_users.sql` meng-key baris `users` pada
`external_id` = **Keycloak `sub`**, dengan `ON CONFLICT (external_id) DO NOTHING`.
Kalau Keycloak minted UUID acak (misal import `payu-realm.json` yang `id: null`,
atau create user via Admin REST tanpa set `id`), maka:

- baris `users` ter-seed dengan `external_id` yang **tidak pernah** muncul di token;
- `ON CONFLICT DO NOTHING` diam-diam tidak memperbaiki apa pun;
- setiap endpoint yang digate ownership -> **403**;
- `GET /api/v1/wallets/{SENDER}/balance` tak bisa memetakan wallet pemanggil;
- money journey gagal di **selisih saldo**, bukan di login (login tetap hijau).

Jadi sub terpaket di file import itu wajib, bukan opsional:

| user | Keycloak `sub` (WAJIB dipin) | `accountId` attribute |
| :--- | :--- | :--- |
| customer1 | `8d177115-af87-47d5-8b94-a09e7cfa55bc` | `750e8400-e29b-41d4-a716-446655440001` |
| customer2 | `a4c42cbd-9549-4467-9916-837713941c48` | `750e8400-e29b-41d4-a716-446655440004` |
| admin | `fc1eafbb-ea56-4bde-8d16-7253297f4756` | `750e8400-e29b-41d4-a716-446655440005` |
| backoffice | (tak dipin di git, UUID acak OK) | (tak ada) |

Password keempatnya: `P@ssw0rd123`.

### Perintah (terverifikasi live 2026-10-20)

TXT = host SSO yang dipakai cluster ini. Cluster dev `payu-drrpz` memakai
**domain cluster** `apps.payu.ocp.fajjjar.my.id` (shared domain
`apps.fajjjar.my.id` tak bisa dipakai: delegasi NS-nya rusak, lihat
`docs/roadmap/TODOS.md` `SHARED-DNS-001`).

```bash
# 0) host + admin kredensial dari SECRET cluster (bukan dari README/script)
export HOST="payu-keycloak-service.payu-sso.svc.cluster.local:8080"   # in-cluster
export EXT_HOST="sso-dev.apps.payu.ocp.fajjjar.my.id"                # browser-facing
export ADMIN_U=$(oc get secret payu-keycloak-admin -n payu-sso -o jsonpath='{.data.username}' | base64 -d)
export ADMIN_PW=$(oc get secret payu-keycloak-admin -n payu-sso -o jsonpath='{.data.password}' | base64 -d)
# live: admin / admin-password-2026!

# 1) token master realm
ADMIN_TOKEN=$(curl -skS -X POST "https://${EXT_HOST}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" -d "grant_type=password" \
  -d "username=${ADMIN_U}" -d "password=${ADMIN_PW}" \
  | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))")

# 2)realm payu harus ada; kalau belum, import dari file repo
#    (tools/… -> infrastructure/platform/identity/keycloak/keycloak-realm-import.yaml)
#    via POST /admin/realms dengan spec.realm sebagai body, SET 'id' user ke UUID di atas.
oc get keycloak payu-keycloak -n payu-sso -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}{"\n"}'   # True

# 3) unmanagedAttributePolicy -> ENABLED  (field-nya di USER PROFILE, bukan realm)
curl -skS -X PUT "https://${EXT_HOST}/admin/realms/payu/users/profile" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" -H "Content-Type: application/json" \
  -d '{"unmanagedAttributePolicy":"ENABLED"}'

# 4) create user dengan id terpaket + atribut accountId (id WAJIB diisi).
#    RHBK 26.6 MENGABAIKAN field "id" pada POST /users (201 tapi sub acak!).
#    Pin sub via partialImport (terbukti SIT 2026-10-22, L-491).
curl -skS -X POST "https://${EXT_HOST}/admin/realms/payu/partialImport" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" -H "Content-Type: application/json" \
  -d '{"ifResourceExists":"OVERWRITE","users":[
    {"id":"8d177115-af87-47d5-8b94-a09e7cfa55bc","username":"customer1",
     "email":"customer1@payu.fajjjar.my.id","emailVerified":true,
     "firstName":"Customer","lastName":"One","enabled":true,
     "attributes":{"phoneNumber":["+6281234567890"],"nik":["3201234567890001"],
                   "externalId":["EXT-CUST-001"],
                   "accountId":["750e8400-e29b-41d4-a716-446655440001"]},
     "credentials":[{"type":"password","value":"P@ssw0rd123","temporary":false}],
     "realmRoles":["USER","KYC_VERIFIED","PREMIUM"]}]}'
# {"overwritten":1,...,"id":"8d177115-..."}. Ulangi untuk customer2 / admin
# (id & atribut spt tabel di atas). Verifikasi: GET /users/<id> -> username cocok.

# 5) mapper account_id di client payu-web-app DAN payu-backend
#    (user attribute "accountId" -> claim "account_id"; tanpa ini claim tak mengalir ke BFF)
```

### Verifikasi (semua harus true)

```bash
# sub + accountId + role tiap user
curl -skS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "https://${EXT_HOST}/admin/realms/payu/users" | python3 -m json.tool

# profile policy
curl -skS -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "https://${EXT_HOST}/admin/realms/payu/users/profile" | python3 -c "import json,sys; print(json.load(sys.stdin).get('unmanagedAttributePolicy'))"
# ENABLED

# token customer1: sub + account_id + iss
SECRET=$(oc get secret payu-keycloak-client-secrets -n payu-dev -o jsonpath='{.data.payu-backend-client-secret}' | base64 -d)
TOKEN=$(curl -skS -X POST "https://${EXT_HOST}/realms/payu/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" -d "client_id=payu-backend" -d "client_secret=${SECRET}" \
  -d "username=customer1" -d "password=P@ssw0rd123" \
  | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))")
echo "$TOKEN" | cut -d. -f2 | tr '_-' '/+' | base64 -d 2>/dev/null | python3 -c "
import json,sys
d=json.load(sys.stdin)
print('sub       =', d.get('sub'))       # 8d177115-af87-47d5-8b94-a09e7cfa55bc
print('account_id=', d.get('account_id'))  # 750e8400-e29b-41d4-a716-446655440001
print('iss       =', d.get('iss'))        # https://sso-dev.apps.payu.ocp.fajjjar.my.id/realms/payu
"
```

Verified live 2026-10-20: `sub=8d177115-af87-47d5-8b94-a09e7cfa55bc`,
`account_id=750e8400-e29b-41d4-a716-446655440001`,
`iss=https://sso-dev.apps.payu.ocp.fajjjar.my.id/realms/payu`.

## 5. Client secret realm diselaraskan dengan ESO (OTOMATIS via PostSync Job)

Dev memakai ESO `Password` generator (`infrastructure/workloads/overlays/payu-dev/keycloak-client-secret-generator.yaml`)
yang menghasilkan nilai **acak** untuk `payu-keycloak-client-secrets`. Repo pin
statis di `keycloak-realm-import.yaml` (`payu-backend-secret-dev-2026!`) **DITOLAK**
realm di cluster baru:

```
POST .../realms/payu/protocol/openid-connect/token  client_secret=payu-backend-secret-dev-2026!
{"error":"unauthorized_client","error_description":"Invalid client or Invalid client credentials"}
```

`keycloak-client-secret-sync` (Job PostSync di `identity/overlays/common`, E2E-CLIENTSECRET-001
CLOSED 2026-10-22) membaca `payu-keycloak-client-secrets` ter-mirror di `payu-sso` dan
menulisnya ke kedua client realm via GET-modify-PUT (L-482: `POST client-secret` me-regenerate
acak — jangan dipakai). Job idempoten + fail-closed (check `client_credentials` akhir).
Perintah manual di bawah hanya untuk darurat (Job gagal / cluster tanpa Argo):

```bash
# nilai yang DIPAKAI service (auth-service, partner-service) = nilai ESO live
export BACKEND_SECRET=$(oc get secret payu-keycloak-client-secrets -n payu-dev -o jsonpath='{.data.payu-backend-client-secret}' | base64 -d)
export WEB_SECRET=$(oc get secret payu-keycloak-client-secrets -n payu-dev -o jsonpath='{.data.payu-web-client-secret}' | base64 -d)

# ambil client payu-backend di realm, lalu set secret = nilai di atas
CID=$(curl -skS -H "Authorization: Bearer ${ADMIN_TOKEN}" "https://${EXT_HOST}/admin/realms/payu/clients" \
  | python3 -c "
import json,sys
for c in json.load(sys.stdin):
    if c['clientId']=='payu-backend': print(c['id'])")
curl -skS -X GET -H "Authorization: Bearer ${ADMIN_TOKEN}" "https://${EXT_HOST}/admin/realms/payu/clients/${CID}" \
  | python3 -c "
import json,sys,os
c=json.load(sys.stdin); c['secret']=os.environ['BACKEND_SECRET']; print(json.dumps(c))" \
  | curl -skS -X PUT -H "Authorization: Bearer ${ADMIN_TOKEN}" -H "Content-Type: application/json" \
    -d @- "https://${EXT_HOST}/admin/realms/payu/clients/${CID}" -o /dev/null -w '%{http_code}\n'   # 204
```

Untuk `payu-web-app`, `auth-service` kini membaca `PAYU_KEYCLOAK_WEB_CLIENT_SECRET` dari
secretRef ESO (bukan hardcoded) — realm `payu-web-app` = nilai ESO `payu-web-client-secret`.
Verified live 2026-10-22: Job `Complete`, kedua client token 200:

```bash
# payu-backend client_credentials (nilai ESO live)
curl -skS -X POST "https://${EXT_HOST}/realms/payu/protocol/openid-connect/token" \
  -d grant_type=client_credentials -d client_id=payu-backend --data-urlencode "client_secret=${BACKEND_SECRET}"
# {"access_token":"...","expires_in":300}
```

## 6. Verifikasi seed end-to-end (semua harus cocok)

```bash
# (a) wallets: 5 row ACTIVE
oc exec -n payu-dev payu-database-1 -c postgres -- \
  env PGPASSWORD="$(oc get secret payu-database-app -n payu-dev -o jsonpath='{.data.password}' | base64 -d)" \
  psql -h localhost -U payu -d payu_wallet \
  -c "SET app.tenant_id='default'; SELECT account_id, balance, status FROM wallets WHERE account_id LIKE '750e%' ORDER BY 1;"

# (b) users + accounts: external_id == sub Keycloak, id == accountId
oc exec -n payu-dev payu-database-1 -c postgres -- \
  env PGPASSWORD="$(oc get secret payu-database-app -n payu-dev -o jsonpath='{.data.password}' | base64 -d)" \
  psql -h localhost -U payu -d payu_account \
  -c "SET app.tenant_id='default'; SELECT username, external_id, id FROM users ORDER BY 1;"

# (c) money journey: transfer internal harus COMPLETED (bukan FAILED)
GATEWAY_MODE=internal \
  ACCT=750e8400-e29b-41d4-a716-446655440001 \
  RECIPIENT_ACCT=1001001002 \
  bash scripts/e2e/transaction-history.sh
# T5 201 status=COMPLETED, T6 GET /api/v1/transactions/{id} 200, ALL TESTS PASSED
```

Verified live 2026-10-20 (setelah `GRPC-AUTH-001` diperbaiki): saldo sender
10.000.000 -> 9.998.000, penerima 5.000.000 -> 5.002.000, `ledger_entries`
DEBIT 1.000 + CREDIT 1.000 dengan `balance_after` nyambung.

## 7. Urutan bootstrap cluster baru (ringkas)

1. Namespace + operator (CSV `Succeeded` semua).
2. Data: `oc apply --server-side -k infrastructure/platform/data/overlays/dev`.
   Tunggu CNPG `ready=3/3` **sebelum** job bootstrap; kalau job `outbox-bootstrap`
   / `shedlock-bootstrap` / `post-deploy-db-grants` gagal karena DB belum siap,
   hapus lalu apply ulang.
3. Messaging: `oc apply --server-side -k infrastructure/platform/messaging/overlays/payu-dev`,
   lalu `oc apply --server-side -k infrastructure/platform/amq-broker/overlays/dev`
   (size 1 menang).
4. Identity: `oc apply --server-side -k infrastructure/platform/identity/overlays/dev`
   -> RHBK `Ready=True`; tunggu `keycloak-admin-password-sync` Complete.
5. **Realm import manual** (bagian 4) + **selaraskan client secret** (bagian 5).
6. Images build + push, lalu workload: root overlay **dahulu**, per-service overlay
   **terakhir** (root men-strip env yang cuma ada di per-service).
7. Verifikasi: bagian 6 + `./scripts/verify-overlay-drift.sh payu-dev`
   -> `OK: no overlay drift in payu-dev`.
