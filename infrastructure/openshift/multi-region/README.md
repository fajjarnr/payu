# Multi-Region Active-Passive Failover Configuration

This directory contains the complete configuration for running PayU Digital Banking Platform across multiple OpenShift regions with Active-Passive failover capabilities.

## Architecture Overview

```
Primary Region (Active)          Secondary Region (Passive/DR)
┌─────────────────────────┐      ┌─────────────────────────┐
│                         │      │                         │
│  All Services (3x)      │      │  All Services (0x)      │
│  ────────────────       │      │  ────────────────       │
│  account-service        │      │  account-service        │
│  auth-service           │      │  auth-service           │
│  transaction-service    │      │  transaction-service    │
│  wallet-service         │─────▶│  wallet-service         │
│  billing-service        │      │  billing-service        │
│  notification-service   │      │  notification-service   │
│  gateway-service        │      │  gateway-service        │
│  kyc-service            │      │  kyc-service            │
│  analytics-service      │      │  analytics-service      │
│  compliance-service     │      │  compliance-service     │
│                         │      │                         │
│  PostgreSQL (Primary)   │─────▶│  PostgreSQL (Standby)   │
│  Kafka (Primary)        │─────▶│  Kafka (Mirror)         │
│  Redis (Primary)        │─────▶│  Redis (Replica)        │
└─────────────────────────┘      └─────────────────────────┘
           │                               │
           └───────────┬───────────────────┘
                       ▼
              Global DNS / Load Balancer
```

## Directory Structure

```
infrastructure/openshift/multi-region/
├── primary/
│   └── deployment.yaml              # Primary region full deployment
├── secondary/
│   └── deployment.yaml              # Secondary region (replica: 0)
├── replication/
│   ├── postgres-replication.yaml    # PostgreSQL logical replication
│   └── kafka-mirroring.yaml         # Kafka MirrorMaker2 config
├── failover/
│   └── failover-job.yaml            # Automated failover/failback jobs
├── monitoring/
│   └── replication-lag-service-monitor.yaml  # Monitoring & alerts
└── README.md                         # This file
```

## Components

### 1. Primary Region (`primary/deployment.yaml`)

All services deployed with full capacity:
- **Spring Boot Services**: 3 replicas each (account, auth, transaction, wallet, compliance)
- **Quarkus Services**: 2 replicas each (billing, notification, gateway)
- **Python Services**: 1-2 replicas each (kyc, analytics)
- **PostgreSQL**: Primary with logical replication enabled
- **Kafka**: 3-node cluster with MirrorMaker2
- **Redis**: Primary master

### 2. Secondary Region (`secondary/deployment.yaml`)

Passive standby region:
- **All services**: Deployed but scaled to 0 replicas
- **PostgreSQL**: Hot standby with continuous replication
- **Kafka**: 3-node cluster receiving mirrored data
- **Redis**: Replica of primary

### 3. Data Replication

#### PostgreSQL Logical Replication

Located in `replication/postgres-replication.yaml`:

- **Publication**: Created on primary for all tables
- **Subscription**: Configured on secondary
- **Monitoring**: CronJob checks replication lag every 5 minutes
- **Metrics**: PostgreSQL exporter exposes lag metrics

**Key Settings**:
```yaml
wal_level = logical
max_replication_slots = 10
max_logical_replication_workers = 8
```

**Monitoring Query**:
```sql
SELECT sum(lag_bytes) FROM pg_stat_subscription;
```

#### Kafka MirrorMaker2

Located in `replication/kafka-mirroring.yaml`:

- **MirrorMaker2**: Replicates all topics (except `__.*` internal topics)
- **Replication Policy**: IdentityReplicationPolicy (preserves topic names)
- **Configuration**:
  - Sync topic offsets: Enabled (5s interval)
  - Sync group offsets: Enabled (5s interval)
  - Heartbeats: Enabled

**Health Check**:
```bash
kafka-consumer-groups.sh --bootstrap-server secondary:9092 --describe --group mirror-maker
```

### 4. Failover Automation

Located in `failover/failover-job.yaml`:

#### Failover Process (Primary → Secondary)

1. **Pre-flight Checks**
   - Verify secondary region connectivity
   - Check PostgreSQL replication lag
   - Verify Kafka mirror status

2. **Stop Replication**
   - Scale down MirrorMaker2
   - Disable PostgreSQL subscription

3. **Promote Secondary Database**
   - Execute `pg_promote()`
   - Create publication for reverse replication

4. **Scale Up Services**
   - Scale all deployments to full capacity
   - Wait for readiness probes to pass

5. **Update DNS**
   - Update external DNS records
   - Switch load balancer traffic

6. **Health Verification**
   - Test critical endpoints
   - Verify service health

**Execution**:
```bash
kubectl create -f failover/failover-job.yaml
```

**Manual Trigger**:
```bash
kubectl create job dr-failover --from=cronjob/dr-failover-primary-to-secondary
```

#### Failback Process (Secondary → Primary)

1. Verify primary region health
2. Set up reverse replication
3. Wait for data sync
4. Scale down secondary services
5. Scale up primary services
6. Update DNS back to primary

