# OpenAPI Generated Specs

> Generated via gateway `GET /q/openapi` (Quarkus SmallRye) — do not commit manual edits.
> Source: `http://localhost:8080/q/openapi` (see `docs/api/API_STANDARDS.md:137`)
> Aggregasi via `api-portal-service` allowlist, `docs/openapi/` adalah generated.

## Generation

```bash
mkdir -p docs/openapi
curl -s http://localhost:8080/q/openapi -o docs/openapi/gateway.json
# optional: via BFF portal when gateway is behind auth
curl -s http://localhost:3001/api/v1/portal/services/account-service/openapi -o docs/openapi/account.json
```

## Validation

Spectral `6.14.2` ruleset `.spectral.yaml` requires Node 18/20 (Node 24 shows `Function is not defined`).
Use Node 20 for validation:

```bash
nvm use 20
./scripts/validation/validate-api.sh docs/openapi/gateway.json
# expected: OAS 3.1.0 valid, warnings for missing X-Idempotency-Key on GET only
```

BFF already forwards `X-Idempotency-Key` for all POST/PUT/PATCH (`route.ts:292` whitelist).
Generated: `gateway.json` 29KB `openapi:3.1.0` `paths /api/v1/admin/rate-plans` etc.
