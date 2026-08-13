# PRD §12.1 Launch Criteria Tracker

Status snapshot: 2026-08-13 (QAMVP-006). Each PRD §12.1 criterion mapped to
evidence. "Hijau" = live evidence exists; item hanya hijau dengan bukti nyata
(manifest/unit test bukan bukti production).

| # | Criterion | Status | Evidence |
|:--|:--|:--|:--|
| 1 | OJK license approved | 🟢 | External legal milestone (PRD) |
| 2 | Security audit passed | 🟡 | Pen test report SIGNED OFF 2025-01-22 (stale — re-pentest = PARTNER-PROD-010) |
| 3 | Load testing completed (100K) | 🟡 | k6 suite ada + CI (`k6-tests.yml`); green run butuh kredensial staging |
| 4 | Beta testing 1000 users | 🟡 | E2E blackbox 20 journey; beta gate belum ada |
| 5 | All core features functional | 🟡 | Money-flow integration tests live (QAMVP-002/007/008/009/010/013); MVP belum penuh (ACCOUNT-006, PROD-044) |
| 6 | Support team trained | 🟢 | Runbook (`docs/operations/`) |
| 7 | Local infra verification | 🟢 | podman-compose deploy 1.11.1 v2 — 36/36 healthy, 0 error loop, smoke UP |
| 8 | DRP & Backup-Restore | 🟡 | INFRA-026 Vault HA restore drill; PG HA/PITR belum (PARTNER-PROD-008) |
| 9 | Monitoring & Alerting | 🟢 | LokiStack/Prometheus (OPS-2026-08-01-04; lokistack-gateway rego blocked) |
| 10 | Pentest signed off | 🟡 | Stale 2025-01-22; re-pentest di PARTNER-PROD-010 |
| 11 | PCI-DSS & OJK compliance audit | 🟡 | UU PDP/POJK/BI evidence (kyc NIK AES-GCM, ledger immutable, dll) |
| 12 | **Production Deployment (OCP)** | 🔴 | **CB-006** — butuh cluster OCP + kredensial (tidak tersedia sesi ini) |
| 13 | **Mobile App Stores** | ⏸️ | **Deferred** — READY-061 mobile ditunda product owner |
| 14 | **Legal Readiness (ToS/Privacy)** | 🔴 | Dokumen legal eksternal (belum dipublikasikan) |
| 15 | **Security Hardening (Vault & CI/CD)** | 🟡 | CI/CD: 7 workflow (backend-tests, account-tests, contract-tests, kyc-tests, k6-tests, analytics-tests, login-gate) — per-service CI, contract verifier, coverage, mutation, k6. Vault: konfigurasi ada (Vault HA live di lab); secrets lint kosong di kode (ARCH-SECRET-001 sebagian fix) |

## Per-criterion detail yang ditambahkan sesi ini (1.11.1)

- **CI/CD hardening (no. 15)**: `.github/workflows/backend-tests.yml` (per-service
  matrix via paths-filter), `account-tests.yml` (jacoco report), `contract-tests.yml`
  (verifier 3+1 service), `kyc-tests.yml` (coverage gate 80%), `k6-tests.yml`
  (smoke/load/stress + SLO), ditambah yang sudah ada `analytics-tests.yml` +
  `login-gate.yml`.
- **Local infra (no. 7)**: deploy v2 1.11.1 — 36/36 sehat, 0 `APPLICATION FAILED`,
  scan log bersih.
- **Load testing (no. 3)**: k6 terhadap local stack terverifikasi (gateway + OIDC
  200); token flow butuh kredensial client staging.

## Blocker eksternal

- OCP cluster access (no. 12, CB-006, ARCH-TOPIC-002) — butuh login cluster.
- Mobile app stores (no. 13) — ditunda (READY-061).
- Dokumen legal ToS/Privacy (no. 14) — tim legal.
- Kredensial provider OCR/liveness (PROD-002 analog) & FX rate provider.
