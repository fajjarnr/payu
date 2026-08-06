---
name: cybersecurity-architect
description: PayU cybersecurity architecture for Keycloak/RHBK OIDC, Spring Security and Quarkus JWT validation, authorization, BFF/cookie sessions, Vault/VSO/ESO secrets, AES-GCM field encryption, mTLS, gateway hardening, Kubernetes security, threat modeling, PCI-DSS/UU PDP evidence, and security testing with Context7-first verification.
---

# Cybersecurity Architect — PayU

Use this skill when designing, implementing, reviewing, testing, or operating security controls for PayU services, APIs, clients, data, containers, or infrastructure. Security claims require evidence from code, configuration, tests, deployment policy, or an approved audit artifact.

## Operating contract

1. Read `AGENTS.md`, `docs/security/SECURITY.md`, the relevant architecture/runbook, and the owning service configuration before changing a security boundary.
2. Identify the asset, trust boundary, actor, abuse case, impact, control owner, and verification evidence before proposing a fix.
3. Before using Keycloak, Spring Security, Vault, crypto, Kubernetes, or another security library/operator, resolve the official library with Context7 and query the exact installed version. If unavailable, use the closest documented version, record the mismatch, and avoid undocumented behavior.
4. Prefer the repository's `security-starter`, gateway filters, BFF routes, Vault/VSO/ESO integration, and platform policies. Do not create a parallel auth, encryption, secret, or audit implementation for one service.
5. Deny by default. Fail closed on missing issuer, key, secret, policy, signature, tenant context, or authorization decision. A test bypass must be isolated to a test profile and impossible in production.
6. Treat a finding as open until a focused regression test and runtime/configuration evidence prove the control. Never infer compliance from a checklist or a security score.

## Repository security baseline

Verify manifests and POMs before work; these values can drift.

| Boundary | Observed baseline |
|---|---|
| Identity | Red Hat Build of Keycloak/RHBK 26 family; auth service currently pins Keycloak 26.0.0 |
| JVM APIs | Spring Boot 4.1.0 / Spring Security 7-era APIs, OAuth2 Resource Server, shared `security-starter` |
| Gateway | Quarkus gateway with JWT validation, rate limits, request validation, request signing, and route policy |
| Web | Next.js BFF; access/refresh tokens are HttpOnly cookies and are forwarded server-side |
| Mobile | Expo/RN auth layer; tokens are kept in SecureStore, not Zustand or React Query cache |
| Secrets | Vault plus Vault Secrets Operator/External Secrets integrations; injected secrets, not source-controlled values |
| Crypto | Shared AES-256-GCM `EncryptionService` with random IVs and previous-key rotation support |
| Platform | OpenShift restricted-v2 posture, non-root workloads, NetworkPolicies, Kyverno, image/signature controls, mTLS policy where deployed |

Context7 currently documents Keycloak 26.5.2, so verify differences before applying it to the installed 26.0.0 client/server.

## Threat modeling workflow

For every feature or integration:

1. Classify data: public, internal, confidential, PII, authentication material, payment/card data, or regulated financial record.
2. Draw the flow: browser/mobile/partner → gateway/BFF → service → database/cache/broker/third party.
3. Enumerate spoofing, tampering, repudiation, information disclosure, denial of service, elevation of privilege, SSRF, replay, and abuse of business limits.
4. State controls at each boundary: identity, authorization, validation, rate limit, encryption, audit, isolation, recovery, and alerting.
5. Add negative tests for the highest-impact abuse cases before implementation and attach evidence after deployment.

Do not introduce a generic “secure” middleware without showing which trust boundary and threat it closes.

## Authentication: Keycloak/OIDC

