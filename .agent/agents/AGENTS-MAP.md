# 🗺️ PayU AI Agents Mapping (Skills-to-Agents)

Dokumen ini memetakan bagaimana **Skills** (High-level capabilities) mengorkestrasi **Agents** (Dedicated execution units) untuk mencapai efisiensi maksimal dalam siklus pengembangan PayU.

## 🏗️ Core Mapping Strategy

| Triggering Skill     | Orchestrated Agent    | Rationale                                      |
| :------------------- | :-------------------- | :--------------------------------------------- |
| `@principal-architect` | `@scaffolder`         | Mengotomatisasi pembuatan service baru.        |
| `@principal-architect` | `@lifecycle-manager`  | Pengelola SDLC penuh (End-to-End).             |
| `@principal-architect` | `@scaffolding-expert` | Setup service end-to-end terintegrasi.         |
| `@core-banking-engineer` | `@logic-builder`    | Implementasi Domain logic dan DDD entities.    |
| `@api-architect`     | `@logic-builder`      | Standarisasi API Schemas & DTO logic.          |
| `@event-systems-architect` | `@logic-builder` | Implementasi Kafka messaging & Saga patterns.  |
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
| `@ai-engineer`       | `@logic-builder`      | Implementasi Async Service, Repository, & ETL. |
| `@nodejs-bff-architect` | `@logic-builder`   | BFF pattern, API aggregation, Express/Fastify. |
| `@information-architect` | `@lifecycle-manager` | ADR, RFC, Technical Documentation.            |
| **CONSOLIDATED SKILLS** | | |
| `@quality-engineer`  | `@tester`             | Full-stack testing, Contract testing, Perf.   |
| `@reliability-engineer` | `@auditor`          | SRE, Chaos, DR/BCP, Observability (LGTM).      |
| `@reliability-engineer` | `@tester`           | Chaos experiments, Game Days, fault injection. |
| `@finops-engineer`   | `@auditor`            | Cloud cost, Recon, GL, Regulatory (OJK/BI).    |
| `@finops-engineer`   | `@logic-builder`      | Reconciliation engine, Settlement logic.       |
| `@developer-experience` | `@orchestrator`     | Git workflow, PR standards, CI/CD integration. |
| `@developer-experience` | `@styler`           | Slidev presentations, Documentation styling.   |
| `@release-engineer`  | `@orchestrator`       | Feature flags, blue-green, canary rollouts.    |

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

## ⚡ Parallel Execution & Skill Interconnectivity

Untuk mencapai kecepatan ekstrim, asisten AI harus menjalankan agen secara paralel ketika menangani tugas yang melibatkan banyak skill:

1. **Parallel Dispatching**: Jika tugas melibatkan Frontend (`@frontend-architect`) dan Backend (`@core-banking-engineer`), aktifkan `@styler` and `@logic-builder` secara bersamaan (Swarm Mode).
2. **Skill-to-Agent Handshake**:
   - Jika `@frontend-architect` butuh visualisasi data, delegasikan ke `@web-artifacts-builder` -> `@builder`.
   - Jika pengembangan fitur butuh presentasi, delegasikan ke `@developer-experience` -> `@styler`.
3. **Automated Interconnect**: Asisten wajib secara proaktif memanggil agen spesialis jika instruksi mencakup area yang di luar tanggung jawab agen utama (misal: penulisan dokumen teknis di `@information-architect` didelegasikan ke `@developer-experience` jika butuh deck).

---
*Last Updated: January 2026*
