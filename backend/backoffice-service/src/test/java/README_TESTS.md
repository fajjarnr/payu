# Test Structure

This directory contains all test code for the backoffice-service (Spring Boot).

## Directory Organization

```
src/test/java/id/payu/backoffice/
├── integration/
│   └── BackofficeIntegrationTest.java         # Integration tests (Docker + PostgreSQL)
├── resource/
│   ├── BackofficeResourceTest.java           # REST API tests (Docker + PostgreSQL)
│   └── UniversalSearchResourceTest.java      # Search API tests (currently disabled)
├── service/
│   ├── CustomerCaseServiceTest.java          # Service layer tests
│   ├── FraudCaseServiceTest.java             # Service layer tests
│   ├── KycReviewServiceTest.java             # Service layer tests
│   └── UniversalSearchServiceTest.java       # Service layer tests (currently disabled)
├── testutil/
│   └── IntegrationTest.java                  # Marker annotation for Docker-gated tests
└── ArchitectureTest.java                      # Architecture validation tests
```

## Test Categories

### 1. Service Tests (Default)
Tests in `service/` packages are Spring Boot tests that require a PostgreSQL test database.
- Use `@SpringBootTest` and `@ActiveProfiles("test")`
- Configured by `src/test/resources/application-test.properties`
- Run with: `./mvnw test`

### 2. Integration Tests (Require Docker)
Tests in `integration/` package validate end-to-end service logic.
- Use `@IntegrationTest` + `@SpringBootTest`
- Profile: `integrationtest`
- Require Docker + PostgreSQL available
- Run with: `./mvnw test -Ddocker.enabled=true -Dspring.profiles.active=integrationtest`

### 3. REST API Tests (Require Docker)
Tests in `resource/` package validate REST endpoints with RestAssured.
- Use `@IntegrationTest` + `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- Profile: `integrationtest`
- Require Docker + PostgreSQL available
- Run with: `./mvnw test -Ddocker.enabled=true -Dspring.profiles.active=integrationtest`

### 4. Architecture Tests
The `ArchitectureTest` class uses ArchUnit and does not require a database.

## Test Configuration Files

### application-test.properties
- Location: `src/test/resources/application-test.properties`
- Used by tests with `@ActiveProfiles("test")`
- Points to local PostgreSQL test DB

### application-integrationtest.properties
- Location: `src/test/resources/application-integrationtest.properties`
- Activated by `-Dspring.profiles.active=integrationtest`
- Used for Docker-gated integration/resource tests

## Running Tests (Quick Reference)

```bash
# Run service + architecture tests (requires local PostgreSQL)
./mvnw test

# Run integration + resource tests (requires Docker)
./mvnw test -Ddocker.enabled=true -Dspring.profiles.active=integrationtest

# Run a specific test class
./mvnw test -Dtest=CustomerCaseServiceTest

# Run a specific test method
./mvnw test -Dtest=CustomerCaseServiceTest#testCreateCustomerCase_Success

# Run architecture tests only
./mvnw test -Dtest=ArchitectureTest
```

## Prerequisites

### For Service Tests
- PostgreSQL running on `localhost:5432`
- Database `backoffice_test` with user `test` / `test`

### For Integration/Resource Tests
- Docker running
- PostgreSQL available (via Docker or local)
- Run with `-Ddocker.enabled=true`

