---
name: api-design
description: Expert in REST API design, OpenAPI specifications, versioning, and API best practices for PayU Digital Banking Platform.
---

# PayU API Design Skill

You are an API design expert for the **PayU Digital Banking Platform**. Your expertise covers REST API standards, OpenAPI specifications, versioning strategies, and API security patterns.

## 🎯 Core Principles

| Principle | Description |
|-----------|-------------|
| **Consistency** | Same patterns across all services |
| **Predictability** | Developers know what to expect |
| **Simplicity** | Easy to understand and use |
| **Evolvability** | APIs can change without breaking clients |

---

## 📐 REST API Standards

### URL Structure

```
https://api.payu.id/v1/{resource}/{id}/{sub-resource}

Examples:
GET    /v1/accounts                    # List accounts
POST   /v1/accounts                    # Create account
GET    /v1/accounts/{id}               # Get account
PUT    /v1/accounts/{id}               # Update account
DELETE /v1/accounts/{id}               # Delete account
GET    /v1/accounts/{id}/transactions  # Get account transactions
POST   /v1/accounts/{id}/pockets       # Create pocket for account
```

### Naming Conventions

| Rule | Example |
|------|---------|
| Use nouns, not verbs | ✅ `/accounts` ❌ `/getAccounts` |
| Use plural nouns | ✅ `/users` ❌ `/user` |
| Use kebab-case | ✅ `/bank-accounts` ❌ `/bankAccounts` |
| Use lowercase | ✅ `/transactions` ❌ `/Transactions` |

### HTTP Methods

| Method | Usage | Idempotent | Safe |
|--------|-------|------------|------|
| `GET` | Retrieve resource | ✅ | ✅ |
| `POST` | Create resource | ❌ | ❌ |
| `PUT` | Replace resource | ✅ | ❌ |
| `PATCH` | Partial update | ❌ | ❌ |
| `DELETE` | Remove resource | ✅ | ❌ |

### HTTP Status Codes

```java
// Success
200 OK              // GET, PUT, PATCH success
201 Created         // POST success (include Location header)
204 No Content      // DELETE success

// Client Errors
400 Bad Request     // Validation error
401 Unauthorized    // Missing/invalid authentication
403 Forbidden       // Authenticated but not authorized
404 Not Found       // Resource doesn't exist
409 Conflict        // Duplicate or state conflict
422 Unprocessable   // Business rule violation
429 Too Many Req    // Rate limit exceeded

// Server Errors
500 Internal Error  // Unexpected server error
502 Bad Gateway     // Upstream service error
503 Unavailable     // Service temporarily down
504 Gateway Timeout // Upstream timeout
```

---

## 📦 Request/Response Format

### Standard Response Envelope

```java
// Success Response
{
    "success": true,
    "data": {
        "id": "acc-123",
        "accountNumber": "1234567890",
        "balance": 1000000.00
    },
    "meta": {
        "requestId": "req-abc-123",
        "timestamp": "2026-01-26T10:30:00Z"
    }
}

// Error Response
{
    "success": false,
    "error": {
        "code": "INSUFFICIENT_BALANCE",
        "message": "Saldo tidak mencukupi untuk transaksi ini",
        "details": [
            {
                "field": "amount",
                "message": "Jumlah melebihi saldo tersedia (Rp 500.000)"
            }
        ]
    },
    "meta": {
        "requestId": "req-abc-456",
        "timestamp": "2026-01-26T10:30:00Z"
    }
}
```

### Pagination

```java
// Request
GET /v1/transactions?page=1&size=20&sort=createdAt,desc

// Response
{
    "success": true,
    "data": [...],
    "pagination": {
        "page": 1,
        "size": 20,
        "totalElements": 150,
        "totalPages": 8,
        "hasNext": true,
        "hasPrevious": false
    },
    "links": {
        "self": "/v1/transactions?page=1&size=20",
        "next": "/v1/transactions?page=2&size=20",
        "last": "/v1/transactions?page=8&size=20"
    }
}
```

### Cursor-Based Pagination (for Real-time Data)

```java
// Request
GET /v1/notifications?cursor=eyJpZCI6MTIzfQ&limit=20

// Response
{
    "success": true,
    "data": [...],
    "pagination": {
        "nextCursor": "eyJpZCI6MTQzfQ",
        "hasMore": true
    }
}
```

---

## 🔍 Filtering & Searching

### Query Parameters

```java
// Filtering
GET /v1/transactions?status=COMPLETED&type=TRANSFER

// Date Range
GET /v1/transactions?startDate=2026-01-01&endDate=2026-01-31

// Search
GET /v1/users?search=john

// Combined
GET /v1/transactions?status=COMPLETED&minAmount=100000&startDate=2026-01-01&sort=amount,desc&page=1&size=20
```

### Filter Operators (Advanced)

```java
// For complex filtering, use filter parameter
GET /v1/transactions?filter=amount:gte:100000,status:in:COMPLETED|PENDING

// Operators:
// eq  - equals (default)
// ne  - not equals
// gt  - greater than
// gte - greater than or equals
// lt  - less than
// lte - less than or equals
// in  - in list (pipe separated)
// like - contains (for strings)
```

---

## 🔐 API Security

### Authentication Header

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Required Headers

