# Contributing to PayU

Selamat berkontribusi di PayU Digital Banking Platform! Untuk menjaga kualitas dan konsistensi codebase kami, mohon ikuti panduan berikut.

## 🛠️ Development Guidelines (SOP)

1. **Shared Starters**: Selalu gunakan `security-starter`, `resilience-starter`, dan `cache-starter` untuk fitur-fitur cross-cutting. Jangan implementasi manual di level service.
2. **DTO First**: Definisikan DTO/Request/Response di package `interfaces.dto` sebelum implementasi logic.
3. **Port-Adapter Interface**: Gunakan Hexagonal Architecture untuk core services. Semua external communication harus lewat Port interface di domain layer.
4. **Error Handling**: Gunakan `GlobalExceptionHandler` dan custom `BusinessException` dengan error codes yang unik (e.g., `ACC_001`).
5. **Idempotency**: Semua endpoint payment/transfer WAJIB support `X-Idempotency-Key` header.
6. **Annotation Processor**: Prioritaskan Lombok. Jika gagal kompilasi setelah 2 upaya, beralih ke implementasi manual.
7. **Doc Sync**: Setiap update signifikan WAJIB memperbarui `CHANGELOG.md`.

## 🌿 Git Workflow

- **Branch Naming**: 
  - `feature/PAYU-[Jiras-ID]-description`
  - `fix/PAYU-[Jira-ID]-description`
  - `refactor/description`
- **Pull Requests**:
  - Berikan ringkasan perubahan yang jelas.
  - Lampirkan bukti testing (screenshot atau output log).
  - Minimal 1 approval dari tim terkait.
- **Commit Messages**: Gunakan [Conventional Commits](https://www.conventionalcommits.org/).

## 🧪 Testing Standards

- **Unit Tests**: Minimum 80% coverage untuk logic baru.
- **Integration Tests**: Wajib untuk aliran data antar-service.
- **E2E Tests**: Pastikan tidak ada regresi pada aliran transaksi utama.

## 🔐 E2E Test Auth: Keycloak URL Selection (READY-072)

**Rule**: Untuk SEMUA E2E scripts (cards-crud.sh, verify-nik-cache.sh, web-app BFF, dll.) yang memerlukan JWT token, **WAJIB** menggunakan **INTERNAL Keycloak URL** untuk issuance, BUKAN public HTTPS route.

| Endpoint | URL | Use case |
|----------|-----|----------|
| ✅ INTERNAL (correct) | `http://payu-keycloak-service.payu-sso.svc.cluster.local:8080/realms/payu` | E2E scripts, service-to-service JWT |
| ❌ PUBLIC (wrong) | `https://payu-keycloak.apps.payu.ocp.fajjjar.my.id/realms/payu` | Browser login UI only |

```bash
# ✅ CORRECT — INTERNAL URL, issues JWT with iss=http://payu-keycloak-service.payu-sso.svc.cluster.local:8080/realms/payu
JWT=$(oc exec -n payu-dev gateway-service-... -- curl -s -X POST \
  "http://payu-keycloak-service.payu-sso.svc.cluster.local:8080/realms/payu/protocol/openid-connect/token" \
  -d "client_id=payu-backend" \
  -d "client_secret=payu-backend-d3v-0nly-a7c2f1e8b4d9063e5c8a2b7f1d4e9a3c" \
  -d "grant_type=password" -d "username=customer1" -d "password=customer1-test-pass" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['access_token'])")

# ❌ WRONG — public URL, JWT has different issuer claim → gateway rejects with 401 INVALID_TOKEN
```

**Why**: Gateway `QUARKUS_OIDC_TOKEN_ISSUER` env var is set to the INTERNAL Keycloak URL. JWTs from the public route have a different `iss` claim → token validation fails → 401.

**Background**: See `E2E-2026-06-13-10` in `docs/roadmap/TODOS.md` for the full investigation.

---

_Last Updated: June 13, 2026_
