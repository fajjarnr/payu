# API Standards & Guidelines

> **REST API naming, versioning, error codes, and response format standards for PayU Platform**

## 📋 Overview

This document defines the API standards that all PayU services must follow to ensure consistency, predictability, and ease of integration.

---

## 🌐 Base URL Structure

### Environment-Based URLs

| Environment | Base URL Pattern |
|-------------|-------------------|
| **Local** | `http://localhost:8080/api/v1` |
| **Development** | `https://api-dev.payu.id/api/v1` |
| **SIT** | `https://api-sit.payu.id/api/v1` |
| **UAT** | `https://api-uat.payu.id/api/v1` |
| **Production** | `https://api.payu.id/api/v1` |

### URL Pattern

```
https://{environment}.payu.id/api/v{version}/{service}/{resource}[/{id}][/{sub-resource}]
```

---

## 📐 URL Naming Conventions

### Resource Names

| Rule | Example |
|------|---------|
| **Plural nouns** | `/api/v1/accounts` (not `/account`) |
| **Kebab-case** | `/api/v1/transaction-histories` (not `/transactionHistories`) |
| **Noun-based** | `/api/v1/balances` (not `/getBalances`) |

### Hierarchical Resources

```
# Good - Clear hierarchy
/api/v1/accounts/{accountId}/pockets/{pocketId}
/api/v1/accounts/{accountId}/transactions

# Bad - Flat structure
/api/v1/pockets/{pocketId}
/api/v1/account-transactions/{accountId}
```

### Query Parameters

| Parameter | Format | Example |
|-----------|--------|---------|
| **Pagination** | `page`, `size` | `?page=0&size=20` |
| **Sorting** | `sort` | `?sort=createdAt:desc` |
| **Filtering** | Resource-specific | `?status=ACTIVE&type=SAVINGS` |
| **Search** | `search` | `?search=John+Doe` |

---

## 📤 HTTP Methods

### Method Usage Matrix

| Method | Usage | Idempotent | Safe |
|--------|-------|------------|------|
| **GET** | Retrieve resources | ✅ | ✅ |
| **POST** | Create resource | ❌ | ❌ |
| **PUT** | Full update | ✅ | ❌ |
| **PATCH** | Partial update | ❌ | ❌ |
| **DELETE** | Delete resource | ✅ | ❌ |

### Examples

```http
# GET - Retrieve collection
GET /api/v1/accounts?page=0&size=20

# GET - Retrieve single resource
GET /api/v1/accounts/{accountId}

# POST - Create resource
POST /api/v1/accounts
Content-Type: application/json

# PUT - Full update
PUT /api/v1/accounts/{accountId}
Content-Type: application/json

# PATCH - Partial update
PATCH /api/v1/accounts/{accountId}
Content-Type: application/json

# DELETE - Remove resource
DELETE /api/v1/accounts/{accountId}
```

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

### Request Body (Create/Update)

```json
{
  "accountType": "SAVINGS",
  "currency": "IDR",
  "initialDeposit": {
    "amount": 1000000,
    "currency": "IDR"
  },
  "customer": {
    "id": "cust-123",
    "type": "INDIVIDUAL"
  }
}
```

### Validation Rules

| Rule | Description |
|------|-------------|
| **Required fields** | Return 400 if missing |
| **Type validation** | Return 400 if wrong type |
| **Format validation** | Email, phone, NIK format checks |
| **Business rules** | Minimum balance, age requirements |

---

## 📥 Response Format

### Success Response

```json
{
  "data": {
    "id": "acc-123456",
    "accountNumber": "88901234567890",
    "accountType": "SAVINGS",
    "balance": {
      "amount": 1000000,
      "currency": "IDR"
    },
    "status": "ACTIVE",
    "createdAt": "2026-01-30T10:00:00Z",
    "updatedAt": "2026-01-30T10:00:00Z"
  },
  "meta": {
    "requestId": "req-abc123",
    "timestamp": "2026-01-30T10:00:00Z"
  }
}
```

### Collection Response

```json
{
  "data": [
    { "id": "acc-001", "accountNumber": "8890123456789001" },
    { "id": "acc-002", "accountNumber": "8890123456789002" }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "requestId": "req-abc123",
    "timestamp": "2026-01-30T10:00:00Z"
  }
}
```

### Error Response

```json
{
  "error": {
    "code": "ACC_001",
    "message": "Account not found",
    "details": "Account with ID 'acc-999' does not exist",
    "timestamp": "2026-01-30T10:00:00Z",
    "path": "/api/v1/accounts/acc-999",
    "requestId": "req-abc123"
  }
}
```

---

## 🔢 Error Codes

### HTTP Status Codes

