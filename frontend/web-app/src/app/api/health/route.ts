import { NextResponse } from 'next/server';
import logger from '@/lib/logger';

const DEPENDENCY_TIMEOUT_MS = 3_000;

function version(): string {
  return process.env.APP_VERSION?.trim() || process.env.npm_package_version || 'unknown';
}

function response(status: 'healthy' | 'unhealthy', httpStatus: number, extra: Record<string, unknown> = {}) {
  return NextResponse.json(
    {
      status,
      timestamp: new Date().toISOString(),
      service: 'payu-web-app',
      version: version(),
      ...extra,
    },
    {
      status: httpStatus,
      headers: { 'Cache-Control': 'no-store, no-cache, must-revalidate' },
    }
  );
}

/**
 * Health check endpoint for container orchestration
 * Used by Docker HEALTHCHECK and Kubernetes readiness probes
 */
export async function GET(request: Request) {
  const probe = request.headers.get('x-probe') || new URL(request.url).searchParams.get('probe');
  if (probe === 'liveness') {
    return response('healthy', 200);
  }

  try {
    const gatewayUrl = process.env.GATEWAY_URL?.trim();
    if (!gatewayUrl) throw new Error('GATEWAY_URL is not configured');

    const gatewayHealthUrl = `${gatewayUrl.replace(/\/$/, '')}/health`;
    const gatewayHealth = await fetch(gatewayHealthUrl, {
      signal: AbortSignal.timeout(DEPENDENCY_TIMEOUT_MS),
      headers: { Accept: 'application/json' },
      cache: 'no-store',
    });
    if (!gatewayHealth.ok) {
      // Dependency down is not a code error — log at warn, not error, to avoid noisy level 50.
      logger.warn({ action: 'health', gatewayStatus: gatewayHealth.status, gatewayUrl: gatewayHealthUrl }, 'Gateway health check returned non-OK');
      return response('unhealthy', 503, { error: 'Service unavailable', dependencies: { gateway: 'DOWN' } });
    }

    return response('healthy', 200, { dependencies: { gateway: 'UP' } });
  } catch (error) {
    const msg = error instanceof Error ? error.message : String(error);
    // Network/timeout errors are expected during startup — downgrade from error to warn
    logger.warn({ action: 'health', err: { message: msg } }, 'Health check gateway unavailable');
    return response('unhealthy', 503, { error: 'Service unavailable', dependencies: { gateway: 'DOWN' } });
  }
}
