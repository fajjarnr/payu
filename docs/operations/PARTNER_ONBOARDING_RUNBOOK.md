# Partner Onboarding Runbook — ADR-0035 (PARTNER-PROD-011)

**Service**: `partner-service` | **On-call**: reuse `partner-service` owner (checker pool 2 orang primary/secondary, platform team) | **Links**: [INCIDENT_RESPONSE.md](./INCIDENT_RESPONSE.md) · [ADR-0035](../adr/0035-dual-control-partner-onboarding-and-sla-runbook.md)

## 1. Overview

Dual-control (maker-checker) untuk provision partner production. `POST /v1/partners` oleh `PARTNER_MAKER` → `PENDING_APPROVAL`; `POST /v1/partners/{id}/approve|reject` oleh `PARTNER_CHECKER` (`maker≠checker` DB-enforced `chk_maker_checker`). `INTERNAL`/`SANDBOX` bypass untuk seeder/test. Break-glass `PARTNER_ADMIN` + `X-Justification` header.

## 2. SLO / SLA & SLI

| Metric | Target | Window |
|---|---|---|
| **SLI** `time_to_decision = decided_at - requested_at` | — | per request |
| **SLO p95** | `<4 jam` **jam kerja** WIB 08–17 Sen–Jum | rolling 28d |
| **SLO p99** | `<24 jam` **kalender** | rolling 28d |
| **SLA kontrak** | `1×24 jam kerja` | per request |
| **Escalation T+4j** | Telegram `payu-partner-checkers` + `partner_onboarding_age_hours` gauge | scheduler every 15m |
| **Escalation T+24j** | Page on-call (Wazuh/PagerDuty) + alert `PARTNER_SLA_001` | same scheduler |

Dashboard Grafana: `Partner Onboarding SLO` (gauge pending, histogram age, breach total). Metrics: `payu.partner.onboarding.escalation.telegram`, `.page`, `.sla_breaches`, `partner_onboarding_age_hours`, `payu.partner.onboarding.time_to_decision`.

## 3. State Machine

```
PENDING_APPROVAL --(checker approve)--> ACTIVE --(checker)--> SUSPENDED --> TERMINATED
        |              \                | \--(checker)--> TERMINATED
        +--(checker reject)--> REJECTED |
        |                      |         |
        +--(maker resubmit)----+         |
DELETE only REJECTED (maker). EXPIRED >7d ditunda iterasi 2.
```

Optimistic lock `@Version` → `409 CONFLICT`; bukan `PENDING_APPROVAL` → `409 PARTNER_CONFLICT_STATUS`; self-approve → `403 PARTNER_FORBIDDEN_SELF_APPROVAL` (RFC 9457).

## 4. API & RBAC (Keycloak)

| Role | Allowed |
|---|---|
| `PARTNER_MAKER` | `POST /v1/partners`, `POST /{id}/resubmit`, `GET /me` |
| `PARTNER_CHECKER` | `GET /v1/partners?status=PENDING_APPROVAL`, `POST /{id}/approve|reject`, `GET /v1/partners` |
| `PARTNER_VIEWER` | `GET` read-only |
| `PARTNER_ADMIN` | break-glass `approve` + `X-Justification` + `@Audited(WARN)` |

## 5. Audit & Events

- `@Audited` `CREATE/APPROVE/REJECT/RESUBMIT` → `audit_log`.
- Outbox (`outbox-starter`, CloudEvents 1.0.2): `payu.partner.onboarding-requested.v1`, `payu.partner.approved.v1`, `payu.partner.rejected.v1` (DLQ `.dlq`). SLA escalations: `payu.partner.sla-telegram.v1` / `payu.partner.sla-page.v1`.

## 6. Scheduler

`PartnerSlaScheduler` `@Scheduled(fixedDelay=15m)` + `@SchedulerLock(name="PartnerSlaScheduler_checkSla")` (ShedLock). Queries `findByStatus(PENDING_APPROVAL)` + `requested_at`. `T+4j` → outbox `sla-telegram`, `T+24j` → `sla-page` + `PARTNER_SLA_001` log.

## 7. Runbook Playbook

### Pending brews >4h (Telegram alert)
1. `GET /v1/partners?status=PENDING_APPROVAL` — list pending.
2. Verify `requested_at`, `maker_id` — ping maker for docs.
3. Checker reviews sandbox test + docs → `POST /{id}/approve` or `reject { "rejection_reason": "..." }`.
4. Confirm Grafana gauge drops; audit_log entry exists.

### Pending >24h (Page, SLA breach)
1. Acknowledge PagerDuty/Wazuh `PARTNER_SLA_001`.
2. Same triage as above, escalate to secondary checker if primary unavailable.
3. If `PARTNER_ADMIN` break-glass needed: `curl -H "X-Justification: <ticket>" POST /{id}/approve` — review weekly.
4. Post-incident: record `time_to_decision` histogram, file P1/P2 per [INCIDENT_RESPONSE.md](./INCIDENT_RESPONSE.md).

### Maker cannot self-approve (403)
- Expected. Reassign to different checker. If only one checker on-call, wait for secondary or use `PARTNER_ADMIN` break-glass with justification.

### 409 Conflict (optimistic lock / wrong status)
- Re-fetch `GET /{id}`, verify `status` and `version`, retry. If `REJECTED`, maker must `POST /{id}/resubmit` first.

### DB `chk_maker_checker` violation
- Should never happen if service guard works — indicates bypass of service layer. Audit: check `partners` row `maker_id==checker_id`; revert + alert.

### Resubmit flow
- Maker `POST /{id}/resubmit` → resets to `PENDING_APPROVAL`, clears `checker_id/decided_at`, new `requested_at`.

## 8. On-Call

- Reuse `partner-service` owner rotation (no new rotation).
- Checker pool: 2 orang platform team (primary/secondary); iterasi 1 Telegram, PagerDuty jika breach >2×/bulan.
- Escalation path per [INCIDENT_RESPONSE.md](./INCIDENT_RESPONSE.md): P1 15m → Eng Lead → CTO.

## 9. Verification

```bash
# maker creates
curl -H "Authorization: Bearer $MAKER_JWT" -H "Content-Type: application/json" \
  -d '{"name":"TokoBapak","type":"SNAP_BI","email":"ops@tokobapak.id","phone":"+62123456789"}' \
  http://partner-service/v1/partners
# checker approves
curl -X POST -H "Authorization: Bearer $CHECKER_JWT" http://partner-service/v1/partners/{id}/approve
# metrics
curl http://partner-service/actuator/prometheus | grep partner_onboarding
```

## 10. Rollback / Migration

- Flyway `V21__dual_control_maker_checker.sql` is idempotent (`IF NOT EXISTS`, `DROP CONSTRAINT IF EXISTS`).
- `PENDING_VERIFICATION` migrated to `PENDING_APPROVAL` one-time; rollback requires manual `UPDATE partners SET status='PENDING_VERIFICATION' WHERE status='PENDING_APPROVAL'`.
