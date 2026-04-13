# Disaster Recovery Procedures

## Overview

Prosedur pemulihan bencana untuk PayU Platform dengan target:
- **RTO** (Recovery Time Objective): < 4 hours
- **RPO** (Recovery Point Objective): < 15 minutes

---

## 🏗️ DR Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      ACTIVE-PASSIVE DR                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   PRIMARY (Jakarta)              SECONDARY (Singapore)         │
│   ┌─────────────────┐           ┌─────────────────┐           │
│   │ OpenShift 4.20  │           │ OpenShift 4.20  │           │
│   │ (Active)        │──WAL──────│ (Standby)       │           │
│   └────────┬────────┘   Async   └────────┬────────┘           │
│            │                             │                     │
│   ┌────────▼────────┐           ┌────────▼────────┐           │
│   │ PostgreSQL      │           │ PostgreSQL      │           │
│   │ (Primary)       │──────────▶│ (Replica)       │           │
│   └─────────────────┘  Streaming└─────────────────┘           │
│                        Replication                             │
│   ┌─────────────────┐           ┌─────────────────┐           │
│   │ Kafka (Active)  │──────────▶│ Kafka (Mirror)  │           │
│   └─────────────────┘  MirrorMaker└────────────────┘          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📋 DR Procedure: Full Site Failover

### Pre-Conditions
- [ ] Primary site confirmed unreachable for > 30 minutes
- [ ] CTO/VP Engineering approval obtained
- [ ] DR team assembled (min 3 people)

### Phase 1: Assessment (15 min)

```bash
# 1. Verify primary site status
curl -s https://api.payu.fajjjar.my.id/health || echo "PRIMARY DOWN"

# 2. Check replication lag
psql -h dr-postgres.payu-dr.svc -c "SELECT pg_last_wal_receive_lsn() - pg_last_wal_replay_lsn() AS lag_bytes;"

# 3. Check Kafka mirror lag
kafka-consumer-groups.sh --bootstrap-server dr-kafka:9092 \
  --group mirror-maker --describe
```

### Phase 2: Failover Execution (30 min)

```bash
# 1. Promote PostgreSQL replica to primary
psql -h dr-postgres -c "SELECT pg_promote();"

# 2. Update DNS to point to DR site
aws route53 change-resource-record-sets \
  --hosted-zone-id Z123456 \
  --change-batch file://dr-dns-failover.json

# 3. Scale up DR workloads
oc scale deployment --all --replicas=3 -n payu

# 4. Verify services healthy
for svc in account wallet transaction; do
  curl -s https://api-dr.payu.fajjjar.my.id/$svc/health
done
```

### Phase 3: Validation (30 min)

```bash
# 1. Run smoke tests
./scripts/dr-smoke-tests.sh

# 2. Verify critical flows
curl -X POST https://api-dr.payu.fajjjar.my.id/v1/transfers/validate \
  -H "X-Test-Mode: true" \
  -d '{"amount": 10000}'

# 3. Check monitoring
open https://grafana-dr.payu.fajjjar.my.id/d/payu-overview
```

### Phase 4: Communication

- [ ] Status page updated: status.payu.fajjjar.my.id
- [ ] Customer notification sent
- [ ] Regulator (OJK) notified if outage > 2 hours
- [ ] Internal Slack announcement

---

## 📋 DR Procedure: Database Recovery

### Scenario: Primary PostgreSQL Corrupted

```bash
# 1. Stop writes to corrupted primary
oc scale deployment/wallet-service --replicas=0

# 2. Identify last good backup
aws s3 ls s3://payu-backups/postgres/ | tail -10

# 3. Restore from backup
./scripts/restore-postgres.sh \
  --backup-id backup-2026-01-30-0600 \
  --target-time "2026-01-30 05:55:00"

# 4. Verify data integrity
psql -c "SELECT COUNT(*) FROM transactions WHERE created_at > '2026-01-30';"

# 5. Resume services
oc scale deployment/wallet-service --replicas=3
```

---

## 📋 DR Procedure: Kafka Recovery

### Scenario: Kafka Cluster Failure

```bash
# 1. Check cluster status
oc get pods -l app=kafka

# 2. If total loss, restore from S3 backup
./scripts/restore-kafka-topics.sh --date 2026-01-30

# 3. Reset consumer offsets
kafka-consumer-groups.sh --bootstrap-server kafka:9092 \
  --group wallet-service-group \
  --reset-offsets --to-datetime 2026-01-30T05:00:00.000 \
  --execute

# 4. Verify topic integrity
kafka-topics.sh --describe --topic transactions
```

---

## 🧪 DR Drill Schedule

| Drill Type | Frequency | Duration | Last Run |
|------------|-----------|----------|----------|
| Tabletop Exercise | Monthly | 2 hours | 2026-01-15 |
| Database Failover | Quarterly | 4 hours | 2026-01-10 |
| Full Site Failover | Annually | 8 hours | 2025-11-20 |
| Chaos Engineering | Weekly | 1 hour | Continuous |

---

## 📊 Recovery Metrics

| Metric | Target | Last Drill |
|--------|--------|------------|
| RTO (Recovery Time) | < 4 hours | 2.5 hours |
| RPO (Data Loss) | < 15 minutes | 8 minutes |
| MTTR (Mean Time to Recovery) | < 1 hour | 45 minutes |
| Failover Success Rate | 100% | 100% |

---

## 📞 DR Contacts

| Role | Primary | Secondary |
|------|---------|-----------|
| DR Coordinator | Platform Lead | SRE Lead |
| Database Admin | DBA Team | On-call DBA |
| Network Admin | NetOps Team | Cloud Provider |
| Executive Sponsor | CTO | VP Engineering |
