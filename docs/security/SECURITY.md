# PayU Digital Banking Platform - Security Policy

> **Classification**: Confidential
> **Last Updated**: February 20, 2026
> **Version**: 2.0

---

## Security Compliance Status

| Standard | Score | Status | Report |
|----------|-------|--------|--------|
| **PCI-DSS v4.0** | 94/100 | Compliant | [Full Report](./PCI-DSS-UU-PDP-AUDIT-REPORT.md) |
| **UU PDP No. 27/2022** | 96/100 | Compliant | [Full Report](./PCI-DSS-UU-PDP-AUDIT-REPORT.md) |
| **OJK Regulations** | 95/100 | Compliant | [Full Report](./PCI-DSS-UU-PDP-AUDIT-REPORT.md) |
| **Overall** | **95/100** | **Compliant** | - |

---

## Table of Contents

1. [Security Overview](#security-overview)
2. [Data Protection](#data-protection)
3. [Encryption Standards](#encryption-standards)
4. [Authentication & Authorization](#authentication--authorization)
5. [Audit Logging](#audit-logging)
6. [Incident Response](#incident-response)
7. [Compliance Reports](#compliance-reports)
8. [Security Contacts](#security-contacts)

---

## Security Overview

PayU Digital Banking Platform implements defense-in-depth security architecture with multiple layers of protection:

- **Application Security**: Spring Security, OAuth2/OIDC, RBAC
- **Data Security**: Field-level encryption, PII masking, secure key management
- **Infrastructure Security**: OpenShift, mTLS, Network Policies
- **Operational Security**: Audit logging, monitoring, incident response

### Security Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                          │
│  (Next.js Web App, Mobile App, Partner APIs)                │
│  - httpOnly cookies (no JWT in localStorage)                │
│  - CSP headers, XSS protection                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      GATEWAY LAYER                           │
│  (Quarkus API Gateway)                                      │
│  - Rate limiting, WAF                                       │
│  - TLS termination, mTLS                                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    MICROSERVICES LAYER                       │
│  (22 Services: Spring Boot, Quarkus, Python)                │
│  - JWT validation, RBAC (@PreAuthorize)                     │
│  - @Audited for audit logging                               │
│  - @Sensitive for PII masking                               │
│  - EncryptedStringConverter for field encryption            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      DATA LAYER                              │
│  (PostgreSQL, Redis, Kafka)                                 │
│  - AES-256-GCM encryption at rest                           │
│  - TLS 1.3 for data in transit                              │
│  - Field-level encryption for PII                           │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Protection

### PII Classification

| Data Type | Classification | Encryption | Masking | Retention |
|-----------|---------------|------------|---------|-----------|
| **NIK** (National ID) | Critical | AES-256-GCM | `3201********0001` | 5 years post-closure |
| **Card Number** (PAN) | Critical | AES-256-GCM | `1234********5678` | Per PCI-DSS |
| **Email** | High | No | `u***@example.com` | Account lifetime |
| **Phone Number** | High | AES-256-GCM | `+62****1234` | Account lifetime |
| **Full Name** | Medium | No | Partial | Account lifetime |
| **Address** | Medium | No | Partial | Account lifetime |
| **Password/PIN** | Critical | Argon2/BCrypt | `****` | N/A (hashed only) |

### @Sensitive Annotation

Fields annotated with `@Sensitive` are automatically masked in logs:

```java
@Entity
public class Profile {
    @Sensitive
    @Convert(converter = EncryptedStringConverter.class)
    private String nik;  // Masked in logs: 3201********0001

    @Sensitive
    private String fullName;  // Masked in logs
}
```

**Location**: `backend/shared/security-starter/src/main/java/id/payu/security/annotation/Sensitive.java`

---

## Encryption Standards

### Encryption at Rest

- **Algorithm**: AES-256-GCM
- **Key Derivation**: PBKDF2WithHmacSHA256 (600,000 iterations)
- **IV Generation**: SecureRandom (12 bytes, unique per encryption)
- **Key Management**: HashiCorp Vault with auto-rotation

**Implementation**:
```java
@Convert(converter = EncryptedStringConverter.class)
@Column(name = "nik")
private String nik;
```

**Service**: `backend/shared/security-starter/src/main/java/id/payu/security/crypto/EncryptionService.java`

### Encryption in Transit

- **Protocol**: TLS 1.3
- **Certificates**: Let's Encrypt via cert-manager
- **Certificate Rotation**: Automatic (90-day lifecycle)
- **mTLS**: Between microservices (service mesh)

### Key Management

- **Storage**: HashiCorp Vault (dev mode in development)
- **Rotation**: Quarterly key rotation policy
- **Access**: Vault Kubernetes Auth for pod identity

---

## Authentication & Authorization

### JWT Token Handling

**Important**: PayU uses httpOnly cookies for JWT storage, NOT localStorage.

- **Access Token**: Short-lived (15 minutes), httpOnly cookie
- **Refresh Token**: Long-lived (7 days), httpOnly cookie
- **Cookie Flags**: Secure, httpOnly, SameSite=Strict

**BFF Pattern Implementation**:
- `frontend/web-app/src/services/AuthService.ts`
- `frontend/web-app/src/stores/authStore.ts`

### OAuth2/OIDC

- **Provider**: Keycloak (Red Hat SSO)
- **Grant Types**: Authorization Code, Client Credentials
- **Scopes**: `read:account`, `write:transaction`, `admin`, etc.

### RBAC

```java
@PreAuthorize("hasAuthority('SCOPE_account:verify')")
@PostMapping("/verify-nik")
public ResponseEntity<VerifyNikResponse> verifyNik(@RequestBody VerifyNikRequest request) {
    // Only users with account:verify scope can access
}
```

---

## Audit Logging

### @Audited Annotation

All sensitive operations are audited:

```java
@Audited(
    operation = Audited.Operation.TRANSFER,
    entityType = "Transaction",
    maskData = true,
    level = AuditLevel.INFO
)
@PostMapping("/transfer")
public ResponseEntity<InitiateTransferResponse> initiateTransfer(@RequestBody InitiateTransferRequest request) {
    // Automatically audited
}
```

### Audit Event Schema

```json
{
  "eventType": "TRANSFER",
  "operation": "initiateTransfer",
  "entityType": "Transaction",
  "entityId": "txn-uuid",
  "userId": "user-uuid",
  "timestamp": "2026-02-20T10:00:00Z",
  "ipAddress": "client-ip",
  "userAgent": "browser-agent",
  "success": true,
  "context": {
    "amount": "****",
    "recipient": "****"
  }
}
```

### Audit Storage

- **Primary**: Kafka topic (`audit-events`)
- **Secondary**: PostgreSQL (compliance-service)
- **Retention**: 7 years (regulatory requirement)
- **Search**: LokiStack for log aggregation

**Components**:
- `backend/shared/security-starter/src/main/java/id/payu/security/audit/AuditAspect.java`
- `backend/shared/security-starter/src/main/java/id/payu/security/audit/AuditEvent.java`
- `backend/shared/security-starter/src/main/java/id/payu/security/audit/AuditLogPublisher.java`

---

## Incident Response

### Severity Levels

| Level | Response Time | Examples |
|-------|--------------|----------|
| **P0 - Critical** | 15 minutes | Data breach, system compromise, ransomware |
| **P1 - High** | 1 hour | Account compromise, service disruption |
| **P2 - Medium** | 4 hours | Security misconfiguration, failed controls |
| **P3 - Low** | 24 hours | Policy violations, best practice gaps |

### Incident Response Procedures

See [SECURITY_RUNBOOK.md](./SECURITY_RUNBOOK.md) for detailed procedures.

Quick reference:
```bash
# Isolate affected systems
oc scale deployment <service> --replicas=0

# Enable maintenance mode
oc annotate route <service> maintenance="true"

# Stop data exfiltration
oc patch networkpolicy deny-all-egress -p '{"spec":{"policyTypes":["Egress"]}}'

# Enable enhanced logging
oc set env deployment/<service> LOG_LEVEL=DEBUG
```

### Breach Notification

Per UU PDP requirements:
- **Detection to Assessment**: 24 hours
- **Assessment to Notification**: 72 hours (3x24)
- **Regulator Notification**: OJK within 72 hours
- **Customer Notification**: As required by severity

---

## Compliance Reports

### Available Reports

| Report | Date | Status | Link |
|--------|------|--------|------|
| PCI-DSS & UU PDP Audit | Feb 2026 | Compliant | [Full Report](./PCI-DSS-UU-PDP-AUDIT-REPORT.md) |
| Penetration Testing | Jan 2025 | Passed | [PENTEST_REPORT.md](./PENTEST_REPORT.md) |
| Data Protection Audit | Jan 2025 | Remediated | [DATA_PROTECTION_AUDIT_2025-01-30.md](./audits/DATA_PROTECTION_AUDIT_2025-01-30.md) |
| OJK/BI Regulatory Audit | Jan 2026 | Compliant | [OJK_BI_REGULATORY_AUDIT.md](../compliance/OJK_BI_REGULATORY_AUDIT.md) |

### Audit Scripts

Security verification scripts are available in `scripts/security/`:

```bash
# Verify PII masking implementation
./scripts/security/verify-pii-masking.sh [service-name]

# Check encryption configuration
./scripts/security/check-encryption-config.sh

# Verify audit logging coverage
./scripts/security/audit-logger-verification.sh [service-name]
```

---

## Security Contacts

### Reporting Security Issues

**DO NOT** create public GitHub issues for security vulnerabilities.

Instead, report to:
- **Security Team**: security@payu.co.id
- **CISO**: ciso@payu.co.id
- **Emergency Hotline**: +62-xxx-xxxx-xxxx (24/7)

### Response Timeline

- **Acknowledgment**: Within 24 hours
- **Initial Assessment**: Within 72 hours
- **Fix Timeline**: Based on severity (P0: 24h, P1: 72h, P2: 1 week)

### Security Champions

| Role | Name | Contact |
|------|------|---------|
| CISO | [Name] | ciso@payu.co.id |
| Security Architect | [Name] | security-arch@payu.co.id |
| Compliance Officer | [Name] | compliance@payu.co.id |
| Incident Response Lead | [Name] | incident-response@payu.co.id |

---

## Security Best Practices

### For Developers

1. **Use @Sensitive**: Annotate all PII fields in entities/DTOs
2. **Use @Audited**: Annotate all sensitive operations
3. **Never log sensitive data**: Use masking for logs
4. **Use @PreAuthorize**: Enforce RBAC on all endpoints
5. **Externalize secrets**: Use Vault, never hardcode
6. **Validate input**: Use Jakarta Validation annotations
7. **Use prepared statements**: Prevent SQL injection

### For Operations

1. **Rotate credentials**: Quarterly key rotation
2. **Monitor audit logs**: Daily review of security events
3. **Patch promptly**: Apply security patches within 7 days
4. **Backup regularly**: Test restore procedures monthly
5. **Network segmentation**: Enforce zero-trust networking

---

## Related Documentation

- [PCI-DSS-UU-PDP-AUDIT-REPORT.md](./PCI-DSS-UU-PDP-AUDIT-REPORT.md) - Full compliance audit
- [SECURITY_RUNBOOK.md](./SECURITY_RUNBOOK.md) - Incident response procedures
- [PENTEST_REPORT.md](./PENTEST_REPORT.md) - Penetration testing results
- [DISASTER_RECOVERY.md](../operations/DISASTER_RECOVERY.md) - DR procedures
- [OJK_BI_REGULATORY_AUDIT.md](../compliance/OJK_BI_REGULATORY_AUDIT.md) - Regulatory compliance

---

**Document Owner**: Chief Information Security Officer
**Review Cycle**: Quarterly
**Next Review**: May 20, 2026

---

*This document contains confidential information. Distribution is limited to authorized personnel only.*
