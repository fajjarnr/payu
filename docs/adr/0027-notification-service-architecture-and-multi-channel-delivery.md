# ADR-0027: Notification Service Architecture, Multi-Channel Delivery & Zero-Cost Provider Strategy

**Status**: Accepted  
**Date**: 2026-08-18  
**Deciders**: Principal Architect, Core Banking Engineer, Integration Architect, Cybersecurity Architect  
**Supersedes**: —  
**Related**: [ADR-0003](0003-quarkus-for-supporting-services.md) (Quarkus for Supporting Services), [ADR-0010](0010-security-standards.md) (Security Standards), [ADR-0026](0026-kafka-topic-governance-and-dlq-strategy.md) (Kafka Topic Governance & DLQ), PROD-044, IMP-4, SEC-NOTIF-002

---

## Context

`notification-service` adalah microservice pendukung (Quarkus 3.33.1 Native) yang bertugas mendistribusikan notifikasi transaksi, OTP/autentikasi, KYC, dan informasi tagihan ke pengguna melalui berbagai kanal (Email, SMS, Push Notification, In-App).

Berdasarkan audit arsitektur dan evaluasi operasional, ditemukan beberapa tantangan dan gap:

1. **Paradoks Fallback Multi-Kanal (IMP-4 / CB-037)**:  
   Implementasi `NotificationService` mencoba fallback antar-kanal (`PUSH -> EMAIL -> SMS`) menggunakan string `recipient` tunggal. Jika token Push (FCM) gagal, sistem mencoba mengirim string token tersebut ke alamat Email/SMS.
2. **Kebutuhan Provider SMS Gratis untuk Lab Skala Production**:  
   PayU adalah platform perbankan digital open-core/lab dengan standar arsitektur setara production (28 microservice di OpenShift/Podman). Pengujian transaksi end-to-end (E2E), QA, dan OTP membutuhkan pengiriman notifikasi nyata ke perangkat tester tanpa membebani biaya pulsa/kredit SMS telco komersial (Twilio/Zenziva).
3. **Status Provider Fail-Closed (PROD-044)**:  
   Adapter SMS dan Push notification saat ini berstatus fail-closed (`payu.sms.provider=NONE`, `payu.push.provider=NONE`) atau hanya mencetak log di console.
4. **Ketidaksesuaian Topik Kafka & Event Semantics**:  
   Kanal `payment-events` di `application.yml` mengonsumsi `payu.transaction.payment-expired.v1`, namun kode `EventConsumer` memprosesnya sebagai notifikasi tagihan sukses (`billingId`).
5. **Kepatuhan Data Pribadi (UU PDP No. 27/2022 & POJK No. 21/2023)**:  
   Log sudah dimasking (`RecipientMasker`), namun kolom `recipient` dan `body` di database `notifications` (PostgreSQL) masih disimpan dalam bentuk plaintext.

---

## Decision

Kami menetapkan arsitektur standar untuk `notification-service` yang mencakup **Message Tiering**, **Zero-Cost Lab Provider Strategy**, **Multi-Channel Contact Model**, **Data Protection at-Rest**, dan **Topic Realignment**.

