# PayU A/B Testing Service

A comprehensive A/B testing framework for UI features and promotional offers, built with Spring Boot 3.4 and Java 21.

## Overview

The A/B Testing Service provides:
- **Experiment Management** - Create, update, and manage A/B testing experiments
- **Variant Assignment** - Consistent hashing-based user-to-variant assignment
- **Conversion Tracking** - Track conversions and participation metrics
- **Real-time Metrics** - Monitor experiment performance with statistical analysis
- **Event Streaming** - Kafka integration for real-time event publishing
- **Caching** - Redis-backed variant assignment caching

## Technology Stack

- **Runtime**: Java 21
- **Framework**: Spring Boot 3.4.1
- **Database**: PostgreSQL with JSONB support
- **Cache**: Redis (Data Grid RESP compatible)
- **Events**: Kafka (AMQ Streams)
- **Security**: OAuth2/Keycloak
- **Migration**: Flyway
- **Documentation**: OpenAPI 3.0 (Swagger)

## Architecture

The service follows **Hexagonal/Clean Architecture** principles:

```
src/main/java/id/payu/abtesting/
├── application/          # Configuration & Security
│   ├── config/          # Beans and configuration
│   └── security/        # Security configuration
├── domain/              # Core business logic
│   ├── entity/         # JPA entities
│   ├── repository/     # Data access layer
│   └── service/        # Domain services
├── infrastructure/      # External integrations
│   ├── kafka/          # Event producers
│   └── redis/          # Cache implementation
└── interfaces/         # External interfaces
    ├── dto/            # Request/Response DTOs
    └── rest/           # REST controllers
```

## API Endpoints

### Experiment Management

| Method | Endpoint | Description | Authority |
|--------|----------|-------------|-----------|
| GET | `/api/v1/experiments` | List all experiments (paginated) | `ab-testing:experiments:read` |
| GET | `/api/v1/experiments/{id}` | Get experiment by ID | `ab-testing:experiments:read` |
| GET | `/api/v1/experiments/key/{key}` | Get experiment by key | `ab-testing:experiments:read` |
| GET | `/api/v1/experiments/active` | Get active experiments | `ab-testing:experiments:read` |
| POST | `/api/v1/experiments` | Create new experiment | `ab-testing:experiments:write` |
| PUT | `/api/v1/experiments/{id}` | Update experiment | `ab-testing:experiments:write` |
| DELETE | `/api/v1/experiments/{id}` | Delete experiment | `ab-testing:experiments:delete` |
| PATCH | `/api/v1/experiments/{id}/status` | Change experiment status | `ab-testing:experiments:write` |

### Variant Assignment & Tracking

| Method | Endpoint | Description | Authority |
|--------|----------|-------------|-----------|
| POST | `/api/v1/experiments/{key}/assign` | Get variant for user | `ab-testing:experiments:assign` |
| POST | `/api/v1/experiments/{id}/track` | Track conversion event | `ab-testing:experiments:track` |

## Experiment Entity

```java
{
  "id": "uuid",
  "name": "Homepage Hero Banner Test",
  "key": "homepage_hero_banner",
  "status": "RUNNING",  // DRAFT, RUNNING, PAUSED, COMPLETED, CANCELLED
  "startDate": "2026-01-24",
  "endDate": "2026-02-24",
  "trafficSplit": 50,  // 0-100 percentage for variant B
  "variantAConfig": {
    "backgroundColor": "#ffffff",
    "textColor": "#000000",
    "ctaText": "Learn More"
  },
  "variantBConfig": {
    "backgroundColor": "#10b981",
    "textColor": "#ffffff",
    "ctaText": "Get Started"
  },
  "targetingRules": {
    "minAge": 18,
    "countries": ["ID"]
  },
  "metrics": {
    "CONTROL": {
      "participants": 150,
      "conversions": 30
    },
    "VARIANT_B": {
      "participants": 145,
      "conversions": 42
    }
  },
  "confidenceLevel": 0.95,
  "winner": "VARIANT_B"  // CONTROL, VARIANT_B, INCONCLUSIVE
}
```

## Usage Examples

### 1. Create an Experiment

```bash
curl -X POST http://localhost:8080/api/v1/experiments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Checkout Button Color Test",
    "key": "checkout_button_color",
    "trafficSplit": 50,
    "startDate": "2026-01-25",
    "endDate": "2026-02-25",
    "variantAConfig": {
      "color": "#10b981",
      "text": "Pay Now"
    },
    "variantBConfig": {
      "color": "#3b82f6",
      "text": "Pay Now"
    }
  }'
```

