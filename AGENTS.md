# AGENTS.md — PayU Digital Banking Platform

> Panduan singkat untuk AI Agent. Detail lengkap ada di `docs/` — file ini hanya berisi aturan yang **wajib dipatuhi** + pointer.

## What is PayU

Core banking & payment gateway (microservices, event-driven, hexagonal) di Red Hat OpenShift 4.20+. Melayani integrator eksternal (TokoBapak, Nobar) dengan standar **SNAP-BI**. Stack: Java (Spring Boot), Quarkus, Python (FastAPI), TypeScript (Next.js/Expo), PostgreSQL, Kafka.

## Commands

| Action         | Command                                                  |
| :------------- | :------------------------------------------------------- |
| Build backend  | `mvn -f backend/pom.xml clean package -DskipTests -T 1C` |
| Run web app    | `cd frontend/web-app && npm run dev`                     |
| Local infra    | `podman compose up -d`                                   |
| All tests      | `make test`                                              |
| Single service | `./scripts/test-single-service.sh <service>`             |
| Check services | `oc get pods` / `podman ps`                              |

## Layout

- backend/ services + shared/ (starters) + simulators/
- frontend/ web-app (Next.js) · mobile (Expo) · developer-docs
- sdk/ · infrastructure/ (OpenShift/Helm/ArgoCD) · scripts/ · tests/
- docs/ architecture · product · operations · security · roadmap · guides
- .agents/ skills/ · agents/ · workflows/ (BACA sebelum tugas arsitektural)

## 🚨 Non-Negotiable Rules

1. **Money**: Gunakan `BigDecimal` (NEVER `float`/`double`), rounding `HALF_EVEN`. Kolom DB `DECIMAL(19,4)`.
2. **Immutable ledger**: No UPDATE/DELETE data keuangan. Double-entry (debit+credit), koreksi via reversal entry.
3. **Idempotency**: Semua endpoint payment/transfer wajib header `X-Idempotency-Key`.
4. **Event Publishing**: Publish events via `outbox-starter` (bukan direct `kafkaTemplate.send()`). Format CloudEvents 1.0.2, topic `payu.<domain>.<event-type>.v<n>`. DLQ pakai suffix `.dlq`.
5. **Hexagonal Architecture**: External comms wajib lewat Port. Gunakan shared starters (`security-`, `resilience-`, `cache-`). DTOs ditaruh di package `interfaces.dto` sebelum logic.
6. **API & Error Standards**: Path versioned (`/v1/...`), plural kebab-case. Error format RFC 9457 dengan code unik (e.g. `ACC_001`).
7. **Lombok Fallback**: Jika Lombok (`@Getter`, `@Setter`, `@Builder`, `@Slf4j`) gagal compile >2x, tulis implementasi manual (explicit) demi stabilitas build.
8. **Enum Placement**: Selalu definisikan Enum domain sebagai file top-level (bukan inner class).
9. **Frontend**: Next.js maksimalkan Server Components; gunakan `"use client"` seminimal mungkin (hanya leaf components).
10. **Container Standard**: UBI9, non-root (UID 1001), drop ALL capabilities, read-only FS, port 8080.
11. **Security**: Mask PII (NIK/PIN) di logs, encrypt di DB. No secrets di code/properties (gunakan Vault). Strict mTLS. No `setenforce 0`.
12. **TDD (Test-Driven Development)**: 
    - NO PRODUCTION CODE tanpa failing test terlebih dahulu.
    - Core domain 100% coverage, others 80-90%. ArchUnit per service.
    - Jangan test mock behavior. Fokus ke real behavior.
    - Frontend testing: uji user behavior (React Testing Library), jangan internal state/CSS.
13. **Git & SemVer**:
    - Conventional Commits: `type(scope): msg`. No force-push ke protected branches.
    - SemVer `MAJOR.MINOR.PATCH` (MAJOR = breaking API/DB/event, MINOR = feature/service, PATCH = bugfix/config).
    - Pre-release: `-alpha.N`, `-beta.N`, `-rc.N` untuk staging.
    - Image tag wajib matching dengan git tag (contoh: `v1.8.7`).
    - CHANGELOG: No duplicate version entries, gunakan format tanggal ISO 8601 (`YYYY-MM-DD`).

## 🧠 AI Working Protocol & Debugging

- **Design-First Gate**: Dilarang menulis code/scaffold fitur baru sebelum mengajukan design plan dan disetujui user.
- **Root Cause Reproduction**: Jangan langsung fix bug. Mulai dengan membuat test case yang mereproduksi error secara konsisten (TDD).
- **Development Loop**: Selalu jalankan loop sekuensial: Error Analysis → Minimal Fix → Local Test → Build Tag → Deploy → E2E Verify.
- **Stop on Blockers**: Jika menemui ambiguitas atau perbaikan gagal >2x, segera STOP dan tanya user. Dilarang menggunakan placeholder (`TODO`, `TBD`).
- **Evidence Before Claims**: Tunjukkan bukti run/output command. Dilarang mengklaim "tests pass" atau "should work" tanpa bukti nyata.
- **Subagent Strategy (Swarm Mode)**: Manfaatkan subagent untuk riset/eksekusi terisolasi secara paralel. Collision guard: eksekusi paralel HANYA jika menyentuh file/service berbeda; jika berbagi file/state wajib sekuensial. Gunakan `git worktree` di `.worktrees/` (masuk `.gitignore`) untuk tugas paralel skala besar agar workspace bersih. Sebelum merge/PR, jalankan subagent reviewer untuk mengaudit diff.
- **Skills Usage**: Jika ada skill di `.agents/skills/` yang relevan (misal: `debugging-methodology`), **wajib** dibaca dan diikuti.
- **No Performative Agreement**: Hindari kalimat basa-basi seperti "You're absolutely right!" atau "Great point!". Cukup acknowledge teknis atau langsung eksekusi.
- **Self-Improvement Loop**: Setelah menerima koreksi/feedback dari user, update `docs/guides/LESSONS.md` untuk menghindari error berulang.

## 🤝 Collaboration Modes
*   **Driver**: AI menulis kode aktif.
*   **Navigator**: AI menyusun rencana, mereview, membimbing; User menulis kode.
*   **TDD**: Fokus penulisan test merah-hijau-refactor.
*   **Review**: AI hanya mengaudit keamanan, logic, & style (tanpa menulis kode).
*   **Mentor**: AI menjelaskan konsep & pola tanpa memberi solusi langsung.

## 🔄 Doc Routing (Jangan Campur Konten)

| Konten | File |
| :--- | :--- |
| Bug baru, open items, actionable todos | `docs/roadmap/TODOS.md` |
| Deployment status, completed milestones | `docs/roadmap/PROGRESS.md` |
| Architecture decisions, gap analysis | `docs/roadmap/GATEWAY_ARCH.md` |
| Version changelog | `CHANGELOG.md` |
| Implementation patterns & lessons learned | `docs/guides/LESSONS.md` |

## 🛰️ Deep Reference (Baca Saat Relevan)

- Tech stack & 14 Laws & DORA → `docs/architecture/ARCHITECTURE.md`
- Service status → `docs/roadmap/SERVICES.md`
- Database & schema standards (ledger, RLS, Flyway, pgcrypto) → `docs/architecture/`
- API & integration (retry/webhook/SNAP-BI) → `docs/architecture/`
- Frontend / Mobile / Python-ML & Design System (Premium Emerald) standards → `docs/guides/`
- Skills & workflows → `.agents/skills/` dan `.agents/workflows/`
