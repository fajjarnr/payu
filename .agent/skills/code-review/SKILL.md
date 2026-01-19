---
name: code-review
description: Code review assistance with linting, style checking, and best practices
---

# Code Review Skill

You are a code review assistant. When reviewing code, follow these steps:

## Review Process

1. **Check Style**: Reference the style guide using `get_skill_reference("code-review", "style-guide.md")`
2. **Run Style Check**: Use `get_skill_script("code-review", "check_style.py")` for automated style checking
3. **Look for Issues**: Identify potential bugs, security issues, and performance problems
4. **Provide Feedback**: Give structured feedback with severity levels

## Feedback Format

- **Critical**: Must fix before merge (security vulnerabilities, bugs that cause crashes)
- **Important**: Should fix, but not blocking (performance issues, code smells)
- **Suggestion**: Nice to have improvements (naming, documentation, minor refactoring)

## Review Checklist

### 1. General Hygiene

- [ ] Code follows naming conventions (camelCase vars, PascalCase classes)
- [ ] No hardcoded secrets, passwords, or API keys
- [ ] Functions are kept small and focused (< 50 lines)
- [ ] Meaningful variable and function names
- [ ] Comments explain "WHY", not "WHAT"

### 2. Architecture & Design (Crucial)

- [ ] **Hexagonal Architecture** (Core Banking):
  - [ ] Domain entities have NO dependency on frameworks/infra
  - [ ] Business logic is inside Domain Services/Use Cases
  - [ ] Interfaces used for Repositories (Ports)
- [ ] **Layered Separation**: No Controller logic in Service layer
- [ ] **Coupling**: Loosely coupled components, Dependency Injection used

### 3. Security (Financial Standard)

- [ ] **Input Validation**: All inputs validated? (JSR-303/380)
- [ ] **Authentication**: `@PreAuthorize` or security checks present?
- [ ] **SQL Injection**: Using JPA/Hibernate parameters (No raw string concat)
- [ ] **PII Safety**: No logging of sensitive data (NIK, card numbers)

### 4. Testing

- [ ] **TDD**: Tests exist for the new logic
- [ ] **Unit Tests**: Mockito used correctly, isolated tests
- [ ] **Integration Cases**: Happy paths AND Edge cases covered
- [ ] **Architecture Tests**: No new ArchUnit violations

### 5. Technology Specifics

- **Spring Boot**:
  - Correct Usage of `@Transactional`
  - Proper Exception Handling (`@ControllerAdvice`)
- **Quarkus**:
  - Native image compatibility checked (no reflection issues)
  - Reactive patterns (`Mutiny`) used where appropriate
- **FastAPI**:
  - Async/await used correctly
  - Pydantic models for request/response bodies

## Actionable Feedback Examples

- **Good**: "This logic in the Controller should be moved to a Service to adhere to clean architecture. It makes testing easier."
- **Bad**: "Move this code."

---

## Financial Services Security Checklist 🔒

### 6. PCI-DSS Compliance (Critical for Payment Systems)

> [!CAUTION]
> These checks are **MANDATORY** for all payment-related code changes.

- [ ] **Cardholder Data Protection**:
  - [ ] No storage of sensitive authentication data (CVV, PIN, magnetic stripe)
  - [ ] Card numbers (PAN) masked when displayed (show only last 4 digits)
  - [ ] PAN encrypted at rest using AES-256
  - [ ] Tokenization used for card storage (prefer token over raw PAN)

- [ ] **Secure Transmission**:
  - [ ] TLS 1.2+ for all external communications
  - [ ] Certificate validation enabled (no `InsecureTrustManager`)
  - [ ] mTLS for inter-service communication

- [ ] **Access Control**:
  - [ ] Unique user IDs for system access
  - [ ] Role-based access control (RBAC) implemented
  - [ ] Principle of least privilege applied

### 7. OJK Compliance (Indonesian Financial Regulations)

- [ ] **Data Localization**: Customer data stored in Indonesian data centers
- [ ] **Transaction Limits**: Enforced per OJK regulations
- [ ] **KYC Requirements**: eKYC verification before account activation
- [ ] **Reporting**: Suspicious transaction flagging implemented

### 8. Secrets Management

> [!IMPORTANT]
> Never hardcode secrets. Always use secret management solutions.

**Approved Secret Storage:**
| Environment | Solution |
|-------------|----------|
| Local Dev | `.env` files (gitignored) |
| OpenShift | OpenShift Secrets / Sealed Secrets |
| Production | HashiCorp Vault |

**Code Checklist:**

- [ ] No hardcoded passwords, API keys, or tokens
- [ ] Secrets loaded from environment variables or secret manager
- [ ] Secret rotation support implemented
- [ ] No secrets in log output
- [ ] No secrets in error messages or stack traces

**Example - Spring Boot:**

```java
// ✅ CORRECT: Use @Value or ConfigurationProperties
@Value("${payment.gateway.api-key}")
private String apiKey;

// ❌ WRONG: Hardcoded secret
private String apiKey = "sk_live_xxxxx";
```

### 9. Audit Logging Requirements

> [!NOTE]
> All financial transactions MUST have complete audit trails.

**Mandatory Audit Events:**
| Event Type | Required Fields |
|------------|-----------------|
| Authentication | userId, timestamp, IP, deviceId, success/failure |
| Authorization | userId, resource, action, decision, timestamp |
| Transaction | txnId, userId, amount, type, status, timestamp |
| Data Access | userId, dataType, operation, timestamp |
| Admin Actions | adminId, action, targetResource, timestamp |

**Audit Log Format (JSON):**

```json
{
  "timestamp": "2026-01-20T00:30:00Z",
  "correlationId": "uuid",
  "eventType": "TRANSACTION",
  "userId": "user-uuid",
  "action": "TRANSFER_INITIATED",
  "resource": "transaction/txn-uuid",
  "outcome": "SUCCESS",
  "metadata": {
    "amount": 100000,
    "currency": "IDR",
    "destinationAccount": "****1234"
  },
  "sourceIp": "192.168.1.1",
  "userAgent": "PayU-Mobile/1.0"
}
```

**Code Checklist:**

- [ ] All financial transactions logged with correlation ID
- [ ] Audit logs immutable (append-only)
- [ ] PII masked in logs (NIK, phone, email partially hidden)
- [ ] Log retention policy: minimum 5 years for financial data
- [ ] Centralized logging (LokiStack/ELK)

### 10. Sensitive Data Handling

**PII Classification:**
| Data Type | Classification | Handling |
|-----------|---------------|----------|
| NIK (ID Number) | High | Encrypted, masked in logs |
| Phone Number | Medium | Partial masking (0812\***_5678) |
| Email | Medium | Partial masking (j_**@mail.com) |
| Card Number | Critical | Tokenized, never stored raw |
| PIN/Password | Critical | Bcrypt/Argon2 hashed only |
| Biometric Data | Critical | Encrypted, isolated storage |

**Code Checklist:**

- [ ] PII encrypted at rest
- [ ] PII masked in all log outputs
- [ ] PII not included in error responses
- [ ] Data retention policies implemented
- [ ] Right to erasure (GDPR-like) supported
