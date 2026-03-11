# GEMINI.md - PayU Digital Banking Platform

> [!IMPORTANT]
> **Source of Truth**: File ini adalah salinan dari `GEMINI.md` yang ada di root project. Gunakan file di root project sebagai referensi utama untuk instruksi AI.

> AI Assistant Guidelines & Project Context for Gemini, Claude, and other Agents

---

## ✅ Platform Status (Feb 2026)

> **Deployment**: 🟢 22/22 services live on OpenShift | **E2E Tests**: 🟢 399/399 pass
>
> ⚠️ **Code Review Status (Feb 24, 2026)**: ~117 bugs teridentifikasi dari deep code review.
> Scorecard infra/deployment tetap green, tapi logic correctness & security perlu perbaikan.
> Gateway readiness untuk integrasi TokoBapak/Nobar: 5 P0 gaps belum diimplementasikan.
>
> **Dokumen Roadmap (split Feb 24)**:
>
> - **Bug backlog & open items**: `docs/roadmap/TODOS.md`
> - **Deployment history & scorecard**: `docs/roadmap/PROGRESS.md`
> - **Gateway architecture & gap analysis**: `docs/roadmap/GATEWAY_ARCH.md`
> - **Implementation patterns**: `docs/guides/LESSONS.md`
> - **Architecture**: `docs/architecture/ARCHITECTURE.md`

---

## �📋 Project Overview

**PayU** adalah **core banking & payment gateway platform** yang dibangun dengan arsitektur microservices di atas **Red Hat OpenShift 4.20+** ecosystem. Platform ini dirancang sebagai payment infrastructure berskala enterprise yang **diintegrasikan oleh multiple project eksternal** (TokoBapak, Nobar, dll.).

### Quick Facts

| Attribute             | Value                                               |
| :-------------------- | :-------------------------------------------------- |
| **Project Name**      | PayU                                                |
| **Type**              | Core Banking & Payment Gateway Platform             |
| **Architecture**      | Scalable Microservices + Event-Driven + Hexagonal   |
| **Primary Languages** | Java 21, Python 3.12, TypeScript                    |
| **Last Updated**      | February 24, 2026                                   |
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
| `docs/roadmap/TODOS.md`        | **Bug backlog & open items** (~117 bugs, P0-P3)         |
| `docs/roadmap/PROGRESS.md`     | Deployment history, scorecard, DORA metrics             |
| `docs/roadmap/GATEWAY_ARCH.md` | **Gateway architecture** — gap analysis TokoBapak/Nobar |
| `docs/guides/LESSONS.md`       | Implementation patterns & lessons learned               |
| `backend/SERVICES_STATUS.md`   | Current service status summary                          |
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
| **Identity**            | Red Hat SSO (Keycloak 24+)         | Keycloak, Auth0      |
| **Logging**             | OpenShift Logging (LokiStack)      | Grafana Loki         |
| **Monitoring**          | OpenShift Monitoring               | Prometheus/Grafana   |
| **Developer Hub**       | Red Hat Developer Hub              | Backstage.io (CNCF)  |
| **Service Mesh**        | OpenShift Service Mesh             | Istio                |

### Microservices

