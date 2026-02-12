# PayU Platform - Infrastructure Summary

## Deployed Components

### Core Infrastructure

| Component | Technology | Version | Status | Namespace |
|-----------|------------|---------|--------|-----------|
| PostgreSQL | Crunchy Postgres | 16 (ubi8-16.4-0) | ✅ Running | payu-dev |
| Cache/Data Grid | Red Hat Data Grid (Infinispan) | 8.5.5-4 | ✅ Running | payu-dev |
| Message Broker | AMQ Streams (Kafka) | 4.0.0 (KRaft) | ✅ Running | payu-dev |
| Kafka UI | AMQ Streams Console | 3.1.0 | ✅ Running | payu-dev |
| Identity/SSO | Red Hat Single Sign-On | 7.6 | ✅ Running | payu-dev |

## Access Endpoints

### External Routes (HTTPS)

| Service | URL | Purpose |
|---------|-----|---------|
| **RHSSO Keycloak** | https://keycloak-payu-dev.apps.cluster.payu.fajjjar.my.id/auth | Identity Management |
| **Kafka Console** | https://kafka-console-payu-dev.apps.cluster.payu.fajjjar.my.id | Kafka UI |
| **Web App** | https://web-app-payu-dev.apps.cluster.payu.fajjjar.my.id | Frontend Application |

### Internal Services

| Service | Endpoint | Port | Purpose |
|---------|----------|------|---------|
| **PostgreSQL Primary** | payu-postgres-primary.payu-dev.svc.cluster.local | 5432 | Primary database (write) |
| **PostgreSQL Replicas** | payu-postgres-replicas.payu-dev.svc.cluster.local | 5432 | Read replicas |
| **Data Grid** | payu-datagrid.payu-dev.svc.cluster.local | 11222 | REST/HotRod API |
| **Redis (alias)** | redis.payu-dev.svc.cluster.local | 6379 | Redis-compatible API |
| **Kafka** | kafka.payu-dev.svc.cluster.local | 9092 | Plaintext bootstrap |
| **Kafka TLS** | kafka.payu-dev.svc.cluster.local | 9093 | TLS bootstrap |
| **Keycloak** | keycloak.payu-dev.svc.cluster.local | 8080/8443 | Internal SSO |

## Credentials

### PostgreSQL
- **Username**: `payu`
- **Password**: Get from secret
  ```bash
  oc get secret payu-postgres-pguser-payu -n payu-dev -o jsonpath='{.data.password}' | base64 -d
  ```

### Keycloak Admin
- **Username**: Get from secret
  ```bash
  oc get secret credential-payu-keycloak -n payu-dev -o jsonpath='{.data.ADMIN_USERNAME}' | base64 -d
  ```
- **Password**: Get from secret
  ```bash
  oc get secret credential-payu-keycloak -n payu-dev -o jsonpath='{.data.ADMIN_PASSWORD}' | base64 -d
  ```

### Database Credentials (for microservices)
```bash
oc get secret db-credentials -n payu-dev -o jsonpath='{.data.password}' | base64 -d
```

## YAML Files Location

All infrastructure YAML files are located in:
```
infrastructure/openshift/examples/
```

### File Structure

```
infrastructure/openshift/examples/
├── 01-namespaces.yaml              # Namespace definitions
├── 02-crunchy-postgres.yaml        # Crunchy Postgres cluster
├── 03-data-grid.yaml               # Red Hat Data Grid (Infinispan)
├── 04-amq-streams.yaml             # AMQ Streams Kafka (KRaft mode)
├── 05-rhsso-keycloak.yaml          # Red Hat Single Sign-On
├── 06-common-configmaps-secrets.yaml  # Shared config
└── 07-amq-streams-console.yaml     # AMQ Streams Console (Kafka UI)
```

## Deployment Commands

### Deploy All Infrastructure
```bash
oc apply -f infrastructure/openshift/examples/
```

### Deploy Individual Components
```bash
# Namespaces
oc apply -f infrastructure/openshift/examples/01-namespaces.yaml

# PostgreSQL
oc apply -f infrastructure/openshift/examples/02-crunchy-postgres.yaml

# Data Grid
oc apply -f infrastructure/openshift/examples/03-data-grid.yaml

# Kafka
oc apply -f infrastructure/openshift/examples/04-amq-streams.yaml

# Keycloak
oc apply -f infrastructure/openshift/examples/05-rhsso-keycloak.yaml

# Kafka Console
oc apply -f infrastructure/openshift/examples/07-amq-streams-console.yaml
```

