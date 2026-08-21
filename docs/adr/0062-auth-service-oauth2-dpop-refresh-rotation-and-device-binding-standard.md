# ADR-0062: Auth Service — OAuth2, DPoP, Refresh Rotation & Device Binding Standard

**Status**: Accepted  
**Date**: 2026-08-22  
**Deciders**: Core Banking Engineering, Platform Engineering, Risk & Compliance  
**Relates to**: ADR-0010 (Security), ADR-0028 (Step-Up & Dynamic Linking), ADR-0039 (BFF Token Relay), ADR-0044 (Vault), RHBK 26.4/26.6, RFC 9449 DPoP, RFC 9700 BCP OAuth2.1

---

## Context

`backend/auth-service` (`AuthServiceApplication.java`, `application/service/RefreshTokenService.java:19`, `config/SecurityConfig.java:21`) saat ini:

* Flows: `POST /api/v1/auth/callback` (OIDC code exchange), `/register`, `/refresh`, `/logout`, `/mfa/verify` — public di `PUBLIC_ENDPOINTS`, JWT chain `Order 1..3`. Issuer `spring.security.oauth2.resourceserver.jwt.issuer-uri` ke RHBK `http://localhost:8080/realms/payu`.
* Refresh: `RefreshTokenService` `BCrypt(12)` hash + `DistributedCache` `auth:refresh:{userId}:{tokenId}` + `reverseIndex {tokenId}->{userId}` TTL 7d (`REFRESH_TOKEN_TTL`), `rotateRefreshToken()` invalidate old + create new, `invalidateAllUserTokens()` wildcard `evictMatching`. Sudah rotation, tapi **belum revoke enforcement di RHBK realm** (`RealmModel.isRevokeRefreshToken/setRevokeRefreshToken`, `refreshTokenMaxReuse=0` — Context7 RHBK), dan **belum DPoP binding** (RFC 9449).
* Security: `Argon2PasswordEncoder`, `SecurityHeadersFilter`, `SessionCreationPolicy.STATELESS`, `JwtDecoder` Nimbus — benar. Namun `Direct Grant/password` masih ada di codebase (docs RHBK 26.6 warn `MUST NOT use per RFC 9700, prefer Authorization Code + PKCE` atau `Device Grant`), dan public client (mobile/Web BFF) masih pakai Bearer tanpa Proof-of-Possession → token replay risk (docs RHBK DPoP: leaked Bearer via logs/storage = reuse).
* RHBK 26.4+ `DPoP is now supported` (was preview since 23), binds `access+refresh` to client keypair `cnf.jkt` thumbprint, proof `DPoP+jwt` `htm/htu/jti/ath/nonce` per-request, `Require DPoP bound tokens` switch, `dpop-bind-enforcer` client policy, revocation endpoint `/protocol/openid-connect/revoke`. `BFF` pattern (forum keycloak) recommended for public clients: BFF holds refresh, browser only `session_id`.

## Decision Drivers

* **FAPI/PADG**: Financial-Grade API wajib sender-constrained tokens (DPoP/mTLS) untuk payment initiation.
* **PCI-DSS 8.2.4**: rotate authentication keys; refresh rotation `RevokeRefreshToken=true` + `maxReuse=0` is strictest.
* **RFC 9700**: `password grant` & `implicit` MUST NOT/SHOULD NOT — must migrate to `Authorization Code + PKCE` (web) / `Device Authorization Grant` (constrained device).
* **Usability**: mobile expiry UX vs security — short access (5-15m) + rotated refresh.

## Considered Options

### Option A — Harden in-place: DPoP + strict rotation + BFF + PKCE (chosen)

* **Pros**: RHBK 26.4 ready, no extra infra; DPoP per-request proof stops replay even if Bearer leaked; BFF keeps refresh out of browser (session cookie HttpOnly `__Host-`, SameSite Strict, CSRF token per ADR-0039); PKCE covers public client.
* **Cons**: DPoP needs client keypair generation + `DPoP` header per call (extra JS/native code); nonce handling on 401 retry.

### Option B — mTLS bound tokens saja (tanpa DPoP)

* **Pros**: strong binding at TLS layer.
* **Cons**: mobile tidak bisa client cert rotation mudah; DPoP lebih portable untuk SPA/native.

### Option C — Long-lived access token (tanpa refresh rotation)

* **Pros**: simple.
* **Cons**: revoked token tetap valid sampai expiry — violates FAPI 2 `suppress-refresh-token-rotation` only for specific flow, not default.

## Decision

**Option A — DPoP + strict refresh rotation + BFF.**

