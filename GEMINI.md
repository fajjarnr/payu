# GEMINI.md - PayU Digital Banking Platform

> [!IMPORTANT]
> **Source of Truth**: File ini adalah salinan dari `GEMINI.md` yang ada di root project. Gunakan file di root project sebagai referensi utama untuk instruksi AI.

> AI Assistant Guidelines & Project Context for Gemini, Claude, and other Agents

---

## 📋 Project Overview

**PayU** adalah **core banking & payment gateway platform** yang dibangun dengan arsitektur microservices di atas **Red Hat OpenShift 4.20+** ecosystem. Platform ini dirancang sebagai payment infrastructure berskala enterprise yang **diintegrasikan oleh multiple project eksternal** (TokoBapak, Nobar, dll.).

### Quick Facts

| Attribute             | Value                                               |
| :-------------------- | :-------------------------------------------------- |
| **Project Name**      | PayU                                                |
| **Type**              | Core Banking & Payment Gateway Platform             |
| **Architecture**      | Scalable Microservices + Event-Driven + Hexagonal   |
| **Primary Languages** | Java 21, Python 3.12, TypeScript                    |
| **Key Integrations**  | TokoBapak (e-commerce escrow), Nobar (subscription) |
| **Gateway Standard**  | SNAP-BI (Bank Indonesia API Standard)               |

## ⚡ Quick Commands (for AI Agents)

| Action                       | Command                                                    |
| :--------------------------- | :--------------------------------------------------------- |
| **Build Backend**            | `mvn -f backend/pom.xml clean package -DskipTests -T 1C`   |
| **Run Web App**              | `cd frontend/web-app && npm run dev`                       |
| **Run Dev Docs**             | `cd frontend/developer-docs && npm run dev`                |
| **Start Local Infra**        | `podman compose up -d` (default) or `docker compose up -d` |
| **Run All Tests**            | `make test` or `./scripts/run-all-tests.sh`                |
| **Run Single Service Tests** | `./scripts/test-single-service.sh <service-name>`          |
| **Check Services**           | `oc get pods` or `podman ps`                               |

## 📌 Fast Entry Points

| File                           | Tujuan                                                  |
| :----------------------------- | :------------------------------------------------------ |
| `docs/INDEX.md`                | Doc map & navigation hub                                |
| `docs/roadmap/TODOS.md`        | **Bug backlog & open items**                            |
| `docs/roadmap/PROGRESS.md`     | Deployment history, scorecard, DORA metrics             |
| `docs/roadmap/SERVICES.md`     | **Detailed service status summary**                     |
| `docs/roadmap/GATEWAY_ARCH.md` | **Gateway architecture** — gap analysis TokoBapak/Nobar |
| `docs/guides/LESSONS.md`       | Implementation patterns & lessons learned               |
| `CHANGELOG.md`                 | Version history (ISO 8601, semver, no duplicates)       |

---

## 🏗️ Architecture Overview

### Technology Stack

| Layer                   | Red Hat Product                    | Portable Alternative |
| :---------------------- | :--------------------------------- | :------------------- |
| **Container Platform**  | Red Hat OpenShift 4.20+            | Kubernetes           |
| **Core Banking**        | Red Hat Runtimes (Spring Boot 3.4) | Spring Boot          |
| **Supporting Services** | Red Hat Build of Quarkus 3.x       | Quarkus              |
| **ML/Analytics**        | Python 3.12 (UBI-based)            | FastAPI              |
| **Database**            | Crunchy PostgreSQL 16              | Any PostgreSQL       |
| **Caching**             | Red Hat Data Grid (RESP mode)      | Redis, ElastiCache   |
| **Event Streaming**     | AMQ Streams (Kafka)                | Apache Kafka         |
| **Message Queue**       | AMQ Broker (Artemis)               | ActiveMQ Artemis     |
| **Identity**            | Red Hat Build of Keycloak 26.1     | Keycloak, Auth0      |
| **Logging**             | OpenShift Logging (LokiStack)      | Grafana Loki         |
| **Monitoring**          | OpenShift Monitoring               | Prometheus/Grafana   |
| **Developer Hub**       | Red Hat Developer Hub              | Backstage.io (CNCF)  |
| **Service Mesh**        | OpenShift Service Mesh             | Istio                |

> **Catatan**: Daftar lengkap microservices dan shared libraries dapat dilihat di `docs/roadmap/SERVICES.md` atau ditelusuri langsung pada direktori `backend/services/` dan `backend/shared/`.

### 🏛️ 14 Immutable Laws of PayU Architecture

