# ADR-0034: End-to-End Observability, Distributed Tracing (W3C/OTel/Tempo), and Multi-Window Multi-Burn-Rate SLI/SLO Standard

**Status**: Accepted  
**Date**: 2026-08-18  
**Deciders**: Principal Architect, Platform Engineer, Core Banking Lead, Integration Architect, Cybersecurity Architect  

---

## Context

Sebagai platform perbankan digital modern dan payment gateway terintegrasi (SNAP-BI, VA, QRIS, Transfer, Escrow, dan Lending), PayU memproses volume transaksi finansial tinggi yang tersebar di lingkungan microservices heterogen (Java 25 Spring Boot 4.1, Quarkus 3.x, Python 3.12 FastAPI, dan Next.js 16 BFF) di atas platform Red Hat OpenShift 4.20+. 

Dalam ekosistem perbankan digital dan payment gateway multi-tenant (TokoBapak, Nobar, Dolan, Sinau, Maca), ketiadaan observabilitas menyeluruh (*end-to-end observability*) dan standarisasi keandalan (*reliability governance*) menimbulkan risiko operasional dan kepatuhan yang masif:

1. **Kepatuhan Regulasi Finansial & Standar Industri**:
   * **Bank Indonesia SNAP-BI (Standar Nasional Open API Pembayaran)**: Menetapkan Service Level Agreement (SLA) ketersediaan sistem pembayaran minimal **99.90%** dan waktu respon transaksi (*latency*) terikat batas ketat (P95 < 500ms untuk in-house/on-us, P95 < 1500ms untuk interbank).
   * **POJK No. 11/POJK.03/2022 (MRTI OJK) Pasal 20 & 22**: Mewajibkan pemantauan kapasitas, kinerja sistem secara *real-time*, ketersediaan audit trail yang tidak dapat diubah (*immutable*), dan deteksi dini kegagalan operasional.
   * **PCI-DSS v4.0 Requirement 10**: Mengharuskan pencatatan dan korelasi log komprehensif tanpa boleh membocorkan data pemegang kartu (*Primary Account Number* / PAN, PIN, CVV) ke dalam sistem log/telemetri.

2. **Keterbatasan Alerting Berbasis Ambang Batas Statis (*Static Threshold Alerting*)**:
   * Alert statis konvensional (misal: `rate(errors[5m]) > 5%` atau `up < 1`) menimbulkan **alert fatigue** (banyak false alarm pada volume rendah) dan **terlambat mendeteksi *slow-bleed failure*** (kegagalan 0.5% yang berlangsung berhari-hari menguras kuota error budget tanpa memicu alarm).
   * Diperlukan metodologi **Multi-Window Multi-Burn-Rate Alerting** (Google SRE Standard) yang mengukur laju konsumsi *Error Budget* secara presisi.

3. **Diskriminasi Antara *System Failures* vs *Business Rejections***:
   * Pada sistem perbankan, penolakan bisnis (seperti `422 Saldo Tidak Cukup / Insufficient Funds`, `401 Bad Credentials`, `404 Account Not Found`) adalah perilaku sistem yang valid dan sehat, **bukan kegagalan sistem**. Memasukkan error bisnis ke dalam metrik ketersediaan (*availability SLI*) merusak akurasi pengukuran keandalan platform.

4. **Kebutuhan *Distributed Context Propagation* Lintas-Runtime & Asinkron**:
   * Transaksi pembayaran melintasi banyak lapisan: API Gateway $\to$ BFF $\to$ Partner Service $\to$ Kafka Outbox $\to$ Core Banking $\to$ Database $\to$ Partner Webhook.
   * Hilangnya konteks penelusuran (*trace context*) pada *message broker* (Kafka CloudEvents) dan *database connection pool* menyulitkan *root cause analysis* (RCA) saat terjadi transaksi gantung atau anomali latensi.

