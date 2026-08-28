# API Standards & Guidelines

> **REST API naming, versioning, error codes, and response format standards for PayU Platform**

## 📋 Overview

This document defines the API standards that all PayU services must follow to ensure consistency, predictability, and ease of integration.

---

## 🌐 Base URL Structure

### Environment-Based URLs

| Environment     | Base URL Pattern                            |
| --------------- | ------------------------------------------- |
| **Local**       | `http://localhost:8080/api/v1`              |
| **Development** | `https://api.dev.payu.fajjjar.my.id/api/v1` |
| **SIT**         | `https://api.sit.payu.fajjjar.my.id/api/v1` |
| **UAT**         | `https://api.uat.payu.fajjjar.my.id/api/v1` |
| **Production**  | `https://api.payu.fajjjar.my.id/api/v1`     |

### URL Pattern

```
https://{environment}.payu.fajjjar.my.id/api/v{version}/{service}/{resource}[/{id}][/{sub-resource}]
```

---

## 📐 URL Naming Conventions

### Resource Names

| Rule             | Example                                                       |
| ---------------- | ------------------------------------------------------------- |
| **Plural nouns** | `/api/v1/accounts` (not `/account`)                           |
| **Kebab-case**   | `/api/v1/transaction-histories` (not `/transactionHistories`) |
| **Noun-based**   | `/api/v1/balances` (not `/getBalances`)                       |

---

## 📤 HTTP Methods

### Method Usage Matrix

| Method     | Usage              | Idempotent | Safe |
| ---------- | ------------------ | ---------- | ---- |
| **GET**    | Retrieve resources | ✅         | ✅   |
| **POST**   | Create resource    | ❌         | ❌   |
| **PUT**    | Full update        | ✅         | ❌   |
| **PATCH**  | Partial update     | ❌         | ❌   |
| **DELETE** | Delete resource    | ✅         | ❌   |

---

## 📦 Request Format

### Headers

```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer {access_token}
X-Idempotency-Key: {unique_request_id}
X-Request-ID: {correlation_id}
X-Device-ID: {device_identifier}
```

---

## 📥 Response Format

### Success Response

```json
{
  "data": {
    "id": "acc-123456",
    "accountNumber": "88901234567890",
    "status": "ACTIVE"
  },
  "meta": {
    "requestId": "req-abc123",
    "timestamp": "2026-01-30T10:00:00Z"
  }
}
```

---

## 🔢 Error Codes

### HTTP Status Codes

| Status                 | Usage                      |
| ---------------------- | -------------------------- |
| **200 OK**             | Successful GET, PUT, PATCH |
| **201 Created**        | Successful POST            |
| **400 Bad Request**    | Validation error           |
| **401 Unauthorized**   | Missing/invalid token      |
| **403 Forbidden**      | Insufficient permissions   |
| **404 Not Found**      | Resource not found         |
| **422 Unprocessable**  | Business logic error       |
| **500 Internal Error** | Unexpected server error    |

### Application Error Codes prefixes:

`ACC` (Account), `AUT` (Auth), `TXN` (Transaction), `WAL` (Wallet), `KYC` (KYC), `GEN` (Generic).

---

## 🔄 API Versioning

PayU uses **URL-based versioning**: `/api/v1/accounts`. Breaking changes require incrementing to `v2`.

---

## 🔐 Authentication & Authorization

Gunakan **OAuth2 Bearer Token** via `Authorization` header.

- **Access Token**: 1 Hour lifetime.
- **Refresh Token**: 30 Days lifetime.

---

## 🎭 Idempotency

Operasi mutating (POST/PUT/PATCH) wajib mendukung header `X-Idempotency-Key` (UUID v4) untuk mencegah eksekusi ganda pada request yang sama.

---

## ✅ API Validation (Spectral)

PayU menggunakan **Spectral** sebagai OpenAPI linter untuk memastikan spek API mematuhi standar di atas secara otomatis.
Generated spec: gateway `http://localhost:8080/q/openapi` (Quarkus SmallRye) + JVM `http://localhost:8001/v3/api-docs` (Springdoc) — agregasi via `api-portal-service` allowlist; `docs/openapi/` adalah generated, jangan commit manual.

### Installation

```bash
# Install via npm
npm install -g @stoplight/spectral-cli

# Install via script (actual path)
./scripts/validation/validate-api.sh --install
```

### Quick Start

```bash
# Validasi file tunggal (Context7 + Stripe/Adyen X- prefix verified)
./scripts/validation/validate-api.sh docs/openapi/account-api.yaml

# Validasi semua spek di project
./scripts/validation/validate-api.sh
```

### Aturan yang Diverifikasi (Spectral Ruleset)

1. **Response Envelope**: Semua response sukses harus menggunakan field `data` dan `meta`.
2. **Idempotency**: Semua POST/PUT/PATCH harus mendefinisikan header `X-Idempotency-Key` (PayU `X-` prefix, Stripe `Idempotency-Key` / Adyen `idempotency-key` max 64 UUID per Context7, PayU konsisten `X-Idempotency-Key` di gateway/Java/Python/CORS/mobile).
3. **Naming**: Path harus kebab-case (e.g., `/user-accounts`) dan memiliki prefix versi (`/v1/`).
4. **Documentation**: Tiap operasi wajib memiliki `summary`, `description`, dan `operationId` (camelCase).
5. **Pagination**: Endpoint list wajib mendukung parameter `page` dan `size`.


---

## 📋 API Design Checklist

Sebelum merilis API baru, pastikan:

- [ ] URL menggunakan plural nouns dan kebab-case.
- [ ] HTTP method sudah benar secara semantik.
- [ ] Response mengikuti format standar PayU (`data` & `meta`).
- [ ] Support `X-Idempotency-Key` (untuk write operations).
- [ ] Error codes mengikuti prefix domain yang sesuai.
- [ ] **Lulus validasi Spectral** (`./scripts/validation/validate-api.sh`).
- [ ] OpenAPI spec (JSON/YAML) sudah diupdate.
- [ ] Unit tests mencakup happy path & error scenarios.
---

_Last Updated: February 24, 2026 — patched 2026-08-28 `scripts/validation/validate-api.sh` `X-Idempotency-Key` `q/openapi+v3/api-docs` via Context7 Stripe/Adyen X- prefix max64_

