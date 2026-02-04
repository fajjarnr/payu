# PayU Documentation Index

> **Complete documentation catalog for PayU Digital Banking Platform**

## 📚 Documentation Structure

| Path | Purpose | Files |
|-----------|---------|-------|
| `architecture/` | Technical design & C4 diagrams | ARCHITECTURE.md, SERVICE_CATALOG.md, EVENT_CATALOG.md, CICD-MONITORING-GUIDE.md |
| `product/` | Product requirements & features | PRD.md |
| `api/` | API documentation | API_STANDARDS.md |
| `guides/` | Development guides & AI skills | GEMINI.md, CONTRIBUTING.md, AGENT_SKILLS_GUIDE.md, ONBOARDING.md, TDD_QUICK_REFERENCE.md |
| `operations/` | Runbooks & disaster recovery | DISASTER_RECOVERY.md, LOKISTACK.md, runbooks/ |
| `security/` | Security policies & runbooks | SECURITY_RUNBOOK.md, PENTEST_REPORT.md |
| `compliance/` | Regulatory compliance | OJK_BI_REGULATORY_AUDIT.md |
| `qa/` | Testing strategy & reports | QA_STRATEGY.md, JACOCO_SETUP.md |
| `reports/` | Test reports | QA_TEST_REPORT.md |
| `roadmap/` | Project roadmap & todos | TODOS.md |
| `adr/` | Architecture decision records | 0000-0007 (ADR index) |
| `test-improvements.md` | E2E test fixes summary | test-improvements.md |

---

## 📖 Quick Reference

### For Developers
- **[ARCHITECTURE.md](architecture/ARCHITECTURE.md)** - System architecture, microservices, C4 diagrams
- **[SERVICE_CATALOG.md](architecture/SERVICE_CATALOG.md)** - Complete service inventory
- **[EVENT_CATALOG.md](architecture/EVENT_CATALOG.md)** - Kafka topics and event schemas
- **[API_STANDARDS.md](api/API_STANDARDS.md)** - REST API standards and conventions
- **[CONTRIBUTING.md](guides/CONTRIBUTING.md)** - Git workflow, commit conventions, PR process
- **[ONBOARDING.md](guides/ONBOARDING.md)** - Developer onboarding guide
- **[GEMINI.md](guides/GEMINI.md)** - AI Assistant guidelines (mirrored in `CLAUDE.md`)
- **[QA_STRATEGY.md](qa/QA_STRATEGY.md)** - Testing standards, TDD workflow, coverage thresholds

### For Product Managers
- **[PRD.md](product/PRD.md)** - Product requirements, features, success metrics
- **[TODOS.md](roadmap/TODOS.md)** - Project roadmap, task tracking, completion status

### For DevOps/SRE
- **[DISASTER_RECOVERY.md](operations/DISASTER_RECOVERY.md)** - Backup & restore procedures
- **[LOKISTACK.md](operations/LOKISTACK.md)** - Logging & monitoring setup
- **[CICD-MONITORING-GUIDE.md](architecture/CICD-MONITORING-GUIDE.md)** - CI/CD pipeline & monitoring
- **[service-degradation.md](operations/runbooks/service-degradation.md)** - Performance degradation runbook
- **[high-error-rate.md](operations/runbooks/high-error-rate.md)** - High error rate runbook
- **[error-budget.md](operations/runbooks/error-budget.md)** - Error budget exhaustion runbook
- **[slo-availability.md](operations/runbooks/slo-availability.md)** - SLO breach runbook

### For Security/Compliance
- **[SECURITY_RUNBOOK.md](security/SECURITY_RUNBOOK.md)** - Incident response procedures
- **[PENTEST_REPORT.md](security/PENTEST_REPORT.md)** - Penetration testing results
- **[OJK_BI_REGULATORY_AUDIT.md](compliance/OJK_BI_REGULATORY_AUDIT.md)** - Regulatory compliance status

### For QA Engineers
- **[QA_STRATEGY.md](qa/QA_STRATEGY.md)** - Testing philosophy, test pyramid, patterns
- **[JACOCO_SETUP.md](qa/JACOCO_SETUP.md)** - Code coverage setup
- **[QA_TEST_REPORT.md](reports/QA_TEST_REPORT.md)** - Latest test execution reports
- **[test-improvements.md](test-improvements.md)** - E2E fixes summary and remaining work

### For AI/ML Engineers
- **[AGENT_SKILLS_GUIDE.md](guides/AGENT_SKILLS_GUIDE.md)** - AI Skills & Agents catalog
- **[TDD_QUICK_REFERENCE.md](guides/TDD_QUICK_REFERENCE.md)** - TDD quick reference

---

## 🗺️ Documentation Map

