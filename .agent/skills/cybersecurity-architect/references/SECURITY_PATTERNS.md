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

## 🏗️ Security Engineering Best Practices
*   **Zero-Trust Networking**: Use mTLS (e.g., Istio STRICT mode) between services. Apply `AuthorizationPolicy` to restrict service-to-service calls based on service account principals.
*   **Container Security Context**:
    *   `runAsNonRoot: true`
    *   `allowPrivilegeEscalation: false`
    *   `readOnlyRootFilesystem: true`
    *   `drop: [ALL]` capabilities.
*   **Vault Dev Mode Healthchecks**: Always set `VAULT_ADDR=http://127.0.0.1:8200` to avoid HTTPS errors during startup probes in dev mode.
*   **PII Masking**: Ensure `@Sensitive` annotations from `security-starter` are used for NIK, PIN, and Phone numbers to mask them in logs and encrypt them in the database.
