# PayU Agent Skills Guide: Multi-Engineer AI Ecosystem

Selamat datang di ekosistem pengembangan **PayU Digital Banking Platform**. Dokumentasi ini menjelaskan cara kerja, alur penggunaan, dan pembagian peran dari AI Skills dan Specialized Agents yang telah dirancang untuk mempercepat siklus pengembangan perangkat lunak (SDLC) Anda.

---

## 🏛️ Arsitektur AI Ecosystem

Ekosistem AI PayU dibagi menjadi dua komponen utama yang saling berkolaborasi:
1.  **Skills (The Brains)**: Berisi pengetahuan mendalam, best practices, dan panduan desain untuk domain tertentu.
2.  **Agents (The Workers)**: Unit eksekusi spesialis yang dirancang untuk menjalankan tugas teknis secara otomatis, aman, dan efisien.

### 🧩 Peta Komponen (Source of Truth)
Semua konfigurasi AI dipusatkan di direktori `.agent/` dan diakses oleh Claude Code melalui symbolic links di `.claude/`.

```
payu/
├── .agent/               # Master Configuration (Source of Truth)
│   ├── skills/           # 20+ High-level AI Skills
│   ├── agents/           # 7 Specialized Execution Agents
│   ├── workflows/        # SDLC & Coordination Workflows
│   └── settings.json     # Global AI Access (Bypass Permissions)
└── .claude/              # Entry Point (Soft links to .agent/)
```

---

## 🤖 Specialized AI Agents (The Workers)

Agen dirancang untuk eksekusi tugas yang terisolasi. Mereka memiliki izin penuh (`bypassPermissions`) untuk mempercepat automasi teknis.

| Agent | Deskripsi Peran | Target Output |
| :--- | :--- | :--- |
| **`@scaffolder`** | Arsitek boilerplate & struktur. | Folder structure, pom.xml, Dockerfile. |
| **`@logic-builder`** | Spesialis DDD & Business Logic. | Rich Domain Entities, Application Services. |
| **`@tester`** | Gatekeeper kualitas & TDD. | JUnit 5, Integration Tests, Performance Reports. |
| **`@security-auditor`**| Penjaga kepatuhan & audit. | Vulnerability scans, PII audit, RBAC check. |
| **`@migrator`** | Administrasi Database. | Flyway scripts, SQL optimization, JSONB design. |
| **`@styler`** | Spesialis UI/UX "Premium Emerald". | CSS, Framer Motion, A11y compliance. |
| **`@orchestrator`** | Automasi Git & CI/CD. | PR logic, Pipeline updates, Branch management. |

---

## 🧠 AI Skills (The Knowledge Base)

Skill memberikan konteks strategis sebelum eksekusi dimulai.

### 1. Engineering Specialists (Role-Based)
| Skill | Fokus Utama |
| :--- | :--- |
| **`/backend-engineer`** | Spring Boot, Quarkus, Hexagonal Architecture. |
| **`/frontend-engineer`**| Next.js, React, Design System Patterns. |
| **`/database-engineer`**| PostgreSQL optimization, JSONB, Flyway. |
| **`/security-engineer`**| PCI-DSS v4.0, OJK compliance, Encryption. |
| **`/event-driven-architecture`** | Kafka, Saga, Sourcing, Idempotency. |

### 2. Workflow & Infrastructure Specialists
| Skill | Fokus Utama |
| :--- | :--- |
| **`/devops-engineer`**    | CI/CD Pipelines, Tekton, ArgoCD. |
| **`/qa-engineer`**       | TDD, Testcontainers, Performance Strategy. |
| **`/docs-engineer`**      | ADR, OpenAPI docs, Runbooks. |
| **`/c4-architecture`**  | Visualisasi arsitektur (Mermaid). |

---

## 🔄 Alur Kerja SDLC Terintegrasi

Gunakan alur berikut untuk mengorkestrasi Skills dan Agents secara efektif:

### Fase 1: Discovery & Planning (Skill Focus)
1.  Gunakan **`/payu-development`** untuk memahami konteks arsitektur layanan.
2.  Gunakan **`/api-design`** atau **`/database-engineer`** untuk merancang kontrak data.
3.  **Output**: Dokumen rencana atau desain yang divalidasi.

### Fase 2: Scaffolding & Setup (Agent Focus)
1.  Jika membuat service baru, panggil `fork @scaffolder`.
2.  Jika membutuhkan skema DB baru, panggil `fork @migrator`.
3.  **Output**: Boilerplate kode dan skema DB siap pakai.

### Fase 3: Core Implementation (Skill & Agent Collaboration)
1.  Panggil **`/backend-engineer`** untuk panduan implementasi DDD.
2.  Gunakan `@logic-builder` untuk menulis logika domain fungsional.
3.  Gunakan `@tester` secara paralel untuk menulis unit tests (TDD).
4.  **Output**: Kode fungsional dengan 100% logic coverage.

### Fase 4: Optimization & Verification (Audit Focus)
1.  Gunakan `@styler` untuk memoles tampilan frontend (jika relevan).
2.  Panggil `@security-auditor` untuk memastikan tidak ada kebocoran PII dan audit RBAC.
3.  Gunakan **`/code-review`** untuk final check sebelum commit.

### Fase 5: Delivery (Orchestration Focus)
1.  Gunakan `@orchestrator` untuk merapikan branch, melakukan squash commit, dan pushing ke remote.
2.  Panggil **`/devops-engineer`** untuk memantau status pipeline di Tekton/ArgoCD.

---

## 📜 Golden Rules & Guardrails

1.  **Source of Truth**: Selalu edit konfigurasi di folder `.agent/`. Folder `.claude/` hanya berisi link agar Claude Code bekerja.
2.  **No Context Pollution**: Gunakan `fork` untuk tugas teknis berat agar context percakapan utama tetap bersih dan fokus pada strategi.
3.  **Audit Before Release**: Jangan pernah merge kode tanpa laporan sukses dari `@security-auditor` dan `@tester`.
4.  **Bypass Permissions**: Gunakan mode ini hanya di lingkungan pengembangan yang terkontrol demi kecepatan automasi.

---

## 💡 Tips untuk Developer
*   **Delegasi, Bukan Lepas Tangan**: Agents sangat efisien melakukan "heavy lifting", tapi Anda tetap bertanggung jawab untuk memverifikasi laporan akhir mereka.
*   **Gunakan AGENTS-MAP**: Jika bingung harus memanggil siapa, lihat [`AGENTS-MAP.md`](../.agent/agents/AGENTS-MAP.md).

---
*Last Updated: January 2026*
