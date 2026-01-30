# Data Protection Security Remediation Guide

> Quick reference guide for addressing security audit findings
>
> **Last Updated**: 2025-01-30

---

## Priority 1: Critical Fixes (Before Production)

### 1. Remove CVV from Storage

**Files**:
- `/home/ubuntu/payu/backend/wallet-service/src/main/java/id/payu/wallet/domain/model/Card.java`
- `/home/ubuntu/payu/backend/wallet-service/src/main/java/id/payu/wallet/adapter/persistence/entity/CardEntity.java`

**Action**:
```java
// REMOVE these fields entirely:
private String cvv;  // DELETE
public String getCvv() { return cvv; }  // DELETE
public void setCvv(String cvv) { this.cvv = cvv; }  // DELETE
```

**Why**: PCI-DSS prohibits CVV storage after authorization

---

### 2. Mask Card Numbers in API Responses

**File**: `/home/ubuntu/payu/backend/wallet-service/src/main/java/id/payu/wallet/dto/CardResponse.java`

**Action**:
```java
public class CardResponse {
    @JsonIgnore
    private String cardNumber;

    @JsonProperty("cardNumber")
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    public String getRawCardNumber() {
        return cardNumber;  // For internal use only
    }
}
```

**Why**: PCI-DSS Requirement 3.3 - Display PAN with only last 4 digits

---

### 3. Mask NIK in KYC Responses

**File**: `/home/ubuntu/payu/backend/kyc-service/src/app/models/schemas.py`

**Action**:
```python
def mask_nik(nik: str) -> str:
    """Mask NIK showing first 4 and last 4 digits"""
    if not nik or len(nik) != 16:
        return "****"
    return f"{nik[:4]}******{nik[-4:]}"

class GetKycStatusResponse(BaseModel):
    # ... other fields ...
    ktp_ocr_result: Optional[KtpOcrResult] = None

    def model_dump(self, **kwargs):
        data = super().model_dump(**kwargs)
        if data.get("ktp_ocr_result") and data["ktp_ocr_result"].get("nik"):
            data["ktp_ocr_result"]["nik"] = mask_nik(data["ktp_ocr_result"]["nik"])
        return data
```

**Why**: OJK compliance - protect PII in API responses

---

## Priority 2: High Priority (Within 30 Days)

### 4. Implement Field-Level Encryption for NIK

**Step 1**: Create encryption converter
```java
// /home/ubuntu/payu/backend/account-service/src/main/java/id/payu/account/config/NikEncryptionConverter.java
@Converter
public class NikEncryptionConverter implements AttributeConverter<String, String> {

    private final EncryptionService encryptionService;

    public NikEncryptionConverter(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : encryptionService.encryptForDatabase(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : encryptionService.decryptFromDatabase(dbData);
    }
}
```

**Step 2**: Apply to entity
```java
// /home/ubuntu/payu/backend/account-service/src/main/java/id/payu/account/entity/Profile.java
@Column(name = "nik", unique = true, length = 16)
@Convert(converter = NikEncryptionConverter.class)
private String nik;
```

---

### 5. Implement Card Number Encryption

**Step 1**: Create card encryption converter
```java
// /home/ubuntu/payu/backend/wallet-service/src/main/java/id/payu/wallet/config/CardNumberEncryptionConverter.java
@Converter
public class CardNumberEncryptionConverter implements AttributeConverter<String, String> {
    // Similar to NikEncryptionConverter
}
```

**Step 2**: Apply to entity
```java
// /home/ubuntu/payu/backend/wallet-service/src/main/java/id/payu/wallet/adapter/persistence/entity/CardEntity.java
@Column(name = "card_number_encrypted", nullable = false)
@Convert(converter = CardNumberEncryptionConverter.class)
private String cardNumber;
```

---

### 6. Remove Default Passwords

**Action**: Update all application.yml files

**Before**:
```yaml
datasource:
  password: ${DB_PASSWORD:postgres}  # REMOVE DEFAULT
```

**After**:
```yaml
datasource:
  password: ${DB_PASSWORD}  # No default - must be set
```

**Files to update**:
- `/home/ubuntu/payu/backend/backoffice-service/src/main/resources/application.yml`
- `/home/ubuntu/payu/backend/billing-service/src/main/resources/application.yml`
- `/home/ubuntu/payu/backend/partner-service/src/main/resources/application.yml`
- `/home/ubuntu/payu/backend/notification-service/src/main/resources/application.yml`
- `/home/ubuntu/payu/backend/promotion-service/src/main/resources/application.yml`

