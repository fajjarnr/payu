# Dukcapil Simulator

> Simulator service for Dukcapil (Direktorat Jenderal Kependudukan dan Pencatatan Sipil) integration testing.

## Overview

This simulator provides a realistic test environment for Indonesian Civil Registry (Dukcapil) integration, enabling developers to test NIK verification, face matching, and citizen data retrieval without connecting to the actual Dukcapil system.

## Features

- **NIK Verification**: Verify NIK and compare with provided personal data
- **Face Matching**: Compare KTP photo with selfie (simulated scores)
- **Citizen Data Retrieval**: Get full citizen information by NIK
- **Liveness Detection**: Simulated liveness check for anti-spoofing
- **Configurable Behavior**:
  - Network latency simulation (100-800ms)
  - Random failure rate (default 3%)
  - Configurable face match threshold (default 75%)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/verify` | NIK verification with data comparison |
| POST | `/api/v1/match-photo` | Face matching (KTP vs Selfie) |
| GET | `/api/v1/nik/{nik}` | Get citizen data by NIK |
| GET | `/api/v1/health` | Health check |

## Test NIK Data

| NIK | Name | Status | Face Match Score |
|-----|------|--------|------------------|
| 3201234567890001 | JOHN DOE | Valid | ~85% |
| 3201234567890002 | JANE DOE | Valid | ~85% |
| 3201234567890003 | BLOCKED USER | Blocked | N/A |
| 3299999999999999 | INVALID NIK | Invalid | ~30% |
| 3201234567890007 | DECEASED PERSON | Deceased | N/A |

## Running Locally

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 16+ (or use Docker)

### Start PostgreSQL

```bash
docker run -d --name dukcapil-db \
  -e POSTGRES_DB=dukcapil_simulator \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
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
    min: 100    # Minimum latency (ms)
    max: 800    # Maximum latency (ms)
  failure-rate: 3  # Random failure percentage
  face-match:
    threshold: 75  # Minimum score for match (0-100)
    variance: 10   # Random variance for scores
```

## API Examples

### NIK Verification

```bash
curl -X POST http://localhost:8091/api/v1/verify \
  -H "Content-Type: application/json" \
  -d '{
    "nik": "3201234567890001",
    "fullName": "JOHN DOE",
    "birthPlace": "JAKARTA",
    "birthDate": "1990-01-15"
  }'
```

Response:
```json
{
  "requestId": "DUK-1705600000000-A1B2C3D4",
  "nik": "3201234567890001",
  "verified": true,
  "fullName": "JOHN DOE",
  "birthPlace": "JAKARTA",
  "birthDate": "1990-01-15",
  "gender": "MALE",
  "address": "JL. SUDIRMAN NO. 123, RT 001/RW 002",
  "status": "VALID",
  "responseCode": "00",
  "responseMessage": "Verification successful - Data matched"
}
```

### Face Matching

```bash
curl -X POST http://localhost:8091/api/v1/match-photo \
  -H "Content-Type: application/json" \
  -d '{
    "nik": "3201234567890001",
    "ktpPhotoBase64": "base64_encoded_ktp_photo...",
    "selfiePhotoBase64": "base64_encoded_selfie...",
    "livenessCheck": true
  }'
```

Response:
```json
{
  "requestId": "DUK-1705600000000-B2C3D4E5",
  "nik": "3201234567890001",
  "matched": true,
  "matchScore": 87,
  "threshold": 75,
  "livenessDetected": true,
  "status": "MATCHED",
  "responseCode": "00",
  "responseMessage": "Face matched with 87% confidence"
}
```

### Get Citizen Data

```bash
curl http://localhost:8091/api/v1/nik/3201234567890001
```

Response:
```json
{
  "requestId": "DUK-1705600000000-C3D4E5F6",
  "nik": "3201234567890001",
  "fullName": "JOHN DOE",
  "birthPlace": "JAKARTA",
  "birthDate": "1990-01-15",
  "gender": "MALE",
  "bloodType": "O",
  "address": "JL. SUDIRMAN NO. 123, RT 001/RW 002",
  "rt": "001",
  "rw": "002",
  "village": "MENTENG",
  "district": "MENTENG",
  "city": "JAKARTA PUSAT",
  "province": "DKI JAKARTA",
  "religion": "ISLAM",
  "maritalStatus": "MARRIED",
  "occupation": "KARYAWAN SWASTA",
  "nationality": "WNI",
  "status": "VALID",
  "responseCode": "00",
  "responseMessage": "Success"
}
```

## Response Codes

| Code | Description |
|------|-------------|
| 00 | Success |
| 14 | NIK not found |
| 30 | Invalid NIK format |
| 51 | Face not matched |
| 52 | Liveness detection failed |
| 62 | NIK blocked/flagged |
| 96 | System error |

## Health & Metrics

- Health: `http://localhost:8091/q/health`
- Metrics: `http://localhost:8091/q/metrics`
- OpenAPI: `http://localhost:8091/q/openapi`

## Docker

```bash
# Build
docker build -t payu/dukcapil-simulator:1.0.0 .

# Run
docker run -d --name dukcapil-simulator \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5433/dukcapil_simulator \
  -p 8091:8091 \
  payu/dukcapil-simulator:1.0.0
```
