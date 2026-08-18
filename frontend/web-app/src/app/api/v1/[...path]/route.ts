import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';
import { getCorrelationId, withCorrelation } from '@/lib/logger';

const DEFAULT_GATEWAY_URL = 'http://gateway-service:8080';
const MAX_BODY_BYTES = 10_485_760; // 10 MiB for base64 KYC documents
const UPSTREAM_TIMEOUT_MS = 10_000;

class RequestBodyTooLargeError extends Error {}

function getGatewayUrl(): string {
  const configuredUrl = process.env.GATEWAY_URL?.trim();
  if (configuredUrl) return configuredUrl;
  if (process.env.NODE_ENV === 'development' || process.env.NODE_ENV === 'test') {
    return DEFAULT_GATEWAY_URL;
  }
  throw new Error('GATEWAY_URL must be configured outside development and test');
}

function upstreamSignal(): AbortSignal {
  return AbortSignal.timeout(UPSTREAM_TIMEOUT_MS);
}

async function readRequestBody(request: Request): Promise<string | undefined> {
  if (request.method === 'GET' || request.method === 'HEAD') {
    return undefined;
  }

  const declaredLength = Number(request.headers.get('content-length'));
  if (Number.isFinite(declaredLength) && declaredLength > MAX_BODY_BYTES) {
    throw new RequestBodyTooLargeError();
  }

  if (!request.body) {
    if (typeof request.text !== 'function') return undefined;
    const body = await request.text();
    if (new TextEncoder().encode(body).byteLength > MAX_BODY_BYTES) {
      throw new RequestBodyTooLargeError();
    }
    return body || undefined;
  }

  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let totalBytes = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      totalBytes += value.byteLength;
      if (totalBytes > MAX_BODY_BYTES) {
        await reader.cancel();
        throw new RequestBodyTooLargeError();
      }
      chunks.push(value);
    }
  } finally {
    reader.releaseLock();
  }

  if (totalBytes === 0) return undefined;
  const bodyBytes = new Uint8Array(totalBytes);
  let offset = 0;
  for (const chunk of chunks) {
    bodyBytes.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return new TextDecoder().decode(bodyBytes);
}

const SECURITY_HEADERS = {
  'Strict-Transport-Security': 'max-age=31536000; includeSubDomains; preload',
  'Content-Security-Policy': "default-src 'none'",
  'X-Frame-Options': 'DENY',
  'X-Content-Type-Options': 'nosniff',
};

function getSecurityHeaders(correlationId: string): Record<string, string> {
  return {
    ...SECURITY_HEADERS,
    'X-Request-ID': correlationId,
  };
}

/**
 * Whitelist of allowed API path prefixes for SSRF prevention.
 * Only paths starting with these prefixes will be proxied to the backend.
 */
const ALLOWED_PATH_PREFIXES = [
  '/api/v1/accounts',
  '/api/v1/analytics',
  '/api/v1/auth',
  '/api/v1/billing',
  '/api/v1/billers',
  '/api/v1/biometric',
  '/api/v1/cards',
  '/api/v1/cashbacks',
  '/api/v1/cms',
  '/api/v1/compliance',
  '/api/v1/contents',
  '/api/v1/disbursements',
  '/api/v1/disputes',
  '/api/v1/escrow',
  '/api/v1/fx',
  '/api/v1/gamification',
  '/api/v1/integration',
  '/api/v1/investments',
  '/api/v1/lending',
  '/api/v1/loyalty-points',
  '/api/v1/notifications',
  '/api/v1/partners',
  '/api/v1/partner',
  '/api/v1/payments',
  '/api/v1/pockets',
  '/api/v1/products',
  '/api/v1/promotions',
  '/api/v1/public/contents',
  '/api/v1/qris',
  '/api/v1/referrals',
  '/api/v1/rewards',
  '/api/v1/scheduled-transfers',
  '/api/v1/settlements',
  '/api/v1/smart-routing',
  '/api/v1/split-bills',
  '/api/v1/statements',
  '/api/v1/support',
  '/api/v1/topup',
  '/api/v1/transactions',
  '/api/v1/users',
  '/api/v1/wallets',
  '/api/v1/kyc',
  '/api/v1/backoffice',
  '/api/v1/health',
];

/**
 * Validates and sanitizes the backend path to prevent SSRF attacks.
 *
 * Security checks:
 * 1. Rejects paths containing '..' (path traversal)
 * 2. Rejects absolute paths (starting with '/')
 * 3. Rejects empty path segments
 * 4. Validates against allowed prefixes whitelist
 *
 * @param pathSegments - Array of path segments from the URL
 * @returns Sanitized path string
 * @throws Error if path is invalid or potentially malicious
 */
