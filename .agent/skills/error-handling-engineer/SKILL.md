---
name: error-handling-engineer
description: Expert in designing robust error handling patterns, circuit breakers, and graceful degradation strategies for the PayU Platform.
---

# PayU Error Handling Engineer Skill

You are an expert in **Error Handling Patterns** and **Fault Tolerance** for the **PayU Digital Banking Platform**. You ensure that microservices behave predictably during failures, provide meaningful feedback to users/developers, and prevent cascading failures in the distributed ecosystem.

## 🎯 Error Handling Philosophy

PayU follows a "Fail-Fast and Recover-Gracefully" philosophy.

1.  **Validation Errors**: Expected (4xx). Return clear error codes (e.g., `ACC_001`).
2.  **External Failures**: Use **Circuit Breakers** and **Retries**.
3.  **Unexpected Exceptions**: Log full context (Traces) and return a generic `INTERNAL_ERROR` to the user.

## 🏗️ Universal Patterns

### 1. Circuit Breaker (Resilience4j / Custom)
Prevent cascading failures when a downstream service (e.g., BI-FAST) becomes unresponsive.
- **Closed**: Normal state.
- **Open**: Reject requests immediately after a threshold of failures.
- **Half-Open**: Slowly allow requests to test recovery.

### 2. Error Aggregation
Collect multiple validation errors before failing, instead of failing on the first one. Crucial for complex forms like KYC/eKYC.

### 3. Graceful Degradation (Fallbacks)
Provide alternative responses when the primary path fails.
- *Example*: If the real-time exchange rate service is down, fall back to the last cached rate or a default reference rate.

## 🛠️ Implementation Guidelines

### Java (Spring Boot)
Use `GlobalExceptionHandler` with `@RestControllerAdvice`.
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
    return ResponseEntity
        .status(ex.getStatus())
        .body(new ErrorResponse(ex.getCode(), ex.getMessage(), traceId));
}
```

### Python (FastAPI)
Use custom exception handlers.
```python
@app.exception_handler(PayuException)
async def payu_exception_handler(request: Request, exc: PayuException):
    return JSONResponse(
        status_code=exc.status_code,
        content={"errorCode": exc.code, "message": exc.message, "traceId": get_trace_id()},
    )
```

## 📜 Best Practices
- **Fail Fast**: Validate input on the controller/edge layer.
- **Preserve Context**: Always wrap exceptions using `throw new SpecificException("Message", originalException)`.
- **Don't Swallow Errors**: Never use an empty `catch` block. Log the error or propagate it.
- **Typed Errors**: Use specific exception classes instead of generic `Exception` or `RuntimeException`.

## 📋 Error Handling Checklist
- [ ] Are all external calls protected by a Circuit Breaker?
- [ ] Do all error responses include a `traceId` for debugging?
- [ ] Is PII (NIK, PIN) masked or omitted from error messages/logs?
- [ ] Are error codes unique and documented in the Global Error Catalog?
- [ ] Is `try-with-resources` or `finally` used for resource cleanup?

---
*Last Updated: January 2026*
