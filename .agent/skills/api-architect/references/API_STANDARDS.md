# API Design & Engineering Standards

## 📜 OpenAPI Documentation Standards
*   **100% Coverage**: ALL REST endpoints MUST have `@Operation` annotations with `summary`, `description`, and appropriate `ApiResponse` codes.
*   **Tags**: Group operations by business domain (e.g., `Transactions`, `Accounts`, `Identity`).
*   **Idempotency Reporting**: Clearly document which endpoints support/require the `X-Idempotency-Key` header.
*   **Validation Script**: Use `./scripts/validation/validate-openapi.py` to audit documentation coverage. CI/CD pipelines should enforce a minimum coverage threshold (default 90%+).

## 🔄 REST Best Practices
*   **Status Codes**:
    *   `201 Created`: For successful POST (resource creation).
    *   `204 No Content`: For successful DELETE or PUT (no response body).
    *   `409 Conflict`: For domain errors like "Insufficient Funds" or "Duplicate Idempotency Key".
    *   `422 Unprocessable Entity`: For business validation failures.
*   **Versioning**: Standardize on URI-based versioning (e.g., `/api/v1/...`).
*   **DTO Design**: Use generic wrappers for standardizing error responses (e.g., `ErrorResponse`). Avoid returning raw entities.

## 🔗 Webhook Delivery Patterns (L-009)

**HMAC-SHA256 Signing**: All webhook payloads MUST be signed. Include the signature in the `X-Webhook-Signature` header so partners can verify authenticity.

**Retry Strategy**:
*   **Exponential backoff** with jitter: 3 attempts (e.g., 10s, 30s, 90s)
*   Store every delivery attempt (timestamp, status code, response body) in the `webhook_delivery_log` table for audit
*   Mark webhook as `FAILED` after exhausting retries; support manual re-trigger from backoffice

**Idempotent Webhook Consumers**: Include a unique `eventId` in every webhook payload so partner systems can deduplicate on their side.

**Mobile Deeplink Security**:
*   Signed URLs with expiry — never trust client-side parameters
*   Support universal links (iOS) and app links (Android) as fallback

## 🔑 Idempotency Architecture — `@Idempotent` Annotation (L-018)

**Architecture (5 layers)**:
1.  `@Idempotent(required = true)` annotation on mutation endpoints — returns 400 if `X-Idempotency-Key` header missing
2.  `IdempotencyInterceptor` (`HandlerInterceptor`) — auto-registered via `IdempotencyAutoConfiguration`
3.  `IdempotencyService` — SHA-256 fingerprints request body, detects key reuse with different payloads (`409 IDEMPOTENCY_KEY_REUSE`)
4.  `RedisIdempotencyRepository` — atomic Lua script (`SETEX if not EXISTS`) for concurrent duplicate detection
5.  State machine: `IN_PROGRESS` → `COMPLETED` / `FAILED` with 24-hour TTL

**Key Design Decisions**:
*   Fingerprint check catches accidental key reuse (different requests with same key), not just exact duplicates
*   Key must be UUID v4 — reject non-UUID keys with 400
*   **Known Gap**: `ContentCachingResponseWrapper` placeholder — successful responses are NOT cached yet. Duplicate POSTs get processed twice on success path. Fix by replacing with Spring's actual `ContentCachingResponseWrapper`.

**Rule**: Use `@Idempotent(required = true)` on ALL payment/transfer/mutation POST endpoints. Document the header requirement in OpenAPI with `@Parameter(in = ParameterIn.HEADER, name = "X-Idempotency-Key", required = true)`.

## 🛡️ API Security & Gateway
*   **Rate Limiting**: Apply `@RateLimit` annotations to public endpoints (login, register). 
*   **Standard Port (8080)**: All backend services MUST expose port 8080. Gateway routes MUST point to port 8080 of the target service for internal discovery.
*   **CORS**: Configure CORS at the Gateway level, not individual services. Allow only trusted origins (`https://*.payu.fajjjar.my.id`).
*   **Auth Propagation**: Ensure the Gateway propagates the `Authorization` bearer token to downstream services.
