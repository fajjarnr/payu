# ADR-0061: Account Service — Lifecycle, Multi-Tenancy & PII Protection Standard

**Status**: Proposed  
**Date**: 2026-08-22  
**Deciders**: Core Banking Engineering, Platform Engineering, Risk & Compliance, DPO  
**Relates to**: ADR-0022 (Money & Idempotency), ADR-0033 (RLS), ADR-0040 (Field Encryption & Blind Index), ADR-0041 (Outbox), ADR-0049 (Wallet Ledger), PADG BI-FAST, UU PDP, PCI-DSS

---

## Context

`backend/account-service` (`AccountServiceApplication.java`, `domain/model/Account.java:37`, `adapter/persistence/entity/AccountEntity.java:18`, `adapter/persistence/entity/UserEntity.java`) adalah sumber kebenaran untuk **identity & account lifecycle** — bukan untuk saldo (saldo truth di `wallet-service` Ledger `ADR-0049`). Current code:

* Domain `Account` rich (credit/debit/freeze/close, `version` optimistic locking, `MINIMUM_SAVINGS_BALANCE=10000`, `MAXIMUM_BALANCE`) — sudah DDD, tapi `AccountEntity.balance DECIMAL(19,4)` baru di `V102` (`AUDIT-042`), `budgets` juga.
* `UserEntity` email/phone `AES-GCM` via `security-starter` `EncryptedStringConverter` + `V6 VARCHAR(512)` ciphertext-ready, plus `V105` blind index `email_hash/phone_number_hash HMAC-SHA256` `UNIQUE(tenant_id, hash)` — benar, tapi `pgcrypto` (`V--`) hanya enable, belum dipakai untuk NIK; key masih single per env (`SecurityAutoConfiguration.java:50` `ENCRYPTION_KEY` via Vault), belum per-tenant KMS.
* Multi-tenancy: `V15/V106` `tenant_id DEFAULT 'default'` + `@TenantAware` + `TenantEntityListener` + `TenantEnforcementAspect` + `V107` `FORCE RLS` **hanya** di `users` — `accounts`/`beneficiaries`/`budgets` belum `FORCE RLS`, jadi `WHERE tenant_id` miss = leak (Crassula 2026-04: shared schema tanpa RLS = regulatory incident).
* Audit: `AuditLogAspect` ada tapi outbox `payu.account.*.v1` belum konsisten untuk `account-created/pocket-created`; logs masih risk PII jika masking off.
* Industry: AWS CLM 2024 (PII transient, delete after engagement, Macie tagging, no PII in logs), Crassula multi-tenant (5 mechanisms: separate DB-per-tier, RLS, JWT tenant claim, per-tenant KMS/BYOK, audit tenant context), BBVA 6-layer (IAM least-privilege, KMS CMK, Lake Formation fine-grained), DORA Jan 2025 ICT-risk, GDPR art.5 data minimization.

## Decision Drivers