5. **Risiko *High-Cardinality Explosion* pada Prometheus**:
   * Menambahkan label identitas pengguna (`user_id`, `account_number`, `idempotency_key`) ke metrik Prometheus dapat melipatgandakan *time-series* hingga jutaan entri, melumpuhkan memori Prometheus. Perlu batas tegas antara *metrics*, *logs*, dan *traces*.

---

## Decision Drivers

- **Standarisasi Universal W3C TraceContext**: Propagasi `traceparent` (`00-{trace_id}-{span_id}-{flags}`) konsisten di HTTP, gRPC, Kafka CloudEvents, dan Database SQL Commenter.
- **Google SRE Multi-Window Multi-Burn-Rate Framework**: Alerting berbasis laju konsumsi Error Budget dengan jaminan *high precision* (zero false alarm) dan *high recall* (cepat tanggap pada insiden nyata).
- **FinTech-Specific SLI Categorization**: Pemisahan tegas antara kegagalan infrastruktur/sistem (5xx, timeouts, DB drops) dengan penolakan bisnis (4xx, insufficient balance, validation errors).
- **Zero-Cost & OpenShift Native Stack**: Menggunakan ekosistem Open Source kelas dunia (OpenTelemetry Collector, Red Hat OpenShift TempoStack via S3/ODF, Prometheus/CMO, Vector, LokiStack, Grafana) tanpa biaya lisensi software komersial pihak ketiga.
- **Strict Data Masking & PCI-DSS Compliance**: Jaminan zero-PII pada log, traces, dan span attributes (PAN, CVV, PIN, NIK wajib di-masking di level agent/filter).
- **Multi-Tenant Partitioning**: Visibilitas performa dan latensi per-partner (TokoBapak vs Nobar vs Dolan) tanpa memicu *high-cardinality metrics*.

---

## Decision

Kami memutuskan untuk mengadopsi dan membakukan arsitektur **End-to-End Observability, Distributed Tracing, dan Multi-Window Multi-Burn-Rate SLI/SLO Platform** PayU sebagai berikut:

```mermaid
flowchart TD
    subgraph INGRESS_LAYER["1. Perimeter & Ingress Gate"]
        CLIENT["Partner / Mobile / Web Client"] -->|HTTPS + X-EXTERNAL-ID| CORAZA["Coraza WAF / Ingress"]
        CORAZA -->|Inject W3C traceparent| APICAST["3scale APIcast / Edge Gateway"]
    end

    subgraph WORKLOAD_LAYER["2. Polyglot Microservices (OpenShift)"]
        APICAST -->|traceparent / Baggage| GW["gateway-service (BFF)"]
        GW -->|REST / gRPC| PARTNER["partner-service (Quarkus)"]
        PARTNER -->|JDBC + SQLCommenter| PG_DB[("PostgreSQL")]
        PARTNER -->|Outbox Pattern| OUTBOX_TABLE[("Outbox Table")]
        OUTBOX_TABLE -->|CloudEvents 1.0.2 + traceparent| KAFKA["Strimzi Kafka Cluster"]
        KAFKA -->|Consumer Span Linkage| CORE_BANKING["account / transaction (Spring Boot 4.1)"]
        KAFKA -->|Consumer Span Linkage| NOTIF["notification-service (Java 25)"]
    end

    subgraph TELEMETRY_PIPELINE["3. OpenTelemetry Telemetry Pipeline"]
        GW & PARTNER & CORE_BANKING & NOTIF -->|OTLP gRPC 4317 / HTTP 4318| OTEL_COLLECTOR["OpenTelemetry Collector DaemonSet/Deployment"]
        OTEL_COLLECTOR -->|Filtered Traces| TEMPO["Red Hat TempoStack (S3 Storage)"]
        OTEL_COLLECTOR -->|Aggregated Metrics| PROM["OpenShift Prometheus (CMO)"]
        GW & PARTNER & CORE_BANKING & NOTIF -->|JSON Logs + MDC| VECTOR["Vector Agent / ClusterLogForwarder"]
        VECTOR -->|Structured Logs| LOKI["LokiStack (Object Storage)"]
        VECTOR -->|Security & Audit Logs| WAZUH["Wazuh SIEM (RFC 5424)"]
    end

    subgraph VISUALIZATION["4. SRE & Business Visualization"]
        PROM & TEMPO & LOKI --> GRAFANA["Grafana SRE & Partner Portal"]
        PROM -->|Multi-Burn-Rate Rules| ALERTMANAGER["Prometheus Alertmanager"]
        ALERTMANAGER -->|Page P1 (Burn Rate > 14.4x)| PAGERDUTY["On-Call SRE (PagerDuty)"]
        ALERTMANAGER -->|Ticket P2/P3 (Burn Rate 1x-3x)| SLACK["DevOps Slack / Jira Backlog"]
    end
```

