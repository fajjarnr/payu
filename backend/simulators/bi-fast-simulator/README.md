# BI-FAST Simulator

> Simulator service for BI-FAST (Bank Indonesia Fast Payment) integration testing.

## Overview

This simulator provides a realistic test environment for BI-FAST integration, allowing developers to test fund transfers, account inquiries, and webhook callbacks without connecting to the actual BI-FAST network.

## Features

- **Account Inquiry**: Validate destination account before transfer
- **Fund Transfer**: Simulate real-time fund transfers
- **Status Check**: Query transfer status by reference number
- **Webhook Callbacks**: Async notification simulation
- **Configurable Behavior**:
  - Network latency simulation (50-500ms)
  - Random failure rate (default 5%)
  - Timeout scenarios

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/inquiry` | Account inquiry |
| POST | `/api/v1/transfer` | Initiate fund transfer |
| GET | `/api/v1/status/{ref}` | Get transfer status |
| GET | `/api/v1/health` | Health check |

## Test Bank Accounts

| Bank Code | Account Number | Name | Status |
|-----------|----------------|------|--------|
| BCA | 1234567890 | JOHN DOE | Active |
| BRI | 0987654321 | JANE DOE | Active |
| MANDIRI | 1111222233 | TEST BLOCKED | Blocked |
| BNI | 9999888877 | TEST TIMEOUT | Timeout |

## Running Locally

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 16+ (or use Docker)

### Start PostgreSQL

```bash
docker run -d --name bifast-db \
  -e POSTGRES_DB=bifast_simulator \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

### Run in Development Mode

```bash
./mvnw quarkus:dev
```

### Run Tests

```bash
./mvnw test
```

### Build Native Image

```bash
./mvnw package -Pnative
```

## Configuration

Key configuration options in `application.yaml`:

```yaml
simulator:
  latency:
    min: 50     # Minimum latency (ms)
    max: 500    # Maximum latency (ms)
  failure-rate: 5  # Random failure percentage
  webhook:
    enabled: true
    delay-ms: 2000
```

## API Examples

### Account Inquiry

```bash
curl -X POST http://localhost:8090/api/v1/inquiry \
  -H "Content-Type: application/json" \
  -d '{
    "bankCode": "BCA",
    "accountNumber": "1234567890"
  }'
```

Response:
```json
{
  "bankCode": "BCA",
  "accountNumber": "1234567890",
  "accountName": "JOHN DOE",
  "status": "ACTIVE",
  "responseCode": "00",
  "responseMessage": "Success"
}
```

### Fund Transfer

```bash
curl -X POST http://localhost:8090/api/v1/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "sourceBankCode": "BCA",
    "sourceAccountNumber": "9876543210",
    "sourceAccountName": "SENDER NAME",
    "destinationBankCode": "BRI",
    "destinationAccountNumber": "0987654321",
    "amount": 100000,
    "description": "Test transfer",
    "webhookUrl": "http://localhost:8080/webhook/bifast"
  }'
```

Response:
```json
{
  "referenceNumber": "BIFAST-1705600000000-A1B2C3D4",
  "sourceBankCode": "BCA",
  "sourceAccountNumber": "9876543210",
  "destinationBankCode": "BRI",
  "destinationAccountNumber": "0987654321",
  "destinationAccountName": "JANE DOE",
  "amount": 100000,
  "currency": "IDR",
  "status": "COMPLETED",
  "responseCode": "00",
  "responseMessage": "Transfer completed successfully"
}
```

### Transfer Status

```bash
curl http://localhost:8090/api/v1/status/BIFAST-1705600000000-A1B2C3D4
```

## Health & Metrics

- Health: `http://localhost:8090/q/health`
- Metrics: `http://localhost:8090/q/metrics`
- OpenAPI: `http://localhost:8090/q/openapi`

## Docker

```bash
# Build
docker build -t payu/bi-fast-simulator:1.0.0 .

# Run
docker run -d --name bi-fast-simulator \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/bifast_simulator \
  -p 8090:8090 \
  payu/bi-fast-simulator:1.0.0
```
