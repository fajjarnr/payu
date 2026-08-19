# ADR-0035: Dual-Control (Maker-Checker) Partner Onboarding, SLA & Runbook

**Status**: Accepted  
**Date**: 2026-08-19  
**Deciders**: Principal Architect, Platform Lead, Core Banking Lead, Cybersecurity Architect, Integration Architect  
**Relates to**: PARTNER-PROD-011, PARTNER-PROD-002/003/006, ADR-0033 (RLS), ADR-0034 (SLO), ADR-0025 (SNAP-BI)  

---

## Context

`partner-service` adalah trust boundary untuk integrator eksternal (TokoBapak, Nobar, Dolan) via SNAP-BI. Temuan codegraph 2026-08-19:

* `partner/domain/PartnerStatus.java:6` hanya `PENDING_VERIFICATION|ACTIVE|SUSPENDED|TERMINATED` — tidak ada `PENDING_APPROVAL`/`REJECTED`, tidak ada kolom `maker_id/checker_id`.
* `partner/application/service/PartnerService.java:46` `createPartner()` langsung `active=true` + `save()` — **auto-active tanpa 4-eyes**.
* `partner/adapter/persistence/entity/PartnerEntity.java:123` `isActive()=active||status==ACTIVE` — status `PENDING_VERIFICATION` tetap aktif.
* `partner/adapter/web/PartnerController.java:192` single `hasRole('ADMIN')` — maker bisa self-approve.

Ini melanggar:

* **POJK 38/POJK.03/2016 & POJK 12/2021** — Separation of Duties & 4-eyes untuk entitas finansial.
* **PCI-DSS v4 Req 7.1/7.2, ISO27001 A.5.3/A.5.18** — least privilege + segregation.
* **UU PDP** — audit trail immutable untuk keputusan akses pihak ketiga.
* **BI SNAP-BI** — onboarding partner wajib verifikasi dokumen + uji sandbox sebelum `ACTIVE`.

Gate `PARTNER-PROD-011` mensyaratkan dual-control + SLA/eskalasi + runbook + on-call sebelum `partner-service` Production Ready. Tanpa ini, risk: insider unilateral provisioning, webhook hijack, dan breach SLA tanpa deteksi.

## Decision Drivers

* **4-eyes wajib** untuk semua provision partner production — zero self-approval, DB-enforced.
* **Minimal diff, reuse tabel** — hindari tabel request terpisah (YAGNI) sampai ada kebutuhan multi-resubmit history.
* **Least privilege Keycloak** — `PARTNER_MAKER` ≠ `PARTNER_CHECKER`.
* **SLA terukur** — SLI `time_to_decision`, SLO `p95<4j` jam kerja / `p99<24j` kalender, reuse framework ADR-0034.
* **Audit + outbox** — setiap keputusan `Audited` + `outbox-starter` CloudEvents `payu.partner.*.v1`.
* **Runbook 1 halaman + on-call reuse** — tidak buat rotasi baru, escalate via Telegram dulu.

## Considered Options

### Option 1 — Reuse `partners` + kolom maker/checker (dipilih)

* Tambah `PENDING_APPROVAL`, `REJECTED` ke `PartnerStatus`; kolom `maker_id, checker_id, requested_at, decided_at, rejection_reason` di `partners`; `CHECK (maker_id<>checker_id)`; index `status+requested_at`.
* **Pros**: 1 Flyway (V19), 0 tabel baru, query `GET /v1/partners?status=PENDING_APPROVAL` gratis, `@Version` sudah ada, seeder sandbox bypass via `type=INTERNAL`.
* **Cons**: history resubmit hanya terakhir (audit log cover).

### Option 2 — Tabel terpisah `partner_onboarding_requests`

* Immutable ledger, `partners` hanya terisi setelah APPROVED.
* **Pros**: history lengkap, no UPDATE finansial.
* **Cons**: JOIN + 2 sumber kebenaran, migrasi lebih besar, overkill untuk volume partner PayU (<50). Ditunda sampai >20 partner/bulan.

### Option 3 — Tetap `ADMIN` single role + app check `maker!=checker`

* **Pros**: tanpa role baru.
* **Cons**: melanggar least privilege, audit tidak bisa bedakan intent, Keycloak policy tidak eksplisit — ditolak.

## Decision

Adopsi **Option 1 — Dual-Control reuse `partners`** dengan state machine, RBAC, SLA, dan runbook berikut.

### 1. State Machine

