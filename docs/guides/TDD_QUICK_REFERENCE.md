# TDD Quick Reference Card

> PayU Digital Banking Platform - Test-Driven Development Guide

---

## 🔴🟢🔵 TDD Cycle

```
┌─────────────────────────────────────────────────────────┐
│                    TDD CYCLE                            │
│                                                         │
│    🔴 RED ──────► 🟢 GREEN ──────► 🔵 REFACTOR         │
│      │              │                │                  │
│      │              │                │                  │
│      ▼              ▼                ▼                  │
│   Write a        Make it          Make it              │
│   failing        pass with        clean &              │
│   test first     minimal code     maintainable         │
│                                                         │
│                  ◄────────────────────┘                 │
│                     Repeat                              │
└─────────────────────────────────────────────────────────┘
```

---

## 📋 Test Naming Convention

```java
// Pattern: should_ExpectedBehavior_When_StateUnderTest
@Test
void should_CreateUser_When_ValidDataProvided() { }

@Test
void should_ThrowException_When_EmailAlreadyExists() { }

@Test
void should_ReturnEmpty_When_NoTransactionsFound() { }
```

---

## 🏗️ Test Structure (AAA Pattern)

```java
@Test
void should_TransferMoney_When_SufficientBalance() {
    // Arrange - Setup test data and mocks
    var sender = new Account("sender", 1000.0);
    var receiver = new Account("receiver", 0.0);
    when(accountRepo.findById("sender")).thenReturn(Optional.of(sender));
    when(accountRepo.findById("receiver")).thenReturn(Optional.of(receiver));
    
    // Act - Execute the method under test
    var result = transferService.transfer("sender", "receiver", 500.0);
    
    // Assert - Verify the expected outcome
    assertThat(result.isSuccess()).isTrue();
    assertThat(sender.getBalance()).isEqualTo(500.0);
    assertThat(receiver.getBalance()).isEqualTo(500.0);
    verify(eventPublisher).publish(any(TransferCompletedEvent.class));
}
```

---

## 🎯 Test Pyramid

```
                    ┌───────┐
                   /   E2E   \         5-10%
                  /  (Slow)   \        Integration with UI
                 ┌─────────────┐
                /  Integration  \      20-30%
               /   (Medium)      \     Service + DB/Kafka
              ┌───────────────────┐
             /     Unit Tests      \   60-70%
            /      (Fast)           \  Isolated, Mocked
           └─────────────────────────┘
```

---

## 🛠️ Quick Commands

### Java (Spring Boot)
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AccountServiceTest

# Run with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Java (Quarkus)
```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=BillingServiceTest

# Run in continuous mode
./mvnw quarkus:test
```

### Python (FastAPI)
```bash
# Run all tests
pytest -v

# Run specific file
pytest tests/test_ocr_service.py -v

# With coverage
pytest --cov=src --cov-report=html

# View coverage
open htmlcov/index.html
```

---

## 📊 Coverage Targets

| Layer | Target | Rationale |
|-------|--------|-----------|
| Service/Domain | 85%+ | Core business logic |
| Controller/API | 80%+ | Request/response handling |
| Repository | 70%+ | Data access layer |
| Utils/Helpers | 90%+ | Pure functions |
| Config | 50%+ | Spring configuration |

---

## ✅ Test Checklist

Before committing, ensure:

- [ ] All tests pass: `mvn test` or `pytest`
- [ ] No compilation errors: `mvn compile`
- [ ] Architecture rules pass: `mvn test -Dtest=*ArchTest`
- [ ] New code has tests (80%+ coverage)
- [ ] Edge cases covered (null, empty, invalid)
- [ ] Error scenarios tested
- [ ] No hardcoded test data (use builders/fixtures)

---

## 🎭 Mocking Cheat Sheet

### Mockito (Java)
```java
// Mock a dependency
@Mock private AccountRepository accountRepo;
@InjectMocks private AccountService accountService;

// Stub a return value
when(accountRepo.findById("123")).thenReturn(Optional.of(account));

// Stub void method
doNothing().when(eventPublisher).publish(any());

// Verify interaction
verify(accountRepo, times(1)).save(any(Account.class));
verify(eventPublisher, never()).publish(any());

// Capture argument
ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
verify(accountRepo).save(captor.capture());
assertThat(captor.getValue().getEmail()).isEqualTo("test@payu.id");
```

### pytest (Python)
```python
# Mock a dependency
@pytest.fixture
def mock_db(mocker):
    return mocker.patch('src.repositories.user_repo.find_by_id')

# Use in test
def test_get_user(mock_db):
    mock_db.return_value = User(id="123", name="Test")
    result = user_service.get_user("123")
    assert result.name == "Test"
    mock_db.assert_called_once_with("123")
```

---

## 🏛️ Architecture Test Examples

```java
@AnalyzeClasses(packages = "id.payu.account")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule controllers_should_only_call_services =
        classes()
            .that().resideInAPackage("..api..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..application..", "..domain..", 
                               "java..", "jakarta..", "org.springframework..");
}
```

---

## 🔥 Common Mistakes to Avoid

| ❌ Don't | ✅ Do |
|----------|-------|
| Test implementation details | Test behavior/outcomes |
| Use real database in unit tests | Use Mockito/in-memory |
| Skip edge cases | Test null, empty, invalid |
| Write tests after code | Write tests first (RED-GREEN) |
| Copy-paste test data | Use test fixtures/builders |
| Ignore flaky tests | Fix or quarantine them |
| Test private methods | Test through public API |

---

## 📚 Resources

- **ArchUnit Guide**: https://www.archunit.org/userguide/html/000_Index.html
- **Mockito Docs**: https://site.mockito.org/
- **AssertJ Fluent Assertions**: https://assertj.github.io/doc/
- **Testcontainers**: https://www.testcontainers.org/
- **pytest**: https://docs.pytest.org/

---

_PayU Engineering Team - January 2026_