### 2. Assign Variant to User

```bash
curl -X POST http://localhost:8080/api/v1/experiments/checkout_button_color/assign \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-uuid"
  }'
```

Response:
```json
{
  "experimentKey": "checkout_button_color",
  "variant": "VARIANT_B",
  "config": {
    "color": "#3b82f6",
    "text": "Pay Now"
  }
}
```

### 3. Track Conversion

```bash
curl -X POST http://localhost:8080/api/v1/experiments/{experimentId}/track \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-uuid",
    "variant": "VARIANT_B",
    "eventType": "conversion"
  }'
```

## Local Development

### Prerequisites

- Java 21
- Maven 3.9+
- Docker (for PostgreSQL, Redis, Kafka)

### Start Infrastructure

```bash
docker-compose up -d
```

### Run Application

```bash
mvn spring-boot:run
```

### Run Tests

```bash
mvn test
```

### Build Package

```bash
mvn clean package -DskipTests
```

## Configuration

Key configuration in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/payu_ab_testing
spring.datasource.username=payu
spring.datasource.password=payu_password

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# Keycloak
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/realms/payu/protocol/openid-connect/certs
```

## Key Features

### Consistent Hashing

Users are consistently assigned to variants based on their user ID hash:

```java
int hash = userId.hashCode();
int bucket = Math.abs(hash % 100);
String variant = bucket < trafficSplit ? "VARIANT_B" : "CONTROL";
```

This ensures the same user always gets the same variant.

### Redis Caching

Variant assignments are cached in Redis for 24 hours to reduce database load and ensure consistency.

### Kafka Events

All experiment changes and conversions are published to Kafka topics:
- `ab-testing.experiments` - Experiment lifecycle events
- `ab-testing.assignments` - Variant assignment events
- `ab-testing.conversions` - Conversion tracking events

### Conversion Rate Calculation

```java
double conversionRate = conversions / participants;
```

## Metrics & Monitoring

### Actuator Endpoints

- `/actuator/health` - Health check
- `/actuator/metrics` - Prometheus metrics
- `/actuator/info` - Application info

### Prometheus Metrics

The service exposes:
- `experiment_assignments_total` - Total variant assignments
- `experiment_conversions_total` - Total conversions tracked
- `experiment_cache_hits` - Redis cache hits

## API Documentation

Swagger UI is available at:
- `http://localhost:8080/swagger-ui.html`

OpenAPI spec:
- `http://localhost:8080/api-docs`

## Security

### OAuth2/Keycloak

The service uses Keycloak for authentication and authorization. Required authorities:

| Authority | Description |
|-----------|-------------|
| `ab-testing:experiments:read` | Read experiments |
| `ab-testing:experiments:write` | Create/update experiments |
| `ab-testing:experiments:delete` | Delete experiments |
| `ab-testing:experiments:assign` | Assign variants |
| `ab-testing:experiments:track` | Track conversions |

## Database Schema

See `V1__init.sql` for the complete schema:

```sql
CREATE TABLE ab_experiments (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    key VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date DATE,
    end_date DATE,
    traffic_split INTEGER NOT NULL,
    variant_a_config JSONB,
    variant_b_config JSONB,
    targeting_rules JSONB,
    metrics JSONB,
    confidence_level DOUBLE PRECISION,
    winner VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(100)
);
```

## Testing

### Unit Tests

```bash
mvn test -Dtest=ExperimentServiceTest
```

### Architecture Tests

```bash
mvn test -Dtest=ArchitectureTest
```

## Deployment

### Build Docker Image

```bash
mvn clean package -DskipTests
docker build -t payu/ab-testing-service:latest .
```

### Deploy to OpenShift

```bash
oc new-app payu/ab-testing-service:latest
oc expose svc/ab-testing-service
```

## Troubleshooting

### Redis Connection Issues

Check Redis is running:
```bash
docker ps | grep redis
redis-cli ping
```

### Kafka Connection Issues

Check Kafka is running:
```bash
docker ps | grep kafka
kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Database Migration Issues

Check Flyway status:
```bash
mvn flyway:info
```

## Support

- **Backend Team**: backend-team@payu.id
- **Architecture**: architect@payu.id
- **Issues**: https://github.com/payu/ab-testing-service/issues

## License

Apache License 2.0
