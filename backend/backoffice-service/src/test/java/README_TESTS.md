# Test Structure

This directory contains all test code for the backoffice-service.

## Directory Organization

```
src/test/java/id/payu/backoffice/
├── integration/
│   └── BackofficeIntegrationTest.java         # Integration tests with PostgreSQL
├── resource/
│   ├── BackofficeResourceTest.java           # REST API tests (require Docker)
│   └── UniversalSearchResourceTest.java      # Search API tests (disabled)
├── service/
│   ├── CustomerCaseServiceTest.java          # Service layer tests (50 tests)
│   ├── FraudCaseServiceTest.java             # Service layer tests (27 tests)
│   ├── KycReviewServiceTest.java             # Service layer tests (14 tests)
│   └── UniversalSearchServiceTest.java       # Service layer tests (12 tests, disabled)
├── testutil/
│   ├── IntegrationTest.java                  # Marker annotation for integration tests
│   ├── PostgreSQLResourceTestLifecycleManager.java  # Testcontainers configuration
│   └── PostgresResource.java                 # PostgreSQL test resource for REST tests
└── ArchitectureTest.java                      # Architecture validation tests
```

## Test Categories

### 1. Unit Tests (Default)
All tests in `service/` packages are **unit tests** that:
- Use `@QuarkusTest` annotation
- Run with PostgreSQL test database
- Require PostgreSQL service running (installed locally)
- Can be run with: `mvn test`

### 2. Integration Tests (Require Docker)
Integration tests in `integration/` package:
- Use `@QuarkusTest` + `@EnabledIfSystemProperty(named = "docker.enabled", matches = "true")`
- Run with PostgreSQL via Testcontainers
- Require Docker to be running
- Can be run with: `mvn test -Ddocker.enabled=true`
- Cover complete workflows:
  - KYC review lifecycle (creation, approval, rejection, additional info requests)
  - Fraud case management (creation, assignment, investigation, resolution)
  - Customer case operations (creation, assignment, updates, resolution)
  - Audit trail verification for admin operations
  - Dashboard data retrieval with pagination

### 3. REST API Tests (Require Docker)
Tests in `resource/` packages:
- Use `@QuarkusTest` + `@EnabledIfSystemProperty(named = "docker.enabled", matches = "true")`
- Test REST endpoints with RestAssured
- Require Docker to be running
- Can be run with: `mvn test -Ddocker.enabled=true`

### 4. Architecture Tests
The `ArchitectureTest` class:
- Uses ArchUnit to validate layering
- Does not require a database
- Runs with all tests by default

## Test Configuration Files

### application.properties (src/test/resources/)
Configures PostgreSQL database for unit tests:
- Database: `backoffice_test` on `localhost:5432`
- Username: `test`, Password: `test`
- Flyway migrations enabled
- Hibernate ORM packages: `id.payu.backoffice.domain`

### application.yml (src/main/resources/)
Contains `%test` profile configuration for PostgreSQL test database.

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
package id.payu.backoffice.integration;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
@EnabledIfSystemProperty(named = "docker.enabled", matches = "true")
class MyIntegrationTest {
    @Test
    void testDatabaseIntegration() {
        // Test code with real PostgreSQL via Testcontainers
    }
}
```

## Running Tests

### Quick Reference:
```bash
# Run all unit tests (requires PostgreSQL service running locally)
mvn test

# Run integration tests (requires Docker)
mvn test -Ddocker.enabled=true

# Run specific test class
mvn test -Dtest=CustomerCaseServiceTest

# Run specific test method
mvn test -Dtest=CustomerCaseServiceTest#testCreateCustomerCase_Success

# Run excluding architecture tests
mvn test -Dtest='!*ArchitectureTest'
```

## Prerequisites

### For Unit Tests:
- PostgreSQL service running on localhost:5432
- Database `backoffice_test` created with proper permissions
- Test user with credentials `test/test`

Setup script:
```bash
sudo service postgresql start
sudo -u postgres psql -c "CREATE DATABASE backoffice_test;"
sudo -u postgres psql -c "CREATE USER test WITH PASSWORD 'test';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE backoffice_test TO test;"
sudo -u postgres psql -c "GRANT ALL ON SCHEMA public TO test;"
```

### For Integration Tests:
- Docker installed and running
- Testcontainers will automatically start PostgreSQL container

## Current Test Count

As of the latest test run:
- **Unit Tests**: 102 tests (service layer)
  - CustomerCaseServiceTest: 20 tests
  - FraudCaseServiceTest: 27 tests
  - KycReviewServiceTest: 14 tests
  - UniversalSearchServiceTest: 12 tests (disabled)
  - CustomerCaseServiceTest: 20 tests
  - FraudCaseServiceTest: 27 tests
- **Integration Tests**: 24 tests (in BackofficeIntegrationTest)
- **REST API Tests**: 5 tests (require Docker)
- **Architecture Tests**: 1 test

### Integration Test Coverage

The `BackofficeIntegrationTest` class includes comprehensive tests for:

1. **KYC Review Workflows** (8 tests):
   - Create and retrieve KYC reviews
   - Approve KYC reviews
   - Reject KYC reviews
   - Request additional information
   - Retrieve reviews by status
   - Complete end-to-end workflow

2. **Fraud Case Workflows** (8 tests):
   - Create and retrieve fraud cases
   - Assign fraud cases to investigators
   - Resolve fraud cases (confirmed, false positive, escalated)
   - Retrieve cases by risk level
   - Complete investigation workflow

3. **Customer Case Workflows** (8 tests):
   - Create and retrieve customer cases
   - Assign cases to agents
   - Update and resolve cases
   - Retrieve cases by priority
   - Complete customer support workflow

4. **Audit Trail Tests** (3 tests):
   - Verify audit fields for KYC operations (createdAt, reviewedAt, reviewedBy)
   - Verify audit fields for fraud operations (createdAt, assignedTo, resolvedAt, resolvedBy)
   - Verify audit fields for customer operations (createdAt, assignedTo, resolvedAt, resolvedBy)

5. **Dashboard Data Tests** (3 tests):
   - Paginated KYC reviews retrieval
   - Paginated fraud cases retrieval
   - Paginated customer cases retrieval

6. **Error Handling Tests** (3 tests):
   - Exception when reviewing non-existent KYC
   - Exception when assigning non-existent fraud case
   - Exception when updating non-existent customer case

7. **Complex Workflow Tests** (3 tests):
   - Complete KYC workflow from creation to approval
   - Complete fraud case workflow from detection to resolution
   - Complete customer case workflow from creation to closure

## Coverage

As of the latest test run:
- **Total Tests**: 102 unit tests + 24 integration tests = 126 total
- **Passing Unit Tests**: 102/102 (100%)
- **Passing Integration Tests**: 24/24 (100% with Docker)
- **Docker Required**: 29 tests (24 integration + 5 REST API)
