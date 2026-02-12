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

export default function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  // Define public vs protected paths
  // Locale-agnostic check
  const pathWithoutLocale = pathname.replace(/^\/(en|id)/, '') || '/';
  
  // Authentication check - verify existence of session tokens
  // These are httpOnly cookies set by the backend
  const hasSession = request.cookies.has('accessToken') || 
                     request.cookies.has('refreshToken') || 
                     request.cookies.has('payu_session');

  // 1. Auto-redirect from Landing to Dashboard if already logged in
  if (pathWithoutLocale === '/' && hasSession) {
    const localeMatch = pathname.match(/^\/(en|id)/);
    const locale = localeMatch ? localeMatch[0] : '';
    edgeLogger.info('Redirecting authenticated user to dashboard', {
      action: 'middleware',
      path: pathname,
    });
    return NextResponse.redirect(new URL(`${locale}/dashboard`, request.url));
  }

  // 2. Protect authenticated routes — everything except public pages
  const publicRoutes = [
    '/login',
    '/onboarding',
    '/legal/privacy',
    '/legal/terms',
    '/merchant/register',
  ];

  const isPublicRoute = pathWithoutLocale === '/' || 
    publicRoutes.some(route => pathWithoutLocale.startsWith(route));

  if (!isPublicRoute && !hasSession) {
    const localeMatch = pathname.match(/^\/(en|id)/);
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

  return intlMiddleware(request);
}

export const config = {
  // Catch all paths that should be localized, 
  // but ignore internals like api, _next, and files with extensions
  matcher: ['/((?!api|_next|_static|_vercel|.*\\..*).*)']
};
