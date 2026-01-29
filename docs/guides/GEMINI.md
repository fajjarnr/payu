# AI_GUIDELINES.md - PayU Digital Banking Platform

> AI Assistant Guidelines & Project Context for Gemini, Claude, and other Agents

---

## 📋 Project Overview

**PayU** adalah platform digital banking standalone yang dibangun dengan arsitektur microservices di atas **Red Hat OpenShift 4.20+** ecosystem. Platform ini dirancang sebagai payment infrastructure berskala enterprise yang dapat digunakan oleh multiple projects.

### Quick Facts

| Attribute             | Value                                 |
| --------------------- | ------------------------------------- |
| **Project Name**      | PayU                                  |
| **Type**              | Standalone Digital Banking Platform   |
| **Architecture**      | Scalable Microservices + Event-Driven |
| **Platform**          | Red Hat OpenShift 4.20+               |
| **Primary Languages** | Java 21, Python 3.12, TypeScript      |
| **Last Updated**      | January 2026                          |

## ⚡ Quick Commands (for AI Agents)

| Action             | Command                                     |
| ------------------ | ------------------------------------------- |
| **Build Backend**  | `mvn clean package -DskipTests -T 1C`       |
| **Run Web App**    | `cd frontend/web-app && npm run dev`        |
| **Run Dev Docs**   | `cd frontend/developer-docs && npm run dev` |
| **Infrastructure** | `docker-compose up -d`                      |
| **Check Services** | `oc get pods` or `docker ps`                |

---

## 🏗️ Architecture Overview

### Technology Stack

| Layer                   | Red Hat Product                    | Portable Alternative |
| ----------------------- | ---------------------------------- | -------------------- |
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
| **Service Mesh**        | OpenShift Service Mesh             | Istio                |

### Microservices

| Service                | Technology         | Domain                                     |
| ---------------------- | ------------------ | ------------------------------------------ |
| `account-service`      | Spring Boot 3.4    | User accounts, profile, multi-pocket       |
| `auth-service`         | Spring Boot 3.4    | Authentication, Risk-based MFA, Biometrics |
| `transaction-service`  | Spring Boot 3.4    | Transfers, BI-FAST, QRIS, Sharding         |
| `wallet-service`       | Spring Boot 3.4    | Double-entry ledger, balance management    |
| `investment-service`   | Spring Boot 3.4    | Mutual funds, Gold, Robo-advisory          |
| `lending-service`      | Spring Boot 3.4    | Loans, PayLater, Credit Scoring            |
| `fx-service`           | Spring Boot 3.4    | Currency exchange rates & conversion       |
| `statement-service`    | Spring Boot 3.4    | PDF E-Statement generation                 |
| `backoffice-service`   | Spring Boot 3.4    | Internal admin dashboard, audit            |
| `partner-service`      | Spring Boot 3.4    | Partner integration & management           |
| `promotion-service`    | Spring Boot 3.4    | Promo campaigns, vouchers, rewards         |
| `support-service`      | Spring Boot 3.4    | Customer support, ticketing                |
| `compliance-service`   | Spring Boot 3.4    | Regulatory compliance, AML                 |
| `billing-service`      | Quarkus 3.x Native | Bill payments (PLN, PDAM, etc)             |
| `notification-service` | Quarkus 3.x Native | Push, SMS, Email, WhatsApp                 |
| `gateway-service`      | Quarkus 3.x Native | API Gateway, Rate limiting                 |
| `cms-service`          | Spring Boot 3.4    | Banners, Promos, Dynamic Content           |
| `ab-testing-service`   | Spring Boot 3.4    | UI/Feature experimentation                 |
| `api-portal-service`   | Quarkus 3.x Native | Centralized OpenAPI Docs & Sandbox         |
| `kyc-service`          | Python FastAPI     | OCR, Liveness Detection                    |
| `analytics-service`    | Python FastAPI     | Fraud Scoring, User Insights               |

