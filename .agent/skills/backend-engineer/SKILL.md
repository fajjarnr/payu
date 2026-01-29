---
name: backend-engineer
description: Expert Backend Engineer for PayU Digital Banking Platform - specializing in Spring Boot 3.4, Quarkus Native, FastAPI, Hexagonal Architecture, and enterprise-grade microservices development.
---

# Senior Backend Engineer Skill

Complete toolkit for building enterprise-grade backend services on PayU Digital Banking Platform with Spring Boot 3.4, Quarkus Native, and FastAPI.

## Related Resources

| Resource | Path |
|----------|------|
| PayU Development Skill | `.agent/skills/payu-development/SKILL.md` |
| PayU Development Standards | `docs/guides/GEMINI.md` |
| API Design | `.agent/skills/api-design/SKILL.md` |
| Backend Patterns | `.agent/skills/backend-patterns/SKILL.md` |
| Event-Driven Architecture | `.agent/skills/event-driven-architecture/SKILL.md` |
| Database Engineer | `.agent/skills/database-engineer/SKILL.md` |
| Security Engineer | `.agent/skills/security-engineer/SKILL.md` |
| Error Handling Engineer | `.agent/skills/error-handling-engineer/SKILL.md` |
| FastAPI Templates | `.agent/skills/fastapi-templates/SKILL.md` |
| Container Engineer | `.agent/skills/container-engineer/SKILL.md` |

## PayU Technology Stack

### Core Services (Spring Boot 3.4 + Java 21)
- **account-service**: User accounts, profile, multi-pocket
- **auth-service**: Authentication, Risk-based MFA, Biometrics
- **transaction-service**: Transfers, BI-FAST, QRIS, Sharding
- **wallet-service**: Double-entry ledger, balance management
- **investment-service**: Mutual funds, Gold, Robo-advisory
- **lending-service**: Loans, PayLater, Credit Scoring
- **fx-service**: Currency exchange rates & conversion
- **statement-service**: PDF E-Statement generation
- **backoffice-service**: Internal admin dashboard, audit
- **partner-service**: Partner integration & management
- **promotion-service**: Promo campaigns, vouchers, rewards
- **support-service**: Customer support, ticketing
- **compliance-service**: Regulatory compliance, AML
- **cms-service**: Banners, Promos, Dynamic Content
- **ab-testing-service**: UI/Feature experimentation

### Supporting Services (Quarkus 3.x Native)
- **billing-service**: Bill payments (PLN, PDAM, etc)
- **notification-service**: Push, SMS, Email, WhatsApp
- **gateway-service**: API Gateway, Rate limiting
- **api-portal-service**: Centralized OpenAPI Docs & Sandbox

### ML/Analytics Services (Python FastAPI)
- **kyc-service**: OCR, Liveness Detection
- **analytics-service**: Fraud Scoring, User Insights

### Shared Libraries (backend/shared/)
- **security-starter**: Field encryption, Data masking, Audit logging
- **resilience-starter**: Circuit Breaker, Retry, Bulkhead (Resilience4j)
- **cache-starter**: Multi-layer caching (Redis + Caffeine)

## Architecture Patterns

### 1. Hexagonal Architecture (Ports & Adapters)

PayU services MUST follow Hexagonal Architecture pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Service   │  │  Use Case   │  │  Application Service│  │
│  │             │  │   Port In   │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Entity    │  │  Value Obj  │  │    Domain Event     │  │
│  │             │  │             │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐                            │
│  │  Repository │  │   Service   │                            │
│  │   Port Out  │  │   Port In   │                            │
│  └─────────────┘  └─────────────┘                            │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   JPA Repo  │  │  REST Ctrl  │  │   Kafka Producer    │  │
│  │  (Adapter)  │  │  (Adapter)  │  │     (Adapter)       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Ext Client  │  │   Config    │  │    Exception Hdlr   │  │
│  │  (Adapter)  │  │             │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**Package Structure:**
```
src/main/java/id/payu/{service}/
├── domain/
│   ├── model/           # Entities, Value Objects
│   ├── port/
│   │   ├── in/          # Input ports (Use Cases)
│   │   └── out/         # Output ports (Repository, External)
│   ├── event/           # Domain events
│   └── exception/       # Domain exceptions
├── application/
│   ├── service/         # Application services
│   └── dto/             # Application DTOs
├── adapter/
│   ├── persistence/     # JPA entities, repositories
│   ├── web/             # REST controllers
│   ├── messaging/       # Kafka producers/consumers
│   └── client/          # External service clients
└── config/              # Configuration classes
```

