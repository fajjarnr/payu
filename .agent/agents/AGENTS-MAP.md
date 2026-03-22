# 🗺️ PayU AI Agents Mapping (Skills-to-Agents)

Dokumen ini memetakan bagaimana **Skills** (High-level capabilities) mengorkestrasi **Agents** (Dedicated execution units) untuk mencapai efisiensi maksimal dalam siklus pengembangan PayU.

> **Note**: Setelah konsolidasi Januari 2026, PayU memiliki **17 Skills** yang terintegrasi.
> **🚨 POST-AUDIT STATUS (Mar 2026)**: Phase 1–12 Complete (Readiness 100%).
> **CURRENT FOCUS**: Post-Audit Deep Remediation (56 Open Bugs per Mar 2026).
> **Technical Inventory**: `docs/roadmap/SERVICES.md` | Actionable items: `docs/roadmap/TODOS.md`
> Fix instructions: `docs/guides/LESSONS.md` | Remediation workflow: `.agent/workflows/p19-remediation.md`

## 🏗️ Core Mapping Strategy

| Triggering Skill     | Orchestrated Agent    | Rationale                                      |
| :------------------- | :-------------------- | :--------------------------------------------- |
| `@principal-architect` | `@scaffolder`         | Mengotomatisasi pembuatan service baru.        |
| `@principal-architect` | `@lifecycle-manager`  | Pengelola SDLC penuh (End-to-End).             |
| `@principal-architect` | `@scaffolding-expert` | Setup service end-to-end terintegrasi.         |
| `@core-banking-engineer` | `@logic-builder`    | Implementasi Domain logic dan DDD entities.    |
| `@api-architect`     | `@logic-builder`      | Standarisasi API Schemas, DTOs, & Contract-first logic. |
| `@integration-architect` | `@logic-builder` | Implementasi Kafka messaging & Saga patterns.  |
| `@data-architect`    | `@migrator`           | Pengelolaan skema database dan migrasi Flyway. |
| `@data-governance-architect` | `@compliance-auditor` | Data lineage, PII audit, retention policies. |
| `@frontend-architect`| `@styler`             | Estetika "Premium Emerald" dan A11y.           |
| `@mobile-architect`  | `@styler`             | Styling React Native (NativeWind/Reanimated).  |
| `@mobile-architect`  | `@logic-builder`      | Logic React Native, Offline sync, State mgmt.  |
| `@product-designer`  | `@styler`             | Design Tokens, Palettes, Typography systems.   |
| `@web-artifacts-builder` | `@builder`           | Scaffolding dan bundling single-file artifacts. |
| `@cybersecurity-architect` | `@auditor`          | Audit keamanan dan kepatuhan (PCI-DSS/OJK).    |
| `@cybersecurity-architect` | `@compliance-auditor` | Audit kepatuhan standar OJK/PCI-DSS mendalam.  |
| `@platform-engineer` | `@builder`            | Build, packaging, dan containerization.        |
| `@platform-engineer` | `@orchestrator`       | Alur CI/CD dan sinkronisasi git.               |
| `@platform-engineer` | `@tester`             | Chaos experiments, Game Days, fault injection. |
| `@ai-engineer`       | `@logic-builder`      | Implementasi Async Service, Repository, & ETL. |
| `@quality-engineer`  | `@tester`             | Full-stack testing, Contract testing, Perf.   |
| `@finops-engineer`   | `@auditor`            | Cloud cost, Recon, GL, Regulatory (OJK/BI).    |
| `@finops-engineer`   | `@logic-builder`      | Reconciliation engine, Settlement logic.       |
| `@dx-engineer` | `@orchestrator`     | Git workflow, PR standards, CI/CD integration. |
| `@dx-engineer` | `@styler`           | Slidev presentations, Documentation styling.   |
| `@debugging-methodology` | `@tester`         | Root cause analysis, systematic debugging.     |

## 🔄 Execution Workflow