function sanitizeBackendPath(pathSegments: string[]): string {
  // Check for empty path
  if (!pathSegments || pathSegments.length === 0) {
    throw new Error('Path is required');
  }

  // Validate each segment
  for (const segment of pathSegments) {
    // Reject empty segments
    if (!segment || segment.length === 0) {
      throw new Error('Invalid path: empty segment');
    }

    // Reject path traversal attempts (..)
    if (segment === '..' || segment.includes('..')) {
      throw new Error('Invalid path: path traversal detected');
    }

    // Reject absolute paths (starting with /)
    if (segment.startsWith('/')) {
      throw new Error('Invalid path: absolute path not allowed');
    }

    // Reject null bytes and control characters
    if (/[\x00-\x1f\x7f]/.test(segment)) {
      throw new Error('Invalid path: control characters detected');
    }

    // Reject URL-encoded traversal attempts
    if (segment.includes('%2e') || segment.includes('%2E') ||
        segment.includes('%2f') || segment.includes('%2F') ||
        segment.includes('%5c') || segment.includes('%5C')) {
      throw new Error('Invalid path: encoded traversal detected');
    }
  }

  const backendPath = pathSegments.join('/');

  // Validate against whitelist
  const fullPath = `/api/v1/${backendPath}`;
  const isAllowed = ALLOWED_PATH_PREFIXES.some(prefix =>
    fullPath.startsWith(prefix + '/') || fullPath === prefix
  );

  if (!isAllowed) {
    throw new Error(`Invalid path: '${fullPath}' is not in allowed whitelist`);
  }

  return backendPath;
}

/**
 * BFF Catch-All API Proxy — Forwards authenticated requests to the gateway.
 *
 * Flow:
 *   Browser  →  GET /api/v1/wallets (cookie sent automatically)
 *   This route reads the httpOnly cookie, converts it to a Bearer header,
 *   then forwards to the gateway.  The raw JWT is never exposed to JS.
 *
 * Public endpoints still work — if no cookie is present the request is
 * forwarded without an Authorization header (gateway decides 401 vs 200).
 */
