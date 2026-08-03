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

    const gatewayHealth = await fetch(`${gatewayUrl.replace(/\/$/, '')}/health`, {
      signal: AbortSignal.timeout(DEPENDENCY_TIMEOUT_MS),
      headers: { Accept: 'application/json' },
      cache: 'no-store',
    });
    if (!gatewayHealth.ok) throw new Error(`gateway health returned ${gatewayHealth.status}`);

    return response('healthy', 200, { dependencies: { gateway: 'UP' } });
  } catch (error) {
    logger.error({ action: 'health', err: error instanceof Error ? error : { message: String(error) } }, 'Health check failed');
    return response('unhealthy', 503, { error: 'Service unavailable', dependencies: { gateway: 'DOWN' } });
  }
}