**RBAC Permissions**:
ServiceAccount `failover-sa` with `ClusterRole` `failover-role` has permissions to:
- Scale deployments and statefulsets
- Execute commands in pods
- Update routes and DNS

### 5. Monitoring & Alerting

Located in `monitoring/replication-lag-service-monitor.yaml`:

#### ServiceMonitors

- **PostgreSQL Replication**: Metrics on port 9187
- **Kafka MirrorMaker2**: JMX metrics on port 9404
- **Application Services**: Actuator metrics

#### Alerting Rules

| Alert | Severity | Threshold | Duration |
|-------|----------|-----------|----------|
| `PostgresReplicationLagHigh` | Warning | > 1GB | 5m |
| `PostgresReplicationLagCritical` | Critical | > 5GB | 2m |
| `KafkaReplicationLagHigh` | Warning | > 10K records | 5m |
| `KafkaReplicationLagCritical` | Critical | > 100K records | 2m |
| `RegionUnhealthy` | Warning | < 80% services up | 5m |
| `FailoverNotReady` | Warning | High lag | 2m |

#### Grafana Dashboard

Included in the monitoring config:
- Replication lag graphs
- Throughput metrics
- Service health by region
- Replication status table

## Deployment Guide

### Prerequisites

1. Two OpenShift 4.20+ clusters (primary and secondary regions)
2. Network connectivity between clusters (VPN or VPC peering)
3. External DNS/LB for routing (AWS Route53, Azure Traffic Manager, etc.)
4. S3-compatible storage for backups (optional)

### Initial Deployment

#### 1. Deploy Primary Region

```bash
# Login to primary cluster
oc login --server=primary-cluster.example.com

# Apply configuration
oc apply -f infrastructure/openshift/multi-region/primary/deployment.yaml

# Verify
oc get pods -n payu-prod
oc get statefulset -n payu-prod
```

#### 2. Deploy Secondary Region

```bash
# Login to secondary cluster
oc login --server=secondary-cluster.example.com

# Apply configuration
oc apply -f infrastructure/openshift/multi-region/secondary/deployment.yaml

# Verify all pods are scaled to 0
oc get deployments -n payu-prod
```

#### 3. Configure Replication

```bash
# On primary cluster
oc apply -f infrastructure/openshift/multi-region/replication/postgres-replication.yaml
oc apply -f infrastructure/openshift/multi-region/replication/kafka-mirroring.yaml

# Verify PostgreSQL publication
oc exec -n payu-prod postgres-primary-0 -- psql -U payu -d payudb -c "\dRp+"

# Verify Kafka MirrorMaker2
oc get kafkamirrormaker2 -n payu-prod
```

#### 4. Deploy Monitoring

```bash
# On secondary cluster (where monitoring typically runs)
oc apply -f infrastructure/openshift/multi-region/monitoring/replication-lag-service-monitor.yaml

# Verify ServiceMonitors
oc get servicemonitor -n payu-prod
```

#### 5. Prepare Failover

```bash
# Apply failover resources (can be applied to either cluster)
oc apply -f infrastructure/openshift/multi-region/failover/failover-job.yaml

# Verify permissions
oc auth can-i scale deployment --as=system:serviceaccount:payu-prod:failover-sa
```

### Ongoing Operations

#### Monitoring Replication Health

```bash
# Check PostgreSQL replication lag
oc exec -n payu-prod postgres-secondary-0 -- psql -U payu -d payudb -c \
  "SELECT slot_name, pg_wal_lag_diff(pg_current_wal_lsn(), replay_lsn) as lag_bytes FROM pg_stat_replication;"

# Check Kafka consumer lag
oc exec -n payu-prod kafka-secondary-kafka-0 -- kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group mirrormaker2-cluster
```

#### Testing Failover

```bash
# 1. Run failover job
oc create job dr-failover-test --from=cronjob/dr-failover-primary-to-secondary -n payu-prod

# 2. Monitor job progress
oc logs -f job/dr-failover-test -n payu-prod

# 3. Verify services are running on secondary
oc get pods -n payu-prod -l region=secondary

# 4. Test application endpoints
curl https://secondary-gateway.example.com/actuator/health
```

#### Testing Failback

```bash
# 1. Run failback job
oc create job dr-failback-test --from=cronjob/dr-failback-secondary-to-primary -n payu-prod

# 2. Monitor progress
oc logs -f job/dr-failback-test -n payu-prod

# 3. Verify services are back on primary
oc get pods -n payu-prod -l region=primary
```

## Network Configuration

### Inter-Cluster Connectivity

Ensure the following ports are open between clusters:

| Service | Port | Protocol | Purpose |
|---------|------|----------|---------|
| PostgreSQL | 5432 | TCP | Logical replication |
| Kafka | 9092, 9093 | TCP | MirrorMaker2 |
| Redis | 6379 | TCP | Replication |

### DNS Configuration

Configure external DNS with health-based routing:

**AWS Route53 Example**:
```json
{
  "RoutingPolicy": "LATENCY",
  "HealthCheck": {
    "Type": "HTTPS",
    "ResourcePath": "/actuator/health",
    "FullyQualifiedDomainName": "primary-gateway.example.com"
  }
}
```

