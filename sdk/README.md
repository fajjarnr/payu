# PayU SDK

Official SDKs for PayU Payment Gateway integration.

## Overview

PayU SDKs provide easy integration with the PayU Payment Gateway for processing payments, transfers, and wallet operations.

## Available SDKs

### TypeScript/JavaScript

```bash
npm install @payu/sdk
```

### Java

```xml
<dependency>
    <groupId>id.payu</groupId>
    <artifactId>payu-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### TypeScript

```typescript
import { PayUClient } from '@payu/sdk';

const client = new PayUClient({
  apiKey: 'your-api-key',
  apiSecret: 'your-api-secret',
  environment: 'sandbox' // or 'production'
});

// Create a payment
const payment = await client.payments.create({
  amount: 100000,
  currency: 'IDR',
  description: 'Test payment',
  callbackUrl: 'https://your-app.com/callback'
});

console.log('Payment created:', payment.id);
```

### Java

```java
import id.payu.sdk.PayUClient;
import id.payu.sdk.PayUEnvironment;

PayUClient client = PayUClient.builder()
    .apiKey("your-api-key")
    .apiSecret("your-api-secret")
    .environment(PayUEnvironment.SANDBOX)
    .build();

// Create a payment
CreatePaymentRequest request = CreatePaymentRequest.builder()
    .amount(100000)
    .currency("IDR")
    .description("Test payment")
    .callbackUrl("https://your-app.com/callback")
    .build();

PaymentResponse payment = client.payments().create(request);
System.out.println("Payment created: " + payment.getId());
```

## Authentication

All API requests are authenticated using HMAC-SHA256 signatures. The SDK automatically handles signing requests with your API key and secret.

## Error Handling

### TypeScript

```typescript
import { PayUError, PayUValidationError, PayUAuthError } from '@payu/sdk';

try {
  const payment = await client.payments.create({...});
} catch (error) {
  if (error instanceof PayUValidationError) {
    console.log('Validation failed:', error.fieldErrors);
  } else if (error instanceof PayUAuthError) {
    console.log('Authentication failed');
  } else if (error instanceof PayUError) {
    console.log('PayU error:', error.code, error.message);
  }
}
```

### Java

```java
try {
    PaymentResponse payment = client.payments().create(request);
} catch (PayUValidationException e) {
    System.out.println("Validation failed: " + e.getFieldErrors());
} catch (PayUAuthException e) {
    System.out.println("Authentication failed");
} catch (PayUException e) {
    System.out.println("PayU error: " + e.getCode() + " - " + e.getMessage());
}
```

## Sandbox Testing

Use the sandbox environment for testing without moving real money:

### Test Accounts

| Bank | Account Number | Account Name |
|------|---------------|--------------|
| BCA | 1234567890 | John Doe (Test) |
| BNI | 0987654321 | Jane Smith (Test) |
| Mandiri | 1122334455 | Bob Wilson (Test) |

### Test Merchants

| Merchant ID | Category |
|-------------|----------|
| TEST-MERCHANT-001 | Retail |
| TEST-MERCHANT-002 | Food & Beverage |
| TEST-MERCHANT-003 | Services |

### Test Scenarios

- **Success**: Use any valid test account
- **Insufficient Funds**: Amount > 999,999,999
- **Invalid Account**: Account number 0000000000
- **Pending**: Use Mandiri test account (1122334455)

## Features

- **Automatic Retries**: Exponential backoff for transient failures
- **Request Signing**: HMAC-SHA256 authentication
- **Type Safety**: Full TypeScript/Java type definitions
- **Error Handling**: Comprehensive error types
- **Sandbox Support**: Test environment with deterministic responses

## API Coverage

- Payments
  - Create payment
  - Get payment status
  - Cancel payment
  - Refund payment
- Transfers
  - Create transfer
  - Get transfer status
- Wallets
  - Get balance
  - Top up
  - Withdraw
- Budgets
  - Create budget
  - Check spending
  - Get budget status

## Documentation

Full API documentation: https://docs.payu.fajjjar.my.id

## License

MIT License - see LICENSE file for details.

## Support

- Email: dev@payu.fajjjar.my.id
- Issues: https://github.com/payu/sdk/issues