| # | Law | Deskripsi |
|:--|:----|:----------|
| 1 | **Domain-Driven Boundaries** | Setiap service = 1 bounded context. Cross-domain hanya via events atau well-defined APIs |
| 2 | **Hexagonal Architecture** | Core business logic terisolasi dari infrastructure. External deps via ports & adapters |
| 3 | **Event-First Communication** | Prefer async events over sync HTTP untuk cross-service state |
| 4 | **Immutable Financial Records** | No UPDATE/DELETE pada data keuangan. Semua perubahan via entry baru + audit trail |
| 5 | **Zero Trust Security** | Setiap service authenticate setiap request. No implicit trust based on network |
| 6 | **API-First Design** | OpenAPI/AsyncAPI contracts SEBELUM implementasi dimulai |
| 7 | **Configuration as Code** | Semua infrastructure dan config di Git. No manual changes to production |
| 8 | **Observability by Default** | Logs, metrics, traces wajib sebelum deploy. No deployment tanpa observability |
| 9 | **Graceful Degradation** | Circuit breakers dan fallbacks wajib untuk handle downstream failures |
| 10 | **Data Residency Compliance** | User data stays within regional boundaries. Explicit residency tags on PII |
| 11 | **Independent Deployability** | Services deployable dan scalable independently. No coordinated releases |
| 12 | **Test Automation First** | No merge tanpa automated tests. Coverage thresholds enforced di CI |
| 13 | **Documentation as Code** | ADRs, API specs, runbooks versioned alongside code di Git |
| 14 | **Continuous Improvement** | 20% sprint dedicated untuk tech debt, tooling, dan developer experience |

### 🎯 Technology Radar

| Ring | Technologies |
|:-----|:-------------|
| **ADOPT** | Java + Spring Boot, TypeScript, Python + FastAPI, Next.js, Expo SDK, React Native, PostgreSQL, Redis, Kafka (Strimzi), ArgoCD, Tekton, Istio, Tailwind CSS |
| **TRIAL** | Kotlin (Android modules), Serverless/Knative, TimescaleDB (time-series analytics) |
| **ASSESS** | Go (high-performance utilities) |
| **HOLD** | MongoDB (avoid for new services), Vue.js (legacy only) |

### 📈 DORA Metrics Targets

| Metric | PayU Target |
|:-------|:-----------:|
| **Deployment Frequency** | ≥ 1 per day |
| **Lead Time for Changes** | < 4 hours |
| **Mean Time to Recovery** | < 30 minutes |
| **Change Failure Rate** | < 10% |

---

## ⚡ Decentralized Orchestration (Swarm Mode)

Platform PayU didesain untuk dikembangkan menggunakan pola **Decentralized Parallel Execution**. AI Assistant tidak bekerja sebagai monolit, melainkan sebagai orkestrator yang mendispatch tugas ke agen spesialis secara simultan.

### Swarm Principles (Parallel Dispatch)

1. **Independent Domains**: Dispatch one agent per independent problem domain (misal: perbaiki 3 file _test_ yang gagal secara bersamaan jika _root cause_-nya berbeda).
2. **Focused Execution**: Beri tiap sub-agent batasan yang jelas (scope spesifik, dilarang edit file di luar scope, dan output yang diminta).
3. **Full-stack Parallelism**: Tugas fitur didelegasikan ke UI/Frontend dan Backend secara bersamaan untuk reduksi waktu eksekusi hingga 80%.
4. **Specialized Handshake**: Setiap agen wajib proaktif memanggil agen pendukung (contoh: minta `@auditor` cek security) tanpa menunggu instruksi manual.
5. **Collision Guard**: Eksekusi paralel **HANYA** jika menyentuh _file_ atau _service_ yang berbeda. Jika berbagi _state_ atau _file_ yang sama, wajib sequential.
6. **Isolated Workspaces**: Untuk pengembangan fitur berskala besar secara paralel, gunakan `git worktree` (misal di folder `.worktrees/`) agar tiap agen memiliki isolasi environment yang bersih tanpa mengotori _branch_ utama. Pastikan folder tersebut masuk ke `.gitignore` dan _test baseline_-nya hijau sebelum mulai.

---

## 📁 Project Structure

```
payu/
├── .agents/              # AI Agent Ecosystem (Symlinked to .claude/ & .opencode/)
│   ├── skills/           # AI Skills (Logic, Stack, Standards)
│   ├── agents/           # Specialized Sub-agents (System-level prompts)
│   ├── workflows/        # SOP for complex tasks (MUST READ BEFORE EXECUTION)
│   └── resources/        # Shared assets (shadcn components, templates)
├── backend/             # Microservices implementation
│   ├── shared/          # Shared starters & libraries
│   ├── simulators/      # External service mocks (BI-FAST, QRIS, dll)
│   └── [services]/      # Individual service implementations
├── frontend/            # All frontend applications
│   ├── web-app/         # Digital Banking UI (Next.js 15+)
│   ├── mobile/          # Mobile App (Expo/React Native)
│   └── developer-docs/  # Partner Portal (Next.js)
├── sdk/                 # Client SDKs for external integration
├── docs/                # Project documentation
│   ├── architecture/    # ARCHITECTURE.md
│   ├── product/         # PRD.md
│   ├── operations/      # Runbooks, DISASTER_RECOVERY.md
│   ├── guides/          # AI Skills Guide (termasuk file ini)
│   ├── security/        # Security policies
│   └── roadmap/         # TODOS.md · PROGRESS.md · GATEWAY_ARCH.md
├── infrastructure/      # OpenShift, Helm, Tekton, ArgoCD
├── scripts/             # Automation scripts (backup, deploy, test)
├── tests/               # Gatling (Performance), Pytest (Regression), E2E
├── .editorconfig        # Code formatting rules
├── .env.example         # Environment variables template
├── CODE_OF_CONDUCT.md   # Community guidelines
├── LICENSE              # Proprietary license
├── SECURITY.md          # Security policy
└── CHANGELOG.md         # Detailed version history
```

