# ADR-0010: Security Standards (PII & Encryption)

**Status**: Accepted
**Date**: 2026-01-30
**Deciders**: Security Team

## Context

As a digital banking platform, we handle Personally Identifiable Information (PII) and financial data. We must comply with OJK, PCI-DSS, and UU PDP regulations regarding data protection.

## Decision

Enforce strict security controls via `security-starter`.

### Key Controls

1.  **PII Protection**:
    - **Data Masking**: All logs must mask PII (NIK, Phone, Email) using Logback masking.
    - **Field Encryption**: Sensitive columns in DB (NIK, Mother's Name) MUST be encrypted using AES-256 standard.
    - **Annotation**: Use `@Sensitive` on DTO fields to trigger masking/encryption.
2.  **Audit Logging**:
    - Tracking: User ID, IP, Action, Timestamp, Resource.
    - Integrity: Audit logs are immutable.
3.  **Authentication**:
    - OAuth2 / OIDC via Keycloak.
    - No basic auth for internal APIs.

## Implementation

- `security-starter` provides `AttributeConverter` for JPA encryption.
- `security-starter` provides Logback layout for masking.
- All services must enable audit logging for write operations.

## Consequences

- **Positive**: Compliance with regulations, reduced data leak risk.
- **Negative**: Processing overhead for encryption/decryption.
