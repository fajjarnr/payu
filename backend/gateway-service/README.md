# PayU API Gateway

> API Gateway service for PayU Digital Banking Platform built with Quarkus.

## Overview

The Gateway Service serves as the single entry point for all client applications (Web, Mobile, Admin). It provides:

- **Request Routing**: Routes requests to appropriate backend services
- **Authentication**: JWT/OIDC token validation (Red Hat SSO/Keycloak)
- **Rate Limiting**: Distributed rate limiting using Redis
- **Circuit Breaker**: Fault tolerance with automatic fallback
- **Correlation ID**: Distributed tracing support
- **Metrics**: Prometheus-compatible metrics

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      GATEWAY SERVICE (Port 8080)                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Correlation │  │  Rate Limit  │  │    OIDC      │          │
│  │   ID Filter  │  │    Filter    │  │  Validation  │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                 │                   │
│         └─────────────────┴─────────────────┘                   │
│                           │                                     │
│                    ┌──────▼──────┐                              │
│                    │   Router    │                              │
│                    └──────┬──────┘                              │
│                           │                                     │
│    ┌──────────────────────┼──────────────────────┐             │
│    │                      │                      │             │
│    ▼                      ▼                      ▼             │
│ Simulators          Core Services         Supporting           │
│ ├─ BI-FAST          ├─ Account            ├─ Billing           │
│ ├─ Dukcapil         ├─ Auth               ├─ Notification      │
│ └─ QRIS             ├─ Transaction        ├─ KYC               │
│                     └─ Wallet             └─ Analytics         │
└─────────────────────────────────────────────────────────────────┘
```

## API Routes

### Simulator APIs (Development/Testing)

| Gateway Path | Target Service | Port |
|--------------|----------------|------|
| `/api/v1/simulator/bifast/*` | bi-fast-simulator | 8090 |
| `/api/v1/simulator/dukcapil/*` | dukcapil-simulator | 8091 |
| `/api/v1/simulator/qris/*` | qris-simulator | 8092 |

### Core Service APIs (Production)

| Gateway Path | Target Service | Port |
|--------------|----------------|------|
| `/api/v1/accounts/*` | account-service | 8081 |
| `/api/v1/auth/*` | auth-service | 8082 |
| `/api/v1/transactions/*` | transaction-service | 8083 |
| `/api/v1/wallet/*` | wallet-service | 8084 |
| `/api/v1/billing/*` | billing-service | 8085 |
| `/api/v1/notifications/*` | notification-service | 8086 |
| `/api/v1/kyc/*` | kyc-service | 8087 |
| `/api/v1/analytics/*` | analytics-service | 8088 |

## Features

### Rate Limiting

| Endpoint Category | Requests/Min | Burst |
|-------------------|--------------|-------|
| Authentication | 5 | 10 |
| OTP | 3 | 5 |
| Transfer | 10 | 20 |
| Balance | 30 | 50 |
| Default | 60 | 100 |

### Circuit Breaker

- **Failure Ratio**: 50%
- **Delay**: 30 seconds
- **Success Threshold**: 3 successful requests to close

### Fault Tolerance

| Operation | Timeout | Retries |
|-----------|---------|---------|
| BI-FAST Transfer | 30s | 1 |
| Dukcapil Match Photo | 60s | 1 |
| QRIS Generate | 15s | 2 |
| Status Check | 10-15s | 3 |

## Running Locally

### Prerequisites

- Java 21+
- Maven 3.9+
- Redis 7+ (for rate limiting)
- Simulators running (optional)

### Start Redis

```bash
docker run -d --name redis \
  -p 6379:6379 \
  redis:7-alpine
```

### Run in Development Mode

```bash
./mvnw quarkus:dev
```

### Run Tests

```bash
./mvnw test
```

## Configuration

Key environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_URL` | `redis://localhost:6379` | Redis connection URL |
| `OIDC_ENABLED` | `false` | Enable OIDC authentication |
| `OIDC_AUTH_SERVER_URL` | - | Keycloak realm URL |
| `BIFAST_SIMULATOR_URL` | `http://localhost:8090` | BI-FAST simulator |
| `DUKCAPIL_SIMULATOR_URL` | `http://localhost:8091` | Dukcapil simulator |
| `QRIS_SIMULATOR_URL` | `http://localhost:8092` | QRIS simulator |

## API Examples

### Route to Simulator

```bash
# BI-FAST Inquiry via Gateway
curl -X POST http://localhost:8080/api/v1/simulator/bifast/inquiry \
  -H "Content-Type: application/json" \
  -d '{
    "bankCode": "BCA",
    "accountNumber": "1234567890"
  }'

# QRIS Generate via Gateway
curl -X POST http://localhost:8080/api/v1/simulator/qris/generate \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "MCH001",
    "amount": 50000
  }'
```

### Health Check

```bash
curl http://localhost:8080/health
```

Response:
```json
{
  "status": "UP",
  "service": "gateway-service",
  "version": "1.0.0",
  "timestamp": "2026-01-18T23:30:00Z"
}
```

### Status

```bash
curl http://localhost:8080/status
```

Response:
```json
{
  "status": "UP",
  "service": "gateway-service",
  "uptime": "1h 23m 45s",
  "memory": {
    "total": 536870912,
    "used": 134217728,
    "free": 402653184
  },
  "processors": 4
}
```

## Headers

### Request Headers

| Header | Description |
|--------|-------------|
| `X-Correlation-Id` | Distributed tracing ID (auto-generated if not provided) |
| `Authorization` | Bearer JWT token (when OIDC enabled) |

### Response Headers

| Header | Description |
|--------|-------------|
| `X-Correlation-Id` | Correlation ID for tracing |
| `X-Request-Id` | Unique request ID |

## Health & Metrics

- Health: `http://localhost:8080/q/health`
- Liveness: `http://localhost:8080/q/health/live`
- Readiness: `http://localhost:8080/q/health/ready`
- Metrics: `http://localhost:8080/q/metrics`
- OpenAPI: `http://localhost:8080/q/openapi`
- Swagger UI: `http://localhost:8080/q/swagger-ui`

## Docker

```bash
# Build
docker build -t payu/gateway-service:1.0.0 .

# Run
docker run -d --name gateway-service \
  -e REDIS_URL=redis://host.docker.internal:6379 \
  -e BIFAST_SIMULATOR_URL=http://host.docker.internal:8090 \
  -e DUKCAPIL_SIMULATOR_URL=http://host.docker.internal:8091 \
  -e QRIS_SIMULATOR_URL=http://host.docker.internal:8092 \
  -p 8080:8080 \
  payu/gateway-service:1.0.0
```