```
PENDING_APPROVAL --(checker approve)--> ACTIVE
        |                                | \
        +--(checker reject)--> REJECTED   |  +--(checker)--> SUSPENDED --> TERMINATED
        |                      |           |  +--(checker)--> TERMINATED
        +--(maker resubmit)----+           +--(maker resubmit dari REJECTED via POST /{id}/resubmit)
```

* `createPartner()` oleh `PARTNER_MAKER` → `PENDING_APPROVAL` (`maker_id=requester`, `requested_at=now()`), **bukan** `ACTIVE`.
* `POST /v1/partners/{id}/approve` oleh `PARTNER_CHECKER` (`maker_id != checker_id`) → `ACTIVE` (`checker_id`, `decided_at`).
* `POST /v1/partners/{id}/reject` oleh `PARTNER_CHECKER` → `REJECTED` + `rejection_reason` wajib.
* `POST /v1/partners/{id}/resubmit` oleh `PARTNER_MAKER` (owner maker) → `PENDING_APPROVAL` (reset `checker_id/decided_at`, bump `@Version`).
* `DELETE` hanya `REJECTED` oleh maker.
* `PENDING_APPROVAL` auto-`EXPIRED` (opsional) via scheduler `>7 hari` → ditunda iterasi 2.
* Mutasi sensitif `webhookUrl/publicKey/keys/regenerate/SUSPEND/TERMINATE` ikut 4-eyes di iterasi 2 (dicatat, belum di-enforce).

### 2. RBAC (Keycloak)

| Role | Boleh |
|---|---|
| `PARTNER_MAKER` | `POST /v1/partners`, `POST /{id}/resubmit`, `GET /me` (own) |
| `PARTNER_CHECKER` | `GET /v1/partners?status=PENDING_APPROVAL`, `POST /{id}/approve|reject`, `GET /v1/partners` |
| `PARTNER_VIEWER` | `GET` read-only |
| `PARTNER_ADMIN` | break-glass `approve` dengan header `X-Justification` + `@Audited(level=WARN)` |

Enforce di `@PreAuthorize` + service guard `makerId != checkerId` + DB CHECK.

### 3. Persistensi (Flyway V19)

```sql
ALTER TABLE partners ADD COLUMN maker_id VARCHAR(64);
ALTER TABLE partners ADD COLUMN checker_id VARCHAR(64);
ALTER TABLE partners ADD COLUMN requested_at TIMESTAMPTZ;
ALTER TABLE partners ADD COLUMN decided_at TIMESTAMPTZ;
ALTER TABLE partners ADD COLUMN rejection_reason VARCHAR(512);
ALTER TABLE partners ADD CONSTRAINT chk_maker_checker CHECK (maker_id IS NULL OR checker_id IS NULL OR maker_id <> checker_id);
CREATE INDEX idx_partners_status_requested ON partners(status, requested_at) WHERE status='PENDING_APPROVAL';
UPDATE partners SET status='PENDING_APPROVAL' WHERE status='PENDING_VERIFICATION';
```

`PartnerStatus.java` → `PENDING_APPROVAL, ACTIVE, REJECTED, SUSPENDED, TERMINATED` (`PENDING_VERIFICATION` deprecated). `type` jadi enum top-level `PartnerType` (`SNAP_BI, VIRTUAL_ACCOUNT, DISBURSEMENT, QRIS, INTERNAL, SANDBOX`).

Bypass: `type=INTERNAL/SANDBOX` skip dual-control (seeder & test).

### 4. API

```
POST   /v1/partners                  @PreAuthorize("hasRole('PARTNER_MAKER')")
GET    /v1/partners?status=PENDING_APPROVAL @PreAuthorize("hasAnyRole('PARTNER_MAKER','PARTNER_CHECKER')")
POST   /v1/partners/{id}/approve    @PreAuthorize("hasRole('PARTNER_CHECKER')") @Audited
POST   /v1/partners/{id}/reject     @PreAuthorize("hasRole('PARTNER_CHECKER')") @Audited
POST   /v1/partners/{id}/resubmit   @PreAuthorize("hasRole('PARTNER_MAKER')") @Audited
```

Optimistic lock `@Version` → `409 CONFLICT`; `404` jika bukan `PENDING_APPROVAL`; `403` jika self-approve; RFC 9457 error `PARTNER_FORBIDDEN_SELF_APPROVAL`, `PARTNER_CONFLICT_STATUS`.

### 5. Audit & Events

