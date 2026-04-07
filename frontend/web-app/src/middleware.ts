import { NextRequest, NextResponse } from 'next/server';
import createMiddleware from 'next-intl/middleware';
import { locales, defaultLocale } from './i18n/config';
import { edgeLogger } from './lib/edge-logger';

const intlMiddleware = createMiddleware({
  locales,
  defaultLocale,
  localePrefix: 'as-needed',
  localeDetection: false
});

// Dynamically build locale pattern from config instead of hardcoding
const localePattern = new RegExp(`^/(${locales.join('|')})`);

export default async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  // Define public vs protected paths
  // Locale-agnostic check
  const pathWithoutLocale = pathname.replace(localePattern, '') || '/';
  
  // Authentication check - verify existence of session tokens
  const hasAccessToken = request.cookies.has('accessToken');
  const hasRefreshToken = request.cookies.has('refreshToken');
  const hasPayuSession = request.cookies.has('payu_session');

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
      const refreshUrl = new URL('/api/auth/refresh', request.url);
      const refreshRes = await fetch(refreshUrl.toString(), {
        method: 'POST',
        headers: {
          'Cookie': request.headers.get('cookie') || '',
        },
      });

      if (refreshRes.ok) {
        refreshSucceeded = true;
        edgeLogger.info('Session rehydrated successfully', { action: 'middleware' });
        // Forward the Set-Cookie headers from the refresh response to the client
        response = intlMiddleware(request);
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
    hasAccessToken || (hasRefreshToken && refreshSucceeded) || hasPayuSession;

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

  // 2. Protect authenticated routes — everything except public pages
  const publicRoutes = [
    '/login',
    '/onboarding',
    '/legal/privacy',
    '/legal/terms',
    '/merchant/register',
  ];

  // BUG-FE-046: Use exact match or segment boundary to prevent /login-debug, /onboarding-secret matching
  const isPublicRoute = pathWithoutLocale === '/' || 
    publicRoutes.some(route => pathWithoutLocale === route || pathWithoutLocale.startsWith(route + '/'));

  if (!isPublicRoute && !hasSession) {
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
  return response ?? intlMiddleware(request);
}

export const config = {
  // Catch all paths that should be localized, 
  // but ignore internals like api, _next, and files with extensions
  matcher: ['/((?!api|_next|_static|_vercel|.*\\..*).*)']
};
