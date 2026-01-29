# Testing Guide for Backoffice Service

This document explains how to run and organize tests in the backoffice-service.

## Test Types

### Unit Tests
Unit tests test individual components in isolation with mocked dependencies.
- **Location**: `src/test/java/id/payu/backoffice/**/`
- **Database**: H2 (in-memory)
- **Docker Required**: No
- **Annotation**: `@QuarkusTest`

### Integration Tests
Integration tests test the full application with real database connections.
- **Location**: `src/test/java/id/payu/backoffice/**/*IntegrationTest.java`
- **Database**: PostgreSQL (via Testcontainers)
- **Docker Required**: Yes
- **Annotation**: `@IntegrationTest`, `@QuarkusTest`

### Architecture Tests
Architecture tests enforce code structure and layering rules.
- **Location**: `src/test/java/id/payu/backoffice/ArchitectureTest.java`
- **Database**: None
- **Docker Required**: No
- **Annotation**: `@AnalyzeClasses`

## Running Tests

### Run All Tests (Unit + Architecture)
```bash
./mvnw test
```

### Run Only Unit Tests
```bash
./mvnw test -Dtest.exclude.integration=true
```

### Run Integration Tests (Requires Docker)
```bash
./mvnw test -Ddocker.enabled=true
```

### Run Specific Test Class
```bash
# Unit test
./mvnw test -Dtest=CustomerCaseServiceTest

# Integration test (requires Docker)
./mvnw test -Ddocker.enabled=true -Dtest=*IntegrationTest
```

### Run Specific Test Method
```bash
./mvnw test -Dtest=CustomerCaseServiceTest#testCreateCustomerCase_Success
```

### Run Architecture Tests Only
```bash
./mvnw test -Dtest=ArchitectureTest
```

## Test Configuration

### H2 Configuration (Unit Tests)
- **File**: `src/test/resources/application.properties`
- **Database**: H2 in-memory
- **Generation**: drop-and-create (fresh schema for each test)
- **Flyway**: Disabled

### PostgreSQL Configuration (Integration Tests)
- **File**: `src/test/resources/application-integrationtest.properties`
- **Database**: PostgreSQL 16 (via Testcontainers)
- **Generation**: validate (uses Flyway migrations)
- **Flyway**: Enabled

## Test Profiles

### Unit Test Profile
The default test profile uses H2:
```properties
%test.quarkus.datasource.db-kind=h2
%test.quarkus.datasource.jdbc.url=jdbc:h2:mem:test;MODE=PostgreSQL
%test.quarkus.hibernate-orm.database.generation=drop-and-create
%test.quarkus.flyway.migrate-at-start=false
```

### Integration Test Profile
The integration-test profile uses PostgreSQL:
```properties
%integration-test.quarkus.datasource.db-kind=postgresql
%integration-test.quarkus.flyway.migrate-at-start=true
```

## Test Annotations

### @IntegrationTest
Marker annotation for integration tests requiring Docker:
```java
@IntegrationTest
@QuarkusTest
@QuarkusTestResource(PostgreSQLResourceTestLifecycleManager.class)
class MyRepositoryIntegrationTest {
    @Test
    void testDatabaseIntegration() {
        // Test with real PostgreSQL
    }
}
```

### @QuarkusTest
Standard Quarkus test annotation for unit tests:
```java
@QuarkusTest
class MyServiceTest {
    @Inject
    MyService myService;

    @Test
    void testBusinessLogic() {
        // Test with H2
    }
}
```

## Current Test Coverage

### Service Layer (Unit Tests)
- `CustomerCaseServiceTest` - 17 tests
- `FraudCaseServiceTest` - 15 tests
- `KycReviewServiceTest` - 16 tests
- `UniversalSearchServiceTest` - 12 tests

### Resource Layer (Unit Tests)
- `BackofficeResourceTest` - 5 tests
- `UniversalSearchResourceTest` - 10 tests

### Architecture Tests
- `ArchitectureTest` - Layered architecture validation

## Docker Requirements

Integration tests use Testcontainers to run PostgreSQL in Docker. To run integration tests:

1. Install Docker Desktop or Docker Engine
2. Start Docker daemon
3. Run tests with `-Ddocker.enabled=true`

Example:
```bash
# Verify Docker is running
docker ps

# Run integration tests
./mvnw test -Ddocker.enabled=true
```

## Troubleshooting

### Tests Fail with "Docker not enabled"
**Cause**: Trying to run integration tests without enabling Docker
**Solution**: Add `-Ddocker.enabled=true` to the test command

### Tests Fail with "Connection refused"
**Cause**: Docker daemon is not running
**Solution**: Start Docker Desktop or Docker Engine

### Tests Fail with "Container startup failed"
**Cause**: Insufficient Docker resources or port conflicts
**Solution**:
- Check Docker logs: `docker logs <container-id>`
- Free up ports: `lsof -i :5432`
- Restart Docker daemon

### H2 Tests Fail with "Table not found"
**Cause**: Schema generation issue
**Solution**: Ensure `%test.quarkus.hibernate-orm.database.generation=drop-and-create`

## Adding New Tests

### Adding Unit Tests
1. Create test class in `src/test/java/id/payu/backoffice/`
2. Annotate with `@QuarkusTest`
3. Use `@Inject` for dependencies
4. Tests will automatically use H2

### Adding Integration Tests
1. Create test class ending with `IntegrationTest`
2. Annotate with `@IntegrationTest` and `@QuarkusTest`
3. Add `@QuarkusTestResource(PostgreSQLResourceTestLifecycleManager.class)`
4. Tests will use PostgreSQL via Testcontainers

Example:
```java
package id.payu.backoffice.repository;

import id.payu.backoffice.testutil.IntegrationTest;
import id.payu.backoffice.testutil.PostgreSQLResourceTestLifecycleManager;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
@QuarkusTest
@QuarkusTestResource(PostgreSQLResourceTestLifecycleManager.class)
class CustomerCaseRepositoryIntegrationTest {

    @Inject
    CustomerCaseRepository repository;

    @Test
    void testSaveAndRetrieve() {
        // Test with real PostgreSQL
    }
}
```

## Test Execution Best Practices

1. **Run unit tests frequently** during development (fast, no Docker)
2. **Run integration tests before committing** (validates database interactions)
3. **Run architecture tests after refactor** (validates code structure)
4. **Run all tests in CI/CD** with Docker enabled

## CI/CD Integration

```yaml
# GitHub Actions example
- name: Run Unit Tests
  run: ./mvnw test

- name: Run Integration Tests
  run: ./mvnw test -Ddocker.enabled=true
```

```yaml
# Jenkins example
stage('Test') {
  parallel {
    stage('Unit Tests') {
      steps {
        sh './mvnw test'
      }
    }
    stage('Integration Tests') {
      steps {
        sh './mvnw test -Ddocker.enabled=true'
      }
    }
  }
}
```

## References

- [Quarkus Testing Guide](https://quarkus.io/guides/testing)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
