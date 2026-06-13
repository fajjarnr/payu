# PII Classification & Handling Guide

## Data Classification Matrix

### Level 1 - Restricted (Highly Sensitive PII)

| Field | Description | Encryption | Masking | Access Control |
|:------|:------------|:-----------|:--------|:---------------|
| NIK | National ID Number | AES-256-GCM | `••••••••••••3456` | 2FA + Role: COMPLIANCE_OFFICER |
| Card PAN | Credit/Debit Card Number | AES-256-GCM (PCI-DSS) | `•••• •••• •••• 1234` | Role: CARD_ADMIN |
| CVV | Card Verification Value | Never stored | N/A | Never stored |
| PIN | Transaction PIN | bcrypt hash only | N/A | Never logged |
| Biometric | Fingerprint/Face data | AES-256-GCM | N/A | Role: KYC_ADMIN |
| Password | User password | Argon2id hash only | N/A | Never logged |

### Level 2 - Confidential (Sensitive PII)

| Field | Description | Encryption | Masking | Access Control |
|:------|:------------|:-----------|:--------|:---------------|
| Full Name | Customer name | At rest | `J••• D••` | Role: CUSTOMER_SERVICE |
| Phone Number | Mobile number | At rest | `+62•••••••789` | Role: CUSTOMER_SERVICE |
| Email | Email address | At rest | `j•••@g••.com` | Role: CUSTOMER_SERVICE |
| Date of Birth | DOB | At rest | `••/••/1990` | Role: KYC_ADMIN |
| Address | Physical address | At rest | Partial mask | Role: CUSTOMER_SERVICE |
| Mother's Maiden Name | Security question | At rest | Full mask | Role: SECURITY_ADMIN |

### Level 3 - Internal (Business Sensitive)

| Field | Description | Encryption | Masking | Access Control |
|:------|:------------|:-----------|:--------|:---------------|
| Account Balance | Current balance | In transit | N/A (owner visible) | Role: FINANCE_OPS |
| Transaction Amount | Transfer amount | In transit | N/A | Role: FINANCE_OPS |
| Transaction History | Past transactions | In transit | N/A | Role: FINANCE_OPS |
| Credit Score | Internal scoring | At rest | N/A | Role: RISK_ANALYST |
| Fraud Signals | Risk indicators | At rest | N/A | Role: RISK_ANALYST |

### Level 4 - Public

| Field | Description | Encryption | Masking | Access Control |
|:------|:------------|:-----------|:--------|:---------------|
| Product Names | Banking products | In transit | N/A | Public |
| Interest Rates | Published rates | In transit | N/A | Public |
| Branch Locations | Physical locations | In transit | N/A | Public |

---

## Implementation Patterns

### Java - Security Starter Annotations

```java
@Entity
@Table(name = "users")
public class User {
    
    @Id
    private UUID id;
    
    // Level 1 - Encrypted + Full mask
    @Sensitive(level = SensitivityLevel.RESTRICTED, mask = MaskType.FULL)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "nik")
    private String nik;
    
    // Level 2 - Encrypted + Partial mask
    @Sensitive(level = SensitivityLevel.CONFIDENTIAL, mask = MaskType.PARTIAL)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "full_name")
    private String fullName;
    
    // Level 2 - Encrypted + Email mask
    @Sensitive(level = SensitivityLevel.CONFIDENTIAL, mask = MaskType.EMAIL)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "email")
    private String email;
    
    // Level 3 - No encryption, internal only
    @Sensitive(level = SensitivityLevel.INTERNAL)
    @Column(name = "credit_score")
    private Integer creditScore;
}
```

### Python - FastAPI Pydantic Models

```python
from pydantic import BaseModel, Field, validator
from typing import Annotated
import re

class MaskedStr(str):
    """Custom type for masked string output"""
    pass

def mask_email(email: str) -> str:
    if not email or '@' not in email:
        return email
    local, domain = email.split('@')
    return f"{local[0]}{'•' * (len(local) - 1)}@{domain[0]}{'•' * (len(domain) - 1)}"

def mask_phone(phone: str) -> str:
    if not phone or len(phone) < 6:
        return phone
    return f"{phone[:3]}{'•' * (len(phone) - 6)}{phone[-3:]}"

def mask_nik(nik: str) -> str:
    if not nik or len(nik) < 4:
        return '•' * len(nik) if nik else ''
    return f"{'•' * (len(nik) - 4)}{nik[-4:]}"

class UserResponse(BaseModel):
    """Public-facing user response with automatic PII masking"""
    
    id: str
    full_name: Annotated[str, Field(description="Masked name")]
    email: Annotated[str, Field(description="Masked email")]
    phone: Annotated[str, Field(description="Masked phone")]
    
    @validator('full_name', pre=True)
    def mask_name(cls, v):
        if not v:
            return v
        parts = v.split()
        return ' '.join(f"{p[0]}{'•' * (len(p) - 1)}" for p in parts)
    
    @validator('email', pre=True)
    def mask_email_field(cls, v):
        return mask_email(v)
    
    @validator('phone', pre=True)
    def mask_phone_field(cls, v):
        return mask_phone(v)

class UserInternalResponse(UserResponse):
    """Internal response with unmasked data - requires elevated permissions"""
    
    nik: str
    full_name: str  # Override to not mask
    email: str  # Override to not mask
    phone: str  # Override to not mask
```

