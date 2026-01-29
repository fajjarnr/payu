# Testing Guide for Analytics Service

This document explains how to run the test suite for the analytics-service, including infrastructure-dependent tests.

## Test Categories

### Unit Tests
- **Location**: `tests/unit/`
- **Purpose**: Test individual functions and classes in isolation
- **Dependencies**: Mocked (no external services required)
- **Run Command**: `pytest -v tests/unit/` or `pytest -m "unit"`

### Integration Tests
- **Location**: `tests/integration/`
- **Purpose**: Test interaction between components (e.g., Kafka consumer + business logic)
- **Dependencies**: May require mocked infrastructure
- **Run Command**: `pytest -v tests/integration/` or `pytest -m "integration"`

### End-to-End (E2E) Tests
- **Location**: `tests/e2e/`
- **Purpose**: Test complete workflows through the API
- **Dependencies**: May require WebSocket infrastructure
- **Run Command**: `pytest -v tests/e2e/` or `pytest -m "e2e"`

## Infrastructure Tests

Some tests are marked with `@pytest.mark.infrastructure` and require external services:

- **Kafka**: Message streaming (aiokafka)
- **PostgreSQL**: Database connections (via Testcontainers)
- **WebSocket**: Real-time communication (via FastAPI TestClient)

### Running Tests WITHOUT Infrastructure (Default)

By default, infrastructure tests are **skipped**:

```bash
# Run all non-infrastructure tests
pytest -v

# Explicitly exclude infrastructure tests
pytest -v -m "not infrastructure"
```

This will run 140+ tests and achieve ~86% code coverage without requiring Docker.

### Running Tests WITH Infrastructure

To run infrastructure tests, you must:

1. **Start required services** using Docker Compose:
   ```bash
   cd /home/ubuntu/payu
   docker-compose up -d kafka postgres
   ```

2. **Enable infrastructure tests**:
   ```bash
   export ENABLE_INFRASTRUCTURE_TESTS=true
   pytest -v
   ```

   Or:
   ```bash
   ENABLE_INFRASTRUCTURE_TESTS=true pytest -v
   ```

3. **Run only infrastructure tests**:
   ```bash
   ENABLE_INFRASTRUCTURE_TESTS=true pytest -v -m "infrastructure"
   ```

## Docker Infrastructure Requirements

### Kafka (aiokafka)
- **Port**: 9092
- **Purpose**: Integration tests for Kafka consumer message handling
- **Tests**:
  - `tests/integration/test_kafka_consumer.py`
  - Message handling for transactions, wallet events, KYC verification

### WebSocket (FastAPI TestClient)
- **Port**: Not required (uses in-memory TestClient)
- **Purpose**: Real-time dashboard updates
- **Tests**:
  - `tests/e2e/test_analytics_websocket.py`
  - Connection management, ping/pong, broadcast

### PostgreSQL (via Testcontainers)
- **Port**: 5432 (auto-managed by Testcontainers)
- **Purpose**: Database integration tests
- **Note**: Most tests use mocked SQLAlchemy sessions

## Test Coverage

Current coverage targets (configured in `pyproject.toml`):
- **Minimum Coverage**: 80%
- **Current Coverage**: ~86%

### Coverage by Module
```
src/app/api/v1/analytics.py                 88%
src/app/api/v1/websocket.py                 69%
src/app/database.py                         95%
src/app/main.py                             97%
src/app/ml/fraud_detection.py               96%
src/app/ml/recommendation_engine.py         96%
src/app/ml/robo_advisory.py                100%
src/app/models/schemas.py                  100%
src/app/services/analytics_service.py       75%
src/app/websocket/connection_manager.py     86%
```

## CI/CD Integration

In CI/CD pipelines (GitHub Actions, GitLab CI, Jenkins):

```yaml
# Example CI configuration
- name: Run unit tests (no infrastructure)
  run: pytest -v -m "not infrastructure"

- name: Start infrastructure
  run: docker-compose up -d kafka postgres

- name: Run all tests including infrastructure
  run: |
    export ENABLE_INFRASTRUCTURE_TESTS=true
    pytest -v
```

## Common Issues

### Issue: KafkaConnectionError
```
aiokafka.errors.KafkaConnectionError: Unable to bootstrap from [('localhost', 9092)]
```

**Solution**: Start Kafka with Docker Compose or skip infrastructure tests.

### Issue: Tests Skipped Unexpectedly
```
SKIPPED [1] tests/integration/test_kafka_consumer.py:28: Infrastructure tests skipped...
```

**Solution**: Set `ENABLE_INFRASTRUCTURE_TESTS=true` if you want to run these tests.

### Issue: WebSocket Test Failures
```
AssertionError: assert 'connection_established' == 'pong'
```

**Solution**: Fixed in latest test updates. Tests now consume the `connection_established` message first.

## Test Fixtures

Shared fixtures are available in `tests/conftest.py`:

- `mock_db_session`: Mock SQLAlchemy async session
- `mock_query_result`: Factory for creating mock query results
- `mock_scalar_result`: Mock for scalar() queries
- `mock_scalars_result`: Mock for scalars() queries
- `sample_user_id`: Sample user ID for testing
- `sample_user_metrics`: Sample UserMetricsEntity
- `sample_transaction_analytics`: Sample transaction entities

## Additional Resources

- [Pytest Documentation](https://docs.pytest.org/)
- [pytest-asyncio Documentation](https://pytest-asyncio.readthedocs.io/)
- [FastAPI Testing Docs](https://fastapi.tiangolo.com/tutorial/testing/)
- [aiokafka Documentation](https://aiokafka.readthedocs.io/)