### 2. DTO First Approach

Define DTO/Request/Response in `interfaces.dto` package BEFORE implementation:

```java
// interfaces/dto/CreateAccountRequest.java
public record CreateAccountRequest(
    @NotBlank(message = "ACC_001: Full name is required")
    @Size(max = 100)
    String fullName,
    
    @NotBlank(message = "ACC_002: Email is required")
    @Email
    String email,
    
    @NotBlank(message = "ACC_003: Phone number is required")
    @Pattern(regexp = "^08[0-9]{8,11}$", message = "ACC_004: Invalid Indonesian phone number")
    String phoneNumber,
    
    @NotNull(message = "ACC_005: Account type is required")
    AccountType accountType
) {}

// interfaces/dto/AccountResponse.java
public record AccountResponse(
    String accountId,
    String accountNumber,
    String fullName,
    String email,
    String phoneNumber,
    AccountStatus status,
    BigDecimal balance,
    Instant createdAt
) {}
```

### 3. Port-Adapter Interface Pattern

All external communication MUST go through Port interfaces in domain layer:

```java
// domain/port/out/AccountRepositoryPort.java
public interface AccountRepositoryPort {
    Account save(Account account);
    Optional<Account> findById(AccountId id);
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByUserId(UserId userId);
    boolean existsByEmail(String email);
}

// domain/port/out/WalletServicePort.java
public interface WalletServicePort {
    WalletBalance getBalance(AccountId accountId);
    TransactionResult debit(AccountId accountId, Money amount, String idempotencyKey);
    TransactionResult credit(AccountId accountId, Money amount, String idempotencyKey);
}

// domain/port/in/CreateAccountUseCase.java
public interface CreateAccountUseCase {
    Account createAccount(CreateAccountCommand command);
}
```

### 4. Error Handling with Business Exceptions

Use `GlobalExceptionHandler` and custom `BusinessException` with unique error codes:

```java
// domain/exception/BusinessException.java
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;
    
    public BusinessException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}

// Usage examples:
// ACC_001 - ACC_099: Account Service
// TXN_001 - TXN_099: Transaction Service
// WAL_001 - WAL_099: Wallet Service
// BIF_001 - BIF_099: BI-FAST
// QRS_001 - QRS_099: QRIS
// LND_001 - LND_099: Lending
// INV_001 - INV_099: Investment
// KYC_001 - KYC_099: KYC
// AUTH_001 - AUTH_099: Authentication
```

## Spring Boot 3.4 Patterns

### 1. Service Implementation with Shared Starters

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountApplicationService implements CreateAccountUseCase, GetAccountUseCase {
    
    private final AccountRepositoryPort accountRepository;
    private final WalletServicePort walletService;
    private final EventPublisherPort eventPublisher;
    private final IdempotencyKeyValidator idempotencyValidator;
    
    @Transactional
    @CircuitBreaker(name = "account-creation", fallbackMethod = "createAccountFallback")
    @Retry(name = "account-creation")
    @AuditLog(operation = "CREATE_ACCOUNT")
    public Account createAccount(CreateAccountCommand command) {
        // Validate idempotency
        idempotencyValidator.validate(command.idempotencyKey());
        
        // Check duplicates
        if (accountRepository.existsByEmail(command.email())) {
            throw new BusinessException("ACC_010", "Email already registered", HttpStatus.CONFLICT);
        }
        
        // Create account
        Account account = Account.create(command);
        Account saved = accountRepository.save(account);
        
        // Publish event
        eventPublisher.publish(new AccountCreatedEvent(saved.getId(), saved.getEmail()));
        
        log.info("Account created: {}", saved.getId());
        return saved;
    }
    
    private Account createAccountFallback(CreateAccountCommand command, Exception ex) {
        log.error("Account creation failed, queueing for retry: {}", command.email());
        // Queue for async processing
        throw new ServiceUnavailableException("ACC_503", "Service temporarily unavailable");
    }
}
```

### 2. REST Controller with Validation

```java
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Validated
@Tag(name = "Account Management", description = "Account operations")
public class AccountController {
    
    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;
    
