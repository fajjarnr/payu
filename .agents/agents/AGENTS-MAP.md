# 🗺️ AI Agents Mapping (Skills-to-Agents)

Dokumen ini memetakan bagaimana **Skills** (capabilities) mengorkestrasi
**Agents** (dedicated execution units) dalam siklus pengembangan. Skills
mendefinisikan *apa* yang dikerjakan dan standarnya; Agents mengeksekusi
pekerjaan spesifik.

> **Source of Truth**: Daftar skill di `.agents/skills/REGISTRY.yaml`, daftar
> agen di `.agents/agents/`. Jangan merujuk agen yang tidak ada di folder ini.

## 🏗️ Core Mapping Strategy

| Triggering Skill | Orchestrated Agent | Rationale |
| :--------------- | :----------------- | :-------- |
| `@principal-architect` | `@scaffolder` | Membuat struktur service/modul baru. |
| `@principal-architect` | `@scaffolding-expert` | Setup service end-to-end (CI/CD, observability, gateway). |
| `@principal-architect` | `@lifecycle-manager` | Menjalankan SDLC penuh untuk task besar. |
| `@core-banking-engineer` | `@logic-builder` | Implementasi domain logic dan DDD entities. |
| `@api-architect` | `@logic-builder` | Implementasi skema API, DTOs, dan contract-first logic. |
| `@integration-architect` | `@logic-builder` | Implementasi messaging (Kafka) & saga patterns. |
| `@data-architect` | `@migrator` | Skema database, migrasi, partitioning, optimasi query. |
| `@data-governance-architect` | `@compliance-auditor` | Data lineage, PII audit, retention policies. |
| `@frontend-architect` | `@styler` | Implementasi design system dan a11y. |
| `@mobile-architect` | `@styler` | Styling React Native (NativeWind/Reanimated). |
| `@product-designer` | `@styler` | Design tokens, palettes, typography systems. |
| `@web-artifacts-builder` | `@builder` | Scaffolding dan bundling single-file artifacts. |
| `@cybersecurity-architect` | `@auditor` | Audit keamanan dan kepatuhan. |
| `@cybersecurity-architect` | `@compliance-auditor` | Audit kepatuhan standar mendalam (PCI-DSS/OJK/GDPR). |
| `@platform-engineer` | `@builder` | Build, packaging, dan containerization. |
| `@platform-engineer` | `@orchestrator` | Alur CI/CD dan sinkronisasi git. |
| `@ai-engineer` | `@logic-builder` | Implementasi service logic dan ETL. |
| `@quality-engineer` | `@tester` | Full-stack testing, contract testing, perf. |
| `@dx-engineer` | `@orchestrator` | Git workflow, PR standards, CI/CD integration. |
| `@debugging-methodology` | `@tester` | Root cause analysis, systematic debugging. |
| `@finops-engineer` | `@auditor` | Audit biaya cloud (OpenCost/Kubecost), alokasi, budget alerts & idle-resource detection. |

## 🔄 Execution Workflow

1. **Fase Plan**: `@lifecycle-manager` merancang rencana implementasi (dengan
   persetujuan user).
2. **Fase Build**:
   - `@scaffolder` / `@scaffolding-expert` membuat struktur awal.
   - `@logic-builder` menulis domain logic.
   - `@tester` menulis unit/integration tests (TDD).
   - `@migrator` menangani perubahan skema database.
   - `@styler` mengimplementasikan UI styling.
   - `@builder` memastikan kode dapat di-compile dan di-package.
3. **Fase Verify**:
   - `@tester` menjalankan seluruh suite testing.
   - `@auditor` / `@compliance-auditor` melakukan audit keamanan dan kualitas.
4. **Fase Sign-off**: `@orchestrator` menangani PR dan integrasi git;
   `@lifecycle-manager` menyajikan walkthrough.

## 🛡️ Guardrails

- **Single Responsibility**: Setiap agen memiliki satu tujuan spesifik; jangan
  menumpuk tanggung jawab.
- **Context Isolation**: Penggunaan agen mengisolasi context eksekusi.
- **Unattended Execution**: Agents dirancang untuk berjalan secara mandiri,
  tetapi perubahan besar tetap butuh persetujuan user.

## ⚡ Parallel Execution & Skill Interconnectivity

Untuk tugas yang melibatkan banyak skill, jalankan agen secara paralel ketika
mereka menyentuh file/area yang berbeda:

1. **Parallel Dispatching**: Tugas yang melibatkan Frontend
   (`@frontend-architect`) dan Backend (`@core-banking-engineer`) → jalankan
   `@styler` dan `@logic-builder` secara bersamaan.
2. **Skill-to-Agent Handshake**:
   - Jika butuh visualisasi data → `@web-artifacts-builder` → `@builder`.
   - Jika butuh presentasi → `@dx-engineer` → `@orchestrator` (docs) / `@styler`.
3. **Automated Interconnect**: Panggil agen spesialis secara proaktif jika
   instruksi mencakup area di luar tanggung jawab agen utama.

---

*Last Updated: 2026-08-21*