```mermaid
flowchart TD
    subgraph Ingestion ["Ingestion Channels"]
        CMD[Direct Command / OTP] -->|Artemis Queue: payu.notification.commands| CONSUMER_JMS[Artemis Command Consumer]
        EVENTS[Domain Events: TX, Wallet, Billing, KYC] -->|Kafka Topics: payu.*.v1| CONSUMER_KAFKA[SmallRye Event Consumer]
        REST[REST API: POST /api/v1/notifications] --> AUTH_GUARD[OIDC Auth & IDOR Guard]
    end

    subgraph CoreEngine ["Notification Core Engine (Hexagonal)"]
        AUTH_GUARD --> ROUTER[Notification Router & Tier Resolver]
        CONSUMER_JMS --> ROUTER
        CONSUMER_KAFKA --> ROUTER
        
        ROUTER --> IDEMP{Idempotency Check}
        IDEMP -->|Duplicate| DUP_HANDLER[Return Existing / Skip]
        IDEMP -->|New| CIPHER[AES-256 GCM Encryptor]
        CIPHER --> REPO[(PostgreSQL: notifications)]
        
        REPO --> DISPATCHER[Intelligent Dispatcher & Fallback Engine]
    end

    subgraph Senders ["Channel Adapters (Zero-Cost & Prod Ready)"]
        DISPATCHER -->|EMAIL| EMAIL_ADAPT[Quarkus Mailer Adapter]
        DISPATCHER -->|PUSH| PUSH_ADAPT[FCM HTTP v1 Adapter]
        DISPATCHER -->|SMS / ALERT| SMS_ROUTER{SMS Provider Strategy}
        
        SMS_ROUTER -->|Lab: Free Real Delivery| TELEGRAM_ADAPT[Telegram Bot API Adapter]
        SMS_ROUTER -->|Lab: Local Mock Inbox| SIM_ADAPT[PayU SMS Simulator Adapter]
        SMS_ROUTER -->|Prod: Telco Gateway| TWILIO_ADAPT[Twilio / Zenziva / Telkomsel Adapter]
        
        DISPATCHER -->|IN_APP| INAPP_ADAPT[In-App Inbox Store]
    end

    subgraph Destinations ["Destinations & Clients"]
        EMAIL_ADAPT --> MAILPIT[Mailpit / Local SMTP / SES]
        PUSH_ADAPT --> FCM_GW[Google Firebase FCM (100% Free)]
        TELEGRAM_ADAPT --> TG_USER[Tester Mobile Device (Telegram)]
        SIM_ADAPT --> WEB_INBOX[Dev / QA Web Inbox UI]
        TWILIO_ADAPT --> CELL_PHONE[End-User Cellular Network]
        INAPP_ADAPT --> APP_CLIENT[Web & Mobile In-App Inbox]
    end
```

---

### 1. Message Tiering & SLA

Notifikasi diklasifikasikan ke dalam 4 Tier:

| Tier | Tipe Pesan | Contoh Kasus | SLA Pengiriman | Saluran Utama | Mekanisme Ingestion |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Tier 1 (Critical)** | Autentikasi & Keamanan | OTP login, reset PIN, Fraud alert, Lockout | **< 3 detik** | Push / Telegram (Lab) / SMS | Artemis Priority Queue (`payu.notification.commands`) & REST API |
| **Tier 2 (Transactional)** | Finansial & Ledger | Transfer berhasil, saldo didebit, tagihan lunas | **< 15–30 detik** | Push + In-App (+ Email Receipt) | Kafka Event Consumer (Outbox Pattern) |
| **Tier 3 (Informational)** | Status Operasional | E-Statement siap, KYC approved/rejected | **< 2–5 menit** | Email + In-App | Kafka Event Consumer (Standard) |
| **Tier 4 (Marketing)** | Promosi & Gamifikasi | Promo cashback, voucher merchant | **Batch / Menit** | In-App Banner / Quiet Hours Push | Low Priority Topic / Scheduled Batch |

---

### 2. Zero-Cost & Production-Scale Provider Strategy

Untuk menjaga agar PayU dapat berjalan sebagai lab berskala enterprise tanpa biaya telco eksternal, kami menetapkan strategi provider:

```
                  ┌───────────────────────────────────────────────┐
                  │           payu.sms.provider (Config)          │
                  └───────────────────────┬───────────────────────┘
                                          │
        ┌───────────────────┬─────────────┴───────┬───────────────────┐
        ▼                   ▼                     ▼                   ▼
┌───────────────┐   ┌───────────────┐     ┌───────────────┐   ┌───────────────┐
│   TELEGRAM    │   │   SIMULATOR   │     │ TWILIO/ZENZIVA│   │  LOG / NONE   │
│ (Free Real    │   │ (Local Web    │     │ (Production   │   │ (Dev fallback/│
│  Delivery)    │   │  Mock Inbox)  │     │  Fail-Closed) │   │  Unit Tests)  │
└───────────────┘   └───────────────┘     └───────────────┘   └───────────────┘
```

