# PayU Agent Skills Guide: Multi-Engineer AI Ecosystem

Selamat datang di ekosistem pengembangan **PayU Digital Banking Platform**. Dokumentasi ini menjelaskan cara kerja, alur penggunaan, dan pembagian peran dari AI Skills dan Specialized Agents.

---

## 🏛️ Arsitektur AI Ecosystem

Ekosistem AI PayU dibagi menjadi tiga komponen utama:

1. **Skills (The Brains)**: Pengetahuan mendalam dan best practices domain.
2. **Agents (The Workers)**: Unit eksekusi spesialis untuk automasi teknis.
3. **Commands (The Shortcuts)**: Slash commands untuk eksekusi cepat.

### 🧩 Peta Komponen (Source of Truth)

Semua konfigurasi AI dipusatkan di direktori `.agent/` dan diakses oleh Claude Code melalui symbolic links di `.claude/`.

```
payu/
├── .agent/               # Master Configuration (Source of Truth)
│   ├── skills/           # 20+ High-level AI Skills
│   ├── agents/           # 12 Specialized Execution Agents
│   ├── workflows/        # SDLC & Coordination Workflows
│   ├── commands/         # Custom Slash Commands
│   └── settings.json     # Global AI Access
└── .claude/              # Entry Point (Symlinks to .agent/)
    ├── skills -> ../.agent/skills
    ├── agents -> ../.agent/agents
    ├── workflows -> ../.agent/workflows
    ├── commands -> ../.agent/commands
    └── settings.json -> ../.agent/settings.json
```

---

### 🧩 Ecosystem Mapping
The PayU platform is powered by specialized skill clusters:

#### 🏗️ Core & Architecture
| Spec | Skill | Description |
| :--- | :--- | :--- |
| **Dev/Arch** | `principal-architect` | **Master Skill**: High-level Architecture, DORA & Strategy. |
| **Knowledge**| `information-architect` | **Master Skill**: Documentation, C4 Architecture, ADRs, and Task Planning. |

#### ☕ Backend & Logic
| Spec | Skill | Description |
| :--- | :--- | :--- |
| **Core** | `core-banking-engineer` | **Master Skill**: Spring Boot 3.4, Hexagonal Architecture, & Resilience. |
| **API** | `api-architect` | **Master Skill**: REST, OpenAPI, & 3rd-party Integrations. |
| **DB** | `data-architect` | **Master Skill**: PostgreSQL Design, Performance, & Flyway. |
| **Events** | `event-systems-architect` | **Master Skill**: Sagas, Event Sourcing, & Kafka (CDC). |
| **Logic** | `nodejs-bff-architect` | **Master Skill**: Node.js BFFs, Prisma, and Zod (Type-Safe APIs). |
| **AI** | `ai-engineer` | **Master Skill**: Intelligent Systems, FastAPI, & TimescaleDB. |

#### 📱 Frontend & Mobile
| Spec | Skill | Description |
| :--- | :--- | :--- |
| **Web** | `frontend-architect` | **Master Skill**: Next.js 15+, React, & Web Perf. |
| **Mobile** | `mobile-architect` | **Master Skill**: React Native, Expo, & Mobile Security. |
| **Design** | `product-designer` | **Master Skill**: Premium Aesthetics, Atomic Design, & A11y. |

#### 🛡️ Security & Observability
| Spec | Skill | Description |
| :--- | :--- | :--- |
| **Audit** | `cybersecurity-architect` | **Master Skill**: Zero Trust, Auth Patterns, & Compliance. |
| **Metrics** | `sre-engineer` | **Master Skill**: SRE, Golden Signals, Logs, & Tracing. |
| **QA** | `sdet-solutions-engineer` | **Master Skill**: TDD, E2E, RCA, & Financial Recon. |
| **FinOps** | `finops-architect` | **New**: Recon, Settlement, GL Integration, and Regulatory Reporting. |

#### ⚙️ DevOps & Automation
| Spec | Skill | Description |
| :--- | :--- | :--- |
| **Infra** | `platform-engineer` | **Master Skill**: Tekton/ArgoCD, OpenShift, & Container Hardening. |
| **Logic** | `typescript-specialist` | **Master Skill**: TypeScript & Modern JS Functional patterns. |
| **Git** | `developer-experience-specialist` | **Master Skill**: Git Strategy, Conventional Commits, and PR Mastery. |
| **Tools** | `slidev` | Presentation slides for developers (Markdown-based). |

