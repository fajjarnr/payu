# 🗺️ PayU AI Agents Mapping (Skills-to-Agents)

Dokumen ini memetakan bagaimana **Skills** (High-level capabilities) mengorkestrasi **Agents** (Dedicated execution units) untuk mencapai efisiensi maksimal dalam siklus pengembangan PayU.

## 🏗️ Core Mapping Strategy

| Triggering Skill     | Orchestrated Agent    | Rationale                                      |
| :------------------- | :-------------------- | :--------------------------------------------- |
| `@payu-development`  | `@scaffolder`         | Mengotomatisasi pembuatan service baru.        |
| `@backend-engineer`  | `@logic-builder`      | Implementasi Domain logic dan DDD entities.    |
| `@qa-engineer`       | `@tester`             | Penulisan test code dan eksekusi.              |
| `@security-engineer` | `@auditor`            | Audit keamanan dan kepatuhan (PCI-DSS/OJK).    |
| `@database-engineer` | `@migrator`           | Pengelolaan skema database dan migrasi Flyway. |
| `@devops-engineer`   | `@builder`            | Build, packaging, dan containerization.        |
| `@frontend-engineer` | `@styler`             | Estetika "Premium Emerald" dan A11y.           |
| `@devops-engineer`   | `@orchestrator`       | Alur CI/CD dan sinkronisasi git.               |
| `@payu-development`  | `@lifecycle-manager`  | Pengelola SDLC penuh (End-to-End).             |
| `@payu-development`  | `@scaffolding-expert` | Setup service end-to-end terintegrasi.         |
| `@security-engineer` | `@compliance-auditor` | Audit kepatuhan standar OJK/PCI-DSS mendalam.  |
| `@web-artifacts-builder` | `@builder`           | Scaffolding dan bundling single-file artifacts. |

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
