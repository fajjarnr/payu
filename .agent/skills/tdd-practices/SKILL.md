---
name: tdd-practices
description: Test-Driven Development practices for PayU Digital Banking Platform - ensuring high-quality, error-free code through comprehensive testing strategies.
---

# PayU TDD Practices Skill

You are a TDD expert for the **PayU Digital Banking Platform**. Your role is to ensure all code follows strict TDD practices to prevent errors during development and maintain high code quality.

## 🎯 Core Philosophy

> **"Write tests first, code second, refactor always"**

The goal is to **prevent errors before they occur** through:
1. Interface-first design
2. Comprehensive test coverage
3. Pre-commit validation
4. Architecture enforcement

---

## 📐 Test Pyramid

PayU follows the **Test Pyramid** principle:

```
         ╱╲
        ╱  ╲         10% - E2E Tests (Slow, Expensive)
       ╱────╲
      ╱      ╲       20% - Integration Tests (Medium)
     ╱────────╲
    ╱          ╲     70% - Unit Tests (Fast, Cheap)
   ╱────────────╲
```

| Test Type | Percentage | Speed | Dependencies |
|-----------|------------|-------|--------------|
| **Unit Tests** | 70% | < 100ms | None (Mocked) |
| **Integration Tests** | 20% | < 30s | Testcontainers |
| **E2E Tests** | 10% | < 5min | Full Stack |

---

## 🔴🟢🔵 TDD Cycle (Red-Green-Refactor)

### Step 1: RED - Write Failing Test First

```java
@Test
void shouldTransferMoneyBetweenAccounts() {
    // Given
    var sourceAccount = new Account("ACC-001", new BigDecimal("1000.00"));
    var targetAccount = new Account("ACC-002", BigDecimal.ZERO);
    
    // When
    var result = transferService.transfer(sourceAccount, targetAccount, new BigDecimal("500.00"));
    
    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(sourceAccount.getBalance()).isEqualTo(new BigDecimal("500.00"));
    assertThat(targetAccount.getBalance()).isEqualTo(new BigDecimal("500.00"));
}
```

**⚠️ Test MUST fail first!** If it passes immediately, either:
- The feature already exists, or
- The test is wrong

### Step 2: GREEN - Write Minimal Code to Pass

```java
public TransferResult transfer(Account source, Account target, BigDecimal amount) {
    source.debit(amount);
    target.credit(amount);
    return TransferResult.success();
}
```

**Rules:**
- Write the **simplest code** that makes the test pass
- Don't over-engineer
- Don't add "future" features

### Step 3: REFACTOR - Clean Up

```java
public TransferResult transfer(Account source, Account target, BigDecimal amount) {
    validateTransfer(source, target, amount);
    
    source.debit(amount);
    target.credit(amount);
    
    publishEvent(new TransferCompletedEvent(source.getId(), target.getId(), amount));
    
    return TransferResult.success();
}
```

**Rules:**
- Keep tests green while refactoring
- Extract methods, rename variables
- Apply design patterns if needed

---

## 🏗️ Test Structure for PayU

### Directory Layout

```
src/
├── main/java/id/payu/{service}/
│   ├── domain/              # Domain entities, value objects
│   ├── application/         # Use cases, ports (interfaces)
│   ├── infrastructure/      # Adapters (JPA, Kafka, REST)
│   └── config/              # Spring/Quarkus configuration
│
└── test/java/id/payu/{service}/
    ├── unit/                # Unit tests (NO external deps)
    │   ├── domain/          # Domain logic tests
    │   └── application/     # Service/Use case tests
    ├── integration/         # Integration tests (Testcontainers)
    │   ├── repository/      # Repository tests
    │   └── kafka/           # Kafka producer/consumer tests
    ├── controller/          # @WebMvcTest
    ├── architecture/        # ArchUnit tests
    └── config/              # Configuration validation tests
```

---

## 🧪 Unit Testing Guidelines

### Rule 1: NO External Dependencies