---

## 🤖 Specialized AI Agents (The Workers)

Agen dirancang untuk eksekusi tugas yang terisolasi.

| Agent                     | Deskripsi Peran                    | Target Output                               |
| :------------------------ | :--------------------------------- | :------------------------------------------ |
| **`@scaffolder`**         | Spesialis boilerplate & struktur.  | Hexagonal structure, pom.xml, Dockerfile.   |
| **`@logic-builder`**      | Spesialis DDD & Business Logic.    | Rich Domain Models, Ports, Use Cases.       |
| **`@tester`**             | Gatekeeper kualitas & testing.     | JUnit 5, Mockito, Maestro (Mobile E2E).     |
| **`@auditor`**            | Penjaga kepatuhan & kualitas.      | Security Audit Reports, Complexity scans.   |
| **`@migrator`**           | Administrasi Database & Flyway.    | Optimized SQL migrations, JSONB indexing.  |
| **`@builder`**            | Build & Packaging Specialist.      | Native Images (Quarkus), Web Artifacts.     |
| **`@styler`**             | Spesialis UI/UX Emerald Design.    | Premium CSS, Glassmorphism, Animations.     |
| **`@orchestrator`**       | Automasi Git & CI/CD Pipelines.    | Conventional Commits, Tekton/ArgoCD config. |
| **`@lifecycle-manager`**  | Pengelola siklus SDLC Antigravity. | Step-by-step feature implementation plan.   |
| **`@scaffolding-expert`** | Setup service E2E terintegrasi.    | Fully functional microservice scaffold.     |
| **`@compliance-auditor`** | Audit standar OJK/PCI-DSS.         | Compliance Checklists, Risk Matrix (ALE).   |

---

## 🔄 Alur Kerja SDLC Terintegrasi

Gunakan alur berikut untuk mengorkestrasi ekosistem AI secara efektif:

### Fase 1: Discovery & Planning

1. Gunakan skill **`@principal-architect`** untuk memahami konteks arsitektur global.
2. Gunakan `@lifecycle-manager` untuk merancang dokumen rencana implementasi.

### Fase 2: Scaffolding & Setup

1. Jalankan `@scaffolder` atau `/project:scaffold` untuk membuat service.
2. Gunakan `@migrator` untuk membuat skema database awal.

### Fase 3: Core Implementation

1. Gunakan `@logic-builder` untuk menulis logika domain fungsional.
2. Gunakan `@tester` secara paralel untuk menulis unit tests (TDD).

### Fase 4: Optimization & Verification

1. Gunakan `@styler` untuk memoles tampilan frontend.
2. Gunakan `@auditor` untuk security dan performance check.
3. Gunakan `@tester` untuk memvalidasi coverage dan integrasi.

### Fase 5: Delivery

1. Gunakan `@builder` untuk memastikan build dan container sukses.
2. Gunakan `@orchestrator` untuk merapikan branch dan push ke remote.

## ⚡ Hyper-Parallelism (Native Claude Code Capability)

Platform PayU memanfaatkan kapabilitas native dari **Claude Code** untuk menjalankan hingga **12 Agen Paralel** secara bersamaan. Kapabilitas ini memungkinkan:
- **Massive Refactoring**: Merombak multiple microservices sekaligus menggunakan subagents spesialis.
- **Full-Scale Audit**: Menjalankan security audit, test suite, dan performance validation secara serentak melalui delegasi pararel.
- **Cross-Platform Sync**: Sinkronisasi perubahan di Backend, Web, dan Mobile dalam satu siklus dispatch.

> **Cara Menggunakan**: Gunakan workflow `/multi-agent-coordination` dalam mode **Swarm** untuk mendistribusikan beban tugas berat ke agen-agen Anda menggunakan fitur subagent orkestrasi dari Claude Code.

---

## 📜 Golden Rules

1. **Source of Truth**: Selalu edit konfigurasi di folder `.agent/`.
2. **Agent Focus**: Gunakan agen spesifik untuk tugas yang sesuai (lihat [AGENTS-MAP.md](../../.agent/agents/AGENTS-MAP.md)).
3. **Audit Before Release**: Jangan pernah merge kode tanpa laporan sukses dari `@auditor` dan `@tester`.

---

_Last Updated: January 2026_
