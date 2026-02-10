import { cookies } from 'next/headers';
import { NextRequest, NextResponse } from 'next/server';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://gateway-service:8080';

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
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get('accessToken')?.value;

    const { path } = await params;
    const backendPath = path.join('/');
    const url = new URL(`/api/v1/${backendPath}`, GATEWAY_URL);

    // Forward query parameters
    request.nextUrl.searchParams.forEach((v: string, k: string) => url.searchParams.set(k, v));

    // Build upstream headers
    const headers: HeadersInit = {
      'X-Correlation-Id':
        request.headers.get('x-correlation-id') || crypto.randomUUID(),
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const contentType = request.headers.get('content-type');
    if (contentType) {
      headers['Content-Type'] = contentType;
    }

    // Forward accept header for content negotiation
    const accept = request.headers.get('accept');
    if (accept) {
      headers['Accept'] = accept;
    }

    const body =
      request.method === 'GET' || request.method === 'HEAD'
        ? undefined
        : await request.text();

    const res = await fetch(url.toString(), {
      method: request.method,
      headers,
      body,
    });

    const responseBody = await res.text();

    return new NextResponse(responseBody, {
      status: res.status,
      headers: {
        'Content-Type': res.headers.get('Content-Type') || 'application/json',
      },
    });
  } catch (error) {
    // Graceful fallback when gateway is unreachable.
    // GET requests return an empty payload so the UI renders with defaults.
    // Mutating methods still surface the 503 so users know the write failed.
    if (request.method === 'GET' || request.method === 'HEAD') {
      console.warn('[BFF] Gateway offline – returning empty payload for', request.method, request.nextUrl.pathname);
      return NextResponse.json(
        { data: null, items: [], total: 0, _fallback: true },
        {
          status: 200,
          headers: { 'X-Fallback': 'gateway-offline' },
        },
      );
    }

    console.error('[BFF] Proxy error:', error);
    return NextResponse.json(
      { error: 'Service unavailable' },
      { status: 503 },
    );
  }
}

export const GET = proxyRequest;
export const POST = proxyRequest;
export const PUT = proxyRequest;
export const DELETE = proxyRequest;
export const PATCH = proxyRequest;
