---
name: api-architect
description: PayU API architecture for versioned REST and OpenAPI contracts, Spring Boot/Quarkus services, FastAPI/Pydantic APIs, BFF and gateway boundaries, financial idempotency, RFC 9457 errors, OAuth2/HMAC webhooks, and resilient partner integrations with Context7-first library verification.
---

# API Architect — PayU

Use this skill when designing, implementing, reviewing, versioning, testing, or integrating PayU APIs. An API is a published contract across browsers, mobile clients, partners, services, retries, and audit systems—not just a controller method.

## Operating contract

1. Read `AGENTS.md`, `docs/api/API_STANDARDS.md`, relevant security/integration guidance, and the owning service's existing routes before changing a contract.
2. Inspect the service POM/`requirements.txt`/`pyproject.toml`, generated OpenAPI endpoint, shared starter, and contract tests before choosing an API.
3. Resolve the official library with Context7 and query the exact installed version before using a framework, client, validator, auth SDK, retry API, or OpenAPI tool. If unavailable, use the nearest documented version, record the mismatch, and avoid undocumented behavior.
4. Design the request, response, error, auth, idempotency, timeout, and compatibility behavior before writing implementation. For a new feature, present the plan before coding.
5. Prefer the existing shared starter and gateway/BFF boundary. Do not add a second envelope, auth flow, HTTP client, retry layer, or versioning scheme for one endpoint.
6. Verify with generated OpenAPI, contract tests, focused integration tests, security tests, and evidence from the actual command. Do not claim compatibility from a type-check alone.

## Repository baseline

Verify these values before work; manifests and deployed configuration win.

| Surface | Observed baseline |
|---|---|
| JVM services | Spring Boot 4.1.0 / Java 25, Spring MVC or Quarkus depending on service |
| Python services | FastAPI 0.115.0, Pydantic 2.9.0, SQLAlchemy 2.0.35, asyncpg 0.29.0 |
| Public path | `/api/v1/...`, normally reached through gateway or the web BFF |
| JVM integration | Shared `rest-client-starter`, `resilience-starter`, OAuth2 resource-server, Springdoc/OpenAPI |
| Contract tests | Spring Cloud Contract under `tests/contract` plus service integration tests |
| Domain events | Shared outbox starter and CloudEvents contract; not a direct controller-to-Kafka shortcut |

## Resource and HTTP design

- Use versioned, plural, kebab-case resource paths such as `/api/v1/accounts` and `/api/v1/transaction-histories`.
- Model resources and state transitions explicitly. Prefer `POST /transfers` with a command body over arbitrary action verbs; use a subresource when the action is a durable domain operation such as `/transfers/{id}/reversals`.
- Keep path parameters identifiers, not unbounded filters. Put bounded filters and pagination in query parameters and validate every limit, sort field, and cursor.
- Use `GET` for safe reads, `POST` for creation/commands, `PUT` for complete replacement, `PATCH` only with documented merge semantics, and `DELETE` only where deletion is legal. Financial records are reversed/closed, not deleted.
- Return `201 Created` with `Location` for a newly created resource when the resource exists synchronously; use `202 Accepted` only when processing is genuinely asynchronous and expose a status/reconciliation path.
- Return `204` only when there is intentionally no representation. Otherwise return the canonical updated resource and its version/state.
- Use `409 Conflict` for duplicate/idempotency/state conflicts and `422 Unprocessable Content` for a syntactically valid request rejected by a business rule, following the existing service contract.
- Add `Deprecation`/`Sunset` metadata and migration documentation before removing or changing a public field/path. Additive fields are not automatically safe for every strict client.

## PayU financial contract

### Money

- Use `BigDecimal`/`Decimal` server-side and preserve four fractional digits: database `DECIMAL(19,4)`, `HALF_EVEN` rounding.
- Never use `float`, `double`, JavaScript `number`, or a binary floating-point JSON example for `amount`, `balance`, `fee`, limit, or exchange-rate business values.
- Choose and document one precise wire representation for money. Prefer decimal text when clients may lose precision; if an existing contract emits JSON numbers, prove its precision behavior and do not silently change it.
- Validate amount syntax, scale, currency, limits, and account ownership at the service boundary. The client cannot authorize or calculate the final balance.
- Include currency with every monetary value where ambiguity is possible. Never infer currency from locale or a display string.

### Idempotency

- Require `X-Idempotency-Key` on payment, transfer, top-up, refund, disbursement, and other financial mutation endpoints. For other side-effecting writes, require it whenever replay could duplicate an external effect.
- Document the header as required in OpenAPI and reject missing/invalid keys before business processing.
- Bind a key to an authenticated principal, route/operation, and canonical request fingerprint. Same key + same request returns the stored outcome; same key + different request is a conflict.
- Make the reservation atomic (`IN_PROGRESS` → `COMPLETED`/`FAILED`) and define TTL, response replay, crash recovery, and ambiguous timeout behavior. A short cache entry alone is not a durable financial record.
- Retries must preserve the same key. Never generate a new key for an automatic retry of the same user intent.
- Keep the exact header name consistent across gateway, Java, Python, OpenAPI, CORS, mobile, and contract tests. Treat existing `Idempotency-Key` variants as compatibility gaps, not a reason to silently publish two contracts.

