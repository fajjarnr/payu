# Security Architecture & Cybersecurity Patterns

## 🔑 IAM & Keycloak Patterns
*   **Unified Keycloak MFA**: Standardize on Keycloak-native MFA. Remove custom biometric/MFA logic from individual services to reduce attack surface and improve compliance.
*   **Token Refresh Strategy**: Don't try to parse or rotate Keycloak refresh tokens locally in `auth-service`. Always proxy the refresh request back to Keycloak's token endpoint to maintain standard OIDC behavior.
*   **OIDC Issuer Alignment**: In container environments, Ensure `OIDC_ISSUER` is set to the internal cluster DNS of Keycloak (e.g., `http://keycloak:8080/realms/payu`) for reliable JWT verification.
*   **Direct Access Grants**: Use `directAccessGrantsEnabled: true` for backend-to-backend or CLI-based authentication where redirect-based OIDC flows aren't possible.

## 🛡️ Data Protection & Encryption
*   **JWT Storage (BFF Pattern)**: NEVER store raw JWTs in `localStorage`. This is an XSS vector and violates PCI-DSS.
    *   **The Pattern**: Use a Backend-for-Frontend (BFF). Next.js API routes should handle the token exchange and set **httpOnly, Secure, SameSite=Strict** cookies.
    *   **Proxying**: The BFF proxies requests to the backend gateway, attaching the token from the secure cookie.
*   **Strong Key Derivation**: Upgrade from plain SHA-256 to **PBKDF2WithHmacSHA256** (minimum 600,000 iterations) for deriving encryption keys from master secrets. SHA-256 is too fast and susceptible to brute-force.
*   **Key Rotation (AES-GCM)**: 
    *   Implement multi-key decryption fallback.
    *   Store multiple key versions in Vault.
    *   Try current key -> try previous keys in order.
    *   Re-encrypt data during batch migration jobs if needed.

## 🚨 IDOR Vulnerability Pattern — Ownership Verification (L-015)

Every controller endpoint accessing user-scoped resources MUST verify ownership via JWT subject BEFORE any data retrieval or mutation.

**The Anti-Pattern**: Copy-pasting `extractUserId()` into each controller with inconsistent error handling (some throw, some return null).

**Three Sub-Patterns**:
1.  **Direct comparison**: `if (!accountId.equals(userId)) throw new AccessDeniedException(...)`
2.  **Fetch-then-compare**: Load resource, check `response.getSenderAccountId().equals(userId)`
3.  **Dedicated security service**: `SplitBillSecurityService.isOwner(id, userId)` — best for complex ownership rules

**Rule**: Extract a shared `SecurityContextUtils.extractAuthenticatedUserId()` into `security-starter`. The method MUST throw `AccessDeniedException`, never return null. Apply to every user-scoped endpoint.

## 🌐 BFF Path Whitelist — SSRF Defense (L-016)

The BFF proxy (`frontend/web-app/src/app/api/v1/[...path]/route.ts`) uses an explicit `ALLOWED_PATH_PREFIXES` array. When backend services add new API paths, the BFF silently returns 400 if the prefix isn't whitelisted.

**Two-Layer SSRF Defense**:
1.  **Path sanitization**: Per-segment validation rejects `..`, control chars, encoded traversals
2.  **Prefix whitelist**: `fullPath.startsWith(prefix + '/')` — the trailing `/` prevents `/api/v1/accountsEvil` from matching `/api/v1/accounts`

**Gotcha**: The whitelist must be manually updated whenever a new service path is added. Phase 3 added 6 missing prefixes (`cards`, `pockets`, `payments`, `topup`, `billers`, `biometric`).

**Rule**: When adding a new backend API path prefix, ALWAYS add the corresponding prefix to `ALLOWED_PATH_PREFIXES` in the BFF route handler. Validate using `startsWith(prefix + '/')` with trailing slash to prevent prefix overlap attacks.

## 🏗️ Security Engineering Best Practices
*   **Zero-Trust Networking**: Use mTLS (e.g., Istio STRICT mode) between services. Apply `AuthorizationPolicy` to restrict service-to-service calls based on service account principals.
*   **Container Security Context**:
    *   `runAsNonRoot: true`
    *   `allowPrivilegeEscalation: false`
    *   `readOnlyRootFilesystem: true`
    *   `drop: [ALL]` capabilities.
*   **Vault Dev Mode Healthchecks**: Always set `VAULT_ADDR=http://127.0.0.1:8200` to avoid HTTPS errors during startup probes in dev mode.
*   **PII Masking**: Ensure `@Sensitive` annotations from `security-starter` are used for NIK, PIN, and Phone numbers to mask them in logs and encrypt them in the database.