---

### 1. Distributed Tracing & W3C Context Propagation Standard

#### 1.1 Format Standar W3C TraceContext
Seluruh komponen PayU wajib mengadopsi standar **W3C Trace Context Level 1**:
* **HTTP Header**: `traceparent: 00-{trace_id}-{span_id}-{trace_flags}` (contoh: `00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01`).
* **Baggage Header**: `baggage: tenant_id=tokobapak,channel=snap_bi,idempotency_key=REQ-992183`.

#### 1.2 Asynchronous Event Propagation (Kafka CloudEvents)
Sesuai CloudEvents 1.0.2 dan [ADR-0026](0026-kafka-topic-governance-and-dlq-strategy.md), `traceparent` dipetakan ke dua layer pada Kafka:
1. **CloudEvent Attribute**: Atribut ekstensi `ce_traceparent` dan `ce_tracestate`.
2. **Kafka Record Header**: Header biner/string `traceparent` sehingga consumer (Spring Kafka / SmallRye Reactive Messaging) dapat mengekstrak `SpanContext` secara transparan tanpa perlu mem-parsing JSON payload.

```java
// Contoh Injeksi Context pada events-starter / outbox-starter
CloudEventEnvelope<T> event = CloudEventEnvelope.<T>builder()
    .id(UUID.randomUUID().toString())
    .source(URI.create("/services/partner-service"))
    .type("payu.partner.snap-bi.payment-received.v1")
    .traceparent(Span.current().getSpanContext().asTraceparent()) // 00-{traceId}-{spanId}-01
    .tenantId(MDC.get("tenant_id"))
    .data(payload)
    .build();
```

#### 1.3 Database Tracing via SQLCommenter
Driver JDBC / Hibernate di shared `datasource-starter` dikonfigurasi untuk menyisipkan SQL Comments pada setiap query ke PostgreSQL:
```sql
SELECT balance, currency FROM accounts WHERE account_number = $1 /* traceparent='00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01',tenant_id='tokobapak' */
```
Ini memungkinkan korelasi langsung antara lambatnya query di PostgreSQL `pg_stat_statements` dengan trace ID di Red Hat Tempo.

#### 1.4 Sampling Strategy
Untuk mengoptimalkan penyimpanan storage Tempo tanpa kehilangan jejak insiden kritis:
* **Financial Transactions (100% Sampling)**: Semua request pada path `/v1/payments`, `/v1/transfers`, `/v1/snap-bi/*`, `/v1/escrow/*` di-sample 100%.
* **Failed Requests / Errors (100% Sampling)**: Semua request yang menghasilkan response code $\ge 400$ atau unhandled exception di-sample 100% (tail-sampling via OTel Collector).
* **General Read APIs (5% Adaptive Sampling)**: Endpoint baca non-finansial (`/v1/promotions`, `/v1/catalog`) di-sample secara probabilistik 5%.
* **Synthetic Probes (0% Sampling)**: Endpoint `/actuator/health`, `/q/health`, `/readyz`, `/livez` di-drop (0% sampling) agar tidak membebani collector.

---

