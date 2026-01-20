# PayU Analytics Service

User insights, spending patterns, and ML recommendations with TimescaleDB time-series analytics.

## Technology Stack

- **Framework**: FastAPI 0.115.0
- **Runtime**: Python 3.12 (UBI9-based)
- **Database**: TimescaleDB (PostgreSQL extension)
- **Message Broker**: Kafka (aiokafka)
- **ML**: scikit-learn, pandas
- **Monitoring**: Prometheus + OpenTelemetry + Jaeger

## Features

### 1. Time-Series Analytics
- Transaction analytics with TimescaleDB hypertables
- Wallet balance history tracking
- User activity logs
- Automatic data partitioning and retention

### 2. Spending Insights
- Spending trends by category
- Month-over-month change analysis
- Top merchant identification
- Cash flow analysis

### 3. User Metrics
- Total transactions and amount
- Average transaction value
- Account age tracking
- KYC status integration

### 4. ML Recommendations
- Savings goal recommendations
- Budget alerts
- Spending trend analysis
- Inactivity reminders
- Investment suggestions

### 5. Kafka Integration
- Real-time event consumption
- Transaction events tracking
- Wallet balance updates
- KYC verification events

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/analytics/user/{user_id}/metrics` | GET | Get user metrics |
| `/api/v1/analytics/spending/trends` | POST | Get spending trends |
| `/api/v1/analytics/cashflow` | POST | Get cash flow analysis |
| `/api/v1/analytics/user/{user_id}/recommendations` | GET | Get recommendations |
| `/health` | GET | Health check |
| `/metrics` | GET | Prometheus metrics |

## Configuration

Environment variables (`.env`):
```env
# Database (TimescaleDB)
DATABASE_URL=postgresql+asyncpg://payu:payu@localhost:5432/payu_analytics

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Security
SECRET_KEY=your-secret-key-change-in-production

# TimescaleDB
TIMESCALE_HYPERTABLE_RETENTION_DAYS=365
TIMESCALE_CHUNK_INTERVAL_DAYS=7
```

## Database Schema

### TimescaleDB Hypertables

```sql
-- Transaction Analytics Hypertable
CREATE TABLE transaction_analytics (
    event_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    transaction_id VARCHAR(36) NOT NULL,
    amount FLOAT NOT NULL,
    currency VARCHAR(3) DEFAULT 'IDR',
    transaction_type VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    recipient_id VARCHAR(36),
    merchant_id VARCHAR(36),
    metadata JSONB,
    timestamp TIMESTAMP NOT NULL
);

SELECT create_hypertable('transaction_analytics', 'timestamp',
    chunk_time_interval => interval '7 days');

CREATE INDEX idx_transactions_user_time ON transaction_analytics(user_id, timestamp);
CREATE INDEX idx_transactions_type_time ON transaction_analytics(transaction_type, timestamp);

-- Wallet Balance History Hypertable
CREATE TABLE wallet_balance_history (
    event_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    wallet_id VARCHAR(36) NOT NULL,
    balance FLOAT NOT NULL,
    currency VARCHAR(3) DEFAULT 'IDR',
    change_amount FLOAT NOT NULL,
    change_type VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP NOT NULL
);

SELECT create_hypertable('wallet_balance_history', 'timestamp',
    chunk_time_interval => interval '7 days');

-- User Activity Analytics Hypertable
CREATE TABLE user_activity_analytics (
    event_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    session_id VARCHAR(36),
    device_type VARCHAR(50),
    ip_address VARCHAR(45),
    user_agent TEXT,
    duration_seconds INTEGER,
    metadata JSONB,
    timestamp TIMESTAMP NOT NULL
);

SELECT create_hypertable('user_activity_analytics', 'timestamp',
    chunk_time_interval => interval '7 days');

-- User Metrics Table
CREATE TABLE user_metrics (
    user_id VARCHAR(36) PRIMARY KEY,
    total_transactions BIGINT DEFAULT 0,
    total_amount FLOAT DEFAULT 0,
    average_transaction FLOAT DEFAULT 0,
    last_transaction_date TIMESTAMP,
    account_age_days INTEGER DEFAULT 0,
    kyc_status VARCHAR(20),
    updated_at TIMESTAMP NOT NULL
);

-- Recommendations Table
CREATE TABLE recommendations (
    recommendation_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    recommendation_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    action_url VARCHAR(255),
    priority INTEGER DEFAULT 0,
    is_dismissed BOOLEAN DEFAULT FALSE,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    dismissed_at TIMESTAMP
);

CREATE INDEX idx_recommendations_user_created ON recommendations(user_id, created_at);
CREATE INDEX idx_recommendations_dismissed ON recommendations(is_dismissed);
```

## Development

```bash
cd backend/analytics-service

# Install dependencies
pip install -r requirements.txt

# Run development server
uvicorn app.main:app --reload --port 8008

# Run tests
pytest

# Run with coverage
pytest --cov=src
```

## Docker Build

```bash
# Build image
docker build -t payu/analytics-service:1.0.0 .

# Run container
docker run -p 8008:8008 \
  -e DATABASE_URL=postgresql+asyncpg://payu:payu@timescaledb:5432/payu_analytics \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e SECRET_KEY=your-secret-key \
  payu/analytics-service:1.0.0
```

## Error Codes

| Code | Category | Description |
|------|----------|-------------|
| `ANA_VAL_001` | Validation | User not found |
| `ANA_SYS_001` | System | Internal server error (metrics) |
| `ANA_SYS_002` | System | Internal server error (trends) |
| `ANA_SYS_003` | System | Internal server error (cashflow) |
| `ANA_SYS_004` | System | Internal server error (recommendations) |

## Recommendation Types

| Type | Description |
|------|-------------|
| `SAVINGS_GOAL` | Suggest creating savings goals |
| `BUDGET_ALERT` | Warn about budget overruns |
| `SPENDING_TREND` | Notify of spending pattern changes |
| `NEW_FEATURE` | Suggest trying new features |
| `PROMOTION` | Offer promotional content |
| `INVESTMENT` | Suggest investment opportunities |

## Monitoring

- **Metrics**: Prometheus endpoint `/metrics`
- **Tracing**: OpenTelemetry integration
- **Logging**: Structured JSON logs

## License

Proprietary - PayU Indonesia
