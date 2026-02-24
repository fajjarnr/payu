# PayU Documentation Index

> **Complete documentation catalog for PayU Digital Banking Platform**

## 📚 Documentation Structure

| Path | Purpose | Files |
|-----------|---------|-------|
| `architecture/` | Technical design & C4 diagrams | ARCHITECTURE.md, SERVICE_CATALOG.md, EVENT_CATALOG.md, CICD-MONITORING-GUIDE.md |
| `product/` | Product requirements & features | PRD.md |
| `api/` | API documentation | API_STANDARDS.md |
| `guides/` | Development guides & AI skills | GEMINI.md, CONTRIBUTING.md, AGENT_SKILLS_GUIDE.md, ONBOARDING.md, USAGE.md, LESSONS.md, VAULT.md, TDD_QUICK_REFERENCE.md |
| `operations/` | Deployment & runbooks | INFRASTRUCTURE_DEPLOYMENT.md, ZERO-DOWNTIME-DEPLOYMENT.md, DISASTER_RECOVERY.md, runbooks/ |
| `security/` | Security policies & reports | SECURITY.md, SECURITY_RUNBOOK.md, PCI-DSS-UU-PDP-AUDIT-REPORT.md, PENTEST_REPORT.md |
| `compliance/` | Regulatory compliance | OJK_BI_REGULATORY_AUDIT.md |
| `qa/` | Testing strategy | QA_STRATEGY.md, JACOCO_SETUP.md |
| `reports/` | Test reports | QA_TEST_REPORT.md |
| `roadmap/` | Project roadmap & gaps | TODOS.md · PROGRESS.md · GATEWAY_ARCH.md |
| `adr/` | Architecture decision records | 0000-0013 (ADR index) |

---

## 📖 Quick Reference

### For Developers
- **[ARCHITECTURE.md](architecture/ARCHITECTURE.md)** - System architecture, microservices, C4 diagrams
- **[ONBOARDING.md](guides/ONBOARDING.md)** - **START HERE**: Developer onboarding & environment setup
- **[API_STANDARDS.md](api/API_STANDARDS.md)** - REST API standards & Spectral validation
- **[CONTRIBUTING.md](guides/CONTRIBUTING.md)** - Git workflow & branch naming
- **[LESSONS.md](guides/LESSONS.md)** - Lessons learned & implementation patterns
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Common issues & fixes (Local & OpenShift)
- **[USAGE.md](guides/USAGE.md)** - Internal API credentials & how to use the app

### For DevOps/SRE
- **[INFRASTRUCTURE_DEPLOYMENT.md](operations/INFRASTRUCTURE_DEPLOYMENT.md)** - Infrastructure setup & service endpoints
- **[ZERO-DOWNTIME-DEPLOYMENT.md](operations/ZERO-DOWNTIME-DEPLOYMENT.md)** - Blue-Green & Canary strategies
- **[DISASTER_RECOVERY.md](operations/DISASTER_RECOVERY.md)** - Backup & restore procedures
- **[runbooks/](operations/runbooks/)** - Operations runbooks (slo, error-rate, etc.)

### For Security & Compliance
- **[SECURITY.md](security/SECURITY.md)** - Core security policy & compliance status
- **[PCI-DSS-UU-PDP-AUDIT-REPORT.md](security/PCI-DSS-UU-PDP-AUDIT-REPORT.md)** - Latest audit report
- **[SECURITY_RUNBOOK.md](security/SECURITY_RUNBOOK.md)** - Incident response procedures

---

## 🗺️ Documentation Map

```
docs/
├── architecture/            # Architecture & design
├── product/                 # Product requirements (PRD)
├── api/                     # API Standards & Spectral validation
├── guides/                  # Dev guides (Onboarding, Lessons, Usage)
├── operations/              # Deployment, Runbooks, Infra
├── security/                # Security Policy, Audit reports, Pentest
├── compliance/              # Regulatory (OJK/BI)
├── qa/                      # Testing Strategy
├── reports/                 # Test Execution Reports
├── roadmap/                 # TODOS, Progress, Gateway Arch
├── adr/                     # Architecture Decision Records
├── archive/                 # Historical/deprecated docs
├── INDEX.md                 # This file
└── TROUBLESHOOTING.md       # Unified troubleshooting guide
```

---

## 📝 Documentation Standards

1. **Source of Truth**: Untuk instruksi AI, gunakan root `GEMINI.md`. Folder `docs/guides/GEMINI.md` adalah cermin/copy.
2. **Consistency**: Gunakan kebab-case untuk file pembantu dan UPPER_CASE untuk dokumen utama.
3. **Updates**: Setiap update besar wajib memperbarui `CHANGELOG.md` dan `INDEX.md`.

---

**Selamat berkarya! 🚀**
_Last Updated: February 24, 2026_
