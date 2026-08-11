# PayU Async Components Catalog — Consumers, Schedulers, gRPC

> Fitur non-REST per service: Kafka/AMQ consumers, scheduled jobs, gRPC service.
> Pendamping [`FEATURES.md`](./FEATURES.md) (yang berisi REST endpoints saja).
> Sumber: scan `@KafkaListener`/`@JmsListener`/`@Scheduled`/`@GrpcService` (2026-08-11).

## Kafka / AMQ Consumers

| Service | Consumer | Topic / Queue | Fungsi |
|:---|:---|:---|:---|
| wallet-service | `RefundRequestedConsumer` | `payu.dispute.refund-requested.v1` | Eksekusi reversal refund (idempotent by refundId) |
| wallet-service | `WalletEventConsumer` | `user.created` | Buat wallet saat user dibuat |
| wallet-service | `FxRateEventConsumer` | `fx-rates-updated` | Update rate untuk konversi |
| partner-service | `FinancialEventConsumer` | `payu.transactions.initiated`, `payu.transactions.validated`, dll | Trigger webhook delivery ke partner (DLQ + retry 3×) |
| partner-service | `SubscriptionEventConsumer` | subscription events | Update subscription webhook |
| promotion-service | `TransactionCompletedConsumer` | `transaction.completed` | Trigger cashback/reward |
| transaction-service | `BatchDisbursementService` listener | `disbursement-batch` | Proses batch disbursement |
| billing-service | `SubscriptionScheduledChargeListener` | `payu.billing.scheduled` (AMQ/Artemis, delayed) | Charge recurring + dunning retry |
| analytics-service | `KafkaConsumerService` (AIOKafkaConsumer) | `settings.kafka_topics` (multi-topic, group `settings.kafka_consumer_group`) | Ingest event → TimescaleDB; dedup by event identity |
| kyc-service | `KafkaProducerService` (AIOKafkaProducer) | publish `payu.kyc.verified`, `payu.kyc.failed` | Event hasil verifikasi KYC — ⚠️ **direct producer tanpa outbox** (Python tak punya outbox-starter): DB sukses + publish gagal = event hilang |
| notification-service | `ArtemisCommandConsumer` (**JMS programmatic** — `session.createConsumer` + loop `receive`, `@Observes StartupEvent`) | queue `payu.notification.commands` (AMQ/Artemis) | Terima command notifikasi (OTP, transaksi) → kirim via channel |

> ⚠️ Catatan: kyc-service (Python) publish langsung ke Kafka tanpa transactional outbox — beda dari Java services. Perlu evaluasi (outbox lite / retry + reconciliation).

## Scheduled Jobs

