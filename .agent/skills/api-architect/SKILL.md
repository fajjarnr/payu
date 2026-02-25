---
name: api-architect
version: 2.0.0
maturity: stable
updated: 2026-01-30
author: payu-platform-team
requires: []
tags: [api, rest, openapi, governance, fastapi, pydantic]
related: [core-banking-engineer]
description: **Master Skill**: REST API design, OpenAPI standards, FastAPI Python patterns, and robust 3rd-party integrations (OAuth2, Webhooks, Retries).
---

## 📚 Reference Implementation Patterns
For detailed patterns and historical context on PayU API standards, see:
- [API Design & Standards](./references/API_STANDARDS.md)

# PayU API Expert Skill

You are the **Lead API Architect** for the **PayU Digital Banking Platform**. You own the standards for RESTful design, contract-first development, and robust integration with external financial partners (BI-FAST, QRIS, Payment Gateways).

## 🎯 Core Principles

| Principle | Description |
|-----------|-------------|
| **Consistency** | Same URL patterns and envelope across all services. |
| **Idempotency** | Mandatory for all mutations to prevent double-spending. |
| **Resilience** | Integration must handle upstream slow-downs and failures gracefully. |
| **Security** | Zero-trust authentication, signed requests, and PII masking. |

---

## 📐 REST API Standards

### URL Structure & Naming
- **Version**: Always include version in URI (e.g., `/v1/accounts`).
- **Resource**: Use nouns, plural, kebab-case (e.g., `/bank-accounts`).
- **Successors**: Use `Deprecation` and `Sunset` headers for old versions.

### HTTP Methods & Status
- **GET**: Retrieve (200 OK).
- **POST**: Create (201 Created) - include `Location` header.
- **PUT/PATCH**: Update (200 OK).
- **DELETE**: Remove (204 No Content).
- **Errors**: 400 (Validation), 401 (Auth), 403 (Forbidden), 422 (Business Rule), 429 (Rate Limit).

---

## 📦 Request/Response Format (Modern Standards)

### 1. Success Envelope
```json
{
    "success": true,
    "data": { ... },
    "meta": {
        "requestId": "req-123",
        "timestamp": "2026-01-31T10:30:00Z"
    }
}
```

### 2. Error Response (RFC 9457 - Problem Details)
PayU uses RFC 9457 for errors to provide a standardized, machine-readable format.

```json
{
    "type": "https://api.payu.fajjjar.my.id/errors/insufficient-funds",
    "title": "Insufficient Funds",
    "status": 422,
    "detail": "Your wallet balance (Rp 5.000) is less than the requested transfer amount (Rp 10.000).",
    "instance": "/api/v1/transfers/trx-999",
    "error_code": "WAL_001",
    "trace_id": "req-123-abc",
    "errors": [
        { "field": "amount", "message": "Balance too low" }
    ]
}
```

---

## 🐍 FastAPI Patterns (Python Services)

**Stack**: FastAPI 0.128.0 | Pydantic v2.11+ | SQLAlchemy 2.0 async | Python 3.9+

### Project Structure (Domain-Based)

```
my-api/
├── pyproject.toml
├── src/
│   ├── main.py              # FastAPI app initialization
│   ├── config.py            # Global settings
│   ├── database.py          # Database connection
│   ├── auth/                # Auth domain
│   │   ├── router.py        # Auth endpoints
│   │   ├── schemas.py       # Pydantic models
│   │   ├── models.py        # SQLAlchemy models
│   │   ├── service.py       # Business logic
│   │   └── dependencies.py  # Auth dependencies
│   ├── items/               # Items domain
│   │   ├── router.py
│   │   ├── schemas.py
│   │   ├── models.py
│   │   └── service.py
│   └── shared/              # Shared utilities
│       └── exceptions.py
└── tests/
```

### Pydantic Schemas (Validation)

