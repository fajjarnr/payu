# Test Configuration Templates

Standardized test configuration templates for PayU Digital Banking Platform microservices. These templates ensure consistent testing patterns across all services and prevent common configuration pitfalls.

## Overview

This directory contains platform-standard templates for configuring tests in different technology stacks used across PayU services:

- **Quarkus 3.x Native** services (gateway-service, billing-service, notification-service, api-portal-service)
- **FastAPI/Python** services (kyc-service, analytics-service)

## Templates

### 1. Quarkus Test Configuration (`quarkus-test-config.yml.template`)

**Purpose**: Standard test configuration for Quarkus microservices using H2 in-memory database.

**When to Use**: Any new or existing Quarkus service that needs unit/integration testing with database access.

**How to Apply**:

Option A - Copy to test resources (recommended):
```bash
cp quarkus-test-config.yml.template <service>/src/test/resources/application.yml
```

Option B - Add as test profile in main config:
```yaml
# In src/main/resources/application.yml
%test:
  quarkus:
    datasource:
      # ... paste template content here
```

**Key Configuration Options**:

| Setting | Value | Purpose |
|---------|-------|---------|
| `db-kind` | `h2` | Use H2 in-memory database for fast tests |
| `url` | `jdbc:h2:mem:testdb;MODE=PostgreSQL` | PostgreSQL compatibility mode prevents dialect differences |
| `generation` | `drop-and-create` | Clean schema for each test suite |
| `flyway.migrate-at-start` | `false` | Disable migrations in tests (use Hibernate DDL instead) |
| `kafka.devservices.enabled` | `false` | Disable Kafka DevServices for unit tests |
| `opentelemetry.enabled` | `false` | Disable distributed tracing during tests |

**Common Pitfalls to Avoid**:

1. **Using PostgreSQL in Tests**: Don't use Testcontainers PostgreSQL for unit tests. It's slow and creates environment divergence. Use H2 with PostgreSQL mode instead.
   - **Exception**: Use Testcontainers ONLY for integration tests that verify PostgreSQL-specific features (JSONB, stored procedures).

2. **Migrating with Flyway in Tests**: Don't enable `flyway.migrate-at-start: true` for unit tests. Hibernate schema generation is faster and sufficient.
   - **Exception**: Enable ONLY when testing actual migration scripts.

3. **Leaky Test Isolation**: Ensure `DB_CLOSE_DELAY=-1` is set to prevent database from closing between tests in the same suite.

4. **Missing SmallRye In-Memory Connector**: For Kafka integration tests, use `smallrye-in-memory` connector instead of real Kafka broker for faster execution.

**Example Usage in Tests**:

```java
@QuarkusTest
public class AccountServiceTest {

    @Inject
    EntityManager entityManager;

    @Test
    public void testCreateAccount() {
        // H2 database is automatically available
        Account account = new Account();
        account.setAccountNumber("1234567890");
        entityManager.persist(account);
        // Assertions...
    }
}
```

### 2. FastAPI Test Configuration (`fastapi-test-conftest.py.template`)

**Purpose**: Standard pytest configuration for FastAPI microservices using SQLAlchemy async and SQLite in-memory database.

**When to Use**: Any new or existing FastAPI service that needs async testing with database access.

**How to Apply**:

```bash
cp fastapi-test-conftest.py.template <service>/tests/conftest.py
```

Then ensure `pyproject.toml` has the following:

```toml
[tool.pytest.ini_options]
asyncio_mode = "auto"
testpaths = ["tests"]
```

And install required dependencies:

```bash
pip install pytest-asyncio aiosqlite sqlalchemy
```

**Key Configuration Options**:

| Fixture | Scope | Purpose |
|---------|-------|---------|
| `test_db_engine` | `session` | Single in-memory SQLite database for entire test session |
| `sqlite+aiosqlite` | In-memory | Fast, fileless database for testing |
| `StaticPool` | - | Prevents connection pool exhaustion in tests |
| `check_same_thread: False` | - | Required for SQLite async operations |

**Common Pitfalls to Avoid**:

1. **Creating event_loop Fixtures**: Do NOT create custom `event_loop` fixtures. `pytest-asyncio` handles this automatically with `asyncio_mode = "auto"`.
   - **Why**: Custom event loop fixtures cause "loop is closed" errors in async tests.

2. **Using PostgreSQL in Unit Tests**: Don't use Testcontainers PostgreSQL for unit tests. Use SQLite in-memory instead.
   - **Exception**: Use Testcontainers ONLY for integration tests verifying PostgreSQL-specific features.

3. **Missing StaticPool**: Without `StaticPool`, SQLite tests may fail with "SQLite objects created in a thread can only be used in that same thread".

4. **Database Recreation Overhead**: Don't use function-scoped database fixtures. Session-scoped fixtures are faster and sufficient for test isolation when using transactions.

**Example Usage in Tests**:

```python
import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

@pytest.mark.asyncio
async def test_create_kyc_verification(async_client: AsyncClient, test_db_engine):
    response = await async_client.post(
        "/api/v1/kyc/verify",
        json={"nik": "1234567890123456"}
    )
    assert response.status_code == 201
```

## Platform Standards Reference

All test configurations in PayU must adhere to the following platform standards:

### 1. Test Speed Requirements
- **Unit Tests**: Must complete in under 2 seconds per service
- **Integration Tests**: Must complete in under 30 seconds per service
- **No External Dependencies**: Tests must not require running services (PostgreSQL, Kafka, Redis)

### 2. Database Compatibility
- **Quarkus Services**: Use H2 in PostgreSQL compatibility mode (`MODE=PostgreSQL`)
- **FastAPI Services**: Use SQLite in-memory with aiosqlite driver
- **Rationale**: Prevents production-only bugs from reaching production

### 3. Test Isolation
- Each test must be independent and runnable in isolation
- No shared state between tests (use transactions and rollbacks)
- No reliance on test execution order

### 4. Continuous Integration Compatibility
- Tests must pass in CI/CD pipelines (OpenShift Pipelines, GitHub Actions)
- No hardcoded localhost URLs or ports
- Environment variables for configuration only

## Migration from Existing Tests

If you have existing tests that don't follow these templates:

1. **Review current configuration**: Check `src/test/resources/application.yml` (Quarkus) or `tests/conftest.py` (FastAPI)
2. **Identify deviations**: Look for PostgreSQL Testcontainers usage in unit tests, Flyway migrations enabled, or missing in-memory database config
3. **Apply template**: Replace with template content
4. **Update tests**: Modify tests that depend on PostgreSQL-specific features
5. **Verify**: Run test suite to ensure no regressions

## Additional Resources

- **Quarkus Testing Guide**: https://quarkus.io/guides/getting-started-testing
- **pytest-asyncio Documentation**: https://pytest-asyncio.readthedocs.io/
- **PayU Development Guidelines**: See `/home/ubuntu/payu/CLAUDE.md`
- **Platform Architecture**: See `/home/ubuntu/payu/docs/architecture/ARCHITECTURE.md`

## Support

For questions or issues with test configuration:

1. Check existing service implementations for examples
2. Review test failures for configuration-related errors
3. Consult PayU development guidelines in project root
4. Create issue in project repository for platform-wide concerns

---

**Last Updated**: January 2026
**Maintained By**: PayU Platform Team
