# Debezium CDC Configuration for PayU Platform

This directory contains Change Data Capture (CDC) configuration using Debezium for the PayU Digital Banking Platform.

## Overview

Debezium captures row-level changes in PostgreSQL databases and publishes them to Apache Kafka topics, enabling:

- **Event Sourcing**: Outbox pattern implementation for reliable event publishing
- **Saga Orchestration**: Tracking saga state changes for distributed transactions
- **Data Replication**: Real-time CDC for analytics and reporting
- **Audit Logging**: Immutable change history for compliance

## Architecture

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────────┐
│  PayU Services  │────▶│  PostgreSQL  │────▶│    Debezium     │
│                 │     │   (WAL)      │     │    Connect      │
└─────────────────┘     └──────────────┘     └────────┬────────┘
                                                      │
                                                      ▼
                                              ┌──────────────┐
                                              │ Kafka Topics │
                                              │              │
                                              │ • payu.*.events│
                                              │ • payu.saga.*  │
                                              │ • payu.wallet.*│
                                              └──────────────┘
```

## Directory Structure

```
debezium/
├── connectors/                    # Connector configurations
│   ├── outbox-connector.json     # Outbox pattern events
│   ├── saga-connector.json       # Saga state tracking
│   └── wallet-cdc-connector.json # Wallet service CDC
├── k8s/                          # Kubernetes manifests
│   ├── debezium-connect-deployment.yaml
│   └── debezium-connect-service.yaml
├── register-connectors.sh        # Connector registration script
└── README.md                     # This file
```

## Connectors

### 1. Outbox Connector (`outbox-connector.json`)

Captures events from the `outbox_events` table using the Outbox pattern.

**Features:**
- Uses `EventRouter` transform to route events by aggregate type
- Topic naming: `payu.{aggregate_type}.events`
- Supports idempotent event publishing
- Dead letter queue: `payu.outbox.dlq`

**Tables:**
- `public.outbox_events`

**Transforms:**
- `outbox`: Routes events based on `aggregate_type` field

### 2. Saga Connector (`saga-connector.json`)

Captures saga orchestration state changes for distributed transactions.

**Features:**
- Tracks saga instance lifecycle
- Monitors saga step execution
- Topic: `payu.saga.changes`
- Excludes sensitive compensation data

**Tables:**
- `public.saga_instances`
- `public.saga_steps`

**Transforms:**
- `unwrap`: Extracts new record state
- `route`: Routes to unified saga topic

### 3. Wallet CDC Connector (`wallet-cdc-connector.json`)

Captures wallet service data changes for real-time balance tracking.

**Features:**
- High-throughput configuration (batch size: 4096)
- Excludes sensitive columns (PIN hashes, biometric data)
- Topic naming: `payu.wallet.cdc.{table}`
- Transaction metadata included

**Tables:**
- `public.wallets`
- `public.ledger_entries`
- `public.wallet_transactions`

**Security:**
- Excluded columns: `pin_hash`, `security_question_hash`, `biometric_hash`
- User ID hashing with SHA-256
- Column truncation for large text fields

## Prerequisites

### Database Setup

1. **Enable logical replication** in PostgreSQL:
```sql
-- postgresql.conf
wal_level = logical
max_replication_slots = 10
max_wal_senders = 10
```

2. **Create replication user**:
```sql
CREATE USER debezium WITH REPLICATION LOGIN PASSWORD 'secure_password';
GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium;
```

3. **Create publication** for each connector:
```sql
-- For outbox connector
CREATE PUBLICATION dbz_outbox_publication FOR TABLE outbox_events;

-- For saga connector
CREATE PUBLICATION dbz_saga_publication FOR TABLE saga_instances, saga_steps;

-- For wallet connector
CREATE PUBLICATION dbz_wallet_publication FOR TABLE wallets, ledger_entries, wallet_transactions;
```

4. **Create heartbeat table** (required for all connectors):
```sql
CREATE TABLE IF NOT EXISTS public.debezium_heartbeat (
    id INTEGER PRIMARY KEY,
    updated_at TIMESTAMP DEFAULT NOW()
);
INSERT INTO public.debezium_heartbeat (id) VALUES (1) ON CONFLICT DO NOTHING;
```

### Kubernetes Setup

1. **Create namespace**:
```bash
kubectl create namespace payu-data-platform
```

2. **Create secrets** (use Vault or external secret management):
```bash
# Kafka authentication
kubectl create secret generic debezium-kafka-auth \
  --from-literal=sasl.jaas.config='org.apache.kafka.common.security.scram.ScramLoginModule required username="debezium" password="***";' \
  --from-literal=truststore.password='***' \
  -n payu-data-platform
```

3. **Configure Vault integration** (if using HashiCorp Vault):
```bash
# Enable database secrets engine
vault secrets enable database

