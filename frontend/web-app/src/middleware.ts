import createMiddleware from 'next-intl/middleware';
import { locales, defaultLocale } from './i18n/config';

export default createMiddleware({
  locales,
  defaultLocale,
  localePrefix: 'as-needed'
});

export const config = {
  // Catch all paths that should be localized, 
  // but ignore internals like api, _next, and files with extensions
  matcher: ['/((?!api|_next|_static|_vercel|.*\\..*).*)']
};