## Verification Commands

### PostgreSQL
```bash
# Check pods
oc get pods -n payu-dev -l postgres-operator.crunchydata.com/cluster=payu-postgres

# Check cluster status
oc get postgrescluster payu-postgres -n payu-dev
```

### Data Grid
```bash
# Check Infinispan CR
oc get infinispan payu-datagrid -n payu-dev

# Check pods
oc get pods -n payu-dev -l app=infinispan-pod,clusterName=payu-datagrid
```

### Kafka
```bash
# Check Kafka CR
oc get kafka kafka -n payu-dev

# Check topics
oc get kafkatopic -n payu-dev

# Check brokers
oc get pods -n payu-dev -l strimzi.io/kind=Kafka,strimzi.io/name=kafka
```

### Keycloak
```bash
# Check Keycloak CR
oc get keycloak payu-keycloak -n payu-dev

# Check pod
oc get pods -n payu-dev -l app=keycloak
```

### Kafka Console
```bash
# Check Console CR
oc get console payu-kafka-console -n payu-dev

# Check pods
oc get pods -n payu-dev -l app=payu-kafka-console
```

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           PayU Platform Architecture                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                         OpenShift Cluster                                │   │
│  │                                                                          │   │
│  │   ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │   │                     payu-dev Namespace                           │   │   │
│  │   │                                                                   │   │   │
│  │   │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │   │   │
│  │   │  │   RHSSO      │  │    Kafka     │  │Kafka Console │           │   │   │
│  │   │  │  (Keycloak)  │  │(AMQ Streams) │  │    (UI)      │           │   │   │
│  │   │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │   │   │
│  │   │         │                 │                 │                   │   │   │
│  │   │  ┌──────┴──────────────┬──┴─────────────────┴──┐                │   │   │
│  │   │  │                     │                       │                │   │   │
│  │   │  │  ┌──────────────┐   │   ┌──────────────┐   │                │   │   │
│  │   │  │  │Data Grid     │   │   │   Crunchy    │   │                │   │   │
│  │   │  │  │(Infinispan)  │   │   │   Postgres   │   │                │   │   │
│  │   │  │  └──────────────┘   │   └──────────────┘   │                │   │   │
│  │   │  │         │           │           │           │                │   │   │
│  │   │  └─────────┴───────────┴───────────┴───────────┘                │   │   │
│  │   │                            │                                    │   │   │
│  │   │  ┌─────────────────────────┼─────────────────────────────┐     │   │   │
│  │   │  │                         ▼                             │     │   │   │
│  │   │  │   ┌──────────────────────────────────────────────┐   │     │   │   │
│  │   │  │   │           PayU Microservices                  │   │     │   │   │
│  │   │  │   │  account-service │ auth-service              │   │     │   │   │
│  │   │  │   │  transaction │ wallet │ billing              │   │     │   │   │
│  │   │  │   │  gateway-service │ web-app                  │   │     │   │   │
│  │   │  │   └──────────────────────────────────────────────┘   │     │   │   │
│  │   │  │                                                    │     │   │   │
│  │   │  └────────────────────────────────────────────────────┘     │   │   │
│  │   │                                                             │   │   │
│  │   └─────────────────────────────────────────────────────────────┘   │   │
│  │                                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Documentation

- **Deployment Guide**: `docs/operations/INFRASTRUCTURE_DEPLOYMENT.md`
- **Architecture**: `docs/architecture/ARCHITECTURE.md`
- **Troubleshooting**: See troubleshooting section in deployment guide

## Operators Used

| Operator | Purpose | Version |
|----------|---------|---------|
| Crunchy Postgres Operator | PostgreSQL management | v5.x |
| Red Hat Data Grid Operator | Infinispan/Data Grid | v8.5.x |
| AMQ Streams Operator | Kafka management | v2.5.x |
| AMQ Streams Console Operator | Kafka UI | v3.1.x |
| Red Hat Single Sign-On Operator | Keycloak management | v7.6.x |

## Notes

- All infrastructure components are deployed in `payu-dev` namespace
- Kafka is running in KRaft mode (no ZooKeeper required)
- PostgreSQL uses Crunchy Postgres with high availability features
- Data Grid provides Redis-compatible endpoint
- All external access is via HTTPS (OpenShift Routes with edge termination)