### Shared Libraries (backend/shared/)

| Library              | Purpose                                         |
| -------------------- | ----------------------------------------------- |
| `security-starter`   | Field encryption, Data masking, Audit logging   |
| `resilience-starter` | Circuit Breaker, Retry, Bulkhead (Resilience4j) |
| `cache-starter`      | Multi-layer caching (Redis + Caffeine)          |

---

## ⚡ Decentralized Orchestration (Swarm Mode)

Platform PayU didesain untuk dikembangkan menggunakan pola **Decentralized Parallel Execution**. AI Assistant tidak bekerja sebagai monolit, melainkan sebagai orkestrator yang mendispatch tugas ke agen spesialis secara simultan.

### Swarm Principles:
1. **Parallel Dispatching**: Tugas Full-stack didelegasikan ke `@styler` (Frontend) dan `@logic-builder` (Backend) secara bersamaan untuk reduksi waktu eksekusi hingga 80%.
2. **Specialized Handshake**: Setiap Skill (misal: `@backend-engineer`) memiliki instruksi eksplisit untuk memanggil spesialis lain (misal: `@tester` atau `@migrator`) jika tugas menyentuh domain mereka.
3. **Implicit Interconnectivity**: Asisten wajib secara proaktif mendispatch agen pendukung (seperti `@auditor` untuk security atau `@styler` untuk slidev) berdasarkan jenis perubahan kode tanpa menunggu perintah manual.

---

## 📁 Project Structure

