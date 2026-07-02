# PayU Python Logging

Standardized logging for PayU Python microservices with JSON formatting and OpenTelemetry integration.

## Features

- **Structured JSON Logging**: Compatible with PayU Java logging-starter format
- **OpenTelemetry Integration**: Automatic trace and span ID inclusion
- **Correlation ID Propagation**: HTTP header-based request tracking
- **Environment-aware**: Plain text for development, JSON for production
- **FastAPI/Starlette Middleware**: Automatic correlation ID handling

## Installation

```bash
# Install from local path (for development)
pip install -e /path/to/backend/shared/python-logging

# Or install in service requirements.txt
# -e ../../shared/python-logging
```

## Usage

### Basic Setup

```python
from payu_logging import configure_logging
from structlog import get_logger

# Configure once at application startup
configure_logging()

# Get logger instance
logger = get_logger(__name__)

# Log with structured data
logger.info("Processing request", user_id="123", amount=1000)
```

### FastAPI Integration

```python
from fastapi import FastAPI
from payu_logging import configure_logging, CorrelationIdMiddleware
from structlog import get_logger

# Configure logging
configure_logging()

app = FastAPI()

# Add correlation ID middleware
app.add_middleware(CorrelationIdMiddleware)

logger = get_logger(__name__)

@app.get("/health")
async def health_check():
    logger.info("Health check requested")
    return {"status": "ok"}
```

### Environment Configuration

Configure via environment variables:

```bash
# Required
export PAYU_SERVICE_NAME="kyc-service"
export PAYU_SERVICE_VERSION="1.0.0"
export PAYU_ENVIRONMENT="prod"

# Optional
export PAYU_LOG_LEVEL="INFO"
export PAYU_LOG_FORMAT="json"  # 'json' or 'text'
export PAYU_CORRELATION_HEADER="X-Correlation-Id"
```

### Programmatic Configuration

```python
from payu_logging import configure_logging, LoggingConfig

config = LoggingConfig(
    service_name="my-service",
    service_version="1.2.0",
    environment="staging",
    log_level="DEBUG",
    json_format=True,
)

configure_logging(config)
```

## Log Format

### JSON Output (Production)

```json
{
  "timestamp": "2026-02-18T10:30:45.123456",
  "level": "info",
  "logger": "kyc_service.app",
  "message": "KYC verification completed",
  "user_id": "user-123",
  "correlation_id": "a1b2c3d4e5f6789",
  "trace_id": "abc123def456ghi789",
  "span_id": "jkl012mno345pqr678",
  "service": "kyc-service",
  "service_version": "1.0.0",
  "environment": "prod",
  "filename": "kyc.py",
  "func_name": "verify",
  "lineno": 42
}
```

### Plain Text (Development)

```
2026-02-18 10:30:45 [info] kyc_service.app: KYC verification completed [user_id=user-123]
```

## Loki Integration

Query logs in Grafana Loki:

```logql
# All logs from a service
{service="kyc-service"}

# Logs by correlation ID
{correlation_id="a1b2c3d4e5f6789"}

# Logs by trace
{trace_id="abc123def456ghi789"}

# Error logs only
{service="kyc-service"} |= "error" | json
```

## Migration from Standard Logging

### Before

```python
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

logger.info(f"User {user_id} logged in")
```

### After

```python
from payu_logging import configure_logging
from structlog import get_logger

configure_logging()
logger = get_logger(__name__)

logger.info("User logged in", user_id=user_id)
```

## API Reference

### `configure_logging(config=None)`

Configure structlog with PayU standard settings.



### `CorrelationIdMiddleware`

FastAPI/Starlette middleware for correlation ID propagation.

### `LoggingConfig`

Configuration dataclass with options:
- `service_name`: Service identifier
- `service_version`: Semver version
- `environment`: dev/staging/prod
- `log_level`: DEBUG/INFO/WARNING/ERROR
- `json_format`: True for JSON output
- `correlation_id_header`: HTTP header name

## Compatibility

- Python 3.11+
- FastAPI/Starlette
- OpenTelemetry
- Compatible with PayU Java logging-starter format