    @PostMapping
    @Operation(summary = "Create new account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created"),
        @ApiResponse(responseCode = "409", description = "Email already exists"),
        @ApiResponse(responseCode = "503", description = "Service unavailable")
    })
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        
        CreateAccountCommand command = CreateAccountCommand.from(request, idempotencyKey);
        Account account = createAccountUseCase.createAccount(command);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(AccountMapper.toResponse(account));
    }
    
    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        Account account = getAccountUseCase.getAccount(new AccountId(accountId));
        return ResponseEntity.ok(AccountMapper.toResponse(account));
    }
}
```

### 3. JPA Adapter Implementation

```java
@Repository
@RequiredArgsConstructor
public class AccountJpaAdapter implements AccountRepositoryPort {
    
    private final AccountJpaRepository jpaRepository;
    private final AccountEntityMapper mapper;
    
    @Override
    @Transactional
    public Account save(Account account) {
        AccountEntity entity = mapper.toEntity(account);
        AccountEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findById(AccountId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain);
    }
}

@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_accounts_email", columnList = "email", unique = true),
    @Index(name = "idx_accounts_user_id", columnList = "user_id")
})
public class AccountEntity {
    @Id
    private String id;
    
    @Column(nullable = false, unique = true)
    private String accountNumber;
    
    @Column(nullable = false)
    private String fullName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    @Convert(converter = PhoneMaskingConverter.class)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;
    
    @Version
    private Long version;
    
    @Column(updatable = false)
    private Instant createdAt;
    
    private Instant updatedAt;
}
```

## Quarkus Native Patterns

### 1. Native Image Configuration

```java
// billing-service native configuration
@RegisterForReflection({
    BillingRequest.class,
    BillingResponse.class,
    PaymentStatus.class
})
public class NativeConfig {}

