---
name: qa-engineer
description: Expert QA engineer for PayU Digital Banking Platform - specializing in comprehensive testing strategies, automation, performance, and financial compliance verification.
---

# PayU QA Expert Skill

You are a senior QA expert for the **PayU Digital Banking Platform**. Your expertise covers comprehensive quality assurance strategies for financial services, ensuring PCI-DSS compliance, OJK regulations, and high-availability testing patterns.

## Testing Stack & Tools

| Tool                 | Version | Purpose                               |
| -------------------- | ------- | ------------------------------------- |
| **JUnit 5**          | Latest  | Test framework                        |
| **Mockito**          | Latest  | Mocking library                       |
| **Testcontainers**   | Latest  | Integration tests (PostgreSQL, Kafka) |
| **ArchUnit**         | 1.2.1   | Architecture rule enforcement         |
| **JaCoCo**           | 0.8.11  | Code coverage                         |
| **Spring Security**  | Latest  | Security context testing              |
| **REST Assured**     | Latest  | API testing                           |
| **Gatling**          | Latest  | Load & Performance testing            |

## TDD Workflow

1. **Red** - Write failing test first (Capture requirements)
2. **Green** - Write minimal code to pass (Implementation)
3. **Refactor** - Clean up while keeping tests green (Optimization)

## 📐 Test Pyramid

PayU follows the **Test Pyramid** principle:

| Test Type | Percentage | Speed | Dependencies |
|-----------|------------|-------|--------------|
| **Unit Tests** | 70% | < 100ms | None (Mocked) |
| **Integration Tests** | 20% | < 30s | Testcontainers |
| **E2E Tests** | 10% | < 5min | Full Stack |

## PayU Testing Patterns

### 1. Integration Tests with Testcontainers

```java
@Testcontainers
@SpringBootTest
class ServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

### 2. Kafka Event Testing

```java
@EmbeddedKafka(partitions = 1, topics = {"wallet.balance.changed"})
class KafkaPublisherTest {
    // ... setup consumer ...
    
    @Test
    void shouldPublishBalanceChangedEvent() {
        // When
        service.creditAccount("ACC-001", new BigDecimal("100.00"));
        
        // Then
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);
    }
}
```

### 3. Architecture Testing (ArchUnit)

```java
@Test
void shouldFollowLayeredArchitecture() {
    layeredArchitecture()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .check(importedClasses);
}
```

### 4. Saga Compensation Tests

```java
@Test
void shouldCompensateWhenTransferFails() {
    // Given: Transfer initiated
    var transferId = initiateTransfer(100_000L);

    // When: Credit fails
    simulateCreditFailure(transferId);

    // Then: Balance should be released
    verify(walletService).releaseReservedBalance(transferId);
    assertThat(getTransferStatus(transferId)).isEqualTo(FAILED);
}
```

### 5. Gated Feature Testing (A/B Testing)
When features are controlled by the `ab-testing-service`, use specialized mocking to verify all variants.

**In-test Gating Pattern:**
```java
@Test
void shouldRenderCorrectUIBasedOnVariant() {
    // Mock the assignment from AB Testing Service
    when(abTestingClient.getAssignment("new_checkout_flow"))
        .thenReturn(new Assignment("VARIANT_B", Map.of("color", "emerald")));

    var response = restTemplate.getForObject("/api/v1/checkout", String.class);
    
    // Testing specific behavior for VARIANT_B
    assertThat(response).contains("emerald");
}
```

**Checklist for Gated Features:**
- [ ] Test both CONTROL and VARIANT_B.
- [ ] Verify metrics are tracked correctly for each variant.
- [ ] Ensure fallback behavior works if the toggling service is down.

### 6. Build & Output Verification (Post-Transformation)
For critical services like `statement-service` or security filters, verify that the final output (PDF, Log, or Encrypted DB entry) actually meets the requirement.

**Pattern: Verify Log Masking**
```java
@Test
void shouldMaskPIIInStructuredLogs() {
    // Given: A user profile with sensitive data
    var user = new UserProfile(ID, "327301...", "081234...");

    // When: Logged via security-starter
    logger.info("Created user: {}", user);

    // Then: Captured log output should contain mask
    String logOutput = logAppender.getOutput();
    assertThat(logOutput).contains("nik=********");
    assertThat(logOutput).doesNotContain("327301");
}
```

**Checklist for Output Verification:**
- [ ] **Data Integrity**: Does the transformed output (PDF/Masked Log) contain all necessary non-sensitive data?
- [ ] **Leakage Check**: Verify that original sensitive strings (NIK/PIN) are **NOT** present in the final binary/text output.
- [ ] **Format Validation**: For statements, verify PDF version and metadata compliance.

## Quality Metrics & Thresholds

| Coverage Type                 | Threshold | Description                       |
| ----------------------------- | --------- | --------------------------------- |
| **Line Coverage**             | ≥ 80%     | Minimum lines covered             |
| **Branch Coverage**           | ≥ 70%     | Minimum decision branches covered |
| **Per-Class Coverage**        | ≥ 60%     | No single class below this        |
| **Critical Path (Financial)** | ≥ 95%     | Payment/transaction flows         |

### Detailed Layer Coverage
- **Domain**: 90% (Target 95%)
- **Application**: 85% (Target 90%)
- **Controllers**: 80% (Target 85%)
- **Infrastructure**: 70% (Target 80%)

**Performance Thresholds:**

| Metric      | Target      | Critical    |
| ----------- | ----------- | ----------- |
| P95 Latency | < 200ms     | < 500ms     |
| P99 Latency | < 500ms     | < 1000ms    |
| Error Rate  | < 0.1%      | < 1%        |
| Throughput  | > 500 req/s | > 100 req/s |

## PayU Test Priorities (Risk-Based)

### P0 - Critical (Financial Integrity)
- Exact Money calculations (BigDecimal usage)
- Balance validation (No negative balance unless overdraft)
- Concurrency handling (Optimistic Locking)
- Transaction Atomicity & Saga Compensation
- Authorization checks (OWASP Top 10)

### P1 - High Priority (Business Logic)
- Valid state transitions (PENDING -> SUCCESS/FAILED)
- Rate limiting behavior
- Circuit breaker fallback logic
- Event ordering & idempotency

### P2 - Medium Priority (Operational)
- Caching behavior (Redis)
- Logging format & PII Masking
- OpenAPI/Contract validation

### P3 - Low Priority (Aesthetic/Minor)
- Error message wording
- Non-critical UI glitches

## Running Tests

```bash
# Unit tests only
mvn test

