# ADR-0040: Field-Level Encryption, Searchable Encryption via HMAC Blind Indexing & Key Lifecycle

**Status**: Accepted  
**Date**: 2026-08-19  
**Deciders**: Principal Architect, Cybersecurity Architect, Data Architect, Platform Engineer  
**Relates to**: ADR-0039 (BFF), ADR-0033 (RLS), PARTNER-PROD-002, UU PDP, PCI-DSS v4  

---

## Context

PayU stores PII that is high-value for attackers: `nik`, `phone`, `email`, `recipient`, `kyc` blobs. Threat model includes DB dump, backup exfiltration, and insider read. TLS protects in transit; filesystem/LUKS protects stolen disk, but not SQL compromise. Separate, governed field-level protection is required.

Temuan codegraph `2026-08-19` (`backend/shared/security-starter`):

* `EncryptedStringConverter.java:32` (`@Convert` JPA) delegates to `EncryptionService` (`AES-GCM 256`) for `PartnerEntity.apiKey/clientSecret`, `UserEntity.email/phone`, `ProfileEntity`, `KycReviewEntity`. Production enforces `payu.security.encryption.password` (`ENCRYPTION_KEY` via Vault) and `salt` (`GAP-30`), fail-fast in `container/prod/staging/sit/uat/preprod`; dev uses fixed key (`BUG-SHARED-002`).
* `BlindIndexService.java:58` (`HMAC-SHA256` with `blind-index-key`, `key-version`, `previous-keys`) provides deterministic searchable hash for `email`/`phone` (`UserPersistenceAdapter.saveIndexesEmailAndPhone`, `findByEmailUsesHash`), with `SKIP LOCKED` backfill batch.
* `SecurityAutoConfiguration.java:48` wires `encryptionService`, `encryptedStringConverter`, `blindIndexService`; `DataMaskingAspect`, `AuditAspect`, `LogbackMaskingFilter` mask PII in logs/traces (ADR-0034).
* Gaps without a standard: no central inventory of which fields are `CRITICAL` vs `RESTRICTED`, no per-field `purpose` binding, no documented rotation/batching MOP, and no governance for Python services (`analytics-service`, `kyc-service`) which duplicate `AES-GCM` via `python-starter` expectation.

Without this ADR, risk: plaintext PII in DB/backups, search breaks after encryption, key rotation corrupts data, and UU PDP/POJK audit fails.

**Best practice industri bank/e-wallet** (check internet `OWASP Cryptographic Storage Cheat Sheet` 2026-08-19 sebelum tulis):

* AES-256-GCM authenticated mode, 12-byte IV random per record, envelope `DEK` (data key) + `KEK` (Vault KMS/HSM) — never hardcode keys.
* Searchable encryption via HMAC blind index (deterministic, with key-version + previous-keys) — never ECB/deterministic AES for search.
* Key lifecycle: generation via CSPRNG, distribution via Vault/ESO, rotation on fixed cryptoperiod + compromise + staff exit, re-encryption batch with `SKIP LOCKED`, dual-read during rotation.

## Decision Drivers

* **PDP/POJK compliance** — `nik/phone/email` must be confidential at rest and masked in telemetry.
* **Searchability** — `email/phone/nik` lookup must remain `O(1)` without decrypting full table.
* **Rotation safety** — rotate without downtime or undecryptable rows; multi-pod consistency.
* **Separation of keys and data** — keys in Vault KMS/HSM, not in DB/image/env.
* **Least PII** — minimize stored PII; prefer tokenization where possible (PAN already tokenized per READY-060).
* **Single implementation** — one Java `security-starter` + one Python helper, not per-service forks.

## Considered Options

### Option 1 — AES-GCM + HMAC blind index + Vault KEK/DEK + versioned rotation (dipilih)

Pros: OWASP-aligned, searchable, rotatable, bank-grade audit. Cons: extra column (`*_bidx`) + batch job.

### Option 2 — Transparent Data Encryption (TDE) / DB-level only

Pros: no code change. Cons: protects only stolen disk, not SQL dump or `SELECT *` by compromised app — insufficient for PDP/P0. Ditolak sebagai sole control (defense-in-depth only).