- Use the existing Keycloak/OIDC issuer, realm, client, and security starter. Validate issuer, signature/JWKS, expiry, not-before, audience, and required scopes/roles at the resource server.
- Prefer Authorization Code with PKCE (`S256`) for browser/mobile user authentication. The web BFF may keep tokens server-side in HttpOnly cookies; mobile public clients must not contain a client secret.
- Use client credentials/service accounts for service-to-service calls with least-privilege scopes and a separate client per trust boundary. Do not use a user token or admin credential for background work.
- Do not introduce password/direct-grant flow for new clients. If a legacy auth endpoint uses it, isolate it, document the migration, rate-limit it, and verify the current Keycloak policy before changing it.
- Configure exact redirect URIs, allowed origins, post-logout redirects, and Web Origins. Never use wildcard redirects or pass access/refresh tokens in query strings, fragments, logs, or error messages.
- Use Keycloak logout/revocation and refresh-token rotation according to the installed realm/client policy. A local logout flag is not token revocation.
- Treat realm roles, resource roles, and scopes as distinct inputs. Centralize claim-to-authority mapping, review derived permissions for privilege escalation, and test every sensitive route with both allowed and denied claims.
- SAML is permitted only where a partner requires it; validate signed metadata/assertions, audience, recipient, clock skew, and replay behavior through the approved integration boundary.

## JWT and authorization

- Never authorize from an unverified/decoded JWT. Let the resource server validate the token, then authorize the verified subject, tenant, scopes, roles, and resource ownership.
- Use defense in depth: gateway authentication does not replace service authentication and authorization.
- Return `401` for missing/invalid authentication and `403` for an authenticated principal lacking permission. Avoid revealing whether a protected resource exists when that leaks sensitive information.
- Check ownership/tenant scope in the service transaction, not only in a gateway route or client UI. Never trust `X-User-Id`, `X-Tenant-Id`, account IDs, or role headers supplied by the caller.
- Prefer permission checks close to the domain operation. Role names are policy inputs, not a substitute for account ownership, transaction limits, step-up authentication, or maker-checker approval.
- Use separate authorities for customer, backoffice, partner, system, and operator contexts. Do not let a broad admin role silently inherit all financial permissions without explicit policy and audit review.
- Use Spring Security test support or a signed test token in test-only configuration. Mock `alg=none` tokens and `permitAll` test chains must never be active outside a test profile.

## Session, cookie, and client storage

### Web BFF

```text
Browser → Next.js /api/auth/* and /api/v1/* → BFF → gateway → services
          HttpOnly cookies                       server-side Bearer header
```

- Keep access/refresh tokens out of web JavaScript, localStorage, sessionStorage, Zustand, React Query, URLs, telemetry, and screenshots.
- Set cookie flags deliberately: `HttpOnly`, `Secure` in deployed environments, an appropriate `SameSite` policy, narrow path/domain, and an explicit lifetime. Pair cookie-authenticated writes with the repository's CSRF/origin protection.
- Do not treat a client `isAuthenticated` flag as authorization. Refresh/validate with the BFF and backend.
- Preserve BFF path allowlists, gateway URL allowlists, body limits, timeouts, SSRF tests, correlation IDs, and security headers.

### Mobile

- Store native tokens only in Expo SecureStore through the existing auth layer. Never persist tokens in AsyncStorage, Zustand, React Query, logs, or snapshots.
- Biometric authentication is a local unlock for an existing session, not proof of backend authorization. Revalidate the session and enforce server-side step-up requirements for high-risk actions.
- Do not put client secrets in an installed app. Use PKCE and a public-client design where the protocol requires it.

## Secrets and key management

