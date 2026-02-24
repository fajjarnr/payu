# PayU Platform - Infrastructure Deployment Guide

> **Panduan instalasi, konfigurasi, dan ringkasan infrastruktur PayU pada Red Hat OpenShift.**

---

## 📊 Infrastructure Summary (SIT/Dev)

Berikut adalah status dan endpoint infrastruktur yang berjalan di namespace `payu-dev`.

### Deployed Components
| Component | Technology | Version | Status |
|-----------|------------|---------|--------|
| PostgreSQL | Crunchy Postgres | 16 (ubi8) | ✅ Running |
| Cache/Data Grid | Red Hat Data Grid | 8.5.5 | ✅ Running |
| Message Broker | AMQ Streams (Kafka)| 4.0.0 (KRaft) | ✅ Running |
| Identity/SSO | Red Hat Single Sign-On | 7.6 | ✅ Running |
| Kafka UI | AMQ Streams Console | 3.1.0 | ✅ Running |

### Access Endpoints
| Service | URL / Endpoint | Port | Purpose |
|---------|----------------|------|---------|
| **Web App** | https://web-app-payu-dev.apps.... | 443 | Consumer UI |
| **Keycloak Admin** | https://keycloak-payu-dev.apps... | 443 | IAM Console |
| **Kafka Console** | https://kafka-console-payu-dev... | 443 | Kafka UI |
| **Postgres (Internal)** | `payu-postgres-primary` | 5432 | Primary DB |
| **Kafka (Internal)** | `kafka:9092` | 9092 | Event Bus |
| **Redis (Internal)** | `redis:6379` | 6379| Cache (RESP) |

### Credentials Access
- **Postgres**: `oc get secret payu-postgres-pguser-payu -o jsonpath='{.data.password}' | base64 -d`
- **Keycloak Admin**: `oc get secret credential-payu-keycloak -o jsonpath='{.data.ADMIN_PASSWORD}' | base64 -d`

---

## 🚀 Quick Start Deployment

Semua manifest infrastruktur berada di folder `infrastructure/openshift/examples/`.

```bash
# Login ke OpenShift
oc login https://api.cluster.payu.fajjjar.my.id:6443

# Deploy semua komponen infrastruktur secara berurutan
oc apply -f infrastructure/openshift/examples/01-namespaces.yaml
oc project payu-dev
oc apply -f infrastructure/openshift/examples/
```

---

## 🏗️ Detailed Component Configuration

### 1. Crunchy Postgres (PostgreSQL 16)
Enterprise PostgreSQL dengan HA (High Availability) dan backup terintegrasi via pgBackRest.
- **Verification**: `oc get postgrescluster payu-postgres`
- **Primary Service**: `payu-postgres-primary`

### 2. Red Hat Data Grid (Infinispan)
Digunakan sebagai caching layer dengan kompatibilitas protokol Redis (RESP).
- **Service Alias**: `redis:6379` dipetakan ke endpoint RESP Data Grid.

### 3. AMQ Streams (Kafka 4.0)
Event streaming dalam mode **KRaft** (tanpa Zookeeper).
- **Bootstrap Servers**: `kafka:9092` (Plain), `kafka:9093` (TLS).
- **Verification**: `oc get kafka kafka`.

### 4. Red Hat SSO (RHSSO 7.6)
Sistem Identity & Access Management berbasis Keycloak.
- **Realm**: `payu`
- **Auth Server URL**: `http://keycloak.payu-dev.svc:8080/auth` (Internal)

---

## 🔗 Service Dependencies

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
```

---

## 🛠️ Performance & Scalability (Production)

Untuk deployment produksi, pastikan hal berikut dikonfigurasi:
- **HA**: Minimal 3 replica untuk Postgres, Data Grid, dan Kafka.
- **Backup**: Aktifkan pgBackRest S3 backups.
- **Monitoring**: Sambungkan ke Prometheus/Grafana via OpenShift Monitoring.
- **Storage**: Gunakan ReadWriteOnce (RWO) storage class yang cepat (SSD) untuk database pods.

---

## 🆘 Troubleshooting

- **Postgres Connection**: Pastikan `pgBouncer` tidak penuh (cek `max_connections`).
- **Kafka Connectivity**: Jika producer timeout, cek ACL dan KafkaUser sudah diaplikasikan.
- **Keycloak 404**: Cek apakah `/auth` prefix sudah disertakan (RHSSO memerlukan prefix ini, berbeda dengan Keycloak 22+).

---
_Last Updated: February 24, 2026_