### Option 3 — Deterministic AES (SIV) for search

Pros: one column. Cons: leaks equality + frequency, weaker than HMAC; not OWASP-recommended for PII search. Ditolak.

## Decision

Adopsi **Option 1 — Field-Level Encryption (AES-256-GCM) + HMAC Blind Index + Vault-managed Key Lifecycle** sebagai standar PayU.

```mermaid
flowchart LR
    APP["App (JPA/Python)"] -->|@Sensitive + @Convert| ENC["EncryptedStringConverter<br/>AES-GCM DEK"]
    APP -->|indexEmail/phone| BIDX["BlindIndexService<br/>HMAC-SHA256"]
    BIDX --> COL["*_bidx column (indexed)"]
    ENC --> DEK["DEK (per-field)"]
    DEK -->|wrapped by| KEK["KEK in Vault KMS/HSM"]
    KEK --> ROT["Rotation batch<br/>SKIP LOCKED"]
```

### 1. Classification & Inventory

| Level | Examples | Storage |
|---|---|---|
| `CRITICAL` | `nik`, `clientSecret`, `apiKey`, `kyc` blobs | AES-GCM + HMAC index + masked logs |
| `RESTRICTED` | `email`, `phone`, `fullName`, `address` | AES-GCM + HMAC index where searchable |
| `INTERNAL` | `tenant_id`, `account_number` (non-PII) | RLS + masking, no field encryption |

Single inventory file: `docs/security/PII_INVENTORY.md` (field, table, level, search need). `@Sensitive(SensitivityLevel.CRITICAL)` drives converter + masking.

### 2. Encryption (AES-256-GCM)

* **Algorithm**: `AES/GCM/NoPadding`, 256-bit `DEK`, 12-byte IV `SecureRandom` per record, 128-bit tag, AAD = `tenant_id|field|key_version`.
* **Envelope**: `DEK` random per field/table, wrapped by `KEK` in Vault (`transit` or `kv` with `KEK` in HSM). `EncryptionService(password, previousKeys, salt)` derives `KEK` via `PBKDF2` only for dev; prod uses Vault-injected `ENCRYPTION_KEY`.
* **JPA**: `@Sensitive @Convert(converter=EncryptedStringConverter.class) @Column(length=512)` for `CRITICAL`; pass-through mode when `encryption-enabled=false` only in `test` profile.
* **Python**: `payustarter.crypto.EncryptedString` (AES-GCM, same AAD) for `analytics-service`/`kyc-service`; key fetched from Vault ESO env `ENCRYPTION_KEY`.

### 3. Searchable Encryption (HMAC Blind Index)

* **Index**: `HMAC-SHA256(blind-index-key, normalized_value)` hex, stored in `*_bidx` column (`email_bidx`, `phone_bidx`, `nik_bidx`) with `UNIQUE` where needed + `INDEX`.
* **Normalization**: `trim + lowercase` for email, `digits-only` for phone, NFKC for name; reject non-canonical whitespace (existing `BlindIndexServiceTest`).
* **Key versioning**: `payu.security.blind-index-key` + `payu.security.blind-index-key-version` + `payu.security.blind-index-previous-keys` (comma-separated). Lookup tries `current` + `previous` keys during rotation (`lookupSupportsPreviousKeyDuringRotation`), write uses current version.
* **Query**: `findByEmailUsesHash` → `WHERE email_bidx = :bidx AND tenant_id = :tenant` then decrypt + constant-time compare to resist collision forgery. Never `LIKE` on plaintext.

### 4. Key Lifecycle (NIST 800-57)

