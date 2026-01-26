# Security Policy

## 🔒 PayU Digital Banking Security

PayU takes security seriously. This document outlines our security policy and how to report vulnerabilities.

---

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## Reporting a Vulnerability

### ⚠️ DO NOT create a public GitHub issue for security vulnerabilities

If you discover a security vulnerability within PayU, please follow these steps:

1. **Email**: Send a detailed report to **security@payu.id**
2. **Subject**: Use the format `[SECURITY] Brief description`
3. **Include**:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### Response Timeline

| Stage | Timeline |
|-------|----------|
| Initial Response | Within 24 hours |
| Triage & Assessment | Within 72 hours |
| Resolution Plan | Within 7 days |
| Patch Release | Based on severity |

---

## Security Measures

### Authentication & Authorization
- OAuth 2.0 / OpenID Connect via Red Hat SSO (Keycloak)
- Multi-factor authentication (MFA) support
- JWT tokens with short expiry
- Account lockout after failed attempts

### Data Protection
- TLS 1.3 for all communications
- AES-256 encryption at rest
- Field-level encryption for sensitive data (PAN, NIK)
- PKCS#11 HSM integration for key management

### API Security
- Rate limiting per client/IP
- Request signing for partner APIs
- Input validation and sanitization
- OWASP Top 10 compliance

### Infrastructure Security
- Red Hat OpenShift 4.20+ with built-in security
- Network policies for service isolation
- Pod Security Standards (Restricted)
- Regular security scanning (Trivy, Snyk)

### Compliance
- PCI-DSS Level 1 (in progress)
- ISO 27001 (in progress)
- Bank Indonesia regulations

---

## Security Documentation

For detailed security information, see:
- [Security Runbook](docs/security/SECURITY_RUNBOOK.md)
- [Pentest Report](docs/security/PENTEST_REPORT.md)
- [Vault Configuration](docs/guides/VAULT.md)

---

## Acknowledgments

We appreciate the security research community and will acknowledge responsible disclosures in our release notes (with permission).
