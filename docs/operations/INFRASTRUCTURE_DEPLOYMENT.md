# PayU Platform - Infrastructure Deployment Guide

## Overview

This guide documents the deployment of PayU platform infrastructure on Red Hat OpenShift using Operators and Kubernetes native resources.

## Prerequisites

- OpenShift 4.20+ cluster
- `oc` CLI configured
- Cluster administrator access
- OpenShift Operators available:
  - Crunchy Postgres Operator
  - Red Hat Data Grid Operator
  - AMQ Streams Operator
  - AMQ Streams Console Operator
  - Red Hat Single Sign-On Operator

## Quick Start

```bash
# Login to OpenShift
oc login https://api.cluster.payu.fajjjar.my.id:6443 -u kubeadmin

# Apply all infrastructure components
oc apply -f infrastructure/openshift/examples/
```

## Infrastructure Components

### 1. Namespaces

File: `01-namespaces.yaml`

Creates all required namespaces for the platform:
- `payu-dev` - Development environment
- `payu-sit` - System Integration Testing
- `payu-uat` - User Acceptance Testing
- `payu-preprod` - Pre-production
- `payu-prod` - Production

```bash
oc apply -f infrastructure/openshift/examples/01-namespaces.yaml
oc project payu-dev
```

### 2. Crunchy Postgres

File: `02-crunchy-postgres.yaml`

Enterprise PostgreSQL with high availability, backups, and monitoring.

**Features:**
- PostgreSQL 16 with UBI8 base image
- pgBackRest for backups
- pgBouncer for connection pooling
- Automatic user and database creation

**Deployment:**
```bash
oc apply -f infrastructure/openshift/examples/02-crunchy-postgres.yaml
```

**Verification:**
```bash
# Check PostgreSQL pods
oc get pods -n payu-dev -l postgres-operator.crunchydata.com/cluster=payu-postgres

# Get connection details
oc get secret payu-postgres-pguser-payu -n payu-dev
```

**Services:**
| Service | Endpoint | Purpose |
|---------|----------|---------|
| payu-postgres-primary | ClusterIP | Primary database (write) |
| payu-postgres-replicas | ClusterIP | Read replicas |
| payu-postgres-ha | ClusterIP | High availability endpoint |
| payu-postgres-pgbouncer | ClusterIP | Connection pooling |

### 3. Red Hat Data Grid (Infinispan)

File: `03-data-grid.yaml`

Distributed caching and data grid solution.

**Features:**
- Redis-compatible API (port 11222 → 6379)
- Distributed caching
- Session management
- High availability

**Deployment:**
```bash
oc apply -f infrastructure/openshift/examples/03-data-grid.yaml
```

**Redis Compatibility:**
The service `redis` provides a Redis-compatible endpoint at `redis:6379`, mapping to Data Grid's RESP endpoint.

### 4. AMQ Streams (Kafka)

File: `04-amq-streams.yaml`

Event streaming platform with Kafka 4.0 in KRaft mode (no ZooKeeper).

**Features:**
- Kafka 4.0.0
- KRaft mode (no ZooKeeper required)
- Automatic topic creation
- Entity Operator for topic/user management

**Deployment:**
```bash
oc apply -f infrastructure/openshift/examples/04-amq-streams.yaml
```

**Verification:**
```bash
# Check Kafka cluster
oc get kafka -n payu-dev

# Check topics
oc get kafkatopic -n payu-dev
```

**Bootstrap Servers:**
- Plaintext: `kafka:9092`
- TLS: `kafka:9093`

### 5. AMQ Streams Console (Kafka UI)

File: `07-amq-streams-console.yaml`

Web-based UI for managing and monitoring Apache Kafka clusters.

**Features:**
- Browse Kafka topics and messages
- View consumer groups and offsets
- Produce messages to topics
- Monitor broker health
- View cluster configuration

**Prerequisites:**
1. Kafka cluster must have a listener with authentication enabled
2. KafkaUser must be created with matching authentication type
3. Console must reference the KafkaUser in its configuration

**Deployment:**
```bash
# Apply the KafkaUser first
oc apply -f infrastructure/openshift/examples/07-amq-streams-console.yaml
```

**Configuration Requirements:**

1. **Enable authentication on Kafka listener:**
```yaml
listeners:
  - name: plain
    port: 9092
    type: internal
    tls: false
    authentication:
      type: scram-sha-512
```

2. **Create KafkaUser for Console:**
```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaUser
metadata:
  name: payu-kafka-console-user
  labels:
    strimzi.io/cluster: kafka
spec:
  authentication:
    type: scram-sha-512
```

3. **Configure Console with credentials:**
```yaml
kafkaClusters:
  - name: kafka
    namespace: payu-dev
    listener: plain
    credentials:
      kafkaUser:
        name: payu-kafka-console-user
```

**Access:**
- URL: `https://kafka-console-payu-dev.apps.cluster.payu.fajjjar.my.id`
- Authentication: Anonymous (configurable with OIDC)

**Troubleshooting:**
- If "0 Connected Kafka clusters" appears:
  1. Check KafkaUser exists: `oc get kafkauser payu-kafka-console-user`
  2. Check secret exists: `oc get secret payu-kafka-console-user`
  3. Check Kafka listener has matching auth type
  4. Check Console pod logs

