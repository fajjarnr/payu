# ADR-0032: Perimeter Security Architecture — Tiered WAF (AWS WAF & Coraza OWASP CRS) and Centralized SIEM (Wazuh & OpenShift CLF Syslog Sink)

**Status**: Accepted  
**Date**: 2026-08-18  
**Deciders**: Principal Architect, Cybersecurity Architect, Platform Engineer, Core Banking Lead  

---

## Context

Sebagai platform perbankan digital dan payment gateway terintegrasi (SNAP-BI, VA, QRIS, Transfer, dan Escrow), PayU beroperasi pada lingkungan hybrid-cloud di atas Red Hat OpenShift 4.20+. Lalu lintas API publik masuk dari beragam entitas eksternal: mitra perbankan (TokoBapak, Nobar, Dolan, Sinau, Maca), aplikasi mobile perbankan (Expo/React Native), serta web banking (Next.js).

Dalam lanskap ancaman sektor perbankan, platform menghadapi tantangan keamanan perimeter yang kompleks:
1. **Ancaman Volumetrik & Layer 7**: Serangan DDoS Layer 7, credential stuffing bot, scanner otomatis (mass scanning tool), serta web application exploitation (OWASP Top 10: SQLi, NoSQLi, XSS, Remote Code Execution, Command Injection, Server-Side Request Forgery).
2. **Kepatuhan Regulasi Finansial & Standar Industri**:
   - **PCI-DSS v4.0 Requirement 6.4.2**: Mewajibkan solusi teknis otomatis (WAF) di depan aplikasi publik untuk mendeteksi dan mencegah serangan berbasis web secara berkelanjutan.
   - **PCI-DSS v4.0 Requirement 10**: Mewajibkan pencatatan dan pemantauan terpusat seluruh akses ke komponen sistem, log audit sistem operasi, API server, dan autentikasi.
   - **POJK No. 11/POJK.03/2022 (MRTI OJK)**: Kewajiban perlindungan integritas sistem, deteksi insiden keamanan siber secara *real-time*, dan penyimpanan jejak audit (*audit trail*).
   - **Standar Keamanan SNAP-BI Bank Indonesia**: Perlindungan terhadap manipulasi header, payload tampering, serta replay attack.
3. **Karakteristik Payload Perbankan & Risiko False Positive**: Payload SNAP-BI mengandung string kriptografis berkepadatan tinggi (Base64 HMAC-SHA512 pada header `X-SIGNATURE`, RSA public key, ISO-8601 timestamps) yang rentan memicu *false positive* jika WAF tidak di-tuning secara presisi sesuai format perbankan.
4. **Isolasi Log Audit & Keterbatasan OpenShift**: Pengiriman log audit cluster (Kubernetes API, OpenShift OAuth, node syscall) harus diisolasi dari log aplikasi umum dan diteruskan ke Security Information and Event Management (SIEM) secara deterministik tanpa terkendala isu *egress routing* OVN-Kubernetes.

---

## Decision Drivers

- **Defense-in-Depth (Pertahanan Berlapis)**: Tidak mengandalkan single-point WAF; memisahkan filtering volumetrik di cloud edge dari deep inspection di application ingress.
- **Portabilitas & Multi-Cloud Agnostic**: Solusi keamanan cluster harus mampu beroperasi secara independen di AWS, bare-metal on-premise (ODF), maupun hybrid DR tanpa vendor lock-in mutlak.
- **Audit-Grade Log Centralization & Zero Evidence Tampering**: Semua log audit, WAF alerts, Falco runtime detections, dan Keycloak/Vault audit terpusat di SIEM dengan jaminan immutable retention minimal 365 hari.
- **Low Latency & High Throughput Invariant**: Overhead pemrosesan WAF tidak boleh melebihi 5 milidetik ($p99 < 5\text{ms}$) agar tidak merusak SLA transaksi SNAP-BI ($< 500\text{ms}$).
- **OpenShift Hardening Compatibility**: Komponen SIEM & WAF harus patuh terhadap Security Context Constraints (SCC) OpenShift tanpa merusak postur *restricted-v2*.

---

## Decision

Kami menetapkan arsitektur **Perimeter Security & Centralized SIEM** PayU sebagai berikut:

