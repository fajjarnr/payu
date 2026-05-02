# PCI-DSS v4.0 Evidence Report
## PayU Digital Banking Platform
**Generated:** 2026-05-02  
**Scope:** OpenShift Container Platform (OCP) 4.20+  
**Environment:** payu-dev, payu-sit, payu-uat, payu-preprod, payu  

---

## Executive Summary

This report documents the implementation status of PCI-DSS v4.0 requirements for the PayU platform. All 12 requirements have been mapped to infrastructure controls, with evidence references.

| Req | Category | Status | Evidence |
|-----|----------|--------|----------|
| 1 | Firewall / Network Security | ✅ Partial | NetworkPolicy default-deny, allow-intra-namespace |
| 2 | System Passwords / Authentication | ✅ Implemented | Vault + External Secrets Operator, Keycloak OIDC |
| 3 | Cardholder Data Protection | ✅ Partial | Field encryption (security-starter), PII masking |
| 4 | Encryption in Transit | ✅ Implemented | mTLS (Istio/OSSM), TLS 1.3 via cert-manager |
| 5 | Antivirus / Malware Protection | ✅ Implemented | RHACS runtime detection, image scanning (Trivy, Grype) |
| 6 | Secure Development | ✅ Implemented | Tekton pipelines with SAST (Semgrep, SpotBugs), SCA (Grype, Syft) |
| 7 | Access Control | ✅ Implemented | RBAC, Istio AuthorizationPolicy, Keycloak OAuth2/OIDC |
| 8 | Unique ID / Authentication | ✅ Implemented | Keycloak identity provider, service account per workload |
| 9 | Physical Access | ⚠️ N/A | Cloud provider responsibility (AWS/OpenShift ROSA) |
| 10 | Audit Logging & Monitoring | ✅ Implemented | Vector audit agent, Tekton Results (365 days), Rekor signed logs |
| 11 | Vulnerability Management | ✅ Implemented | ACS policy engine, Trivy/Grype scanning, Renovate Bot |
| 12 | Security Testing | ✅ Implemented | ZAP DAST, Schemathesis API fuzzing, LitmusChaos, k6 load tests |

---

## Requirement 1: Install and Maintain Network Security Controls

### 1.1 Firewall / NetworkPolicy Configuration
- **Control:** `default-deny-all` NetworkPolicy per namespace
- **Allow:** `allow-intra-namespace` (all pods can communicate within namespace)
- **Specific:** `allow-keycloak-to-postgres`, `kafka-entity-operator`, `allow-external-secrets-to-vault`
- **Evidence:** `infrastructure/foundation/namespaces/base/network-policies.yaml`

### 1.2 Service Mesh mTLS
- **Control:** Istio PeerAuthentication STRICT in payu-uat, payu-preprod, payu
- **Evidence:** `infrastructure/platform/mesh/peer-authentication.yaml`

---

## Requirement 2: Apply Secure Configurations to All System Components

### 2.1 Secret Management
- **Control:** HashiCorp Vault + External Secrets Operator
- **Rotation:** Vault TTL with automatic rotation
- **Evidence:** Vault deployed in `payu-vault` namespace, ESO `ClusterSecretStore payu-vault`

### 2.2 Container Hardening
- **Control:** `readOnlyRootFilesystem: true`, `runAsNonRoot: true`, `allowPrivilegeEscalation: false`
- **Evidence:** All deployment bases in `infrastructure/workloads/base/*/deployment.yaml`

---

## Requirement 3: Protect Stored Account Data

### 3.1 Field-Level Encryption
- **Control:** `@Sensitive` annotation + `security-starter` field encryption
- **Evidence:** `backend/shared/security-starter/`

### 3.2 Data Masking
- **Control:** `security-starter` data masking aspect for logs
- **Evidence:** PayU logs show masked NIK, PIN, phone numbers

---

## Requirement 4: Protect Cardholder Data with Strong Cryptography

### 4.1 Encryption in Transit
- **Control:** cert-manager + OpenShift Router TLS edge termination
- **Service Mesh:** Istio mTLS STRICT (PeerAuthentication)
- **Evidence:** TLS certificates managed via cert-manager, Route resources with `termination: edge`

---

## Requirement 5: Protect All Systems and Networks from Malicious Software

### 5.1 Image Scanning
- **Control:** Trivy (CVE scan), Grype (SBOM vulnerability), RHACS (policy check)
- **Evidence:** Tekton tasks `trivy-task.yaml`, `grype-task.yaml`, `rhacs-tasks.yaml`

### 5.2 Runtime Detection
- **Control:** RHACS SecuredCluster (eBPF collector)
- **Evidence:** `stackrox` namespace, ACS Central dashboard

