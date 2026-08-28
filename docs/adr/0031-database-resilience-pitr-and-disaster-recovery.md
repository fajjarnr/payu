# ADR-0031: Database High-Availability, Continuous Point-In-Time Recovery (PITR) & Disaster Recovery Standard

**Status**: Accepted  
**Date**: 2026-08-18  
**Deciders**: Principal Architect, Data Architect, Platform Engineer, Cybersecurity Architect  
**Technical Standards**: CloudNativePG (CNPG 1.30+), Barman Cloud S3, PostgreSQL 16.8, POJK No. 11/POJK.03/2022, Bank Indonesia SNAP-BI  
**Related Requirements**: `PARTNER-PROD-008`, `ACCOUNT-007`, `INFRA-026`, `ADR-0006`, `ADR-0022`

---

## 1. Context & Business Problem

PayU beroperasi sebagai platform *core banking* dan *payment gateway* (melayani integrasi eksternal TokoBapak & Nobar dengan standar **SNAP-BI**). Dalam ekosistem perbankan digital, database adalah *single source of truth* untuk seluruh pergerakan dana, mutasi saldo, pencatatan jurnal akuntansi ganda (*double-entry ledger*), dan rekonsiliasi.

Kegagalan sistem data layer (akibat *node failure*, *disk corruption*, *human operational error*, atau *datacenter disaster*) tidak boleh menyebabkan:
1. **Kehilangan data transaksi finansial yang telah dikonfirmasi** (*Zero Data Loss / RPO = 0*).
2. **Downtime berkepanjangan** yang melanggar SLA perbankan 99.99% (*RTO < 5 menit*).
3. **Inkonsistensi data saat failover** (*Split-Brain* atau *Stale Read*).
4. **Ketidakmampuan me-restore data ke titik waktu tertentu** (*Point-In-Time Recovery / PITR*).

### Regulasi & Kepatuhan:
- **POJK No. 11/POJK.03/2022**: Penyelenggara sistem elektronik perbankan wajib memiliki Pusat Data (DC) dan Pusat Pemulihan Bencana (DRC), menguji rencana pemulihan bencana (*Disaster Recovery Plan / DRP*) secara berkala, dan menetapkan RTO/RPO terukur.
- **Standar SNAP-BI (Bank Indonesia)**: Mempersyaratkan keandalan sistem pembayaran dengan integritas mutlak pada *idempotency* dan *reconciliation*.
- **POJK Tata Kelola Data & UU PDP**: Retensi data audit pembukuan minimum 5–7 tahun dengan enkripsi *at-rest* & *in-transit*.

---

## 2. Decision Drivers

- **Zero Data Loss (RPO = 0)**: Transaksi debit/kredit yang telah di-*commit* tidak boleh hilang meski satu Availability Zone musnah.
- **Fast Automatic Failover (RTO < 5m)**: Deteksi *primary node failure* dan promosi *standby* tanpa intervensi manual (< 15–30 detik).
- **Sub-Second Continuous PITR**: Kemampuan *point-in-time recovery* hingga level detik via *continuous WAL streaming* ke *offsite object storage* (S3).
- **Kubernetes-Native Architecture**: Integrasi penuh dengan **CloudNativePG (CNPG 1.30+)** di Red Hat OpenShift 4.22+ dan standardisasi target AWS RDS Multi-AZ / Aurora PostgreSQL di cloud.
- **Verifiable DR Rehearsals**: Prosedur *restore drill* otomatis yang dapat diuji berkala tanpa mengganggu *live workload*.

---

## 3. Architecture & Topology Design

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                             OpenShift 4.22+ Multi-AZ Topology                               │
│                                                                                             │
│  ┌───────────────────────┐   ┌───────────────────────┐   ┌───────────────────────┐          │
│  │     AZ-a (Zone 1)     │   │     AZ-b (Zone 2)     │   │     AZ-c (Zone 3)     │          │
│  │  ┌─────────────────┐  │   │  ┌─────────────────┐  │   │  ┌─────────────────┐  │          │
│  │  │  payu-database-1│  │   │  │  payu-database-2│  │   │  │  payu-database-3│  │          │
│  │  │   [ PRIMARY ]   │──┼───┼─▶│   [ STANDBY ]   │──┼───┼─▶│   [ STANDBY ]   │  │          │
│  │  │  (gp3-csi 20Gi) │  │   │  │  (gp3-csi 20Gi) │  │   │  │  (gp3-csi 20Gi) │  │          │
│  │  └────────┬────────┘  │   │  └─────────────────┘  │   │  └─────────────────┘  │          │
│  └───────────┼───────────┘   └───────────────────────┘   └───────────────────────┘          │
│              │ (Sync Quorum: 1 standby ack required before commit)                          │
│              ▼                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────────────────────┐  │
│  │ Continuous WAL Streaming (archive_timeout=60s) + Daily VolumeSnapshot / Physical Base │  │
│  └───────────────────────────────────┬───────────────────────────────────────────────────┘  │
└──────────────────────────────────────┼──────────────────────────────────────────────────────┘
                                       │ Barman Cloud S3 Plugin / IAM Role
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                          Offsite Object Storage (AWS S3 / Ceph RG)                          │
│                                                                                             │
│  ┌─────────────────────────┐   ┌─────────────────────────┐   ┌───────────────────────────┐  │
│  │  S3 Bucket: payu-wal-s3 │   │ S3: payu-base-snapshots │   │ S3 Glacier (Cold Archive) │  │
│  │  - Continuous WAL files │   │ - Daily base backups    │   │ - 7-year retention        │  │
│  │  - Retention: 30 days   │   │ - Retention: 365 days   │   │ - Immutable Vault Lock    │  │
│  └─────────────────────────┘   └─────────────────────────┘   └───────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Key Architectural Decisions

