## Contract Testing with Spring Cloud Contract

This directory contains contract tests for critical service pairs in the PayU platform.

### Strategy

We use **Spring Cloud Contract** for provider-driven contract testing between:
- `transaction-service` ↔ `wallet-service` (balance operations)
- `auth-service` ↔ `account-service` (user registration)
- `billing-service` ↔ `transaction-service` (payment processing)
- `lending-service` ↔ `wallet-service` (loan disbursement)

### How it works

1. **Provider** defines contracts in Groovy DSL (`src/test/resources/contracts/`)
2. **Maven plugin** generates tests from contracts → runs against provider
3. **Stubs JAR** is published to Maven local/repo
4. **Consumer** uses `@AutoConfigureStubRunner` to test against stubs

### Running

```bash
# Generate & run provider tests
mvn test -pl transaction-service -Dspring.cloud.contract.verifier.enabled=true

# Consumer side (auto-downloads stubs)
mvn test -pl billing-service -Dspring.cloud.contract.stubrunner.enabled=true
```
