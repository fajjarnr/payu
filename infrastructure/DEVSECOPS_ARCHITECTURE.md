# PRD — DevSecOps Enterprise-Grade Pipeline (Open Source Edition)

## OpenShift Container Platform — Payu Namespace Strategy

| Field               | Value                                               |
| ------------------- | --------------------------------------------------- |
| **Versi**           | **1.2.0** _(Updated from 1.1.0)_                    |
| **Status**          | Draft — Internal Review                             |
| **Tanggal**         | April 2026                                          |
| **Author**          | Platform Engineering Team                           |
| **Klasifikasi**     | CONFIDENTIAL _(Internal Personal Project Use Only)_ |
| **Target Audience** | Engineering Lead, CISO, Platform Team               |

> ⚠️ **Disclaimer**: Dokumen ini bersifat CONFIDENTIAL untuk keperluan internal dan personal project development. Tidak untuk distribusi eksternal atau produksi komersial tanpa review legal dan keamanan.

---

## Daftar Isi

1. [Executive Summary](#1-executive-summary)
2. [Tujuan dan Sasaran](#2-tujuan-dan-sasaran)
3. [Namespace Strategy & Promotion Gate](#3-namespace-strategy--promotion-gate)
4. [Pipeline Stages — Kebutuhan Detail](#4-pipeline-stages--kebutuhan-detail)
5. [OWASP Top 10 & API Security Compliance Matrix](#5-owasp-top-10--api-security-compliance-matrix)
6. [Tool Stack — 100% Open Source](#6-tool-stack--100-open-source)
7. [Implementation Roadmap](#7-implementation-roadmap)
8. [Non-Functional Requirements](#8-non-functional-requirements)
9. [Risiko & Mitigasi](#9-risiko--mitigasi)
10. [Developer Experience (DevEx)](#10-developer-experience-devex)
11. [Glosarium](#11-glosarium)

---

## 1. Executive Summary

Dokumen ini mendefinisikan kebutuhan produk untuk implementasi pipeline DevSecOps enterprise-grade pada platform OpenShift Container Platform (OCP) yang digunakan oleh Payu. Pipeline ini dirancang untuk memastikan keamanan end-to-end mulai dari commit kode hingga deployment production, dengan kepatuhan penuh terhadap **OWASP Top 10 2025**, **OWASP API Security Top 10 2023**, standar **SLSA Level 2+**, dan regulasi keamanan finansial (**PCI-DSS v4.0**, ISO 27001, Bank Indonesia).

> **Scope:** Semua aplikasi yang di-deploy di namespace `payu-dev`, `payu-sit`, `payu-uat`, `payu-preprod`, dan `payu` (production) wajib melewati pipeline ini tanpa pengecualian.

Pipeline ini mengintegrasikan **OpenShift Pipelines (Tekton)**, **OpenShift GitOps (ArgoCD)**, dan **OpenShift Advanced Cluster Security (StackRox)** sebagai foundation, diperkuat dengan **100% tooling open-source** untuk menutup gap keamanan pada setiap stage dengan biaya lisensi minimal.

**Prinsip Utama:**

- ✅ **Shift-Left Security**: Deteksi celah keamanan sedini mungkin di stage source
- ✅ **Zero-Trust Architecture**: mTLS mandatory antar service, deny-by-default network policy
- ✅ **Immutable Infrastructure**: Tidak ada perubahan langsung ke namespace tanpa GitOps workflow
- ✅ **Supply Chain Security**: Image signing wajib (Cosign + Sigstore) sebelum deployment
- ✅ **Continuous Compliance**: Monitoring via ComplianceOperator, Wazuh, dan ACS policy engine
- ✅ **Developer Experience**: Pipeline feedback loop < 15 menit, local setup < 30 menit

---

## 2. Tujuan dan Sasaran

### 2.1 Tujuan Bisnis

- Mengurangi risiko insiden keamanan di environment production sebesar 80% dalam 12 bulan pertama
- Memastikan audit trail yang lengkap untuk semua perubahan di setiap namespace (retention 12+ bulan)
- Memenuhi persyaratan kepatuhan **PCI-DSS v4.0**, ISO 27001, dan regulasi Bank Indonesia untuk platform pembayaran
- Mempercepat siklus deployment dengan automated security gate yang tidak memerlukan review manual berulang
- Mencapai target MTTR (Mean Time to Recover) < 30 menit untuk insiden keamanan di production
- Mengoptimalkan TCO (Total Cost of Ownership) dengan stack 100% open-source

### 2.2 Tujuan Teknis

| Tujuan                       | Implementasi                                                                           |
| ---------------------------- | -------------------------------------------------------------------------------------- |
| **Shift-Left Security**      | SAST/SCA di pre-commit & CI, DAST di early staging                                     |
| **Zero-Trust Network**       | mTLS via OSSM/Istio, NetworkPolicy deny-by-default, AuthorizationPolicy explicit allow |
| **Immutable Infrastructure** | GitOps dengan ArgoCD, drift detection aktif, no manual kubectl exec di prod            |
| **Supply Chain Security**    | Cosign keyless signing + Rekor transparency log, SBOM (Syft) wajib di-attach ke image  |
| **Continuous Compliance**    | ComplianceOperator (CIS Benchmark), Wazuh (PCI-DSS dashboard), ACS policy engine       |
| **API Security First**       | Schemathesis fuzzing + Dredd contract testing + OWASP DSOP validation                  |
| **Chaos Engineering**        | LitmusChaos (app-level) di SIT, Kraken (infra-level) di Pre-Prod                       |
| **Observability Native**     | LokiStack + Grafana (OCP default), Wazuh untuk SIEM/compliance                         |

### 2.3 KPI Keberhasilan

| KPI                              | Baseline | Target 6 Bulan | Target 12 Bulan |
| -------------------------------- | -------- | -------------- | --------------- |
| Critical vuln di prod            | Unknown  | < 5 open       | 0 open          |
| MTTR insiden keamanan            | > 4 jam  | < 1 jam        | < 30 menit      |
| Pipeline coverage                | ~40%     | 80%            | 100% workloads  |
| OWASP Web + API compliance score | ~50%     | 85%            | > 95%           |
| Image signing adoption           | 0%       | 80%            | 100%            |
| Policy-as-code coverage          | ~20%     | 70%            | 100%            |
| **Pipeline feedback loop**       | -        | < 20 menit     | < 15 menit      |
| **Local dev setup time**         | -        | < 45 menit     | < 30 menit      |

---

## 3. Namespace Strategy & Promotion Gate

Setiap namespace merepresentasikan environment yang terisolasi dengan kebijakan keamanan berbeda. Promosi antar namespace hanya dapat dilakukan melalui GitOps workflow dan memerlukan gate yang lolos secara otomatis maupun manual sesuai tingkat risiko environment.

### 3.1 Namespace Matrix

| Namespace      | Environment             | Deployment Mode       | Approval Gate                          | RBAC                   | Network Policy      | Chaos Strategy                                                                    |
| -------------- | ----------------------- | --------------------- | -------------------------------------- | ---------------------- | ------------------- | --------------------------------------------------------------------------------- |
| `payu-dev`     | Development             | Auto (CI green)       | Pipeline pass                          | Dev + SRE              | Permissive internal | None                                                                              |
| `payu-dev-*`   | **Preview/Ephemeral**   | Auto per PR           | Pipeline pass + PR label               | Dev + QA               | Isolated per branch | None _(opsional: Litmus light)_                                                   |
| `payu-sit`     | System Integration Test | Auto + security gate  | ACS policy pass + no critical CVE      | QA + Dev + SRE         | Restricted ingress  | **LitmusChaos** (app-level: pod kill, network latency, disk fill)                 |
| `payu-uat`     | User Acceptance Test    | Semi-auto             | Manual PO/QA + ACS + Schemathesis pass | QA + PM + SRE          | Strict — UAT only   | None                                                                              |
| `payu-preprod` | Pre-Production          | Manual trigger        | Pen test + CAB + **Kraken chaos pass** | SRE + Security         | Mirror production   | **Kraken + Cerberus** (infra-level: etcd kill, node crash, API server disruption) |
| `payu` (prod)  | Production              | Blue/Green via ArgoCD | CAB + CISO sign-off + health check     | SRE only (break-glass) | Zero-trust strict   | None _(red team exercise quarterly)_                                              |

> 💡 **Preview Environment**: Namespace `payu-dev-{branch-name}` di-spin up otomatis via ArgoCD ApplicationSet saat PR dibuat, dan di-destroy otomatis saat PR di-merge/close. Memungkinkan QA fitur sebelum masuk `payu-dev` utama.

### 3.2 Promotion Rules

> **Aturan emas:** Promote by **image digest**, bukan by tag. Setiap image harus ditandatangani dengan Cosign sebelum masuk namespace manapun.

1. **payu-dev** — Triggered otomatis setiap merge ke branch `develop`. Tidak memerlukan approval manual.
2. **payu-dev-{branch}** — Triggered otomatis saat PR dibuat ke `develop`. Namespace otomatis di-cleanup saat PR closed.
3. **payu-sit** — Triggered otomatis setelah payu-dev sukses. Memerlukan: (a) ACS policy pass (no critical CVE), (b) LitmusChaos experiment pass (app resiliency).
4. **payu-uat** — Memerlukan approval manual dari Product Owner dan QA Lead via ArgoCD sync gate + Schemathesis API fuzzing pass.
5. **payu-preprod** — Memerlukan: (a) Hasil pen test internal, (b) Kraken chaos experiment pass dengan Cerberus go/no-go signal, (c) CAB approval.
6. **payu (prod)** — Memerlukan: (a) CISO sign-off, (b) CAB approval, (c) window maintenance terdefinisi, (d) rollback otomatis jika health check gagal dalam 5 menit.

### 3.3 Emergency / Hotfix Workflow

> ⚡ **Critical Path untuk Production Incident**

```mermaid
graph LR
    A[Critical Bug Detected] --> B[Create hotfix/{ticket-id} branch]
    B --> C[CI Pipeline: SAST+SCA+Image Scan]
    C --> D[Deploy to payu-sit: ACS + Litmus smoke test]
    D --> E[Emergency Break-glass Bypass / Chatops]
    E --> F[Deploy to payu-preprod: Kraken chaos smoke + Cerberus]
    F --> G[Deploy to payu: Blue/Green with auto-rollback]
    G --> H[Post-Deployment: Full security review dalam 24 jam]
    H --> I{Review Pass?}
    I -->|Ya| J[Hotfix merged to main, normal workflow resumed]
    I -->|Tidak| K[Rollback + hotfix branch quarantined]
```

**Rules Hotfix:**

- Bypass manual UAT approval, tapi **tidak bypass** security scanning (SAST/SCA/Image Scan)
- Wajib deploy ke `payu-sit` minimal 15 menit untuk smoke test + LitmusChaos basic experiment
- Dalam skala lab, formal CAB approval dipercepat/di-bypass dengan mekanisme *glass-break* auto-approval atau persetujuan instan via Chatops (Teams/Slack bot).
- Post-deployment security review wajib dalam 24 jam; jika gagal, hotfix di-rollback dan branch di-quarantine
- Semua hotfix activity di-audit terpisah dan dilaporkan ke CISO dalam 48 jam

---

## 4. Pipeline Stages — Kebutuhan Detail

### Stage 1 — Source & Commit Security

**OWASP Coverage:** A05 (Injection) · A02 (Security Misconfig) · A03 (Software Supply Chain Failures) · A08 (Integrity) · A09 (Logging & Alerting) · **API1:2023 (Broken Object Level Authorization)**

#### 4.1.1 Pre-Commit Hooks (Recommended, Not Blocking)

> 🎯 **DevEx Principle**: Pre-commit hooks bersifat _recommended_ untuk developer experience. **CI/CD pipeline adalah enforcer sesungguhnya** untuk menghindari `--no-verify` bypass.

| Hook               | Tool                                                                                 | Language     | Purpose                              |
| ------------------ | ------------------------------------------------------------------------------------ | ------------ | ------------------------------------ |
| Secret Detection   | `detect-secrets` / `gitleaks`                                                        | All          | Scan hardcoded secrets & credentials |
| Commit Lint        | `commitlint`                                                                         | All          | Enforce conventional commit format   |
| Code Lint Security | `eslint-plugin-security` (JS/TS), `bandit` (Python), `gosec` (Go), `spotbugs` (Java) | Per-language | Static security linting              |
| License Check      | `license-checker` / `FOSSA CLI`                                                      | All          | Detect license compliance issues     |

> ✅ **CI Enforcement**: Semua check di atas **wajib dijalankan ulang di Tekton pipeline**. Pre-commit hanya untuk feedback cepat developer.

#### 4.1.2 Secret Scanning (CI Stage — Mandatory)

| Tool                   | Tipe        | Lisensi    | Integrasi                     | Rekomendasi                           |
| ---------------------- | ----------- | ---------- | ----------------------------- | ------------------------------------- |
| **Gitleaks**           | Open Source | MIT        | Tekton task + pre-commit hook | ✅ **Wajib** — fast, accurate, low FP |
| **Trufflehog v3**      | Open Source | AGPL-3.0   | Tekton task, scan git history | ✅ **Wajib** — deep history scanning  |
| **IBM Detect Secrets** | Open Source | Apache 2.0 | CLI + CI plugin               | ⚙️ Alternatif enterprise-friendly     |

#### 4.1.3 SAST (Static Application Security Testing)

| Tool                       | Tipe        | Bahasa      | OWASP Coverage              | Rekomendasi                                          |
| -------------------------- | ----------- | ----------- | --------------------------- | ---------------------------------------------------- |
| **Semgrep OSS**            | Open Source | 30+         | A02, A05, A06, A10, API1-10 | ✅ **Utama** — fast, rule-based, easy custom rules   |
| **SonarQube CE**           | Open Source | 27+         | A03, A05, A06, Code Quality | ✅ **Komplemen** — quality gate + tech debt tracking |
| **Bandit**                 | Open Source | Python only | A02, A05                    | ✅ Wajib untuk service Python                        |
| **Gosec**                  | Open Source | Go only     | A02, A05, A08               | ✅ Wajib untuk service Go                            |
| **SpotBugs + FindSecBugs** | Open Source | Java/Kotlin | A03, A05, A06               | ✅ Wajib untuk service Java                          |

#### 4.1.4 SCA & SBOM (Software Composition Analysis)

- **Syft** — generate SBOM (CycloneDX/SPDX format) untuk setiap image; artifact wajib di-attach ke image manifest
- **Grype** — scan SBOM terhadap CVE database (NVD, GitHub Advisory, OSV); fail pipeline jika critical CVE ditemukan
- **OWASP Dependency-Check** — alternatif/pendamping untuk deteksi vulnerability di dependency tree
- **Renovate Bot** — automated dependency update PR dengan security advisory filtering

> 📦 **SBOM Policy**: Setiap image yang masuk registry wajib memiliki SBOM valid. SBOM digunakan untuk: (1) vulnerability tracing, (2) license compliance audit, (3) supply chain attestation (SLSA).

---

### Stage 2 — Build & Image Security

**OWASP Coverage:** A03 (Software Supply Chain Failures) · A06 (Insecure Design) · A08 (Integrity Failures) · **SLSA L2+**

#### 4.2.1 Build Requirements

- Semua build menggunakan **Buildah** atau **Source-to-Image (s2i)** di dalam Tekton Pipeline (no Docker-in-Docker)
- Base image harus dari daftar **approved images** di internal registry (Quay.io atau OpenShift Registry)
- Wajib menggunakan **UBI minimal** atau **distroless image** untuk mengurangi attack surface
- Build berjalan dalam **unprivileged mode** — tidak ada root container saat build
- Reproducible builds: `BUILD_DATE`, `GIT_SHA`, `BUILDER_ID` tertanam sebagai image label
- **Hermetic builds** (target SLSA L3): isolasi network saat build, dependency dari cache terverifikasi

#### 4.2.2 Image Scanning

| Tool               | Tipe          | Scan Coverage                     | Integrasi                                  | Rekomendasi                                              |
| ------------------ | ------------- | --------------------------------- | ------------------------------------------ | -------------------------------------------------------- |
| **Trivy**          | Open Source   | CVE, misconfig, secret, SBOM, IaC | Tekton task, registry webhook              | ✅ **Wajib** — all-in-one, fast, OCP-friendly            |
| **Grype**          | Open Source   | CVE, SBOM matching                | CLI + CI/CD, Anchore Enterprise compatible | ✅ **Komplemen** — deep SBOM analysis                    |
| **ACS / StackRox** | Berbayar (RH) | CVE, policy, runtime, compliance  | OCP native operator                        | ✅ **Sudah terpakai** — admission controller + dashboard |

#### 4.2.3 Image Signing (Wajib)

- **Cosign (Sigstore)** — sign setiap image dengan **keyless signing via OIDC provider** (GitHub Actions, OpenShift OAuth)
- Signature harus di-verify di **admission controller** (ACS/Kyverno) sebelum pod dapat berjalan di namespace manapun
- Implementasi **Rekor transparency log** untuk audit trail signing yang tidak dapat dimanipulasi
- **Opsional Enterprise**: Untuk environment dengan requirement HSM, Cosign mendukung key-based signing dengan Vault Transit atau AWS KMS

> 🔐 **Admission Policy**: ACS policy / Kyverno policy wajib memblokir image tanpa valid Cosign signature dari trusted issuer (`https://oauth-openshift.apps.<cluster>`).

---

### Stage 3 — Test (payu-dev, payu-dev-\*, payu-sit)

**OWASP Coverage:** A01 (Access Control) · A04 (Crypto) · A05 (Injection) · A07 (Auth) · A10 (Mishandling Except.) · **API Security Top 10**

#### 4.3.1 DAST (Dynamic Application Security Testing)

| Tool                     | Tipe        | Mode                              | OWASP Coverage                           | Rekomendasi                                              |
| ------------------------ | ----------- | --------------------------------- | ---------------------------------------- | -------------------------------------------------------- |
| **OWASP ZAP (Headless)** | Open Source | Active + Passive scan             | A01, A02, A05, A07, A10, API1-10         | ✅ **Wajib** — baseline DAST otomatis di payu-dev        |
| **Nuclei**               | Open Source | Template-based vulnerability scan | A02, A03, A07, exposed panels, misconfig | ✅ **Wajib** — fast scanning untuk known CVE & misconfig |
| **Schemathesis**         | Open Source | Schema-based API fuzzing          | API1, API2, API3, API5, API7             | ✅ **Wajib** — automatic fuzzing dari OpenAPI spec       |

- ZAP dijalankan dalam mode headless di Tekton task setiap deploy ke `payu-dev`
- Scan report di-parse untuk quality gate: **tidak ada high/critical finding** boleh lolos ke `payu-sit`
- Schemathesis dijalankan di `payu-sit` untuk API-heavy services; fail pipeline jika ditemukan crash atau unauthorized access

#### 4.3.2 API Security Testing

- **Schemathesis** — automatic API fuzzing berbasis OpenAPI spec untuk menemukan crash, injection, dan broken access control
- **OWASP DSOP (API Security)** — test API authentication, authorization, rate limiting, input validation, dan mass assignment
- **Dredd** — contract testing antara API spec dan implementasi aktual (memastikan tidak ada breaking changes atau spec drift)

#### 4.3.3 Performance & Chaos Testing

- **k6** — load testing wajib sebelum promote ke `payu-uat` (minimum 1000 req/s, P95 < 500ms, error rate < 0.1%)
- **LitmusChaos** — digunakan di `payu-sit` untuk **application-level chaos**:
  - Pod delete, container kill, network latency/delay antar service, disk fill, CPU/memory stress
  - Menggunakan CRD-based workflow (`ChaosExperiment` + `ChaosEngine`) yang terintegrasi dengan ArgoCD/Tekton
  - Resiliency probes untuk validasi auto-recovery sebelum experiment dianggap pass
- **Kraken** — digunakan di `payu-preprod` untuk **infrastructure-level chaos** spesifik OpenShift:
  - Kill etcd pod, disrupt openshift-apiserver, node crash/reboot, cluster network partition
  - Terintegrasi dengan **Cerberus** sebagai cluster health guardian: memberikan sinyal _go/no-go_ apakah cluster sudah recover sebelum lanjut ke skenario chaos berikutnya
- _(Chaos Mesh dihapus untuk menghindari redundansi dengan LitmusChaos)_

---

### Stage 4 — Deploy & Policy Gate

**OWASP Coverage:** A02 (Security Misconfig) · A08 (Software Integrity) · GitOps drift prevention

#### 4.4.1 GitOps Requirements

- Semua konfigurasi deployment disimpan di Git (infrastructure-as-code) dengan **branch protection** dan **required reviews**
- **ArgoCD** — single source of truth untuk state deployment di semua namespace
- **ArgoCD Image Updater** — mekanisme otomatis untuk promote image digest antar environment via Git write-back
- Drift detection aktif — ArgoCD alert dan auto-sync jika terjadi drift dari Git (dengan approval gate untuk production)
- App-of-Apps pattern untuk manajemen multi-namespace yang terstandardisasi
- **ApplicationSet** untuk generate Application per namespace (termasuk preview environment `payu-dev-*`) secara otomatis

#### 4.4.2 Policy-as-Code (Admission Controller)

> ⚠️ **Avoid Overlap (Policy Boundary)**: Pemisahan ruang lingkup WAJIB dilakukan secara mutlak. Gunakan **ACS Admission** KHUSUS sebagai gatekeeper keamanan (verifikasi Cosign signature, blokir container root, CVE threshold). Gunakan **Kyverno** KHUSUS untuk operational/mutasi K8s (otomatis *inject* label, force `ReadOnlyRootFilesystem`, men-generate *default-deny NetworkPolicy*). Jangan menggunakan Kyverno dan ACS bersamaan untuk tumpang tindih rules yang sama demi menjaga performa cluster.

| Tool                 | Tipe          | Bahasa Policy    | Use Case                                                                  | Rekomendasi                                  |
| -------------------- | ------------- | ---------------- | ------------------------------------------------------------------------- | -------------------------------------------- |
| **ACS Admission**    | Berbayar (RH) | GUI + API + YAML | Security policy: image signature, CVE threshold, runtime behavior         | ✅ **Primary** — sudah terintegrasi OCP      |
| **Kyverno**          | Open Source   | YAML/JSON        | Operational policy: auto-add labels, resource limits, namespace lifecycle | ✅ **Komplemen** — mudah ditulis, native K8s |
| **OPA / Gatekeeper** | Open Source   | Rego             | Complex logic policy yang tidak bisa di-handle Kyverno                    | ⚙️ Opsional — untuk advanced use case        |

**Policy wajib yang harus diimplementasikan:**

| Policy                     | Tool        | Description                                                    |
| -------------------------- | ----------- | -------------------------------------------------------------- |
| No root container          | ACS/Kyverno | Blokir container dengan `runAsUser: 0` atau `privileged: true` |
| Approved registry only     | ACS         | Blokir image dari registry yang tidak di-allowlist             |
| Image signature required   | ACS         | Blokir image tanpa valid Cosign signature                      |
| Resource requests/limits   | Kyverno     | Wajib definisi CPU/memory request & limit                      |
| ReadOnly root filesystem   | Kyverno     | Set `readOnlyRootFilesystem: true` untuk semua container       |
| No host namespace          | ACS/Kyverno | Blokir `hostNetwork`, `hostPID`, `hostIPC`                     |
| NetworkPolicy default deny | Kyverno     | Auto-generate default-deny NetworkPolicy per namespace         |

---

### Stage 5 — Runtime Security

**OWASP Coverage:** A04 (Crypto via mTLS) · A07 (Auth via RBAC) · A09 (Logging & Alerting via Falco) · Zero-trust enforcement

#### 4.5.1 Cloud Workload Protection Platform (CWPP)

> ⚠️ **OVN-Kubernetes & Kernel Conflict Avoidance**: Karena Red Hat OpenShift 4.20 secara default menggunakan **OVN-Kubernetes**, fungsionalitas Tetragon berpotensi rentan bentrokan tanpa Cilium. Fokus penuh gunakan **Falco** sebagai sole alerting engine berbasis eBPF/syscall.

| Tool                  | Tipe          | Deteksi                                               | Integrasi                      | Rekomendasi                                                    |
| --------------------- | ------------- | ----------------------------------------------------- | ------------------------------ | -------------------------------------------------------------- |
| **ACS / StackRox**    | Berbayar (RH) | Policy, CVE, runtime behavior, compliance             | OCP native operator            | ✅ **Sudah terpakai** — primary enforcement                    |
| **Falco**             | Open Source   | Syscall-level rules, cloud-native threat detection    | DaemonSet + Prometheus metrics | ✅ **Wajib** — primary runtime alerting engine                 |

#### 4.5.2 Service Mesh & mTLS

- **OpenShift Service Mesh (OSSM/Istio)** — mTLS mandatory antar semua service di `payu-uat` ke atas
- `PeerAuthentication: STRICT` mode di namespace `payu-uat`, `payu-preprod`, `payu`
- `AuthorizationPolicy` — explicit allow-listing, **deny by default** untuk semua ingress/egress
- Circuit breaker, retry policy, dan timeout dikonfigurasi via Istio VirtualService + DestinationRule
- **Observability**: Kiali untuk service graph, Jaeger untuk distributed tracing

#### 4.5.3 Secrets Management

| Tool                                    | Tipe                 | Dynamic Secrets   | Auto-Rotate             | Rekomendasi                                  |
| --------------------------------------- | -------------------- | ----------------- | ----------------------- | -------------------------------------------- |
| **HashiCorp Vault OSS**                 | Open Source (BSL)    | Ya                | Manual config + CronJob | ✅ **Utama** — self-hosted, feature-complete |
| **External Secrets Operator**           | Open Source (Apache) | Bridge only       | Via Vault backend       | ✅ **Wajib** sebagai bridge K8s ↔ Vault      |
| **Vault Agent Injector / CSI Provider** | Open Source          | Sidecar/CSI mount | Via Vault               | ✅ Untuk secret injection ke pod             |

- Tidak ada secret yang boleh disimpan sebagai environment variable langsung di pod spec
- Semua secret di-inject via **External Secrets Operator** dari Vault
- Secret rotation otomatis setiap **30 hari** untuk kredensial database dan API key (via Vault TTL + CronJob)
- **Zero-Downtime Rotation**: Aplikasi backend (Koneksi Pool HikariCP Spring Boot atau Hibernate ORM Quarkus) wajib dikonfigurasi untuk _hot-reload_ agar memuat kredensial baru secara dinamis usai rotasi Vault, tanpa perlu restart pod.
- Audit log Vault di-forward ke Wazuh untuk compliance monitoring

---

### Stage 6 — Observability & Compliance

**OWASP Coverage:** A09 (Security Logging and Alerting Failures) — stage paling sering diabaikan namun kritikal

#### 4.6.1 Logging & SIEM

| Tool                    | Tipe        | Use Case                                                   | Rekomendasi                                                  |
| ----------------------- | ----------- | ---------------------------------------------------------- | ------------------------------------------------------------ |
| **LokiStack + Grafana** | Open Source | Log aggregation, cost-effective, OCP native                | ✅ **Utama** untuk K8s application logs                      |
| **OpenSearch**          | Open Source | Full-text search, complex query, dashboard                 | ✅ **Komplemen** untuk log retention jangka panjang          |
| **Wazuh**               | Open Source | SIEM, XDR, file integrity monitoring, compliance dashboard | ✅ **Wajib** untuk PCI-DSS/NIST reporting & threat detection |

- **Deployment SIEM**: Wazuh Manager & Indexer akan dikonfigurasi berjalan secara mandiri di dalam klaster OpenShift via Helm, mendukung _isolated lab scalability_. 
- **Rule Management**: Demi efisiensi tim dalam operasional policy (Wazuh, Falco, ACS), platform memprioritaskan hanya menggunakan rule *native/predefined Red Hat*. Apabila *ruleset Red Hat* tidak relevan/tersedia, maka opsi fallback adalah _default rule template_ bawaan komunitas *open-source* (OSS). Pendekatan *highly customized toolsets* akan dihindari sejauh mungkin.
- Semua audit log (`kubectl exec`, API server, policy violation, admission reject) harus dikirim ke Wazuh
- Log retention minimum **12 bulan** untuk compliance PCI-DSS dan Bank Indonesia
- Alert wajib dikonfigurasi untuk: privilege escalation, policy violation, CVE kritis baru, anomali runtime behavior

#### 4.6.2 Monitoring & Alerting

- **Prometheus + Alertmanager** — built-in OCP, wajib untuk metrics platform dan application SLO
- **Grafana** — dashboard security posture, pipeline health, namespace resource usage, chaos experiment result
- Custom alert: SLO breach, error rate > 5%, latency P99 > 2s, Falco critical alert, ACS policy violation
- **k6 + Grafana** — performance metrics dashboard untuk capacity planning

#### 4.6.3 Continuous Compliance

- **ComplianceOperator** — scan CIS Kubernetes Benchmark dan NIST SP 800-53 secara terjadwal; hasil di-forward ke Wazuh
- **ACS Compliance** — dashboard compliance per namespace dengan remediation guidance
- **OpenSCAP** — scan OS-level compliance di node OpenShift
- **Wazuh Compliance Module** — built-in dashboard untuk PCI-DSS v4.0, ISO 27001, NIST 800-53
- Report compliance digenerate otomatis mingguan dan dikirim ke CISO + Security Team

---

## 5. OWASP Top 10 & API Security Compliance Matrix

### 5.1 OWASP Web Top 10 2025

| OWASP ID | Kategori                                 | Stage Mitigasi  | Tool Utama                                                               | Status    |
| -------- | ---------------------------------------- | --------------- | ------------------------------------------------------------------------ | --------- |
| A01:2025 | Broken Access Control                    | Stage 3 + 4 + 5 | ZAP + Kyverno + ACS RBAC + OSSM AuthPolicy                               | **Wajib** |
| A02:2025 | Security Misconfiguration                | Stage 1 + 4 + 6 | Semgrep + Kyverno + ComplianceOperator + Wazuh FIM                       | **Wajib** |
| A03:2025 | Software Supply Chain Failures           | Stage 1 + 2     | Grype + Trivy + Syft SBOM + Renovate Bot                                 | **Wajib** |
| A04:2025 | Cryptographic Failures                   | Stage 2 + 5     | Cosign + Vault + mTLS OSSM + TLS 1.3 enforcement                         | **Wajib** |
| A05:2025 | Injection                                | Stage 1 + 3     | Semgrep + SonarQube + ZAP DAST + parameterized query enforcement         | **Wajib** |
| A06:2025 | Insecure Design                          | Stage 1 + 2     | Threat model template + distroless base + secure coding guideline        | **Wajib** |
| A07:2025 | Authentication Failures                  | Stage 3 + 5     | Schemathesis + mTLS + RBAC + MFA enforcement via OAuth                   | **Wajib** |
| A08:2025 | Software or Data Integrity Failures      | Stage 1 + 2 + 4 | Signed commit + Cosign + Admission controller + SBOM attestation         | **Wajib** |
| A09:2025 | Security Logging and Alerting Failures   | Stage 6         | Loki + Wazuh + Falco + SIEM correlation rules                            | **Wajib** |
| A10:2025 | Mishandling of Exceptional Conditions    | Stage 1 + 3     | Semgrep + SonarQube + ZAP DAST error detection                           | **Wajib** |

### 5.2 OWASP API Security Top 10 2023 _(Payu: API-Heavy Platform)_

| API ID     | Kategori                                        | Stage Mitigasi  | Tool Utama                                                            | Status    |
| ---------- | ----------------------------------------------- | --------------- | --------------------------------------------------------------------- | --------- |
| API1:2023  | Broken Object Level Authorization               | Stage 3 + 5     | Schemathesis fuzzing + OSSM AuthorizationPolicy + ZAP auth test       | **Wajib** |
| API2:2023  | Broken Authentication                           | Stage 3 + 5     | OAuth2/OIDC enforcement + mTLS + rate limiting via Istio              | **Wajib** |
| API3:2023  | Broken Object Property Level Authorization      | Stage 3         | Schemathesis + custom ZAP script + API schema validation              | **Wajib** |
| API4:2023  | Unrestricted Resource Consumption               | Stage 3 + 4     | Rate limiting + resource quota + k6 load test gate                    | **Wajib** |
| API5:2023  | Broken Function Level Authorization             | Stage 3 + 5     | RBAC + OSSM AuthorizationPolicy + ZAP authz test                      | **Wajib** |
| API6:2023  | Unrestricted Access to Sensitive Business Flows | Stage 3 + 4     | Business logic test + anomaly detection via Wazuh                     | **Wajib** |
| API7:2023  | Server Side Request Forgery                     | Stage 3 + 5     | ZAP SSRF test + NetworkPolicy egress control + OSSM                   | **Wajib** |
| API8:2023  | Security Misconfiguration                       | Stage 1 + 4 + 6 | Semgrep + Kyverno + ComplianceOperator                                | **Wajib** |
| API9:2023  | Improper Inventory Management                   | Stage 1 + 2     | SBOM (Syft) + SCA (Grype) + API versioning policy                     | **Wajib** |
| API10:2023 | Unsafe Consumption of APIs                      | Stage 1 + 2     | SCA for external API client + schema validation + timeout enforcement | **Wajib** |

---

## 6. Tool Stack — 100% Open Source

### 6.1 SAST & Code Quality

| Tool                       | Lisensi                  | Bahasa      | False Positive | Verdict                                              |
| -------------------------- | ------------------------ | ----------- | -------------- | ---------------------------------------------------- |
| **Semgrep OSS**            | Open Source (LGPL-2.1)   | 30+         | Rendah         | ✅ Rekomendasi utama — rule-based, fast, easy custom |
| **SonarQube CE**           | Open Source (LGPL-3.0)   | 27+         | Sedang         | ✅ Komplemen — quality gate + tech debt tracking     |
| **Bandit**                 | Open Source (Apache-2.0) | Python only | Rendah         | ✅ Wajib untuk service Python                        |
| **Gosec**                  | Open Source (Apache-2.0) | Go only     | Rendah         | ✅ Wajib untuk service Go                            |
| **SpotBugs + FindSecBugs** | Open Source (LGPL)       | Java/Kotlin | Sedang         | ✅ Wajib untuk service Java                          |

### 6.2 DAST & API Security

| Tool             | Lisensi                  | Mode                              | API Support                | Verdict                                                           |
| ---------------- | ------------------------ | --------------------------------- | -------------------------- | ----------------------------------------------------------------- |
| **OWASP ZAP**    | Open Source (Apache-2.0) | Active + Passive scan             | REST/GraphQL/gRPC          | ✅ Rekomendasi utama — mature, extensible                         |
| **Nuclei**       | Open Source (MIT)        | Template-based vulnerability scan | HTTP/REST/gRPC             | ✅ Komplemen — fast scanning for known CVE/misconfig              |
| **Schemathesis** | Open Source (MIT)        | Schema-based API fuzzing          | OpenAPI 3.x, GraphQL       | ✅ Wajib untuk API-heavy services — automatic edge-case discovery |
| **Dredd**        | Open Source (BSD-3)      | Contract testing                  | OpenAPI 2/3, API Blueprint | ✅ Wajib — ensure spec-implementation consistency                 |

### 6.3 Container & Runtime Security

| Tool                  | Lisensi                            | Scope                           | OCP Integration                 | Verdict                                                   |
| --------------------- | ---------------------------------- | ------------------------------- | ------------------------------- | --------------------------------------------------------- |
| **Trivy**             | Open Source (Apache-2.0)           | Image + IaC + SBOM + secret     | Tekton task, registry webhook   | ✅ Wajib di pipeline — all-in-one scanner                 |
| **Grype**             | Open Source (Apache-2.0)           | CVE + SBOM matching             | CLI + CI/CD, Anchore compatible | ✅ Komplemen — deep SBOM analysis                         |
| **Falco**             | Open Source (Apache-2.0)           | Syscall-level runtime detection | DaemonSet + Prometheus          | ✅ Wajib — primary runtime alerting engine                |
| **ACS / StackRox**    | Berbayar (RH, included in OCP sub) | Full lifecycle + compliance     | OCP native operator             | ✅ Sudah terpakai — primary enforcement & dashboard       |

### 6.4 Policy & GitOps

| Tool                       | Lisensi                  | Bahasa Policy       | Use Case                                               | Verdict                               |
| -------------------------- | ------------------------ | ------------------- | ------------------------------------------------------ | ------------------------------------- |
| **Kyverno**                | Open Source (Apache-2.0) | YAML/JSON           | Operational K8s policy (auto-label, quota, lifecycle)  | ✅ Utama — native K8s, mudah ditulis  |
| **OPA / Gatekeeper**       | Open Source (Apache-2.0) | Rego                | Complex logic policy yang tidak bisa di-handle Kyverno | ⚙️ Opsional — untuk advanced use case |
| **ArgoCD + Image Updater** | Open Source (Apache-2.0) | YAML/Helm/Kustomize | GitOps deployment + image digest promotion             | ✅ Wajib — single source of truth     |

### 6.5 Secrets Management

| Tool                           | Lisensi                  | Dynamic Secrets   | Auto-Rotate             | Verdict                                              |
| ------------------------------ | ------------------------ | ----------------- | ----------------------- | ---------------------------------------------------- |
| **HashiCorp Vault OSS**        | Open Source (BSL-1.1)    | Ya                | Manual config + CronJob | ✅ Rekomendasi utama — self-hosted, feature-complete |
| **External Secrets Operator**  | Open Source (Apache-2.0) | Bridge only       | Via Vault backend       | ✅ Wajib sebagai bridge K8s ↔ Vault                  |
| **Vault Agent Injector / CSI** | Open Source              | Sidecar/CSI mount | Via Vault TTL           | ✅ Untuk secret injection ke pod                     |

### 6.6 Observability & Compliance

| Tool                          | Lisensi                             | Use Case                                            | Verdict                                            |
| ----------------------------- | ----------------------------------- | --------------------------------------------------- | -------------------------------------------------- |
| **LokiStack + Grafana**       | Open Source (AGPL-3.0 / Apache-2.0) | Log aggregation + visualization                     | ✅ Utama — OCP native, cost-effective              |
| **OpenSearch**                | Open Source (Apache-2.0)            | Full-text search + long-term retention              | ✅ Komplemen — powerful dashboard & query          |
| **Wazuh**                     | Open Source (GPL-2.0)               | SIEM, XDR, FIM, compliance dashboard (PCI-DSS/NIST) | ✅ Wajib — game changer untuk compliance reporting |
| **Prometheus + Alertmanager** | Open Source (Apache-2.0)            | Metrics + alerting                                  | ✅ Wajib — built-in OCP                            |
| **ComplianceOperator**        | Open Source (Apache-2.0)            | CIS Benchmark + NIST scan                           | ✅ Wajib — continuous compliance scanning          |

### 6.7 Chaos Engineering

| Tool            | Lisensi                  | Target                                              | OCP-Aware           | Verdict                                                                |
| --------------- | ------------------------ | --------------------------------------------------- | ------------------- | ---------------------------------------------------------------------- |
| **LitmusChaos** | Open Source (Apache-2.0) | App-level chaos (pod, network, disk, CPU)           | Via CRD + Operator  | ✅ Utama di `payu-sit` — mature, CNCF, ChaosHub marketplace            |
| **Kraken**      | Open Source (Apache-2.0) | Infra-level chaos (etcd, API server, node, cluster) | ✅ Native OCP-aware | ✅ Utama di `payu-preprod` — Red Hat project, Cerberus integration     |
| **Cerberus**    | Open Source (Apache-2.0) | Cluster health guardian (go/no-go signal)           | ✅ Native OCP       | ✅ Wajib pendamping Kraken — ensure cluster recovery before next chaos |

> ❌ **Chaos Mesh dihapus** untuk menghindari redundansi dengan LitmusChaos (fitur app-level overlap) dan kompleksitas operasional.

---

## 7. Implementation Roadmap

### Phase 1 — Foundation (Bulan 1–2)

> Priority: wajib diselesaikan sebelum phase berikutnya.

- [ ] Implementasi Gitleaks + Trufflehog di Tekton pipeline (CI enforcement)
- [ ] Integrasi Semgrep OSS dan SonarQube CE ke pipeline Tekton yang ada
- [ ] Setup Cosign + Sigstore (keyless OIDC) untuk image signing di semua build pipeline
- [ ] Deploy HashiCorp Vault OSS + External Secrets Operator di cluster
- [ ] Konfigurasi Kyverno baseline policies (no-root, resource limits, approved registry, default-deny NetworkPolicy)
- [ ] Aktifkan ACS admission controller untuk enforce image signature policy
- [ ] Setup LokiStack + Grafana untuk log aggregation dasar

### Phase 2 — Hardening (Bulan 3–4)

- [ ] Integrasi OWASP ZAP headless + Schemathesis ke Tekton task untuk setiap deploy ke `payu-dev`
- [ ] Setup Falco di semua node sebagai supplement ACS/StackRox (runtime alerting)
- [ ] Implementasi OSSM (Istio) dengan `PeerAuthentication: STRICT` di `payu-uat` ke atas
- [ ] Konfigurasi ComplianceOperator untuk CIS Kubernetes Benchmark scan + forward ke Wazuh
- [ ] Deploy Wazuh manager + agent untuk SIEM/compliance dashboard (PCI-DSS v4.0 ready)
- [ ] Migrasi semua secret dari env vars ke Vault + External Secrets Operator
- [ ] Setup ArgoCD Image Updater untuk automated image digest promotion via Git write-back

### Phase 3 — Optimization (Bulan 5–6)

- [ ] Integrasi LitmusChaos di `payu-sit` untuk app-level chaos engineering (CRD-based workflow)
- [ ] Integrasi Kraken + Cerberus di `payu-preprod` untuk infra-level chaos + cluster health validation
- [ ] Setup k6 load testing sebagai gate sebelum promote ke `payu-uat` (SLO-based)
- [ ] Implementasi full OWASP Web + API Top 10 test suite di pipeline DAST
- [ ] Automated compliance reporting ke CISO (weekly report via Wazuh + ComplianceOperator)
- [ ] Setup preview environment (`payu-dev-*`) via ArgoCD ApplicationSet + auto-cleanup

### Phase 4 — Continuous Improvement (Bulan 7+)

- [ ] Evaluasi dan tuning tool berdasarkan metrics, incident report, dan false positive rate
- [ ] Implementasi pen testing terjadwal di `payu-preprod` (quarterly) dengan report ke CAB
- [ ] Target SLSA Level 3 — hermetic builds, provenance attestation, build isolation
- [ ] Red team exercise tahunan untuk validasi end-to-end security posture
- [ ] Review dan update OWASP compliance matrix setiap 6 bulan
- [ ] Developer feedback loop: survey DevEx, optimasi pipeline speed, reduce friction

---

## 8. Non-Functional Requirements

### 8.1 Performance

| Metric                                           | Target                         | Rationale                       |
| ------------------------------------------------ | ------------------------------ | ------------------------------- |
| Total pipeline waktu `payu-dev` → `payu-sit`     | < **20 menit**                 | Developer feedback loop         |
| Security scan (SAST + SCA + image scan) overhead | < **5 menit** tambahan         | Parallelization + caching       |
| ACS admission controller response time           | < **2 detik** per pod creation | No deployment bottleneck        |
| Vault secret retrieval latency (P99)             | < **100ms**                    | Application startup performance |
| ArgoCD sync time (app-of-apps)                   | < **3 menit** untuk 50 apps    | Scalable GitOps workflow        |

### 8.2 Availability

| Component         | HA Configuration                                            | Target Uptime |
| ----------------- | ----------------------------------------------------------- | ------------- |
| ArgoCD            | 3 replica + Redis HA + S3 backend                           | **99.9%**     |
| Vault             | HA with Raft consensus, 3 node minimum, cross-AZ            | **99.95%**    |
| Tekton Controller | 2+ replica + persistent volume for task run                 | **99.9%**     |
| ACS Central       | HA mode (included in OCP sub), sensor per node              | **99.95%**    |
| Wazuh Manager     | Cluster mode (1 master + 2 workers) + Elasticsearch backend | **99.9%**     |
| LokiStack         | Distributor + Ingester + Querier HA + S3 storage            | **99.9%**     |

### 8.3 Scalability _(Realistic for Enterprise Mid-Scale)_

| Component                   | Target Capacity             | Notes                                              |
| --------------------------- | --------------------------- | -------------------------------------------------- |
| Pipeline concurrent builds  | **20–50**                   | Horizontal scaling via Tekton task parallelization |
| Vault secret requests       | **1.000 req/s**             | Scalable via Vault cluster + caching layer         |
| Log ingestion (Loki/Wazuh)  | **10.000 eps**              | Scalable via sharding + S3 cold storage            |
| ArgoCD managed applications | **200 apps**                | Via ApplicationSet + cluster sharding if needed    |
| Falco events processing     | **5.000 events/s per node** | Via Prometheus remote-write batching               |

> 📌 **Scalability Principle**: Design for horizontal scaling first. Vertical scaling (bigger nodes) hanya sebagai last resort.

### 8.4 Maintainability

- Semua konfigurasi pipeline (Tekton, ArgoCD, Kyverno, Falco rules) disimpan di Git dengan branch protection
- Setiap tool harus memiliki: (1) runbook operasional, (2) alert playbook, (3) upgrade procedure
- Upgrade tool harus dapat dilakukan tanpa downtime (rolling update + canary deployment untuk control plane)
- Dokumentasi harus diperbarui otomatis via Git commit hook atau CI job saat konfigurasi berubah
- **Chaos Experiment Catalog**: Semua experiment (Litmus/Kraken) didokumentasikan di Git dengan: tujuan, scope, expected outcome, rollback procedure

---

## 9. Risiko & Mitigasi

| Risiko                                              | Probabilitas | Dampak | Mitigasi                                                                                                 |
| --------------------------------------------------- | ------------ | ------ | -------------------------------------------------------------------------------------------------------- |
| False positive SAST memblokir pipeline              | Tinggi       | Sedang | Tuning rules per project + allowlist + SLA review 24 jam + developer feedback channel                    |
| Vault downtime memblokir deployment                 | Rendah       | Tinggi | Vault HA + break-glass procedure terdokumentasi + cached secret TTL 5 menit                              |
| Pipeline waktu terlalu lama (> 30 menit)            | Sedang       | Sedang | Parallelisasi scan, caching layer (Trivy/Semgrep), incremental scan untuk PR                             |
| Image signing adoption lambat                       | Sedang       | Tinggi | Policy enforcement bertahap (warn → block) + training tim + dokumentasi jelas                            |
| Chaos experiment menyebabkan outage tidak terduga   | Rendah       | Tinggi | Cerberus go/no-go signal + run di pre-prod dulu + rollback automation + change window                    |
| Compliance gap terdeteksi audit BI                  | Rendah       | Tinggi | ComplianceOperator weekly scan + Wazuh compliance dashboard + proactive remediation sprint               |
| Developer friction (too many gates)                 | Tinggi       | Sedang | DevEx KPI monitoring + feedback loop < 15 menit + pre-commit sebagai recommendation, CI sebagai enforcer |
| Tool overlap/conflict (Falco+Tetragon, Kyverno+ACS) | Sedang       | Sedang | Clear boundary definition + single-owner per policy domain + integration testing di SIT                  |

---

## 10. Developer Experience (DevEx)

> 🎯 **Prinsip**: Security tidak boleh mengorbankan developer velocity. Shift-left harus berarti "detect early", bukan "block often".

### 10.1 DevEx KPI

| KPI                                      | Target                                   | Measurement                                                     |
| ---------------------------------------- | ---------------------------------------- | --------------------------------------------------------------- |
| Local dev setup time (new project)       | < **30 menit**                           | Time from `git clone` to first successful local run             |
| Pipeline feedback loop (commit → result) | < **15 menit**                           | P95 latency dari push ke branch hingga pipeline status tersedia |
| False positive rate (SAST/DAST)          | < **5%**                                 | Ratio of blocked PRs that were false alarms                     |
| Pre-commit hook execution time           | < **10 detik**                           | Local execution time for recommended hooks                      |
| Documentation discoverability            | < **3 klik** dari repo README ke runbook | Navigation audit quarterly                                      |

### 10.2 DevEx Enablers

- **Pre-commit hooks bersifat recommended**, bukan blocking. CI pipeline adalah enforcer sesungguhnya.
- **Pipeline as Code**: Semua Tekton Task/Pipeline disimpan di Git; developer bisa preview pipeline config di PR.
- **Local Pipeline Simulation**: `tkn pipeline start --dry-run` atau `act` (GitHub Actions compatible) untuk testing pipeline logic lokal.
- **Security Findings Dashboard**: Grafana dashboard yang menampilkan: (1) open vulnerabilities per service, (2) pipeline success rate, (3) mean time to fix — visible untuk developer.
- **Automated Remediation PR**: Renovate Bot + Dependabot untuk dependency update; Semgrep auto-fix rules untuk common issues.
- **Chaos Experiment Catalog**: Developer bisa trigger LitmusChaos experiment di `payu-sit` via self-service portal (dengan quota & approval) untuk testing resiliensi.

### 10.3 Developer Onboarding

```bash
# Payu Platform Quickstart (target: < 30 menit)
$ git clone https://git.payu.internal/platform-starter
$ cd platform-starter
$ make bootstrap  # Setup local OCP context, Vault auth, ArgoCD access
$ make new-service NAME=my-api LANGUAGE=go  # Scaffold secure service template
$ make pipeline-test  # Run pipeline locally via Tekton CLI
$ make deploy-dev  # Deploy to payu-dev with auto-preview URL
```

Template service sudah include:

- Secure Dockerfile (UBI minimal, non-root user, read-only FS)
- Pre-configured Semgrep + k6 + ZAP tasks
- Vault integration via External Secrets Operator
- ArgoCD Application manifest ready-to-commit

---

## 11. Glosarium

| Istilah               | Definisi                                                                                                                                    |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| **ACS / StackRox**    | Advanced Cluster Security — platform keamanan container Red Hat yang terintegrasi ke OpenShift (termasuk dalam subscription OCP)            |
| **Cerberus**          | Cluster health guardian untuk chaos engineering; memberikan sinyal go/no-go apakah OpenShift cluster sudah recover setelah chaos experiment |
| **CWPP**              | Cloud Workload Protection Platform — kategori tool untuk melindungi workload di cloud dan container runtime                                 |
| **DAST**              | Dynamic Application Security Testing — pengujian keamanan aplikasi saat runtime dengan simulasi attack                                      |
| **GitOps**            | Praktik menggunakan Git sebagai single source of truth untuk state infrastruktur dan deployment; perubahan hanya via merge request          |
| **Kraken**            | Chaos engineering tool khusus OpenShift oleh Red Hat; fokus pada infra/control-plane chaos (etcd, API server, node)                         |
| **LitmusChaos**       | CNCF incubating project untuk chaos engineering Kubernetes-agnostic; fokus pada app-level chaos dengan CRD-based workflow                   |
| **mTLS**              | Mutual TLS — autentikasi dua arah antara client dan server menggunakan sertifikat X.509; mandatory di zero-trust architecture               |
| **OPA**               | Open Policy Agent — policy engine universal untuk Kubernetes dan sistem lainnya menggunakan bahasa Rego                                     |
| **RBAC**              | Role-Based Access Control — mekanisme kontrol akses berdasarkan peran pengguna; di-enforce via Kubernetes + OSSM                            |
| **SAST**              | Static Application Security Testing — analisis keamanan pada kode sumber tanpa eksekusi; shift-left detection                               |
| **SBOM**              | Software Bill of Materials — daftar komponen perangkat lunak yang digunakan oleh aplikasi; format CycloneDX/SPDX                            |
| **Schemathesis**      | Open-source API fuzzing tool berbasis OpenAPI spec; menemukan edge-case, crash, dan broken access control di API                            |
| **SLSA**              | Supply chain Levels for Software Artifacts — framework untuk keamanan supply chain perangkat lunak (Level 1-4)                              |
| **SCA**               | Software Composition Analysis — analisis komponen open source untuk kerentanan, lisensi, dan maintenance status                             |
| **Sigstore / Cosign** | Infrastruktur open-source untuk signing dan verifikasi artifact perangkat lunak secara transparan (keyless via OIDC)                        |
| **Wazuh**             | Open-source SIEM/XDR platform dengan built-in compliance dashboard untuk PCI-DSS, NIST, ISO 27001                                           |
| **Zero-trust**        | Model keamanan yang tidak mempercayai entitas manapun secara default — verifikasi selalu diperlukan, baik internal maupun eksternal         |

---

## Appendix A: Quick Reference — Policy Snippets

### Kyverno: Default Deny NetworkPolicy Auto-Generate

```yaml
apiVersion: kyverno.io/v1
kind: ClusterPolicy
metadata:
  name: generate-default-deny-networkpolicy
spec:
  rules:
    - name: generate-default-deny
      match:
        any:
          - resources:
              kinds: [Namespace]
              selector:
                matchLabels:
                  payu/environment: "sit,uat,preprod,prod"
      generate:
        apiVersion: networking.k8s.io/v1
        kind: NetworkPolicy
        name: default-deny-all
        namespace: "{{request.object.metadata.name}}"
        data:
          spec:
            podSelector: {}
            policyTypes: [Ingress, Egress]
```

### Cosign: Verify Image Signature in Admission (Kyverno)

```yaml
apiVersion: kyverno.io/v1
kind: ClusterPolicy
metadata:
  name: require-cosign-signature
spec:
  validationFailureAction: enforce
  background: false
  rules:
    - name: verify-image-signature
      match:
        any:
          - resources:
              kinds: [Pod]
      validate:
        message: "Image {{request.object.spec.containers[0].image}} must be signed by Cosign with trusted issuer"
        verifyImages:
          - imageReferences:
              - "*"
            attestors:
              - entries:
                  - keyless:
                      url: https://oauth-openshift.apps.<cluster>.payu.internal
```

### LitmusChaos: Pod Delete Experiment (payu-sit)

```yaml
apiVersion: litmuschaos.io/v1alpha1
kind: ChaosEngine
metadata:
  name: pod-delete-sit
  namespace: payu-sit
spec:
  engineState: "active"
  annotationCheck: "false"
  appinfo:
    appns: "payu-sit"
    applabel: "app=my-service"
    appkind: "deployment"
  chaosServiceAccount: litmus-admin
  experiments:
    - name: pod-delete
      spec:
        components:
          env:
            - name: TOTAL_CHAOS_DURATION
              value: "30"
            - name: CHAOS_INTERVAL
              value: "10"
            - name: FORCE
              value: "true"
        probe:
          - name: "http-probe"
            type: "httpProbe"
            httpProbe/inputs:
              url: "http://my-service.payu-sit.svc/health"
              method: "GET"
              responseTimeout: 5
              interval: 2
              retry: 3
            mode: "Continuous"
            runProperties:
              probeTimeout: 10
              interval: 5
              attempt: 3
```

---

_Dokumen ini bersifat CONFIDENTIAL dan hanya untuk distribusi internal personal project development. Payu Platform Engineering — 2026_  
_Versi 1.2.0 — Updated dengan: OWASP Top 10 2025, 100% open-source tooling, chaos engineering strategy (Kraken+Litmus), emergency workflow, DevEx KPI, OWASP API Security Top 10, PCI-DSS v4.0 alignment._

          attempt: 3
```

---

*Dokumen ini bersifat CONFIDENTIAL dan hanya untuk distribusi internal personal project development. Payu Platform Engineering — 2026*  
*Versi 1.2.0 — Updated dengan: OWASP Top 10 2025, 100% open-source tooling, chaos engineering strategy (Kraken+Litmus), emergency workflow, DevEx KPI, OWASP API Security Top 10, PCI-DSS v4.0 alignment.*
