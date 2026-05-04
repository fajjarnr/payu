---
name: ai-engineer
version: 2.0.0
maturity: stable
updated: 2026-05-04
author: payu-platform-team
requires: [data-architect]
tags: [ai, ml, python, fastapi, llm, mlops]
related: [data-architect, api-architect]
description: **Master Skill**: Intelligent Systems Architect. Covers ML Lifecycle (Scikit-Learn/ONNX), Python API Scaffolding (FastAPI/Pydantic v2), Data Engineering (TimescaleDB), MLOps, and Generative AI patterns.
---

# PayU Intelligent Systems Architect Master Skill

You are a **Senior ML & Backend Engineer** for the **PayU Platform**. You build scalable, production-grade AI microservices using **Python 3.12**, **FastAPI**, and robust engineering patterns on **OpenShift**.

---

## 🐍 FastAPI Project Structure

```
ml-service/
├── pyproject.toml
├── src/
│   ├── main.py              # FastAPI app initialization
│   ├── config.py            # Settings (Pydantic BaseSettings)
│   ├── database.py          # Async SQLAlchemy/TimescaleDB
│   ├── models/              # ML model inference
│   │   ├── fraud_detector.py
│   │   └── risk_scorer.py
│   ├── api/                 # API domain
│   │   ├── router.py        # API endpoints
│   │   ├── schemas.py       # Pydantic v2 models
│   │   └── dependencies.py  # Shared dependencies
│   ├── services/            # Business logic
│   │   ├── fraud_service.py
│   │   └── feature_service.py
│   └── utils/               # Shared utilities
└── tests/
```

---

## 🚀 FastAPI Performance & Async

### Async vs Sync Routes

```python
# ✅ Use async def for I/O bound tasks (DB, REST calls)
@app.get("/users/{user_id}")
async def get_user(user_id: str, db: AsyncSession = Depends(get_db)):
    return await db.execute(select(User).where(User.id == user_id))

# ✅ Use def (not async) for CPU-bound tasks
# FastAPI runs these in a thread pool automatically
@app.post("/predict")
def predict(data: InputSchema):
    return model.predict(data.features)  # CPU-bound inference

# ✅ Use run_in_executor for blocking calls in async routes
@app.post("/heavy-inference")
async def heavy_inference(data: InputSchema):
    loop = asyncio.get_event_loop()
    result = await loop.run_in_executor(
        executor, 
        lambda: heavy_model.predict(data.features)
    )
    return {"prediction": result}
```

### Never Block the Event Loop

```python
# ❌ WRONG: Blocks event loop
@app.get("/slow")
async def slow_endpoint():
    time.sleep(5)  # NEVER do this!
    return {"status": "done"}

# ✅ RIGHT: Non-blocking
@app.get("/slow")
async def slow_endpoint():
    await asyncio.sleep(5)  # Non-blocking
    return {"status": "done"}
```

---

## 🛡️ Pydantic v2 Schemas

```python
from pydantic import BaseModel, Field, ConfigDict
from datetime import datetime
from enum import Enum

class RiskLevel(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"

class TransactionInput(BaseModel):
    transaction_id: str = Field(..., min_length=10)
    amount: float = Field(..., gt=0, le=100_000_000)
    user_id: str
    merchant_id: str
    timestamp: datetime
    metadata: dict | None = None

class FraudScoreResponse(BaseModel):
    transaction_id: str
    score: float = Field(..., ge=0, le=1)
    risk_level: RiskLevel
    features_used: list[str]
    model_version: str
    inference_time_ms: float
    
    model_config = ConfigDict(from_attributes=True)
```

---

## ⚠️ FastAPI Known Issues & Fixes

### Issue #1: 422 Error with Form Data + Pydantic Model

```python
# ❌ PROBLEMATIC: Form data with Pydantic model
@app.post("/form")
async def endpoint(model: Annotated[MyModel, Form()]):
    pass  # May cause 422 errors

# ✅ USE: Individual form fields
@app.post("/form")
async def endpoint(
    name: Annotated[str, Form()],
    amount: Annotated[float, Form()]
):
    pass
```

### Issue #2: Union Types in Path Parameters

```python
# ❌ PROBLEMATIC: Pydantic v2 always parses as str
@app.get("/item/{id}")
async def get_item(id: int | str):
    print(type(id))  # Always <class 'str'>!

# ✅ USE: Specific type
@app.get("/item/{id}")
async def get_item(id: int):
    pass
```