```
payu/
├── .agent/               # AI Agent Ecosystem (Symlinked to .claude/)
│   ├── skills/           # 20+ AI Skills (Logic, Stack, Standards)
│   ├── agents/           # Specialized Sub-agents (System-level prompts)
│   ├── workflows/        # SOP for complex tasks (MUST READ BEFORE EXECUTION)
│   └── resources/        # Shared assets (shadcn components, templates)
├── backend/             # Microservices implementation (20+ services)
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
│   ├── guides/          # AI Skills Guide
│   └── security/        # Security policies
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

### Testing Guidelines (TDD)

1. **Unit Tests**: 100% logic coverage menggunakan JUnit 5 & Mockito.
2. **ArchUnit**: Pastikan setiap service baru memiliki `ArchitectureTest` untuk menjaga layering.
3. **Testcontainers**: Gunakan untuk integration tests yang membutuhkan PostgreSQL atau Kafka (jika enviroment memungkinkan).

---

## 🎨 Frontend Design System (Premium Emerald)

Untuk menjaga konsistensi UI yang premium:

1. **Color Palette**: Primary `bank-green` (#10b981), Background `bg-gray-950` (Dark Mode).
2. **Typography**: Inter (UI) dan Outfit (Headers).
3. **Aesthetics**: Glassmorphism, smooth gradients, subtle micro-animations.
4. **A11y**: Pastikan komponen support screen readers dan keyboard navigation (pake `@src/lib/a11y.tsx`).

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

### Ketika diminta untuk area di atas:

1. Tolak dengan sopan dan jelaskan bahwa ini adalah tanggung jawab tim DevOps/SRE
2. Arahkan user ke folder `infrastructure/` untuk referensi
3. Sarankan untuk berkonsultasi dengan tim operations

---

## 📚 Learning Allowed (DevOps & Security)

AI Assistant **BOLEH** membantu area berikut untuk tujuan pembelajaran:

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

> **Note**: Ini adalah lab project, jadi AI dapat membantu implementasi untuk pembelajaran.

---

## 🤖 Available AI Skills

Skills yang tersedia di `.agent/skills/`:

| Skill                       | Description                                         |
| --------------------------- | --------------------------------------------------- |
| `api-design`                | REST API standards, OpenAPI, versioning             |
| `c4-architecture`           | Architecture visualization using C4 Model           |
| `backend-engineer`          | Java/Python microservice development                |
| `code-review`               | Code quality & review checklists                    |
| `container-engineer`        | UBI Images & OpenShift containers                   |
| `cto-advisor`               | Strategic technical leadership & metrics            |
| `database-engineer`         | PostgreSQL, JSONB, migrations                       |
| `debugging-engineer`        | Systematic root cause analysis & debugging          |
| `error-handling-engineer`   | Error patterns, Circuit Breakers, fallbacks         |
| `devops-engineer`           | CI/CD Pipelines, Tekton, ArgoCD, Automation Scripts |
| `docs-engineer`             | Documentation & Tech Writing                        |
| `event-driven-architecture` | Kafka, Saga, Event Sourcing                         |
| `frontend-engineer`         | Next.js, React, Design Systems                      |
| `frontend-patterns`         | React/Next.js component & performance patterns      |
| `git-workflow`              | Git standards & PR workflows                        |
| `ml-engineer`               | Fraud Scoring, Python & Analytics                   |
| `mobile-engineer`           | React Native, Expo, Mobile Security                 |
| `modern-javascript-patterns`| Modern ES6+ syntax, functional patterns, async programming, and performance optimization strategies. |
| `nextjs-app-router-patterns`| Master Next.js App Router patterns, Server Components, Data Fetching, and Caching strategies |
| `observability-engineer`    | Distributed Tracing, Logs, and Metrics              |
| `payu-development`          | Pathfinding & Core Architecture                     |
| `qa-engineer`               | Testing strategies & QA automation                  |
| `react-native-architecture` | Build production React Native apps with Expo, navigation, native modules, offline sync, and cross-platform patterns. Use when developing mobile apps, implementing native integrations, or architecting React Native projects. |
| `react-native-design`       | Create polished React Native UIs with Reanimated, Expo Router, and NativeWind/StyleSheet. |
| `react-modernization`       | Upgrade older React apps to modern standards (Functions, Hooks, Concurrent Features). |
| `react-state-management`    | Master modern React state management with Redux Toolkit, Zustand, Jotai, and React Query |
| `responsive-design`        | Breakpoints strategies, fluid layouts, and container queries |
| `security-engineer`         | PCI-DSS, Encryption, OJK Compliance                 |
| `tailwind-design-system`   | Tailwind CSS design tokens & component variants     |
| `web-artifacts-builder`    | Scaffolding & Bundling single-file HTML Artifacts   |
| `web-component-design`    | Master React, Vue, and Svelte component patterns including CSS-in-JS, composition strategies, and reusable component architecture. |
| `slidev`                   | Presentation slides for developers (Markdown-based) |

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
- **`/security-audit`**: Workflow untuk melakukan audit keamanan pada service PayU sesuai standar PCI-DSS dan OJK.

## ⚡ Global Orchestration Guidelines (MUST FOLLOW)

1. **Parallelism by Default**: Jika tugas besar dapat dipecah, gunakan `@multi-agent-coordination` untuk eksekusi paralel.
2. **Context Sharing**: Gunakan `docs/context/active_task.md` (jika ada) sebagai memori bersama antar agen yang berjalan paralel.
3. **Proactive Delegation**: Jangan kerjakan tugas lintas-domain sendirian. Selalu fork agen spesialis (misal: `@migrator` untuk DB task) untuk menjamin kualitas standar PayU.

## 🤖 Specialized AI Agents

Untuk eksekusi tugas yang terisolasi dan spesifik, agen berikut tersedia di `.agent/agents/` (diakses via `.claude/agents/`):

- `@scaffolder`, `@logic-builder`, `@tester`, `@auditor`, `@migrator`, `@builder`, `@styler`, `@orchestrator`, `@lifecycle-manager`, `@scaffolding-expert`, `@compliance-auditor`.

_Usage_: When tasked with complex refactoring or multi-service updates, read the relevant workflow file first.

---

_Last Updated: January 2026_
