/**
 * PayU TypeScript SDK
 *
 * Official SDK for integrating with PayU Payment Gateway.
 * Supports payments, transfers, wallet operations, and more.
 *
 * @example
 * ```typescript
 * import { PayUClient } from '@payu/sdk';
 *
 * const client = new PayUClient({
 *   apiKey: 'your-api-key',
 *   apiSecret: 'your-api-secret',
 *   environment: 'sandbox' // or 'production'
 * });
 *
 * // Create a payment
 * const payment = await client.payments.create({
 *   amount: 100000,
 *   currency: 'IDR',
 *   description: 'Test payment'
 * });
 * ```
 */

export { PayUClient, PayUClientConfig } from './client';
export { PayUError, PayUApiError, PayUAuthError, PayUValidationError } from './errors';
export { AuthInterceptor } from './interceptors/auth';
export { RetryInterceptor } from './interceptors/retry';
export * from './generated/models';
export * from './generated/api';

// Version
export const VERSION = '1.0.0';