**CRD:**
- `consoles.console.streamshub.github.com/v1alpha1`

### 6. Red Hat Single Sign-On (RHSSO)

File: `05-rhsso-keycloak.yaml`

Enterprise identity and access management.

**Features:**
- RHSSO 7.6 (Keycloak-based)
- External database (Crunchy Postgres)
- OpenID Connect support
- Automatic route creation

**Deployment:**
```bash
# Apply database secret first
oc apply -f infrastructure/openshift/examples/05-rhsso-keycloak.yaml

# Get admin credentials
oc get secret credential-payu-keycloak -n payu-dev -o jsonpath='{.data.ADMIN_USERNAME}' | base64 -d
oc get secret credential-payu-keycloak -n payu-dev -o jsonpath='{.data.ADMIN_PASSWORD}' | base64 -d
```

**Access:**
- URL: `https://keycloak-payu-dev.apps.cluster.payu.fajjjar.my.id/auth`
- Realm: `payu`

### 7. Common ConfigMaps and Secrets

File: `06-common-configmaps-secrets.yaml`

Shared configuration across all services.

**Resources:**
- `db-credentials` - Database connection
- `jwt-secret` - JWT signing
- `encryption-keys` - Data encryption
- `kafka-config` - Kafka settings
- `spring-config` - Spring Boot defaults
- `oidc-config` - OIDC/Keycloak settings
- `service-endpoints` - Internal service URLs

## Service Dependencies

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Infrastructure                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │    Kafka     │  │    Redis     │  │   Keycloak   │  │Kafka Console │     │
│  │  (AMQ Streams)│  │  (Data Grid) │  │   (RHSSO)    │  │(AMQ Streams) │     │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘     │
│         │                │                  │                  │            │
│         └────────────────┼──────────────────┴──────────────────┘            │
│                          │                                                   │
│  ┌──────────────────────────────────────────────────────────────┐          │
│  │                     PostgreSQL                                │          │
│  │                 (Crunchy Postgres)                            │          │
│  └──────────────────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Application Layer                       │
├─────────────────────────────────────────────────────────────┤
│  account-service │ auth-service │ transaction-service        │
│  wallet-service  │ billing-service │ notification-service    │
│  gateway-service │ web-app │ kyc-service │ analytics-service │
└─────────────────────────────────────────────────────────────┘
```

## Environment Variables for Services

### Database Connection
```yaml
env:
  - name: SPRING_DATASOURCE_URL
    value: "jdbc:postgresql://payu-postgres-primary:5432/payu_<service>"
  - name: SPRING_DATASOURCE_USERNAME
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: username
  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: password
```

### Kafka Configuration
```yaml
env:
  - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
    value: "kafka:9092"
```

### Keycloak Integration
```yaml
env:
  - name: KEYCLOAK_AUTH_SERVER_URL
    value: "http://keycloak.payu-dev.svc:8080/auth"
  - name: KEYCLOAK_REALM
    value: "payu"
```

## Troubleshooting

### PostgreSQL Connection Issues
```bash
# Check PostgreSQL pod status
oc get pods -n payu-dev -l postgres-operator.crunchydata.com/cluster=payu-postgres

# Check logs
oc logs payu-postgres-instance1-xxx -c database -n payu-dev

# Verify secret
oc get secret payu-postgres-pguser-payu -n payu-dev -o jsonpath='{.data.password}' | base64 -d
```

### Data Grid Not Responding
```bash
# Check Data Grid status
oc get infinispan payu-datagrid -n payu-dev

# Check pod logs
oc logs payu-datagrid-0 -n payu-dev
```

### Kafka Connection Failures
```bash
# Check Kafka cluster status
oc get kafka kafka -n payu-dev

# Check broker logs
oc logs kafka-broker-0 -n payu-dev
```

### RHSSO Startup Issues
```bash
# Check Keycloak CR status
oc describe keycloak payu-keycloak -n payu-dev

# Check pod logs
oc logs keycloak-0 -n payu-dev

# Verify database secret
oc get secret keycloak-db-secret -n payu-dev
```

## Production Considerations

### High Availability
- Increase PostgreSQL replicas to 3
- Use persistent storage for Kafka
- Deploy multiple Keycloak instances
- Configure Data Grid with replicas >= 3

### Backup and Recovery
- Configure pgBackRest S3 backups
- Enable Kafka topic replication
- Regular etcd backups

### Security
- Enable TLS for all components
- Configure network policies
- Use sealed secrets for sensitive data
- Enable audit logging

### Monitoring
- Install OpenShift monitoring stack
- Configure alerts for infrastructure
- Set up Grafana dashboards

## References

- [Crunchy Postgres Documentation](https://access.crunchydata.com/documentation/postgres-operator/v5/)
- [Red Hat Data Grid](https://access.redhat.com/documentation/en-us/red_hat_data_grid/8.4/)
- [AMQ Streams](https://access.redhat.com/documentation/en-us/red_hat_amq_streams/2.5/)
- [Red Hat Single Sign-On](https://access.redhat.com/documentation/en-us/red_hat_single_sign-on/7.6/)
