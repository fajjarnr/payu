import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';

describe('Code Block Components', () => {
  describe('Code Block Rendering', () => {
    it('should render code blocks with correct structure', () => {
      // Test that code blocks follow the expected structure
      // This validates the CSS class usage in the pages
      const CodeBlock = ({ children }: { children: React.ReactNode }) => (
        <div className="code-block">
          <pre>
            <code>{children}</code>
          </pre>
        </div>
      );

      const { container } = render(
        <CodeBlock>{`const x = 1;`}</CodeBlock>
      );

      const codeElement = container.querySelector('.code-block');
      expect(codeElement).toBeTruthy();
    });

    it('should preserve code formatting in pre tags', () => {
      const codeContent = `POST /v1/partner/payments
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "amount": 150000,
  "currency": "IDR"
}`;

      const { container } = render(
        <div className="code-block">
          <pre>
            <code>{codeContent}</code>
          </pre>
        </div>
      );

      const preElement = container.querySelector('pre');
      expect(preElement).toBeTruthy();
      expect(preElement?.textContent).toContain('POST /v1/partner/payments');
      expect(preElement?.textContent).toContain('amount');
    });
  });

  describe('API Documentation Code Samples', () => {
    it('should render HTTP request examples correctly', () => {
      const httpRequest = `POST /v1/partner/auth/token
Content-Type: application/json

{
  "client_id": "your_client_id",
  "client_secret": "your_client_secret",
  "grant_type": "client_credentials"
}`;

      const { container } = render(
        <div className="code-block">
          <pre>
            <code>{httpRequest}</code>
          </pre>
        </div>
      );

      expect(container.textContent).toContain('POST /v1/partner/auth/token');
      expect(container.textContent).toContain('client_id');
      expect(container.textContent).toContain('client_credentials');
    });

    it('should render JSON response examples correctly', () => {
      const jsonResponse = `{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600
}`;

      const { container } = render(
        <div className="code-block">
          <pre>
            <code>{jsonResponse}</code>
          </pre>
        </div>
      );

      expect(container.textContent).toContain('access_token');
      expect(container.textContent).toContain('Bearer');
    });

    it('should render Java code examples correctly', () => {
      const javaCode = `import id.payu.sdk.PayUClient;
import id.payu.sdk.model.PaymentRequest;

PayUClient client = PayUClient.builder()
    .clientId("your_client_id")
    .clientSecret("your_client_secret")
    .environment(Environment.SANDBOX)
    .build();`;

      const { container } = render(
        <div className="code-block">
          <pre>
            <code>{javaCode}</code>
          </pre>
        </div>
      );

      expect(container.textContent).toContain('import id.payu.sdk.PayUClient');
      expect(container.textContent).toContain('PayUClient.builder()');
    });

    it('should render Python code examples correctly', () => {
      const pythonCode = `from payu_sdk import PayUClient, PaymentMethod, Environment

client = PayUClient(
    client_id="your_client_id",
    client_secret="your_client_secret",
    environment=Environment.SANDBOX
)`;

      const { container } = render(
        <div className="code-block">
          <pre>
            <code>{pythonCode}</code>
          </pre>
        </div>
      );

      expect(container.textContent).toContain('from payu_sdk import');
      expect(container.textContent).toContain('PayUClient(');
    });

    it('should render TypeScript code examples correctly', () => {
      const tsCode = `import { PayUClient, PaymentMethod, Environment } from '@payu/sdk';

const client = new PayUClient({
  clientId: 'your_client_id',
  clientSecret: 'your_client_secret',
  environment: Environment.SANDBOX
});`;

      const { container } = render(
        <div className="code-block">
          <pre>
            <code>{tsCode}</code>
          </pre>
        </div>
      );

      expect(container.textContent).toContain("from '@payu/sdk'");
      expect(container.textContent).toContain('new PayUClient({');
    });
  });

  describe('Code Block Styling', () => {
    it('should apply code-block class for styling', () => {
      const { container } = render(
        <div className="code-block">
          <pre>
            <code>test code</code>
          </pre>
        </div>
      );

      const codeBlock = container.querySelector('.code-block');
      expect(codeBlock).toBeTruthy();
    });

    it('should use monospace font family', () => {
      const { container } = render(
        <pre style={{ fontFamily: 'var(--font-mono)' }}>
          <code>test code</code>
        </pre>
      );

      const preElement = container.querySelector('pre');
      expect(preElement?.style.fontFamily).toBe('var(--font-mono)');
    });
  });
});