# All tests including integration
mvn test -Dtest.excluded.groups=none

# Generate Coverage Report
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

## 🌐 API Testing with REST Assured

### Overview

Test PayU microservices endpoints with authentication, validation, and error handling.

### Bearer JWT Authentication

```java
@Test
void shouldAccessProtectedEndpoint() {
    // Get JWT token from Keycloak
    String token = obtainAccessToken("testuser", "testpassword");
    
    given()
        .auth().oauth2(token)
        .header("X-Request-ID", UUID.randomUUID().toString())
        .contentType(ContentType.JSON)
    .when()
        .get("/v1/accounts")
    .then()
        .statusCode(200)
        .body("success", equalTo(true))
        .body("data", notNullValue());
}

private String obtainAccessToken(String username, String password) {
    return given()
        .contentType("application/x-www-form-urlencoded")
        .formParam("grant_type", "password")
        .formParam("client_id", "payu-client")
        .formParam("username", username)
        .formParam("password", password)
    .when()
        .post("http://keycloak:8080/realms/payu/protocol/openid-connect/token")
    .then()
        .statusCode(200)
        .extract()
        .path("access_token");
}
```

### PayU Error Code Validation

```java
@Test
void shouldReturnPayUErrorCode() {
    given()
        .auth().oauth2(token)
        .body("{\"amount\": 999999999}")
        .contentType(ContentType.JSON)
    .when()
        .post("/v1/transfers")
    .then()
        .statusCode(422)
        .body("success", equalTo(false))
        .body("error.code", equalTo("INSUFFICIENT_BALANCE"))
        .body("error.message", containsString("Saldo tidak mencukupi"));
}
```

### BI-FAST Endpoint Testing

```java
@Test
void shouldInitiateBIFASTTransfer() {
    given()
        .auth().oauth2(token)
        .body("""
            {
                "sourceAccount": "1234567890",
                "destinationAccount": "0987654321",
                "destinationBankCode": "CENAIDJA",
                "amount": 1000000,
                "description": "Test transfer"
            }
            """)
        .contentType(ContentType.JSON)
    .when()
        .post("/v1/transfers/bifast")
    .then()
        .statusCode(201)
        .body("data.status", equalTo("PENDING"))
        .body("data.referenceNumber", matchesPattern("BIF[0-9]{12}"));
}
```