| Service                | Technology         | Domain                                           |
| :--------------------- | :----------------- | :----------------------------------------------- |
| `account-service`      | Spring Boot 3.4    | User accounts, profile, multi-pocket             |
| `auth-service`         | Spring Boot 3.4    | Authentication, Risk-based MFA, Biometrics       |
| `transaction-service`  | Spring Boot 3.4    | Transfers, BI-FAST, QRIS, Sharding               |
| `wallet-service`       | Spring Boot 3.4    | Double-entry ledger, balance management          |
| `investment-service`   | Spring Boot 3.4    | Mutual funds, Gold, Robo-advisory                |
| `lending-service`      | Spring Boot 3.4    | Loans, PayLater, Credit Scoring                  |
| `fx-service`           | Spring Boot 3.4    | Currency exchange rates & conversion             |
| `statement-service`    | Spring Boot 3.4    | PDF E-Statement generation                       |
| `backoffice-service`   | Spring Boot 3.4    | Internal admin dashboard, audit                  |
| `partner-service`      | Spring Boot 3.4    | Partner integration & management                 |
| `promotion-service`    | Spring Boot 3.4    | Promo campaigns, vouchers, rewards               |
| `support-service`      | Spring Boot 3.4    | Customer support, ticketing                      |
| `compliance-service`   | Spring Boot 3.4    | Regulatory compliance, AML                       |
| `billing-service`      | Quarkus 3.x Native | Bill payments (PLN, PDAM, etc)                   |
| `notification-service` | Quarkus 3.x Native | Push, SMS, Email, WhatsApp                       |
| `gateway-service`      | Quarkus 3.x Native | API Gateway, Rate limiting                       |
| `cms-service`          | Spring Boot 3.4    | Banners, Promos, Dynamic Content                 |
| `ab-testing-service`   | Spring Boot 3.4    | UI/Feature experimentation ⚠️ **Kandidat hapus** |
| `api-portal-service`   | Quarkus 3.x Native | Centralized OpenAPI Docs & Sandbox               |
| `kyc-service`          | Python FastAPI     | OCR, Liveness Detection                          |
| `analytics-service`    | Python FastAPI     | Fraud Scoring, User Insights                     |

### Shared Libraries (backend/shared/)

| Library              | Purpose                                         |
| :------------------- | :---------------------------------------------- |
| `security-starter`   | Field encryption, Data masking, Audit logging   |
| `resilience-starter` | Circuit Breaker, Retry, Bulkhead (Resilience4j) |
| `cache-starter`      | Multi-layer caching (Redis + Caffeine)          |

Other shared modules: `api-commons`, `archunit-starter`, `events-starter`, `outbox-starter`, `saga-starter`, `flyway` (see `backend/shared/`).

---

## ⚡ Decentralized Orchestration (Swarm Mode)

Platform PayU didesain untuk dikembangkan menggunakan pola **Decentralized Parallel Execution**. AI Assistant tidak bekerja sebagai monolit, melainkan sebagai orkestrator yang mendispatch tugas ke agen spesialis secara simultan.

### Swarm Principles:

1. **Parallel Dispatching**: Tugas Full-stack didelegasikan ke `@styler` (Frontend) dan `@logic-builder` (Backend) secara bersamaan untuk reduksi waktu eksekusi hingga 80%.
2. **Specialized Handshake**: Setiap Skill (misal: `@core-banking-engineer`) memiliki instruksi eksplisit untuk memanggil spesialis lain (misal: `@tester` atau `@migrator`) jika tugas menyentuh domain mereka.
3. **Implicit Interconnectivity**: Asisten wajib secara proaktif mendispatch agen pendukung (seperti `@auditor` untuk security atau `@dx-engineer` untuk presentasi) berdasarkan jenis perubahan kode tanpa menunggu perintah manual.
4. **Collision Guard**: Parallel hanya jika file/service berbeda. Jika menyentuh file yang sama atau shared module, gunakan eksekusi sequential.

---

## 📁 Project Structure