### Issue #3: Forward References Break OpenAPI

```python
# ❌ AVOID: __future__ annotations in route files
from __future__ import annotations

# ✅ USE: Define classes before use, or use string literals
def get_model() -> "MyModel":
    return MyModel()
```

---

## 🤖 ML Lifecycle & MLOps

### 1. Model Inference Pipeline

```python
# models/fraud_detector.py
import onnxruntime as ort
import numpy as np
from functools import lru_cache

class FraudDetector:
    def __init__(self, model_path: str):
        self.session = ort.InferenceSession(model_path)
        self.input_name = self.session.get_inputs()[0].name
        
    def predict(self, features: np.ndarray) -> float:
        """Run inference and return fraud probability."""
        result = self.session.run(
            None, 
            {self.input_name: features.astype(np.float32)}
        )
        return float(result[0][0])

@lru_cache(maxsize=1)
def get_fraud_detector() -> FraudDetector:
    """Singleton model loader."""
    return FraudDetector("models/fraud_model.onnx")
```

### 2. Feature Engineering Service

```python
# services/feature_service.py
from datetime import datetime, timedelta

class FeatureService:
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def get_user_features(self, user_id: str) -> dict:
        """Extract real-time features for fraud detection."""
        # Get transaction history from TimescaleDB
        query = """
            SELECT 
                COUNT(*) as txn_count_24h,
                SUM(amount) as total_amount_24h,
                AVG(amount) as avg_amount_24h,
                COUNT(DISTINCT merchant_id) as unique_merchants_24h
            FROM transactions
            WHERE user_id = :user_id
            AND created_at > NOW() - INTERVAL '24 hours'
        """
        result = await self.db.execute(text(query), {"user_id": user_id})
        row = result.fetchone()
        
        return {
            "txn_count_24h": row.txn_count_24h or 0,
            "total_amount_24h": float(row.total_amount_24h or 0),
            "avg_amount_24h": float(row.avg_amount_24h or 0),
            "unique_merchants_24h": row.unique_merchants_24h or 0,
        }
```

### 3. Fraud Scoring Endpoint

```python
# api/router.py
from fastapi import APIRouter, Depends
from datetime import datetime
import time

router = APIRouter(prefix="/v1/fraud", tags=["fraud"])

@router.post("/score", response_model=FraudScoreResponse)
async def calculate_fraud_score(
    txn: TransactionInput,
    feature_service: FeatureService = Depends(get_feature_service),
    model: FraudDetector = Depends(get_fraud_detector),
):
    start_time = time.perf_counter()
    
    # Get user features
    user_features = await feature_service.get_user_features(txn.user_id)
    
    # Prepare feature vector
    features = np.array([
        txn.amount,
        user_features["txn_count_24h"],
        user_features["total_amount_24h"],
        user_features["avg_amount_24h"],
        user_features["unique_merchants_24h"],
    ]).reshape(1, -1)
    
    # Run inference
    score = model.predict(features)
    
    inference_time = (time.perf_counter() - start_time) * 1000
    
    return FraudScoreResponse(
        transaction_id=txn.transaction_id,
        score=score,
        risk_level=categorize_risk(score),
        features_used=list(user_features.keys()),
        model_version="v1.2.3",
        inference_time_ms=inference_time,
    )

def categorize_risk(score: float) -> RiskLevel:
    if score < 0.3:
        return RiskLevel.LOW
    elif score < 0.6:
        return RiskLevel.MEDIUM
    elif score < 0.85:
        return RiskLevel.HIGH
    return RiskLevel.CRITICAL
```

---

## 🧠 Generative AI (LLM Ops)

### 1. Prompt Management

```python
# prompts/templates.py
PROMPTS = {
    "transaction_summary": {
        "version": "1.0.0",
        "template": """
You are a financial assistant for PayU Digital Bank.
Summarize the following transactions for the user:

Transactions:
{transactions}

Provide a brief, friendly summary in Indonesian.
""",
    },
    "fraud_explanation": {
        "version": "1.0.0",
        "template": """
Explain why transaction {transaction_id} was flagged as {risk_level} risk.
Factors: {factors}
Keep explanation under 100 words, suitable for customer support.
""",
    },
}

def get_prompt(name: str, **kwargs) -> str:
    """Get versioned prompt with variables filled."""
    prompt_config = PROMPTS[name]
    return prompt_config["template"].format(**kwargs)
```

### 2. Streaming LLM Responses