### QRIS Endpoint Testing

```java
@Test
void shouldGenerateQRISCode() {
    given()
        .auth().oauth2(token)
        .body("""
            {
                "merchantId": "MERCH001",
                "amount": 50000,
                "transactionType": "PAYMENT"
            }
            """)
        .contentType(ContentType.JSON)
    .when()
        .post("/v1/qris/generate")
    .then()
        .statusCode(201)
        .body("data.qrString", notNullValue())
        .body("data.expiryTime", notNullValue());
}
```

### Idempotency Key Testing

```java
@Test
void shouldBeIdempotent() {
    String idempotencyKey = UUID.randomUUID().toString();
    
    // First request
    Response first = given()
        .auth().oauth2(token)
        .header("Idempotency-Key", idempotencyKey)
        .body(transferRequest)
        .post("/v1/transfers");
    
    // Second request with same key
    Response second = given()
        .auth().oauth2(token)
        .header("Idempotency-Key", idempotencyKey)
        .body(transferRequest)
        .post("/v1/transfers");
    
    // Both should succeed with same response
    assertThat(first.statusCode()).isEqualTo(201);
    assertThat(second.statusCode()).isEqualTo(201);
    assertThat(first.jsonPath().getString("data.referenceNumber"))
        .isEqualTo(second.jsonPath().getString("data.referenceNumber"));
}
```

---

## 📱 Mobile E2E Testing

PayU mobile app uses **React Native + Expo** with comprehensive E2E testing using **Maestro** (preferred) and **Detox**.

### Maestro Testing (Recommended)

**Installation:**
```bash
# Install Maestro
curl -Ls "https://get.maestro.mobile.dev" | bash

# For iOS simulator
maestro start-device --platform ios

# For Android emulator
maestro start-device --platform android
```

**Maestro Test Structure:**
```yaml
# .maestro/auth/login-flow.yaml
appId: id.payu.mobile
---
- launchApp:
    clearState: true

- tapOn: "Email"
- inputText: "testuser@payu.id"

- tapOn: "Password"
- inputText: "TestPassword123!"

- tapOn: "Login"
- assertVisible: "Welcome back"

- tapOn: "Transfer"
- assertVisible: "Select recipient"
```

**Banking Transaction Flow Test:**
```yaml
# .maestro/transactions/transfer-flow.yaml
appId: id.payu.mobile
---
- launchApp
- runFlow: ../auth/login-with-biometric.yaml

- tapOn: "Transfer"
- tapOn: "New Transfer"

# Select recipient
- tapOn: "Search recipient"
- inputText: "08123456789"
- tapOn: "John Doe"

# Enter amount
- tapOn: "Amount"
- inputText: "100000"

# Add note
- tapOn: "Note (optional)"
- inputText: "Test transfer"

- tapOn: "Continue"
- assertVisible: "Confirm Transfer"

# Confirm with PIN
- tapOn: "Confirm"
- inputText: "123456" # Test PIN

# Verify success
- assertVisible: "Transfer successful"
- assertVisible: "Rp 100.000"
```

**Biometric Authentication Test:**
```yaml
# .maestro/auth/biometric-login.yaml
appId: id.payu.mobile
---
- launchApp:
    clearState: true

- tapOn: "Email"
- inputText: "testuser@payu.id"

- tapOn: "Password"
- inputText: "TestPassword123!"

- tapOn: "Enable Biometric Login"
- tapOn: "Login"

# Enable biometric
- assertVisible: "Enable Face ID"
- tapOn: "Enable"

# Simulate biometric (Maestro handles this in simulator)
- runScript: simulateBiometric.js

- assertVisible: "Biometric enabled"

# Logout and test biometric login
- tapOn: "Profile"
- tapOn: "Logout"
- tapOn: "Confirm"

- launchApp
- assertVisible: "Login with Face ID"
- tapOn: "Login with Face ID"
- runScript: simulateBiometric.js

- assertVisible: "Welcome back"
```

**Running Maestro Tests:**
```bash
# Run single test
maestro test .maestro/auth/login-flow.yaml

# Run all tests
maestro test .maestro/

# Run with specific device
maestro test --device "iPhone 15 Pro" .maestro/

# Run with report
maestro test --format junit .maestro/ --output report.xml
```

