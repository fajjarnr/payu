---
description: Workflow untuk melakukan audit keamanan pada service PayU sesuai standar PCI-DSS dan OJK. Updated with 
---

# Security Audit Workflow

Gunakan workflow ini sebelum release fitur yang menyentuh data sensitif (PII, finansial).

## 🚨 P19 Known Security Issues (Feb 2026)

**BEFORE running this workflow, check if the target service has known P0/P1 issues:**
- Read `.agents/context/ROADMAP.md` for current status
- Known P0-SEC-001: JWT in localStorage (frontend)
- Known P0-SEC-002: Hardcoded credentials
- Known P1-ARCH-001: 3 Quarkus services without JWT auth
- Known P1-ARCH-002: cms-service, ab-testing-service, statement-service without security-starter

## Scope Definition

1. **Identify Audit Target**
   - Service name(s)
   - Components affected (API, DB, Message Queue)
   - Data sensitivity level (PII, Financial, Public)

2. **Load Security Context**
   - Baca `@cybersecurity-architect` skill untuk checklist PCI-DSS.
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
   - Gunakan `@cybersecurity-architect` untuk scan OWASP Top 10.

2. **Audit Log Verification**
   - [ ] Semua akses ke data sensitif tercatat di audit log?

## Phase 4: Sign-off

1. **Create Security Attestation**
   - Tulis hasil audit ke `docs/security/audits/<service>-<date>.md`.

2. **Update Compliance Tracker**
   - Mark service as "Audited" in compliance dashboard.

---

_Last Updated: January 2026_
