# Promotion Service (Quarkus - Layered Architecture)

Test Resources (Testcontainers):
- PostgresTestResource: PostgreSQL 16 container for integration tests
- KafkaTestResource: Apache Kafka container for event testing

## Quick Test Command

```bash
# Run unit tests with PostgreSQL + Kafka containers
mvn test -Dtest=PromotionServiceTest

# Run integration tests
mvn verify -DskipITs=false

# Run with Testcontainers
mvn test -Dquarkus.test.profile=test
```