```
payu/
├── .agent/               # AI Agent Ecosystem (Symlinked to .claude/)
│   ├── skills/           # 17 AI Skills (Logic, Stack, Standards)
│   ├── agents/           # Specialized Sub-agents (System-level prompts)
│   ├── workflows/        # SOP for complex tasks (MUST READ BEFORE EXECUTION)
│   └── resources/        # Shared assets (shadcn components, templates)
├── backend/             # Microservices implementation (22 microservices)
│   ├── shared/          # Shared Spring Boot starters
│   │   ├── security-starter/    # PII encryption, audit logging
│   │   ├── resilience-starter/  # Circuit breaker, retry, bulkhead
│   │   └── cache-starter/       # Multi-layer caching
│   ├── simulators/      # External service mocks
│   │   ├── bi-fast-simulator/   # BI-FAST mock
│   │   ├── dukcapil-simulator/  # Dukcapil mock
│   │   └── qris-simulator/      # QRIS mock
│   └── [services]/      # Individual service implementations
├── frontend/            # All frontend applications
│   ├── web-app/         # Digital Banking UI (Next.js 15+)
│   ├── mobile/          # Mobile App (Expo/React Native)
│   └── developer-docs/  # Partner Portal (Next.js)
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

### Testing Guidelines (TDD)

1. **Unit Tests**: 100% coverage untuk core domain/critical flows; minimum 80–90% untuk non-critical modules (exception harus didokumentasikan).
2. **ArchUnit**: Pastikan setiap service baru memiliki `ArchitectureTest` untuk menjaga layering.
3. **Testcontainers**: Gunakan untuk integration tests yang membutuhkan PostgreSQL atau Kafka (jika enviroment memungkinkan).

---

## 🎨 Frontend Design System (Premium Emerald)

Untuk menjaga konsistensi UI yang premium:

1. **Color Palette**: Primary `bank-green` (#10b981), Background `bg-gray-950` (Dark Mode).
2. **Typography**: Inter (UI) dan Outfit (Headers).
3. **Aesthetics**: Glassmorphism, smooth gradients, subtle micro-animations.
4. **A11y**: Pastikan komponen support screen readers dan keyboard navigation (lihat `frontend/web-app/.a11yrc.json`, `frontend/web-app/scripts/a11y-audit.ts`, `frontend/web-app/e2e/a11y-audit.spec.ts`).

---

## 🛡️ Security & Compliance

AI Assistant harus mematuhi aturan keamanan berikut:

1. **PII Protection**: Data sensitif (NIK, PIN, Phone) harus di-mask di logs dan di-encrypt di DB (pake `@Sensitive` & `security-starter`).
2. **No Credentials**: Jangan pernah menuliskan password/key di `application.yml`. Gunakan placeholder atau Vault reference.
3. **Idempotency**: Semua API kritis (transfer, payment) harus mendukung idempotency key.

---

## 🚫 Excluded Scope (DevOps/SRE)

AI Assistant **TIDAK BOLEH** mengimplementasikan kode atau konfigurasi untuk area berikut:

### Infrastructure & Platform

- Kubernetes manifests, Helm charts, OpenShift configurations
- Terraform, Ansible, atau IaC (Infrastructure as Code)

**Default policy**: Jangan mengubah `infrastructure/` atau manifest cluster produksi. Jika user meminta pembelajaran, batasi contoh ke `docs/` atau folder sandbox.

### Ketika diminta untuk area di atas:

1. Tolak dengan sopan dan jelaskan bahwa ini adalah tanggung jawab tim DevOps/SRE
2. Arahkan user ke folder `infrastructure/` untuk referensi
3. Sarankan untuk berkonsultasi dengan tim operations

---

## 📚 Learning Allowed (DevOps & Security)

AI Assistant **BOLEH** membantu area berikut untuk tujuan pembelajaran **hanya** di `docs/` atau sandbox. Jangan ubah `infrastructure/` kecuali diminta eksplisit.

### CI/CD & Pipelines

| Area                 | Contoh                               | AI Dapat Membantu      |
| -------------------- | ------------------------------------ | ---------------------- |
| **Tekton Pipelines** | Pipeline, Task, TriggerTemplate      | ✅ Explain & implement |
| **ArgoCD**           | Application manifests, sync policies | ✅ Explain & implement |

### Containerization & Registry

| Area                  | Contoh                                     | AI Dapat Membantu      |
| --------------------- | ------------------------------------------ | ---------------------- |
| **Containerization**  | Dockerfile, image builds, optimizations    | ✅ Explain & implement |
| **Artifact Registry** | Container registry, image tagging, pushing | ✅ Explain & implement |

### Observability & Monitoring

| Area                    | Contoh                           | AI Dapat Membantu      |
| ----------------------- | -------------------------------- | ---------------------- |
| **Log Aggregation**     | LokiStack, Promtail, log queries | ✅ Explain & implement |
| **Observability Setup** | Prometheus, Grafana, dashboards  | ✅ Explain & implement |
| **Distributed Tracing** | Jaeger, OpenTelemetry            | ✅ Explain & implement |

### Service Mesh & Networking

| Area                   | Contoh                                | AI Dapat Membantu      |
| ---------------------- | ------------------------------------- | ---------------------- |
| **Istio**              | VirtualService, DestinationRule, mTLS | ✅ Explain & implement |
| **Traffic Management** | Canary, Blue-Green, Circuit Breaking  | ✅ Explain & implement |
| **Load Balancer**      | Ingress configuration, routing        | ✅ Explain & implement |

### Security Infrastructure

| Area                       | Contoh                                  | AI Dapat Membantu      |
| -------------------------- | --------------------------------------- | ---------------------- |
| **Secret Management**      | HashiCorp Vault setup, secret injection | ✅ Explain & implement |
| **Certificate Management** | TLS certificates, mTLS, rotation        | ✅ Explain & implement |
| **WAF Configuration**      | ModSecurity rules, OWASP CRS            | ✅ Explain & implement |
| **SSO/Keycloak**           | Realm config, client setup, OIDC        | ✅ Explain & implement |
| **Network Policies**       | Pod-to-pod security, ingress rules      | ✅ Explain & implement |

> **Note**: Ini adalah lab project, jadi AI dapat membantu implementasi untuk pembelajaran **tanpa** menyentuh konfigurasi produksi.

---

## 🤖 Available AI Skills (17 Skills - v3.0.0)

Skills are categorized by domain to help you choose the right tool for the task. All skills are located in `.agent/skills/` and indexed in `REGISTRY.yaml`.
Skill-to-agent mapping ada di `.agent/agents/AGENTS-MAP.md`.

> **Consolidation Notes (January 2026)**:
>
> - `information-architect` merged into `principal-architect`
> - `release-engineer` + `sre` merged into `platform-engineer`
> - `bff-architect` removed (PayU is pure Java backend)

### 🏗️ Core & Architecture

| Skill                 | Description                                                                                 |
| :-------------------- | :------------------------------------------------------------------------------------------ |
| `principal-architect` | **Master Skill**: High-level Architecture, DORA metrics, Strategy, C4, ADRs, Documentation. |

### ☕ Backend & Logic

| Skill                       | Description                                                                                  |
| :-------------------------- | :------------------------------------------------------------------------------------------- |
| `core-banking-engineer`     | **Master Skill**: Spring Boot 3.4, Hexagonal Architecture, & Resilience.                     |
| `api-architect`             | **Master Skill**: REST API standards, OpenAPI, Versioning, & 3rd-party Integrations.         |
| `integration-architect`     | **Master Skill**: Sagas, Event Sourcing, Kafka, Message Queues (CDC).                        |
| `data-architect`            | **Master Skill**: PostgreSQL Design, Performance, Query Optimization, & Flyway.              |
| `data-governance-architect` | **Master Skill**: Data Lineage, PII Classification, Retention Policies, & UU PDP Compliance. |
| `ai-engineer`               | **Master Skill**: Intelligent Systems, FastAPI, Prompt Engineering, & GenAI.                 |

### 📱 Frontend & Mobile

| Skill                   | Description                                                                    |
| :---------------------- | :----------------------------------------------------------------------------- |
| `product-designer`      | **Master Skill**: Premium Aesthetics, Atomic Design, & A11y.                   |
| `frontend-architect`    | **Master Skill**: Next.js 15+, React, Performance, Component Refactoring.      |
| `mobile-architect`      | **Master Skill**: React Native, Expo, Native UI (SF Symbols), Mobile Security. |
| `web-artifacts-builder` | **Master Skill**: Scaffolding single-file HTML/React artifacts for docs/demos. |

### 🛡️ Security & Compliance

| Skill                     | Description                                                                           |
| :------------------------ | :------------------------------------------------------------------------------------ |
| `cybersecurity-architect` | **Master Skill**: Zero Trust, Vault, mTLS, Auth Patterns, & Compliance (PCI-DSS/OJK). |

### ⚙️ DevOps, Reliability & Quality (CONSOLIDATED)

| Skill                   | Description                                                                                                                |
| :---------------------- | :------------------------------------------------------------------------------------------------------------------------- |
| `platform-engineer`     | **UNIFIED (v3.0)**: DevOps + SRE + Release Engineering. Tekton/ArgoCD, OpenShift, Feature Flags, Observability, Chaos, DR. |
| `quality-engineer`      | **UNIFIED**: Testing + Performance + Contracts. TDD, Gatling/k6, Pact, Browser Testing.                                    |
| `debugging-methodology` | **NEW**: Systematic debugging, Root Cause Analysis, Pattern Recognition.                                                   |
| `finops-engineer`       | **UNIFIED**: Financial Ops + Cloud FinOps. Recon, GL, Cost Management, Tagging Strategy.                                   |
| `dx-engineer`           | **UNIFIED**: Git Workflows + Developer Onboarding + Slidev + Release Notes.                                                |

> **Documentation**: For detailed usage flow, see [AGENT_SKILLS_GUIDE.md](./AGENT_SKILLS_GUIDE.md).

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

> [!IMPORTANT]
> **The Iron Law**: If you haven't completed Phase 1 (Root Cause Investigation), you are NOT allowed to propose or implement fixes.
> For detailed patterns and case studies, use the **`debugging-methodology` skill** ([SKILL.md](../../.agent/skills/debugging-methodology/SKILL.md)).

When encountering ANY technical issue (test failure, bug, performance issue):

**Phase 1: Root Cause Investigation**

1.  **Read Error Messages**: Don't skip stack traces. Note error codes.
2.  **Reproduce Consistently**: If you can't reproduce it, you can't fix it.
3.  **Trace Data Flow**: Add logs/prints at component boundaries to see WHERE it breaks.
4.  **Gather Evidence**: Verify environment, config, and state before changing code.

**Phase 2: Pattern Analysis**

1.  **Find Working Examples**: Compare against similar working code in the project.
2.  **Identify Differences**: What is different? (Dependencies, versions, config).

**Phase 3: Hypothesis & Testing**

1.  **Form Single Hypothesis**: "I think X is causing Y because Z".
2.  **Test Minimally**: Change ONE thing.
3.  **Verify**: Did it fix it? If no, revert and try a new hypothesis.

**Phase 4: Implementation**

1.  **Create Failing Test**: Prove the bug exists with a test case (TDD).
2.  **Implement Fix**: Address the root cause.
3.  **Verify Fix**: Ensure test passes and no regressions.

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

Claude Code SHOULD follow established workflows in `.agent/workflows/` for complex operations. **URGENT**: Karena Claude Code tidak auto-discover folder `workflows`, AI asisten wajib melakukan `ls .agent/workflows/` atau membaca file di dalamnya secara proaktif sebelum memulai tugas arsitektural.

- **`/antigravity-lifecycle`**: Standard SDLC lifecycle (Observe -> Plan -> Execute -> Verify).
- **`/multi-agent-coordination`**: Parallel task coordination and synthesis for multi-service changes. **(Principally handled by Main AI)**
- **`/new-service-scaffolding`**: Workflow untuk scaffolding microservice baru di platform PayU dengan arsitektur Hexagonal dan konfigurasi standar.
- **`/orchestration-protocol`**: Advanced AI Orchestration, Task Management, and Core Engineering Principles for PayU.
- **`/security-audit`**: Workflow untuk melakukan audit keamanan pada service PayU sesuai standar PCI-DSS dan OJK.

## 🛰️ Advanced Orchestration Protocol (v2.0)

This section defines the high-performance operational protocol for all AI Agents on the PayU platform.

### 1. Workflow Orchestration Standards

- **Plan Mode Default**: Enter plan mode for ANY non-trivial task (3+ steps or architectural decisions).
- **Graceful Halt**: If something goes sideways, STOP and re-plan immediately – don't keep pushing.
- **Verification-First Planning**: Use plan mode for verification steps, not just building.
- **Detailed Specs**: Write detailed specs upfront to reduce ambiguity.

### 2. Subagent Strategy

- **Liberal Subagent Usage**: Use subagents liberally to keep main context window clean.
- **Offload & Parallelize**: Offload research, exploration, and parallel analysis to subagents.
- **Compute Scaling**: For complex problems, throw more compute at it via subagents.
- **Focused Execution**: One task per subagent for focused execution.

### 3. Self-Improvement Loop

- **Pattern Capturing**: After ANY correction from the user: update `docs/guides/LESSONS.md` with the pattern.
- **Recursive Rules**: Write rules for yourself that prevent the same mistake.
- **Ruthless Iteration**: Iteratively refine lessons until the mistake rate drops.
- **Pre-Session Review**: Review lessons at session start for relevant project context.

### 4. Verification Protocol

- **Proof of Work**: Never mark a task complete without proving it works.
- **Behavioral Diffing**: Diff behavior between main and your changes when relevant.
- **Staff Engineer Standard**: Ask yourself: "Would a staff engineer approve this?"
- **E2E Validation**: Run tests, check logs, and demonstrate correctness explicitly.

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

### 📋 Task Management Protocol

- **Plan First**: Write plan to `docs/roadmap/TODOS.md` with checkable items.
- **Verify Plan**: Check in before starting implementation.
- **Track Progress**: Mark items complete as you go.
- **Explain Changes**: High-level summary at each step.
- **Document Results**: Add bug findings ke `docs/roadmap/TODOS.md`, architectural decisions ke `docs/roadmap/GATEWAY_ARCH.md`.
- **Capture Lessons**: Update `docs/guides/LESSONS.md` after corrections.
- **Changelog**: Update `CHANGELOG.md` `[Unreleased]` section for any significant change.

**Doc Routing Rules**:
| Konten | File Tujuan |
| :--- | :--- |
| Bug baru, open items, actionable todos | `docs/roadmap/TODOS.md` |
| Deployment status, completed milestones | `docs/roadmap/PROGRESS.md` |
| Architecture decisions, gap analysis | `docs/roadmap/GATEWAY_ARCH.md` |
| Version changelog | `CHANGELOG.md` |
| Implementation patterns | `docs/guides/LESSONS.md` |

**Fast Path (Small Changes)**:

- Boleh skip update roadmap docs untuk perubahan kecil (<=2 file, 1 service, tanpa keputusan arsitektural).
- Tetap berikan rencana singkat + langkah verifikasi di respons.

### ⚖️ Core Engineering Principles

- **Simplicity First**: Make every change as simple as possible. Impact minimal code.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.
- **Minimal Impact**: Changes should only touch what's necessary. Avoid introducing bugs.

## 🤖 Specialized AI Agents

Untuk eksekusi tugas yang terisolasi dan spesifik, agen berikut tersedia di `.agent/agents/` (diakses via `.claude/agents/`):

- `@scaffolder`, `@logic-builder`, `@tester`, `@auditor`, `@migrator`, `@builder`, `@styler`, `@orchestrator`, `@lifecycle-manager`, `@scaffolding-expert`, `@compliance-auditor`.

_Usage_: When tasked with complex refactoring or multi-service updates, read the relevant workflow file first.

---

_Last Updated: 2026-02-24 | Platform: Payment Gateway for TokoBapak & Nobar | Active Bug Count: ~117 (see TODOS.md)_
