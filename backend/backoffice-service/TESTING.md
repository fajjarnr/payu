# Testing Guide for Backoffice Service

This document explains how to run and organize tests in the backoffice-service (Spring Boot).

## Test Types

### Service Tests (Default)
Service tests validate application services with a real PostgreSQL test database.
- **Location**: `src/test/java/id/payu/backoffice/service/`
- **Database**: PostgreSQL (local)
- **Docker Required**: No (if local DB is running)
- **Annotations**: `@SpringBootTest`, `@ActiveProfiles("test")`

### Integration Tests (Docker-gated)
Integration tests validate end-to-end workflows against the database.
- **Location**: `src/test/java/id/payu/backoffice/integration/`
- **Database**: PostgreSQL (Docker or local)
- **Docker Required**: Yes
- **Annotations**: `@IntegrationTest`, `@SpringBootTest`, `@ActiveProfiles("integrationtest")`

### REST API Tests (Docker-gated)
Resource tests validate REST endpoints with RestAssured.
- **Location**: `src/test/java/id/payu/backoffice/resource/`
- **Database**: PostgreSQL (Docker or local)
- **Docker Required**: Yes
- **Annotations**: `@IntegrationTest`, `@SpringBootTest(webEnvironment = RANDOM_PORT)`

### Architecture Tests
Architecture tests enforce layering rules.
- **Location**: `src/test/java/id/payu/backoffice/ArchitectureTest.java`
- **Database**: None

## Running Tests

```bash
# Service + architecture tests (requires local PostgreSQL)
./mvnw test

# Integration + resource tests (requires Docker)
./mvnw test -Ddocker.enabled=true -Dspring.profiles.active=integrationtest

# Run a specific class
./mvnw test -Dtest=CustomerCaseServiceTest

# Run a specific method
./mvnw test -Dtest=CustomerCaseServiceTest#testCreateCustomerCase_Success

# Architecture tests only
./mvnw test -Dtest=ArchitectureTest
```

## Test Configuration

### application-test.properties
- **Location**: `src/test/resources/application-test.properties`
- **Profile**: `test`
- **Purpose**: Local PostgreSQL config for service tests

### application-integrationtest.properties
- **Location**: `src/test/resources/application-integrationtest.properties`
- **Profile**: `integrationtest`
- **Purpose**: Integration/resource test config (Docker-gated)

## Docker Requirements

Integration/resource tests are gated by `@IntegrationTest` and only run when:
- `-Ddocker.enabled=true` is set
- Docker is running

## Troubleshooting

### Tests fail with "Connection refused"
- Ensure PostgreSQL is running locally or via Docker
- Verify DB credentials in `application-test.properties`

### Integration tests are skipped
- Add `-Ddocker.enabled=true` to your Maven command

### REST API tests return 401
- Ensure test security config is set up or provide valid JWTs for secured endpoints