* **PDP/UU PDP + PCI-DSS**: NIK/email/phone tidak boleh plaintext di DB/logs; BYOK per-tenant untuk tenant regulasi (OJK).
* **RISK**: cross-tenant leak = sanctions + disclosure — RLS backstop wajib (Crassula pitfall #2).
* **Money safety**: `account-service.balance` hanya cached view — ledger truth di wallet, wajib reconcile vs ledger `SUM`.
* **Auditability**: `who/what/when/tenant` immutable, tanpa PII di log (mask `NIK`).
* **Zero surprise**: account status machine eksplisit, bukan string enum bebas.

## Considered Options

### Option A — Harden in-place: complete RLS + per-tenant KMS + blind index + outbox (chosen)

* **Pros**: reuse `security-starter` `BlindIndexService` + `EncryptionService` rotation (`previousKeys`), `V107` pola tinggal replikasi ke `accounts/beneficiaries/budgets`; cheapest, satisfies UU PDP + Crassula audit.
* **Cons**: ekstra migration `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` + policy per tabel; key rotation perlu re-encrypt batch.

### Option B — DB-per-tenant fisik

* **Pros**: isolation struktural, paling defensible di audit DORA.
* **Cons**: `N × DB` per tenant, Flyway `N` runs, cost tinggi untuk lab; tidak perlu untuk MVP mid-size (bridge approach cukup).

### Option C — Hapus cache `accounts.balance`, selalu query wallet-ledger

* **Pros**: single source of truth.
* **Cons**: read path chatty (gRPC per request), latency — cache `balance` dengan reconciler sudah trade-off diterima (lihat Consequences).

## Decision

**Option A — Harden in-place.**

1. **Status machine**: `AccountStatus ACTIVE/FROZEN/CLOSED/PENDING_VERIFICATION` (`domain/model/AccountStatus.java:3`) + `PocketStatus` (`wallet-service`) — `freeze()` hanya dari `ACTIVE`, `close()` hanya `balance==0` (`Pocket.java:65`), `ACTIVE` check di `credit/debit`. Tambah DB `CHECK`/`enum` top-level file per `AGENTS.md #8`.
2. **Money**: `accounts.balance` + `budgets.limit_amount/current_spent` tetap `DECIMAL(19,4)` `HALF_EVEN` (`V102`), `Money` dari `api-commons` single source; cached view reconciled nightly `SELECT SUM(ledger)` vs `accounts.balance` → alert drift (lihat ADR-0049).
3. **PII**: `users.email/phone/nik` → `EncryptedStringConverter` AES-256-GCM random IV (app-level) + `pgcrypto` `pgp_sym_encrypt` untuk kolom NIK baru (key di Vault `ENCRYPTION_KEY`); `V105` blind index `email_hash/phone_hash HMAC-SHA256` `UNIQUE(tenant_id, hash)` untuk lookup tanpa decrypt; key rotation via `BlindIndexService(key, version, previousKeys)` + `EncryptionService(password, previousKeys, salt)` per `ADR-0040` — re-index runner sebelum prod migration.
4. **Multi-tenancy**: `tenant_id` dari JWT `account_id` claim → `TenantEnforcementAspect` → `current_setting('app.tenant_id')` → DB `FORCE RLS` **semua** tabel core: `users` (sudah `V107`), `accounts`, `beneficiaries`, `budgets`, `sensitive_user_data`. Policy `USING ((tenant_id = current_setting('app.tenant_id', true)))` + `WITH CHECK` sama. Test auto: query tanpa `SET app.tenant_id` must return 0 rows.
5. **Outbox & events**: `payu.account.user-created.v1`, `payu.account.account-created.v1`, `payu.account.pocket-created.v1` via `outbox-starter` CloudEvents `payu.<domain>.<event>.v<n>` + `.dlq` (ADR-0041, ADR-0026); payload PII-minimized (hanya `userId`, `accountNumberHash`, bukan NIK plain).
6. **Audit & masking**: `@Audited` + `AuditLogPublisher` + `DataMaskingAspect` (`masking-enabled=true` required di prod `SecurityAutoConfiguration.java:157`), `nik`/`phone`/`email` never di log JSON (LokiStack), audit row include `tenant_id`, `userId`, `ip`, `action` immutable.
7. **Residency & KMS**: default shared-schema bridge (Crassula bridge) + per-tenant KMS CMK (AWS KMS `alias/payu/<tenant>`) untuk BYOK tenant tier-1; `SecurityProperties.encryption.password` via Vault `VSO/ESO` (ADR-0044), `Kiteworks` hybrid HSM pattern option untuk master key HSM FIPS 140-3.

## Rationale

* Crassula 2026 audit: `RLS is the safety net under shared schema; missing WHERE = breach` — `V107` sudah prove, tinggal perluasan.
* AWS CLM + BBVA: transient PII, no PII in logs, CMK per-tenant, least-privilege IAM — match UU PDP `data minimization`.
* Cost: bridge + RLS lebih murah dari DB-per-tenant untuk <100 tenant Indonesia, tetap lulus DORA evidence (RLS policy visible di `pg_policies`).

## Consequences

**Positive**:
* Cross-tenant leak structurally blocked (RLS `FORCE`), audit `pg_policies` defensible untuk OJK.
* Email/phone lookup tetap O(1) via blind index tanpa decrypt full table.
* Balance cached tetap cepat, drift detect nightly.

**Negative**:
* `FORCE RLS` perlu `SET app.tenant_id` tiap connection —漏 `TenantEntityListener` bug = semua query 0 rows (fail-closed, lebih aman daripada leak, tapi butuh e2e test).
* Key rotation re-encrypt = batch job + dual-read `previousKeys`.

## Implementation Notes

* Flyway `V108__force_rls_accounts_beneficiaries_budgets.sql` — copy `V107` pattern: `ENABLE ROW LEVEL SECURITY; FORCE; CREATE POLICY tenant_isolation_* ON accounts/beneficiaries/budgets USING ...`.
* `BlindIndexService` already `SecurityAutoConfiguration.java:109` — ensure `payu.security.blind-index-key` via Vault `ClusterSecretStore` + `key-version` bump on rotation.
* Contract test: `AccountServiceTest` must assert `findByEmail` goes via `email_hash` blind index, not decrypt scan.
* ArchUnit: `domain/model` must not import `jakarta.persistence`; `AccountStatus` top-level enum.

---
*Teams: core-banking-engineer + data-architect + cybersecurity-architect — references web 2026-08-22 (AWS CLM, Crassula, BBVA, Kiteworks BYOK, PCI-DSS) + CodeGraph `Account.java:37`, `AccountEntity.java:18`*