1. **Email Channel**:
   * **Lab / Dev / Staging**: Quarkus Mailer mengarah ke **Mailpit / MailHog** lokal di cluster OpenShift/Podman (100% gratis, zero external dependency).
   * **Production**: Konfigurasi SMTP relay (SendGrid / AWS SES / Postmark).
2. **Push Notification Channel (Firebase FCM v1)**:
   * **Biaya**: **100% Gratis & Unlimited** (Google FCM tidak membebankan biaya pengiriman pesan push ke Android, iOS, maupun Web PWA).
   * **Implementasi**: Adapter Quarkus menggunakan Google Auth SDK / REST API v1 FCM (`https://fcm.googleapis.com/v1/projects/{project}/messages:send`).
3. **SMS & Instant OTP Channel (Multi-Mode Strategy)**:
   * **Mode `TELEGRAM` (Rekomendasi Utama untuk Lab/Testing Real Device)**:
     - Menggunakan **Telegram Bot API** (`https://api.telegram.org/bot<token>/sendMessage`).
     - **Keunggulan**: 100% Gratis, latensi instan (< 1 detik), langsung terkirim ke ponsel tester/QA (notifikasi popup & suara nyata), mendukung private chat maupun QA Alert Group.
     - **Format**: `recipient` dapat berupa `telegramChatId` atau no HP yang di-map ke chat ID tester.
   * **Mode `SIMULATOR` (OpenShift Native Mock Inbox)**:
     - Mengirim payload ke internal endpoint simulator (`http://sms-simulator:8093/api/v1/sms/inbox` atau in-memory store) yang menyediakan REST API & Swagger UI bagi automated E2E test (Playwright/k6) untuk mengambil kode OTP secara otomatis.
   * **Mode `TWILIO` / `ZENZIVA` (Production Mode)**:
     - Adapter telco gateway komersial dengan *Circuit Breaker* dan fallback fail-closed.
   * **Mode `LOG` / `NONE` (Unit Testing)**:
     - Logging bertingkat dengan sanitasi nomor HP atau fail-closed default.

---

### 3. Multi-Channel Contact Model & Smart Fallback

Kami memperbarui kontrak request dan domain model untuk memisahkan kontak per-kanal:

#### A. Multi-Channel Contact Payload (DTO)
```json
{
  "userId": "usr-8829104",
  "channel": "PUSH",
  "contacts": {
    "pushToken": "eXz810...fcm_token",
    "phone": "+6281234567890",
    "email": "nasabah@payu.id",
    "telegramChatId": "123456789"
  },
  "title": "Transaksi Berhasil",
  "body": "Pembayaran QRIS Rp 50.000 berhasil.",
  "templateId": "qris-payment-success",
  "idempotencyKey": "tx-9948291-pay"
}
```

#### B. Fallback Hierarchy yang Valid
1. **Intra-Channel Failover (Prioritas 1)**:
   * Provider Primer (misal: Push FCM) timeout/error $\rightarrow$ Coba Retry 1x dengan backoff singkat.
2. **Cross-Channel Escalation (Prioritas 2 - Khusus Tier 1/2)**:
   * Jika `PUSH` gagal $\rightarrow$ gunakan `contacts.telegramChatId` (di lab) atau `contacts.phone` (di prod via SMS).
   * Dilarang me-reuse string token FCM ke fungsi pengirim email/SMS.

---

### 4. Perlindungan Data Sensitif & Kepatuhan Regulasi (UU PDP)

1. **Enkripsi Kolom Database at-Rest**:
   * Entity `NotificationEntity` menggunakan converter enkripsi AES-256 GCM (via Quarkus JPA AttributeConverter / pgcrypto) untuk kolom `recipient` dan `body`.
