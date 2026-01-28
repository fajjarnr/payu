# 🗺️ PayU AI Agents Mapping (Skills-to-Agents)

Dokumen ini memetakan bagaimana **Skills** (High-level capabilities) mengorkestrasi **Agents** (Dedicated execution units) untuk mencapai efisiensi maksimal dalam siklus pengembangan PayU.

## 🏗️ Core Mapping Strategy

| Triggering Skill | Orchestrated Agent | Rationale |
| :--- | :--- | :--- |
| `@payu-development` | `@scaffolder` | Mengotomatisasi pembuatan service baru tanpa mengurusi detail boilerplate. |
| `@backend-engineer` | `@logic-builder` | Memfokuskan agen pada implementasi Domain logic dan DDD entities. |
| `@qa-engineer` | `@tester` | Memisahkan penulisan test code dan eksekusi dari pembangunan logic. |
| `@security-engineer`| `@security-auditor` | Melakukan audit kepatuhan (PCI-DSS) secara independen dan akurat. |
| `@database-engineer`| `@migrator` | Mengelola skema database dan migrasi Flyway secara terfokus. |
| `@frontend-engineer`| `@styler` | Menjamin estetika "Premium Emerald" tanpa mencampuradukkan business logic. |
| `@devops-engineer` | `@orchestrator` | Mengurus alur CI/CD dan sinkronisasi git secara efisien. |

## 🔄 Execution Workflow

Berdasarkan `antigravity-lifecycle`, berikut adalah bagaimana kolaborasi terjadi:

1.  **Fase Plan**: Skill (@backend-engineer) merancang solusi.
2.  **Fase Build**:
    - Skill memanggil `fork @logic-builder` untuk menulis kode domain.
    - Skill memanggil `fork @tester` untuk menulis unit tests secara paralel.
    - Jika ada perubahan DB, skill memanggil `fork @migrator`.
3.  **Fase Verify**:
    - Skill memanggil `fork @tester` untuk menjalankan full suite.
    - Skill memanggil `fork @security-auditor` untuk audit kepatuhan.
4.  **Fase Sign-off**: Skill memanggil `fork @orchestrator` untuk merge dan push.

## 🛡️ Guardrails

- **Single Responsibility**: Setiap agen hanya memiliki tool dan instruksi untuk satu tujuan spesifik.
- **Context Isolation**: Penggunaan agen melalui `fork` mengisolasi context eksekusi, mencegah "context pollution" pada agent utama.
- **Unattended Execution**: Agents dirancang untuk berjalan tanpa pengawasan, dengan kriteria output yang ketat (Final Report format).
