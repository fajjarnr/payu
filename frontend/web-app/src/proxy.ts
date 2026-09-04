import { NextRequest, NextResponse } from 'next/server';
import createMiddleware from 'next-intl/middleware';
import { locales, defaultLocale } from './i18n/config';
import { edgeLogger } from './lib/edge-logger';

function safeRandomUUID(): string {
  try {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  } catch {}
  return `${Date.now().toString(36)}-${Math.random().toString(36).substring(2, 10)}-${Math.random().toString(36).substring(2, 10)}`;
}

const intlMiddleware = createMiddleware({
  locales,
  defaultLocale,
  localePrefix: 'as-needed',
  localeDetection: false
});

// Dynamically build locale pattern from config instead of hardcoding
const localePattern = new RegExp(`^/(${locales.join('|')})`);

const publicRoutes = [
  '/login',
  '/forgot-password',
  '/onboarding',
  '/legal/privacy',
  '/legal/terms',
  '/merchant/register',
];

const protectedRoutePrefixes = [
  '/analytics',
  '/backoffice',
  '/bills',
  '/cards',
  '/dashboard',
  '/exchange',
  '/investments',
  '/lending',
  '/merchant',
  '/notifications',
  '/pockets',
  '/qris',
  '/rewards',
  '/scheduled-transfers',
  '/security',
  '/settings',
  '/split-bill',
  '/support',
  '/transactions',
  '/transfer',
];

function isProtectedPath(pathname: string): boolean {
  return protectedRoutePrefixes.some(route =>
    pathname === route || pathname.startsWith(`${route}/`),
  );
}

type ValidationResult = { valid: boolean; transient: boolean };

