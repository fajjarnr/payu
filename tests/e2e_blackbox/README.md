# PayU Cross-Service Integration Tests

This directory contains holistic End-to-End (E2E) test suites covering full user journeys across all PayU services.

## Test Suites

### 1. Complete User Journey (`test_complete_user_journey.py`)
Tests the complete onboarding and basic transaction flows:
- User registration and authentication
- Wallet creation (event-driven)
- Balance topup
- P2P transfers
- Bill payments
- QRIS payments

### 2. Investment Flow (`test_investment_flow.py`)
Tests wealth management features:
- Investment account creation
- Digital deposits
- Mutual funds
- Digital gold
- Holdings retrieval

### 3. Lending Flow (`test_lending_flow.py`)
Tests credit and lending services:
- Credit score calculation
- Personal loan application
- Repayment schedules
- PayLater activation
- PayLater transactions

### 4. Promotion Flow (`test_promotion_flow.py`)
Tests rewards and gamification:
- Promotion creation and management
- Promotion claiming
- Cashback rewards
- Loyalty points
- Referral system

### 5. Compliance Flow (`test_compliance_flow.py`)
Tests regulatory compliance:
- AML audit reports
- CFT audit reports
- Compliance checks
- Report searching and filtering

### 6. Support Flow (`test_support_flow.py`)
Tests support team management:
- Support agent management
- Training module creation
- Training assignment
- Training status tracking

### 7. Partner Flow (`test_partner_flow.py`)
Tests partner and SNAP BI integration:
- Partner management
- API key generation and regeneration
- SNAP BI authentication
- SNAP BI payments

### 8. Analytics Flow (`test_analytics_flow.py`)
Tests analytics and ML features:
- User metrics
- Spending trends
- Cash flow analysis
- Personalized recommendations

### 9. Backoffice Flow (`test_backoffice.py`)
Tests operational flows:
- KYC review workflow
- Fraud case management
- Customer support cases

## Prerequisites

1. All PayU services must be running and accessible via the API Gateway
2. Gateway URL: `http://localhost:8080` (configurable in tests)
3. Required services:
   - account-service
   - auth-service
   - wallet-service
   - transaction-service
   - billing-service
   - investment-service
   - lending-service
   - promotion-service
   - compliance-service
   - support-service
   - partner-service
   - analytics-service
   - backoffice-service

## Installation

```bash
cd tests/e2e_blackbox
pip install -r requirements.txt
```

## Running Tests

### Run all tests:
```bash
pytest -v
```

### Run specific test suite:
```bash
pytest test_complete_user_journey.py -v
```

### Run specific test:
```bash
pytest test_complete_user_journey.py::TestFullUserJourney::test_complete_user_onboarding_journey -v
```

### Run with markers:
```bash
pytest -m "smoke"
pytest -m "critical"
```

### Run with detailed output:
```bash
pytest -v -s
```

### Stop on first failure:
```bash
pytest -x
```

## Test Configuration

Environment variables (optional):
- `GATEWAY_URL`: API Gateway URL (default: `http://localhost:8080`)
- `TEST_TIMEOUT`: Request timeout in seconds (default: `30`)

## Test Strategy

The E2E tests follow a holistic approach:
1. **User Journey Coverage**: Tests cover complete user workflows from start to finish
2. **Cross-Service Integration**: Tests verify communication between multiple services
3. **Event-Driven Validation**: Tests include retries for event-driven operations
4. **Graceful Degradation**: Tests use `pytest.skip()` when services are unavailable
5. **Realistic Data**: Uses Faker library to generate realistic test data

## Architecture

The tests use a modular approach:
- `client.py`: HTTP client for interacting with the API Gateway
- Fixtures: Shared test data and authentication
- Test classes: Logical grouping of related tests
- Session fixtures: Reuse of authentication and user data across tests

## Writing New Tests

When adding new E2E tests:
1. Create a new test file following the naming convention: `test_<flow>_flow.py`
2. Import the `PayUClient` from `client.py`
3. Use pytest fixtures for setup and teardown
4. Follow the existing pattern:
   - Register/login users
   - Set authentication tokens
   - Call API endpoints
   - Assert on responses
   - Use `pytest.skip()` for optional features

## Troubleshooting

### Services Unavailable
If tests are skipped due to services being unavailable:
1. Verify all services are running: `kubectl get pods` or `docker ps`
2. Check Gateway logs: `kubectl logs deployment/gateway-service`
3. Verify service URLs in `GatewayConfig`

### Authentication Failures
If authentication tests fail:
1. Check Keycloak/KeycloakService is running
2. Verify user registration is working
3. Check JWT token generation

### Event-Driven Operations
If event-driven operations (wallet creation) fail:
1. Check Kafka is running: `kubectl get pods | grep kafka`
2. Verify topic creation: `kubectl exec -it <kafka-pod> -- kafka-topics.sh --list`
3. Check consumer group lag: `kubectl exec -it <kafka-pod> -- kafka-consumer-groups.sh --describe`

## CI/CD Integration

These E2E tests can be integrated into CI/CD pipelines:
1. Run after all services are deployed
2. Run in a dedicated test environment
3. Use test databases (not production)
4. Run nightly or before releases
5. Report results to the team

## Best Practices

1. **Isolation**: Each test should be independent
2. **Cleanup**: Delete created resources when possible
3. **Time-sensitive**: Use appropriate timeouts and retries
4. **Logging**: Add descriptive log messages for debugging
5. **Documentation**: Document any prerequisites or special setup

## Maintenance

Regularly update tests when:
- New features are added
- API endpoints change
- Services are added or removed
- Business rules change
- Authentication mechanisms change

## Contact

For questions or issues with the E2E tests, contact the QA team or open an issue in the repository.