```python
from pydantic import BaseModel, Field, ConfigDict
from datetime import datetime
from enum import Enum

class ItemStatus(str, Enum):
    DRAFT = "draft"
    PUBLISHED = "published"

class ItemBase(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    price: float = Field(..., gt=0, description="Price must be positive")
    status: ItemStatus = ItemStatus.DRAFT

class ItemCreate(ItemBase):
    pass

class ItemUpdate(BaseModel):
    name: str | None = Field(None, min_length=1, max_length=100)
    price: float | None = Field(None, gt=0)

class ItemResponse(ItemBase):
    id: int
    created_at: datetime
    model_config = ConfigDict(from_attributes=True)
```

### Async SQLAlchemy 2.0

```python
# database.py
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase

DATABASE_URL = "sqlite+aiosqlite:///./database.db"
engine = create_async_engine(DATABASE_URL, echo=True)
async_session = async_sessionmaker(engine, expire_on_commit=False)

class Base(DeclarativeBase):
    pass

async def get_db():
    async with async_session() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise
```

### Router Pattern

```python
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

router = APIRouter(prefix="/items", tags=["items"])

@router.get("", response_model=list[schemas.ItemResponse])
async def list_items(
    skip: int = 0,
    limit: int = 100,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(models.Item).offset(skip).limit(limit))
    return result.scalars().all()

@router.post("", response_model=schemas.ItemResponse, status_code=status.HTTP_201_CREATED)
async def create_item(
    item_in: schemas.ItemCreate,
    db: AsyncSession = Depends(get_db)
):
    item = models.Item(**item_in.model_dump())
    db.add(item)
    await db.commit()
    await db.refresh(item)
    return item
```

### JWT Authentication

```python
# auth/service.py
from datetime import datetime, timedelta
from jose import JWTError, jwt
from passlib.context import CryptContext

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def create_access_token(data: dict, expires_delta: timedelta | None = None) -> str:
    to_encode = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=15))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm="HS256")

# auth/dependencies.py
from fastapi.security import OAuth2PasswordBearer

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")

async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: AsyncSession = Depends(get_db)
) -> models.User:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    payload = service.decode_token(token)
    if payload is None:
        raise credentials_exception
    # ... fetch user from DB
    return user
```

---

## ⚠️ FastAPI: 7 Critical Error Preventions

### Issue #1: Form Data Loses Field Set Metadata

**Problem**: `model.model_fields_set` includes default values when using `Form()`

```python
# ✗ AVOID: Unreliable field_set with Form
@app.post("/form")
async def endpoint(model: Annotated[MyModel, Form()]):
    fields = model.model_fields_set  # Unreliable! ❌

# ✓ USE: Individual form fields or JSON body
@app.post("/form-individual")
async def endpoint(
    field_1: Annotated[bool, Form()] = True,
    field_2: Annotated[str | None, Form()] = None
):
    pass  # You know exactly what was provided ✓
```

### Issue #2: BackgroundTasks Overwritten by Custom Response

**Problem**: Tasks added via `BackgroundTasks` don't run when returning `Response(background=...)`

```python
# ✗ WRONG: Mixing both mechanisms
@app.get("/")
async def endpoint(tasks: BackgroundTasks):
    tasks.add_task(send_email)  # This will be lost! ❌
    return Response(content="Done", background=BackgroundTask(log_event))

# ✓ RIGHT: Use only BackgroundTasks dependency
@app.get("/")
async def endpoint(tasks: BackgroundTasks):
    tasks.add_task(send_email)
    tasks.add_task(log_event)
    return {"status": "done"}  # All tasks run ✓
```

### Issue #3: Optional Form Fields Break with TestClient

**Problem**: Optional Literal fields fail validation when passed `None` via TestClient (FastAPI 0.114.0+)

```python
# ✗ PROBLEMATIC
@app.post("/")
async def endpoint(attribute: Annotated[Optional[Literal["abc", "def"]], Form()]):
    return {"attribute": attribute}

# ✓ WORKAROUND: Omit the field instead of passing None
data = {}  # Omit instead of None
```