## Response and error contracts

- Preserve an existing service's success envelope for backward compatibility. Current services commonly use `success`, `data`, `error`, and `meta`; do not mix camelCase and snake_case or invent a second envelope in the same API.
- New errors use RFC 9457 Problem Details with `Content-Type: application/problem+json`, stable `type`, `title`, `status`, `detail`, and `instance` fields plus PayU extensions such as a unique `code`, request/trace ID, and field violations where appropriate.
- Keep `detail` safe for the caller: no stack traces, SQL, secrets, tokens, internal hostnames, or unmasked PII. Log the diagnostic context server-side with the request/correlation ID.
- Map errors consistently: validation, authentication, authorization, rate limit, conflict/idempotency, not-found, upstream failure, and unknown failure each need documented status and code behavior.
- Do not expose a success response when a financial operation is unknown. Return `PENDING`/`UNKNOWN` with a status endpoint or reconciliation reference when the upstream outcome is ambiguous.

## OpenAPI and contract-first delivery

- Define schemas, security requirements, examples, error responses, pagination, idempotency, and deprecation behavior before implementation.
- Use the OpenAPI version supported by the installed Springdoc/MicroProfile/FastAPI generator. Resolve the generator through Context7; do not assume every service produces identical OpenAPI.
- Annotate operations with summary, description, tags, request/response schemas, auth requirements, and all meaningful status codes. Keep generated docs aligned with runtime behavior.
- Mark sensitive fields and write-only/read-only properties explicitly. Do not publish tokens, PINs, CVV, internal IDs, or debug fields merely because a DTO contains them.
- For financial schemas, encode decimal precision and currency constraints in the contract and include string-preserving examples. Contract tests must not use unconstrained `anyNumber()` for money.
- Use the repository's OpenAPI aggregation/portal only with configured service allowlists and bounded timeouts. Never fetch an arbitrary URL from a request parameter; protect against SSRF and oversized specs.
- Use Spring Cloud Contract under `tests/contract` for provider/consumer behavior. Regenerate and run provider tests when changing status, headers, field names, or envelope shape.

## Authentication and authorization

- Use the existing Keycloak/OIDC resource-server and gateway/security starter. Validate issuer, audience, signature/JWKS, expiry, and required scopes/roles; do not write a custom HS256 JWT flow for a production service.
- Distinguish authentication from authorization and resource ownership. A valid token does not authorize access to another account or tenant.
- Use OAuth2 client credentials or the repository's approved service identity for outbound service calls. Store client secrets/keys in Vault or injected secrets, never source/configuration files.
- Use API keys only where the partner contract requires them; hash/rotate/revoke them and scope them to a partner/environment/operation.
- Sign high-risk partner requests with the approved HMAC scheme over a canonical method/path/timestamp/body representation. Verify with constant-time comparison and reject stale/replayed timestamps.
- Propagate only the minimum authenticated context through trusted headers. Strip client-supplied identity headers at the gateway before injecting verified values.

## FastAPI/Pydantic services

The current Python services use FastAPI 0.115.0, Pydantic 2.9.0, SQLAlchemy 2.0.35, and asyncpg. Re-check exact versions before using APIs.

- Keep routers thin: request model → authenticated dependency → application service → response model. Do not put transaction or external integration logic in route functions.
- Use Pydantic models for request/response validation and generated OpenAPI. Use `Decimal` with explicit `max_digits`/`decimal_places` constraints for money; never model a financial amount as `float`.
- Use `response_model` and explicit status codes. Do not return ORM entities or arbitrary dictionaries from public endpoints when a stable schema is required.
- Use async drivers and `await` for database/network I/O in `async def` routes. A blocking client or CPU-heavy operation must use a synchronous route or an explicit worker/offload boundary; never block the event loop.
- Use lifespan for startup/shutdown resources and close engines/consumers cleanly. Do not use `BackgroundTasks` for durable financial work, retries, or Kafka publication; use the outbox/worker path.
- Install exception handlers for validation, auth, rate limits, upstream failures, and unknown errors. Sanitize responses and preserve request IDs.
- Configure CORS with exact trusted origins and no wildcard when credentials are enabled. Keep the platform header contract (`X-Idempotency-Key`) consistent in `allow_headers`.
- Bound body size, file type, query limits, and upload dimensions before OCR/ML work. Rate-limit public and expensive endpoints by an identity-aware key, not only source IP.

## Spring Boot, Quarkus, and outbound integrations