async function proxyRequest(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const correlationId = getCorrelationId(request);
  const log = withCorrelation(correlationId);
  const startTime = Date.now();

  try {
    const gatewayUrl = getGatewayUrl();
    const cookieStore = await cookies();
    const token = cookieStore.get('accessToken')?.value;

    const { path } = await params;

    // SSRF Prevention: Sanitize and validate path before use
    let backendPath: string;
    try {
      backendPath = sanitizeBackendPath(path);
    } catch (sanitizeError) {
      log.warn({
        action: 'proxy',
        method: request.method,
        path: path.join('/'),
        error: sanitizeError instanceof Error ? sanitizeError.message : 'Unknown error',
      }, 'SSRF prevention: blocked invalid path');

      return NextResponse.json(
        { error: 'Bad Request', message: 'Invalid path' },
        { 
          status: 400,
          headers: getSecurityHeaders(correlationId),
        },
      );
    }

    const url = new URL(`/api/v1/${backendPath}`, gatewayUrl);

    // Forward query parameters
    request.nextUrl.searchParams.forEach((v: string, k: string) => url.searchParams.set(k, v));

    log.info({ action: 'proxy', method: request.method, path: `/api/v1/${backendPath}`, hasAuth: !!token }, 'Proxy request');

    // Build upstream headers
    const headers: Record<string, string> = {
      'X-Correlation-Id': correlationId,
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    // BUG-READY-070 (was E2E-2026-06-13-12): For a body-less POST (e.g.
    // POST /cards/{id}/freeze, /cards/{id}/unfreeze, /cancel, /archive)
    // the browser still sends `Content-Type: application/json` with an
    // empty body. Forwarding that Content-Type verbatim causes the
    // gateway to return 415 Unsupported Media Type. We now read the
    // body FIRST, then forward Content-Type only when the body is
    // non-empty.
    const rawBody = await readRequestBody(request);
    const body = rawBody && rawBody.length > 0 ? rawBody : undefined;

    const contentType = request.headers.get('content-type');
    if (contentType && body !== undefined) {
      headers['Content-Type'] = contentType;
    }

    // Forward accept header for content negotiation
    const accept = request.headers.get('accept');
    if (accept) {
      headers['Accept'] = accept;
    }

    // Forward API version header if present (OCP-010)
    const apiVersion = request.headers.get('accept-version');
    if (apiVersion) {
      headers['Accept-Version'] = apiVersion;
    }

    // BUG-FE-029/085: Forward only explicitly allowed security/custom headers
    // BUG-FE-085: Removed catch-all `lowerKey.startsWith('x-')` to prevent
    // leaking internal headers (e.g. x-forwarded-for, x-real-ip) to the gateway.
    const allowedHeaders = ['x-idempotency-key', 'x-device-id', 'x-client-version', 'x-signature', 'x-timestamp'];
    request.headers.forEach((value, key) => {
      const lowerKey = key.toLowerCase();
      if (allowedHeaders.includes(lowerKey)) {
        // Skip already added X-Correlation-Id
        if (lowerKey !== 'x-correlation-id') {
          headers[key] = value;
        }
      }
    });

    const res = await fetch(url.toString(), {
      method: request.method,
      headers,
      body,
      signal: upstreamSignal(),
    });

    // BUG-FE-001: Auto-retry on 401 by refreshing the access token via BFF
    if (res.status === 401 && token) {
      log.info({ action: 'proxy', path: `/api/v1/${backendPath}` }, 'Got 401 — attempting token refresh and retry');
      try {
        const refreshRes = await fetch(new URL('/api/auth/refresh', request.nextUrl.origin).toString(), {
          method: 'POST',
          headers: { Cookie: request.headers.get('cookie') || '' },
          signal: upstreamSignal(),
        });
        if (refreshRes.ok) {
          const setCookieHeaders = refreshRes.headers.getSetCookie();
          let newToken = '';
          for (const cookie of setCookieHeaders) {
            if (cookie.startsWith('accessToken=')) {
              newToken = cookie.split(';')[0].split('=')[1];
            }
          }
          if (newToken) {
            headers['Authorization'] = `Bearer ${newToken}`;
            const retryRes = await fetch(url.toString(), {
              method: request.method,
              headers,
              body,
              signal: upstreamSignal(),
            });
            const retryBody = await retryRes.text();
            
            const responseHeaders = new Headers();
            responseHeaders.set('Content-Type', retryRes.headers.get('Content-Type') || 'application/json');
            responseHeaders.set('Cache-Control', 'private, no-cache, no-store, must-revalidate');
            for (const [k, v] of Object.entries(getSecurityHeaders(correlationId))) {
              responseHeaders.set(k, v);
            }
            for (const cookie of setCookieHeaders) {
              responseHeaders.append('Set-Cookie', cookie);
            }
            
            log.info({ action: 'proxy', path: `/api/v1/${backendPath}`, status: retryRes.status, durationMs: Date.now() - startTime }, 'Proxy retry response after refresh');
            return new NextResponse(retryBody, {
              status: retryRes.status,
              headers: responseHeaders,
            });
          }
        }
      } catch (refreshError) {
        log.warn({ action: 'proxy', err: refreshError instanceof Error ? refreshError : { message: String(refreshError) } }, 'Token refresh during proxy retry failed');
      }
    }

    const responseBody = await res.text();

    log.info({ action: 'proxy', method: request.method, path: `/api/v1/${backendPath}`, status: res.status, durationMs: Date.now() - startTime }, 'Proxy response');

    return new NextResponse(responseBody, {
      status: res.status,
      headers: {
        'Content-Type': res.headers.get('Content-Type') || 'application/json',
        'Cache-Control': 'private, no-cache, no-store, must-revalidate',
        ...getSecurityHeaders(correlationId),
      },
    });
  } catch (error) {
    if (error instanceof RequestBodyTooLargeError) {
      return NextResponse.json(
        { error: 'Payload Too Large', message: 'Request body exceeds 1 MiB limit' },
        { status: 413, headers: getSecurityHeaders(correlationId) },
      );
    }
    // Graceful fallback when gateway is unreachable.
    // All requests return 503 error so the UI can properly handle error states.
    // The _fallback flag allows FE to distinguish gateway offline vs other errors.
    log.warn({ action: 'proxy', method: request.method, path: request.nextUrl.pathname, err: error instanceof Error ? error : { message: String(error) }, durationMs: Date.now() - startTime }, 'Gateway offline — returning 503 error');
    return NextResponse.json(
      { error: true, _fallback: true, message: 'Service unavailable' },
      {
        status: 503,
        headers: { 
          'X-Fallback': 'gateway-offline',
          ...getSecurityHeaders(correlationId),
        },
      },
    );
  }
}

export const GET = proxyRequest;
export const POST = proxyRequest;
export const PUT = proxyRequest;
export const DELETE = proxyRequest;
export const PATCH = proxyRequest;
