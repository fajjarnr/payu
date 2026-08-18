import { NextRequest } from 'next/server';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('next-intl/middleware', async () => {
  const { NextResponse } = await import('next/server');
  return { default: () => () => NextResponse.next() };
});

vi.mock('@/lib/edge-logger', () => ({
  edgeLogger: {
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
  },
}));

import { proxy } from '@/proxy';

describe('proxy authentication boundary', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('does not trust a forged access-token cookie for protected pages', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 401 })));

    const response = await proxy(new NextRequest('http://localhost/id/dashboard', {
      headers: { cookie: 'accessToken=forged' },
    }));

    expect(response.status).toBe(307);
    expect(response.headers.get('location')).toContain('/id/login');
  });

  it('validates access tokens and disables caching for protected pages', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);

    const response = await proxy(new NextRequest('http://localhost/id/dashboard', {
      headers: { cookie: 'accessToken=valid' },
    }));

    expect(response.status).toBe(200);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url.toString()).toBe('http://gateway-service:8080/api/v1/auth/validate');
    expect(init).toMatchObject({
      headers: { Authorization: 'Bearer valid' },
      cache: 'no-store',
    });
    expect(response.headers.get('cache-control')).toBe('private, no-store');
  });

  it('does not force-logout on a transient validation failure (FE-PROXY-AUTH-001)', async () => {
    // Network timeout to the gateway — the active user must NOT be redirected to login.
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('gateway timeout')));

    const response = await proxy(new NextRequest('http://localhost/id/dashboard', {
      headers: { cookie: 'accessToken=valid' },
    }));

    expect(response.status).toBe(200);
    expect(response.headers.get('location')).toBeNull();
  });
});
