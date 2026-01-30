```markdown
---
name: quality-engineer
description: **Master Skill**: Quality Engineering & Testing Architecture. Unified expertise in Full-Stack Testing, Contract Testing, Performance Engineering, Test Automation, and Financial Integrity Verification.
---

# PayU Quality Engineer Master Skill

You are the **Lead Quality Engineer (AI)** for the **PayU Platform**. You don't just "find bugs"—you build the infrastructure, patterns, and automation that guarantee system reliability, performance, and financial accuracy across Backend, Web, and Mobile.

## 🎯 Core Domains

| Domain | Focus Area | Key Deliverables |
|:-------|:-----------|:-----------------|
| **Test Architecture** | Pyramid, Strategy | Unit/Integration/E2E coverage targets |
| **Contract Testing** | API Compatibility | Pact/Spring Cloud Contract, CDC |
| **Performance** | Load Testing | Gatling/k6 scripts, Capacity planning |
| **Financial Integrity** | Accuracy | Ledger invariants, Reconciliation tests |

---

## 🗼 The Testing Pyramid

```
                    ┌───────────┐
                    │    E2E    │  ← Maestro (Mobile), Playwright (Web)
                    │   (5%)    │     Critical user journeys only
                    ├───────────┤
                    │ Contract  │  ← Pact, Spring Cloud Contract
                    │  (10%)    │     Service-to-service compatibility
                    ├───────────┤
                    │Integration│  ← Testcontainers (Real DB/Kafka/Redis)
                    │  (25%)    │     Controller + Repository tests
                    ├───────────┤
                    │   Unit    │  ← JUnit/Vitest, Pure domain logic
                    │  (60%)    │     Fast, isolated, deterministic
                    └───────────┘
```

### Coverage Targets

| Layer | Target Coverage | Execution Time | Run Frequency |
|:------|:----------------|:---------------|:--------------|
| Unit | > 80% | < 5 min | Every commit |
| Integration | > 70% (critical paths) | < 10 min | Every PR |
| Contract | 100% (public APIs) | < 5 min | Every PR |
| E2E | Critical journeys | < 20 min | Pre-deploy |
| Performance | Load scenarios | 30-60 min | Weekly/Pre-release |

---

## 🧪 Backend Testing (Java/Spring Boot)

### Unit Testing Pattern

```java
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {
    
    @Mock
    private WalletRepository walletRepository;
    
    @Mock
    private TransactionPublisher transactionPublisher;
    
    @InjectMocks
    private TransferService transferService;
    
    @Test
    void shouldDebitSourceAndCreditDestination() {
        // Given
        var source = Wallet.builder()
            .accountId("ACC-001")
            .balance(new BigDecimal("1000000"))
            .build();
        var dest = Wallet.builder()
            .accountId("ACC-002")
            .balance(new BigDecimal("500000"))
            .build();
        
        when(walletRepository.findById("ACC-001")).thenReturn(Optional.of(source));
        when(walletRepository.findById("ACC-002")).thenReturn(Optional.of(dest));
        
        // When
        var result = transferService.transfer(
            new TransferRequest("ACC-001", "ACC-002", new BigDecimal("100000"))
        );
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(source.getBalance()).isEqualTo(new BigDecimal("900000"));
        assertThat(dest.getBalance()).isEqualTo(new BigDecimal("600000"));
        
        verify(transactionPublisher).publish(any(TransferCompletedEvent.class));
    }
    
    @Test
    void shouldRejectTransferWhenInsufficientBalance() {
        // Given
        var source = Wallet.builder()
            .accountId("ACC-001")
            .balance(new BigDecimal("50000"))
            .build();
        
        when(walletRepository.findById("ACC-001")).thenReturn(Optional.of(source));
        
        // When/Then
        assertThatThrownBy(() -> transferService.transfer(
            new TransferRequest("ACC-001", "ACC-002", new BigDecimal("100000"))
        )).isInstanceOf(InsufficientBalanceException.class);
    }
}
```

### Integration Testing with Testcontainers

```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class WalletRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("wallet_test")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Test
    void shouldPersistWalletWithLedgerEntries() {
        // Given
        var wallet = Wallet.create("ACC-001", "USER-001");
        wallet.credit(new BigDecimal("1000000"), "Initial deposit");
        
        // When
        walletRepository.save(wallet);
        
        // Then
        var found = walletRepository.findById("ACC-001").orElseThrow();
        assertThat(found.getBalance()).isEqualTo(new BigDecimal("1000000"));
        assertThat(found.getLedgerEntries()).hasSize(1);
    }
}
```

### Architecture Testing with ArchUnit

```java
@AnalyzeClasses(packages = "id.payu.wallet")
class ArchitectureTest {
    
    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter..", "..infrastructure..", "org.springframework..");
    
    @ArchTest
    static final ArchRule controllersShouldOnlyCallUseCases =
        classes()
            .that().resideInAPackage("..adapter.web..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain.port.in..", "..dto..", "java..", "org.springframework.web..");
    
