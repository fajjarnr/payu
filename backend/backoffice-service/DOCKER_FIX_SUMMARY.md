# Docker Dependency Fix Summary (Backoffice Service)

## Problem Statement
Backoffice tests had unclear Docker requirements and inconsistent documentation:
- Docs referenced Quarkus/Testcontainers utilities that no longer exist
- Unit vs integration test boundaries were unclear
- Integration tests were not clearly gated

## Current Approach (Spring Boot)

### 1. Test Profiles
- **`test` profile**: Service tests using local PostgreSQL
- **`integrationtest` profile**: Integration/resource tests (Docker-gated)

Config files:
- `src/test/resources/application-test.properties`
- `src/test/resources/application-integrationtest.properties`

### 2. Integration Test Gating
- Integration and resource tests use `@IntegrationTest`
- Tests only run when `-Ddocker.enabled=true` is provided

### 3. Maven Execution
```bash
# Service tests (local PostgreSQL)
./mvnw test

# Integration + resource tests (Docker)
./mvnw test -Ddocker.enabled=true -Dspring.profiles.active=integrationtest
```

## Test Categories

### Service Tests (Default)
- **Location**: `src/test/java/id/payu/backoffice/service/`
- **DB**: Local PostgreSQL (`backoffice_test`)
- **Docker**: Not required

### Integration Tests (Docker-gated)
- **Location**: `src/test/java/id/payu/backoffice/integration/`
- **DB**: PostgreSQL (Docker or local)
- **Docker**: Required

### Resource Tests (Docker-gated)
- **Location**: `src/test/java/id/payu/backoffice/resource/`
- **DB**: PostgreSQL (Docker or local)
- **Docker**: Required

### Architecture Tests
- **Location**: `src/test/java/id/payu/backoffice/ArchitectureTest.java`
- **DB**: Not required

## Files Updated
- `src/test/resources/application-test.properties`
- `src/test/resources/application-integrationtest.properties`
- `src/test/java/README_TESTS.md`
- `TESTING.md`

## Notes
- If REST API tests return 401, ensure test security setup (mocked JWTs or permissive test security) is configured.
- For full-stack testing, run Docker/Podman compose environment.

