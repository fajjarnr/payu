/**
 * SSRF Prevention Tests for BFF Proxy Route
 *
 * Tests for BUG-FE-027: Fix BFF Proxy SSRF vulnerability
 * Ensures path traversal and unauthorized path access is blocked.
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock the logger module
vi.mock('@/lib/logger', () => ({
  default: {
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
  },
  getCorrelationId: vi.fn(() => 'test-correlation-id'),
  withCorrelation: vi.fn(() => ({
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
  })),
}));

// Mock next/headers
const mockCookieStore = {
  get: vi.fn(),
};

vi.mock('next/headers', () => ({
  cookies: vi.fn(() => Promise.resolve(mockCookieStore)),
}));

// Mock fetch
global.fetch = vi.fn();

import { GET, POST, PUT, DELETE, PATCH } from '@/app/api/v1/[...path]/route';
import { NextRequest } from 'next/server';

describe('BFF Proxy SSRF Prevention', () => {
  const mockFetch = global.fetch as ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCookieStore.get.mockReturnValue(undefined);
    mockFetch.mockResolvedValue({
      status: 200,
      headers: {
        get: vi.fn().mockReturnValue('application/json'),
      },
      text: vi.fn().mockResolvedValue('{"data": "test"}'),
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // Helper to create mock request
  const createMockRequest = (
    pathname: string,
    method: string = 'GET',
    headers: Record<string, string> = {}
  ): NextRequest => {
    const url = new URL(`http://localhost:3000${pathname}`);
    return {
      method,
      url: url.toString(),
      nextUrl: url,
      headers: new Headers(headers),
    } as unknown as NextRequest;
  };

  // Helper to create params
  const createParams = (pathSegments: string[]) =>
    Promise.resolve({ path: pathSegments });

  describe('Path Traversal Blocking', () => {
    it('should block path traversal with double dots (../)', async () => {
      const request = createMockRequest('/api/v1/../../../etc/passwd');
      const params = createParams(['..', '..', '..', 'etc', 'passwd']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      const body = await response.json();
      expect(body.error).toBe('Bad Request');
      expect(body.message).toBe('Invalid path');
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block path traversal with encoded double dots (%2e%2e)', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['%2e%2e', 'etc', 'passwd']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block path traversal with encoded slash (%2f)', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['..%2f..%2fetc%2fpasswd']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block path traversal with backslash encoding (%5c)', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['..%5c..%5cwindows%5csystem32']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block single dot-dot segment', async () => {
      const request = createMockRequest('/api/v1/../admin/secrets');
      const params = createParams(['..', 'admin', 'secrets']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block mixed case encoded traversal (%2E%2F)', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['%2E%2E', '%2Fetc']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });
  });

  describe('Absolute Path Blocking', () => {
    it('should block absolute paths starting with /', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['/etc', 'passwd']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block absolute path to internal admin', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['/internal', 'admin', 'secrets']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });
  });

  describe('Whitelist Validation', () => {
    it('should allow whitelisted path: /api/v1/wallets', async () => {
      const request = createMockRequest('/api/v1/wallets');
      const params = createParams(['wallets']);

      const response = await GET(request, { params });

      expect(response.status).toBe(200);
      expect(mockFetch).toHaveBeenCalled();
    });

    it('should allow whitelisted path: /api/v1/transactions/123', async () => {
      const request = createMockRequest('/api/v1/transactions/123');
      const params = createParams(['transactions', '123']);

      const response = await GET(request, { params });

      expect(response.status).toBe(200);
      expect(mockFetch).toHaveBeenCalled();
    });

    it('should allow whitelisted path: /api/v1/accounts/profile', async () => {
      const request = createMockRequest('/api/v1/accounts/profile');
      const params = createParams(['accounts', 'profile']);

      const response = await GET(request, { params });

      expect(response.status).toBe(200);
      expect(mockFetch).toHaveBeenCalled();
    });

    it('should block non-whitelisted path: /api/v1/internal/secrets', async () => {
      const request = createMockRequest('/api/v1/internal/secrets');
      const params = createParams(['internal', 'secrets']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      const body = await response.json();
      expect(body.error).toBe('Bad Request');
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block non-whitelisted path: /api/v1/admin/users', async () => {
      const request = createMockRequest('/api/v1/admin/users');
      const params = createParams(['admin', 'users']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block non-whitelisted path: /api/v1/debug/env', async () => {
      const request = createMockRequest('/api/v1/debug/env');
      const params = createParams(['debug', 'env']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block non-whitelisted path: /api/v1/config/database', async () => {
      const request = createMockRequest('/api/v1/config/database');
      const params = createParams(['config', 'database']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });
  });

  describe('Empty and Invalid Path Handling', () => {
    it('should block empty path segments', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['wallets', '', 'balance']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block empty path array', async () => {
      const request = createMockRequest('/api/v1/');
      const params = createParams([]);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block paths with null bytes', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['wallets\x00', 'balance']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block paths with control characters', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['wallets\x01\x02', 'balance']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });
  });

  describe('HTTP Method Coverage', () => {
    const testCases = [
      { method: 'GET', handler: GET },
      { method: 'POST', handler: POST },
      { method: 'PUT', handler: PUT },
      { method: 'DELETE', handler: DELETE },
      { method: 'PATCH', handler: PATCH },
    ];

    testCases.forEach(({ method, handler }) => {
      it(`should block SSRF attempt via ${method} method`, async () => {
        const request = createMockRequest('/api/v1/../../../etc/passwd');
        const params = createParams(['..', '..', '..', 'etc', 'passwd']);

        const response = await handler(request, { params });

        expect(response.status).toBe(400);
        expect(mockFetch).not.toHaveBeenCalled();
      });

      it(`should allow valid request via ${method} method`, async () => {
        const request = createMockRequest('/api/v1/wallets');
        const params = createParams(['wallets']);

        const response = await handler(request, { params });

        expect(response.status).toBe(200);
        expect(mockFetch).toHaveBeenCalled();
      });
    });
  });

  describe('Complex Attack Scenarios', () => {
    it('should block SSRF to internal metadata service', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['..', '..', 'latest', 'meta-data']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block SSRF to localhost', async () => {
      const request = createMockRequest('/api/v1/test');
      // Attempt to reach localhost:8080/admin via path manipulation
      const params = createParams(['..', '..', '..', 'localhost:8080', 'admin']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block double encoded traversal', async () => {
      const request = createMockRequest('/api/v1/test');
      // %252e is double-encoded '.' (%25 = '%', 2e = '.')
      const params = createParams(['%252e%252e', 'etc', 'passwd']);

      const response = await GET(request, { params });

      // Double encoding should be blocked by whitelist check
      // since it won't match any valid prefix
      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block unicode homograph attacks', async () => {
      const request = createMockRequest('/api/v1/test');
      // Using unicode characters that look like dots
      const params = createParams(['\u2024\u2024', 'etc', 'passwd']);

      const response = await GET(request, { params });

      // Unicode dots won't match whitelist
      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block path with @ symbol (userinfo injection attempt)', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['user:pass@evil.com', 'path']);

      const response = await GET(request, { params });

      // @ symbol paths won't match whitelist
      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should block path with # fragment injection', async () => {
      const request = createMockRequest('/api/v1/test');
      const params = createParams(['wallets#evil']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(mockFetch).not.toHaveBeenCalled();
    });
  });

  describe('READY-070 Body-less POST Content-Type handling', () => {
    it('should NOT forward Content-Type when POST body is empty (E2E-2026-06-13-12)', async () => {
      // Simulates POST /api/v1/cards/{id}/freeze from the browser:
      // Content-Type: application/json is set, but the body is empty.
      // The previous code forwarded both, causing gateway 415.
      const request = createMockRequest('/api/v1/cards/abc-123/freeze', 'POST', { 'content-type': 'application/json' });
      (request as unknown as { text: () => Promise<string> }).text =
        vi.fn().mockResolvedValue('');
      const params = createParams(['cards', 'abc-123', 'freeze']);

      const response = await POST(request, { params });

      expect(response.status).toBe(200);
      expect(mockFetch).toHaveBeenCalledTimes(1);
      const calledArgs = mockFetch.mock.calls[0];
      const calledInit = calledArgs[1] as RequestInit;
      const sentHeaders = calledInit.headers as Record<string, string>;
      expect(sentHeaders['Content-Type']).toBeUndefined();
      expect(calledInit.body).toBeUndefined();
    });

    it('SHOULD forward Content-Type when POST body is non-empty', async () => {
      const request = createMockRequest('/api/v1/cards', 'POST', { 'content-type': 'application/json' });
      (request as unknown as { text: () => Promise<string> }).text =
        vi.fn().mockResolvedValue('{"accountId":"abc","cardHolderName":"E2E"}');
      const params = createParams(['cards']);

      const response = await POST(request, { params });

      expect(response.status).toBe(200);
      expect(mockFetch).toHaveBeenCalledTimes(1);
      const calledArgs = mockFetch.mock.calls[0];
      const calledInit = calledArgs[1] as RequestInit;
      const sentHeaders = calledInit.headers as Record<string, string>;
      expect(sentHeaders['Content-Type']).toBe('application/json');
      expect(calledInit.body).toBe('{"accountId":"abc","cardHolderName":"E2E"}');
    });
  });

  describe('HTTP Security Headers Enforcement (AUDIT-038)', () => {
    it('should include all required security headers in successful response', async () => {
      const request = createMockRequest('/api/v1/wallets');
      const params = createParams(['wallets']);

      const response = await GET(request, { params });

      expect(response.status).toBe(200);
      expect(response.headers.get('Strict-Transport-Security')).toBe('max-age=31536000; includeSubDomains; preload');
      expect(response.headers.get('Content-Security-Policy')).toBe("default-src 'none'");
      expect(response.headers.get('X-Frame-Options')).toBe('DENY');
      expect(response.headers.get('X-Content-Type-Options')).toBe('nosniff');
      expect(response.headers.get('X-Request-ID')).toBe('test-correlation-id');
    });

    it('should include all required security headers in bad request response', async () => {
      const request = createMockRequest('/api/v1/../../../etc/passwd');
      const params = createParams(['..', '..', '..', 'etc', 'passwd']);

      const response = await GET(request, { params });

      expect(response.status).toBe(400);
      expect(response.headers.get('Strict-Transport-Security')).toBe('max-age=31536000; includeSubDomains; preload');
      expect(response.headers.get('Content-Security-Policy')).toBe("default-src 'none'");
      expect(response.headers.get('X-Frame-Options')).toBe('DENY');
      expect(response.headers.get('X-Content-Type-Options')).toBe('nosniff');
      expect(response.headers.get('X-Request-ID')).toBe('test-correlation-id');
    });

    it('should include all required security headers in service unavailable response', async () => {
      const request = createMockRequest('/api/v1/wallets');
      const params = createParams(['wallets']);
      mockFetch.mockRejectedValueOnce(new Error('Gateway connection refused'));

      const response = await GET(request, { params });

      expect(response.status).toBe(503);
      expect(response.headers.get('Strict-Transport-Security')).toBe('max-age=31536000; includeSubDomains; preload');
      expect(response.headers.get('Content-Security-Policy')).toBe("default-src 'none'");
      expect(response.headers.get('X-Frame-Options')).toBe('DENY');
      expect(response.headers.get('X-Content-Type-Options')).toBe('nosniff');
      expect(response.headers.get('X-Request-ID')).toBe('test-correlation-id');
    });
  });
});
