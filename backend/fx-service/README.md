# FX Service

**Foreign Exchange & Multi-Currency Service** untuk PayU Digital Banking Platform.

## Overview

FX Service menangani:
- 💱 Foreign exchange rates & conversions
- 💵 Multi-currency wallet support
- 📊 Real-time rate feeds
- 🔄 Currency conversion history

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| Database | PostgreSQL 16 |
| Cache | Redis |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/fx/rates` | Get current exchange rates |
| GET | `/api/v1/fx/rates/{currency}` | Get rate for specific currency |
| POST | `/api/v1/fx/convert` | Convert amount between currencies |
| GET | `/api/v1/fx/history` | Get conversion history |

## Configuration

```yaml
payu:
  fx:
    base-currency: IDR
    rate-provider: BI  # Bank Indonesia
    cache-ttl: 300     # 5 minutes
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

- **Service Port**: 8015
- **Actuator**: 8015/actuator/health
