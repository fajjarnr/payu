# 🗺️ PayU AI Agents Mapping (Skills-to-Agents)

Dokumen ini memetakan bagaimana **Skills** (High-level capabilities) mengorkestrasi **Agents** (Dedicated execution units) untuk mencapai efisiensi maksimal dalam siklus pengembangan PayU.

> **Note**: Ekosistem agen PayU didesain untuk skalabilitas microservices dengan prinsip **Decentralized Parallel Execution**.
> **Source of Truth**: Gunakan `docs/roadmap/TODOS.md` untuk backlog tugas dan `docs/roadmap/SERVICES.md` untuk inventori teknis.

## 🏗️ Core Mapping Strategy

| Triggering Skill     | Orchestrated Agent    | Rationale                                      |
| :------------------- | :-------------------- | :--------------------------------------------- |
| `@principal-architect` | `@scaffolder`         | Mengotomatisasi pembuatan service baru.        |
| `@principal-architect` | `@lifecycle-manager`  | Pengelola SDLC penuh (End-to-End).             |
| `@principal-architect` | `@scaffolding-expert` | Setup service end-to-end terintegrasi.         |
| `@principal-architect` | `@doc-writer`         | ADR, C4 diagrams, dan technical documentation. |
| `@core-banking-engineer` | `@logic-builder`    | Implementasi Domain logic dan DDD entities.    |
| `@api-architect`     | `@logic-builder`      | Standarisasi API Schemas, DTOs, & Contract-first logic. |
| `@api-architect`     | `@frontend-dev`       | API client implementation dan typed fetch.     |
| `@integration-architect` | `@logic-builder` | Implementasi Kafka messaging & Saga patterns.  |
| `@integration-architect` | `@kafka-engineer` | Topic design, CDC pipelines, event sourcing.   |
| `@data-architect`    | `@migrator`           | Pengelolaan skema database, migrasi Flyway, partitioning, optimization. |
| `@data-governance-architect` | `@compliance-auditor` | Data lineage, PII audit, retention policies. |
| `@frontend-architect`| `@styler`             | Estetika "Premium Emerald" dan A11y.           |
| `@frontend-architect`| `@frontend-dev`       | Next.js 15+ implementation, Server Components. |
| `@mobile-architect`  | `@styler`             | Styling React Native (NativeWind/Reanimated).  |
| `@mobile-architect`  | `@logic-builder`      | Logic React Native, Offline sync, State mgmt.  |
| `@product-designer`  | `@styler`             | Design Tokens, Palettes, Typography systems.   |
| `@web-artifacts-builder` | `@builder`           | Scaffolding dan bundling single-file artifacts. |
| `@cybersecurity-architect` | `@auditor`          | Audit keamanan dan kepatuhan (PCI-DSS/OJK).    |
| `@cybersecurity-architect` | `@compliance-auditor` | Audit kepatuhan standar OJK/PCI-DSS mendalam.  |
| `@cybersecurity-architect` | `@security-pentester` | Penetration testing, vulnerability scanning.   |
| `@platform-engineer` | `@builder`            | Build, packaging, dan containerization.        |
| `@platform-engineer` | `@orchestrator`       | Alur CI/CD dan sinkronisasi git.               |
| `@platform-engineer` | `@tester`             | Chaos experiments, Game Days, fault injection. |
| `@ai-engineer`       | `@logic-builder`      | Implementasi Async Service, Repository, & ETL. |
| `@ai-engineer`       | `@ml-pipeline-builder`| FastAPI services, model deployment, pipelines. |
| `@mobile-architect`  | `@mobile-dev`         | React Native/Expo implementation, offline-first, biometrics. |
| `@quality-engineer`  | `@tester`             | Full-stack testing, Contract testing, Perf.   |
| `@quality-engineer`  | `@security-pentester` | Security testing integration, OWASP Top 10.    |
| `@finops-engineer`   | `@finops-engineer`    | Reconciliation, settlement, GL, cloud cost, regulatory. |
| `@finops-engineer`   | `@logic-builder`      | Reconciliation engine, Settlement logic.       |
| `@finops-engineer`   | `@auditor`            | Cloud cost audit, Recon review, Regulatory (OJK/BI). |
| `@dx-engineer` | `@orchestrator`     | Git workflow, PR standards, CI/CD integration. |
| `@dx-engineer` | `@styler`           | Slidev presentations, Documentation styling.   |
| `@dx-engineer` | `@doc-writer`       | Developer guides, onboarding docs, changelog.  |
| `@debugging-methodology` | `@tester`         | Root cause analysis, systematic debugging.     |
| `@debugging-methodology` | `@security-pentester` | Security-related bug investigation.          |

## 🔄 Execution Workflow

Berdasarkan `antigravity-lifecycle`, berikut adalah bagaimana kolaborasi terjadi:

1.  **Fase Plan**: `@lifecycle-manager` merancang rencana implementasi.
2.  **Fase Build**:
    -   `@logic-builder` menulis fungsionalitas kode.
    -   `@frontend-dev` mengimplementasikan Next.js components secara paralel.
    -   `@mobile-dev` mengimplementasikan React Native screens secara paralel.
    -   `@tester` menulis unit tests secara paralel.
    -   `@migrator` menangani perubahan skema database, partitioning, dan optimization.
    -   `@kafka-engineer` mengimplementasikan event streaming dan CDC.
    -   `@ml-pipeline-builder` membangun ML services dan ETL pipelines.
    -   `@finops-engineer` membangun reconciliation engine dan settlement logic.
    -   `@builder` memastikan kode dapat di-compile dan di-package.
3.  **Fase Verify**:
    -   `@tester` menjalankan seluruh suite testing (Unit, Integration).
    -   `@security-pentester` melakukan penetration testing dan vulnerability scanning.
    -   `@auditor` melakukan penilaian keamanan dan kualitas kode.
    -   `@finops-engineer` memverifikasi cloud cost dan regulatory compliance.
4.  **Fase Document**: `@doc-writer` membuat ADR, runbooks, dan technical docs.
5.  **Fase Sign-off**: `@orchestrator` menangani PR dan integrasi git.

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

---
*Last Updated: 2026-05-04*
