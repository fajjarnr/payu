# Test Structure

This directory contains all test code for the backoffice-service.

## Directory Organization

```
src/test/java/id/payu/backoffice/
├── resource/
│   ├── BackofficeResourceTest.java           # REST API tests (Unit tests with H2)
│   └── UniversalSearchResourceTest.java      # Search API tests (Unit tests with H2)
├── service/
│   ├── CustomerCaseServiceTest.java          # Service layer tests (Unit tests with H2)
│   ├── FraudCaseServiceTest.java             # Service layer tests (Unit tests with H2)
│   ├── KycReviewServiceTest.java             # Service layer tests (Unit tests with H2)
│   └── UniversalSearchServiceTest.java       # Service layer tests (Unit tests with H2)
├── testutil/
│   ├── IntegrationTest.java                  # Marker annotation for integration tests
│   └── PostgreSQLResourceTestLifecycleManager.java  # Testcontainers configuration
└── ArchitectureTest.java                      # Architecture validation tests
```

## Test Categories

### 1. Unit Tests (Default)
All tests in `resource/` and `service/` packages are **unit tests** that:
- Use `@QuarkusTest` annotation
- Run with H2 in-memory database
- Do NOT require Docker
- Can be run with: `./mvnw test`

### 2. Integration Tests (Optional)
Integration tests (to be added) would:
- Use `@IntegrationTest` annotation
- Run with PostgreSQL via Testcontainers
- Require Docker to be running
- Can be run with: `./mvnw test -Ddocker.enabled=true`

### 3. Architecture Tests
The `ArchitectureTest` class:
- Uses ArchUnit to validate layering
- Does not require a database
- Runs with all tests by default

## Test Configuration Files

### application.properties
Configures H2 database for unit tests.

### application-integrationtest.properties
Configures PostgreSQL for integration tests.

## Adding New Tests

### To add unit tests:
```java
package id.payu.backoffice.service;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MyServiceTest {
    @Test
    void testSomething() {
        // Test code here
    }
}
```

### To add integration tests:
```java
package id.payu.backoffice.repository;

import id.payu.backoffice.testutil.IntegrationTest;
import id.payu.backoffice.testutil.PostgreSQLResourceTestLifecycleManager;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@IntegrationTest
@QuarkusTest
@QuarkusTestResource(PostgreSQLResourceTestLifecycleManager.class)
class MyRepositoryIntegrationTest {
    @Test
    void testDatabaseIntegration() {
        // Test code with real PostgreSQL
    }
}
```

## Running Tests

See [TESTING.md](../../TESTING.md) for detailed instructions.

### Quick Reference:
```bash
# Run all unit tests (default, no Docker)
./mvnw test

# Run integration tests (requires Docker)
./mvnw test -Ddocker.enabled=true

# Run specific test class
./mvnw test -Dtest=CustomerCaseServiceTest

# Run specific test method
./mvnw test -Dtest=CustomerCaseServiceTest#testCreateCustomerCase_Success
```

## Current Test Count

- Unit Tests: 60+ tests across 6 test classes
- Integration Tests: 0 (to be added)
- Architecture Tests: 1 test class

## Coverage

As of the latest test run:
- **Total Tests**: 79
- **Passing**: 79
- **Coverage**: 83%
- **Docker Required**: 0 tests (all use H2)