---

## 🛠️ Development Guidelines

### Standard Operating Procedures (SOP)

1. **Shared Starters**: Selalu gunakan `security-starter`, `resilience-starter`, dan `cache-starter` untuk fitur-fitur cross-cutting. Jangan implementasi manual di level service.
2. **DTO First**: Definisikan DTO/Request/Response di package `interfaces.dto` sebelum implementasi logic.
3. **Port-Adapter Interface**: Gunakan Hexagonal Architecture untuk core services. Semua external communication harus lewat Port interface di domain layer.
4. **Error Handling**: Gunakan `GlobalExceptionHandler` dan custom `BusinessException` dengan error codes yang unik (e.g., `ACC_001`).
5. **Annotation Processor Fallback**: Jika Lombok (`@Getter`, `@Setter`, `@Builder`, `@Slf4j`) gagal dikompilasi setelah 2 upaya perbaikan konfigurasi, segera beralih ke implementasi manual (explicit) untuk menjamin stabilitas build.
6. **Enum Placement**: Selalu definisikan Enum domain sebagai file top-level (bukan inner class) untuk menghindari masalah resolusi simbol dan kompatibilitas dengan Lombok/JPA.
7. **Doc Sync**: Setiap update signifikan WAJIB update `CHANGELOG.md`. Roadmap terbagi 3 file: `TODOS.md` (bugs), `PROGRESS.md` (history), `GATEWAY_ARCH.md` (arsitektur). Jangan campurkan konten.
8. **Idempotency**: Semua endpoint payment/transfer WAJIB support `X-Idempotency-Key` header. Ini absolute requirement untuk gateway role.
9. **Gateway-First Thinking**: Sebelum mengimplementasikan fitur, tanya: "Apakah ini relevan untuk payment gateway yang melayani TokoBapak/Nobar, atau hanya untuk consumer app?" Lihat `docs/roadmap/GATEWAY_ARCH.md` untuk konteks.

10. **Frontend Principles**: Untuk Next.js web-app, maksimalkan Server Components; gunakan `"use client"` se-minimal mungkin hanya pada _leaf components_ yang membutuhkan interaksi DOM/State.
11. **Event Publishing**: Semua service yang publish events WAJIB menggunakan `outbox-starter` (bukan direct `kafkaTemplate.send()`). Format CloudEvents 1.0.2 wajib.
12. **Topic Naming**: Format `payu.<domain>.<event-type>.<version>` (contoh: `payu.wallet.transfer-initiated.v1`). DLQ: tambahkan `.dlq` suffix.
13. **Port 8080 Standard**: Semua backend service WAJIB listen di port 8080 internal. External port mapping via compose/K8s.
14. **Container Base**: Gunakan UBI9 (Red Hat Universal Base Image). Run as non-root (UID 1001), drop ALL capabilities, read-only root filesystem.
15. **Conventional Commits**: Format `type(scope): message` wajib. Branch naming: `feature/*`, `fix/*`, `chore/*`. No force push ke protected branches.
16. **Semantic Versioning**: Format `MAJOR.MINOR.PATCH` (e.g., `1.8.7`). Rules:
  - **MAJOR**: Breaking changes (API contract, database schema, event format)
  - **MINOR**: New features, new services, non-breaking additions
  - **PATCH**: Bug fixes, config changes, infrastructure tweaks, YAML fixes
  - **Pre-release**: `-alpha.N`, `-beta.N`, `-rc.N` untuk staging
  - **Image tags**: Selalu match dengan git tag (e.g., `1.8.7` = commit `v1.8.7`)
  - **No duplicates**: Setiap version hanya boleh ada 1x di CHANGELOG.md
  - **Date format**: ISO 8601 (`YYYY-MM-DD`) untuk semua version entries
17. **Financial Calculations**: `BigDecimal` ONLY (NEVER `float`/`double`). Rounding mode: `HALF_EVEN`. Double-entry ledger wajib (setiap transaksi = debit + credit entry).
18. **Development Loop (Error-Fix-Test-Tag-Deploy-Repeat)**: Setiap troubleshooting dan resolusi platform wajib mengikuti siklus sekuensial:
  - **Error**: Analisis log, trace, atau test failure secara mendalam untuk mencari akar masalah (root cause) sebelum menyentuh kode.
  - **Fix**: Terapkan perbaikan kode seminimal mungkin untuk menyelesaikan akar masalah tersebut.
  - **Test**: Jalankan unit test lokal atau E2E integration test untuk memastikan perbaikan bekerja secara lokal.
  - **Build New Tag**: Build container image baru dengan tag versi target (minimal `1.8.8` atau tag increment terbaru dari `docs/roadmap`).
  - **Deploy**: Push image ke registry dan jalankan deployment/rollout di target cluster.
  - **Repeat**: Verifikasi dengan E2E test di cluster. Jika gagal, ulangi siklus dari awal.

### Testing Guidelines (TDD)