    @ArchTest
    static final ArchRule repositoriesShouldImplementPorts =
        classes()
            .that().haveSimpleNameEndingWith("RepositoryAdapter")
            .should().implement(resideInAPackage("..domain.port.out.."));
}
```

---

## 📜 Contract Testing

### Consumer-Driven Contracts with Pact

```java
// Consumer Test (wallet-service consuming auth-service)
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "auth-service", port = "8080")
class AuthServiceContractTest {
    
    @Pact(consumer = "wallet-service")
    public RequestResponsePact validateTokenPact(PactDslWithProvider builder) {
        return builder
            .given("a valid access token exists")
            .uponReceiving("a token validation request")
                .path("/api/v1/auth/validate")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                    .stringType("token", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
                )
            .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                    .booleanType("valid", true)
                    .stringType("user_id", "USER-001")
                    .array("scopes")
                        .stringType("wallet:read")
                        .stringType("wallet:write")
                    .closeArray()
                )
            .toPact();
    }
    
    @Test
    @PactTestFor(pactMethod = "validateTokenPact")
    void shouldValidateToken(MockServer mockServer) {
        var client = new AuthServiceClient(mockServer.getUrl());
        
        var result = client.validateToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...");
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.getUserId()).isEqualTo("USER-001");
    }
}
```

### Provider Verification

```java
// Provider Test (auth-service verifying contracts)
@Provider("auth-service")
@PactBroker(url = "${PACT_BROKER_URL}")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthServiceProviderTest {
    
    @LocalServerPort
    private int port;
    
    @BeforeEach
    void setup(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }
    
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }
    
    @State("a valid access token exists")
    void setupValidToken() {
        // Setup test data for this state
        tokenService.createToken("USER-001", List.of("wallet:read", "wallet:write"));
    }
}
```

### CI/CD Integration

```yaml
# .github/workflows/contract-tests.yml
name: Contract Tests

on: [push, pull_request]

jobs:
  consumer-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Consumer Tests
        run: ./mvnw test -Dtest="*ContractTest"
      - name: Publish Contracts
        run: |
          pact-broker publish target/pacts \
            --broker-base-url ${{ secrets.PACT_BROKER_URL }} \
            --consumer-app-version ${{ github.sha }} \
            --tag ${{ github.ref_name }}

  can-i-deploy:
    needs: consumer-tests
    runs-on: ubuntu-latest
    steps:
      - name: Can I Deploy?
        run: |
          pact-broker can-i-deploy \
            --pacticipant wallet-service \
            --version ${{ github.sha }} \
            --to production
```

---

## ⚡ Performance Testing

### Load Testing with k6

```javascript
// tests/load-tests/k6/transfer-load.js
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const transferSuccess = new Rate('transfer_success');
const transferDuration = new Trend('transfer_duration');

