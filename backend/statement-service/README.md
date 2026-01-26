# Statement Service

**E-Statement PDF Generation Service** untuk PayU Digital Banking Platform.

## Overview

Statement Service menangani:
- 📄 Monthly account statement generation
- 📧 E-statement delivery via email
- 🗓️ Statement scheduling & automation
- 📁 Statement archive & retrieval

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| PDF Library | Apache PDFBox 3.0.3 |
| Database | PostgreSQL 16 |
| Storage | S3-compatible (MinIO) |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/statements/generate` | Generate statement for account |
| GET | `/api/v1/statements/{id}` | Download statement PDF |
| GET | `/api/v1/statements` | List statements for account |
| POST | `/api/v1/statements/schedule` | Schedule monthly statements |

## Configuration

```yaml
payu:
  statement:
    storage-bucket: payu-statements
    retention-days: 365
    template: default
```

## Local Development

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Test
mvn test
```

## Port

- **Service Port**: 8016
- **Actuator**: 8016/actuator/health