1. **The Iron Law of TDD**: NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST. Write the test, watch it fail, then write minimal code to pass. If code is written before tests, delete it and start over.
2. **Testing Anti-Patterns**: Never test mock behavior. Test real behavior instead. Never add test-only methods to production classes.
3. **Unit Tests**: 100% coverage untuk core domain/critical flows; minimum 80–90% untuk non-critical modules (exception harus didokumentasikan).
4. **ArchUnit**: Pastikan setiap service baru memiliki `ArchitectureTest` untuk menjaga layering.
5. **Testcontainers**: Gunakan untuk integration tests yang membutuhkan PostgreSQL atau Kafka (jika enviroment memungkinkan).
6. **UI/Frontend Testing**: Untuk aplikasi React/Next.js, fokus pada _user behavior_ menggunakan React Testing Library. Jangan menguji _internal state_ atau CSS, melainkan uji apa yang dilihat dan bisa diinteraksikan oleh pengguna.

---

## 🛡️ Security & Compliance

AI Assistant harus mematuhi aturan keamanan berikut:

### Aturan Dasar

1. **PII Protection**: Data sensitif (NIK, PIN, Phone) harus di-mask di logs dan di-encrypt di DB (pake `@Sensitive` & `security-starter`).
2. **No Credentials**: Jangan pernah menuliskan password/key di `application.yml`. Gunakan placeholder atau Vault reference.
3. **Idempotency**: Semua API kritis (transfer, payment) harus mendukung idempotency key.

### Zero-Trust & Network Security

4. **mTLS Strict Mode**: Mutual TLS wajib antar semua service di mesh. No plain HTTP internal.
5. **CSP Headers**: Strict Content-Security-Policy di semua web applications.
6. **Secret Scanning**: Gitleaks + TruffleHog wajib di CI pipeline sebelum build image.
7. **Image Signing**: Semua production container images wajib signed dengan Cosign + SBOM (Syft).

### Token & Auth Standards

| Platform | Storage | Catatan |
|:---------|:--------|:--------|
| **Web** | HttpOnly Cookies / Memory | NEVER localStorage untuk tokens |
| **Mobile** | SecureStore (iOS Keychain / Android Keystore) | Encrypted storage wajib |

8. **Biometric Auth**: Wajib untuk semua financial mutations di mobile (transfer, payment, PIN change).
9. **SSL Pinning**: Mandated untuk production mobile API calls (anti MiTM).

### Container Security

10. **Non-Root Execution**: Semua container run as UID 1001, drop ALL capabilities, `seccompProfile: RuntimeDefault`.
11. **Read-Only Filesystem**: Root filesystem read-only. Gunakan `emptyDir` untuk `/tmp` dan `/app/logs`.
12. **SELinux Enforced**: JANGAN pernah `setenforce 0` di production. Gunakan volume label `:Z` untuk private mounts.

### Data Governance (UU PDP Compliance)

| Klasifikasi | Contoh Data | Perlakuan |
|:------------|:------------|:----------|
| **Public** | Nama produk, kurs | Bebas akses |
| **Internal** | Transaction ID, timestamps | Akses terbatas per service |
| **Confidential** | Email, nama lengkap, alamat | Encrypted at rest, masked di logs |
| **Restricted** | NIK, PIN, nomor rekening | Encrypted + tokenized, akses audit-logged |

13. **Data Retention**: Definisikan retention policy per data class. Right to erasure wajib didukung.
14. **Data Lineage**: Setiap transformasi data harus traceable dari source ke destination.

---

## 🌐 API & Integration Standards

### 1. REST API Naming & Structure
- **Versioned Path**: Selalu sertakan version prefix pada resource URI (contoh: `/v1/accounts`).
- **Nouns Only**: Path menggunakan kata benda jamak (plural nouns) bergaya kebab-case (contoh: `/bank-accounts`).
- **Deprecation**: Selalu sertakan header `Deprecation` dan `Sunset` jika merilis versi API baru.

### 2. Standard Response & Error Formats
- **Success Envelope**: Semua response sukses dibungkus envelope standard (`success` boolean, `data` object, `meta` object).
- **Standardized Errors**: Gunakan spesifikasi RFC 9457 (Problem Details) untuk error responses. Wajib mencantumkan field `type`, `title`, `status`, `detail`, `instance`, `error_code`, dan `trace_id`.

### 3. Integration Robustness
- **Timeout Management**: Koneksi eksternal wajib menetapkan Connect & Read timeouts secara eksplisit (dilarang menggunakan library default).
- **Retry Pattern**: Semua call partner/bank wajib menggunakan Exponential Backoff + Jitter untuk status 5xx dan 429.
- **Webhook Security**: Inbound webhooks wajib memverifikasi signature HMAC menggunakan rolling timestamp. Gunakan caching Redis untuk pengecekan idempotensi `webhook_id`.

---

## 🗄️ Database & Schema Standards

### 1. The Immutable Ledger Pattern
- **Double-Entry Ledger**: Data keuangan tidak boleh di-UPDATE atau di-DELETE. Semua mutasi balance dicatat sebagai entry baru (append-only) di tabel ledger.
- **Ledger Invariant**: Balance saat ini adalah `SUM(amount)` dari baris ledger bersangkutan. Gunakan snapshot/materialized view untuk real-time query, diperbarui via triggers/CDC.
- **Reversal Entry**: Pembatalan atau koreksi transaksi dilakukan dengan membuat baris balance baru bermuatan korektif, bukan menghapus baris lama.

