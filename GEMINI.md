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
├── .agent/               # AI Agent Ecosystem (Symlinked to .claude/ & .opencode/)
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

1. **PII Protection**: Data sensitif (NIK, PIN, Phone) harus di-mask di logs dan di-encrypt di DB (pake `@Sensitive` & `security-starter`).
2. **No Credentials**: Jangan pernah menuliskan password/key di `application.yml`. Gunakan placeholder atau Vault reference.
3. **Idempotency**: Semua API kritis (transfer, payment) harus mendukung idempotency key.

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

> [!IMPORTANT]
> **The Iron Law**: If you haven't completed Phase 1 (Root Cause Investigation), you are NOT allowed to propose or implement fixes.
> For detailed patterns and case studies, use the **`debugging-methodology` skill** ([SKILL.md](../../.agent/skills/debugging-methodology/SKILL.md)).

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

AI Assistants SHOULD follow established workflows in `.agent/workflows/` for complex operations. **URGENT**: Karena AI tidak selalu auto-discover folder `workflows`, asisten wajib melakukan `ls .agent/workflows/` atau membaca file di dalamnya secara proaktif sebelum memulai tugas arsitektural.

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

- **Mandatory Invocation**: If there is even a 1% chance a skill in `.agent/skills/` applies to your task, you **ABSOLUTELY MUST** read and use it before any response or action. This is not optional.
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
