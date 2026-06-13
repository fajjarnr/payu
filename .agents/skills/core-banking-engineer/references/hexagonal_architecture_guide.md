# Hexagonal Architecture Implementation Guide

## Package Structure

```
id.payu.{service}/
├── domain/                          # Pure business logic (NO framework dependencies)
│   ├── model/                       # Entities, Value Objects, Aggregates
│   │   ├── Account.java
│   │   ├── AccountId.java          # Value Object
│   │   ├── Money.java              # Value Object
│   │   └── TransferOrder.java      # Aggregate Root
│   ├── event/                       # Domain Events
│   │   ├── TransferInitiated.java
│   │   └── TransferCompleted.java
│   ├── exception/                   # Domain Exceptions
│   │   ├── InsufficientBalanceException.java
│   │   └── AccountNotFoundException.java
│   ├── port/                        # Interfaces (Ports)
│   │   ├── in/                      # Driving/Primary Ports (Use Cases)
│   │   │   ├── TransferUseCase.java
│   │   │   └── GetBalanceUseCase.java
│   │   └── out/                     # Driven/Secondary Ports
│   │       ├── AccountRepository.java
│   │       ├── TransferEventPublisher.java
│   │       └── ExternalBankClient.java
│   └── service/                     # Domain Services
│       └── TransferDomainService.java
│
├── application/                     # Application Layer (Orchestration)
│   ├── service/                     # Use Case Implementations
│   │   ├── TransferService.java    # Implements TransferUseCase
│   │   └── BalanceQueryService.java
│   ├── command/                     # Commands (Write operations)
│   │   └── InitiateTransferCommand.java
│   └── query/                       # Queries (Read operations)
│       └── GetAccountBalanceQuery.java
│
├── adapter/                         # Infrastructure Adapters
│   ├── in/                          # Driving Adapters (Input)
│   │   ├── web/                     # REST Controllers
│   │   │   ├── TransferController.java
│   │   │   └── dto/
│   │   │       ├── TransferRequest.java
│   │   │       └── TransferResponse.java
│   │   ├── grpc/                    # gRPC Endpoints
│   │   │   └── TransferGrpcService.java
│   │   └── messaging/               # Kafka Consumers
│   │       └── TransferEventConsumer.java
│   │
│   └── out/                         # Driven Adapters (Output)
│       ├── persistence/             # Database
│       │   ├── AccountJpaRepository.java
│       │   ├── AccountPersistenceAdapter.java  # Implements AccountRepository
│       │   └── entity/
│       │       └── AccountJpaEntity.java
│       ├── messaging/               # Kafka Producers
│       │   └── TransferEventKafkaAdapter.java
│       └── external/                # External APIs
│           ├── BiFastClientAdapter.java
│           └── config/
│               └── BiFastClientConfig.java
│
└── config/                          # Spring Configuration
    ├── SecurityConfig.java
    ├── KafkaConfig.java
    └── PersistenceConfig.java
```

---

## Domain Layer (The Core)

### Entity Example

```java
package id.payu.wallet.domain.model;

/**
 * Account Aggregate Root
 * NO Spring annotations here - pure Java
 */
public class Account {
    
    private final AccountId id;
    private final CustomerId customerId;
    private Money balance;
    private AccountStatus status;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    // Private constructor - use factory methods
    private Account(AccountId id, CustomerId customerId, Money initialBalance) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.balance = initialBalance;
        this.status = AccountStatus.ACTIVE;
    }
    
    // Factory method
    public static Account create(CustomerId customerId, Money initialDeposit) {
        Account account = new Account(
            AccountId.generate(),
            customerId,
            initialDeposit
        );
        account.registerEvent(new AccountCreated(account.id, customerId));
        return account;
    }
    
    // Business method with invariant protection
    public void debit(Money amount, String description) {
        validateActive();
        
        if (balance.isLessThan(amount)) {
            throw new InsufficientBalanceException(id, balance, amount);
        }
        
        this.balance = this.balance.subtract(amount);
        registerEvent(new AccountDebited(id, amount, description, balance));
    }
    
    public void credit(Money amount, String description) {
        validateActive();
        this.balance = this.balance.add(amount);
        registerEvent(new AccountCredited(id, amount, description, balance));
    }
    
    private void validateActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(id, status);
        }
    }
    
    // Domain events
    private void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
    
    // Getters only - no setters
    public AccountId getId() { return id; }
    public Money getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
}
```

### Value Object Example

```java
package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Money Value Object - Immutable
 */
public final class Money {
    
    private final BigDecimal amount;
    private final Currency currency;
    
    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(4, RoundingMode.HALF_EVEN);
        this.currency = Objects.requireNonNull(currency);
    }
    
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }
    
    public static Money idr(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("IDR"));
    }
    
    public static Money idr(long amount) {
        return idr(BigDecimal.valueOf(amount));
    }
    
    public static Money zero(String currencyCode) {
        return new Money(BigDecimal.ZERO, Currency.getInstance(currencyCode));
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }
    
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(this.currency, other.currency);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && 
               currency.equals(money.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }
    
    // Getters
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
}
```

### Port (Interface) Examples