- Store credentials, API keys, signing secrets, database passwords, OIDC client secrets, encryption keys, and certificates in Vault or the approved secret operator. Kubernetes Secret objects are delivery artifacts, not permission to commit plaintext values.
- Use Kubernetes authentication with a dedicated service account, Vault role, namespace/path policy, and least-privilege capabilities. Bind each workload only to the paths it needs.
- Do not use `${SECRET:default}`, demo passwords, fallback admin credentials, or checked-in private keys in production profiles. Missing production secrets must fail fast.
- Separate dev/test fixtures from deployed secrets. Redact secret values from Helm output, logs, crash dumps, CI artifacts, and support bundles.
- Define rotation before onboarding a secret: owner, TTL, dual-key overlap, consumers, rollout order, revocation, rollback, and evidence. Test rotation without decrypting old data in application logs.
- Use dynamic credentials or short-lived tokens when the platform supports them. Enable Vault audit logging and alert on unusual reads, policy changes, authentication failures, and disabled/expired leases.
- Reconcile Vault/VSO/ESO ownership. Do not run multiple secret controllers against the same destination without a documented ownership rule.

## Encryption and cryptography

- Use the shared `EncryptionService` or an approved platform primitive. Do not implement crypto, token signing, password hashing, or key derivation with ad-hoc code.
- AES-GCM requires a fresh unpredictable IV/nonce for every encryption and authenticated handling of the tag. Never reuse an IV with a key; never log plaintext, keys, or ciphertext that contains sensitive context.
- Keep key version metadata with ciphertext or use the shared format so previous keys can decrypt during rotation. Re-encrypt in a controlled migration; do not mass-decrypt into logs or temporary files.
- Use Vault Transit/KMS for key-encryption operations when appropriate; keep data keys and key-encryption keys separate. Define access policies for encrypt/decrypt separately where possible.
- Passwords are one-way verifier material, not encryptable data. Use the identity provider's password policy and supported password hashing; never store or log plaintext passwords.
- Use constant-time comparison for HMAC/signature/token hash comparisons. Use approved TLS/mTLS libraries and certificate validation; never disable hostname or chain verification to make an integration work.
- For searchable encrypted PII, use an approved keyed blind index with a documented leakage assessment. Do not decrypt every row or store a raw hash of low-entropy identifiers.

## PII, payment data, and audit evidence

- Minimize collection and retention. Classify NIK, PIN, PAN, CVV, biometrics, tokens, account numbers, and device identifiers separately; tokenize or encrypt before persistence where required.
- Mask sensitive values in logs and responses: show only the minimum suffix/prefix needed for support. Never log Authorization, Cookie, Set-Cookie, client secrets, PINs, CVV, full PAN, or raw identity documents.
- Keep payment/ledger data immutable and auditable. Corrections use reversal/compensating entries, not destructive edits.
- Audit authentication attempts, authorization failures, privileged access, secret/key changes, configuration changes, data access, and financial state transitions with actor, action, target, time, result, request ID, and reason where applicable.
- Protect audit logs from application tampering, define retention/legal hold, restrict readers, synchronize time, and alert on gaps or suspicious access.
- Map controls and evidence to PCI-DSS 4.0, UU PDP, OJK/BI obligations, and contractual partner requirements. Do not label the platform compliant without an approved assessment and current evidence.

## API and gateway security

- Validate input at every trust boundary: content type, body size, schema, encoding, path/query limits, file dimensions/type, and business constraints. Reject unknown or dangerous fields where the contract requires it.
- Enforce authorization in each service. Apply rate limits by the correct identity/tenant/operation and add stricter controls for login, OTP, password reset, payment, transfer, and expensive ML endpoints.
- Protect cookie-authenticated APIs against CSRF. Configure CORS with exact origins and never combine credentials with `*`.
- Protect all URL fetches and proxy routes against SSRF: allowlist schemes/hosts/paths, block private/link-local metadata ranges, prevent redirects to disallowed destinations, and bound response size/time.
- Use CSP, HSTS, frame/content-type/referrer policies, secure cache directives for sensitive responses, and request/correlation IDs. Do not rely on obsolete browser headers as a primary control.
- For signed callbacks/partner requests, verify the raw message, timestamp window, key identity, canonical representation, and replay/idempotency record before side effects.
- Use RFC 9457-safe error responses. Do not reflect exception messages, SQL, stack traces, tokens, or PII to callers.