async function validateAccessToken(token: string): Promise<ValidationResult> {
  const gatewayUrl = process.env.GATEWAY_URL || 'http://gateway-service:8080';
  try {
    const response = await fetch(new URL('/api/v1/auth/validate', gatewayUrl), {
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
      cache: 'no-store',
      signal: AbortSignal.timeout(5_000),
    });
    // A definitive 401/403 means the token is rejected. Any other response
    // (200, or even 5xx from the gateway) is not a conclusive rejection.
    if (response.status === 401 || response.status === 403) {
      return { valid: false, transient: false };
    }
    return { valid: response.ok, transient: !response.ok };
  } catch (error) {
    // Transient network/timeout failure — do NOT force-logout an active user.
    edgeLogger.warn('Session validation request failed (transient)', {
      action: 'middleware',
      error: error instanceof Error ? error.message : String(error),
    });
    return { valid: true, transient: true };
  }
}

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  // Define public vs protected paths
  // Locale-agnostic check
  const pathWithoutLocale = pathname.replace(localePattern, '') || '/';
  
  const needsSessionValidation =
    isProtectedPath(pathWithoutLocale) || pathWithoutLocale === '/' || pathWithoutLocale === '/login';
  const accessToken = request.cookies.get('accessToken')?.value;
  const hasRefreshToken = request.cookies.has('refreshToken');
  const validation = needsSessionValidation && accessToken ? await validateAccessToken(accessToken) : null;

  // FE-PROXY-AUTH-001: a transient validation failure (network timeout to the
  // gateway) must not force-logout an active user. Only a definitive 401/403
  // counts as an invalid token. On transient failure we proceed and let the BFF
  // refresh the token client-side, preserving the user's form state.
  const hasAccessToken = needsSessionValidation && accessToken ? validation!.valid : false;
  const transientAuthFailure = validation?.transient === true;

  // AUDIT-064: CSP nonce — generate per-request nonce for script-src.
  // WEB-001: Next.js injects the nonce into inline scripts only when it can
  // read `x-nonce` from the request headers during render, so propagate the
  // nonce + CSP on the request (not just the response) before next-intl runs.
  const nonce = safeRandomUUID();
  const isDev = process.env.NODE_ENV === 'development';
  const scriptSrc = isDev
    ? `'self' 'unsafe-eval' 'unsafe-inline' 'nonce-${nonce}'`
    : `'self' 'nonce-${nonce}'`;
  const csp = [
    `default-src 'self'`,
    `script-src ${scriptSrc}`,
    `style-src 'self' 'unsafe-inline'`,
    `img-src 'self' blob: data: https://cdn.payu.fajjjar.my.id https://assets.payu.fajjjar.my.id https://payu.fajjjar.my.id https://images.unsplash.com`,
    `font-src 'self'`,
    `connect-src 'self' https://cdn.payu.fajjjar.my.id https://assets.payu.fajjjar.my.id https://payu.fajjjar.my.id`,
    `frame-ancestors 'none'`,
    `base-uri 'self'`,
    `form-action 'self'`,
  ].join('; ');
  const requestHeaders = new Headers(request.headers);
  requestHeaders.set('x-nonce', nonce);
  requestHeaders.set('Content-Security-Policy', csp);
  const nextRequest = new NextRequest(request.url, {
    headers: requestHeaders,
    method: request.method,
  });

  // BUG-AUTH-012: If accessToken is missing/expired but refreshToken exists,
  // trigger a server-side refresh before proceeding. This restores the session
  // after browser restart (refreshToken is a 7-day httpOnly cookie).
  let response: NextResponse | undefined;
  let refreshSucceeded = false;
  if (!hasAccessToken && hasRefreshToken) {
    try {
      edgeLogger.info('Attempting session rehydration via refresh token', {
        action: 'middleware',
        path: pathname,
      });
      // FE-AUDIT-003: self-fetch via the public URL fails from inside the
      // pod (no egress hairpin) — every expired-access navigation dies here.
      // Middleware runs in the same Node process, so hit it via loopback.
      const localPort = process.env.PORT ?? '3000';
      const refreshUrl = new URL('/api/auth/refresh', `http://127.0.0.1:${localPort}`);
      const refreshRes = await fetch(refreshUrl.toString(), {
        method: 'POST',
        headers: {
          'Cookie': request.headers.get('cookie') || '',
        },
        signal: AbortSignal.timeout(10_000),
      });

      if (refreshRes.ok) {
        refreshSucceeded = true;
        edgeLogger.info('Session rehydrated successfully', { action: 'middleware' });
        // Forward the Set-Cookie headers from the refresh response to the client
        response = intlMiddleware(nextRequest);
        const setCookieHeaders = refreshRes.headers.getSetCookie();
        for (const cookie of setCookieHeaders) {
          response.headers.append('Set-Cookie', cookie);
        }
        // After successful refresh, treat as having a session for the checks below
      } else {
        edgeLogger.warn('Session rehydration failed — refresh token rejected', {
          action: 'middleware',
          status: refreshRes.status,
        });
      }
    } catch (err) {
      edgeLogger.error('Session rehydration error', {
        action: 'middleware',
        error: err instanceof Error ? err.message : String(err),
      });
    }
  }

  // Re-evaluate session status after potential rehydration
  const hasSession = // BUG-AUTH-014 FIX: Recalculate session after potential refresh failure
    hasAccessToken || (hasRefreshToken && refreshSucceeded) || (accessToken && transientAuthFailure);

  // 1. Auto-redirect from Landing to Dashboard if already logged in
  if (pathWithoutLocale === '/' && hasSession) {
    const localeMatch = pathname.match(localePattern);
    const locale = localeMatch ? localeMatch[0] : '';
    edgeLogger.info('Redirecting authenticated user to dashboard', {
      action: 'middleware',
      path: pathname,
    });
    const redirectRes = NextResponse.redirect(new URL(`${locale}/dashboard`, request.url));
    // BUG-AUTH-012: Carry over Set-Cookie headers from rehydration
    if (response) {
      for (const cookie of response.headers.getSetCookie()) {
        redirectRes.headers.append('Set-Cookie', cookie);
      }
    }
    return redirectRes;
  }

  // 1b. Auto-redirect from Login to Dashboard if already logged in
  if (pathWithoutLocale === '/login' && hasSession) {
    const localeMatch = pathname.match(localePattern);
    const locale = localeMatch ? localeMatch[0] : '';
    edgeLogger.info('Redirecting authenticated user away from login to dashboard', {
      action: 'middleware',
      path: pathname,
    });
    const redirectRes = NextResponse.redirect(new URL(`${locale}/dashboard`, request.url));
    if (response) {
      for (const cookie of response.headers.getSetCookie()) {
        redirectRes.headers.append('Set-Cookie', cookie);
      }
    }
    return redirectRes;
  }

  // WEB-005: only redirect known app paths to login; unknown paths fall
  // through to Next.js so they render a real 404 instead of masking it.
  // BUG-FE-046: Use exact match or segment boundary to prevent /login-debug, /onboarding-secret matching
  const isPublicRoute = pathWithoutLocale === '/' || 
    publicRoutes.some(route => pathWithoutLocale === route || pathWithoutLocale.startsWith(route + '/'));
  const isProtectedRoute =
    protectedRoutePrefixes.some(route => pathWithoutLocale === route || pathWithoutLocale.startsWith(route + '/'));

  if (!isPublicRoute && isProtectedRoute && !hasSession) {
    const localeMatch = pathname.match(localePattern);
    const locale = localeMatch ? localeMatch[0] : '';
    // Redirect to login, ensuring user doesn't bypass auth
    const loginUrl = new URL(`${locale}/login`, request.url);
    // Optional: add callback URL for better UX
    loginUrl.searchParams.set('callbackUrl', pathname);
    edgeLogger.warn('Unauthenticated access — redirecting to login', {
      action: 'middleware',
      path: pathname,
      callbackUrl: pathname,
    });
    return NextResponse.redirect(loginUrl);
  }

  // Return the response from rehydration (with Set-Cookie headers) or default intl response
  const finalResponse = response ?? intlMiddleware(nextRequest);

  finalResponse.headers.set('Content-Security-Policy', csp);
  finalResponse.headers.set('x-nonce', nonce);
  finalResponse.headers.set('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');
  finalResponse.headers.set('X-Frame-Options', 'DENY');
  finalResponse.headers.set('X-Content-Type-Options', 'nosniff');
  if (isProtectedRoute) {
    finalResponse.headers.set('Cache-Control', 'private, no-store');
  }
  // ponytail: X-Request-ID on client-side routes mirrors BFF API proxy header
  finalResponse.headers.set('X-Request-ID', safeRandomUUID());

  return finalResponse;
}

export const config = {
  // Catch all paths that should be localized, 
  // but ignore internals like api, _next, and files with extensions
  matcher: ['/((?!api|_next|_static|_vercel|.*\\..*).*)']
};