### 2. Data Definition & Performance
- **Data Types**: Kolom finansial wajib bertipe `DECIMAL(19,4)`. Timestamps wajib menggunakan `TIMESTAMPTZ`.
- **Primary Keys**: Gunakan UUID dengan default generator `gen_random_uuid()` untuk kompatibilitas skala distributed.
- **Index Guard**: Buat partial index untuk data yang sangat aktif (hot data seperti `status = 'PENDING'`). Gunakan keyword `CONCURRENTLY` saat migrasi index di production agar tidak men-lock tabel.
- **Schema Migrations**: Semua perubahan schema wajib lewat script Flyway dengan penamaan standard bergaya sequential (`V[No]__description.sql`).

### 3. Security & HA
- **Row-Level Security**: Terapkan RLS untuk multi-tenancy isolation di database level.
- **PII Encryption**: Encrypt kolom NIK, PIN, dan data restricted lainnya menggunakan extension `pgcrypto` (`pgp_sym_encrypt`).

---

## 🐍 Python & Machine Learning Standards

### 1. FastAPI Architecture
- **Domain-Based Directory**: Struktur service berbasis domain terisolasi (`models/`, `api/`, `services/`, `utils/`).
- **Async Execution Strategy**: Gunakan `async def` eksklusif untuk I/O-bound tasks (database, network I/O).
- **Event Loop Protection**: Untuk CPU-bound tasks (seperti run ML model inference), gunakan `def` biasa agar FastAPI menjalankan route tersebut di internal thread pool secara otomatis, atau dispatch via `run_in_executor`.

### 2. Validation & Inference
- **Pydantic v2**: Semua parsing dan validasi input data wajib dideklarasikan secara strict menggunakan Pydantic v2.
- **ONNX Pipeline**: Simpan dan deploy model ML dalam format ONNX Runtime (`.onnx`) untuk mengoptimalkan latency inference di production.
- **Feature Store & Cache**: Gunakan caching layer untuk meminimalkan latensi saat query feature engineering dari TimescaleDB.

### 3. Model Monitoring & Guardrails
- **Inference Metrics**: Promosikan latency dan prediction distribution ke Prometheus.
- **Model Drift**: Monitor feature drift secara berkala menggunakan metric Population Stability Index (PSI).
- **LLM Guardrails**: Sanitasi input LLM (redact PII seperti NIK/Credit Card) secara lokal sebelum dikirim ke API external, dan validasi output secara ketat sebelum ditampilkan ke customer.

---

## 💻 Frontend TypeScript & Next.js Standards

### 1. Strict TypeScript Patterns
- **Const Assertions**: Dilarang menggunakan enum standard. Selalu gunakan `const` objects + `typeof` mapping untuk tipe status/type.
- **Flat Interfaces**: Hindari nested inline interfaces. Ekstrak data model ter-nest ke dalam interface mandiri yang reusable.
- **No-Any Policy**: Menolak penulisan tipe `any`. Gunakan `unknown` digabungkan dengan type-guard function (`isSomething(input: unknown): input is Something`).

### 2. Next.js 15+ Core Strategies
- **Server Components (RSC)**: Maksimalkan RSC untuk heavy operations, data fetching, dan loading data rahasia. Client components dibatasi hanya untuk elemen interaktif (form, dialog, dsb).
- **Form Actions**: Gunakan React 19 `useActionState` Hook untuk interaksi form dengan Server Actions secara robust.
- **Async Next.js APIs**: Selalu gunakan kata kunci `await` saat memanggil runtime API asinkron Next.js 15 (`cookies()`, `headers()`, `params`).
- **Eliminate Waterfalls**: Terapkan parallel fetching (`Promise.all`) daripada sequential await untuk mencegah perlambatan rendering halaman.

---

## 🛰️ AI Orchestration & Skill Map

Untuk efisiensi eksekusi dan meminimalkan duplikasi instruksi, AI Agent wajib memetakan tugasnya ke spesialisasi skill berikut sebelum mengeksekusi perubahan:

| Domain Tugas | Skill Utama (.agents/skills/) | Deskripsi & Kegunaan |
|:---|:---|:---|
| API Contract & REST Design | `api-architect` | Desain endpoint SNAP-BI, error response, OpenAPI & Spectral linting |
| Database & Schema Migration | `data-architect` | Desain DDL PostgreSQL, setup index, Flyway scripts, pgcrypto, TimescaleDB |
| Machine Learning & LLM Ops | `ai-engineer` | Python microservices, FastAPI, model ONNX, prompt engineering, guardrails |
| Next.js & TypeScript | `frontend-architect` | UI web-app, state management (Zustand/React Query), strict TS types |
| Mobile Development | `mobile-architect` | React Native & Expo Router, secure store, Skia rendering, offline sync |
| Security Audit & PCI-DSS | `security-audit` (workflow) | Cek kebocoran PII, audit dependency, mTLS verification |
| TDD & Quality Assurance | `quality-engineer` | Unit & integration tests, Playwright E2E, mock strategies, ArchUnit |

---

## 🎨 Design System Principles (Premium Emerald)