Berdasarkan `antigravity-lifecycle`, berikut adalah bagaimana kolaborasi terjadi:

1.  **Fase Plan**: `@lifecycle-manager` merancang rencana implementasi.
2.  **Fase Build**:
    -   `@logic-builder` menulis fungsionalitas kode.
    -   `@tester` menulis unit tests secara paralel.
    -   `@migrator` menangani perubahan skema database.
    -   `@builder` memastikan kode dapat di-compile dan di-package.
3.  **Fase Verify**:
    -   `@tester` menjalankan seluruh suite testing (Unit, Integration).
    -   `@auditor` melakukan penilaian keamanan dan kualitas kode.
4.  **Fase Sign-off**: `@orchestrator` menangani PR dan integrasi git.

## 🛡️ Guardrails

-   **Single Responsibility**: Setiap agen hanya memiliki satu tujuan spesifik.
-   **Context Isolation**: Penggunaan agen mengisolasi context eksekusi.
-   **Unattended Execution**: Agents dirancang untuk berjalan secara mandiri.

---

## 🔗 Dependency Chaining (v3.0.0)

Skills now support **Implicit Context Loading** via the `requires: [...]` directive in their frontmatter.
*   **Example**: Calling `@finops-engineer` implicitly loads `@data-architect` context.
*   **Impact**: Orchestrator (Anda) tidak perlu lagi menebak skill pendukung. Ikuti saja graf dependensi yang terdefinisi di `REGISTRY.yaml`.

### Consolidated Skills (January 2026)

| Old Skill | Merged Into | Notes |
|:----------|:------------|:------|
| `@information-architect` | `@principal-architect` | C4, ADR, Documentation |
| `@release-engineer` | `@platform-engineer` | Feature flags, rollouts |
| `@sre` | `@platform-engineer` | Observability, chaos, DR |
| `@bff-architect` | *Removed* | PayU is pure Java backend |

---

## ⚡ Parallel Execution & Skill Interconnectivity

Untuk mencapai kecepatan ekstrim, asisten AI harus menjalankan agen secara paralel ketika menangani tugas yang melibatkan banyak skill:

1.  **Parallel Dispatching**: Jika tugas melibatkan Frontend (`@frontend-architect`) dan Backend (`@core-banking-engineer`), aktifkan `@styler` and `@logic-builder` secara bersamaan (Swarm Mode).
2.  **Skill-to-Agent Handshake**:
    -   Jika `@frontend-architect` butuh visualisasi data, delegasikan ke `@web-artifacts-builder` -> `@builder`.
    -   Jika pengembangan fitur butuh presentasi, delegasikan ke `@dx-engineer` -> `@styler`.
3.  **Automated Interconnect**: Asisten wajib secara proaktif memanggil agen spesialis jika instruksi mencakup area yang di luar tanggung jawab agen utama.

---

## 🔧 Post-Audit Remediation Orchestration (Mar 2026 Priority)

Platform has completed Phase 1–12. Current priority is closing the 56 findings from the March 21 Deep Audit:

| Phase | Action | Agents | Priority |
|:------|:-------|:-------|:---|
| **Remediation** | PII Masking in Backoffice | `@core-banking-engineer` + `@cybersecurity-architect` | P1 |
| **Remediation** | IDOR/Access Control Fixes | `@api-architect` + `@cybersecurity-architect` | P0 |
| **Remediation** | Account Lockout Bypass | `@core-banking-engineer` + `@lifecycle-manager` | P1 |
| **Verification**| E2E Regression (703 tests) | `@tester` | Constant |

**Status Tracking**:
- Technical Status: `docs/roadmap/SERVICES.md`
- Roadmap Progress: `docs/roadmap/PROGRESS.md`
- Bug Backlog: `docs/roadmap/TODOS.md`

**Full workflow**: See `.agent/workflows/p19-remediation.md`

---
*Last Updated: March 2026 (v3.2.1 - Post-Audit Integration)*