### 2. Standard SLI / SLO Matrix Platform PayU (Tier-1 Financial Services)

Berdasarkan standar industri perbankan dan Bank Indonesia SNAP-BI, PayU menetapkan target SLI/SLO berbasis periode bergulir 30 hari (*30-day rolling window*):

| Domain / Kategori | Service Level Indicator (SLI) | SLO Target (30 Hari) | Error Budget (30 Hari) | Klasifikasi Severity |
| :--- | :--- | :---: | :---: | :---: |
| **API Availability (Tier-1 Core)** | $\frac{\text{Count}(\text{HTTP status } < 500)}{\text{Total Valid Requests}}$ | **99.90%** (3 Nines) | 0.10% (~43.2 menit) | **P1 (Critical)** |
| **SNAP-BI In-House Latency** | $\frac{\text{Count}(\text{SNAP-BI in-house requests } \le 500\text{ms})}{\text{Total SNAP-BI in-house requests}}$ | **95.00%** ($P95 \le 500\text{ms}$) | 5.00% | **P1 (Critical)** |
| **Interbank / BI-FAST Latency** | $\frac{\text{Count}(\text{Interbank requests } \le 1500\text{ms})}{\text{Total Interbank requests}}$ | **95.00%** ($P95 \le 1500\text{ms}$) | 5.00% | **P2 (Warning)** |
| **Ledger Invariant Integrity** | $\frac{\text{Reconciliation Runs with 0 Discrepancy}}{\text{Total Scheduled Reconciliation Runs}}$ | **100.00%** | **0.00% (Zero-Tolerance)** | **P0 (Blocker)** |
| **Transactional Outbox Lag** | $\frac{\text{Count}(\text{Outbox events published } \le 1000\text{ms})}{\text{Total Outbox events}}$ | **99.90%** | 0.10% | **P1 (Critical)** |
| **Webhook Delivery Durability** | $\frac{\text{Webhooks Delivered Successfully } \le 3 \text{ retries}}{\text{Total Webhooks Triggered}}$ | **99.95%** | 0.05% | **P1 (Critical)** |

> [!IMPORTANT]
> **Pengecualian Status HTTP 4xx dari Availability SLO**:
> Request dengan respon HTTP 4xx (misal: `400 Bad Request`, `401 Unauthorized`, `404 Not Found`, `422 Unprocessable Entity` karena saldo tidak cukup) **DIKECUALIKAN** dari perhitungan error budget, karena merefleksikan validasi bisnis yang benar. Hanya status HTTP `5xx` (`500 Internal Server Error`, `502 Bad Gateway`, `503 Service Unavailable`, `504 Gateway Timeout`) dan koneksi yang drop yang mengonsumsi Error Budget.

---

### 3. Multi-Window Multi-Burn-Rate Alerting Strategy (Google SRE Standard)

Alerting berbasis konsumsi Error Budget menggunakan 4 jendela waktu bergulir (*rolling windows*) berpasangan (*short window* dan *long window*) untuk memastikan tidak ada *alert flapping* dan mendeteksi anomali secara instan:

```
                  ┌─────────────────────────────────────────────────────────────┐
                  │                 TOTAL ERROR BUDGET (100%)                   │
                  └─────────────────────────────────────────────────────────────┘
                                  │                               │
            ┌─────────────────────┴─────────────┐   ┌─────────────┴─────────────────────┐
            │       FAST BURN (Kritis/P1)       │   │       SLOW BURN (Warning/P2)      │
            │   Habis dalam hitungan Jam        │   │   Habis dalam hitungan Hari       │
            └───────────────────────────────────┘   └───────────────────────────────────┘
```

#### Tabel Konfigurasi Multi-Burn-Rate (SLO 99.9%)