| Aspek | Standar |
|:------|:--------|
| **Brand Color** | Primary: `#10b981` (Emerald-500). Success: `#22c55e`. Warning: `#f59e0b`. Error: `#ef4444` |
| **Typography** | Headers/Display: **Outfit**. Body/UI: **Inter**. Monospace: **JetBrains Mono** |
| **Min Font Size** | `text-xs` (12px) — NEVER smaller |
| **Corner Radius** | Cards: `rounded-2xl` (16px). Buttons: `rounded-xl` (12px) |
| **Dark Mode Surface** | `bg-gray-950` + `bg-white/5` overlays. Glassmorphism: `backdrop-blur-xl` |
| **Layout** | Dashboard = full-width fluid (NO `max-w-7xl` centering). Mobile-first responsive |
| **A11y** | WCAG 2.1 AA: contrast 4.5:1, keyboard nav, touch targets > 44px, ARIA labels |
| **Icons** | SVG only (Heroicons/Lucide). No emojis. Fixed `w-6 h-6` |
| **Philosophy** | "Anti-AI Slop" — bespoke, premium, memorable. Bukan generic template |

---

## 📱 Mobile Standards

| Aspek | Standar |
|:------|:--------|
| **Core Stack** | Expo SDK 54+, React Native 0.77+ (Bridgeless), Expo Router v5 (Typed Routes) |
| **Styling** | NativeWind v5 (Tailwind CSS v4) + `react-native-unistyles` |
| **Networking** | TanStack Query v5 dengan `networkMode: 'offlineFirst'` sebagai default |
| **Graphics** | Shopify Skia (high-performance 2D). Animations: Reanimated 4 |
| **Security** | SecureStore untuk JWT/PIN. Biometric auth untuk financial mutations. SSL Pinning wajib |
| **Env Vars** | Gunakan `EXPO_PUBLIC_` prefix. JANGAN pernah taruh secrets (private keys, DB passwords) di sini |
| **Anti-Log** | NEVER log PII atau bearer tokens di production builds |

---

## 💰 FinOps & Financial Integrity

1. **Double-Entry Ledger**: Setiap transaksi = minimal 2 ledger entries (debit + credit). Ledger WAJIB selalu balance: `sum(credits) - sum(debits) == current_balance`.
2. **Reconciliation**: T+1 automated reconciliation wajib untuk semua settlement flows.
3. **Cost Attribution**: Semua K8s resources WAJIB punya label `cost-center` dan `owner`.
4. **Capacity Planning**: Sprint capacity allocation: 60% features, 15% bug fixes, 20% tech debt, 5% on-call buffer.

---

## 🗼 Testing Pyramid & Coverage Targets

| Layer | Target Coverage | Max Waktu Eksekusi | Frekuensi |
|:------|:----------------|:-------------------|:----------|
| **Unit** | > 80% (100% untuk core domain) | < 5 menit | Setiap commit |
| **Integration** | > 70% (critical paths) | < 10 menit | Setiap PR |
| **Contract** | 100% (public APIs) | < 5 menit | Setiap PR |
| **E2E** | Critical user journeys | < 20 menit | Pre-deploy |
| **Performance** | Load scenarios | 30-60 menit | Weekly / Pre-release |

**Financial Integrity Tests (Wajib):**
- No `float`/`double` dalam domain layer (enforce via ArchUnit)
- Ledger balance invariant: `sum(credits) == sum(debits) + current_balance`
- Idempotency stress test: 10 concurrent duplicate requests → hanya 1 yang diproses

---

## 🧠 Reasoning Bank (Cognitive Model)

To emulate "Adaptive Intelligence" without a persistent database, all Agents MUST follow this cognitive cycle:

### 1. Pattern Recognition (Context Loading)

Before solving a problem, ask:

- "Have I seen this error pattern (`api_errors_increase`) before?"
- "Does this architecture match a known pattern (`Microservices` vs `Modular Monolith`)?"
- **Action**: Check `docs/adr/` and `docs/guides/` for historical context.

### 2. Strategy Optimization (Planning)

Don't just execute. Optimize.

- **Option A**: Quick Fix (Low risk, high speed)
- **Option B**: Refactor (High risk, long term benefit)
- **Decision**: Select strategy based on _Context Discovery_ (Team size, Timeline).

### 3. Continuous Learning (Synthesis)

After completing a complex task (Workflow), generate a "Lesson Learned" block in the summary:

```markdown
### 🧠 Meta-Learning

- **Observation**: Parallel dispatching failed for shared files.
- **Correction**: Use Sequential Chain for shared resources in future.
```

### 4. Systematic Debugging Methodology (Strict Protocol)

**Core Principle:** NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST.

- **Rule of Reproduction**: When I report a bug, don't start by trying to fix it. Instead, start by writing a test that reproduces the bug. Then, have subagents try to fix the bug and prove it with a passing test.
- **Don’t fight errors!**: Whenever you encounter the same error twice, research the web or check context7 and find 3-5 possible ways to fix it. Then choose the most efficient solution and implement it.
- **Error, Fix, Test, Build New Tag, Deploy, Repeat (Development Loop)**: Standard sequential loop for platform resolution:
  1. **Error**: Capture and analyze the error log, trace, or test failure to identify the root cause.
  2. **Fix**: Implement the minimum correct code modification to address the root cause.
  3. **Test**: Run the local test suite (unit tests or E2E integration test files) to verify the fix works locally.
  4. **Build New Tag**: Build the new container image tagged with the target deployment tag (e.g. `1.8.8` or next incremental version as defined in `docs/roadmap`).
  5. **Deploy**: Push the image to the OpenShift registry and apply/rollout the updated deployment config.
  6. **Repeat**: Monitor logs and run E2E/integration tests against the deployed cluster environment. If any issues persist, repeat the loop from Step 1.

