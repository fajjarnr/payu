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

## 🤖 Specialized AI Agents (The Workers)

Agen dirancang untuk eksekusi tugas yang terisolasi.

| Agent                     | Deskripsi Peran                    | Target Output                               |
| :------------------------ | :--------------------------------- | :------------------------------------------ |
| **`@scaffolder`**         | Spesialis boilerplate & struktur.  | Folder structure, pom.xml, Dockerfile.      |
| **`@logic-builder`**      | Spesialis DDD & Business Logic.    | Rich Domain Entities, Application Services. |
| **`@tester`**             | Gatekeeper kualitas & testing.     | JUnit 5, Integration Tests, Coverage.       |
| **`@auditor`**            | Penjaga kepatuhan & kualitas.      | Security scans, Code review findings.       |
| **`@migrator`**           | Administrasi Database.             | Flyway scripts, SQL optimization.           |
| **`@builder`**            | Build & Packaging.                 | Compiled artifacts, Container images.       |
| **`@styler`**             | Spesialis UI/UX "Premium Emerald". | CSS, A11y compliance.                       |
| **`@orchestrator`**       | Automasi Git & CI/CD.              | PR logic, Branch management.                |
| **`@lifecycle-manager`**  | Pengelola siklus SDLC penuh.       | End-to-end task execution.                  |
| **`@scaffolding-expert`** | Setup service E2E terintegrasi.    | Full service initialization.                |
| **`@compliance-auditor`** | Audit standar OJK/PCI-DSS.         | Security Attestation documents.             |

---

## 🔄 Alur Kerja SDLC Terintegrasi

Gunakan alur berikut untuk mengorkestrasi ekosistem AI secara efektif:

### Fase 1: Discovery & Planning

1. Gunakan skill **`/backend-engineer`** atau **`/payu-development`** untuk memahami konteks.
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