## OpenShift/Kubernetes zero trust

- Apply default-deny NetworkPolicies and add only required namespace/service/port/egress paths. Verify both ingress and egress, DNS, telemetry, and operator exceptions.
- Run workloads as non-root under the restricted OpenShift SCC. Drop all capabilities, use `allowPrivilegeEscalation: false`, read-only root filesystem where compatible, seccomp/default runtime profile, resource limits, and explicit service accounts.
- Keep secrets out of images and ConfigMaps. Mount only the secret paths required by the workload and avoid broad service-account token automounting.
- Require approved/signed images, pinned immutable digests where release policy requires them, SBOMs, vulnerability scanning, provenance, and admission enforcement. Do not claim SLSA maturity without generated evidence.
- Enforce TLS/mTLS between services where the mesh/platform contract requires it. Never use `setenforce 0`, trust-all TLS, `verify=false`, or an insecure OIDC TLS mode to bypass certificate problems.
- Protect the Kubernetes API and operator resources with least-privilege RBAC, namespace boundaries, audit logging, and reviewable break-glass access.

## Security testing and response

- Unit-test claim mapping, authorization decisions, ownership/tenant checks, encryption round trips/rotation, masking, signature verification, replay rejection, and rate-limit boundaries.
- Integration-test real OIDC/JWKS behavior with the repository's Keycloak/Testcontainers setup, including expired tokens, wrong issuer/audience, rotated keys, missing scopes, and disabled users.
- Test BFF cookie flags/token absence, gateway SSRF/path validation, CORS/CSRF, security headers, request-size limits, and service-to-service mTLS/auth propagation.
- Run SAST, dependency/SCA, secret scanning, image scanning, SBOM/provenance checks, DAST/ZAP, contract tests, and scheduled penetration tests according to CI/platform policy. Treat suppressions as owned, time-bounded risk acceptances.
- For incidents: preserve evidence, contain access, revoke/rotate affected credentials, protect forensic integrity, assess data exposure, notify required owners/regulators, and record a tested recovery action. Do not silently delete logs or “fix” evidence in place.

## Review checklist

- [ ] Asset, threat model, trust boundaries, control owner, and verification evidence are explicit.
- [ ] OIDC issuer/JWKS/audience/expiry/roles and service authorization are verified server-side.
- [ ] Web/mobile token storage follows BFF HttpOnly/SecureStore policy; no token appears in URL, storage, logs, or telemetry.
- [ ] Secrets use Vault/VSO/ESO with least privilege, rotation, fail-fast production behavior, and audit trails.
- [ ] Encryption uses approved primitives, fresh IVs, key versions, rotation, and no plaintext leakage.
- [ ] PII/payment data is minimized, masked, encrypted/tokenized, immutable where required, and retained legally.
- [ ] API/gateway controls cover CSRF/CORS, SSRF, body limits, rate limits, validation, safe errors, and request correlation.
- [ ] OpenShift controls cover NetworkPolicies, non-root, dropped capabilities, read-only filesystem, RBAC, signed images, and mTLS.
- [ ] Negative tests and runtime evidence cover authentication, authorization, replay, secrets, crypto, and incident paths.

## Official documentation to resolve through Context7

- Keycloak: https://www.keycloak.org/documentation
- Spring Security OAuth2 Resource Server: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html
- HashiCorp Vault: https://developer.hashicorp.com/vault/docs
- Vault Secrets Operator: https://developer.hashicorp.com/vault/docs/deploy/kubernetes/vso
- OWASP ASVS: https://owasp.org/www-project-application-security-verification-standard/
- OWASP Cheat Sheets: https://cheatsheetseries.owasp.org/
- PCI Security Standards: https://www.pcisecuritystandards.org/standards/pci-dss/
- Kubernetes Pod Security Standards: https://kubernetes.io/docs/concepts/security/pod-security-standards/