> [!IMPORTANT]
> **The Iron Law**: If you haven't completed Phase 1 (Root Cause Investigation), you are NOT allowed to propose or implement fixes.
> For detailed patterns and case studies, use the **`debugging-methodology` skill** ([SKILL.md](../.agents/skills/debugging-methodology/SKILL.md)).

When encountering ANY technical issue (test failure, bug, performance issue):

**Phase 1: Root Cause Investigation**

1. **Read Error Messages**: Don't skip stack traces. Note error codes.
2. **Reproduce Consistently**: If you can't reproduce it, you can't fix it.
3. **Trace Data Flow**: Add logs/prints at component boundaries to see WHERE it breaks.
4. **Gather Evidence**: Verify environment, config, and state before changing code.

**Phase 2: Pattern Analysis**

1. **Find Working Examples**: Compare against similar working code in the project.
2. **Identify Differences**: What is different? (Dependencies, versions, config).

**Phase 3: Hypothesis & Testing**

1. **Form Single Hypothesis**: "I think X is causing Y because Z".
2. **Test Minimally**: Change ONE thing.
3. **Verify**: Did it fix it? If no, revert and try a new hypothesis.

**Phase 4: Implementation**

1. **Create Failing Test**: Prove the bug exists with a test case (TDD).
2. **Implement Fix**: Address the root cause.
3. **Verify Fix**: Ensure test passes and no regressions.

**Red Flags (STOP IMMEDIATELY if you do this)**:

- "Quick fix for now"
- "Just try changing X"
- "Add multiple changes and hope"
- "One more fix attempt" (if > 2 failed attempts) -> **Stop and Question Architecture**.

## 🤝 Collaboration Modes (Pair Programming)

To align with the user's intent, adopt one of these modes when requested:

| Mode               | Behavior                                                             | When to Use                                  |
| :----------------- | :------------------------------------------------------------------- | :------------------------------------------- |
| **Driver Mode**    | Implement code actively, proposing solutions.                        | "Implement this feature", "Fix this bug"     |
| **Navigator Mode** | Plan, review, and guide; let User code.                              | "Help me plan", "What do you think of this?" |
| **TDD Mode**       | **Strictly** write tests before implementation (Red-Green-Refactor). | "Test first", "Ensure high coverage"         |
| **Review Mode**    | Audit code for security, style, and logic. No implementation.        | "Review my PR", "Check for bugs"             |
| **Mentor Mode**    | Explain concepts, provide examples, avoid direct solution.           | "Explain how this works", "Teach me"         |

---

## 🔄 Workflows & Procedures

AI Assistants SHOULD follow established workflows in `.agents/workflows/` for complex operations. **URGENT**: Karena AI tidak selalu auto-discover folder `workflows`, asisten wajib melakukan `ls .agents/workflows/` atau membaca file di dalamnya secara proaktif sebelum memulai tugas arsitektural.

- **`/antigravity-lifecycle`**: Standard SDLC lifecycle (Observe -> Plan -> Execute -> Verify).
- **`/multi-agent-coordination`**: Parallel task coordination and synthesis for multi-service changes. **(Principally handled by Main AI)**
- **`/new-service-scaffolding`**: Workflow untuk scaffolding microservice baru di platform PayU dengan arsitektur Hexagonal dan konfigurasi standar.
- **`/orchestration-protocol`**: Advanced AI Orchestration, Task Management, and Core Engineering Principles for PayU.
- **`/security-audit`**: Workflow untuk melakukan audit keamanan pada service PayU sesuai standar PCI-DSS dan OJK.

## 🛰️ Advanced Orchestration Protocol (v2.0)

This section defines the high-performance operational protocol for all AI Agents on the PayU platform.

### 1. Workflow Orchestration Standards

- **Design-First Hard Gate**: Do NOT write code, scaffold projects, or take implementation actions for new features until you have explored the context, presented a design, and the user has approved it.
- **Plan Mode Default**: Enter plan mode for ANY non-trivial task (3+ steps or architectural decisions).
- **Graceful Halt**: If something goes sideways, STOP and re-plan immediately – don't keep pushing.
- **Verification-First Planning**: Use plan mode for verification steps, not just building.
- **Detailed Specs**: Write detailed specs upfront to reduce ambiguity.

### 2. Subagent Strategy

- **Liberal Subagent Usage**: Use subagents liberally to keep main context window clean.
- **Offload & Parallelize**: Offload research, exploration, and parallel analysis to subagents.
- **Compute Scaling**: For complex problems, throw more compute at it via subagents.
- **Focused Execution**: One task per subagent for focused execution.
- **Subagent Code Review**: Before completing major features or merging, dispatch a reviewer subagent. Give it the diff (`HEAD_SHA` vs `BASE_SHA`) and the original requirements, to independently flag critical issues.