2. **Lock Screen Privacy (Data Masking)**:
   * Konten notifikasi Push dioptimalkan agar tidak membocorkan saldo penuh di lock screen:
     - *Format*: `"Transaksi Rp 50.000 di Merchant X berhasil. Buka aplikasi untuk melihat rincian."`
     - Saldo sisa dan detail lengkap hanya dapat diakses melalui In-App Inbox setelah autentikasi biometrik/PIN.
3. **Audit Trail & Delivery Receipt (DLR)**:
   * Setiap status perubahan (`PENDING -> SENDING -> SENT -> DELIVERED -> READ / FAILED`) dicatat dengan timestamp, provider message ID, dan failure reason untuk bukti audit OJK.

---

### 5. Penyelarasan Event Topics & Consumer

Topik Kafka di `application.yml` diselaraskan sesuai [EVENT_CATALOG.md](../architecture/EVENT_CATALOG.md):

| Consumer Channel | Topik Kafka Aktual | Deskripsi Event |
| :--- | :--- | :--- |
| `wallet-events` | `payu.wallet.balance-changed.v1` | Notifikasi perubahan saldo wallet |
| `transaction-events` | `payu.transaction.completed.v1` | Notifikasi transaksi transfer/pembayaran berhasil |
| `billing-events` | `payu.billing.payment-completed.v1` | Notifikasi pelunasan tagihan PLN/PDAM/Pulsa |
| `expiry-events` | `payu.transaction.payment-expired.v1` | Notifikasi transaksi/VA kadaluarsa |
| `split-bill-events`| `payu.transaction.split-bill-*.v1` | Notifikasi tagihan bersama & reminder |
| `kyc-events` | `payu.kyc.verified.v1`, `payu.kyc.failed.v1` | Notifikasi status verifikasi identitas |

---

## Consequences

### Positive
- **Zero-Cost Real Device Testing**: Tim developer & QA dapat menguji alur OTP dan notifikasi transaksi langsung di HP menggunakan Telegram Bot API / In-App tanpa biaya pulsa SMS.
- **Arsitektur Tahan Banting (Production-Grade)**: Pemisahan adapter mempermudah switch ke SMS Telco Gateway komersial saat sistem go-live tanpa mengubah core domain logic.
- **Bebas Bug Fallback Cross-Channel**: Menghilangkan error pengiriman string token FCM ke email/SMS.
- **Kepatuhan Regulasi Penuh**: Memenuhi UU PDP No. 27/2022 untuk enkripsi data pribadi at-rest dan audit logging.
- **Event Alignment**: Menghilangkan mismatch semantic antara tagihan billing vs expiry event.

### Negative / Trade-offs
- **Penambahan Parameter Kontak**: Payload event dan command request membutuhkan struktur `contacts` yang lebih lengkap atau look-up internal.
- **Overhead Enkripsi**: Sedikit penambahan CPU cycle untuk enkripsi AES-256 GCM pada setiap operasi insert/read notifikasi di database.

---

## Implementation Roadmap

1. **Phase 1 (Quick Wins & Alignment)**:
   - Perbaiki semantic mapping topic `billing-events` vs `expiry-events` di `application.yml` dan `EventConsumer.java`.
   - Implementasikan DTO `contacts` multi-kanal dan isolasi fallback sender.
2. **Phase 2 (Zero-Cost Senders & Adapters)**:
   - Tambahkan `TelegramSender` (Zero-cost real SMS alternative untuk lab) dan `SmsSimulatorSender`.
   - Implementasikan `FcmPushSender` menggunakan FCM v1 REST API.
3. **Phase 3 (Security & Hardening)**:
   - Tambahkan JPA AttributeConverter untuk enkripsi field PII at-rest di PostgreSQL.
   - Refactor `ArtemisCommandConsumer` dengan connection pooling & thread pool yang aman.