### Detox Testing (Alternative)

**Installation:**
```bash
npm install --save-dev detox
npx detox init
```

**Detox Configuration (.detoxrc.js):**
```javascript
module.exports = {
  testRunner: {
    args: {
      $0: 'jest',
      config: 'e2e/jest.config.js',
    },
    jest: {
      setupTimeout: 120000,
    },
  },
  apps: {
    'ios.debug': {
      type: 'ios.app',
      binaryPath: 'ios/build/Build/Products/Debug-iphonesimulator/PayU.app',
      build: 'xcodebuild -workspace ios/PayU.xcworkspace -scheme PayU -configuration Debug -sdk iphonesimulator -derivedDataPath ios/build',
    },
    'android.debug': {
      type: 'android.apk',
      binaryPath: 'android/app/build/outputs/apk/debug/app-debug.apk',
      build: 'cd android && ./gradlew assembleDebug assembleAndroidTest -DtestBuildType=debug',
    },
  },
  devices: {
    simulator: {
      type: 'ios.simulator',
      device: {
        type: 'iPhone 15 Pro',
      },
    },
    emulator: {
      type: 'android.emulator',
      device: {
        avdName: 'Pixel_7_API_34',
      },
    },
  },
  configurations: {
    'ios.sim.debug': {
      device: 'simulator',
      app: 'ios.debug',
    },
    'android.emu.debug': {
      device: 'emulator',
      app: 'android.debug',
    },
  },
};
```

**Detox Test Example:**
```javascript
// e2e/auth/login.test.js
describe('Authentication Flow', () => {
  beforeAll(async () => {
    await device.launchApp({
      newInstance: true,
      permissions: { notifications: 'YES', faceid: 'YES' },
    });
  });

  it('should login with valid credentials', async () => {
    await element(by.id('email-input')).typeText('testuser@payu.id');
    await element(by.id('password-input')).typeText('TestPassword123!');
    await element(by.id('login-button')).tap();
    
    await expect(element(by.text('Welcome back'))).toBeVisible();
  });

  it('should show error for invalid credentials', async () => {
    await element(by.id('email-input')).typeText('wrong@payu.id');
    await element(by.id('password-input')).typeText('wrongpassword');
    await element(by.id('login-button')).tap();
    
    await expect(element(by.text('Invalid credentials'))).toBeVisible();
  });
});

// e2e/transactions/transfer.test.js
describe('Transfer Flow', () => {
  beforeEach(async () => {
    await device.reloadReactNative();
    await loginWithTestUser(); // Helper function
  });

  it('should complete transfer with PIN', async () => {
    // Navigate to transfer
    await element(by.id('transfer-tab')).tap();
    await element(by.id('new-transfer-button')).tap();
    
    // Select recipient
    await element(by.id('recipient-search')).typeText('08123456789');
    await element(by.text('John Doe')).tap();
    
    // Enter amount
    await element(by.id('amount-input')).typeText('100000');
    
    // Continue
    await element(by.id('continue-button')).tap();
    
    // Confirm with PIN
    await element(by.id('pin-input')).typeText('123456');
    await element(by.id('confirm-button')).tap();
    
    // Verify success
    await expect(element(by.text('Transfer successful'))).toBeVisible();
    await expect(element(by.text('Rp 100.000'))).toBeVisible();
  });

  it('should require biometric for large transfers', async () => {
    await element(by.id('transfer-tab')).tap();
    await element(by.id('new-transfer-button')).tap();
    
    // Select recipient
    await element(by.id('recipient-search')).typeText('08123456789');
    await element(by.text('John Doe')).tap();
    
    // Enter large amount (> 10M)
    await element(by.id('amount-input')).typeText('15000000');
    await element(by.id('continue-button')).tap();
    
    // Should require biometric
    await expect(element(by.text('Authenticate with Face ID'))).toBeVisible();
    
    // Simulate biometric success
    await device.matchFace();
    
    // Enter PIN
    await element(by.id('pin-input')).typeText('123456');
    await element(by.id('confirm-button')).tap();
    
    await expect(element(by.text('Transfer successful'))).toBeVisible();
  });
});

// e2e/helpers.js
export async function loginWithTestUser() {
  await element(by.id('email-input')).typeText('testuser@payu.id');
  await element(by.id('password-input')).typeText('TestPassword123!');
  await element(by.id('login-button')).tap();
  await waitFor(element(by.text('Welcome back'))).toBeVisible().withTimeout(5000);
}

export async function logout() {
  await element(by.id('profile-tab')).tap();
  await element(by.id('logout-button')).tap();
  await element(by.id('confirm-logout')).tap();
}
```

