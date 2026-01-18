# QRIS Simulator

> Simulator service for QRIS (Quick Response Code Indonesian Standard) payment testing.

## Overview

This simulator provides a realistic test environment for QRIS payment integration, enabling developers to test QR code generation, payment processing, and status checking without connecting to actual payment networks.

## Features

- **QR Code Generation**: Generate QRIS-compliant QR codes with embedded payment data
- **Payment Simulation**: Simulate customer scanning and paying QR codes
- **Status Checking**: Query payment status by QR ID or reference number
- **QR Expiry**: Automatic expiry handling (configurable, default 5 minutes)
- **Configurable Behavior**:
  - Network latency simulation (50-300ms)
  - Random failure rate (default 2%)
  - Customizable QR expiry time
  - QR image size and format

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/generate` | Generate QRIS code |
| POST | `/api/v1/pay` | Simulate payment |
| GET | `/api/v1/status/{qrId}` | Get payment status |
| GET | `/api/v1/health` | Health check |

## Test Merchants

| Merchant ID | Name | Category | Status |
|-------------|------|----------|--------|
| MCH001 | Warung Makan Sederhana | Food & Beverage | Active |
| MCH002 | Kopi Kenangan | Food & Beverage | Active |
| MCH020 | Toko Elektronik Jaya | Electronics | Active |
| MCH030 | Apotek Sehat Selalu | Health | Active |
| MCH999 | Test Blocked Merchant | Other | Blocked |

## Running Locally

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 16+ (or use Docker)

### Start PostgreSQL

```bash
docker run -d --name qris-db \
  -e POSTGRES_DB=qris_simulator \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5434:5432 \
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

## Configuration

Key configuration options in `application.yaml`:

```yaml
simulator:
  latency:
    min: 50
    max: 300
  failure-rate: 2
  qr:
    expiry-seconds: 300  # 5 minutes
    image-size: 300      # pixels
    format: PNG
  webhook:
    enabled: true
    delay-ms: 1000
```

## API Examples

### Generate QRIS Code

```bash
curl -X POST http://localhost:8092/api/v1/generate \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "MCH001",
    "amount": 50000,
    "tipAmount": 5000,
    "description": "Makan siang",
    "expirySeconds": 300,
    "webhookUrl": "http://localhost:8080/webhook/qris"
  }'
```

Response:
```json
{
  "qrId": "QR-1705600000000-A1B2C3",
  "referenceNumber": "QRIS-1705600000000-D4E5F6G7",
  "merchantId": "MCH001",
  "merchantName": "Warung Makan Sederhana",
  "amount": 50000,
  "tipAmount": 5000,
  "currency": "IDR",
  "qrContent": "00020101010212...",
  "qrImageBase64": "data:image/png;base64,iVBORw0KGgo...",
  "status": "PENDING",
  "expiresAt": "2026-01-18T23:20:00",
  "createdAt": "2026-01-18T23:15:00",
  "responseCode": "00",
  "responseMessage": "QR code generated successfully"
}
```

### Simulate Payment

```bash
curl -X POST http://localhost:8092/api/v1/pay \
  -H "Content-Type: application/json" \
  -d '{
    "qrId": "QR-1705600000000-A1B2C3",
    "payerName": "JOHN DOE",
    "payerAccount": "1234567890",
    "payerBank": "BCA"
  }'
```

Response:
```json
{
  "qrId": "QR-1705600000000-A1B2C3",
  "referenceNumber": "QRIS-1705600000000-D4E5F6G7",
  "merchantId": "MCH001",
  "merchantName": "Warung Makan Sederhana",
  "amount": 50000,
  "tipAmount": 5000,
  "currency": "IDR",
  "payerName": "JOHN DOE",
  "payerAccount": "1234567890",
  "payerBank": "BCA",
  "status": "PAID",
  "paidAt": "2026-01-18T23:16:00",
  "responseCode": "00",
  "responseMessage": "Payment successful"
}
```

### Get Payment Status

```bash
curl http://localhost:8092/api/v1/status/QR-1705600000000-A1B2C3
```

Response:
```json
{
  "qrId": "QR-1705600000000-A1B2C3",
  "referenceNumber": "QRIS-1705600000000-D4E5F6G7",
  "merchantId": "MCH001",
  "merchantName": "Warung Makan Sederhana",
  "amount": 50000,
  "tipAmount": 5000,
  "currency": "IDR",
  "payerName": "JOHN DOE",
  "payerAccount": "1234567890",
  "payerBank": "BCA",
  "status": "PAID",
  "expiresAt": "2026-01-18T23:20:00",
  "createdAt": "2026-01-18T23:15:00",
  "paidAt": "2026-01-18T23:16:00",
  "failureReason": null,
  "responseCode": "00",
  "responseMessage": "Payment completed"
}
```

## Response Codes

| Code | Description |
|------|-------------|
| 00 | Success |
| 09 | Pending |
| 14 | QR/Merchant not found |
| 51 | Payment failed |
| 54 | QR expired |
| 55 | Already paid |
| 56 | Cancelled |
| 62 | Merchant blocked |
| 96 | System error |

## Payment Flow

```
1. Merchant generates QRIS code
   POST /api/v1/generate
   → Returns qrId, qrImageBase64

2. Customer scans QR code
   (Display QR image to customer)

3. Customer pays via their banking app
   POST /api/v1/pay (simulates payment)
   → Returns payment result

4. Merchant checks payment status
   GET /api/v1/status/{qrId}
   → Returns current status

5. (Optional) Webhook callback sent
   to merchant's webhookUrl
```

## Health & Metrics

- Health: `http://localhost:8092/q/health`
- Metrics: `http://localhost:8092/q/metrics`
- OpenAPI: `http://localhost:8092/q/openapi`

## Docker

```bash
# Build
docker build -t payu/qris-simulator:1.0.0 .

# Run
docker run -d --name qris-simulator \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5434/qris_simulator \
  -p 8092:8092 \
  payu/qris-simulator:1.0.0
```
