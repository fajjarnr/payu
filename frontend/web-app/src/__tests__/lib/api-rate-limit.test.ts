/**
 * RATELOOP-001: 429 Rate Limit Handling Tests
 *
 * The api interceptor surfaces 429 once via toast and NEVER auto-retries:
 * client-side retries amplified one throttled poll into a request storm.
 * These tests drive the real interceptor with a stubbed axios adapter.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { InternalAxiosRequestConfig } from 'axios';
// Mock sonner toast (hoisted: factory runs before imports)
const { mockToastError } = vi.hoisted(() => ({ mockToastError: vi.fn() }));
vi.mock('sonner', () => ({
  toast: {
    error: mockToastError,
  },
}));

import api from '@/lib/api';

function stubAdapter(status: number, headers: Record<string, string> = {}) {
  const calls: InternalAxiosRequestConfig[] = [];
  // Test seam: swap the transport, keep interceptors
  api.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
    calls.push(config);
    return Promise.reject(
      Object.assign(new Error(`Request failed with status code ${status}`), {
        config,
        response: { status, headers, data: {}, config },
      }),
    );
  };
  return calls;
}

describe('RATELOOP-001: 429 is surfaced once, never retried', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('rejects a 429 GET without firing a second request', async () => {
    const calls = stubAdapter(429, { 'retry-after': '1' });
    await expect(api.get('/analytics/user/x/metrics')).rejects.toMatchObject({
      response: { status: 429 },
    });
    expect(calls).toHaveLength(1);
  });

  it('rejects a 429 POST with idempotency key without retrying', async () => {
    const calls = stubAdapter(429);
    await expect(
      api.post('/transactions/transfer', {}, { headers: { 'X-Idempotency-Key': 'k' } }),
    ).rejects.toMatchObject({ response: { status: 429 } });
    expect(calls).toHaveLength(1);
  });

  it('toasts the Retry-After seconds from the server header', async () => {
    stubAdapter(429, { 'retry-after': '7' });
    await expect(api.get('/x')).rejects.toBeDefined();
    expect(mockToastError).toHaveBeenCalledTimes(1);
    expect(mockToastError.mock.calls[0][0]).toContain('7 detik');
  });

  it('toasts a 1-second default when Retry-After is missing', async () => {
    stubAdapter(429);
    await expect(api.get('/x')).rejects.toBeDefined();
    expect(mockToastError.mock.calls[0][0]).toContain('1 detik');
  });
});