| Service | Scheduler | Jadwal | Fungsi |
|:---|:---|:---|:---|
| transaction-service | `ScheduledTransferScheduler` | tiap 60s (ShedLock 55s) | Eksekusi scheduled transfer due |
| transaction-service | `PaymentExpiryScheduler` (2 method) | tiap 5 menit (ShedLock) | Expire pending transaction + VA payment, release reservation |
| transaction-service | `TransactionArchivalScheduler` | cron `0 0 2 * * ?` | Arsip transaksi lama |
| transaction-service | `TransactionServiceApplication` bootstrap | saat boot | Initialisasi/seed |
| wallet-service | `EscrowService` expiry check | tiap 5 menit | Expire escrow HELD |
| wallet-service | `RefundReversalExecutor` reconcile | tiap 60s | Reconcile reversal yang gagal |
| wallet-service | `SplitPaymentService` reconcile | tiap 60s | Reconcile split payment |
| wallet-service | `SettlementService` | tiap 60s (lihat `@Scheduled`) | Proses settlement batch + reconcile |
| billing-service | `PaymentService` reconcile | tiap 60s (ShedLock) | Reconcile bill payment ambiguous |
| billing-service | `SubscriptionService` (2 method) | tiap 5 menit (ShedLock) | Charge subscription due + trial check/expire |
| billing-service | `BillingServiceApplication` bootstrap | saat boot | Initialisasi |
| lending-service | `LoanManagementService` reconcileRepayments | tiap 60s (ShedLock) | Reconcile repayment |
| investment-service | `InvestmentOperationReconciler` | tiap 60s | Reconcile buy operation |
| fx-service | `FxRateUpdateScheduler` | tiap 15 menit | Update kurs dari provider |
| fx-service | `FxRateEventPublisher` (2 method) | tiap 60s + cron | Publish rate update ke Kafka |
| partner-service | `WebhookDispatcherService` (2 method) | retry tiap 30s (ShedLock) + cleanup cron `0 0 3 * * *` | Retry webhook delivery (backoff 4^n×30s, max 10) + cleanup delivery lama |
| partner-service | `SnapBiReconciliationService` | tiap 1 jam | Reconcile SNAP payment vs ledger |
| partner-service | `CertificateRotationService` | cron harian `0 0 8 * * *` | Rotasi sertifikat partner |
| partner-service | `ApiKeyService` rotation | tiap 1 jam | Rotasi/expiry API key |
| partner-service | `CredentialBackfillRunner` | saat boot + cron | Backfill ciphertext credential lama |
| partner-service | `PaymentLinkService` expire | tiap 5 menit (fixedRate 300000) | Expire payment link |
| partner-service | `MerchantService` expireQrPayments | tiap 2 menit (ShedLock) | Expire QR payment merchant |
| partner-service | `PartnerServiceApplication` bootstrap | saat boot | Initialisasi |
| gateway-service | `CheckoutService` | tiap 10 menit | Expire checkout token |
| gateway-service | `ApiKeyRotationService` | `@Scheduled(every = "1h")` | Rotasi key gateway |
| gateway-service | `ApiAnalyticsService` | `every = "{gateway.analytics.flush-interval}"`, delay 1m | Flush analytics in-memory |
| gateway-service | `PersistentAnalyticsService` (3 method) | flush interval (delay 1m) + cron `0 2 * * * ?` + cron `0 3 0 * * ?` | Flush buffer, aggregate daily metrics, cleanup data |
| cms-service | `ContentScheduler` (2 method) | tiap jam (`0 0 * * * *`, `0 30 * * * *`) | Content scheduling |
| account-service | `BudgetService` reset | cron `0 0 0 * * ?` | Reset budget period |
| account-service | `AccountServiceApplication` bootstrap | saat boot | Initialisasi |
| backoffice-service | `KycPiiBackfillRunner` | initial delay 10s + interval configurable | Backfill enkripsi PII KYC lama |
| promotion-service | `CashbackSagaOrchestrator` retry scheduler (`ScheduledExecutorService`) | programmatic (bukan `@Scheduled`) | Retry step saga cashback yang gagal |

> ⚠️ Catatan metodologi (verifikasi Context7 2026-08-11): `@KafkaListener`/`@Scheduled` bukan satu-satunya mekanisme Spring — consumer bisa didaftarkan programmatic via `KafkaMessageListenerContainer` + `setMessageListener`, dan task via `TaskScheduler.schedule()`. Audit repo: `KafkaConfig` (cms/lending) hanya set `AckMode`, tidak ada container programmatic → tidak ada consumer yang terlewat. Mekanisme programmatic yang terlewat dari scan annotation: (1) `sagaRetryScheduler` (retry saga), (2) **`ArtemisCommandConsumer` notification-service** — JMS consumer manual via `session.createConsumer` + `@Observes StartupEvent` (bukan `@JmsListener`) — keduanya sudah ditambahkan. `@PostConstruct` inisialisasi (RouteRegistry, RetryAndTimeoutService, WalletGrpcAdapter) tidak tercantum karena bersifat inisialisasi, bukan fitur.
>
> ⚠️ Temuan doc-staleness: `SERVICES.md` mengklaim statement-service "Async generation via Kafka events" — **tidak benar** (statement-service hanya `@EnableKafka`, tidak ada consumer/producer Kafka di code; generation sync via REST).

## gRPC Service

| Service | Class | Fungsi |
|:---|:---|:---|
| wallet-service | `WalletGrpcService` (`@GrpcService`) | gRPC server: balance, reserve, commit, release, credit, debit, transfer — dipakai transaction/billing/investment/fx/statement (atau lewat gateway `GrpcBridgeResource`) |

---

*Last updated: 2026-08-11. Hanya yang terekspos annotation; fitur dalam `@Component` tanpa annotation tidak tercakup.*