- For blocking JVM calls, use the repository's shared `rest-client-starter`/`PayuRestClient` or verified Spring Boot 4.1 `RestClient`/HTTP interface client. For reactive paths, use the existing WebClient/Vert.x boundary and keep it non-blocking.
- Configure connect, read, pool, and total-call timeouts explicitly. Spring Boot 4.1 supports common `spring.http.clients` settings; verify how the shared starter applies them before adding per-client overrides.
- Classify upstream responses before retrying. Retry transient connection failures, timeouts, selected `5xx`, and `429` with bounded exponential backoff/jitter and `Retry-After`; do not retry validation, auth, permission, or deterministic business errors.
- Retry a mutation only when the partner/API provides idempotency or the operation is provably safe. Preserve request IDs and idempotency keys across attempts.
- Use the shared `resilience-starter` for circuit breaker, retry, time limiter, bulkhead, and rate limiter. One request must not stack several independent retries and multiply load.
- Propagate correlation IDs, partner reference IDs, and trace context. Redact authorization, API keys, signatures, and PII from logs.
- Define an explicit failure contract: timeout, circuit open, rate limited, invalid upstream response, and ambiguous accepted outcome must be distinguishable to callers.

## Webhooks and callbacks

Use a verify → deduplicate → acknowledge → process flow:

1. Read the raw body without normalization, enforce content type/size, and validate required headers.
2. Verify HMAC/signature, timestamp/replay window, partner identity, and event schema with constant-time comparison.
3. Reserve the webhook ID/event ID atomically before any side effect. Same event replay must be safe; conflicting payloads must be rejected and alerted.
4. Return `202 Accepted` only after authenticity and basic validity pass. Queue durable processing through the outbox/worker boundary.
5. Classify failures as retryable or terminal, record delivery/processing attempts, and route exhausted events to a visible DLQ/manual-replay path.

Never acknowledge an unauthenticated webhook, perform a long partner call before ACK, or log the raw signed payload when it contains PII/secrets.

## Gateway, BFF, and boundary security

- Keep gateway routes allowlisted and bounded. Validate service names/paths against configuration; do not proxy arbitrary hostnames or URLs.
- Enforce authentication, authorization, rate limits, body limits, TLS, and request IDs at the appropriate boundary. The gateway is not a substitute for service authorization.
- For browser BFF flows, keep tokens in HttpOnly cookies and let the server attach downstream authorization. Browser JavaScript must not construct Bearer headers from stored tokens.
- Configure CORS at the gateway or service boundary with explicit origins, methods, headers, and credential behavior. Never pair credentials with `*`.
- Validate redirect URLs, webhook URLs, partner URLs, and callback destinations against allowlists to prevent SSRF/open redirects.

## Pagination, filtering, and compatibility

- Bound `limit/size` and reject unknown sort fields. Do not expose arbitrary SQL fragments through `sort`, `filter`, or `select` parameters.
- Use keyset/cursor pagination for high-volume transaction histories with a stable `(created_at, id)` order. Offset pagination is acceptable for small/admin datasets when measured.
- Return a stable cursor opaque to clients. Define behavior for deleted/hidden rows, replica lag, and concurrent inserts.
- Prefer additive evolution: optional response fields, tolerant readers, and new endpoints/versions for breaking changes. Removing or changing enum values is breaking for many clients.

## Verification checklist

- [ ] Route, version, resource semantics, auth, ownership, tenant scope, and status codes are documented.
- [ ] Money uses precise decimal handling and currency; no float/double/`anyNumber()` financial contract exists.
- [ ] Financial writes require one stable `X-Idempotency-Key`, fingerprint reuse, replay response, and ambiguous-outcome handling.
- [ ] RFC 9457 errors are machine-readable, stable, safe, and correlated; existing success envelopes remain compatible.
- [ ] OpenAPI is generated from the actual runtime contract and includes security, headers, examples, pagination, and error responses.
- [ ] Contract/provider tests cover headers, body, status, auth, idempotency, and error behavior.
- [ ] External calls have explicit timeouts, bounded retry policy, circuit/bulkhead behavior, correlation propagation, and safe logging.
- [ ] Webhooks verify raw-body signatures, replay windows, event IDs, durable deduplication, and retry/DLQ behavior.
- [ ] FastAPI routes do not block the event loop; JVM reactive routes do not block their event loop.
- [ ] CORS, SSRF, body limits, rate limits, PII masking, secret storage, and ownership checks are tested.

## Official documentation to resolve through Context7

- FastAPI: https://fastapi.tiangolo.com/
- Pydantic: https://docs.pydantic.dev/
- Spring Boot REST clients: https://docs.spring.io/spring-boot/reference/io/rest-client.html
- OpenAPI Specification: https://spec.openapis.org/oas/latest.html
- RFC 9457 Problem Details: https://www.rfc-editor.org/rfc/rfc9457.html
- Spring Cloud Contract: https://spring.io/projects/spring-cloud-contract