* **Generation**: CSPRNG `SecureRandom` / `secrets` (Python); `DEK` never derived from password in prod.
* **Storage**: `KEK` in Vault HSM/KMS, `DEK` wrapped; separation of keys and data (keys on Vault, data in DB — OWASP). No hardcode, no `env` dump, no git.
* **Rotation triggers**: fixed cryptoperiod (`KEK` 90d, `blind-index-key` 180d), suspected compromise, staff exit with access, algorithm change.
* **Rotation MOP** (zero-downtime):
  1. Add new key version to `previous-keys`, deploy.
  2. Batch backfill (`KycPiiBackfillBatch`, `claimsRowsWithSkipLockedAndWritesCurrentKeyVersion`) re-encrypts with new `DEK`/`bidx` using `SELECT ... FOR UPDATE SKIP LOCKED` 500 rows/batch, writes `key_version` per row.
  3. Dual-read (`current` + `previous`) during backfill.
  4. Drop previous version after `30d` + backup retention + audit.
* **Process**: documented in `docs/security/KEY_MANAGEMENT_RUNBOOK.md`, tested quarterly; old keys retained `90d` for backup decrypt.

### 5. Masking & Audit (defense in depth)

* Logs/traces: `DataMaskingAspect` + `LogbackMaskingFilter` + OTel `SpanProcessor` mask `nik/phone/email` (e.g. `nik 3201********0001`) per ADR-0034; never log `*_bidx` or `DEK`.
* DB audit: `AuditAspect` + outbox `payu.security.pii-accessed.v1` for SIEM (Wazuh) + `docs/security/PENTEST_SCHEDULE.md`.

### 6. When NOT to use

* Non-PII: use RLS + `pgcrypto` `pgp_sym_encrypt` only for legacy batch, not app path.
* Large blobs: store in `object storage` with SSE-KMS, DB holds `ref`.

## Rationale

OWASP mandates AES-256 + GCM/CCM + random IV + CSPRNG, envelope `DEK/KEK`, separation of keys and data, and formal rotation. HMAC blind index is the recommended searchable pattern (deterministic AES leaks frequency). Existing `security-starter` already implements this correctly; ADR-0040 governs inventory + lifecycle so `PARTNER-PROD-002` + UU PDP pass consistently across Java + Python.

## Consequences

**Positive**:
* PII confidential even on DB dump; search remains indexed.
* Rotation without downtime via versioned HMAC + `SKIP LOCKED` batch.
* Single audit inventory for PDP.

**Negative**:
* Extra `*_bidx` column + index per searchable field — mitigasi partial index where `tenant_id`.
* Batch re-encryption window — mitigasi throttled batch + `ShedLock` (ADR-0042).

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Inventory | `docs/security/PII_INVENTORY.md` |
| 2 | Converter | `backend/shared/security-starter/src/main/java/id/payu/security/converter/EncryptedStringConverter.java` (AAD `tenant|field|version`) |
| 3 | Blind index | `backend/shared/security-starter/src/main/java/id/payu/security/crypto/BlindIndexService.java` (HMAC-SHA256, version) |
| 4 | JPA entities | `UserEntity`, `PartnerEntity`, `KycReviewEntity`, `ProfileEntity` (`*_bidx` + `@Sensitive`) |
| 5 | Python | `backend/shared/python-starter/src/payustarter/crypto/encrypted.py` (AES-GCM + HMAC) |
| 6 | Vault | `infrastructure/platform/vault/keys/payu-kek.hcl` + ESO `ExternalSecret` `ENCRYPTION_KEY`, `BLIND_INDEX_KEY` |
| 7 | Batch | `backend/backoffice-service/.../KycPiiBackfillBatch.java` (SKIP LOCKED) |
| 8 | Runbook | `docs/security/KEY_MANAGEMENT_RUNBOOK.md` |
| 9 | Tests | `BlindIndexServiceTest`, `KycEncryptionMappingTest`, `KycPiiBackfillBatchTest` + Python `test_encrypted.py` |

**Verification**:
* `SELECT email_bidx FROM users WHERE email_bidx = :hmac` → 1 row; `SELECT email FROM users` is ciphertext.
* Rotate `blind-index-key` → old `bidx` readable via `previous-keys` until backfill completes; `SELECT * WHERE key_version != current` → 0 post-batch.
* `grep -r "nik" logs/` → masked; SIEM `pii-accessed` arrives.

---
*Created for PARTNER-PROD-002, ADR-0040 — implementasi wajib refer ADR ini.*