```mermaid
flowchart TD
    subgraph PERIMETER["1. Outer Edge Perimeter (Cloud / Edge CDN)"]
        CLIENT["External Traffic (Partners / Mobile / Web)"] -->|HTTPS 443| AWS_WAF["AWS WAF / Edge Gate (CloudFront / ALB)"]
        AWS_WAF -->|DDoS L7 / Bot / Geo-Block Filtered| OCP_ROUTER["OpenShift Ingress Router (HAProxy / TLS Edge Termination)"]
    end

    subgraph INGRESS_LAYER["2. Inner Ingress & Application WAF (OpenShift)"]
        OCP_ROUTER --> CORAZA["Coraza WAF (OWASP CRS v4.x WASM / Ingress Proxy)"]
        CORAZA -->|Inspected & Sanitized Payload| APICAST["APIcast 3scale Edge Gateway (Rate Limit / API Key)"]
        APICAST -->|mTLS / Clean HTTP| GW["Quarkus gateway-service (BFF / Routing / JWT)"]
    end

    subgraph WORKLOAD_MESH["3. Zero-Trust Workload Layer"]
        GW --> MESH["Istio Service Mesh (STRICT mTLS)"]
        MESH --> SVCS["PayU Microservices (partner, transaction, wallet, auth)"]
    end

    subgraph LOG_PIPELINE["4. Security Telemetry & Audit Sinks"]
        CORAZA -->|Audit Log JSON (Port 1514)| WAZUH_MGR["Wazuh Manager Cluster (master/worker)"]
        FALCO["Falco DaemonSet (Kernel Syscalls)"] -->|Syslog / gRPC| WAZUH_MGR
        CLF["OpenShift ClusterLogForwarder (Audit Refs)"] -->|Syslog RFC5424 (UDP/TCP 514)| WAZUH_MGR
        KEYCLOAK["Keycloak Auth Audit"] -->|Syslog Forward| WAZUH_MGR
        VAULT["Vault Audit Device"] -->|Syslog / Vector| WAZUH_MGR
    end

    subgraph SIEM_ANALYTICS["5. Wazuh SIEM & SOC Alerting"]
        WAZUH_MGR --> INDEXER["Wazuh Indexer (OpenSearch DB - gp3 PVC)"]
        INDEXER --> DASHBOARD["Wazuh Dashboard (Security Analytics UI)"]
        WAZUH_MGR -->|Sev >= 10 / MITRE Alerts| SECOPS["Alertmanager / PagerDuty / Telegram Alert"]
    end
```

---

### 1. Two-Tier WAF Strategy: AWS WAF vs Coraza WAF (OWASP CRS)

Kami mengadopsi pola **Tiered WAF Defense-in-Depth** yang menjadi standar emas industri perbankan digital:

| Aspek | Tier 1: Outer Edge (AWS WAF) | Tier 2: Inner Ingress (Coraza WAF + OWASP CRS v4.x) |
| :--- | :--- | :--- |
| **Lokasi Deployment** | AWS CloudFront / Application Load Balancer (ALB) | OpenShift Ingress Gateway / Service Mesh Envoy Filter (`coraza-wasm`) |
| **Fungsi Utama** | Volumetric DDoS L7, Bot Control, Geo-blocking (Non-ID/allowed list), IP Reputation rate-limiting | Deep Application Payload Inspection, OWASP Top 10, Protocol Enforcement, Financial API Decoders |
| **Inspection Depth** | Header, URI, Top 16-64 KiB Body (Token Bucket) | Full Nested JSON parsing, multipart header check, regex normalization |
| **Portabilitas** | AWS Managed (Cloud only) | 100% Cloud-Agnostic (Bisa jalan di AWS, OpenShift on-prem bare-metal, & DR site) |
| **Operasional** | Managed ruleset update otomatis oleh AWS | Reguler CRS rule sync & tuning internal via GitOps (ArgoCD) |