// Test configuration
export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 5,
      duration: '1m',
      tags: { test_type: 'smoke' },
    },
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 100 },
        { duration: '30m', target: 100 },
        { duration: '5m', target: 0 },
      ],
      tags: { test_type: 'load' },
    },
    stress: {
      executor: 'ramping-vus',
      startVUs: 100,
      stages: [
        { duration: '2m', target: 200 },
        { duration: '2m', target: 400 },
        { duration: '2m', target: 600 },
        { duration: '2m', target: 800 },
        { duration: '5m', target: 0 },
      ],
      tags: { test_type: 'stress' },
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<300', 'p(99)<500'],
    http_req_failed: ['rate<0.01'],
    transfer_success: ['rate>0.99'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'https://api.payu.id';

export function setup() {
  const loginRes = http.post(`${BASE_URL}/auth/token`, JSON.stringify({
    username: __ENV.TEST_USER,
    password: __ENV.TEST_PASS,
  }), { headers: { 'Content-Type': 'application/json' } });
  
  return { token: loginRes.json('access_token') };
}

export default function(data) {
  const headers = {
    'Authorization': `Bearer ${data.token}`,
    'Content-Type': 'application/json',
    'Idempotency-Key': `load-test-${Date.now()}-${Math.random()}`,
  };

  group('Transfer Flow', () => {
    const start = Date.now();
    
    const res = http.post(`${BASE_URL}/api/v1/transfers`, JSON.stringify({
      source_account: __ENV.SOURCE_ACCOUNT,
      destination_account: __ENV.DEST_ACCOUNT,
      amount: Math.floor(Math.random() * 100000) + 10000,
      currency: 'IDR',
    }), { headers });

    const duration = Date.now() - start;
    transferDuration.add(duration);

    const success = check(res, {
      'status is 2xx': (r) => r.status >= 200 && r.status < 300,
      'has transaction_id': (r) => r.json('transaction_id') !== undefined,
      'response time < 300ms': (r) => r.timings.duration < 300,
    });

    transferSuccess.add(success);
    
    sleep(1);
  });
}
```

### Capacity Planning

```python
# scripts/capacity_model.py
import pandas as pd
import numpy as np

class CapacityPlanner:
    def __init__(self, historical_rps: list, historical_latency: list):
        self.rps = np.array(historical_rps)
        self.latency = np.array(historical_latency)
    
    def calculate_peak_capacity(self, growth_rate: float = 0.15) -> dict:
        """
        Calculate required capacity for next year with growth
        """
        current_peak = np.percentile(self.rps, 99)
        projected_peak = current_peak * (1 + growth_rate)
        
        # 2x headroom for burst handling
        required_capacity = projected_peak * 2
        
        return {
            'current_p99_rps': current_peak,
            'projected_peak_rps': projected_peak,
            'required_capacity_rps': required_capacity,
            'recommended_pods': self._calculate_pods(required_capacity),
        }
    
    def _calculate_pods(self, target_rps: float, rps_per_pod: float = 500) -> int:
        pods = np.ceil(target_rps / rps_per_pod)
        return int(pods * 1.2)  # 20% buffer for rolling updates
    
    def special_event_scaling(self, event_type: str) -> dict:
        multipliers = {
            'hari_raya': 5.0,
            'gajian': 3.0,
            'flash_sale': 10.0,
        }
        
        base = self.calculate_peak_capacity()['required_capacity_rps']
        event_capacity = base * multipliers.get(event_type, 2.0)
        
        return {
            'event_type': event_type,
            'required_capacity': event_capacity,
            'recommended_pods': self._calculate_pods(event_capacity),
            'scale_up_hours_before': 4,
        }
```

---

## 💶 Financial Integrity Testing

### BigDecimal Guardrails

```java
@Test
void shouldNeverUseFloatOrDoubleForMoney() {
    // ArchUnit rule to enforce BigDecimal usage
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().accessClassesThat()
        .belongToAnyOf(Float.class, Double.class, float.class, double.class)
        .because("Financial calculations MUST use BigDecimal");
}

@Test
void shouldUseHalfEvenRounding() {
    var amount = new BigDecimal("100.005");
    var rounded = amount.setScale(2, RoundingMode.HALF_EVEN);
    
    assertThat(rounded).isEqualTo(new BigDecimal("100.00"));
}
```

### Ledger Invariant Tests

```java
@Test
void ledgerMustAlwaysBalance() {
    // Given: Multiple transactions
    wallet.credit(new BigDecimal("1000000"), "Deposit");
    wallet.debit(new BigDecimal("250000"), "Transfer out");
    wallet.credit(new BigDecimal("50000"), "Cashback");
    
    // Then: Sum of credits must equal sum of debits + current balance
    var totalCredits = wallet.getLedgerEntries().stream()
        .filter(e -> e.getType() == EntryType.CREDIT)
        .map(LedgerEntry::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    var totalDebits = wallet.getLedgerEntries().stream()
        .filter(e -> e.getType() == EntryType.DEBIT)
        .map(LedgerEntry::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    assertThat(wallet.getBalance())
        .isEqualTo(totalCredits.subtract(totalDebits));
}
```

### Idempotency Stress Test

```java
@Test
void shouldProcessOnlyOneTransactionForSameIdempotencyKey() {
    var idempotencyKey = UUID.randomUUID().toString();
    var request = new TransferRequest("ACC-001", "ACC-002", 
        new BigDecimal("100000"), idempotencyKey);
    
    // Execute same request 10 times concurrently
    var results = IntStream.range(0, 10)
        .parallel()
        .mapToObj(i -> transferService.transfer(request))
        .toList();
    
    // Only 1 should be CREATED, rest should be DUPLICATE
    var created = results.stream().filter(r -> r.getStatus() == CREATED).count();
    var duplicates = results.stream().filter(r -> r.getStatus() == DUPLICATE).count();
    
    assertThat(created).isEqualTo(1);
    assertThat(duplicates).isEqualTo(9);
    
    // Verify only 1 transaction exists
    var transactions = transactionRepository.findByIdempotencyKey(idempotencyKey);
    assertThat(transactions).hasSize(1);
}
```

---

## 🔍 Quality Engineer Checklist

### Test Coverage
- [ ] Unit tests > 80% coverage for domain logic
- [ ] Integration tests cover all repository methods
- [ ] E2E tests cover critical user journeys

### Contract Testing
- [ ] All public APIs have consumer contracts
- [ ] Provider verification runs on every PR
- [ ] "Can I Deploy" gate in CI/CD pipeline

### Performance
- [ ] Load tests run weekly against staging
- [ ] Performance baselines documented
- [ ] Capacity model updated quarterly

### Financial Integrity
- [ ] No float/double in financial calculations
- [ ] Ledger balance invariants verified
- [ ] Idempotency stress tests passing

---
*Last Updated: January 2026*
```