| Tier Alert | Konsumsi Budget | Lookback Window (Long) | Short Window | Burn Rate Multiplier | Tindakan Notifikasi | Target Respon |
| :--- | :---: | :---: | :---: | :---: | :--- | :--- |
| **Page-1 (Fast Burn)** | **2.0%** | **1 Jam** | **5 Menit** | **14.4×** | PagerDuty Call (On-Call SRE & Lead) | $< 15\text{ menit}$ |
| **Page-2 (Medium Burn)** | **5.0%** | **6 Jam** | **30 Menit** | **6.0×** | PagerDuty Call (On-Call SRE) | $< 30\text{ menit}$ |
| **Ticket-1 (Slow Burn)** | **10.0%** | **24 Jam** | **2 Jam** | **3.0×** | Slack Channel `#alerts-reliability` | $< 2\text{ jam}$ (Jam kerja) |
| **Ticket-2 (Degradation)**| **10.0%** | **3 Hari** | **6 Jam** | **1.0×** | Jira Ticket Otomatis (Next Sprint) | Review Mingguan |

#### Formula PromQL Multi-Burn-Rate (Contoh Penerapan)

```yaml
# PromQL untuk Page-1 (Burn Rate 14.4x over 1h AND 5m)
- alert: ApiAvailabilityFastBurnCritical
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{job=~"payu-.*",status=~"5.."}[1h]))
      /
      sum(rate(http_server_requests_seconds_count{job=~"payu-.*"}[1h]))
    ) > (14.4 * (1 - 0.999))
    AND
    (
      sum(rate(http_server_requests_seconds_count{job=~"payu-.*",status=~"5.."}[5m]))
      /
      sum(rate(http_server_requests_seconds_count{job=~"payu-.*"}[5m]))
    ) > (14.4 * (1 - 0.999))
  for: 2m
  labels:
    severity: critical
    tier: page
    team: platform-core
  annotations:
    summary: "Fast Error Budget Burn (14.4x) - 2% budget consumed in 1 hour"
    description: "Current error rate is burning the monthly budget in 2 days. Immediate triage required!"
    runbook: "https://docs.payu.fajjjar.my.id/runbooks/slo-availability-burn"
```

---

### 4. High-Cardinality & Multi-Tenant Governance

Untuk mencegah ledakan metrik (*cardinality explosion*) di Prometheus cluster OpenShift:

1. **Aturan Labeling Metrik Prometheus**:
   * **ALLOWED Labels (Bounded Cardinality)**:
     * `tenant_id`: Terbatas pada nama mitra terdaftar (`tokobapak`, `nobar`, `dolan`, `sinau`, `maca`, `payu_retail`). Total $< 50$ nilai.
     * `service_code`: Kode produk/fitur (`snap_va`, `snap_qris`, `snap_transfer`, `wallet_topup`). Total $< 30$ nilai.
     * `http_status`: HTTP Status Code (`200`, `400`, `422`, `500`, `503`).
     * `direction`: `inbound` vs `outbound`.
   * **STRICTLY PROHIBITED Labels (Unbounded / High Cardinality)**:
     * ❌ `user_id` / `customer_id`
     * ❌ `account_number` / `va_number`
     * ❌ `idempotency_key` / `external_id` / `transaction_id`
     * ❌ `ip_address` / `user_agent`
2. **Korelasi via Log MDC & Trace Baggage**:
   * Identitas granular (`idempotency_key`, `account_id`, `partner_reference_no`) **HANYA** disimpan di dalam **MDC Logging** dan **Span Attributes**, yang dialirkan ke Loki/Tempo (storage berbasis S3 yang murah dan dioptimalkan untuk kardinalitas tinggi).

---

### 5. Masking PII & Standar Kepatuhan PCI-DSS v4.0

Seluruh telemetri (Metrics, Traces, Logs) wajib melalui filter penyaring data sensitif sebelum dikirim ke luar memori aplikasi:

