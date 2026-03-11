# ADR-0010: Security Standards (PII & Encryption)

**Status**: Accepted
**Date**: 2026-01-30
**Last Updated**: 2026-03-11
**Deciders**: Security Team

## Context

As a digital banking platform and payment gateway, we handle Personally Identifiable Information (PII) and financial data. We must comply with OJK, PCI-DSS v4.0, and UU PDP (Indonesia Data Protection Law) regulations regarding data protection.

## Decision

Enforce strict security controls via `security-starter`.

### Key Controls

1. **PII Protection**:
    - **Data Masking**: All logs must mask PII (NIK, Phone, Email) using `DataMaskingAspect` (triggered by `@Audited`).
    - **Field Encryption**: Sensitive columns in DB (NIK, Mother's Name) MUST be encrypted using AES-256 standard.
    - **Annotation**: Use `@Sensitive` on DTO fields to trigger masking/encryption.
    - **Response Masking**: `ResponseMaskingFilter` in gateway masks card numbers, account numbers, phone numbers in API responses.
2. **Audit Logging**:
    - Tracking: User ID (from `SecurityContextHolder`), IP, Action, Timestamp, Resource.
    - Integrity: Audit logs are immutable (DELETE endpoint removed — BUG-BE-081).
    - User extraction chain: (1) JWT subject, (2) `X-User-Id` header, (3) `"anonymous"`.
3. **Authentication**:
    - OAuth2 / OIDC via Red Hat Build of Keycloak (RHBK) v26.
    - No basic auth for internal APIs.
    - JWT validation per endpoint with `@PreAuthorize`.
    - Risk-based MFA configurable via `payu.security.risk.mfa-enabled`.
4. **Fail-Closed** (IMP-064):
    - `SecurityAutoConfiguration` defaults: `masking-enabled=true`, `audit-enabled=true` (`matchIfMissing=true`).
    - `encryption-enabled` stays `false` (requires key configuration) — opt-in only.
5. **Partner Security**:
    - HMAC-SHA256 webhook payload signing (`X-PayU-Signature: sha256=...`).
    - API key management with SHA-256 hash storage (plain key returned once at creation).
    - SNAP-BI timestamp window validation (±5 min) for replay attack prevention.

## Implementation

- `security-starter` provides `AttributeConverter` for JPA encryption.
- `security-starter` provides `DataMaskingAspect` for log masking (pointcut narrowed to `@Audited` — BUG-BE-030).
- `AuditAspect` uses `SecurityContextHolder` for user extraction (IMP-065), not `request.getAttribute("principal")`.
- All services must enable audit logging for write operations.
- PBKDF2 salt configurable via `payu.security.encryption.salt` (BUG-BE-019).
- SLF4J fallback when Kafka unavailable for audit logs.

### Compliance Audit Results (Feb 2026)

| Standard     | Score  | Status       |
| :----------- | :----- | :----------- |
| PCI-DSS v4.0 | 94/100 | ✅ Compliant |
| UU PDP       | 96/100 | ✅ Compliant |
| OJK          | 95/100 | ✅ Compliant |

> See `docs/security/PCI-DSS-UU-PDP-AUDIT-REPORT.md` for full audit report.

### Key Security Fixes Applied

- **BUG-BE-005**: Removed plaintext token logging from `KeycloakService`.
- **BUG-BE-006**: Narrowed gateway public prefix to `/api/v1/accounts/register` only.
- **BUG-BE-016**: Added `maskUsername()` helper for PII in logs.
- **BUG-BE-017**: Downgraded Authorization header log from INFO to DEBUG.
- **BUG-BE-033**: CORS restricted from `*` to specific domains.
- **BUG-BE-119**: Replaced `java.util.Random` with `SecureRandom` for card numbers.
- **BUG-BE-134**: SNAP-BI ±5 min timestamp validation for replay attack prevention.

## Consequences

- **Positive**: Compliance with regulations, reduced data leak risk, fail-closed defaults protect against misconfiguration.
- **Negative**: Processing overhead for encryption/decryption, audit log volume at scale.