---

### 7. Add @Sensitive Annotation

**Step 1**: Implement @Sensitive annotation in security-starter
```java
// /home/ubuntu/payu/backend/shared/security-starter/src/main/java/id/payu/security/annotation/Sensitive.java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
    SensitiveFieldType fieldType();
    enum SensitiveFieldType {
        NIK, CARD_NUMBER, CVV, PHONE, EMAIL, PASSWORD
    }
}
```

**Step 2**: Add to entity fields
```java
// /home/ubuntu/payu/backend/account-service/src/main/java/id/payu/account/entity/Profile.java
@Sensitive(fieldType = Sensitive.SensitiveFieldType.NIK)
@Column(name = "nik", unique = true, length = 16)
private String nik;
```

---

### 8. Configure Logback Masking

**Action**: Add to each service's `logback-spring.xml`:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>
                %d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %replace(%msg)('(\\d{3})\\d{4,}(\\d{3})', '$1****$2')%n
            </pattern>
        </encoder>
    </appender>

    <!-- Or use the filter -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="id.payu.security.masking.LogbackMaskingFilter" />
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
</configuration>
```

---

## Priority 3: Medium Priority (Within 60 Days)

### 9. Add PII Access Audit Logging

**File**: `/home/ubuntu/payu/backend/account-service/src/main/java/id/payu/account/adapter/persistence/UserPersistenceAdapter.java`

**Action**:
```java
@Service
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final DataAccessAuditService auditService;  // ADD THIS

    @Override
    @Audited(
        operation = Audited.Operation.READ,
        entityType = "UserProfile",
        maskData = true
    )
    public Optional<User> findById(UUID id) {
        // Log PII access
        auditService.logDataAccess(
            id.toString(),
            getCurrentUserId(),
            "account-service",
            "UserProfile",
            "NIK,PhoneNumber,Email",
            DataOperationType.READ,
            "Profile retrieval"
        );

        return userRepository.findById(id).map(this::toDomain);
    }
}
```

---

### 10. Integrate KYC Audit Logging

**File**: `/home/ubuntu/payu/backend/kyc-service/src/app/api/v1/kyc.py`

**Action**:
```python
@kyc_router.get("/verify/{verification_id}")
async def get_kyc_status(
    request: Request,
    verification_id: str,
    db: AsyncSession = Depends(get_db_session)
):
    """Get KYC verification status by ID with audit logging."""
    log = logger.bind(verification_id=verification_id)

    try:
        service = KycService(db)
        verification = await service.get_verification(verification_id)

        # AUDIT: Log PII access
        # TODO: Integrate with compliance-service gRPC/REST API
        await audit_log_data_access(
            user_id=verification.user_id,
            resource_type="KYCVerification",
            resource_id=verification_id,
            operation="READ",
            accessed_by=get_current_user_id(request),
            ip_address=request.client.host,
            user_agent=request.headers.get("user-agent")
        )

        # ... rest of code
```

---

## Testing Checklist

After implementing fixes, verify:

- [ ] CVV is removed from Card model and database
- [ ] Card numbers are masked in all API responses
- [ ] NIK is masked in KYC API responses
- [ ] NIK is encrypted in database (verify raw data)
- [ ] Card numbers are encrypted in database
- [ ] No default passwords in any application.yml
- [ ] PII fields have @Sensitive annotations
- [ ] Log masking is working (check logs)
- [ ] PII access is logged in audit tables
- [ ] Encryption keys are managed securely
- [ ] Key rotation procedure is documented

---

## Verification Commands

```bash
# Check for remaining CVV references
grep -r "cvv" backend/wallet-service/src/main/java/ --include="*.java"

# Check for card number in responses
grep -r "cardNumber" backend/wallet-service/src/main/java/ --include="*.java" -A 2

# Check for default passwords
grep -r "password.*:" backend/*/src/main/resources/application.yml | grep -v "password: \$"

# Verify @Sensitive annotation usage
grep -r "@Sensitive" backend/*/src/main/java/ --include="*.java"
```

---

## Resources

- PCI-DSS v4.0: https://www.pcisecuritystandards.org/
- OJK Regulations: https://www.ojk.go.id/
- security-starter: `/home/ubuntu/payu/backend/shared/security-starter/`
- Compliance service: `/home/ubuntu/payu/backend/compliance-service/`