| Field Data | Kebijakan Telemetri | Aturan Masking |
| :--- | :--- | :--- |
| **Card PAN (Nomor Kartu)** | Dilarang Plaintext | Masking 6 digit awal & 4 digit akhir: `411111******1111` |
| **CVV / CVC** | **STRICTLY PROHIBITED** | Redacted total: `[REDACTED]` |
| **PIN & Password** | **STRICTLY PROHIBITED** | Redacted total: `[REDACTED]` |
| **Nomor KTP / NIK** | Masking Parsial | Masking 8 digit tengah: `320101********0001` |
| **Nomor Rekening / HP** | Masking Parsial | Masking 4 digit tengah: `0812****7890` |

Implementasi ditegakkan via `MdcMaskingPatternLayout` di `backend/shared/logging-starter` dan `OpenTelemetry SpanProcessor` di `api-commons`.

---

## Consequences

### Positive
- **Transparansi Kinerja SLA Mitra**: Partner (TokoBapak/Nobar) mendapatkan kepastian data performa dan laporan real-time tanpa sengketa audit.
- **Zero False Alarm (Precision High)**: On-call SRE hanya dibangunkan saat insiden nyata mengancam ketersediaan sistem pembayaran.
- **Root Cause Analysis Cepat**: Engineer dapat melacak alur transaksi dari APIcast $\to$ Quarkus $\to$ Kafka $\to$ Spring Boot $\to$ PostgreSQL dalam satu visualisasi trace di Red Hat Tempo.
- **Efisiensi Finansial (Zero Licensing Cost)**: Menggunakan stack OSS (OTel, TempoStack, Prometheus, Vector, Grafana) yang berjalan native di atas OpenShift 4.20+.

### Negative & Mitigations
- **Storage Overhead untuk Tracing**:
  * *Mitigasi*: Tail-sampling (0% healthcheck, 5% reads, 100% financial/errors) dan lifecycle policy bucket S3 Tempo selama 7 hari untuk trace dev/staging dan 30 hari untuk prod.
- **Kompleksitas Propagasi Asinkron**:
  * *Mitigasi*: Dibungkus dalam shared starters (`events-starter`, `outbox-starter`, `logging-starter`) sehingga developer service tidak perlu menulis kode manual injeksi `traceparent`.

---

## Implementation Mapping & Verification Blueprint

| Action Item | Komponen / Target | File / Lokasi Referensi |
| :--- | :--- | :--- |
| **1. Multi-Burn-Rate Rules** | Prometheus Alert Rules | [`infrastructure/platform/observability/monitoring/alerts/slo-alerts.yaml`](file:///home/ubuntu/payu/infrastructure/platform/observability/monitoring/alerts/slo-alerts.yaml) |
| **2. OTel Collector Pipelines** | OpenTelemetry Collector CR | [`infrastructure/platform/observability/tracing/otel-collector.yaml`](file:///home/ubuntu/payu/infrastructure/platform/observability/tracing/otel-collector.yaml) |
| **3. TempoStack Deployment** | Red Hat Tempo Operator CR | [`infrastructure/platform/observability/tracing/tempostack.yaml`](file:///home/ubuntu/payu/infrastructure/platform/observability/tracing/tempostack.yaml) |
| **4. W3C CloudEvents Tracing** | Shared Events Starter | [`backend/shared/events-starter/src/main/java/id/payu/events/cloudevents/CloudEventEnvelope.java`](file:///home/ubuntu/payu/backend/shared/events-starter/src/main/java/id/payu/events/cloudevents/CloudEventEnvelope.java) |
| **5. Trace ID & MDC Filter** | Shared Logging Starter | [`backend/shared/logging-starter/src/main/java/id/payu/logging/filter/TraceIdFilter.java`](file:///home/ubuntu/payu/backend/shared/logging-starter/src/main/java/id/payu/logging/filter/TraceIdFilter.java) |
| **6. Partner & SRE Dashboards** | Grafana Dashboards | [`infrastructure/platform/observability/monitoring/grafana/dashboards/`](file:///home/ubuntu/payu/infrastructure/platform/observability/monitoring/grafana/dashboards/) |