Unit tests MUST NOT:
- ❌ Connect to real databases
- ❌ Call external APIs
- ❌ Use real Kafka
- ❌ Read from filesystem

Unit tests MUST:
- ✅ Use mocks for all dependencies
- ✅ Run in < 100ms
- ✅ Be deterministic (same result every time)

### Rule 2: Test Behavior, Not Implementation

```java
// ❌ BAD - Tests implementation details
@Test
void shouldCallRepositorySaveMethod() {
    service.createAccount(dto);
    verify(repository, times(1)).save(any());
}

// ✅ GOOD - Tests behavior
@Test
void shouldCreateAccountWithInitialBalance() {
    var result = service.createAccount(new CreateAccountDto("John Doe"));
    
    assertThat(result.getBalance()).isEqualTo(BigDecimal.ZERO);
    assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
}
```

### Rule 3: Arrange-Act-Assert (AAA) Pattern

```java
@Test
void shouldRejectTransferWhenInsufficientBalance() {
    // Arrange (Given)
    var account = Account.builder()
        .id("ACC-001")
        .balance(new BigDecimal("100.00"))
        .build();
    when(accountRepository.findById("ACC-001")).thenReturn(Optional.of(account));
    
    // Act (When)
    var exception = assertThrows(InsufficientBalanceException.class, () -> 
        transferService.transfer("ACC-001", "ACC-002", new BigDecimal("500.00"))
    );
    
    // Assert (Then)
    assertThat(exception.getMessage()).contains("Insufficient balance");
    assertThat(exception.getAvailableBalance()).isEqualTo(new BigDecimal("100.00"));
    assertThat(exception.getRequestedAmount()).isEqualTo(new BigDecimal("500.00"));
}
```

### Rule 4: One Assertion Per Concept

```java
// ❌ BAD - Too many unrelated assertions
@Test
void shouldProcessTransfer() {
    var result = service.transfer(...);
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getTransactionId()).isNotNull();
    assertThat(auditLog.getEntries()).hasSize(1);  // Different concept!
    verify(notificationService).sendSms(...);       // Different concept!
}

// ✅ GOOD - Separate tests for separate concepts
@Test
void shouldReturnSuccessResultWhenTransferCompletes() { ... }

@Test
void shouldGenerateTransactionIdForSuccessfulTransfer() { ... }

@Test
void shouldLogAuditEntryForTransfer() { ... }

@Test
void shouldSendNotificationAfterSuccessfulTransfer() { ... }
```

---

## 🔌 Integration Testing with Testcontainers

### PostgreSQL Integration Test

```java
@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class AccountRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("payu_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AccountRepository repository;

    @Test
    void shouldPersistAccountWithPockets() {
        // Given
        var account = Account.builder()
            .userId(UUID.randomUUID())
            .accountNumber("1234567890")
            .build();
        account.addPocket(Pocket.create("Savings", PocketType.SAVINGS));
        
        // When
        var saved = repository.save(account);
        
        // Then
        var found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPockets()).hasSize(1);
    }
}
```

### Kafka Integration Test

```java
@Testcontainers
@SpringBootTest
class TransactionEventPublisherTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private TransactionEventPublisher publisher;

    @Test
    void shouldPublishTransactionCreatedEvent() {
        // Given
        var event = new TransactionCreatedEvent(
            UUID.randomUUID(),
            "ACC-001",
            new BigDecimal("100000.00"),
            TransactionType.TRANSFER
        );

        // When
        publisher.publish(event);

        // Then - Use Kafka consumer to verify
        try (var consumer = createConsumer()) {
            consumer.subscribe(List.of("transaction.created"));
            var records = consumer.poll(Duration.ofSeconds(10));
            
            assertThat(records.count()).isEqualTo(1);
            var receivedEvent = deserialize(records.iterator().next().value());
            assertThat(receivedEvent.getAccountId()).isEqualTo("ACC-001");
        }
    }
}
```

---

## 🏛️ Architecture Testing with ArchUnit

### Hexagonal Architecture Rules