### 3. Self-Improvement Loop

- **Pattern Capturing**: After ANY correction from the user: update `docs/guides/LESSONS.md` with the pattern.
- **Recursive Rules**: Write rules for yourself that prevent the same mistake.
- **Ruthless Iteration**: Iteratively refine lessons until the mistake rate drops.
- **Pre-Session Review**: Review lessons at session start for relevant project context.

### 4. Verification & Completion Protocol

- **Evidence Before Claims**: NO COMPLETION CLAIMS WITHOUT FRESH VERIFICATION EVIDENCE. Never say "it should work now" or "tests pass" without actually running the command and reading the output.
- **Proof of Work**: Never mark a task complete without proving it works.
- **E2E Validation**: Run tests, check logs, and demonstrate correctness explicitly. **Do NOT proceed to merge or PR if any tests fail.**
- **Structured Completion**: Once tests pass, present exact options to the user before finishing: 1) Merge locally, 2) Create PR, 3) Keep branch as-is, 4) Discard.
- **Staff Engineer Standard**: Ask yourself: "Would a staff engineer approve this?"
- **Behavioral Diffing**: Diff behavior between main and your changes when relevant.

### 5. Architectural Elegance

- **Elegance Pause**: For non-trivial changes: pause and ask "is there a more elegant way?"
- **Refactoring for Quality**: If a fix feels hacky: "Knowing everything I know now, implement the elegant solution".
- **Balanced Engineering**: Skip this for simple, obvious fixes – don't over-engineer.
- **Internal Critique**: Challenge your own work before presenting it.

### 6. Autonomous Bug Fixing

- **Test-First Reproduction**: When I report a bug, don't start by trying to fix it. Instead, start by writing a test that reproduces the bug. Then, have subagents try to fix the bug and prove it with a passing test.
- **Evidence-Based Resolution**: Point at logs, errors, failing tests – then resolve them.
- **Zero-Context Switching**: Aim for zero context switching required from the user.
- **Proactive Maintenance**: Fix failing CI tests without being told how.

### 7. Skill Usage Protocol (Superpowers)

- **Mandatory Invocation**: If there is even a 1% chance a skill in `.agents/skills/` applies to your task, you **ABSOLUTELY MUST** read and use it before any response or action. This is not optional.
- **Anti-Rationalization**: Do not skip skills with thoughts like "This is just a simple question," "I can do this quickly," or "I know this already." Unstructured action wastes time.
- **Priority Hierarchy**: 1) User's explicit instructions (this file), 2) Skill instructions, 3) Default system prompt.

### 8. Code Review & Feedback Protocol

- **No Performative Agreement**: Do not say "You're absolutely right!" or "Great point!". Acknowledge technically or just implement the fix.
- **Verify Before Implementing**: Read, understand, and verify the feedback against the codebase. Push back with technical reasoning if the suggestion breaks existing functionality or violates YAGNI.
- **Clarify Unclear Items**: Do not guess or partially implement unclear feedback. STOP and ask for clarification.

### 📋 Task Management Protocol

- **Plan First (No Placeholders)**: Write plan to `docs/roadmap/TODOS.md` with bite-sized, checkable items. Never use placeholders like "TODO", "TBD", or "add error handling". The plan must contain exact file paths, complete code snippets, and exact test commands.
- **Verify Plan**: Check in and ask for review before starting implementation.
- **Strict Execution**: Execute tasks step-by-step exactly as written. Do not skip verifications.
- **Stop on Blockers**: If you hit a blocker, test failure, or ambiguity, **STOP and ask the user**. Do NOT guess or force through blockers.
- **Track Progress**: Mark items complete as you go.
- **Explain Changes**: High-level summary at each step.
- **Document Results**: Add bug findings ke `docs/roadmap/TODOS.md`, architectural decisions ke `docs/roadmap/GATEWAY_ARCH.md`.
- **Capture Lessons**: Update `docs/guides/LESSONS.md` after corrections.
- **Changelog**: Update `CHANGELOG.md` `[Unreleased]` section for any significant change.

**Doc Routing Rules**:

| Konten                                  | File Tujuan                    |
| :-------------------------------------- | :----------------------------- |
| Bug baru, open items, actionable todos  | `docs/roadmap/TODOS.md`        |
| Deployment status, completed milestones | `docs/roadmap/PROGRESS.md`     |
| Architecture decisions, gap analysis    | `docs/roadmap/GATEWAY_ARCH.md` |
| Version changelog                       | `CHANGELOG.md`                 |
| Implementation patterns                 | `docs/guides/LESSONS.md`       |

**Fast Path (Small Changes)**:

- Boleh skip update roadmap docs untuk perubahan kecil (<=2 file, 1 service, tanpa keputusan arsitektural).
- Tetap berikan rencana singkat + langkah verifikasi di respons.

### ⚖️ Core Engineering Principles

- **Simplicity First**: Make every change as simple as possible. Impact minimal code.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.
- **Minimal Impact**: Changes should only touch what's necessary. Avoid introducing bugs.

---

_Platform: Payment Gateway for TokoBapak & Nobar | See TODOS.md for bug backlog and open items._