### Issue #4: Pydantic Json Type Doesn't Work with Form Data

```python
# ✗ WRONG: Json type directly with Form
@app.post("/broken")
async def broken(json_list: Annotated[Json[list[str]], Form()]):
    return json_list  # Returns 422 ❌

# ✓ RIGHT: Accept as str, parse with Pydantic
@app.post("/working")
async def working(json_list: Annotated[str, Form()]):
    model = JsonListModel(json_list=json_list)
    return model.json_list  # Works ✓
```

### Issue #5: Annotated with ForwardRef Breaks OpenAPI

**Problem**: Forward references with `Depends()` break OpenAPI schema generation

```python
# ✓ WORKAROUND: Define classes before they're used in dependencies
@dataclass
class Potato:
    color: str
    size: int

def get_potato() -> Potato:  # Now works ✓
    return Potato(color='red', size=10)
```

### Issue #6: Pydantic v2 Union Type Breaking Change

**Problem**: `int | str` path parameters always parse as `str` in Pydantic v2

```python
# ✗ PROBLEMATIC
@app.get("/int/{path}")
async def int_path(path: int | str):
    return str(type(path))  # Always returns <class 'str'> ❌

# ✓ RIGHT: Avoid union types with str in path parameters
@app.get("/int/{path}")
async def int_path(path: int):
    return str(type(path))  # Works correctly ✓
```

### Issue #7: ValueError in field_validator Returns 500

```python
# ✗ WRONG: ValueError returns 500
class MyForm(BaseModel):
    value: int
    @field_validator('value')
    def validate_value(cls, v):
        if v < 0:
            raise ValueError("Must be positive")  # Returns 500! ❌
        return v

# ✓ RIGHT: Use Pydantic's built-in constraints
class MyForm(BaseModel):
    value: Annotated[int, Field(gt=0)]  # Returns 422 ✓
```

---

## 🔄 Async Blocking Prevention

**Symptoms**: Throughput plateaus, latency balloons, requests queue indefinitely

```python
# ✗ WRONG: Blocks event loop
@app.get("/users")
async def get_users():
    time.sleep(0.1)  # Even small blocking adds up! ❌
    result = sync_db_client.query("SELECT * FROM users")
    return result

# ✓ RIGHT 1: Use async database driver
@app.get("/users")
async def get_users(db: AsyncSession = Depends(get_db)):
    await asyncio.sleep(0.1)  # Non-blocking
    result = await db.execute(select(User))
    return result.scalars().all()

# ✓ RIGHT 2: Use def (not async def) for CPU-bound routes
@app.get("/cpu-heavy")
def cpu_heavy_task():  # FastAPI runs in thread pool automatically
    return expensive_cpu_work()

# ✓ RIGHT 3: Use run_in_executor for blocking calls
@app.get("/mixed")
async def mixed_task():
    result = await asyncio.get_event_loop().run_in_executor(
        executor, blocking_function
    )
    return result
```

---

## 🔗 Internal & External Integration Patterns

### 1. Robust API Client (Spring Boot 3.4+)
Use `RestClient` for sync or `WebClient` for async calls. Spring Boot 3.4 recommends `RestClient` for most sync blocking scenarios.

```java
@Service
public class PartnerGatewayClient {
    private final RestClient restClient;

    public PartnerGatewayClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.partner.com").build();
    }

    @CircuitBreaker(name = "partner-api", fallbackMethod = "fallback")
    public PartnerResponse send(PartnerRequest req) {
        return restClient.post()
            .uri("/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(req)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                throw new PartnerClientException(response.getStatusCode());
            })
            .body(PartnerResponse.class);
    }
}
```

### 2. Declarative Clients (@HttpExchange)
```java
public interface BankInterface {
    @PostExchange("/inquiry")
    InquiryResponse inquiry(@RequestBody InquiryRequest request);
}

// In Config
@Bean
BankInterface bankInterface(RestClient.Builder builder) {
    RestClient restClient = builder.baseUrl("https://api.bank.id").build();
    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(restClient)).build();
    return factory.createClient(BankInterface.class);
}
```

