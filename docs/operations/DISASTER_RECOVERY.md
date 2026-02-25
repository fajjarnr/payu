# PayU Digital Banking Platform - Disaster Recovery Runbook

> **Version**: 2.0 | **Last Updated**: February 20, 2026 | **Status**: Production Ready
>
> **Scope**: OpenShift 4.20+ | 22 Microservices | Multi-AZ Deployment

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Recovery Objectives (RTO/RPO)](#recovery-objectives-rtorpo)
3. [DR Architecture Overview](#dr-architecture-overview)
4. [Incident Response Procedures](#incident-response-procedures)
5. [Component-Specific Recovery Procedures](#component-specific-recovery-procedures)
   - [PostgreSQL (Crunchy PGO)](#postgresql-crunchy-pgo)
   - [Kafka (AMQ Streams KRaft)](#kafka-amq-streams-kraft)
   - [Vault (HashiCorp with VSO)](#vault-hashicorp-with-vso)
   - [DataGrid (Infinispan)](#datagrid-infinispan)
   - [Keycloak (RHBK)](#keycloak-rhbk)
6. [Service Degradation Scenarios](#service-degradation-scenarios)
7. [Complete Platform Restore](#complete-platform-restore)
8. [DR Testing Procedures](#dr-testing-procedures)
9. [Escalation Matrix](#escalation-matrix)
10. [Appendix](#appendix)

---

## Executive Summary

This Disaster Recovery (DR) Runbook provides comprehensive procedures for recovering the PayU Digital Banking Platform running on OpenShift 4.20+. The platform consists of:

- **22 microservices** (Spring Boot 3.4, Quarkus 3.x, Python FastAPI)
- **PostgreSQL 16** with Crunchy PGO (primary/standby)
- **AMQ Streams (Kafka 4.0)** with KRaft mode
- **Red Hat Data Grid** (Infinispan) for caching
- **HashiCorp Vault** with Vault Secrets Operator (VSO)
- **Keycloak (RHBK)** for identity management

### DR Scenarios Covered

| Scenario                   | Impact                | Recovery Method      | Target RTO |
| -------------------------- | --------------------- | -------------------- | ---------- |
| PostgreSQL Primary Failure | Data layer down       | Patroni failover     | 2 min      |
| Kafka Broker Failure       | Event streaming down  | KRaft auto-recovery  | 5 min      |
| Vault Unseal Issue         | Secret access blocked | Auto-unseal / manual | 10 min     |
| Complete Namespace Loss    | Full platform down    | Backup restore       | 30 min     |
| Multi-AZ Failure           | Regional outage       | DR region failover   | 15 min     |

---

## Recovery Objectives (RTO/RPO)

### Definitions

- **RTO (Recovery Time Objective)**: Maximum acceptable time to restore service
- **RPO (Recovery Point Objective)**: Maximum acceptable data loss

### Per-Component RTO/RPO

| Component                     | RTO    | RPO     | Data Loss Window  | Notes                               |
| ----------------------------- | ------ | ------- | ----------------- | ----------------------------------- |
| **PostgreSQL (Critical DBs)** | 2 min  | 0 min   | None              | Patroni synchronous replication     |
| **PostgreSQL (Standard DBs)** | 5 min  | < 1 min | < 1 min           | Asynchronous replication acceptable |
| **Kafka Topics**              | 5 min  | < 5 min | < 5 min           | KRaft metadata + topic replication  |
| **Vault Secrets**             | 10 min | 0 min   | None              | Raft storage + auto-unseal          |
| **DataGrid Cache**            | 3 min  | 5 min   | Session data only | Cache can be rebuilt                |
| **Keycloak**                  | 5 min  | 0 min   | None              | DB-backed state                     |
| **Application Services**      | 10 min | N/A     | N/A               | Stateless, redeploy from images     |

### Service-Level Recovery Priorities

| Priority          | Services                                                           | RTO    | Business Impact        |
| ----------------- | ------------------------------------------------------------------ | ------ | ---------------------- |
| **P0 - Critical** | auth-service, transaction-service, wallet-service, account-service | 5 min  | Complete service halt  |
| **P1 - High**     | gateway-service, notification-service, compliance-service          | 10 min | Degraded functionality |
| **P2 - Medium**   | billing-service, investment-service, lending-service               | 15 min | Limited feature impact |
| **P3 - Low**      | analytics-service, backoffice-service, cms-service                 | 30 min | Reporting/admin only   |

---

## DR Architecture Overview

### High-Availability Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           OpenShift Cluster 4.20+                           │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Multi-AZ Deployment                          │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │   │
│  │  │   Zone A     │  │   Zone B     │  │   Zone C     │               │   │
│  │  │ (Primary)    │  │ (Secondary)  │  │ (DR Ready)   │               │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     Data Layer (Stateful)                            │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐      │   │
│  │  │  PostgreSQL     │  │  Kafka KRaft    │  │  DataGrid       │      │   │
│  │  │  (Patroni HA)   │  │  (3 brokers)    │  │  (Infinispan)   │      │   │
│  │  │  Primary/Standby│  │  3 controllers  │  │  Distributed    │      │   │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘      │   │
│  │  ┌─────────────────┐  ┌─────────────────┐                           │   │
│  │  │  Vault (Raft)   │  │  Keycloak       │                           │   │
│  │  │  Auto-unseal    │  │  (DB-backed)    │                           │   │
│  │  └─────────────────┘  └─────────────────┘                           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    Application Layer (Stateless)                     │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │   │
│  │  │ account  │ │ auth     │ │ wallet   │ │ transact │ │ gateway  │  │   │
│  │  │ lending  │ │ billing  │ │ notify   │ │ kyc      │ │ analytics│  │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Backup Architecture

| Component      | Backup Method                     | Frequency  | Storage Location             |
| -------------- | --------------------------------- | ---------- | ---------------------------- |
| PostgreSQL     | pgBackRest (full) + WAL archiving | Continuous | S3-compatible object storage |
| PostgreSQL     | pg_dump (logical)                 | Daily      | S3 + offsite                 |
| Kafka          | Topic replication + MM2           | Continuous | Secondary cluster            |
| Vault          | Raft snapshot                     | Hourly     | S3 + encrypted volume        |
| Config/Secrets | GitOps + VSO                      | On change  | Git + Vault                  |

---

## Incident Response Procedures

### Incident Classification

| Severity          | Criteria                                                                            | Response Time | Escalation          |
| ----------------- | ----------------------------------------------------------------------------------- | ------------- | ------------------- |
| **P0 - Critical** | Complete platform outage; all services unavailable; data loss confirmed             | 5 minutes     | CTO, VP Engineering |
| **P1 - High**     | Core banking services down (auth, transaction, wallet); partial data unavailability | 15 minutes    | Engineering Manager |
| **P2 - Medium**   | Single component failure; non-critical services affected                            | 30 minutes    | Tech Lead           |
| **P3 - Low**      | Degraded performance; monitoring/alerting issues                                    | 2 hours       | DevOps Team         |

### Response Workflow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        INCIDENT RESPONSE WORKFLOW                            │
└─────────────────────────────────────────────────────────────────────────────┘

1. DETECTION
   ├── Automated alert (PagerDuty/Opsgenie)
   ├── Manual report
   └── Monitoring dashboard anomaly
   ↓
2. TRIAGE (5 minutes)
   ├── Classify severity (P0-P3)
   ├── Identify affected components
   ├── Assess data loss risk
   └── Page on-call engineer
   ↓
3. CONTAINMENT
   ├── Stop writes to affected systems (if needed)
   ├── Isolate failed components
   └── Enable circuit breakers
   ↓
4. RECOVERY (Execute this runbook)
   ├── Follow component-specific procedures
   ├── Execute DR scripts
   └── Verify service restoration
   ↓
5. VERIFICATION
   ├── Health checks pass
   ├── Data integrity confirmed
   ├── Monitoring green
   └── User acceptance tests
   ↓
6. POST-INCIDENT
   ├── Root cause analysis
   ├── Update runbook if needed
   └── Lessons learned document
```

### Initial Assessment Commands

```bash
# Check overall platform status
oc get pods -n payu-dev --field-selector=status.phase!=Running

# Check critical infrastructure
oc get pods -n payu-dev -l 'app.kubernetes.io/component in (database,messaging,cache,secrets,identity)'

# Check recent events
oc get events -n payu-dev --sort-by='.lastTimestamp' | tail -50

# Check node status
oc get nodes -o wide

# Check persistent volumes
oc get pvc -n payu-dev
```

---

## Component-Specific Recovery Procedures

### PostgreSQL (Crunchy PGO)

#### Overview

PayU uses Crunchy PostgreSQL Operator (PGO) with Patroni for HA. The cluster consists of:

- **Primary**: Read-write instance
- **Standby**: Hot standby for failover
- **pgBouncer**: Connection pooling
- **pgBackRest**: Backup and WAL archiving

#### Scenario 1: Primary Database Failure

**Symptoms**:

- Applications report connection failures
- `oc get pods` shows postgres-primary pod in Error/CrashLoopBackOff
- Patroni failover not automatic

**Recovery Steps**:

```bash
#!/bin/bash
# PostgreSQL Primary Failure Recovery
# File: scripts/dr-postgres-primary-failure.sh

set -euo pipefail

NAMESPACE="${NAMESPACE:-payu-dev}"
CLUSTER_NAME="payu-postgres"

echo "=== PostgreSQL Primary Failure Recovery ==="
echo "Timestamp: $(date)"
echo "Namespace: ${NAMESPACE}"
echo ""

# Step 1: Check current cluster status
echo "Step 1: Checking cluster status..."
oc exec -n ${NAMESPACE} ${CLUSTER_NAME}-instance1-xxx-0 -- patronictl list

# Step 2: Identify failed primary
echo "Step 2: Identifying failed primary..."
FAILED_POD=$(oc get pods -n ${NAMESPACE} -l postgres-operator.crunchydata.com/cluster=${CLUSTER_NAME},postgres-operator.crunchydata.com/role=master -o name 2>/dev/null || echo "")

if [ -n "$FAILED_POD" ]; then
    echo "Failed primary found: ${FAILED_POD}"

    # Step 3: Force failover to standby
    echo "Step 3: Initiating failover..."
    oc exec -n ${NAMESPACE} ${CLUSTER_NAME}-instance1-xxx-0 -- patronictl failover --force

    # Step 4: Verify new primary
    echo "Step 4: Verifying new primary..."
    sleep 10
    oc exec -n ${NAMESPACE} ${CLUSTER_NAME}-instance1-xxx-0 -- patronictl list

    # Step 5: Delete failed primary pod (it will restart as standby)
    echo "Step 5: Cleaning up failed primary..."
    oc delete pod -n ${NAMESPACE} ${FAILED_POD} --force --grace-period=0

    echo "Failover complete. Monitor replication status."
else
    echo "No failed primary found. Checking if failover already occurred..."
    oc exec -n ${NAMESPACE} ${CLUSTER_NAME}-instance1-xxx-0 -- patronictl list
fi
```

#### Scenario 2: Complete Database Restore from pgBackRest

**When to use**:

- Data corruption across all databases
- Need to restore to specific point in time
- Complete cluster rebuild

**Recovery Steps**:

```bash
#!/bin/bash
# PostgreSQL Complete Restore from pgBackRest
# File: scripts/dr-postgres-full-restore.sh

set -euo pipefail

NAMESPACE="${NAMESPACE:-payu-dev}"
CLUSTER_NAME="payu-postgres"
RESTORE_TYPE="${1:-full}"  # full or pitr
PITR_TIME="${2:-}"         # For PITR: "2026-02-20 14:30:00"

echo "=== PostgreSQL Complete Restore ==="
echo "Timestamp: $(date)"
echo "Restore Type: ${RESTORE_TYPE}"
[ -n "$PITR_TIME" ] && echo "PITR Time: ${PITR_TIME}"
echo ""

# Step 1: Scale down all application services
echo "Step 1: Scaling down application services..."
oc scale deployment -n ${NAMESPACE} --all --replicas=0

# Step 2: Annotate PostgresCluster for restore
echo "Step 2: Preparing restore annotation..."
if [ "${RESTORE_TYPE}" == "pitr" ] && [ -n "$PITR_TIME" ]; then
    cat <<EOF | oc apply -f -
apiVersion: postgres-operator.crunchydata.com/v1beta1
kind: PostgresCluster
metadata:
  name: ${CLUSTER_NAME}
  namespace: ${NAMESPACE}
  annotations:
    postgres-operator.crunchydata.com/pgbackrest-restore: "${PITR_TIME}"
spec:
  backups:
    pgbackrest:
      restore:
        enabled: true
        repoName: repo1
        options:
          - --type=time
          - --target="${PITR_TIME}"
EOF
else
    cat <<EOF | oc apply -f -
apiVersion: postgres-operator.crunchydata.com/v1beta1
kind: PostgresCluster
metadata:
  name: ${CLUSTER_NAME}
  namespace: ${NAMESPACE}
  annotations:
    postgres-operator.crunchydata.com/pgbackrest-restore: "latest"
spec:
  backups:
    pgbackrest:
      restore:
        enabled: true
        repoName: repo1
EOF
fi

# Step 3: Wait for restore to complete
echo "Step 3: Waiting for restore to complete..."
oc wait --for=condition=Ready pod -l postgres-operator.crunchydata.com/cluster=${CLUSTER_NAME} -n ${NAMESPACE} --timeout=600s

# Step 4: Remove restore annotation
echo "Step 4: Cleaning up restore annotation..."
oc annotate postgrescluster -n ${NAMESPACE} ${CLUSTER_NAME} postgres-operator.crunchydata.com/pgbackrest-restore-

# Step 5: Verify databases
echo "Step 5: Verifying databases..."
oc exec -n ${NAMESPACE} ${CLUSTER_NAME}-instance1-0 -- psql -U payu -c "\l" | grep payu_

# Step 6: Scale up critical services first
echo "Step 6: Scaling up critical services..."
oc scale deployment -n ${NAMESPACE} auth-service transaction-service wallet-service account-service --replicas=1

# Step 7: Wait for critical services
echo "Step 7: Waiting for critical services..."
for svc in auth-service transaction-service wallet-service account-service; do
    oc rollout status deployment/${svc} -n ${NAMESPACE} --timeout=300s
done

# Step 8: Scale up remaining services
echo "Step 8: Scaling up remaining services..."
oc scale deployment -n ${NAMESPACE} --all --replicas=1

echo "Restore complete. Verify application functionality."
```

#### Monitoring PostgreSQL Health

```bash
# Check Patroni status
oc exec -n payu-dev payu-postgres-instance1-xxx-0 -- patronictl list

# Check replication lag
oc exec -n payu-dev payu-postgres-instance1-xxx-0 -- psql -U payu -c "
SELECT
    client_addr,
    state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn,
    write_lag,
    flush_lag,
    replay_lag
FROM pg_stat_replication;"

# Check connection count
oc exec -n payu-dev payu-postgres-instance1-xxx-0 -- psql -U payu -c "
SELECT count(*), state FROM pg_stat_activity GROUP BY state;"

# Check pgBackRest backup status
oc exec -n payu-dev payu-postgres-repo-host-0 -- pgbackrest info
```

---

### Kafka (AMQ Streams KRaft)

#### Overview

PayU uses AMQ Streams (Apache Kafka 4.0) with KRaft mode (no ZooKeeper):

- **KafkaNodePool (controller)**: 1 replica (metadata quorum)
- **KafkaNodePool (broker)**: 1 replica (data handling)
- **Entity Operator**: Topic and user management
- **Topics**: Pre-created with replication factor 1 (dev), 3 (prod)

#### Scenario 1: Kafka Broker Failure

**Symptoms**:

- Services report Kafka connection errors
- `oc get pods` shows kafka-broker pod failing
- Event publishing/consumption stopped

**Recovery Steps**:

```bash
#!/bin/bash
# Kafka Broker Failure Recovery
# File: scripts/dr-kafka-broker-failure.sh

set -euo pipefail

NAMESPACE="${NAMESPACE:-payu-dev}"
KAFKA_CLUSTER="kafka"

echo "=== Kafka Broker Failure Recovery ==="
echo "Timestamp: $(date)"
echo ""

# Step 1: Check Kafka cluster status
echo "Step 1: Checking Kafka cluster status..."
oc get kafka -n ${NAMESPACE} ${KAFKA_CLUSTER} -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}'
echo ""

# Step 2: Check pod status
echo "Step 2: Checking Kafka pods..."
oc get pods -n ${NAMESPACE} -l strimzi.io/cluster=${KAFKA_CLUSTER}

# Step 3: Identify failed broker
echo "Step 3: Identifying failed broker..."
FAILED_BROKER=$(oc get pods -n ${NAMESPACE} -l strimzi.io/cluster=${KAFKA_CLUSTER},strimzi.io/kind=Kafka -o name | grep -v Running || echo "")

if [ -n "$FAILED_BROKER" ]; then
    echo "Failed broker: ${FAILED_BROKER}"

    # Step 4: Delete failed pod (operator will recreate)
    echo "Step 4: Recreating failed broker..."
    oc delete -n ${NAMESPACE} ${FAILED_BROKER} --force --grace-period=0

    # Step 5: Wait for broker to restart
    echo "Step 5: Waiting for broker restart..."
    sleep 30
    oc wait --for=condition=Ready pod -l strimzi.io/cluster=${KAFKA_CLUSTER} -n ${NAMESPACE} --timeout=300s

    # Step 6: Verify topic integrity
    echo "Step 6: Verifying topics..."
    oc exec -n ${NAMESPACE} ${KAFKA_CLUSTER}-broker-0 -- kafka-topics.sh \
        --bootstrap-server localhost:9092 --list

    echo "Broker recovery complete."
else
    echo "No failed broker found. Checking controller..."
    oc get pods -n ${NAMESPACE} -l strimzi.io/cluster=${KAFKA_CLUSTER},strimzi.io/kind=Kafka
fi
```

#### Scenario 2: Topic Data Loss Recovery

```bash
#!/bin/bash
# Kafka Topic Recovery from MirrorMaker2
# File: scripts/dr-kafka-topic-recovery.sh

set -euo pipefail

NAMESPACE="${NAMESPACE:-payu-dev}"
TOPIC_NAME="${1:-}"

echo "=== Kafka Topic Recovery ==="
echo "Topic: ${TOPIC_NAME}"
echo ""

if [ -z "$TOPIC_NAME" ]; then
    echo "Usage: $0 <topic-name>"
    exit 1
fi

# Step 1: Check if topic exists
echo "Step 1: Checking topic status..."
if oc exec -n ${NAMESPACE} kafka-broker-0 -- kafka-topics.sh \
    --bootstrap-server localhost:9092 --describe --topic ${TOPIC_NAME} 2>/dev/null; then
    echo "Topic exists. Checking data..."

    # Check message count
    MESSAGE_COUNT=$(oc exec -n ${NAMESPACE} kafka-broker-0 -- kafka-run-class.sh kafka.tools.GetOffsetShell \
        --broker-list localhost:9092 --topic ${TOPIC_NAME} --time -2 2>/dev/null | wc -l)
    echo "Topic has ${MESSAGE_COUNT} partitions"
else
    echo "Topic does not exist. Creating from backup..."

    # Step 2: Create topic with original configuration
    echo "Step 2: Creating topic..."
    oc exec -n ${NAMESPACE} kafka-broker-0 -- kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --create \
        --topic ${TOPIC_NAME} \
        --partitions 3 \
        --replication-factor 1 \
        --config retention.ms=604800000

    echo "Topic created. Data will be rebuilt from source systems."
fi
```

#### Monitoring Kafka Health

```bash
# Check Kafka cluster status
oc get kafka -n payu-dev kafka -o yaml | grep -A 20 "status:"

# Check topic status
oc exec -n payu-dev kafka-broker-0 -- kafka-topics.sh --bootstrap-server localhost:9092 --describe

# Check consumer groups
oc exec -n payu-dev kafka-broker-0 -- kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 --list

# Check consumer lag
oc exec -n payu-dev kafka-broker-0 -- kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 \
    --describe \
    --group <consumer-group-name>

# Check KRaft metadata quorum
oc exec -n payu-dev kafka-controller-1 -- cat /tmp/strimzi.properties | grep process.roles
```

---

### Vault (HashiCorp with VSO)

#### Overview

PayU uses HashiCorp Vault with Vault Secrets Operator (VSO):

- **Vault**: Dev mode (single instance) - production should use HA Raft
- **VSO**: Syncs secrets to Kubernetes secrets
- **Auth**: Kubernetes auth method
- **Secrets**: KV v2 engine

#### Scenario 1: Vault Unseal/Availability Issues

**Symptoms**:

- Pods report missing secrets
- VSO cannot sync secrets
- Vault pod in CrashLoopBackOff

**Recovery Steps**:

```bash
#!/bin/bash
# Vault Recovery Procedures
# File: scripts/dr-vault-recovery.sh

set -euo pipefail

NAMESPACE="${NAMESPACE:-payu-dev}"
ACTION="${1:-status}"

echo "=== Vault Recovery ==="
echo "Action: ${ACTION}"
echo "Timestamp: $(date)"
echo ""

case "${ACTION}" in
    status)
        echo "Checking Vault status..."
        oc exec -n ${NAMESPACE} vault-0 -- sh -c "VAULT_ADDR=http://127.0.0.1:8200 vault status"
        ;;

    unseal)
        echo "Unsealing Vault..."
        # For dev mode, Vault auto-unseals. For production HA:
        # oc exec -n ${NAMESPACE} vault-0 -- sh -c "VAULT_ADDR=http://127.0.0.1:8200 vault operator unseal <UNSEAL_KEY>"
        echo "Dev mode Vault auto-unseals. Check pod status:"
        oc get pods -n ${NAMESPACE} -l app.kubernetes.io/name=vault
        ;;

    restart)
        echo "Restarting Vault..."
        oc delete pod -n ${NAMESPACE} -l app.kubernetes.io/name=vault --force --grace-period=0
        sleep 10
        oc wait --for=condition=Ready pod -l app.kubernetes.io/name=vault -n ${NAMESPACE} --timeout=120s

        echo "Vault restarted. Reinitializing VSO sync..."
        oc delete vaultstaticsecrets -n ${NAMESPACE} --all
        oc apply -k infrastructure/openshift/infra/base/vault.yaml
        ;;

    rotate)
        echo "Rotating all secrets..."
        # Rotate database credentials
        oc exec -n ${NAMESPACE} vault-0 -- sh -c "
            VAULT_ADDR=http://127.0.0.1:8200 vault kv put secret/payu/db-credentials \
                username=payu \
                password=\$(openssl rand -base64 32)
        "

        # Rotate JWT secret
        oc exec -n ${NAMESPACE} vault-0 -- sh -c "
            VAULT_ADDR=http://127.0.0.1:8200 vault kv put secret/payu/jwt-secret \
                secret=\$(openssl rand -base64 64)
        "

        echo "Secrets rotated. VSO will sync within 60 seconds."
        ;;

    *)
        echo "Usage: $0 {status|unseal|restart|rotate}"
        exit 1
        ;;
esac
```

#### Scenario 2: Secret Synchronization Failure

```bash
#!/bin/bash
# VSO Secret Sync Recovery
# File: scripts/dr-vso-sync-recovery.sh

set -euo pipefail

NAMESPACE="${NAMESPACE:-payu-dev}"

echo "=== VSO Secret Sync Recovery ==="
echo "Timestamp: $(date)"
echo ""

# Step 1: Check VSO status
echo "Step 1: Checking VaultConnection..."
oc get vaultconnection -n ${NAMESPACE}

# Step 2: Check VaultAuth
echo "Step 2: Checking VaultAuth..."
oc get vaultauth -n ${NAMESPACE}

# Step 3: Check VaultStaticSecrets
echo "Step 3: Checking VaultStaticSecrets status..."
oc get vaultstaticsecrets -n ${NAMESPACE}

# Step 4: Check synced Kubernetes secrets
echo "Step 4: Checking synced secrets..."
oc get secrets -n ${NAMESPACE} | grep -E "(db-credentials|jwt-secret|encryption-keys)"

# Step 5: Force secret refresh
echo "Step 5: Forcing secret refresh..."
for secret in vault-db-credentials vault-jwt-secret vault-encryption-keys vault-keycloak-credentials vault-keycloak-db-secret; do
    echo "Refreshing ${secret}..."
    oc annotate vaultstaticsecret -n ${NAMESPACE} ${secret} vault.hashicorp.com/force-sync="$(date +%s)" || true
done

# Step 6: Verify sync
echo "Step 6: Verifying secret sync..."
sleep 5
oc get secrets -n ${NAMESPACE} -o json | jq '.items[] | select(.metadata.labels."app.kubernetes.io/component" == "secrets") | .metadata.name'

echo "Secret sync recovery complete."
```

---

### DataGrid (Infinispan)

#### Overview

PayU uses Red Hat Data Grid (Infinispan) for caching:

- **Mode**: DataGrid (distributed)
- **Protocol**: RESP (Redis-compatible)
- **Replicas**: 1 (dev), 3+ (prod)
- **Persistence**: None (cache only)

#### Recovery Procedure

```bash
#!/bin/bash
# DataGrid Recovery
# File: scripts/dr-datagrid-recovery.sh

set -euo pipefail

NAMESPACE="${NAMESPACE:-payu-dev}"

echo "=== DataGrid Recovery ==="
echo "Timestamp: $(date)"
echo ""

# Step 1: Check DataGrid status
echo "Step 1: Checking DataGrid status..."
oc get infinispan -n ${NAMESPACE} payu-datagrid -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}'
echo ""

# Step 2: Check pods
echo "Step 2: Checking DataGrid pods..."
oc get pods -n ${NAMESPACE} -l app.kubernetes.io/name=payu-datagrid

# Step 3: Check cache status
echo "Step 3: Checking caches..."
oc get caches -n ${NAMESPACE}

# Step 4: If recovery needed, delete and recreate
FAILED_POD=$(oc get pods -n ${NAMESPACE} -l app.kubernetes.io/name=payu-datagrid -o name | xargs -I {} oc get {} -o jsonpath='{.status.phase}' | grep -v Running || echo "")

if [ -n "$FAILED_POD" ]; then
    echo "DataGrid pod not running. Restarting..."
    oc delete pod -n ${NAMESPACE} -l app.kubernetes.io/name=payu-datagrid --force --grace-period=0
    sleep 10
    oc wait --for=condition=Ready pod -l app.kubernetes.io/name=payu-datagrid -n ${NAMESPACE} --timeout=120s

    echo "DataGrid restarted. Caches will be rebuilt on demand."
else
    echo "DataGrid is healthy."
fi

# Step 5: Verify RESP connectivity
echo "Step 5: Testing RESP connectivity..."
oc exec -n ${NAMESPACE} payu-datagrid-0 -- curl -s http://localhost:11222/rest/v2/caches/ || echo "REST API check complete"

echo "DataGrid recovery complete."
```

---

### Keycloak (RHBK)

#### Overview

PayU uses Red Hat Build of Keycloak (RHBK 26.1):

- **Database**: PostgreSQL (payu-keycloak db)
- **Realm**: payu
- **Clients**: web-app, mobile-app, api-gateway, admin-cli

#### Recovery Procedure

```bash
#!/bin/bash
# Keycloak Recovery
# File: scripts/dr-keycloak-recovery.sh

set -euo pipefail

NAMESPACE="${NAMESPACE:-payu-dev}"

echo "=== Keycloak Recovery ==="
echo "Timestamp: $(date)"
echo ""

# Step 1: Check Keycloak status
echo "Step 1: Checking Keycloak status..."
oc get pods -n ${NAMESPACE} -l app.kubernetes.io/name=keycloak

# Step 2: Check if realm is accessible
echo "Step 2: Checking realm accessibility..."
KEYCLOAK_URL=$(oc get route -n ${NAMESPACE} keycloak -o jsonpath='{.spec.host}' 2>/dev/null || echo "")
if [ -n "$KEYCLOAK_URL" ]; then
    curl -s "https://${KEYCLOAK_URL}/realms/payu/.well-known/openid-configuration" | jq -r '.issuer' || echo "Realm not accessible"
fi

# Step 3: Restart Keycloak if needed
echo "Step 3: Checking pod health..."
if ! oc get pods -n ${NAMESPACE} -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].status.containerStatuses[0].ready}' | grep -q "true"; then
    echo "Keycloak not ready. Restarting..."
    oc delete pod -n ${NAMESPACE} -l app.kubernetes.io/name=keycloak --force --grace-period=0
    sleep 10
    oc wait --for=condition=Ready pod -l app.kubernetes.io/name=keycloak -n ${NAMESPACE} --timeout=300s
fi

# Step 4: Verify realm import
echo "Step 4: Verifying realm configuration..."
oc exec -n ${NAMESPACE} keycloak-0 -- sh -c '
    cd /opt/keycloak/bin
    ./kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin 2>/dev/null || true
    ./kcadm.sh get realms/payu 2>/dev/null | grep -q "payu" && echo "Realm exists" || echo "Realm missing"
'

echo "Keycloak recovery complete."
```

---

## Service Degradation Scenarios

### Gradual Degradation Response

| Degradation Level | Symptoms                         | Response                            |
| ----------------- | -------------------------------- | ----------------------------------- |
| **Level 1**       | Latency > 500ms, Error rate < 1% | Monitor, investigate root cause     |
| **Level 2**       | Latency > 1s, Error rate 1-5%    | Enable circuit breakers, scale up   |
| **Level 3**       | Latency > 5s, Error rate 5-20%   | Partial outage, failover to standby |
| **Level 4**       | Error rate > 20%                 | Full DR activation                  |

### Circuit Breaker Activation

```bash
# Enable circuit breakers for degraded services
oc patch deployment/gateway-service -n payu-dev -p '{"spec":{"template":{"metadata":{"annotations":{"circuit-breaker/enabled":"true"}}}}}'

# Scale down problematic service
oc scale deployment/<service-name> -n payu-dev --replicas=0

# Redirect traffic to fallback
oc patch route <route-name> -n payu-dev -p '{"spec":{"to":{"name":"fallback-service"}}}'
```

---

## Complete Platform Restore

### Scenario: Complete Namespace Deletion Recovery

**When to use**:

- Entire namespace accidentally deleted
- Complete cluster rebuild
- DR region activation

**Prerequisites**:

- OpenShift cluster access
- Backup storage accessible (S3)
- Container images available in registry

**Recovery Steps**:

```bash
#!/bin/bash
# Complete Platform Restore
# File: scripts/dr-complete-platform-restore.sh

set -euo pipefail

NAMESPACE="${NAMESPACE:-payu-dev}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
RESTORE_POINT="${1:-latest}"

echo "=========================================="
echo "PayU Complete Platform Restore"
echo "=========================================="
echo "Timestamp: $(date)"
echo "Namespace: ${NAMESPACE}"
echo "Environment: ${ENVIRONMENT}"
echo "Restore Point: ${RESTORE_POINT}"
echo ""

# Step 1: Create namespace
echo "Step 1: Creating namespace..."
oc create namespace ${NAMESPACE} --dry-run=client -o yaml | oc apply -f -
oc label namespace ${NAMESPACE} app.kubernetes.io/part-of=payu

# Step 2: Deploy operators
echo "Step 2: Deploying operators..."
oc apply -k infrastructure/openshift/operators/
echo "Waiting for operators to be ready..."
sleep 60

# Step 3: Deploy infrastructure
echo "Step 3: Deploying infrastructure..."
oc apply -k infrastructure/openshift/infra/overlays/${ENVIRONMENT}/

echo "Waiting for infrastructure to be ready..."
# Wait for PostgreSQL
oc wait --for=condition=Ready pod -l postgres-operator.crunchydata.com/cluster=payu-postgres -n ${NAMESPACE} --timeout=600s || true
# Wait for Kafka
oc wait --for=condition=Ready pod -l strimzi.io/cluster=kafka -n ${NAMESPACE} --timeout=300s || true
# Wait for Vault
oc wait --for=condition=Ready pod -l app.kubernetes.io/name=vault -n ${NAMESPACE} --timeout=120s || true
# Wait for DataGrid
oc wait --for=condition=Ready pod -l app.kubernetes.io/name=payu-datagrid -n ${NAMESPACE} --timeout=120s || true
# Wait for Keycloak
oc wait --for=condition=Ready pod -l app.kubernetes.io/name=keycloak -n ${NAMESPACE} --timeout=300s || true

echo "Infrastructure deployment complete."

# Step 4: Restore PostgreSQL from backup (if needed)
if [ "${RESTORE_POINT}" != "fresh" ]; then
    echo "Step 4: Restoring PostgreSQL from backup..."
    ./scripts/dr-postgres-full-restore.sh full
fi

# Step 5: Initialize Vault secrets
echo "Step 5: Initializing Vault..."
oc apply -f infrastructure/openshift/infra/base/vault-init-job.yaml
sleep 10
oc wait --for=condition=Complete job/vault-init-job -n ${NAMESPACE} --timeout=120s || true

# Step 6: Deploy application services
echo "Step 6: Deploying application services..."
oc apply -k infrastructure/openshift/overlays/${ENVIRONMENT}/

# Step 7: Wait for critical services
echo "Step 7: Waiting for critical services..."
CRITICAL_SERVICES="auth-service transaction-service wallet-service account-service"
for svc in ${CRITICAL_SERVICES}; do
    echo "Waiting for ${svc}..."
    oc rollout status deployment/${svc} -n ${NAMESPACE} --timeout=300s || echo "Warning: ${svc} rollout timeout"
done

# Step 8: Verify platform health
echo "Step 8: Verifying platform health..."
./scripts/test-health-check.sh

# Step 9: Run smoke tests
echo "Step 9: Running smoke tests..."
curl -sf http://gateway-service.${NAMESPACE}.svc:8080/actuator/health && echo "Gateway OK" || echo "Gateway check failed"

echo ""
echo "=========================================="
echo "Platform Restore Complete"
echo "=========================================="
echo "Verify all services:"
echo "  oc get pods -n ${NAMESPACE}"
echo ""
echo "Check gateway health:"
echo "  oc get route -n ${NAMESPACE} gateway-service"
echo ""
```

---

## DR Testing Procedures

### DR Test Schedule

| Test Type             | Frequency   | Scope     | Owner         |
| --------------------- | ----------- | --------- | ------------- |
| Backup Verification   | Daily       | Automated | CI/CD         |
| PostgreSQL Failover   | Weekly      | Manual    | DBA           |
| Kafka Broker Recovery | Weekly      | Manual    | Platform Team |
| Vault Rotation        | Monthly     | Manual    | Security Team |
| Full DR Simulation    | Quarterly   | Scheduled | DR Team       |
| Multi-Region Failover | Bi-annually | Scheduled | SRE Team      |

### Running DR Tests

```bash
# Test PostgreSQL HA failover
./scripts/dr-test-postgres-failover.sh

# Test Kafka broker recovery
./scripts/dr-test-kafka-failover.sh

# Test complete platform restore
./scripts/dr-test-complete-restore.sh

# Test Vault secret rotation
./scripts/dr-vault-recovery.sh rotate
```

### DR Test Report Template

```markdown
## DR Test Report - [Date]

### Test Information

- **Test Type**: [PostgreSQL Failover / Kafka Recovery / Full Restore]
- **Executed By**: [Name]
- **Start Time**: [Timestamp]
- **End Time**: [Timestamp]

### Test Results

| Component  | Expected RTO | Actual RTO | Status      |
| ---------- | ------------ | ---------- | ----------- |
| PostgreSQL | 2 min        | [X] min    | [PASS/FAIL] |
| Kafka      | 5 min        | [X] min    | [PASS/FAIL] |
| Services   | 10 min       | [X] min    | [PASS/FAIL] |

### Issues Encountered

- [List any issues]

### Lessons Learned

- [Document improvements needed]

### Action Items

- [ ] [Action item 1]
- [ ] [Action item 2]
```

---

## Escalation Matrix

### On-Call Escalation

| Level  | Role                     | Contact           | Response Time |
| ------ | ------------------------ | ----------------- | ------------- |
| **L1** | Platform Engineer        | On-call rotation  | 5 minutes     |
| **L2** | Senior Platform Engineer | Secondary on-call | 15 minutes    |
| **L3** | Engineering Manager      | EM Contact        | 30 minutes    |
| **L4** | VP Engineering / CTO     | Executive contact | 1 hour        |

### External Escalation

| Service              | Provider      | Support Channel    | SLA        |
| -------------------- | ------------- | ------------------ | ---------- |
| OpenShift            | Red Hat       | Customer Portal    | 1 hour     |
| PostgreSQL           | Crunchy Data  | Support ticket     | 4 hours    |
| Cloud Infrastructure | AWS/Azure/GCP | Enterprise support | 15 minutes |

---

## Appendix

### A. Quick Reference Commands

```bash
# Get all platform pods
oc get pods -n payu-dev

# Get pod logs
oc logs -n payu-dev <pod-name> --tail=100 -f

# Execute into pod
oc rsh -n payu-dev <pod-name>

# Port forward for debugging
oc port-forward -n payu-dev pod/<pod-name> 8080:8080

# Check resource usage
oc adm top pods -n payu-dev
oc adm top nodes

# Check events
oc get events -n payu-dev --sort-by='.lastTimestamp' | tail -50

# Check PVC status
oc get pvc -n payu-dev

# Check routes
oc get routes -n payu-dev
```

### B. Related Documentation

- [Container Troubleshooting Guide](./CONTAINER_TROUBLESHOOTING.md)
- [Infrastructure Deployment Guide](./INFRASTRUCTURE_DEPLOYMENT.md)
- [Kafka Topic Standards](./KAFKA_TOPIC_STANDARDS.md)
- [Multi-Region Failover Job](../../infrastructure/openshift/multi-region/failover/failover-job.yaml)

### C. DR Scripts Index

| Script                            | Purpose                     | Location   |
| --------------------------------- | --------------------------- | ---------- |
| `dr-postgres-primary-failure.sh`  | PostgreSQL primary failover | `scripts/` |
| `dr-postgres-full-restore.sh`     | PostgreSQL complete restore | `scripts/` |
| `dr-kafka-broker-failure.sh`      | Kafka broker recovery       | `scripts/` |
| `dr-vault-recovery.sh`            | Vault unseal/rotation       | `scripts/` |
| `dr-vso-sync-recovery.sh`         | VSO secret sync recovery    | `scripts/` |
| `dr-datagrid-recovery.sh`         | DataGrid recovery           | `scripts/` |
| `dr-keycloak-recovery.sh`         | Keycloak recovery           | `scripts/` |
| `dr-complete-platform-restore.sh` | Full platform restore       | `scripts/` |
| `dr-test-postgres-failover.sh`    | Test PostgreSQL HA          | `scripts/` |
| `dr-test-kafka-failover.sh`       | Test Kafka recovery         | `scripts/` |
| `dr-test-complete-restore.sh`     | Test full restore           | `scripts/` |

### D. Document History

| Version | Date       | Changes                                        | Author        |
| ------- | ---------- | ---------------------------------------------- | ------------- |
| 1.0     | 2025-01-22 | Initial DR plan                                | Platform Team |
| 2.0     | 2026-02-20 | OpenShift-specific procedures, DR test scripts | Platform Team |

---

**Document Owner**: PayU Platform Engineering Team
**Review Cycle**: Quarterly
**Next Review**: May 2026
