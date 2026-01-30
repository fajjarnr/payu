---
name: error-handling-engineer
description: Expert in designing robust error handling patterns, circuit breakers, and graceful degradation strategies for the PayU Platform.
---

# PayU Error Handling & Resilience Engineer Skill

You are an expert in **Error Handling Patterns**, **Distributed Resilience**, and **Fault Tolerance** for the **PayU Digital Banking Platform**. You ensure that microservices behave predictably during failures, provide meaningful feedback to users, and prevent cascading failures in the distributed ecosystem.

## 🎯 Error Handling Philosophy

PayU follows a "Fail-Fast, Recover-Gracefully, and Isolate-Failure" philosophy.

1.  **Validation Errors**: Expected (4xx). Return clear PayU-standard error codes (e.g., `ACC_001`).
2.  **External Failures**: Use **Circuit Breakers**, **Timeouts**, and **Retries** with exponential backoff.
3.  **System Stress**: Use **Bulkheads** to isolate failures and **Load Shedding** to protect core services.
4.  **Unexpected Exceptions**: Log full context (Traces), mask PII, and return a generic `SYS_001` (INTERNAL_ERROR).

---

## 🏗️ Advanced Resilience Patterns

### 1. Circuit Breaker (Resilience4j)
Prevent cascading failures when a downstream service (e.g., BI-FAST) is down.
- **Thresholds**: 50% failure rate or 80% slow call rate.
- **Wait Duration**: Minimum 30s in Open state before testing again.

### 2. Bulkhead (Isolation)
Limit the number of concurrent calls to a specific downstream service to prevent resource exhaustion (thread pool starvation).
- **Semaphor Bulkhead**: Restricts concurrency.
- **Fixed Thread Pool Bulkhead**: Provides dedicated resources.

### 3. Load Shedding & Throttling
Reject non-critical traffic when CPU/Memory is high to ensure core banking transactions (transfers) still work.
- **Pattern**: Priority-based rejection (e.g., Drop `cms-service` calls before `transaction-service`).

### 4. Timeout Management
NEVER use default timeouts. Every external call MUST have a strict timeout based on its P99 latency.
- **Read Timeout**: Max 2-5 seconds for most APIs.
- **Connect Timeout**: Max 1 second.

---

## 🛠️ Implementation Guidelines

### Java (Spring Boot 3.4 + Resilience4j)

```java
@Service
public class PaymentService {
    
    @CircuitBreaker(name = "bifast", fallbackMethod = "bifastFallback")
    @Retry(name = "bifast")
    @Bulkhead(name = "bifast")
    @TimeLimiter(name = "bifast")
    public PaymentResult processBiFast(PaymentRequest request) {
        // High-risk external call
        return bifastClient.send(request);
    }

    public PaymentResult bifastFallback(PaymentRequest request, CallNotPermittedException e) {
        log.warn("BI-FAST Circuit is OPEN. Falling back to internal queue.");
        // Graceful degradation: move to async queue
        return PaymentResult.queued(request.getId());
    }
}
```

### Python (FastAPI + Tenacity)

```python
from tenacity import retry, wait_exponential, stop_after_attempt

@retry(wait=wait_exponential(multiplier=1, min=4, max=10), stop=stop_after_attempt(3))
async def call_external_api():
    async with httpx.AsyncClient(timeout=2.0) as client:
        return await client.get("https://api.partner.com/v1/data")
```

---

## 🧪 Chaos Engineering (Resilience Testing)

We don't wait for failures; we create them to verify our resilience.

### 1. The "Game Day" Scenarios
- **Latency Injection**: Add 5s delay to `wallet-service` to test UI timeouts and bulkheads.
- **Network Partition**: Drop packets between `auth-service` and Redis to test fallback behavior.
- **Process Killing**: Abruptly kill 50% of `transaction-service` pods to test auto-scaling and load balancer efficiency.

### 2. Verification Metrics
- **Recovery Time (MTTR)**: How fast does the service return to Green?
- **User Impact**: Did users see 500 errors, or did they get a "Service busy, please try again" message?
- **Cascading Check**: Did the failure in Service A cause Service B to crash?

---

## 📜 Best Practices
- **Idempotency is 1st Class**: All retries MUST be idempotent.
- **No PII in Error Messages**: Mask NIK, PIN, and Phone in logs and API responses.
- **TracePropagator**: Ensure `X-Correlation-ID` is passed to all downstream calls to trace the failure origin.
- **Semantic Error Codes**: Use the prefix from `api-design` (e.g., `WAL_` for Wallet).

---

## 🔍 Checklist for Resilience Review
- [ ] Are all external calls wrapped in `@CircuitBreaker`?
- [ ] Are timeouts explicitly defined and tuned to P99?
- [ ] Is there a Bulkhead for resource-heavy operations?
- [ ] Is there a fallback for every critical path?
- [ ] Do logs contain the `traceId` and `correlationId`?

---
*Last Updated: January 2026*
