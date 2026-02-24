# API Design & Engineering Standards

## 📜 OpenAPI Documentation Standards
*   **100% Coverage**: ALL REST endpoints MUST have `@Operation` annotations with `summary`, `description`, and appropriate `ApiResponse` codes.
*   **Tags**: Group operations by business domain (e.g., `Transactions`, `Accounts`, `Identity`).
*   **Idempotency Reporting**: Clearly document which endpoints support/require the `X-Idempotency-Key` header.
*   **Validation Script**: Use `./scripts/validate-openapi.py` to audit documentation coverage. CI/CD pipelines should enforce a minimum coverage threshold (default 90%+).

## 🔄 REST Best Practices
*   **Status Codes**:
    *   `201 Created`: For successful POST (resource creation).
    *   `204 No Content`: For successful DELETE or PUT (no response body).
    *   `409 Conflict`: For domain errors like "Insufficient Funds" or "Duplicate Idempotency Key".
    *   `422 Unprocessable Entity`: For business validation failures.
*   **Versioning**: Standardize on URI-based versioning (e.g., `/api/v1/...`).
*   **DTO Design**: Use generic wrappers for standardizing error responses (e.g., `ErrorResponse`). Avoid returning raw entities.

## 🛡️ API Security & Gateway
*   **Rate Limiting**: Apply `@RateLimit` annotations to public endpoints (login, register). 
*   **Standard Port (8080)**: All backend services MUST expose port 8080. Gateway routes MUST point to port 8080 of the target service for internal discovery.
*   **CORS**: Configure CORS at the Gateway level, not individual services. Allow only trusted origins (`https://*.payu.id`).
*   **Auth Propagation**: Ensure the Gateway propagates the `Authorization` bearer token to downstream services.