```java
@AnalyzeClasses(packages = "id.payu.account", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..config..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_use_spring_annotations =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Component");

    @ArchTest
    static final ArchRule controllers_should_only_call_services =
        classes()
            .that().resideInAPackage("..controller..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..controller..",
                "..application..",
                "..dto..",
                "java..",
                "jakarta..",
                "org.springframework.."
            );
}
```

### Naming Convention Rules

```java
@ArchTest
static final ArchRule services_should_be_suffixed =
    classes()
        .that().resideInAPackage("..application.service..")
        .should().haveSimpleNameEndingWith("Service")
        .orShould().haveSimpleNameEndingWith("UseCase");

@ArchTest
static final ArchRule repositories_should_be_interfaces =
    classes()
        .that().resideInAPackage("..application.port.out..")
        .should().beInterfaces();

@ArchTest
static final ArchRule exceptions_should_extend_runtime_exception =
    classes()
        .that().haveSimpleNameEndingWith("Exception")
        .should().beAssignableTo(RuntimeException.class);
```

---

## 🎮 Controller Testing with @WebMvcTest

```java
@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnAccountDetails() throws Exception {
        // Given
        var account = AccountDto.builder()
            .id(UUID.randomUUID())
            .accountNumber("1234567890")
            .balance(new BigDecimal("1000000.00"))
            .build();
        when(accountService.getAccount(any())).thenReturn(account);

        // When/Then
        mockMvc.perform(get("/api/v1/accounts/{id}", account.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value("1234567890"))
            .andExpect(jsonPath("$.balance").value(1000000.00));
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn400WhenInvalidInput() throws Exception {
        var invalidRequest = """
            {
                "amount": -100
            }
            """;

        mockMvc.perform(post("/api/v1/accounts/{id}/deposit", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("amount"));
    }
}
```

---

## ✅ Pre-Commit Validation Checklist

Sebelum commit, pastikan:

```bash
# 1. Compile check
mvn compile -q

# 2. Unit tests pass
mvn test -Dtest="**/unit/**" -q

# 3. Architecture tests pass
mvn test -Dtest="**/*ArchitectureTest" -q

# 4. Code coverage > 80%
mvn test jacoco:report
open target/site/jacoco/index.html
```

### Pre-commit Hook Script

```bash
#!/bin/bash
# .git/hooks/pre-commit

echo "🔍 Running pre-commit checks..."

# Check compilation
echo "📦 Compiling..."
mvn compile -q -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

# Run unit tests
echo "🧪 Running unit tests..."
mvn test -Dtest="**/unit/**" -q
if [ $? -ne 0 ]; then
    echo "❌ Unit tests failed!"
    exit 1
fi

# Run architecture tests
echo "🏛️ Running architecture tests..."
mvn test -Dtest="**/*ArchitectureTest" -q
if [ $? -ne 0 ]; then
    echo "❌ Architecture tests failed!"
    exit 1
fi

echo "✅ All checks passed!"
exit 0
```

---

## 🚫 Error Prevention Patterns

### 1. Interface-First Design

```java
// Step 1: Define the port (interface) FIRST
public interface TransferUseCase {
    TransferResult execute(TransferCommand command);
}

// Step 2: Write test against the interface
@Test
void shouldTransferBetweenAccounts() {
    TransferUseCase useCase = new TransferService(mockRepo, mockPublisher);
    var result = useCase.execute(new TransferCommand(...));
    assertThat(result.isSuccess()).isTrue();
}

// Step 3: Implement the interface
@Service
public class TransferService implements TransferUseCase {
    @Override
    public TransferResult execute(TransferCommand command) {
        // Implementation
    }
}
```

### 2. Fail-Fast Validation

```java
public TransferResult transfer(TransferCommand cmd) {
    // Validate early, fail fast
    Objects.requireNonNull(cmd.getSourceAccountId(), "Source account required");
    Objects.requireNonNull(cmd.getTargetAccountId(), "Target account required");
    
    if (cmd.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        throw new InvalidAmountException("Amount must be positive");
    }
    
    if (cmd.getSourceAccountId().equals(cmd.getTargetAccountId())) {
        throw new SameAccountTransferException("Cannot transfer to same account");
    }
    
    // Business logic after validation passes
    ...
}
```