```http
X-Request-ID: req-abc-123          # Unique request identifier
X-Correlation-ID: corr-xyz-789     # For distributed tracing
X-Client-Version: 1.0.0            # Mobile app version
Accept-Language: id-ID             # Localization
```

### Rate Limiting Headers

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1706267400
Retry-After: 60                    # Only when 429
```

### Idempotency Key

```http
# For POST requests that should be idempotent
Idempotency-Key: idem-123-456-789

# Server checks if request was already processed
# Returns cached response if key exists
```

---

## 📝 OpenAPI Specification

### Standard Schema

```yaml
openapi: 3.0.3
info:
  title: PayU Account Service API
  version: 1.0.0
  description: API for managing user accounts and pockets
  contact:
    email: api-support@payu.id

servers:
  - url: https://api.payu.id/v1
    description: Production
  - url: https://api.staging.payu.id/v1
    description: Staging

security:
  - bearerAuth: []

paths:
  /accounts:
    get:
      summary: List accounts
      operationId: listAccounts
      tags:
        - Accounts
      parameters:
        - $ref: '#/components/parameters/PageParam'
        - $ref: '#/components/parameters/SizeParam'
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AccountListResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'
```

### Reusable Components

```yaml
components:
  schemas:
    Account:
      type: object
      required:
        - id
        - accountNumber
        - status
      properties:
        id:
          type: string
          format: uuid
          example: "550e8400-e29b-41d4-a716-446655440000"
        accountNumber:
          type: string
          pattern: "^[0-9]{10}$"
          example: "1234567890"
        balance:
          $ref: '#/components/schemas/Money'
        status:
          type: string
          enum: [ACTIVE, FROZEN, CLOSED]
        createdAt:
          type: string
          format: date-time
    
    Money:
      type: object
      required:
        - amount
        - currency
      properties:
        amount:
          type: number
          format: decimal
          example: 1000000.00
        currency:
          type: string
          pattern: "^[A-Z]{3}$"
          default: "IDR"
    
    Error:
      type: object
      required:
        - code
        - message
      properties:
        code:
          type: string
          example: "INSUFFICIENT_BALANCE"
        message:
          type: string
          example: "Saldo tidak mencukupi"
        details:
          type: array
          items:
            $ref: '#/components/schemas/FieldError'

  parameters:
    PageParam:
      name: page
      in: query
      schema:
        type: integer
        minimum: 1
        default: 1
    
    SizeParam:
      name: size
      in: query
      schema:
        type: integer
        minimum: 1
        maximum: 100
        default: 20

  responses:
    Unauthorized:
      description: Authentication required
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
          example:
            code: "UNAUTHORIZED"
            message: "Token tidak valid atau sudah kadaluarsa"

  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

---

## 🔄 API Versioning

### URI Versioning (Preferred)

```
/v1/accounts
/v2/accounts
```

### Deprecation Strategy

```http
# Response headers for deprecated endpoints
Deprecation: true
Sunset: Sat, 01 Jul 2026 00:00:00 GMT
Link: </v2/accounts>; rel="successor-version"
```

### Breaking vs Non-Breaking Changes

| Non-Breaking (OK) | Breaking (New Version) |
|-------------------|------------------------|
| Add optional field | Remove field |
| Add new endpoint | Rename field |
| Add enum value | Change field type |
| Increase limit | Remove endpoint |
| Add optional param | Change URL structure |

---

## 🛠️ Implementation Patterns

### Controller Structure (Spring Boot)

```java
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "List accounts")
    public ResponseEntity<ApiResponse<Page<AccountDto>>> listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var accounts = accountService.findByUserId(principal.getUserId(), 
            PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<ApiResponse<AccountDto>> getAccount(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var account = accountService.findById(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping
    @Operation(summary = "Create account")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var account = accountService.create(request, principal.getUserId());
        var location = URI.create("/v1/accounts/" + account.getId());
        return ResponseEntity.created(location)
            .body(ApiResponse.success(account));
    }
}
```

### API Response Wrapper

```java
@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorInfo error;
    private MetaInfo meta;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .meta(MetaInfo.now())
            .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .error(new ErrorInfo(code, message))
            .meta(MetaInfo.now())
            .build();
    }
}

@Data
@AllArgsConstructor
public class MetaInfo {
    private String requestId;
    private Instant timestamp;

    public static MetaInfo now() {
        return new MetaInfo(
            MDC.get("requestId"),
            Instant.now()
        );
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
            .toList();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<Void>builder()
                .success(false)
                .error(ErrorInfo.builder()
                    .code("VALIDATION_ERROR")
                    .message("Request validation failed")
                    .details(errors)
                    .build())
                .meta(MetaInfo.now())
                .build());
    }
}
```

---

## 📋 API Design Checklist

Before publishing an API:

- [ ] URL follows REST conventions (nouns, plural, kebab-case)
- [ ] Correct HTTP methods and status codes
- [ ] Request/response follows standard envelope
- [ ] Pagination implemented for list endpoints
- [ ] Proper error responses with codes
- [ ] OpenAPI specification documented
- [ ] Authentication required on protected endpoints
- [ ] Rate limiting configured
- [ ] Idempotency key supported for mutations
- [ ] Request validation with clear error messages

---

*Last Updated: January 2026*