### 4.1. Zero-Data Loss Replication (RPO = 0)
1. **Synchronous Quorum Configuration**:
   ```yaml
   postgresql:
     parameters:
       synchronous_commit: "on"
       archive_timeout: "60s"
       wal_level: "replica"
     synchronous:
       method: "any"
       number: 1
       failoverQuorum: true
   ```
   - Setiap transaksi finansial *wajib* mendapatkan *acknowledgement* (write + flush) dari minimal 1 *standby node* sebelum transaksi dinyatakan sukses (`COMMIT 200 OK`).
   - Dengan 3 *instances* tersebar di 3 AZ (*hard PodAntiAffinity*), kegagalan 1 AZ tidak akan pernah menyebabkan *data loss* atau *cluster freeze*.

2. **Dedicated WAL Volume**:
   - Menghindari I/O contention antara *table data* dan *transaction logs*.
   - Data Volume: 20Gi (`gp3-csi`), WAL Volume: 5Gi (`gp3-csi` dedicated fast I/O).

### 4.2. Dual Backup Engine (Continuous PITR + Fast VolumeSnapshot)
Sesuai best practice industri, PayU mengadopsi pendekatan **kombinasi dua lapis backup**:

1. **Lapis 1 — Continuous WAL Archiving via Barman Cloud (RPO = 0 / Sub-Minute PITR)**:
   - Plugin Barman Cloud CNPG secara *real-time* mengunggah segment WAL (16MB atau maksimal tiap 60 detik) ke S3-compatible storage terenkripsi KMS.
   - Memungkinkan pemulihan ke *exact timestamp* (misal: `"2026-08-18 14:35:22.100 UTC"` tepat sebelum terjadi kesalahan fatal/data corrupt).

2. **Lapis 2 — Daily CSI VolumeSnapshot (Fast Baseline Backup)**:
   - Scheduled Snapshot tiap pukul 01:00 UTC dijalankan langsung di *standby node* (`target: prefer-standby`) tanpa membebani performa Primary.
   - Mempercepat waktu *restore* awal (*base image clone*) sebelum me-replay sisa WAL dari S3.

### 4.3. Client & Connection Resilience
1. **Service Endpoint Separation**:
   - `payu-database-rw`: Rute otomatis ke instance Primary (Port 5432).
   - `payu-database-ro`: Rute *round-robin* ke seluruh instance Standby untuk *read-heavy queries* / pelaporan.
   - `payu-database-r`: Rute ke semua pod (Primary + Standby).

2. **Application Connection Pool (HikariCP / datasource-starter)**:
   - URL JDBC: `jdbc:postgresql://payu-database-rw:5432/payu_{service}?targetServerType=primary&connectTimeout=3&socketTimeout=5&loginTimeout=3`
   - `maxLifetime = 60000ms` (1 menit): Memaksa socket pool untuk me-refresh koneksi secara berkala, memastikan koneksi segera berpindah ke Primary baru pasca failover tanpa *stale socket deadlock*.

3. **Financial Replay & Idempotency Safeguard**:
   - Failover di tengah transaksi dilindungi oleh `X-Idempotency-Key`, tabel `outbox_events`, dan `ShedLock`.
   - Jika koneksi terputus saat commit ambigu, client/partner akan mengirim ulang request dengan idempotency key yang sama; database akan mengembalikan hasil asli yang sudah tersimpan tanpa duplikasi debet/kredit.

### 4.4. Retention & Archival Lifecycle (Kepatuhan POJK 11/2022)

| Kategori Data | Lokasi Penyimpanan | Retensi | Mekanisme Enkripsi |
| :--- | :--- | :--- | :--- |
| **Continuous WAL Logs** | AWS S3 Standard / Ceph | 30 Hari | AWS KMS (SSE-KMS) / AES-256 |
| **Daily Full Backups** | AWS S3 Standard-IA | 365 Hari | AWS KMS (SSE-KMS) |
| **Year-End Ledger Archives** | AWS S3 Glacier Flexible | **7 Tahun** | S3 Glacier Object Lock (Compliance Mode / WORM) |