```
docs/
├── architecture/
│   ├── ARCHITECTURE.md              # Complete system architecture (C4 diagrams)
│   └── CICD-MONITORING-GUIDE.md     # CI/CD pipeline & monitoring setup
│
├── product/
│   └── PRD.md                       # Product Requirements Document
│
├── guides/
│   ├── GEMINI.md                    # AI Assistant Guidelines (CLAUDE.md alias)
│   ├── CONTRIBUTING.md              # Git workflow & commit conventions
│   ├── AGENT_SKILLS_GUIDE.md        # AI Skills & Agents catalog
│   ├── TDD_QUICK_REFERENCE.md       # TDD quick reference
│   ├── DATABASE_CACHE_OPTIMIZATION.md  # DB & Cache optimization guide
│   └── VAULT.md                     # HashiCorp Vault setup
│
├── operations/
│   ├── DISASTER_RECOVERY.md         # Backup & restore procedures
│   └── LOKISTACK.md                 # Logging & monitoring (LokiStack)
│
├── security/
│   ├── SECURITY_RUNBOOK.md          # Security incident response
│   └── PENTEST_REPORT.md            # Penetration testing results
│
├── compliance/
│   └── OJK_BI_REGULATORY_AUDIT.md   # OJK & BI regulatory compliance
│
├── qa/
│   ├── QA_STRATEGY.md               # Testing strategy & standards
│   └── JACOCO_SETUP.md              # Code coverage setup
│
├── reports/
│   └── QA_TEST_REPORT.md            # Latest test execution reports
│
├── roadmap/
│   └── TODOS.md                     # Project roadmap & task tracking
│
├── test-improvements.md             # E2E test fixes summary
│
└── adr/
    ├── README.md                    # ADR index
    ├── 0000-adr-guidelines.md       # ADR writing guidelines
    └── 0001-template.md             # ADR template
```

---

## 🔍 Search by Topic

### Architecture & Design
- [ARCHITECTURE.md](architecture/ARCHITECTURE.md) - System architecture overview
- [SERVICE_CATALOG.md](architecture/SERVICE_CATALOG.md) - Complete service inventory
- [EVENT_CATALOG.md](architecture/EVENT_CATALOG.md) - Kafka topics and event schemas
- [API_STANDARDS.md](api/API_STANDARDS.md) - REST API naming & conventions
- [ADR Index](adr/README.md) - Architecture Decision Records (0002-0007)
- [ARCHITECTURE.md](architecture/ARCHITECTURE.md#3-microservices-architecture) - Service specifications
- [ARCHITECTURE.md](architecture/ARCHITECTURE.md#4-event-driven-architecture) - Event streaming (Kafka)
- [ARCHITECTURE.md](architecture/ARCHITECTURE.md#5-data-architecture) - Database strategy

### Development
- [CONTRIBUTING.md](guides/CONTRIBUTING.md) - Git workflow & branching
- [QA_STRATEGY.md](qa/QA_STRATEGY.md) - TDD workflow & testing patterns
- [DATABASE_CACHE_OPTIMIZATION.md](guides/DATABASE_CACHE_OPTIMIZATION.md) - DB optimization

### Operations
- [DISASTER_RECOVERY.md](operations/DISASTER_RECOVERY.md) - Backup procedures
- [SECURITY_RUNBOOK.md](security/SECURITY_RUNBOOK.md) - Incident response
- [LOKISTACK.md](operations/LOKISTACK.md) - Logging & monitoring

### Security & Compliance
- [SECURITY_RUNBOOK.md](security/SECURITY_RUNBOOK.md) - Security incidents
- [OJK_BI_REGULATORY_AUDIT.md](compliance/OJK_BI_REGULATORY_AUDIT.md) - Regulatory compliance
- [PENTEST_REPORT.md](security/PENTEST_REPORT.md) - Security audit results

---

## 📝 Documentation Standards

### Writing Guidelines
1. **Use clear, descriptive titles**
2. **Include code examples with language tags**
3. **Use Mermaid diagrams for flows**
4. **Add tables for comparisons**
5. **Include last updated date**

### File Naming
- Use `UPPER_CASE.md` for major documents
- Use `kebab-case.md` for guides
- Use `0001-template.md` for ADRs

### Review Process
1. Create/Edit documentation
2. Run `@dx-engineer` skill for review (or `@principal-architect` for architecture docs)
3. Submit PR for review
4. Update INDEX.md if adding new docs

---

## 🚀 Getting Started

1. **New Developers**: Start with [ARCHITECTURE.md](architecture/ARCHITECTURE.md) and [CONTRIBUTING.md](guides/CONTRIBUTING.md)
2. **Product Managers**: Read [PRD.md](product/PRD.md) and [TODOS.md](roadmap/TODOS.md)
3. **QA Engineers**: Review [QA_STRATEGY.md](qa/QA_STRATEGY.md)
4. **DevOps**: Check [DISASTER_RECOVERY.md](operations/DISASTER_RECOVERY.md) and [CICD-MONITORING-GUIDE.md](architecture/CICD-MONITORING-GUIDE.md)

---

_Last Updated: February 4, 2026_
