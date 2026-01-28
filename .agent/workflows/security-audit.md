---
description: Workflow untuk melakukan audit keamanan pada service PayU sesuai standar PCI-DSS dan OJK.
---

# Security Audit Workflow

Gunakan workflow ini sebelum release fitur yang menyentuh data sensitif (PII, finansial).

## Scope Definition

1. **Identify Audit Target**
   - Service name(s)
   - Components affected (API, DB, Message Queue)
   - Data sensitivity level (PII, Financial, Public)

2. **Load Security Context**
   - Baca `@security-engineer` skill untuk checklist PCI-DSS.
   - Review `docs/security/SECURITY_POLICY.md`.

## Phase 1: Static Analysis

1. **Code Review for PII Handling**
   - [ ] Semua field sensitif menggunakan `@Sensitive` annotation?
   - [ ] Tidak ada PII yang di-log tanpa masking?
   - [ ] Encryption at rest menggunakan AES-256-GCM?

2. **Dependency Check**
   ```bash
   mvn dependency-check:check
   ```

   - [ ] Tidak ada CVE Critical/High?

## Phase 2: Configuration Audit

1. **Secrets Management**
   - [ ] Tidak ada password/key di `application.yml`?
   - [ ] Semua secrets menggunakan Vault reference atau env placeholder?

2. **Authentication & Authorization**
   - [ ] Endpoint sensitif dilindungi oleh `@PreAuthorize`?
   - [ ] Rate limiting aktif di API Gateway?
   - [ ] Idempotency key didukung untuk mutasi kritis?

## Phase 3: Runtime Verification

1. **Penetration Test (Optional)**
   - Gunakan `@security-engineer` untuk scan OWASP Top 10.

2. **Audit Log Verification**
   - [ ] Semua akses ke data sensitif tercatat di audit log?

## Phase 4: Sign-off

1. **Create Security Attestation**
   - Tulis hasil audit ke `docs/security/audits/<service>-<date>.md`.

2. **Update Compliance Tracker**
   - Mark service as "Audited" in compliance dashboard.

---

_Last Updated: January 2026_