---

## 5. Implementation Specification

### 5.1. Cluster Manifest (`ha-patch.yaml`)
```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: payu-database
  namespace: payu-dev
spec:
  instances: 3
  imageName: ghcr.io/cloudnative-pg/postgresql:16.8
  
  storage:
    storageClass: gp3-csi
    size: 20Gi
  walStorage:
    storageClass: gp3-csi
    size: 5Gi

  postgresql:
    parameters:
      shared_buffers: "1GB"
      effective_cache_size: "3GB"
      work_mem: "16MB"
      maintenance_work_mem: "256MB"
      wal_level: "replica"
      archive_timeout: "60s"
      checkpoint_timeout: "10min"
      checkpoint_completion_target: "0.9"
    synchronous:
      method: "any"
      number: 1
      failoverQuorum: true

  affinity:
    enablePodAntiAffinity: true
    topologyKey: topology.kubernetes.io/zone
    podAntiAffinityType: required

  backup:
    target: prefer-standby
    volumeSnapshot:
      className: csi-aws-vsc
      online: true
      snapshotOwnerReference: backup
    barmanObjectStore:
      destinationPath: "s3://payu-db-backups/cnpg/"
      s3Credentials:
        accessKeyId:
          name: cnpg-s3-credentials
          key: ACCESS_KEY_ID
        secretAccessKey:
          name: cnpg-s3-credentials
          key: SECRET_ACCESS_KEY
      wal:
        compression: gzip
        maxParallel: 4
      data:
        compression: gzip
        jobs: 2
      retentionPolicy: "30d"
```

### 5.2. Point-in-Time Recovery (PITR) Manifest
Untuk memulihkan database ke titik waktu spesifik (misal ke cluster baru `payu-database-recovery`):

```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: payu-database-recovery
  namespace: payu-dev
spec:
  instances: 3
  bootstrap:
    recovery:
      source: production-backup
      targetTime: "2026-08-18 15:30:00.000000+00"
  
  externalClusters:
    - name: production-backup
      barmanObjectStore:
        destinationPath: "s3://payu-db-backups/cnpg/"
        s3Credentials:
          name: cnpg-s3-credentials
          key: ACCESS_KEY_ID
          secretAccessKey:
            name: cnpg-s3-credentials
            key: SECRET_ACCESS_KEY
```

---

## 6. Verification & Automated DR Drill Protocol

Pengujian berkala wajib dijalankan minimal setiap kuartal dengan tahapan:

1. **Failover Drill (`dr-cnpg-failover-drill.sh`)**:
   - Simulasi pemadaman paksa Primary (`kubectl delete pod payu-database-1 --now`).
   - Ukur waktu promosi Standby -> Primary via CNPG (< 15 detik).
   - Verifikasi tidak ada koneksi *stuck* di backend service (`actuator/health` tetap UP).
2. **PITR Restore Drill (`dr-cnpg-pitr-restore.sh`)**:
   - Buat *dummy test transaction* dengan timestamp $T_1$.
   - Simulasikan bencana / drop table pada $T_2$.
   - Spin up cluster `payu-database-drill` dengan target recovery $T_1$.
   - Eksekusi [cleanup-test-db.sh](../../scripts/cleanup-test-db.sh) dan verifikasi data transaksi $T_1$ utuh, sedangkan korupsi $T_2$ tereliminasi.
3. **Double-Entry Ledger Invariant Check (`dr-verify-ledger-integrity.sh`)**:
   - Query otomatis: `SELECT SUM(debit_amount) - SUM(credit_amount) FROM journal_entries;` (Wajib = `0.0000`).
   - Verifikasi Flyway schema history integrity di seluruh 22 database.

---

## 7. Consequences & Trade-offs

### Positive:
- **Zero Financial Data Loss**: Memenuhi regulasi ketat perbankan Indonesia (POJK 11/2022 & SNAP-BI).
- **Sub-Minute PITR**: Pemulihan bencana granular tanpa kehilangan transaksi berharga.
- **Automated High Availability**: RTO < 15 detik untuk kegagalan single node/zone.
- **Standardized Tooling**: Menggantikan skrip lama Crunchy PGO / Docker lokal dengan CloudNativePG resmi.

### Trade-offs & Mitigations:
- **Write Latency Overhead**: Synchronous replication menambah ~2–5ms latency per commit. *Mitigasi*: Menempatkan 3 AZ dalam satu region AWS dengan low-latency fiber (< 1ms inter-AZ RTT) dan SSD NVMe gp3.
- **S3 Storage Cost**: Archival WAL dan snapshot memerlukan biaya cloud. *Mitigasi*: Lifecycle rules otomatis menggeser data > 30 hari ke S3 IA dan > 365 hari ke S3 Glacier Vault.