> **Rasionalisasi Kebutuhan Coraza WAF**:
> Meskipun AWS WAF melindungi pintu gerbang cloud, **Coraza WAF wajib diimplementasikan** di cluster OpenShift karena:
> 1. **Kemandirian Infrastruktur**: Menghindari *vendor lock-in* dan memungkinkan PayU menjalankan postur keamanan identik saat disaster recovery ke on-premise datacenter.
> 2. **Deep Tuning Financial API**: Coraza mengevaluasi aturan OWASP CRS v4.x secara granular dengan konteks microservices internal.
> 3. **Direct SIEM Telemetry**: Event audit Coraza dikirim langsung ke Wazuh Manager di dalam cluster tanpa latensi dan biaya data-transfer keluar cloud.

---

### 2. Coraza WAF Tuning & SNAP-BI Exception Policy

1. **Paranoia Level (PL) & Scoring Threshold**:
   - **Paranoia Level 1 (PL1)**: Diaktifkan secara *default* untuk seluruh endpoint publik (proteksi terhadap high-confidence SQLi, XSS, RCE, LFI, Command Injection).
   - **Paranoia Level 2 (PL2)**: Diaktifkan khusus pada endpoint autentikasi dan transfer finansial sensitif:
     - `/v1.0/access-token/*`
     - `/internal/v1/auth/*`
     - `/api/v1/wallets/*`
     - `/api/v1/transactions/*`
   - **Anomaly Inbound Blocking Threshold**: Skor threshold = `5`. Request dengan skor akumulasi $\ge 5$ otomatis diblokir dengan HTTP status `403 Forbidden` RFC 9457 (`SEC_WAF_BLOCK`).
2. **SNAP-BI & Kriptografi Exclusions (False Positive Prevention)**:
   - Header `X-SIGNATURE` (88-char base64 string) dan `X-CLIENT-KEY` dikecualikan dari rule SQLi/RCE inspection (CRS Rule 942100, 932100).
   - JSON Body Inspection Limit: Dibatasi maksimal **128 KiB** (mencakup seluruh spesifikasi SNAP-BI JSON payload).
   - Endpoint upload dokumen KYC (`/api/v1/kyc/documents` multipart form-data) di-bypass dari deep body string inspection CRS dan dialihkan ke scanning malware/antivirus terisolasi.
3. **Staging Profiling Window**:
   - Deployment Coraza di staging berjalan dalam mode **Detection / Monitor-Only** selama 14 hari under load k6 test untuk memverifikasi zero false positives pada partner integrations sebelum diaktifkan menjadi **Enforcing / Block Mode**.

---

### 3. OpenShift ClusterLogForwarder (CLF) Syslog Sink ke Wazuh (`INFRA-029` / `SEC-020`)

Audit log OpenShift dan Kubernetes dialirkan secara terpusat ke Wazuh Manager menggunakan protokol standar **Syslog RFC 5424**:

```yaml
# cluster-logging-wazuh-sink.yaml
apiVersion: observability.openshift.io/v1
kind: ClusterLogForwarder
metadata:
  name: instance
  namespace: openshift-logging
spec:
  serviceAccount:
    name: logcollector
  outputs:
    - name: loki
      type: lokiStack
      lokiStack:
        target:
          name: loki
          namespace: openshift-logging
        authentication:
          token:
            from: serviceAccount
      tls:
        ca:
          configMapName: loki-gateway-ca-bundle
          key: service-ca.crt
    - name: wazuh-syslog
      type: syslog
      syslog:
        rfc: RFC5424
        facility: authpriv
        severity: informational
        addLogSource: true
      url: tcp://wazuh-manager.wazuh.svc.cluster.local:514
  pipelines:
    - name: audit-to-loki
      inputRefs:
        - audit
      outputRefs:
        - loki
    - name: audit-to-wazuh-siem
      inputRefs:
        - audit
      labels:
        security_zone: perimeter
        compliance: pci-dss-v4.0
      outputRefs:
        - wazuh-syslog
```

---

### 4. Wazuh SIEM Architecture & OpenShift Constraints (`INFRA-011`)

Wazuh SIEM di-deploy di namespace `wazuh` dengan konfigurasi yang telah diverifikasi kompatibel dengan OpenShift 4.20+:

1. **Security Context Constraints (SCC) Mapping**:
   - `wazuh-manager` ServiceAccount $\rightarrow$ `wazuh-manager-scc` (Custom SCC dengan `SYS_CHROOT` capability agar `wazuh-analysisd` dapat melakukan chroot ke `/var/ossec`, fsGroup MustRunAs `101`).
   - `wazuh-indexer` & `wazuh-dashboard` ServiceAccount $\rightarrow$ `anyuid` SCC (Menjalankan OpenSearch & Dashboard OpenShift-native).
   - `wazuh-agent` ServiceAccount $\rightarrow$ `privileged` SCC via `ClusterRoleBinding` (DaemonSet untuk monitoring kernel node, file integrity, dan OS auditd).
2. **Sizing & Storage**:
   - Lab / Dev / Staging: 1x Indexer (500m/1Gi req, 20Gi gp3 PVC), 1x Master Manager (250m/512Mi req, 20Gi gp3 PVC), 1x Dashboard.
   - Production Baseline: 3x Indexer Nodes (Clustered OpenSearch), 1x Master + 2x Worker Managers, 2x Dashboards.

---

### 5. Multi-Source Ingestion & MITRE ATT&CK Mapping

Wazuh Manager mengorelasikan 5 sumber telemetri keamanan:
1. **OpenShift & Kubernetes API Audit Logs**: Mendeteksi modifikasi RBAC tidak sah, unauthorized Pod exec, dan eskalasi hak akses (MITRE T1078, T1611).
2. **Coraza WAF Audit Logs**: Mendeteksi serangan injeksi web, automated scanner, dan pelanggaran protokol (MITRE T1190).
3. **Falco Runtime Security**: Mendeteksi syscall mencurigakan pada kontainer (misal spawn shell di dalam pod backend, membaca `/etc/shadow`, atau modifikasi binary runtime) (MITRE T1059, T1068).
4. **Keycloak Authentication Logs**: Mendeteksi *brute-force login*, anomali *credential stuffing*, dan *excessive token generation* (MITRE T1110).
5. **HashiCorp Vault Audit Logs**: Mendeteksi pembacaan rahasia di luar *service account policy* dan upaya unsealing tidak sah (MITRE T1552).

**Threshold Notifikasi Insiden**:
- Alert dengan Severity Level $\ge 10$ atau terpetakan ke taktik MITRE *Initial Access / Privilege Escalation / Defense Evasion* otomatis memicu alert prioritas tinggi ke kanal SecOps dan Incident Response Runbook ([INCIDENT_RESPONSE.md](../operations/INCIDENT_RESPONSE.md)).

---

## Consequences

### Positive
- **Kepatuhan Regulasi Penuh**: Memenuhi mandat PCI-DSS v4.0 (Req 6.4.2 dan Req 10), POJK 11/2022, dan standar Bank Indonesia SNAP-BI.
- **True Defense-in-Depth**: Menghalau serangan volumetrik di cloud edge sebelum mencapai cluster dan menginspeksi ancaman aplikasi secara presisi di ingress cluster.
- **Audit Trail Terintegrasi & Anti-Tamper**: Jejak audit k8s API, WAF, OS, dan aplikasi terpusat di Wazuh Indexer dengan retensi jangka panjang.
- **Portabilitas Tinggi**: Tidak bergantung 100% pada fitur cloud provider tertentu, siap untuk multi-region dan on-premise deployment.

### Negative / Trade-offs
- Menambahkan komponen operasional WAF (Coraza) dan SIEM (Wazuh Manager/Indexer) di dalam cluster OpenShift yang memerlukan alokasi CPU, RAM, dan persistent storage (gp3).
- Diperlukan fase *tuning & rule exclusion* pada awal peluncuran SNAP-BI guna mencegah pemblokiran transaksi sah akibat false positive.

---

## Implementation & Verification References

- [ADR-0010: Security Standards](./0010-security-standards.md)
- [ADR-0025: SNAP-BI & Partner Gateway Security Standards](./0025-snap-bi-and-partner-gateway-security-standard.md)
- [DevSecOps Architecture](../architecture/DEVSECOPS_ARCHITECTURE.md)
- [Incident Response Runbook](../operations/INCIDENT_RESPONSE.md)
- [Wazuh Platform Helm Values](../../infrastructure/platform/security/wazuh/values.yaml)
- [OpenShift Cluster Logging Forwarder](../../infrastructure/platform/security/logging/cluster-logging.yaml)
- [PCI-DSS v4.0 Evidence Report](../compliance/PCI-DSS-v4.0-Evidence-Report.md)