1. **Realm**: `isRevokeRefreshToken=true`, `refreshTokenMaxReuse=0` (Keycloak Admin `RealmModel` per Context7), `SsoSessionIdleTimeout=30m`, `SsoSessionMaxLifespan=12h` (rememberMe separate). Client policy `suppress-refresh-token-rotation` **tidak** dipakai untuk public clients (hanya untuk confidential service-to-service jika perlu FAPI 2).
2. **DPoP**: enable `Require DPoP bound tokens` untuk `payu-web` (public) + `payu-mobile` (public) clients di RHBK Admin `Capability config`; resource server validate `Authorization: DPoP <token>` + `DPoP` proof (`typ=dpop+jwt`, `alg=ES256`, `jwk`, `jti`, `htm`, `htu`, `iat`, `ath` hash, optional `nonce` challenge). Refresh endpoint public client requires DPoP proof signed with same key (RHBK DPoP handling refresh token). Fallback: legacy confidential `payu-internal` tetap Bearer+mTLS.
3. **BFF**: Web App (`frontend/web-app`) via `ADR-0039` BFF token relay — browser hanya `session_id` HttpOnly, BFF stores `access+refresh` + DPoP private key (per-session, memory or `Data Grid` encrypted), auto `updateToken(minValidity=5)` / rotation, revoke on logout via `POST /protocol/openid-connect/revoke` (refresh+access). `Keycloak forum BFF` best practice.
4. **Flows**: migrate off `password grant` → `Authorization Code + PKCE` (web) + `Device Authorization Grant` (`/auth/device`, polling) untuk TV/constrained; `Implicit` removed (already SHOULD NOT per RHBK 26.6).
5. **Device binding & step-up**: `POST /api/v1/auth/mfa/challenge` + `MFA verify` (public, no JWT yet `BUG-BE-166`) + risk-based step-up (`ADR-0028`) + device fingerprint `DeviceId` header propagated to `transactions` `additionalInfo.deviceId` for dynamic linking.
6. **Spring Resource Server**: `NimbusJwtDecoder` via `jwk-set-uri`, local JWT validation + `cnf.jkt` DPoP thumbprint check in custom `JwtAuthenticationConverter`/`DPoPProofFilter`; fallback introspection only for lightweight tokens. `SecurityConfig` keep 3 chains `Order 1 public` (includes `/callback`, `/refresh` with DPoP proof), `Order 2 actuator`, `Order 3 JWT`.
7. **Revocation**: `POST /api/v1/auth/logout` calls RHBK `revoke` + `RefreshTokenService.invalidateToken` + `backchannel-logout` (`/protocol/openid-connect/logout/backchannel-logout`) for single-sign-out across clients.

## Rationale

* RHBK 26.4 DPoP officially supported + RHBK docs: `Public Clients: DPoP is critical, binds both access+refresh` — matches our `payu-web/mobile` public clients.
* Context7 refresh rotation strict (`revoke=true, maxReuse=0`) + token revocation endpoint = exactly the `GAP-30` style fail-closed we already do for encryption (`SecurityAutoConfiguration.java:62`).
* RFC 9700 + RHBK guide: implicit/password are deprecated for banking — migration cost paid once.

## Consequences

**Positive**:
* Stolen Bearer useless without private key (sender-constrained) — FAPI compliant.
* Replay of old refresh fails immediately (`maxReuse=0`).
* Refresh never lands in browser storage.

**Negative**:
* Native/mobile must ship keypair + DPoP signer + nonce retry loop (library `oauth2-dpop`).
* Extra roundtrip on first `DPoP-Nonce` 401 (cached 5-15m).

## Implementation Notes

* Realm export: `realm.json` `revokeRefreshToken:true, refreshTokenMaxReuse:0, ssoSessionIdleTimeout:1800, ssoSessionMaxLifespan:43200`.
* Client `payu-web`: `standardFlowEnabled:true, directAccessGrantsEnabled:false, publicClient:true, attributes.DPoPBound:true`, `webOrigins: ["https://payu.co.id"]`, `redirectUris: ["https://payu.co.id/api/v1/auth/callback"]` + `PKCE S256`.
* Env: `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` via Vault (ADR-0044), `RHBK_JWK_CACHE_TTL=5m`.
* Tests: `RefreshTokenService` rotation reuse must throw `BadCredentialsException`; DPoP e2e via `Keycloak FAPI Playground` flow; `SecurityConfig` test `permitAll` for `/auth/refresh` carries DPoP proof.
* Observability: metric `keycloak_revoked_refresh_total`, `dpop_nonce_retry_total` per ADR-0034.

---
*Teams: cybersecurity-architect + core-banking-engineer — references web 2026-08-22 (RHBK 26.6 DPoP RFC9449, RHBK forum BFF, RFC9700) + Context7 `/keycloak/keycloak` + CodeGraph `RefreshTokenService.java:19`, `SecurityConfig.java:21`*
