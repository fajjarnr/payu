import { NextRequest, NextResponse } from 'next/server';
import createMiddleware from 'next-intl/middleware';
import { locales, defaultLocale } from './i18n/config';

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
    return NextResponse.redirect(new URL(`${locale}/dashboard`, request.url));
  }

  // 2. Protect /dashboard and related financial routes
  const protectedRoutes = [
    '/dashboard',
    '/cards',
    '/investments',
    '/pockets',
    '/transfer',
    '/qris',
    '/analytics',
    '/accounts',
    '/rewards'
  ];

  const isProtectedRoute = protectedRoutes.some(route => pathWithoutLocale.startsWith(route));

  if (isProtectedRoute && !hasSession) {
    const localeMatch = pathname.match(/^\/(en|id)/);
    const locale = localeMatch ? localeMatch[0] : '';
    // Redirect to login, ensuring user doesn't bypass auth
    const loginUrl = new URL(`${locale}/login`, request.url);
    // Optional: add callback URL for better UX
    loginUrl.searchParams.set('callbackUrl', pathname);
    return NextResponse.redirect(loginUrl);
  }

  return intlMiddleware(request);
}

export const config = {
  // Catch all paths that should be localized, 
  // but ignore internals like api, _next, and files with extensions
  matcher: ['/((?!api|_next|_static|_vercel|.*\\..*).*)']
};
