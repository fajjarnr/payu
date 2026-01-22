# PayU Disaster Recovery Plan (DRP)

> Complete backup and restore procedures for PayU Digital Banking Platform

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Recovery Objectives](#recovery-objectives)
3. [Backup Strategy](#backup-strategy)
4. [Backup Procedures](#backup-procedures)
5. [Restore Procedures](#restore-procedures)
6. [Testing & Verification](#testing--verification)
7. [Incident Response](#incident-response)
8. [Contact Information](#contact-information)

---

## Executive Summary

This Disaster Recovery Plan (DRP) provides comprehensive procedures for backing up and restoring the PayU Digital Banking Platform. The plan ensures:

- **Data integrity** through regular, automated backups
- **Fast recovery** with defined RTO (15 minutes) and RPO (1 minute)
- **Verification** through regular testing of backup/restore operations
- **Compliance** with PCI DSS and ISO 27001 requirements

### Components Covered

| Component | Backup Method | Frequency | Retention |
|-----------|---------------|-----------|-----------|
| PostgreSQL (11 databases) | pg_dump + WAL archiving | Continuous + Daily | 30 days (7 years compliance) |
| Redis (Data Grid) | RDB snapshots | Hourly | 7 days |
| Kafka Topics | MirrorMaker to backup cluster | Continuous | 7 days |
| Configuration Files | Git versioning | On change | Forever |

---

## Recovery Objectives

### Primary Metrics

| Metric | Target | Lab Environment |
|--------|--------|-----------------|
| **RTO** (Recovery Time Objective) | < 15 minutes | < 30 minutes |
| **RPO** (Recovery Point Objective) | < 1 minute | < 5 minutes |

### Service-Level Recovery Priorities

| Priority | Service | RTO | RPO |
|----------|---------|-----|-----|
| 1 | PostgreSQL (Account, Transaction, Wallet) | 5 min | 1 min |
| 2 | Redis (Caching, Sessions) | 10 min | 5 min |
| 3 | Kafka (Event Stream) | 15 min | 1 min |
| 4 | Configuration (All services) | 5 min | 0 min |

---

## Backup Strategy

### PostgreSQL Backup Strategy

#### 1. Continuous WAL Archiving (Production)

PostgreSQL Write-Ahead Log (WAL) files are continuously archived to enable point-in-time recovery (PITR).

- **WAL Archive Location**: `/backups/postgres/wal/`
- **Archive Mode**: Enabled
- **Archive Command**: `wal-g wal-push %p`
- **Retention**: 7 days

#### 2. Daily Logical Backups

Full logical dumps of all databases using `pg_dump`.

- **Backup Time**: 02:00 UTC
- **Format**: Custom format (compressed)
- **Location**: `/backups/postgres/daily/`
- **Retention**: 30 days (7 years for compliance data)

#### 3. Weekly Full Physical Backups

Base backups for PITR.

- **Backup Time**: Sunday 02:00 UTC
- **Format**: PGDATA directory snapshot
- **Location**: `/backups/postgres/weekly/`
- **Retention**: 1 year

### Redis Backup Strategy

- **Method**: RDB snapshots
- **Frequency**: Hourly
- **Location**: `/backups/redis/snapshots/`
- **Retention**: 7 days
- **AOF**: Disabled (RDB preferred for speed)

### Kafka Backup Strategy

- **Method**: MirrorMaker to backup cluster
- **Topics**: All topics replicated
- **Replication Factor**: 3
- **Log Retention**: 7 days (configured per topic)
- **Location**: `/backups/kafka/topics/`

---

## Backup Procedures

### Automated Daily Backup

All backups are automated via the `scripts/run_backup.sh` script.

```bash
# Run all backups
./scripts/run_backup.sh

# Run specific component backup
./scripts/run_backup.sh postgres
./scripts/run_backup.sh redis
./scripts/run_backup.sh kafka
```

### PostgreSQL Backup (Manual)

```bash
# Backup all databases
docker exec payu-postgres pg_dumpall -U payu | gzip > /backups/postgres/daily/all_databases_$(date +%Y%m%d_%H%M%S).sql.gz

# Backup specific database
docker exec payu-postgres pg_dump -U payu -Fc payu_account | gzip > /backups/postgres/daily/payu_account_$(date +%Y%m%d_%H%M%S).dump.gz

# List available backups
ls -lh /backups/postgres/daily/
```

### Redis Backup (Manual)

```bash
# Trigger immediate snapshot
docker exec payu-redis redis-cli BGSAVE

# Wait for save to complete
docker exec payu-redis redis-cli LASTSAVE

# Copy RDB file to backup location
docker cp payu-redis:/data/dump.rdb /backups/redis/snapshots/dump_$(date +%Y%m%d_%H%M%S).rdb
```

### Kafka Backup (Manual)

```bash
# Export topic data to backup directory
docker exec payu-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic transactions \
  --from-beginning \
  --backup-file /backups/kafka/topics/transactions_$(date +%Y%m%d_%H%M%S).json
```

---

## Restore Procedures

### Emergency Response Flow

```
1. Detect Incident
   ↓
2. Assess Impact (What's down? What data is affected?)
   ↓
3. Choose Recovery Strategy (PITR / Full Restore / Partial)
   ↓
4. Execute Restore (Follow component-specific procedures)
   ↓
5. Verify Recovery (Data integrity, service connectivity)
   ↓
6. Monitor Stability (30 minutes of observation)
   ↓
7. Document Incident (Root cause, lessons learned)
```

### PostgreSQL Restore

#### Option 1: Full Database Restore

```bash
# Stop application services to prevent data corruption
docker-compose stop account-service transaction-service wallet-service

# Drop existing database (CAUTION!)
docker exec payu-postgres psql -U payu -c "DROP DATABASE IF EXISTS payu_account;"

# Recreate database
docker exec payu-postgres psql -U payu -c "CREATE DATABASE payu_account;"

# Restore from backup
gunzip -c /backups/postgres/daily/payu_account_20250122_020000.dump.gz | \
  docker exec -i payu-postgres pg_restore -U payu -d payu_account -Fc

# Restart services
docker-compose start account-service transaction-service wallet-service
```

#### Option 2: Point-in-Time Recovery (Production)

```bash
# Restore base backup
docker exec payu-postgres pg_restore -U payu -d payu_account -Fc /backups/postgres/weekly/base_backup.dump

# Replay WAL logs to desired time
docker exec payu-postgres pg_ctl promote -D /var/lib/postgresql/data

# Verify recovery
docker exec payu-postgres psql -U payu -d payu_account -c "SELECT now();"
```

#### Option 3: Partial Restore (Specific Tables)

```bash
# Extract specific table from dump
pg_restore -U payu -Fc -t accounts -f accounts_table.sql /backups/postgres/daily/payu_account_20250122_020000.dump

# Import into database
docker exec -i payu-postgres psql -U payu -d payu_account < accounts_table.sql
```

### Redis Restore

```bash
# Stop Redis service
docker stop payu-redis

# Copy backup RDB file to data directory
cp /backups/redis/snapshots/dump_20250122_100000.rdb /var/lib/docker/volumes/payu_redis_data/_data/dump.rdb

# Start Redis service
docker start payu-redis

# Verify restore
docker exec payu-redis redis-cli DBSIZE
```

### Kafka Restore

```bash
# Create new topic or reset existing offsets
docker exec payu-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic transactions-restored \
  --partitions 3 \
  --replication-factor 1

# Import messages from backup
docker exec -i payu-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic transactions-restored < /backups/kafka/topics/transactions_20250122_020000.json

# Verify restore
docker exec payu-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic transactions-restored \
  --from-beginning \
  --max-messages 10
```

---

## Testing & Verification

### Automated Backup Verification

The `tests/infrastructure/test_backup_restore.py` test suite verifies:

1. **Backup Completeness**: All databases backed up successfully
2. **Backup Integrity**: Restore from backup completes without errors
3. **Data Consistency**: Restored data matches source data
4. **RPO Verification**: Maximum data loss within acceptable limits

### Running Tests

```bash
# Run all backup/restore tests
pytest tests/infrastructure/test_backup_restore.py -v

# Run specific test
pytest tests/infrastructure/test_backup_restore.py::test_postgres_backup_restore -v

# Run tests with detailed output
pytest tests/infrastructure/test_backup_restore.py -vv
```

### Manual Verification Steps

#### PostgreSQL Verification

```bash
# 1. Verify backup file exists
ls -lh /backups/postgres/daily/

# 2. Verify backup file is valid
docker exec payu-postgres pg_restore -l /backups/postgres/daily/payu_account_20250122_020000.dump.gz | head

# 3. Record count verification
# Source:
docker exec payu-postgres psql -U payu -d payu_account -c "SELECT COUNT(*) FROM accounts;"

# After restore:
docker exec payu-postgres psql -U payu -d payu_account_restored -c "SELECT COUNT(*) FROM accounts;"
```

#### Redis Verification

```bash
# 1. Verify backup file size
ls -lh /backups/redis/snapshots/

# 2. Key count verification
# Source:
docker exec payu-redis redis-cli DBSIZE

# After restore:
docker exec payu-redis-restored redis-cli DBSIZE
```

#### Kafka Verification

```bash
# 1. Verify backup file exists
ls -lh /backups/kafka/topics/

# 2. Message count verification
# Source:
docker exec payu-kafka kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 --topic transactions --time -1

# After restore:
docker exec payu-kafka kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 --topic transactions-restored --time -1
```

### Scheduled Testing

- **Daily**: Automated backup integrity check
- **Weekly**: Partial restore test (single database/table)
- **Monthly**: Full system restore test (dev environment)

---

## Incident Response

### Incident Classification

| Severity | Description | Response Time | Escalation |
|----------|-------------|---------------|------------|
| **P1** | Complete system outage | 15 minutes | VP Engineering |
| **P2** | Single database failure | 30 minutes | Engineering Manager |
| **P3** | Data corruption suspected | 1 hour | Tech Lead |
| **P4** | Backup verification failure | 4 hours | DevOps Team |

### Incident Response Checklist

1. **Containment**: Stop all writes to affected systems
2. **Assessment**: Determine scope and impact
3. **Communication**: Notify stakeholders (SLA breaches)
4. **Execution**: Execute appropriate restore procedure
5. **Verification**: Confirm system is operational
6. **Post-Incident**: Document root cause and preventive measures

### Communication Templates

#### Initial Notification

```
SUBJECT: [INCIDENT] PayU Service Outage - Database Failure

STATUS: P1 Incident
STARTED: [Timestamp]
IMPACT: [Affected services and users]
ACTION: Restoring from backup
ETA: [Estimated recovery time]
UPDATES: [Public status page URL]
```

#### Resolution Notification

```
SUBJECT: [RESOLVED] PayU Service Outage - Database Failure

RESOLVED: [Timestamp]
ROOT CAUSE: [Brief description]
DATA LOSS: [None / Partial - details]
PREVENTIVE ACTIONS: [What we're doing to prevent recurrence]
```

---

## Contact Information

### On-Call Rotation

| Role | Name | Contact |
|------|------|---------|
| **Platform Lead** | [Name] | [Phone/Email] |
| **Database Admin** | [Name] | [Phone/Email] |
| **DevOps Engineer** | [Name] | [Phone/Email] |

### External Contacts

| Service | Contact |
|---------|---------|
| **Cloud Provider** | [AWS/Azure/GCP Support] |
| **Database Support** | [PostgreSQL Support Vendor] |
| **Security Team** | [Incident Response Team] |

---

## Appendix

### Backup Directory Structure

```
/backups/
├── postgres/
│   ├── daily/
│   │   ├── all_databases_20250122_020000.sql.gz
│   │   ├── payu_account_20250122_020000.dump.gz
│   │   ├── payu_auth_20250122_020000.dump.gz
│   │   └── ...
│   ├── weekly/
│   │   └── base_backup_20250119_020000/
│   └── wal/
│       └── [archived WAL files]
├── redis/
│   └── snapshots/
│       ├── dump_20250122_100000.rdb
│       └── ...
└── kafka/
    └── topics/
        ├── transactions_20250122_020000.json
        └── ...
```

### Cron Jobs (Production)

```bash
# PostgreSQL daily backup - 02:00 UTC
0 2 * * * /home/payu/scripts/backup_postgres.sh daily

# PostgreSQL weekly backup - Sunday 02:00 UTC
0 2 * * 0 /home/payu/scripts/backup_postgres.sh weekly

# Redis hourly backup
0 * * * * /home/payu/scripts/backup_restore_redis.sh backup

# Kafka continuous replication (running as service)
@reboot /home/payu/scripts/backup_restore_kafka.sh start
```

### Environment-Specific Settings

| Setting | Development | Staging | Production |
|---------|-------------|---------|------------|
| **Backup Frequency** | Weekly | Daily | Continuous + Daily |
| **Retention** | 7 days | 30 days | 30 days (7 years compliance) |
| **RTO** | 1 hour | 30 minutes | 15 minutes |
| **RPO** | 1 hour | 5 minutes | 1 minute |
| **Backup Location** | Local disk | Network storage | Cloud storage + offsite |

---

**Document Version**: 1.0
**Last Updated**: 2025-01-22
**Next Review**: 2025-07-22
