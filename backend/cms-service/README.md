# PayU CMS Service

Content Management Service for managing banners, promotions, alerts, and popups.

## Features

- **Content Management**: CRUD operations for all content types
- **Scheduling**: Automatic activation and archival based on dates
- **Targeting**: User segmentation by segment, location, and device
- **Caching**: Redis-based caching for performance
- **Event Publishing**: Kafka events for content changes
- **Security**: OAuth2/Keycloak JWT authentication
- **API Documentation**: OpenAPI/Swagger UI

## Content Types

| Type | Description |
|------|-------------|
| BANNER | Promotional banners on home screen |
| PROMO | In-app promotions and offers |
| ALERT | System alerts and notifications |
| POPUP | Modal popups for announcements |

## Content Status

| Status | Description |
|--------|-------------|
| DRAFT | Not yet active |
| SCHEDULED | Scheduled for future activation |
| ACTIVE | Currently visible |
| PAUSED | Temporarily disabled |
| ARCHIVED | No longer in use |

## Architecture

```
cms-service/
├── domain/
│   ├── entity/        # Domain entities
│   ├── dto/           # Request/Response DTOs
│   └── repository/    # Repository interfaces
├── application/
│   └── service/       # Business logic (ports)
├── adapter/
│   ├── web/rest/      # REST controllers (driving adapters)
│   ├── persistence/   # Persistence adapters
│   └── messaging/     # Kafka event publishers
└── config/            # Spring configuration
```

## API Endpoints

### Admin APIs (Requires `ROLE_CMS_ADMIN`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/contents` | Create new content |
| PUT | `/api/v1/contents/{id}` | Update content |
| GET | `/api/v1/contents/{id}` | Get content by ID |
| GET | `/api/v1/contents` | List all content (paginated) |
| GET | `/api/v1/contents/type/{type}` | Get content by type |
| GET | `/api/v1/contents/status/{status}` | Get content by status |
| PATCH | `/api/v1/contents/{id}/status` | Update content status |
| DELETE | `/api/v1/contents/{id}` | Delete content |

### Public APIs (No Authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/public/contents/type/{type}` | Get active content by type |

## Scheduled Tasks

| Task | Schedule | Description |
|------|----------|-------------|
| Activate Scheduled Content | Every hour at :00 | Activates content with `start_date` reached |
| Archive Expired Content | Every hour at :30 | Archives content past `end_date` |

## Local Development

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 16
- Redis 7+
- Kafka 3+

### Running Locally

```bash
# Start infrastructure services
docker-compose up -d postgres redis kafka

# Run the service
cd backend/cms-service
mvn spring-boot:run

# Access Swagger UI
open http://localhost:8082/swagger-ui.html
```

### Database Setup

```bash
# Run Flyway migrations
mvn flyway:migrate

# Or let Spring Boot auto-migrate on startup
```

### Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/payu_cms
spring.datasource.username=payu
spring.datasource.password=payu_password

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# Keycloak
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/payu
```

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=ContentServiceTest
```

## Build & Package

```bash
# Build JAR
mvn clean package

# Build container image
mvn spring-boot:build-image

# Run with Docker
docker run -p 8082:8082 cms-service:1.0.0
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/payu_cms` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `payu` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | - |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | Keycloak issuer | - |

## OpenShift Deployment

```bash
# Build from source
oc new-build --binary --name=cms-service -l app=cms-service

# Deploy
oc new-app cms-service:1.0.0 -l app=cms-service

# Expose service
oc expose svc/cms-service
```

## Monitoring

- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`

## Kafka Events

### Topics

| Topic | Event Type |
|-------|------------|
| `cms-content-published` | Content published/activated |
| `cms-content-updated` | Content updated |
| `cms-content-archived` | Content archived |

### Event Schema

```json
{
  "eventId": "uuid",
  "eventType": "CONTENT_PUBLISHED",
  "contentId": "uuid",
  "contentType": "BANNER",
  "title": "Promo Title",
  "status": "ACTIVE",
  "startDate": "2026-01-25",
  "endDate": "2026-01-31",
  "priority": 100,
  "targetingRules": {},
  "publishedAt": "2026-01-24T10:00:00",
  "publishedBy": "admin@payu.id"
}
```

## License

Copyright (c) 2026 PayU. All rights reserved.