* `@Audited` untuk `CREATE/APPROVE/REJECT/RESUBMIT` (level INFO, APPROVE/REJECT WARN) → `audit_log`.
* Outbox via `outbox-starter`: `payu.partner.onboarding-requested.v1`, `payu.partner.approved.v1`, `payu.partner.rejected.v1` (CloudEvents 1.0.2, topic `payu.partner.*.v1`, DLQ `.dlq`).
* RLS: `tenant_id` tetap via `TenantAware` (ADR-0033).

### 6. SLA / SLO & Eskalasi (reuse ADR-0034)

* **SLI**: `time_to_decision = decided_at - requested_at`.
* **SLO**: `p95 <4 jam` (jam kerja WIB 08–17, Sen–Jum), `p99 <24 jam` kalender. **SLA kontrak**: `1×24 jam kerja`.
* **Eskalasi**: `T+4j` → Telegram `payu-partner-checkers` + metric `partner_onboarding_age_hours`; `T+24j` → page on-call (Wazuh/PagerDuty) + `PARTNER_SLA_001` alert. Scheduler `PartnerSlaScheduler` (`@Scheduled every=15m`) dengan `ShedLock` (hindari `GW-CONCUR-001`).
* **Dashboard**: Grafana `Partner Onboarding SLO` (gauge pending, histogram age, breach total).

### 7. Runbook & On-Call

* Runbook: `docs/operations/PARTNER_ONBOARDING_RUNBOOK.md` (1 hal) + link `INCIDENT_RESPONSE.md`.
* On-call: reuse `partner-service` owner; Checker pool 2 orang (primary/secondary) dari platform team; iterasi 1 via Telegram, PagerDuty jika breach >2×/bulan.
* Break-glass: `PARTNER_ADMIN` + `X-Justification` header, log WARN, review mingguan.

## Rationale

Reuse tabel = Flyway terkecil yang lolos audit SoD; 2 role eksplisit = least privilege + audit jelas; SLA 4j/24j = selaras OJK 1×24j & Midtrans 1–2 hari; Telegram dulu = zero-cost, PagerDuty on-demand (ponytail ultra). Ditolak: tabel terpisah (YAGNI volume rendah) & single ADMIN (violasi PCI).

## Consequences

**Positive**:
* 4-eyes DB-enforced, no unilateral provisioning.
* Audit lengkap + outbox untuk downstream (webhook, billing).
* SLA terukur & alert sebelum breach.
* Sandbox/seeder tidak terblok.

**Negative**:
* History resubmit hanya last state — mitigasi via `audit_log` + event.
* Perlu migrasi `PENDING_VERIFICATION` → `PENDING_APPROVAL` (one-time).
* Butuh 2 Keycloak roles baru + update realm export.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Enum + Entity | `partner/domain/PartnerStatus.java`, `PartnerType.java` (top-level), `adapter/persistence/entity/PartnerEntity.java` |
| 2 | Flyway V19 | `partner-service/src/main/resources/db/migration/V19__dual_control_maker_checker.sql` |
| 3 | Service | `partner/application/service/PartnerService.java` (maker/checker guards) |
| 4 | Controller | `partner/adapter/web/PartnerController.java` (`/approve` `/reject` `/resubmit`) |
| 5 | DTO | `partner/interfaces/dto/PartnerDTO.java` + `RejectionReason` |
| 6 | SLA scheduler | `partner/application/scheduler/PartnerSlaScheduler.java` + ShedLock |
| 7 | Metrics | `partner/infrastructure/metrics/PartnerOnboardingMetrics.java` |
| 8 | Runbook | `docs/operations/PARTNER_ONBOARDING_RUNBOOK.md` |
| 9 | Realm | `payu-realm.json` (`PARTNER_MAKER`, `PARTNER_CHECKER`, `PARTNER_VIEWER`) |
| 10 | Tests | `PartnerServiceTest`, `PartnerControllerTest`, `PartnerSlaSchedulerTest` (TDD, ArchUnit) |

**Verification**:
* `PartnerServiceTest`: self-approve 403, approve→ACTIVE, reject→REJECTED, resubmit→PENDING_APPROVAL, optimistic lock 409, INTERNAL bypass.
* `PartnerControllerTest`: RBAC 403 per role, RFC 9457 errors.
* Manual: `curl` maker create → checker approve, metric `partner_onboarding_age_hours` di Prometheus, Telegram T+4j, trace `traceparent` di Tempo.

---
*Created for PARTNER-PROD-011 — implementasi wajib refer ADR ini.*
