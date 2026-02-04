# Integration Tests for Backoffice Service

## Overview

Integration tests verify end-to-end functionality against a real PostgreSQL database using Spring Boot.
Tests are Docker-gated via `@IntegrationTest` and run only when `-Ddocker.enabled=true` is set.

## Test Location

- **File**: `backend/backoffice-service/src/test/java/id/payu/backoffice/integration/BackofficeIntegrationTest.java`

## Test Coverage (Summary)

The integration suite covers:
- KYC review lifecycle (create, approve/reject, status queries)
- Fraud case management (create, assign, resolve, queries)
- Customer case operations (create, update, resolve, queries)
- Dashboard list retrieval
- Error handling for invalid IDs

## Running the Tests

```bash
cd backend/backoffice-service

# Run integration tests (Docker-gated)
./mvnw test -Ddocker.enabled=true -Dspring.profiles.active=integrationtest

# Run only the integration class
./mvnw test -Ddocker.enabled=true -Dspring.profiles.active=integrationtest -Dtest=BackofficeIntegrationTest
```

## Requirements

- Docker running (or a locally available PostgreSQL instance)
- PostgreSQL database `backoffice_test` accessible
- Test profile configured in `src/test/resources/application-integrationtest.properties`

## Notes

- Tests use `@Transactional` where DB writes occur for isolation
- If REST API tests return 401, ensure test security setup or provide valid JWTs

## References

- JUnit 5: https://junit.org/junit5/docs/current/user-guide/
- Spring Boot Testing: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing

