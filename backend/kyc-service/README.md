# PayU KYC Service

eKYC (Electronic Know Your Customer) service with OCR, liveness detection, and face matching capabilities.

## Technology Stack

- **Framework**: FastAPI 0.115.0
- **Runtime**: Python 3.12 (UBI9-based)
- **OCR**: PaddleOCR (Indonesian KTP support)
- **Face Recognition**: OpenCV + Custom ML models
- **Liveness Detection**: Computer vision-based
- **Database**: PostgreSQL (asyncpg + SQLAlchemy 2.0)
- **Message Broker**: Kafka (aiokafka)
- **Monitoring**: Prometheus + OpenTelemetry + Jaeger

## Features

### 1. KTP OCR (Optical Character Recognition)
- Extracts data from Indonesian ID cards (KTP)
- Supports: NIK, Name, Birth Date, Gender, Address, Province, City, District
- Confidence score calculation
- PaddleOCR-based with Indonesian language support

### 2. Liveness Detection
- Prevents spoofing with photo/video attacks
- Face detection and quality assessment
- Eye openness, mouth movement, head pose analysis
- Skin texture verification

### 3. Face Matching
- Matches KTP photo with selfie
- Cosine similarity-based matching
- Configurable threshold
- Face encoding and comparison

### 4. Dukcapil Integration
- NIK verification with external service
- Real-time citizen data validation
- Status checking (VALID, BLOCKED, INVALID, DECEASED)

### 5. KYC Workflow
```
1. POST /api/v1/kyc/verify/start
   ↓
2. POST /api/v1/kyc/verify/ktp (upload KTP)
   ↓ OCR extraction
   ↓
3. POST /api/v1/kyc/verify/selfie (upload selfie)
   ↓ Liveness check
   ↓ Face matching
   ↓ Dukcapil verification
   ↓
4. GET /api/v1/kyc/verify/{id}
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/kyc/verify/start` | POST | Start new KYC verification |
| `/api/v1/kyc/verify/ktp` | POST | Upload KTP image for OCR |
| `/api/v1/kyc/verify/selfie` | POST | Upload selfie for verification |
| `/api/v1/kyc/verify/{id}` | GET | Get verification status |
| `/api/v1/kyc/user/{user_id}` | GET | Get user KYC history |
| `/health` | GET | Health check |
| `/metrics` | GET | Prometheus metrics |

## Configuration

Environment variables (`.env`):
```env
# Database
DATABASE_URL=postgresql+asyncpg://payu:payu@localhost:5432/payu_kyc

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Dukcapil Simulator
DUKCAPIL_URL=http://localhost:8091/api/v1

# Security
SECRET_KEY=your-secret-key-change-in-production
```

## Development

```bash
cd backend/kyc-service

# Install dependencies
pip install -r requirements.txt

# Run development server
uvicorn app.main:app --reload --port 8007

# Run tests
pytest

# Run with coverage
pytest --cov=src
```

## Docker Build

```bash
# Build image
docker build -t payu/kyc-service:1.0.0 .

# Run container
docker run -p 8007:8007 \
  -e DATABASE_URL=postgresql+asyncpg://payu:payu@postgres:5432/payu_kyc \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e DUKCAPIL_URL=http://dukcapil-simulator:8091/api/v1 \
  payu/kyc-service:1.0.0
```

## Database Schema

```sql
CREATE TABLE kyc_verifications (
    verification_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    verification_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    ktp_image_url VARCHAR(255),
    selfie_image_url VARCHAR(255),
    ktp_ocr_result JSONB,
    liveness_result JSONB,
    face_match_result JSONB,
    dukcapil_result JSONB,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_kyc_user_id ON kyc_verifications(user_id);
CREATE INDEX idx_kyc_status ON kyc_verifications(status);
```

## Error Codes

| Code | Category | Description |
|------|----------|-------------|
| `KYC_VAL_001` | Validation | KTP validation failed |
| `KYC_VAL_002` | Validation | Selfie validation failed |
| `KYC_VAL_003` | Validation | Verification not found |
| `KYC_SYS_001` | System | Internal server error |
| `KYC_SYS_002` | System | KTP processing failed |
| `KYC_SYS_003` | System | Selfie processing failed |
| `KYC_SYS_004` | System | Status fetch failed |
| `KYC_SYS_005` | System | History fetch failed |

## Security

- NIK and PII data masked in logs
- Image data size limited (10MB max)
- JWT authentication support
- Rate limiting recommended

## Monitoring

- **Metrics**: Prometheus endpoint `/metrics`
- **Tracing**: OpenTelemetry integration
- **Logging**: Structured JSON logs

## License

Proprietary - PayU Indonesia
