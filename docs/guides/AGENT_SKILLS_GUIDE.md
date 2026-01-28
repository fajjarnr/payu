# PayU Agent Skills Guide: Multi-Engineer AI Ecosystem

Selamat datang di ekosistem pengembangan **PayU Digital Banking Platform**. Dokumentasi ini menjelaskan cara kerja, alur penggunaan, dan pembagian peran dari 20 AI Skills yang telah dirancang untuk mempercepat siklus pengembangan perangkat lunak (SDLC) Anda.

---

## 🏛️ Arsitektur Skill

Skill di PayU dirancang dengan prinsip **Specialized Agents**. Alih-alih satu AI yang mengetahui segalanya secara dangkal, kita menggunakan banyak AI spesialis yang berkolaborasi.

### 1. Skill Inti (The Entry Point)
*   **`@payu-development`**: Pintu utama. Gunakan ini untuk memahami arsitektur makro, daftar microservices, pola desain utama (Hexagonal, CQRS, DDD), dan alur kerja antar-layanan.

### 2. Engineering Specialists (Role-Based)
Setiap skill ini memiliki pengetahuan mendalam tentang tech stack dan best practices spesifik:

| Skill | Fokus Utama |
| :--- | :--- |
| **`@backend-engineer`** | Spring Boot, Quarkus, FastAPI, Hexagonal Architecture, TDD cycle. |
| **`@frontend-engineer`**| Next.js, React, Design System Patterns (Polymorphic components, Slot, Theming). |
| **`@mobile-engineer`**  | React Native, Expo, Biometrics, Secure Token Storage. |
| **`@ml-engineer`**      | Fraud Detection, Analytics, Python Data Processing. |
| **`@cto-advisor`**      | Strategic leadership, Tech Debt analysis, Team Scaling, and DORA metrics. |
| **`@database-engineer`**| PostgreSQL optimization, JSONB patterns, migrations (Flyway). |
| **`@security-engineer`**| PCI-DSS v4.0, OJK compliance, Encryption (AES-GCM), PII Masking. |
| **`@error-handling-engineer`** | Robust error patterns, Circuit Breakers, and Graceful Degradation. |
| **`@api-design`**       | REST API documentation (OpenAPI), versioning, and endpoint standards. |
| **`@event-driven-architecture`** | Kafka messaging, Saga patterns, Event Sourcing, and Idempotency. |

### 3. Workflow & Infrastructure Specialists
Skill yang menangani siklus hidup kode dan eksekusi:

| Skill | Fokus Utama |
| :--- | :--- |
| **`@devops-engineer`**    | Tekton/ArgoCD pipelines, Deployment strategies, Shell/BATS automation. |
| **`@container-engineer`** | Container hardening (UBI-9), OpenShift Security Contexts (SCC). |
| **`@observability-engineer`** | Distributed Tracing (Jaeger), Logs (Loki), Metrics (Prometheus). |
| **`@git-workflow`**      | Branching strategies (Gitflow/Trunk), Conventional Commits. |
| **`@qa-engineer`**       | TDD, Testcontainers, Performance Testing (Gatling). |
| **`@debugging-engineer`** | Systematic RCA, memory leak analysis, and performance debugging. |
| **`@code-review`**      | Automated code quality gates, formatting, and review consistency. |
| **`@c4-architecture`**  | Architecture visualization using the C4 model (Context, Container, Component, Deployment). |
| **`@docs-engineer`**      | ADR (Architecture Decision Records), OpenAPI docs, and Runbooks. |

---

## 🔄 Alur Penggunaan (SDLC Workflow)

Gunakan alur berikut saat mengimplementasikan fitur baru untuk hasil yang maksimal:

### Fase 1: Perencanaan & Desain
1.  Panggil **`@payu-development`** untuk memahami konteks layanan.
2.  Gunakan **`@api-design`** untuk merencanakan kontrak REST/Kafka.
3.  Konsultasikan dengan **`@security-engineer`** untuk kepatuhan PCI-DSS/OJK sejak awal.

### Fase 2: Implementasi (TDD)
1.  Gunakan **`@qa-engineer`** untuk merancang strategi pengujian dan cakupan coverage.
2.  Panggil **`@backend-engineer`** (yang mencakup pola TDD) atau **`@frontend-engineer`** untuk menulis logika bisnis menggunakan pola Clean/Hexagonal.
3.  Jika tugas sangat besar (misal: merombak 3 service), gunakan workflow **`/multi-agent-coordination`** untuk membagi tugas secara paralel.

### Fase 3: Verifikasi & Review
1.  Selalu jalankan checklist dari **`@code-review`** sebelum membuat PR.
2.  Pastikan commit message dan branch name sesuai standar **`@git-workflow`**.

### Fase 4: Pengiriman (Deploy)
1.  Gunakan **`@container-engineer`** untuk memastikan image kontainer aman dan efisien.
2.  Panggil **`@devops-engineer`** untuk merancang pipeline Tekton atau manifes ArgoCD untuk rilis Canary.

---

## 📜 Aturan Emas (Guardrails)

1.  **Strict Isolation**: Jangan biarkan logika infrastruktur (DB/Framework) bocor ke layer Domain. (Lihat `@backend-engineer`).
2.  **PII Privacy**: Jangan pernah mencatat (log) data sensitif seperti CVV atau NIK tanpa masking. (Lihat `@security-engineer`).
3.  **Idempotency**: Semua operasi kritis (transfer/pembayaran) harus mendukung kunci idempotensi. (Lihat `@event-driven-architecture`).
4.  **Aesthetics First**: Aplikasi frontend/mobile harus memukau secara visual dan mendukung aksesibilitas (A11y). (Lihat `@frontend-engineer`).

---

## 💡 Tips untuk Developer & AI Agent

*   **Panggil Skill Secara Spesifik**: Saat menulis kode backend, aktifkan `@backend-engineer`. Jangan hanya mengandalkan AI umum.
*   **Gunakan Skill Map**: Jika Anda buntu, buka `@payu-development` bagian "Specialized Skills Map" untuk menemukan spesialis yang tepat.
*   **Checklist adalah Hukum**: Jangan abaikan checklist di akhir setiap file SKILL.md. Itu adalah standar kualitas minimal kami.

---
*Last Updated: January 2026*
