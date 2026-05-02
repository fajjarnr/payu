# Quarterly Penetration Testing Schedule & CAB Report Template
## PayU Digital Banking Platform

---

## Schedule

| Quarter | Window | Environment | Scope | Team |
|---------|--------|-------------|-------|------|
| Q1 2026 | Mar 15–28 | payu-preprod | All 23 services + API Gateway | External vendor + Internal security |
| Q2 2026 | Jun 15–28 | payu-preprod | API Gateway, Auth, Transaction | External vendor |
| Q3 2026 | Sep 15–28 | payu-preprod | Full platform + OWASP Top 10 | External vendor + Red team |
| Q4 2026 | Dec 15–28 | payu-preprod | PCI-DSS focused scope | External vendor |

## Pre-Test Checklist

- [ ] CAB approval obtained (minimum 2 weeks prior)
- [ ] Environment isolated from production
- [ ] Backup verified and restore tested
- [ ] Wazuh alerting silenced for expected scan traffic
- [ ] Kraken chaos schedule paused during pen test window
- [ ] Emergency contact list distributed

## Post-Test Deliverables

1. Executive summary (CISO + Board)
2. Technical findings report (CVSS scoring)
3. Remediation roadmap with timeline
4. Re-test validation report

## CAB Report Template

```markdown
# Penetration Test Report — Q{X} 2026
**Date:** {start_date} – {end_date}
**Scope:** {scope_description}
**Tester:** {vendor_name} / Internal Security Team

## Executive Summary
{high_level_findings}

## Risk Rating
| Severity | Count | Status |
|----------|-------|--------|
| Critical | {count} | {status} |
| High | {count} | {status} |
| Medium | {count} | {status} |
| Low | {count} | {status} |

## Key Findings
### {finding_1_title} ({severity})
- **Description:** {description}
- **Impact:** {business_impact}
- **Remediation:** {remediation_steps}
- **ETA:** {target_date}

## Remediation Plan
| # | Finding | Owner | Target Date | Status |
|---|---------|-------|-------------|--------|
| 1 | {finding} | {owner} | {date} | Open |

## CAB Approval
**Approved by:** {names}
**Date:** {date}
**Conditions:** {conditions}
```

## Automation

Tekton Pipeline `payu-pentest-pipeline` akan di-trigger manual untuk setiap quarter.
Pipeline mencakup: ZAP full scan → Schemathesis → k6 load test → report aggregation.