## Disaster Recovery Procedures

### Scenario 1: Primary Region Outage

1. **Detection**
   - Alert: `RegionUnhealthy` or `ActiveRegionServicesDown`
   - Manual verification via console

2. **Decision**
   - If primary unrecoverable > 1 hour: Initiate failover
   - If quick recovery expected: Wait

3. **Execution**
   ```bash
   oc create job dr-failover-$(date +%s) \
     --from=cronjob/dr-failover-primary-to-secondary -n payu-prod
   ```

4. **Verification**
   - Monitor job logs
   - Test application endpoints
   - Notify teams

5. **Post-Failover**
   - Update all documentation
   - Monitor performance closely
   - Plan for failback

### Scenario 2: Planned Migration

1. **Preparation**
   - Schedule maintenance window
   - Notify stakeholders
   - Ensure replication lag < 1GB

2. **Execution**
   - Run failover job
   - Monitor all services
   - Update DNS TTL

3. **Verification**
   - End-to-end testing
   - Performance monitoring
   - Data integrity checks

### Scenario 3: Failback After Recovery

1. **Preparation**
   - Ensure primary cluster is healthy
   - Check available capacity
   - Schedule maintenance window

2. **Execution**
   ```bash
   oc create job dr-failback-$(date +%s) \
     --from=cronjob/dr-failback-secondary-to-primary -n payu-prod
   ```

3. **Verification**
   - Monitor data sync
   - Test all services
   - Update DNS

## Cost Optimization

### Secondary Region (Passive)

Compute costs minimized by:
- Scaling all services to 0 replicas
- Only running infrastructure (DB, Kafka, Redis)

Estimated cost savings: **~70%** compared to active-active

### Storage Costs

- Primary: 500GB PostgreSQL + 500GB Kafka = ~1TB
- Secondary: 500GB PostgreSQL + 500GB Kafka = ~1TB
- Total: 2TB replicated storage

## Troubleshooting

### PostgreSQL Replication Issues

**Problem**: High replication lag

```bash
# Check replication status
oc exec -n payu-prod postgres-secondary-0 -- psql -U payu -d payudb -c \
  "SELECT * FROM pg_stat_subscription;"

# Restart subscription
oc exec -n payu-prod postgres-secondary-0 -- psql -U payu -d payudb -c \
  "ALTER SUBSCRIPTION payu_subscription RESTART;"
```

**Problem**: Replication connection lost

```bash
# Check primary is accepting connections
oc exec -n payu-prod postgres-primary-0 -- psql -U payu -d payudb -c \
  "SELECT * FROM pg_stat_replication;"

# Verify replication slot exists
oc exec -n payu-prod postgres-primary-0 -- psql -U payu -d payudb -c \
  "SELECT * FROM pg_replication_slots;"
```

### Kafka Mirroring Issues

**Problem**: MirrorMaker2 not replicating

```bash
# Check connector status
oc get kafkacconnector -n payu-prod

# Check MirrorMaker2 logs
oc logs -n payu-prod kafka-mirror-primary-to-secondary-0 -c mirrormaker2

# Restart MirrorMaker2
oc delete pod -n payu-prod -l strimzi.io/kind=KafkaMirrorMaker2
```

**Problem**: High consumer lag

```bash
# Check consumer group lag
oc exec -n payu-prod kafka-secondary-kafka-0 -- kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group mirrormaker2-cluster

# Check topic sizes
oc exec -n payu-prod kafka-secondary-kafka-0 -- kafka-topics.sh \
  --bootstrap-server localhost:9092 --describe
```

### Failover Issues

**Problem**: Failover job hangs

```bash
# Check job status
oc describe job/dr-failover-xxx -n payu-prod

# Check pod logs
oc logs -f job/dr-failover-xxx -n payu-prod

# Manual steps if automated fails
oc scale deployment/account-service --replicas=3 -n payu-prod
```

## Security Considerations

1. **Replication Credentials**: Use Kubernetes secrets
2. **TLS Encryption**: Enable for all inter-cluster communication
3. **Network Policies**: Restrict replication traffic
4. **RBAC**: Least privilege for failover service account
5. **Audit Logging**: Enable for all failover operations

## Compliance & Governance

- **Data Residency**: Ensure secondary region meets jurisdiction requirements
- **Audit Trail**: Log all failover activities
- **Testing**: Quarterly failover drills recommended
- **Documentation**: Update runbooks after each event

## References

- [OpenShift Multi-Cluster Documentation](https://docs.openshift.com/container-platform/4.20/architecture/control_plane.html)
- [PostgreSQL Logical Replication](https://www.postgresql.org/docs/16/logical-replication.html)
- [Kafka MirrorMaker2](https://kafka.apache.org/documentation/#mirrormaker)
- [Disaster Recovery Best Practices](https://docs.openshift.com/container-platform/4.20/backup_and_restore/index.html)

## Support

For issues or questions:
- Platform Team: platform-team@payu.id
- Architecture: architect@payu.id
- On-Call: +62-21-XXXX-XXXX

---

**Last Updated**: January 2026
**Version**: 1.0.0