**Running Detox Tests:**
```bash
# Build the app
detox build --configuration ios.sim.debug

# Run tests
detox test --configuration ios.sim.debug

# Run specific test file
detox test --configuration ios.sim.debug e2e/auth/login.test.js

# Run with artifacts (screenshots/videos)
detox test --configuration ios.sim.debug --artifacts-location ./artifacts
```

### Mobile Testing Best Practices

**1. Test IDs for Elements:**
```typescript
// Always add testID for testable elements
<Button
  testID="login-button"
  onPress={handleLogin}
  title="Login"
/>

<TextInput
  testID="email-input"
  value={email}
  onChangeText={setEmail}
  placeholder="Email"
/>
```

**2. Mock External Services:**
```javascript
// e2e/mocks/server.js
const mockServer = {
  setup: () => {
    // Mock BI-FAST responses
    mockResponse('/api/v1/transfers/bifast', {
      status: 'SUCCESS',
      referenceNumber: 'BIF20240101123456',
    });
    
    // Mock QRIS generation
    mockResponse('/api/v1/qris/generate', {
      qrString: '000201010212...',
      expiryTime: '2024-01-01T12:00:00Z',
    });
  },
};
```

**3. Test Data Management:**
```yaml
# .maestro/config.yaml
flows:
  - name: Setup Test Data
    file: setup.yaml
  - name: Run Tests
    file: run-tests.yaml
  - name: Cleanup Test Data
    file: cleanup.yaml
```

**4. CI/CD Integration:**
```yaml
# .github/workflows/mobile-e2e.yml
name: Mobile E2E Tests
on: [push]
jobs:
  ios-e2e:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm install
      - run: npm run ios:build:e2e
      - run: maestro test .maestro/
      - uses: actions/upload-artifact@v3
        if: failure()
        with:
          name: maestro-screenshots
          path: ~/.maestro/tests/
```

**5. Performance Testing:**
```javascript
// e2e/performance/startup.test.js
describe('App Performance', () => {
  it('should start within 3 seconds', async () => {
    const startTime = Date.now();
    await device.launchApp({ newInstance: true });
    const endTime = Date.now();
    
    const startupTime = endTime - startTime;
    expect(startupTime).toBeLessThan(3000);
  });

  it('should handle 1000 transactions list smoothly', async () => {
    await loginWithTestUser();
    await element(by.id('history-tab')).tap();
    
    // Scroll through long list
    await element(by.id('transaction-list')).scroll(1000, 'down');
    
    // Verify no crashes
    await expect(element(by.id('transaction-list'))).toBeVisible();
  });
});
```

### Mobile Test Priorities

| Priority | Test Type | Example |
|----------|-----------|---------|
| **P0** | Critical Flows | Login, Transfer, Balance Check |
| **P0** | Security | Biometric auth, PIN validation |
| **P1** | Business Logic | Transaction limits, Fee calculation |
| **P1** | Error Handling | Network errors, Timeout handling |
| **P2** | UI/UX | Animations, Loading states |
| **P3** | Edge Cases | Very long names, Special characters |
        .post("/v1/transfers");
    
    assertThat(first.body().path("data.id"))
        .isEqualTo(second.body().path("data.id"));
}
```

## 🤖 Agent Delegation & Parallel Execution

Untuk cakupan testing yang masif tanpa menghambat kecepatan, gunakan pola delegasi paralel (Swarm Mode):

- **Standard Testing**: Delegasikan ke **`@tester`** untuk setup Testcontainers dan penulisan JUnit/FastAPI boilerplate.
- **Architecture & Security Audit**: Jalankan **`@auditor`** secara paralel untuk melakukan ArchUnit check dan security scan pada level kode.
- **Performance Benchmarking**: Jika fitur butuh load test, aktifkan **`@builder`** (untuk build optimasi) dan masifkan eksekusi Gatling secara paralel.
- **Visual/Design QA**: Jika testing menyangkut UI (Web/Mobile), panggil **`@styler`** untuk memverifikasi kesesuaian "Premium Emerald" design.