### 2. Authentication & Key Management
- **API Keys**: Store in Vault/Environment, never in code.
- **OAuth2**: Use Client Credentials flow for service-to-service.
- **Request Signing**: Use HMAC-SHA256 for high-security partner calls.

### 3. Webhook Handling (Inbound)
PayU relies heavily on webhooks for async payment confirmations.
- **Verification**: Always verify HMAC signatures using a rolling timestamp (prevent replay).
- **Quick ACK**: Return `202 Accepted` immediately, process logic via Kafka.
- **Idempotency**: Check `webhook_id` in Redis/DB before processing.

---

## ⛩️ API Gateway Patterns (Gateway-Service)
The **Gateway Service** (Quarkus Native) is the entry point for all mobile and partner traffic.

### 1. Security & Traffic Control
- **Rate-Limiting**: Enforced per `API-Key` and `IP Address`. Return `429 Too Many Requests` when limits are exceeded.
- **PII Striping**: Automatically strip or mask sensitive headers before forwarding requests to internal microservices.
- **TLS Termination**: Handle HTTPS at the gateway to offload compute from internal pods.

### 2. Request Transformation
- **Header Injection**: Inject `X-User-Id` and `X-Request-Correlation-Id` into the internal request context.
- **BFF Aggregation**: Use the gateway or dedicated Node.js BFF to aggregate data from multiple services (Account + Recent Transactions) into a single response.

---

## 📜 OpenAPI & Type Synchronization
- **Contract-First**: Use OpenAPI 3.1 to define schemas before coding.
- **Zod Sync**: Generate Zod schemas and TypeScript interfaces from OpenAPI for Frontend/Mobile type safety.

---

## 🛡️ Contract-First Enforcement (Spectral)

To maintain high API quality, every microservice must validate its OpenAPI specification against PayU standards using **Spectral**.

### 1. PayU Spectral Ruleset (.spectral.yaml)
```yaml
extends: ["spectral:oas", "spectral:oas3"]
rules:
  # Ensure standard envelope usage
  use-standard-envelope:
    description: All responses must follow the PayU standard envelope (success, data, error, meta).
    severity: error
    given: $.paths.*.*.responses[200,201].content.application/json.schema
    then:
      field: properties
      function: contains
      functionOptions:
        fields: ["success", "data", "meta"]

  # Enforce kebab-case for paths
  path-kebab-case:
    description: Path segments must be in kebab-case.
    severity: error
    given: $.paths[*]~
    then:
      function: pattern
      functionOptions:
        match: "^(/[a-z0-0-]+)+$"

  # Mandatory Idempotency-Key for POST/PUT
  mandatory-idempotency:
    description: All mutation endpoints must require Idempotency-Key header.
    severity: error
    given: $.paths.*[post,put].parameters
    then:
      field: name
      function: contains
      functionOptions:
        fields: ["Idempotency-Key"]
```

### 2. CI/CD Pipeline Enforcement (Tekton/GitHub Actions)
```bash
# Fail the build if OpenAPI spec doesn't match standards
spectral lint ./docs/openapi/service-spec.yaml --ruleset .spectral.yaml --fail-severity error
```

---

---

## 🛠️ Integration Checklist
- [ ] **Idempotency**: Does the POST endpoint support `Idempotency-Key`?
- [ ] **Timeouts**: Are Connect/Read timeouts configured or using defaults (danger)?
- [ ] **Retries**: Does it use exponential backoff for 5xx/429?
- [ ] **Webhooks**: Is signature verification and idempotency implemented?
- [ ] **PII**: Are sensitive fields (PIN, CVV) encrypted/masked in transit?
- [ ] **Async**: Are all I/O operations truly async (not blocking)?

---
*Last Updated: January 2026*
