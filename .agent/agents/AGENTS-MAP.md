# 🗺️ PayU AI Agents Mapping (Skills-to-Agents)

Dokumen ini memetakan bagaimana **Skills** (High-level capabilities) mengorkestrasi **Agents** (Dedicated execution units) untuk mencapai efisiensi maksimal dalam siklus pengembangan PayU.

> **Note**: Setelah konsolidasi Januari 2026, PayU memiliki **17 Skills** yang terintegrasi.
> 
> **🚨 P19 AUDIT (Feb 2026)**: Production Readiness **48/100** — 5 P0 blockers.
> **ALWAYS read `.agent/context/P19-AUDIT-STATUS.md` FIRST** before any development work.
> Fix instructions: `docs/guides/REMEDIATION_PLAYBOOK.md` | Remediation workflow: `.agent/workflows/p19-remediation.md`

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

1. **Fase Plan**: `@lifecycle-manager` merancang rencana implementasi.
2. **Fase Build**:
   - `@logic-builder` menulis fungsionalitas kode.
   - `@tester` menulis unit tests secara paralel.
   - `@migrator` menangani perubahan skema database.
   - `@builder` memastikan kode dapat di-compile dan di-package.
3. **Fase Verify**:
   - `@tester` menjalankan seluruh suite testing (Unit, Integration).
   - `@auditor` melakukan penilaian keamanan dan kualitas kode.
4. **Fase Sign-off**: `@orchestrator` menangani PR dan integrasi git.

## 🛡️ Guardrails

- **Single Responsibility**: Setiap agen hanya memiliki satu tujuan spesifik.
- **Context Isolation**: Penggunaan agen mengisolasi context eksekusi.
- **Unattended Execution**: Agents dirancang untuk berjalan secara mandiri.

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

1. **Parallel Dispatching**: Jika tugas melibatkan Frontend (`@frontend-architect`) dan Backend (`@core-banking-engineer`), aktifkan `@styler` and `@logic-builder` secara bersamaan (Swarm Mode).
2. **Skill-to-Agent Handshake**:
   - Jika `@frontend-architect` butuh visualisasi data, delegasikan ke `@web-artifacts-builder` -> `@builder`.
   - Jika pengembangan fitur butuh presentasi, delegasikan ke `@dx-engineer` -> `@styler`.
3. **Automated Interconnect**: Asisten wajib secara proaktif memanggil agen spesialis jika instruksi mencakup area yang di luar tanggung jawab agen utama.

---

## 🔧 P19 Remediation Orchestration (Feb 2026 Priority)

Untuk membawa platform dari 48% → 80%, gunakan alur ini:

| Phase | Remedy | Agents | SP |
|:------|:-------|:-------|:---|
| **P0 Sprint 1** | R-001: JWT BFF | `@frontend-architect` + `@cybersecurity-architect` | 8 |
| **P0 Sprint 1** | R-002: Outbox integration | `@integration-architect` + `@core-banking-engineer` | 5 |
| **P0 Sprint 1** | R-003: Credential cleanup | `@cybersecurity-architect` + `@platform-engineer` | 3 |
| **P0 Sprint 1** | R-004: Starter tests | `@tester` | 5 |
| **P0 Sprint 1** | R-005: Port conflict | `@platform-engineer` | 1 |
| **P0 Sprint 1** | R-006: Starter integration | `@core-banking-engineer` | 3 |
| **P1 Sprint 2-3** | R-007: Quarkus security | `@core-banking-engineer` + `@cybersecurity-architect` | 8 |
| **P1 Sprint 2-3** | R-008: Hexagonal refactor | `@core-banking-engineer` + `@scaffolding-expert` | 13 |
| **P1 Sprint 2-3** | R-009: Fix E2E tests | `@tester` + `@frontend-architect` | 8 |
| **P2 Sprint 4** | R-013: Load tests | `@tester` | 8 |
| **P2 Sprint 4** | R-014: Contract tests | `@tester` | 5 |

**Full workflow**: See `.agent/workflows/p19-remediation.md`

---
*Last Updated: February 2026 (v3.1.0 - P19 Audit Integration)*
