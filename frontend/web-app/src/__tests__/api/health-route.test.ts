import { describe, expect, it, beforeEach, afterEach, vi } from 'vitest';
import { GET } from '../../app/api/health/route';

describe('health route', () => {
  const originalVersion = process.env.APP_VERSION;
  const originalGateway = process.env.GATEWAY_URL;

  beforeEach(() => {
    process.env.APP_VERSION = '1.5.10-test';
    process.env.GATEWAY_URL = 'http://gateway-service:8080';
  });

  afterEach(() => {
    vi.restoreAllMocks();
    process.env.APP_VERSION = originalVersion;
    process.env.GATEWAY_URL = originalGateway;
  });

  it('returns liveness without calling dependencies', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');

    const response = await GET(new Request('http://localhost/api/health?probe=liveness'));
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toMatchObject({ status: 'healthy', version: '1.5.10-test' });
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('accepts the Kubernetes probe header for liveness', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');

    const response = await GET(new Request('http://localhost/api/health', { headers: { 'X-Probe': 'liveness' } }));

    expect(response.status).toBe(200);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('returns readiness only when the gateway is healthy', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{}', { status: 200 }));

    const response = await GET(new Request('http://localhost/api/health?probe=readiness'));
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toMatchObject({ status: 'healthy', version: '1.5.10-test' });
    expect(fetchMock).toHaveBeenCalledWith('http://gateway-service:8080/health', expect.objectContaining({ signal: expect.any(AbortSignal) }));
  });

  it('returns 503 when the gateway is unavailable', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('connection refused'));

    const response = await GET(new Request('http://localhost/api/health'));
    const body = await response.json();

    expect(response.status).toBe(503);
    expect(body).toMatchObject({ status: 'unhealthy', version: '1.5.10-test' });
  });
});