### Log Masking Configuration

```yaml
# Logback configuration for PII masking
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  
# Custom pattern layout with masking
appender:
  name: MASKED_CONSOLE
  class: ch.qos.logback.core.ConsoleAppender
  encoder:
    class: id.payu.shared.logging.MaskingPatternLayoutEncoder
    patterns:
      # NIK: 16 digits -> mask first 12
      - pattern: '\b(\d{12})(\d{4})\b'
        replacement: '••••••••••••$2'
      # Email
      - pattern: '\b([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\b'
        replacement: '***@***'
      # Phone: +62 or 08 format
      - pattern: '\b(\+62|0)(\d{2,3})(\d+)(\d{3})\b'
        replacement: '$1$2•••••$4'
      # Card PAN
      - pattern: '\b(\d{4})(\d{8,12})(\d{4})\b'
        replacement: '$1••••••••$3'
```

---

## Access Control Matrix

### Role-Based Data Access

| Role | L1 (Restricted) | L2 (Confidential) | L3 (Internal) | L4 (Public) |
|:-----|:----------------|:------------------|:--------------|:------------|
| **CUSTOMER** (Self) | Own data (masked) | Own data | Own data | ✅ |
| **CUSTOMER_SERVICE** | ❌ | View (masked) | View | ✅ |
| **CS_SUPERVISOR** | ❌ | View (unmasked) | View | ✅ |
| **KYC_ADMIN** | View (2FA required) | View/Edit | View | ✅ |
| **COMPLIANCE_OFFICER** | View (2FA + audit) | View | View | ✅ |
| **FINANCE_OPS** | ❌ | ❌ | View/Edit | ✅ |
| **RISK_ANALYST** | View (masked) | View (masked) | View/Edit | ✅ |
| **DATA_ENGINEER** | ❌ (anonymized only) | ❌ (anonymized only) | View | ✅ |
| **SYS_ADMIN** | ❌ | ❌ | ❌ | ✅ |

### Permission Enforcement

```java
@Service
public class DataAccessService {
    
    @PreAuthorize("hasRole('KYC_ADMIN') and hasTwoFactorAuth()")
    public UserFullDetails getRestrictedData(UUID userId) {
        auditLogger.log(AuditEvent.builder()
            .action("ACCESS_RESTRICTED_DATA")
            .subject(userId)
            .accessor(SecurityContext.getCurrentUser())
            .dataLevel("L1_RESTRICTED")
            .build());
            
        return userRepository.findFullDetailsById(userId);
    }
    
    @PreAuthorize("hasAnyRole('CUSTOMER_SERVICE', 'CS_SUPERVISOR')")
    public UserMaskedDetails getConfidentialData(UUID userId) {
        UserDetails details = userRepository.findById(userId);
        
        // CS sees masked, Supervisor sees unmasked
        if (!SecurityContext.hasRole("CS_SUPERVISOR")) {
            return maskingService.mask(details);
        }
        
        return details;
    }
}
```

---

## Compliance Reporting

### Data Inventory Report

```sql
-- Generate data inventory for compliance audit
SELECT 
    table_schema,
    table_name,
    column_name,
    data_type,
    COALESCE(
        (SELECT description FROM column_classifications 
         WHERE schema_name = c.table_schema 
         AND table_name = c.table_name 
         AND column_name = c.column_name),
        'UNCLASSIFIED'
    ) as classification,
    COALESCE(
        (SELECT is_encrypted FROM column_classifications 
         WHERE schema_name = c.table_schema 
         AND table_name = c.table_name 
         AND column_name = c.column_name),
        false
    ) as is_encrypted
FROM information_schema.columns c
WHERE table_schema NOT IN ('pg_catalog', 'information_schema')
ORDER BY table_schema, table_name, ordinal_position;
```

### Access Audit Report

```sql
-- Generate access audit for L1/L2 data
SELECT 
    date_trunc('day', accessed_at) as access_date,
    accessor_id,
    accessor_role,
    data_level,
    action,
    subject_id,
    COUNT(*) as access_count
FROM audit_log
WHERE data_level IN ('L1_RESTRICTED', 'L2_CONFIDENTIAL')
AND accessed_at > NOW() - INTERVAL '30 days'
GROUP BY 1, 2, 3, 4, 5, 6
ORDER BY access_date DESC, access_count DESC;
```