```python
from fastapi.responses import StreamingResponse
import httpx

@router.post("/chat/stream")
async def chat_stream(message: str, user_id: str):
    """Stream LLM response for better UX."""
    
    async def generate():
        async with httpx.AsyncClient() as client:
            async with client.stream(
                "POST",
                f"{LLM_API_URL}/chat/completions",
                json={
                    "model": "gpt-4",
                    "messages": [{"role": "user", "content": message}],
                    "stream": True,
                },
                headers={"Authorization": f"Bearer {LLM_API_KEY}"},
            ) as response:
                async for chunk in response.aiter_text():
                    yield chunk
    
    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
    )
```

### 3. LLM Guardrails

```python
# services/llm_guardrails.py
import re

class LLMGuardrails:
    PII_PATTERNS = [
        r'\b\d{16}\b',           # Credit card
        r'\b\d{3}-\d{2}-\d{4}\b', # SSN
        r'\b[A-Z]{2}\d{6,}\b',    # ID numbers
    ]
    
    def sanitize_input(self, text: str) -> str:
        """Remove PII before sending to external LLM."""
        for pattern in self.PII_PATTERNS:
            text = re.sub(pattern, "[REDACTED]", text)
        return text
    
    def validate_output(self, text: str) -> str:
        """Ensure LLM output doesn't contain sensitive data."""
        # Additional validation logic
        return text
```

---

## 📊 Data Engineering (TimescaleDB)

### Hypertable Setup for Time-Series Data

```sql
-- Create transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL,
    merchant_id VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'IDR',
    status VARCHAR(20) NOT NULL,
    fraud_score DECIMAL(5, 4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Convert to hypertable (partitioned by time)
SELECT create_hypertable('transactions', 'created_at');

-- Create continuous aggregate for real-time analytics
CREATE MATERIALIZED VIEW hourly_transaction_stats
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', created_at) AS bucket,
    user_id,
    COUNT(*) as txn_count,
    SUM(amount) as total_amount,
    AVG(fraud_score) as avg_fraud_score
FROM transactions
GROUP BY bucket, user_id;

-- Refresh policy
SELECT add_continuous_aggregate_policy('hourly_transaction_stats',
    start_offset => INTERVAL '1 day',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour'
);
```

---

## 🔬 Model Monitoring & Drift Detection

```python
# monitoring/model_monitor.py
from prometheus_client import Histogram, Counter, Gauge

# Metrics
INFERENCE_LATENCY = Histogram(
    'ml_inference_latency_seconds',
    'Model inference latency',
    ['model_name', 'model_version']
)

PREDICTION_DISTRIBUTION = Histogram(
    'ml_prediction_distribution',
    'Distribution of model predictions',
    ['model_name'],
    buckets=[0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]
)

DRIFT_SCORE = Gauge(
    'ml_feature_drift_score',
    'Feature drift score (PSI)',
    ['feature_name']
)

class ModelMonitor:
    def record_prediction(
        self, 
        model_name: str, 
        model_version: str,
        prediction: float,
        latency: float
    ):
        INFERENCE_LATENCY.labels(
            model_name=model_name,
            model_version=model_version
        ).observe(latency)
        
        PREDICTION_DISTRIBUTION.labels(
            model_name=model_name
        ).observe(prediction)
```

---

## 🔍 Quality & AI Guardrails Checklist

### API & Validation
- [ ] Every input validated with Pydantic v2
- [ ] Thread pools configured for heavy inference
- [ ] Async used correctly (I/O vs CPU bound)

### ML Operations
- [ ] Model versioning implemented
- [ ] Inference latency tracked via Prometheus
- [ ] Model drift monitored (PSI/KS test)
- [ ] A/B testing framework ready

### Security & Compliance
- [ ] PII never sent to external LLM providers
- [ ] Input sanitization before LLM calls
- [ ] Output validation after LLM responses
- [ ] Audit logging for all predictions

### Performance
- [ ] ONNX Runtime for cross-platform inference
- [ ] Feature caching for repeated users
- [ ] Batch inference for high throughput

---

## 📚 References

- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [Pydantic v2 Documentation](https://docs.pydantic.dev/latest/)
- [ONNX Runtime](https://onnxruntime.ai/)
- [TimescaleDB Documentation](https://docs.timescale.com/)
- [MLflow](https://mlflow.org/)
- [Evidently AI (Model Monitoring)](https://www.evidentlyai.com/)
- [LangChain](https://python.langchain.com/)

---
*Last Updated: 2026-05-04*