// application.properties
quarkus.native.additional-build-args=-H:+ReportExceptionStackTraces,-H:Log=registerResource:
quarkus.native.resources.includes=db/migration/**
quarkus.datasource.db-kind=postgresql
quarkus.hibernate-orm.database.generation=validate
quarkus.flyway.migrate-at-start=true
```

### 2. Reactive Programming with Mutiny

```java
@ApplicationScoped
public class NotificationService {
    
    @Inject
    NotificationRepository repository;
    
    @Inject
    @RestClient
    SmsGatewayClient smsClient;
    
    public Uni<NotificationResult> sendNotification(NotificationRequest request) {
        return repository.persist(NotificationEntity.from(request))
            .chain(entity -> sendToGateway(request))
            .onItem().transform(result -> NotificationResult.success(result))
            .onFailure().recoverWithItem(e -> NotificationResult.failed(e.getMessage()));
    }
    
    private Uni<GatewayResponse> sendToGateway(NotificationRequest request) {
        return switch (request.channel()) {
            case SMS -> smsClient.send(request);
            case EMAIL -> emailClient.send(request);
            case PUSH -> pushClient.send(request);
            case WHATSAPP -> whatsappClient.send(request);
        };
    }
}
```

## FastAPI Patterns (KYC & Analytics)

### 1. Service Structure

```python
# kyc-service structure
kyc-service/
├── src/
│   ├── app/
│   │   ├── __init__.py
│   │   ├── main.py              # FastAPI app
│   │   ├── config.py            # Pydantic settings
│   │   ├── dependencies.py      # FastAPI dependencies
│   │   ├── api/
│   │   │   ├── __init__.py
│   │   │   ├── v1/
│   │   │   │   ├── __init__.py
│   │   │   │   ├── router.py
│   │   │   │   └── endpoints/
│   │   │   │       ├── ocr.py
│   │   │   │       └── liveness.py
│   │   ├── core/
│   │   │   ├── __init__.py
│   │   │   ├── models.py        # Pydantic models
│   │   │   ├── exceptions.py    # Custom exceptions
│   │   │   └── security.py      # JWT, encryption
│   │   ├── services/
│   │   │   ├── __init__.py
│   │   │   ├── ocr_service.py
│   │   │   └── liveness_service.py
│   │   └── infrastructure/
│   │       ├── __init__.py
│   │       ├── ml/
│   │       │   └── ocr_model.py
│   │       └── storage/
│   │           └── s3_client.py
│   └── tests/
├── pyproject.toml
└── Dockerfile
```

### 2. FastAPI Endpoint with Error Handling

```python
# app/api/v1/endpoints/ocr.py
from fastapi import APIRouter, UploadFile, File, Depends, HTTPException
from app.core.models import OCRRequest, OCRResponse, ErrorResponse
from app.services.ocr_service import OCRService
from app.dependencies import get_ocr_service, verify_jwt

router = APIRouter(prefix="/ocr", tags=["OCR"])

@router.post(
    "/ktp",
    response_model=OCRResponse,
    responses={
        400: {"model": ErrorResponse, "description": "Invalid image"},
        422: {"model": ErrorResponse, "description": "OCR failed"},
        500: {"model": ErrorResponse, "description": "Internal error"}
    }
)
async def extract_ktp(
    file: UploadFile = File(..., description="KTP image (JPG/PNG)"),
    service: OCRService = Depends(get_ocr_service),
    user_id: str = Depends(verify_jwt)
) -> OCRResponse:
    """
    Extract KTP (Indonesian ID Card) information from uploaded image.
    
    Returns structured data including NIK, name, address, and other fields.
    All sensitive data is encrypted at rest.
    """
    try:
        result = await service.extract_ktp(file, user_id)
        return OCRResponse(
            success=True,
            data=result,
            request_id=generate_request_id()
        )
    except InvalidImageError as e:
        raise HTTPException(status_code=400, detail=f"KYC_001: {e.message}")
    except OCRExtractionError as e:
        raise HTTPException(status_code=422, detail=f"KYC_002: {e.message}")
    except Exception as e:
        logger.error(f"OCR processing failed: {e}")
        raise HTTPException(status_code=500, detail="KYC_500: Internal processing error")
```

## Shared Starters Usage

### 1. Security Starter

```java
// Add dependency
<dependency>
    <groupId>id.payu.shared</groupId>
    <artifactId>security-starter</artifactId>
    <version>${payu.shared.version}</version>
</dependency>

// Usage
@Entity
public class Customer {
    @Id
    private String id;
    
    @Sensitive(type = SensitiveType.NIK)
    private String nationalId;
    
    @Sensitive(type = SensitiveType.PHONE)
    private String phoneNumber;
    
    @Sensitive(type = SensitiveType.EMAIL)
    private String email;
}

// Automatic masking in logs
log.info("Customer created: {}", customer); 
// Output: Customer created: Customer(id=123, nationalId=***5678, phone=****1234, email=***@gmail.com)
```

### 2. Resilience Starter

```java
// Add dependency
<dependency>
    <groupId>id.payu.shared</groupId>
    <artifactId>resilience-starter</artifactId>
    <version>${payu.shared.version}</version>
</dependency>

// Configuration
resilience:
  circuit-breaker:
    configs:
      default:
        failure-rate-threshold: 50
        slow-call-rate-threshold: 80
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 30s
  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2

// Usage
@Service
public class PaymentService {
    
    @CircuitBreaker(name = "external-payment", fallbackMethod = "fallback")
    @Retry(name = "external-payment")
    @TimeLimiter(name = "external-payment")
    @Bulkhead(name = "external-payment")
    public PaymentResult processExternalPayment(PaymentRequest request) {
        // External API call
    }
}
```

### 3. Cache Starter

```java
// Add dependency
<dependency>
    <groupId>id.payu.shared</groupId>
    <artifactId>cache-starter</artifactId>
    <version>${payu.shared.version}</version>
</dependency>

// Configuration
cache:
  multi-layer:
    enabled: true
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
    redis:
      time-to-live: 30m

// Usage
@Service
public class FxRateService {
    
    @Cacheable(value = "fx-rates", key = "#currencyPair")
    public FxRate getRate(String currencyPair) {
        // Fetch from external provider
    }
    
    @CacheEvict(value = "fx-rates", key = "#currencyPair")
    public void invalidateRate(String currencyPair) {
        // Invalidate cache
    }
}
```

## Testing Guidelines

### 1. Unit Tests with JUnit 5 & Mockito

```java
@ExtendWith(MockitoExtension.class)
class AccountApplicationServiceTest {
    
    @Mock
    private AccountRepositoryPort accountRepository;
    
    @Mock
    private EventPublisherPort eventPublisher;
    
    @InjectMocks
    private AccountApplicationService service;
    
    @Test
    void shouldCreateAccountSuccessfully() {
        // Given
        CreateAccountCommand command = new CreateAccountCommand(
            "John Doe", "john@example.com", "08123456789", AccountType.SAVINGS, "idempotency-key"
        );
        
        when(accountRepository.existsByEmail(any())).thenReturn(false);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        
        // When
        Account result = service.createAccount(command);
        
        // Then
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(eventPublisher).publish(any(AccountCreatedEvent.class));
    }
    
    @Test
    void shouldThrowWhenEmailExists() {
        // Given
        CreateAccountCommand command = new CreateAccountCommand(
            "John Doe", "john@example.com", "08123456789", AccountType.SAVINGS, "idempotency-key"
        );
        
        when(accountRepository.existsByEmail(any())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> service.createAccount(command))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", "ACC_010");
    }
}
```

### 2. Architecture Tests with ArchUnit

```java
@AnalyzeClasses(packages = "id.payu.account")
class ArchitectureTest {
    
    @ArchTest
    static final ArchRule domainShouldNotDependOnOtherLayers = 
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..application..", "..adapter..", "..config..");
    
    @ArchTest
    static final ArchRule applicationShouldOnlyDependOnDomain = 
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter..", "..config..");
    
    @ArchTest
    static final ArchRule adaptersShouldDependOnDomain = 
        classes()
            .that().resideInAPackage("..adapter..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "..application..", "java..", "javax..", "org.springframework..");
    
    @ArchTest
    static final ArchRule useCasesShouldBeInterfaces = 
        classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .should().beInterfaces();
}
```

### 3. Integration Tests with Testcontainers

```java
@SpringBootTest
@Testcontainers
class AccountIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("payu_test")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateAccountAndPublishEvent() {
        // Given
        CreateAccountRequest request = new CreateAccountRequest(
            "John Doe", "john@example.com", "08123456789", AccountType.SAVINGS
        );
        
        // When
        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
            "/api/v1/accounts", request, AccountResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("john@example.com");
    }
}
```

## Security & Compliance

### 1. PII Protection

```java
@Service
public class CustomerService {
    
    @SensitiveDataHandler
    public CustomerResponse getCustomer(String customerId) {
        Customer customer = repository.findById(customerId)
            .orElseThrow(() -> new NotFoundException("ACC_404", "Customer not found"));
        
        // NIK, phone, email automatically masked in logs
        log.info("Retrieved customer: {}", customer);
        
        // Return masked response for non-admin users
        return CustomerResponse.builder()
            .id(customer.getId())
            .name(customer.getName())
            .email(maskEmail(customer.getEmail()))
            .phone(maskPhone(customer.getPhone()))
            .build();
    }
    
    private String maskEmail(String email) {
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }
    
    private String maskPhone(String phone) {
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 3);
    }
}
```

### 2. Idempotency Implementation

```java
@Component
@RequiredArgsConstructor
public class IdempotencyKeyValidator {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);
    
    public void validate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException("GEN_001", "Idempotency-Key header is required", HttpStatus.BAD_REQUEST);
        }
        
        String key = KEY_PREFIX + idempotencyKey;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "processing", TTL);
        
        if (Boolean.FALSE.equals(isNew)) {
            String status = redisTemplate.opsForValue().get(key);
            if ("completed".equals(status)) {
                throw new BusinessException("GEN_002", "Request already processed", HttpStatus.CONFLICT);
            }
            throw new BusinessException("GEN_003", "Request is being processed", HttpStatus.CONFLICT);
        }
    }
    
    public void markCompleted(String idempotencyKey) {
        redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, "completed", TTL);
    }
}
```

## Development Workflow

### 1. Creating a New Service

Follow the workflow in `.agent/workflows/new-service-scaffolding/SKILL.md`:

```bash
# 1. Use the scaffolding workflow
# Read: .agent/workflows/new-service-scaffolding/SKILL.md

# 2. Generate service structure
mvn archetype:generate \
  -DarchetypeGroupId=id.payu \
  -DarchetypeArtifactId=payu-service-archetype \
  -DarchetypeVersion=1.0.0 \
  -DgroupId=id.payu \
  -DartifactId={service-name} \
  -Dversion=1.0.0-SNAPSHOT

# 3. Add shared starters
# Edit pom.xml to include security-starter, resilience-starter, cache-starter

# 4. Implement hexagonal architecture
# Follow package structure: domain/, application/, adapter/, config/

# 5. Add tests
# Unit tests: 100% logic coverage
# ArchUnit: Layering validation
# Integration tests: Testcontainers (if applicable)
```

### 2. Database Migration with Flyway

```sql
-- V1__Create_schema.sql
CREATE TABLE accounts (
    id VARCHAR(36) PRIMARY KEY,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(15) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    account_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_status ON accounts(status);

-- V2__Add_tenant_support.sql
ALTER TABLE accounts ADD COLUMN tenant_id VARCHAR(36);
CREATE INDEX idx_accounts_tenant_id ON accounts(tenant_id);

-- V3__Create_materialized_views.sql
CREATE MATERIALIZED VIEW account_summary AS
SELECT 
    tenant_id,
    status,
    COUNT(*) as count,
    DATE_TRUNC('day', created_at) as date
FROM accounts
GROUP BY tenant_id, status, DATE_TRUNC('day', created_at);

CREATE INDEX idx_account_summary_tenant ON account_summary(tenant_id);
```

### 3. Build Commands

```bash
# Build specific service
cd backend/account-service
mvn clean package -DskipTests -T 1C

# Build all services
cd backend
mvn clean package -DskipTests -T 1C

# Run tests
mvn test

# Run integration tests
mvn verify -P integration-test

# Build Docker image
docker build -t payu/account-service:latest .

# Run locally
docker-compose up -d
```

## Best Practices Summary

### Code Quality
- Follow Hexagonal Architecture strictly
- Use shared starters for cross-cutting concerns
- Implement 100% unit test coverage for logic
- Add ArchUnit tests for architecture validation
- Document all public APIs with OpenAPI

### Performance
- Use multi-layer caching (Redis + Caffeine)
- Implement database query optimization
- Use connection pooling (HikariCP)
- Enable async processing for non-critical operations
- Monitor with distributed tracing

### Security
- Never log sensitive data (use @Sensitive annotation)
- Implement idempotency for all critical operations
- Use field-level encryption for PII
- Validate all inputs with Bean Validation
- Implement proper RBAC and audit logging

### Maintainability
- Use semantic versioning
- Follow conventional commits
- Write comprehensive README for each service
- Keep dependencies updated
- Document architectural decisions in ADRs

---

**Note**: This skill works in conjunction with other PayU skills. For frontend development, use `@frontend-engineer`. For database tasks, use `@database-engineer`. For security audits, use `@security-engineer`.