# Configure PostgreSQL connection
vault write database/config/payu-postgresql \
    plugin_name=postgresql-database-plugin \
    allowed_roles="debezium" \
    connection_url="postgresql://{{username}}:{{password}}@postgres:5432/payu_core" \
    username="vaultadmin" \
    password="***"

# Create role with TTL
vault write database/roles/debezium \
    db_name=payu-postgresql \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' REPLICATION; GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"
```

## Deployment

### 1. Deploy Debezium Connect

```bash
kubectl apply -f k8s/debezium-connect-deployment.yaml
kubectl apply -f k8s/debezium-connect-service.yaml
```

### 2. Wait for deployment

```bash
kubectl rollout status deployment/debezium-connect -n payu-data-platform
```

### 3. Register connectors

```bash
# For development
./register-connectors.sh dev

# For staging
./register-connectors.sh staging

# For production
./register-connectors.sh production
```

## Monitoring

### Health Checks

```bash
# Check connector status
curl http://debezium-connect.payu-data-platform.svc:8083/connectors/payu-outbox-connector/status

# List all connectors
curl http://debezium-connect.payu-data-platform.svc:8083/connectors

# Get connector config
curl http://debezium-connect.payu-data-platform.svc:8083/connectors/payu-outbox-connector/config
```

### Metrics

Debezium exposes Prometheus metrics on port 8080:

```yaml
# ServiceMonitor for Prometheus
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: debezium-connect-metrics
  namespace: payu-monitoring
spec:
  selector:
    matchLabels:
      app: debezium-connect
  endpoints:
    - port: metrics
      interval: 30s
      path: /metrics
```

### Key Metrics

| Metric | Description |
|--------|-------------|
| `debezium_postgres_streaming_lsn` | Current WAL position |
| `debezium_postgres_snapshot_duration` | Snapshot completion time |
| `debezium_postgres_events_streamed` | Events streamed count |
| `kafka_connect_connector_task_status` | Connector task status |

## Operations

### Pause/Resume Connector

```bash
# Pause
curl -X PUT http://debezium-connect:8083/connectors/payu-outbox-connector/pause

# Resume
curl -X PUT http://debezium-connect:8083/connectors/payu-outbox-connector/resume
```

### Restart Connector

```bash
curl -X POST http://debezium-connect:8083/connectors/payu-outbox-connector/restart
```

### Update Connector Configuration

```bash
curl -X PUT \
  -H "Content-Type: application/json" \
  -d @connectors/outbox-connector.json \
  http://debezium-connect:8083/connectors/payu-outbox-connector/config
```

### Delete Connector

```bash
curl -X DELETE http://debezium-connect:8083/connectors/payu-outbox-connector
```

## Troubleshooting

### Common Issues

#### 1. Replication Slot Not Found

**Error**: `PSQLException: ERROR: replication slot "debezium_outbox_slot" does not exist`

**Solution**:
```sql
SELECT * FROM pg_replication_slots WHERE slot_name = 'debezium_outbox_slot';
-- If not exists, restart connector to recreate
```

#### 2. Publication Does Not Exist

**Error**: `PSQLException: ERROR: publication "dbz_outbox_publication" does not exist`

**Solution**:
```sql
CREATE PUBLICATION dbz_outbox_publication FOR TABLE outbox_events;
```

#### 3. WAL Accumulation

**Symptom**: Disk space usage growing rapidly

**Solution**:
```sql
-- Check replication slot lag
SELECT slot_name, confirmed_flush_lsn, pg_current_wal_lsn(),
       pg_current_wal_lsn() - confirmed_flush_lsn AS lag_bytes
FROM pg_replication_slots;

-- If connector is stuck, drop and recreate slot
SELECT pg_drop_replication_slot('debezium_outbox_slot');
```

#### 4. Connector Task Failed

**Check logs**:
```bash
kubectl logs -l app=debezium-connect -n payu-data-platform --tail=100
```

**Common fixes**:
- Verify database connectivity
- Check credentials in Vault/secrets
- Ensure replication permissions
- Validate table names in configuration

## Security Considerations

1. **Secret Management**: All credentials are externalized to Vault/CSI secrets
2. **Network Policies**: Restrict egress to database and Kafka only
3. **Column Masking**: Sensitive columns are excluded or hashed
4. **TLS**: All connections use TLS (SASL_SSL for Kafka)
5. **RBAC**: Service account with minimal permissions

## Performance Tuning

| Parameter | Default | Description |
|-----------|---------|-------------|
| `max.batch.size` | 2048 | Records per batch |
| `max.queue.size` | 8192 | Queue size for buffering |
| `poll.interval.ms` | 1000 | Polling frequency |
| `snapshot.fetch.size` | 10000 | Rows per snapshot fetch |

## References

- [Debezium Documentation](https://debezium.io/documentation/)
- [PostgreSQL Connector](https://debezium.io/documentation/reference/stable/connectors/postgresql.html)
- [Outbox Event Router](https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html)
- [PayU Architecture Guide](../../docs/architecture/ARCHITECTURE.md)