| Status | Usage |
|--------|-------|
| **200 OK** | Successful GET, PUT, PATCH |
| **201 Created** | Successful POST |
| **204 No Content** | Successful DELETE |
| **400 Bad Request** | Validation error |
| **401 Unauthorized** | Missing/invalid token |
| **403 Forbidden** | Valid token, insufficient permissions |
| **404 Not Found** | Resource not found |
| **409 Conflict** | Resource conflict |
| **422 Unprocessable** | Business logic error |
| **429 Too Many Requests** | Rate limit exceeded |
| **500 Internal Server Error** | Unexpected server error |
| **503 Service Unavailable** | Service temporarily unavailable |

### Application Error Codes

| Prefix | Domain | Example |
|--------|--------|---------|
| **ACC** | Account | `ACC_001`: Account not found |
| **AUT** | Auth | `AUT_001`: Invalid credentials |
| **TXN** | Transaction | `TXN_001`: Insufficient balance |
| **WAL** | Wallet | `WAL_001**: Wallet frozen |
| **KYC** | KYC | `KYC_001`: NIK verification failed |
| **GEN** | Generic | `GEN_001`: Internal server error |

### Error Message Format

```json
{
  "error": {
    "code": "TXN_004",
    "message": "Insufficient balance for transfer",
    "details": "Account balance (IDR 500,000) is less than transfer amount (IDR 1,000,000) plus fee (IDR 5,000)",
    "field": "amount",
    "timestamp": "2026-01-30T10:00:00Z"
  }
}
```

---

## 🔄 API Versioning

### Versioning Strategy

PayU uses **URL-based versioning**:

```
/api/v1/accounts    # Current version
/api/v2/accounts    # Future version (breaking changes)
```

### Versioning Rules

| Change Type | Action |
|-------------|--------|
| **Non-breaking** | No version change (add fields, endpoints) |
| **Breaking** | Increment version (v1 → v2) |
| **Deprecation** | Mark deprecated, support for 6 months |

---

## 🔐 Authentication & Authorization

### OAuth2 Bearer Token

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

### Token Lifecycle

| Token Type | Lifetime | Purpose |
|-------------|----------|---------|
| **Access Token** | 1 hour | API requests |
| **Refresh Token** | 30 days | Get new access token |

### Permission Scopes

| Scope | Description |
|-------|-------------|
| `accounts:read` | Read account information |
| `accounts:write` | Create/update accounts |
| `transactions:read` | Read transactions |
| `transactions:write` | Initiate transactions |

---

## ⚡ Rate Limiting

### Rate Limit Rules

| Tier | Requests | Window |
|------|----------|--------|
| **Free** | 100/hour | Per IP |
| **Basic** | 1000/hour | Per user |
| **Premium** | 10000/hour | Per user |
| **Partner** | 100000/hour | Per API key |

### Rate Limit Headers

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 995
X-RateLimit-Reset: 1706622400
```

### Rate Limit Exceeded Response

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1706622400
Retry-After: 3600

{
  "error": {
    "code": "RATE_001",
    "message": "Rate limit exceeded",
    "details": "Maximum 1000 requests per hour allowed"
  }
}
```

---

## 🎭 Idempotency

### Idempotency Key Header

```http
POST /api/v1/transactions
X-Idempotency-Key: uuid-v4-unique-key
Content-Type: application/json
```

### Idempotency Behavior

| Scenario | First Request | Duplicate Request |
|----------|---------------|-------------------|
| **Processing** | Execute normally | Return cached result |
| **Completed** | Execute normally | Return original result |
| **Failed** | Execute normally | Return original error |

---

## 📋 OpenAPI Documentation

### OpenAPI Spec Location

Each service must expose its OpenAPI specification at:

```
/api/v3/api-docs    # JSON spec
/swagger-ui.html    # Swagger UI
```

### Required OpenAPI Fields

```yaml
openapi: 3.0.0
info:
  title: Account Service API
  version: 1.0.0
  description: PayU Account Service API
  contact:
    name: API Team
    email: api-team@payu.id
  license:
    name: Proprietary
servers:
  - url: https://api.payu.id/api/v1
    description: Production
```

---

## ✅ API Design Checklist

Before releasing a new API endpoint, verify:

- [ ] URL follows naming conventions
- [ ] HTTP method is semantically correct
- [ ] Request/response DTOs are documented
- [ ] Error codes are defined
- [ ] OpenAPI spec is updated
- [ ] Idempotency key support (for write operations)
- [ ] Rate limiting configured
- [ ] Authentication/authorization defined
- [ ] Unit tests for happy path
- [ ] Unit tests for error scenarios

---

_Last Updated: January 30, 2026_
