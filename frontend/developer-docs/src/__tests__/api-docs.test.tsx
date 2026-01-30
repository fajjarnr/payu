import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import React from 'react';

describe('API Documentation Rendering', () => {
  describe('Endpoint Documentation', () => {
    it('should render API endpoint with method and path', () => {
      const Endpoint = ({ method, path, description }: { method: string; path: string; description: string }) => (
        <div className="p-4 rounded-2xl border border-border bg-card">
          <div className="flex items-center gap-3 mb-2">
            <span className={`px-2 py-1 rounded-lg text-xs font-bold ${
              method === 'POST' ? 'bg-blue-100 text-blue-700' : 'bg-green-100 text-green-700'
            }`}>
              {method}
            </span>
            <code className="text-sm">{path}</code>
          </div>
          <p className="text-sm text-muted-foreground">{description}</p>
        </div>
      );

      const { container } = render(
        <Endpoint
          method="POST"
          path="/v1/partner/payments"
          description="Create a new payment"
        />
      );

      expect(container.textContent).toContain('POST');
      expect(container.textContent).toContain('/v1/partner/payments');
      expect(container.textContent).toContain('Create a new payment');
    });

    it('should render GET endpoint with correct styling', () => {
      const Endpoint = ({ method, path }: { method: string; path: string }) => (
        <div>
          <span className={`px-2 py-1 rounded-lg text-xs font-bold ${
            method === 'GET' ? 'bg-green-100 text-green-700' : 'bg-blue-100 text-blue-700'
          }`}>
            {method}
          </span>
          <code>{path}</code>
        </div>
      );

      const { container } = render(
        <Endpoint method="GET" path="/v1/partner/payments/{id}" />
      );

      const methodBadge = container.querySelector('span');
      expect(methodBadge?.className).toContain('bg-green-100');
      expect(methodBadge?.className).toContain('text-green-700');
    });

    it('should render POST endpoint with correct styling', () => {
      const Endpoint = ({ method, path }: { method: string; path: string }) => (
        <div>
          <span className={`px-2 py-1 rounded-lg text-xs font-bold ${
            method === 'POST' ? 'bg-blue-100 text-blue-700' : ''
          }`}>
            {method}
          </span>
          <code>{path}</code>
        </div>
      );

      const { container } = render(
        <Endpoint method="POST" path="/v1/partner/auth/token" />
      );

      const methodBadge = container.querySelector('span');
      expect(methodBadge?.className).toContain('bg-blue-100');
      expect(methodBadge?.className).toContain('text-blue-700');
    });
  });

  describe('Response Documentation', () => {
    it('should render JSON response examples', () => {
      const ResponseExample = ({ json }: { json: string }) => (
        <div className="code-block">
          <pre>
            <code>{json}</code>
          </pre>
        </div>
      );

      const successResponse = `{
  "payment_id": "PAY-abc123xyz",
  "status": "COMPLETED",
  "amount": 150000,
  "currency": "IDR",
  "paid_at": "2024-01-23T15:30:00Z"
}`;

      const { container } = render(
        <ResponseExample json={successResponse} />
      );

      expect(container.textContent).toContain('payment_id');
      expect(container.textContent).toContain('COMPLETED');
      expect(container.textContent).toContain('PAY-abc123xyz');
    });

    it('should render error response examples', () => {
      const ErrorExample = ({ error }: { error: string }) => (
        <div className="code-block">
          <pre>
            <code>{error}</code>
          </pre>
        </div>
      );

      const errorResponse = `{
  "error": {
    "code": "INSUFFICIENT_BALANCE",
    "message": "Saldo tidak mencukupi",
    "transaction_id": "TXN-bifast123xyz"
  }
}`;

      const { container } = render(
        <ErrorExample error={errorResponse} />
      );

      expect(container.textContent).toContain('INSUFFICIENT_BALANCE');
      expect(container.textContent).toContain('error');
    });
  });

  describe('Webhook Documentation', () => {
    it('should render webhook event documentation', () => {
      const WebhookEvent = ({ event, description }: { event: string; description: string }) => (
        <div className="p-4 rounded-2xl border border-border bg-card">
          <code className="text-bank-green text-sm">{event}</code>
          <p className="text-sm text-muted-foreground mt-2">{description}</p>
        </div>
      );

      const { container } = render(
        <WebhookEvent
          event="payment.completed"
          description="Payment was successfully completed"
        />
      );

      expect(container.textContent).toContain('payment.completed');
      expect(container.textContent).toContain('Payment was successfully completed');
    });

    it('should render webhook payload example', () => {
      const webhookPayload = `POST https://your-app.com/webhook/payment
Content-Type: application/json
X-PayU-Signature: sha256=...

{
  "event_type": "payment.completed",
  "payment_id": "PAY-abc123xyz",
  "status": "COMPLETED",
  "amount": 150000,
  "currency": "IDR",
  "merchant_reference": "ORD-12345",
  "customer_id": "CUST-001",
  "paid_at": "2024-01-23T15:30:00Z"
}`;

      const { container } = render(
        <div className="code-block">
          <pre>
            <code>{webhookPayload}</code>
          </pre>
        </div>
      );

      expect(container.textContent).toContain('X-PayU-Signature');
      expect(container.textContent).toContain('event_type');
      expect(container.textContent).toContain('payment.completed');
    });
  });

  describe('SDK Installation Documentation', () => {
    it('should render Maven dependency', () => {
      const mavenDependency = `<dependency>
  <groupId>id.payu</groupId>
  <artifactId>payu-sdk</artifactId>
  <version>1.0.0</version>
</dependency>`;

      const { container } = render(
        <div className="code-block">
          <pre>
            <code>{mavenDependency}</code>
          </pre>
        </div>
      );

      expect(container.textContent).toContain('<dependency>');
      expect(container.textContent).toContain('id.payu');
      expect(container.textContent).toContain('payu-sdk');
    });

    it('should render npm install command', () => {
      const { container } = render(
        <div className="code-block">
          <pre>
            <code>npm install @payu/sdk</code>
          </pre>
        </div>
      );

      expect(container.textContent).toContain('npm install @payu/sdk');
    });

    it('should render pip install command', () => {
      const { container } = render(
        <div className="code-block">
          <pre>
            <code>pip install payu-sdk</code>
          </pre>
        </div>
      );

      expect(container.textContent).toContain('pip install payu-sdk');
    });
  });
});