### 3. Immutable Value Objects

```java
// Use records for immutability
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "Amount required");
        Objects.requireNonNull(currency, "Currency required");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException();
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

### 4. Domain Invariant Enforcement

```java
public class Account {
    private BigDecimal balance;
    private AccountStatus status;
    
    public void debit(BigDecimal amount) {
        // Invariant: Cannot debit from frozen account
        if (status == AccountStatus.FROZEN) {
            throw new AccountFrozenException(this.id);
        }
        
        // Invariant: Cannot go negative
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(this.id, balance, amount);
        }
        
        this.balance = this.balance.subtract(amount);
    }
}
```

---

## 📊 Coverage Requirements

| Layer | Minimum Coverage | Target Coverage |
|-------|------------------|-----------------|
| Domain | 90% | 95% |
| Application (Services) | 85% | 90% |
| Controllers | 80% | 85% |
| Infrastructure | 70% | 80% |

### JaCoCo Configuration

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 🔧 Troubleshooting Common TDD Issues

### Issue 1: Test is Flaky (Sometimes Passes, Sometimes Fails)

**Causes:**
- Time-dependent tests
- Shared state between tests
- Race conditions

**Solution:**
```java
// ❌ BAD - Time dependent
assertThat(result.getCreatedAt()).isEqualTo(LocalDateTime.now());

// ✅ GOOD - Use Clock injection
@Test
void shouldSetCreatedAtToCurrentTime() {
    var fixedClock = Clock.fixed(Instant.parse("2026-01-26T10:00:00Z"), ZoneId.of("UTC"));
    var service = new AccountService(repository, fixedClock);
    
    var result = service.createAccount(...);
    
    assertThat(result.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 26, 10, 0, 0));
}
```

### Issue 2: Tests are Too Slow

**Causes:**
- Using integration tests for unit testing
- Not using parallel execution

**Solution:**
```xml
<!-- pom.xml - Enable parallel test execution -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <parallel>classes</parallel>
        <threadCount>4</threadCount>
    </configuration>
</plugin>
```

### Issue 3: Hard to Mock Dependencies

**Cause:** Tight coupling, `new` keyword in services

**Solution:** Use Dependency Injection
```java
// ❌ BAD - Hard to test
public class TransferService {
    private final NotificationClient client = new NotificationClient();
}

// ✅ GOOD - Inject dependencies
public class TransferService {
    private final NotificationClient client;
    
    public TransferService(NotificationClient client) {
        this.client = client;
    }
}
```

---

## 📚 Quick Reference

### Test Annotations Cheat Sheet

| Annotation | Purpose |
|------------|---------|
| `@Test` | Mark method as test |
| `@BeforeEach` | Run before each test |
| `@AfterEach` | Run after each test |
| `@BeforeAll` | Run once before all tests |
| `@DisplayName` | Human-readable test name |
| `@Disabled` | Skip test (temporary) |
| `@ParameterizedTest` | Data-driven test |
| `@Nested` | Group related tests |
| `@Tag("integration")` | Tag for filtering |

### Mockito Cheat Sheet

```java
// Create mock
var mockRepo = mock(AccountRepository.class);

// Stub return value
when(mockRepo.findById(any())).thenReturn(Optional.of(account));

// Stub exception
when(mockRepo.save(any())).thenThrow(new DataIntegrityViolationException("Duplicate"));

// Verify interaction
verify(mockRepo).save(any());
verify(mockRepo, times(2)).findById(any());
verify(mockRepo, never()).delete(any());

// Argument capture
ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
verify(mockRepo).save(captor.capture());
assertThat(captor.getValue().getStatus()).isEqualTo(ACTIVE);
```

---

*Last Updated: January 2026*
