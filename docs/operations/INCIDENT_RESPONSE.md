# Incident Severity & Escalation Framework

> INFRA-020 / READY-051 — PayU Digital Banking Platform

## Severity Levels

| Level | Name | Description | Response Time | Resolution Time |
|:---:|:---|:---|:---:|:---:|
| **P1** | Critical | Platform-wide outage. Payment processing stopped. Data breach in progress. Regulatory SLA breach. | **15 min** | **4 hours** |
| **P2** | Major | Single service degraded (not down). Payment delays > 5 min. Key partner integration down. Audit trail gap. Rate limit exceeded for legitimate traffic. | **30 min** | **8 hours** |
| **P3** | Minor | Non-production alert. Cosmetic UI bug. Single user affected. Scheduled maintenance window exceeded. Deprecated API warning. | **4 hours** | **Next business day** |
| **P4** | Trivial | Documentation gap. Non-critical dependency deprecation. Test flake. Optional feature request. | **1 week** | **Next sprint** |

## Escalation Path

```
P1 → Alert fires → On-call SRE acknowledges (15 min)
    → If no ack in 15 min → PagerDuty escalates to Engineering Lead
    → If no ack in 30 min → Escalates to CTO
    → Postmortem required within 24 hours

P2 → Alert fires → On-call SRE acknowledges (30 min)
    → If no ack in 30 min → Escalates to Service Owner
    → Postmortem required within 48 hours

P3 → Ticket created in backlog → Service Owner triages
    → Fixed in next sprint or patch release

P4 → Ticket created → Backlog grooming → Scheduled when capacity permits
```

## P1 Triggers (Auto-Escalate)

1. **Payment processing down**: `payment_gateway_error_rate > 5%` for > 2 minutes
2. **Database connectivity loss**: `db_connection_failures > 0` across any service
3. **Kafka broker down**: All 3 brokers unreachable for > 1 minute
4. **Security incident**: Wazuh critical alert, Vault unsealed, unauthorized access detected
5. **Regulatory SLA breach**: SNAP-BI transaction > 10s p99 for > 5 minutes
6. **Data integrity**: Ledger imbalance detected by reconciliation job

## P2 Triggers (Escalate to Service Owner)

1. **Single service degraded**: Health check failing but service still serving
2. **Payment delays**: Transaction processing > 5 min end-to-end
3. **Partner integration down**: TokoBapak/Nobar API unreachable
4. **DLQ growth**: Dead letter queue > 100 messages
5. **Certificate expiry**: < 7 days remaining on any production certificate
6. **Disk usage**: > 80% on any production PV

## Communication Channels

| Channel | Use Case |
|:---|:---|
| **PagerDuty** | P1/P2 on-call alerting |
| **Slack #payu-incidents** | Incident coordination, status updates |
| **Slack #payu-eng** | P3/P4 announcements, deployment notifications |
| **Email (stakeholders)** | Postmortem distribution, scheduled maintenance |
| **Status Page** | Public-facing service status for partners |

## Postmortem Template

Every P1 incident requires a postmortem within 24 hours:

1. **Summary**: What happened, duration, impact
2. **Timeline**: Detection → escalation → mitigation → resolution
3. **Root Cause**: 5 Whys analysis
4. **Impact**: Customers affected, transactions lost, regulatory impact
5. **Resolution**: Steps taken to restore service
6. **Prevention**: Action items to prevent recurrence (JIRA tickets)
7. **Lessons**: Added to `docs/guides/LESSONS.md`

## On-Call Rotation

- **Primary**: SRE team (weekly rotation)
- **Secondary**: Service owner (account-service → wallet-service → transaction-service → gateway-service, weekly rotation)
- **Escalation**: Engineering Lead → CTO

> ⚠️ **Action Required**: Configure PagerDuty/Opsgenie service + rotation schedule. Current state: document only — no alerting integration active (OCP destroyed).