```java
// Primary/Driving Port - Use Case Interface
package id.payu.wallet.domain.port.in;

public interface TransferUseCase {
    TransferResult execute(TransferCommand command);
}

public record TransferCommand(
    AccountId sourceAccount,
    AccountId destinationAccount,
    Money amount,
    String description,
    String idempotencyKey
) {}

// Secondary/Driven Port - Repository Interface
package id.payu.wallet.domain.port.out;

public interface AccountRepository {
    Optional<Account> findById(AccountId id);
    Account save(Account account);
    boolean existsById(AccountId id);
}

// Secondary/Driven Port - Event Publisher Interface
package id.payu.wallet.domain.port.out;

public interface TransferEventPublisher {
    void publish(TransferCompleted event);
    void publish(TransferFailed event);
}
```

---

## Application Layer

```java
package id.payu.wallet.application.service;

@Service
@Transactional
@RequiredArgsConstructor
public class TransferService implements TransferUseCase {
    
    private final AccountRepository accountRepository;
    private final TransferEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    
    @Override
    public TransferResult execute(TransferCommand command) {
        // 1. Idempotency check
        Optional<TransferResult> existing = idempotencyService.get(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // 2. Load aggregates
        Account source = accountRepository.findById(command.sourceAccount())
            .orElseThrow(() -> new AccountNotFoundException(command.sourceAccount()));
            
        Account destination = accountRepository.findById(command.destinationAccount())
            .orElseThrow(() -> new AccountNotFoundException(command.destinationAccount()));
        
        // 3. Execute domain logic
        source.debit(command.amount(), "Transfer to " + command.destinationAccount());
        destination.credit(command.amount(), "Transfer from " + command.sourceAccount());
        
        // 4. Persist changes
        accountRepository.save(source);
        accountRepository.save(destination);
        
        // 5. Publish domain events
        source.pullDomainEvents().forEach(eventPublisher::publish);
        destination.pullDomainEvents().forEach(eventPublisher::publish);
        
        // 6. Store idempotency result
        TransferResult result = TransferResult.success(
            TransferId.generate(),
            command.sourceAccount(),
            command.destinationAccount(),
            command.amount()
        );
        idempotencyService.store(command.idempotencyKey(), result);
        
        return result;
    }
}
```

---

## Adapter Layer

### Driving Adapter (REST Controller)

```java
package id.payu.wallet.adapter.in.web;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Validated
public class TransferController {
    
    private final TransferUseCase transferUseCase;
    private final TransferMapper mapper;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse createTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        
        TransferCommand command = mapper.toCommand(request, idempotencyKey);
        TransferResult result = transferUseCase.execute(command);
        return mapper.toResponse(result);
    }
}

// DTO - Separate from Domain
public record TransferRequest(
    @NotNull String sourceAccountId,
    @NotNull String destinationAccountId,
    @NotNull @Positive BigDecimal amount,
    @Size(max = 100) String description
) {}

public record TransferResponse(
    String transferId,
    String status,
    BigDecimal amount,
    String currency,
    Instant timestamp
) {}
```

### Driven Adapter (Persistence)

```java
package id.payu.wallet.adapter.out.persistence;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountRepository {
    
    private final AccountJpaRepository jpaRepository;
    private final AccountPersistenceMapper mapper;
    
    @Override
    public Optional<Account> findById(AccountId id) {
        return jpaRepository.findById(id.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = mapper.toEntity(account);
        AccountJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}

// JPA Entity - Separate from Domain Entity
@Entity
@Table(name = "accounts")
@Data
public class AccountJpaEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    
    @Column(name = "balance", precision = 19, scale = 4)
    private BigDecimal balance;
    
    @Column(name = "currency", length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    
    @Version
    private Long version;
}
```

### Driven Adapter (Kafka)

```java
package id.payu.wallet.adapter.out.messaging;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferEventKafkaAdapter implements TransferEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public void publish(TransferCompleted event) {
        try {
            String payload = objectMapper.writeValueAsString(
                TransferCompletedEvent.fromDomain(event)
            );
            
            kafkaTemplate.send(
                "payu.wallet.transfer-completed.v1",
                event.getTransferId().toString(),
                payload
            ).get(5, TimeUnit.SECONDS);
            
            log.info("Published TransferCompleted event: {}", event.getTransferId());
            
        } catch (Exception e) {
            log.error("Failed to publish TransferCompleted event", e);
            throw new EventPublishException("Failed to publish event", e);
        }
    }
}
```

---

## ArchUnit Enforcement

```java
package id.payu.wallet;

@AnalyzeClasses(packages = "id.payu.wallet")
public class ArchitectureTest {
    
    @ArchTest
    static final ArchRule domainShouldNotDependOnAdapters = 
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter..", "..config..");
    
    @ArchTest
    static final ArchRule domainShouldNotDependOnSpring = 
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..");
    
    @ArchTest
    static final ArchRule applicationShouldOnlyDependOnDomain = 
        classes()
            .that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..domain..", 
                "..application..",
                "java..",
                "lombok.."
            );
    
    @ArchTest
    static final ArchRule adaptersShouldNotDependOnEachOther = 
        noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter.out..");
    
    @ArchTest
    static final ArchRule controllersShouldNotAccessRepositoriesDirectly = 
        noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat()
            .areAnnotatedWith(Repository.class);
}
```
