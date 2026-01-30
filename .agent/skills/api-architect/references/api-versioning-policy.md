# API Versioning & Deprecation Policy

## Overview

Kebijakan versioning dan deprecation untuk semua PayU REST APIs.

---

## 🔢 Versioning Strategy

### URL Path Versioning (Primary)

```
https://api.payu.id/v1/accounts
https://api.payu.id/v2/accounts
```

### Version Header (Secondary)

```http
GET /accounts HTTP/1.1
Host: api.payu.id
Accept: application/json
X-API-Version: 2024-01-15
```

---

## 📋 Version Lifecycle

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   ALPHA     │───▶│    BETA     │───▶│   STABLE    │───▶│ DEPRECATED  │
│ (Internal)  │    │ (Partners)  │    │ (Public)    │    │ (Sunset)    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
     │                   │                  │                  │
     ▼                   ▼                  ▼                  ▼
  No SLA            Best Effort       99.9% SLA          6 months
                                                         to sunset
```

| Stage | Stability | Breaking Changes | Support |
|-------|-----------|------------------|---------|
| **Alpha** | Experimental | Anytime | None |
| **Beta** | Testing | With 2-week notice | Best effort |
| **Stable** | Production | Major version only | Full SLA |
| **Deprecated** | Legacy | None | 6 months |

---

## ⚠️ Deprecation Process

### Timeline

| Phase | Duration | Action |
|-------|----------|--------|
| **Announcement** | T-0 | Deprecation notice in changelog, docs, headers |
| **Warning Period** | 3 months | `Deprecation` header added to responses |
| **Migration Support** | 3 months | Migration guide published, support available |
| **Sunset** | T+6 months | API version removed |

### Deprecation Header

```http
HTTP/1.1 200 OK
Deprecation: Sun, 30 Jun 2026 23:59:59 GMT
Sunset: Mon, 30 Dec 2026 23:59:59 GMT
Link: <https://api.payu.id/v2/accounts>; rel="successor-version"
X-Deprecation-Notice: "v1 accounts API deprecated. Migrate to v2 by 2026-12-30"
```

### Response Body Warning

```json
{
  "data": { ... },
  "meta": {
    "deprecation": {
      "message": "This API version is deprecated",
      "sunset": "2026-12-30T23:59:59Z",
      "migration_guide": "https://docs.payu.id/migration/accounts-v1-to-v2"
    }
  }
}
```

---

## 🔄 Breaking vs Non-Breaking Changes

### ✅ Non-Breaking (Allowed in minor versions)

- Adding new optional fields to response
- Adding new optional query parameters
- Adding new endpoints
- Adding new enum values (with proper handling)
- Relaxing validation (e.g., accepting longer strings)

### ❌ Breaking (Requires major version)

- Removing fields from response
- Changing field types
- Changing field names
- Making optional fields required
- Removing endpoints
- Changing URL structure
- Changing authentication method
- Changing error response format

---

## 📝 API Changelog Format

```markdown
# API Changelog

## v2.3.0 (2026-01-30) - STABLE

### Added
- `GET /v2/accounts/{id}/pockets` - List sub-accounts
- `transactionCategory` field in transfer response

### Deprecated
- `GET /v1/accounts/{id}/balance` - Use v2 endpoint instead (Sunset: 2026-06-30)

## v2.2.0 (2026-01-15) - STABLE

### Changed
- Increased `description` max length from 100 to 500 characters

### Fixed
- Pagination cursor encoding issue
```

---

## 🛡️ Compatibility Guarantees

### Stable Version Guarantees

1. **12-month minimum support** after deprecation announcement
2. **No breaking changes** within major version
3. **Backward compatible** minor version updates
4. **Migration guide** for every major version upgrade
5. **Parallel operation** of old and new versions during transition

### Partner SLA

| Version Age | Support Level |
|-------------|---------------|
| Current (N) | Full support, all features |
| Previous (N-1) | Security fixes, critical bugs |
| Legacy (N-2) | Security fixes only |
| Sunset (N-3+) | No support, scheduled removal |

---

## 🔧 Implementation

### Version Router (Spring Boot)

```java
@Configuration
public class ApiVersionConfig {
    
    @Bean
    public RouterFunction<ServerResponse> versionedRoutes(
            AccountControllerV1 v1,
            AccountControllerV2 v2) {
        return route()
            .path("/v1/accounts", builder -> builder
                .GET("/{id}", v1::getAccount)
                .filter(deprecationFilter("2026-06-30")))
            .path("/v2/accounts", builder -> builder
                .GET("/{id}", v2::getAccount))
            .build();
    }
    
    private HandlerFilterFunction<ServerResponse, ServerResponse> deprecationFilter(String sunset) {
        return (request, next) -> next.handle(request)
            .map(response -> ServerResponse.from(response)
                .header("Deprecation", "true")
                .header("Sunset", sunset)
                .build());
    }
}
```

### OpenAPI Specification

```yaml
paths:
  /v1/accounts/{id}:
    get:
      deprecated: true
      x-sunset: "2026-06-30"
      x-successor: "/v2/accounts/{id}"
      summary: "[DEPRECATED] Get account by ID"
      description: |
        ⚠️ **Deprecated**: This endpoint will be removed on 2026-06-30.
        Please migrate to `/v2/accounts/{id}`.
        
        See [Migration Guide](https://docs.payu.id/migration/accounts-v1-to-v2)
```

---

## 📊 Monitoring Deprecated Endpoints

```promql
# Alert when deprecated endpoint usage > threshold
sum(rate(http_requests_total{path=~"/v1/.*"}[1h])) > 100
```

### Dashboard Metrics

- Requests per deprecated endpoint
- Unique clients using deprecated endpoints
- Time until sunset
- Migration progress percentage