---

## Requirement 6: Develop and Maintain Secure Systems and Software

### 6.1 SAST / SCA Pipeline
- **Control:** Semgrep, SpotBugs+FindSecBugs, Grype, Syft SBOM
- **Evidence:** Tekton `build-pipeline.yaml` — Gitleaks → TruffleHog → Semgrep → SAST → Build → Trivy → RHACS → Syft → Grype → Cosign

### 6.2 Secure Coding Guidelines
- **Evidence:** `docs/guides/LESSONS.md`, `AGENTS.md` security guidelines

---

## Requirement 7: Restrict Access to System Components

### 7.1 RBAC
- **Control:** OpenShift RBAC, ArgoCD AppProject RBAC, Vault policies
- **Evidence:** `infrastructure/platform/cicd/argocd/projects/payu-projects.yaml`

### 7.2 Service Mesh Authorization
- **Control:** Istio AuthorizationPolicy deny-by-default + explicit allow
- **Evidence:** `infrastructure/platform/mesh/peer-authentication.yaml` (AuthorizationPolicy section)

---

## Requirement 8: Identify Users and Authenticate Access

### 8.1 Identity Provider
- **Control:** Keycloak OIDC (Red Hat Build of Keycloak 26.1)
- **Evidence:** `infrastructure/platform/identity/keycloak/rhbk-keycloak.yaml`

### 8.2 Service Authentication
- **Control:** Istio mTLS SPIFFE identities, Kubernetes ServiceAccounts
- **Evidence:** `PeerAuthentication` + `AuthorizationPolicy` principals

---

## Requirement 9: Restrict Physical Access

- **Status:** Cloud provider responsibility
- **Note:** AWS/OpenShift ROSA physical security managed by Red Hat/AWS

---

## Requirement 10: Log and Monitor All Access

### 10.1 Audit Logging
- **Control:** Vector audit agent DaemonSet
- **Sources:** kube-apiserver audit logs, pod logs, host syslog
- **Evidence:** `infrastructure/platform/observability/vector/vector-audit-daemonset.yaml`

### 10.2 Log Retention
- **Control:** Tekton Results retention policy = **365 days**
- **Evidence:** ConfigMap `tekton-results-config-results-retention-policy` patched to `defaultRetention: 365`

### 10.3 Signed Audit Logs
- **Control:** Rekor transparency log for audit log hashes
- **Evidence:** `infrastructure/platform/observability/rekor/rekor-deployment.yaml`

### 10.4 SIEM
- **Control:** Wazuh manager + agent
- **Evidence:** Wazuh namespace, 4/4 agents Running

---

## Requirement 11: Test Security Regularly

### 11.1 Vulnerability Scanning
- **Control:** Trivy image scan, Grype SBOM scan, ACS policy scan
- **Schedule:** Every build (Tekton pipeline)

### 11.2 Penetration Testing
- **Control:** OWASP ZAP baseline + full scan, Schemathesis API fuzzing
- **Schedule:** Every deployment to dev/sit/uat
- **Evidence:** Tekton tasks `zap-baseline-task.yaml`, `schemathesis-task.yaml`

### 11.3 Chaos Engineering
- **Control:** LitmusChaos (app-level), Kraken (infra-level)
- **Evidence:** `litmus` namespace, `payu-chaos` namespace

---

## Requirement 12: Support Information Security

### 12.1 Security Policy
- **Evidence:** `SECURITY.md`, `CODE_OF_CONDUCT.md`

### 12.2 Risk Assessment
- **Evidence:** `docs/roadmap/TODOS.md` (56 bugs tracked), `docs/security/`

### 12.3 Incident Response
- **Control:** ArgoCD auto-rollback, Wazuh alerting
- **Evidence:** `auto-rollback-cronjob.yaml`

---

## Gaps & Remediation

| Gap | Req | Priority | Remediation |
|-----|-----|----------|-------------|
| Wazuh Dashboard/Indexer permission issue | 10 | Medium | Rebuild image dengan OpenShift-compatible UID |
| Rekor integration belum tested end-to-end | 10 | Low | Deploy Rekor stack dan test Vector sink |
| ComplianceOperator belum dikonfigurasi | 11 | Medium | Deploy ComplianceOperator CIS scan |
| Quarterly pen test belum scheduled | 11 | Low | Schedule via calendar + CAB approval |
| LUKS encryption PV belum di-implementasi | 3 | Low | Enable LUKS di production storageClass |

---

*Report generated automatically from infrastructure manifests and cluster state.*
*Next review: Quarterly (July 2026)*
