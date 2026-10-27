import { createNavigation } from 'next-intl/navigation';
import { defineRouting } from 'next-intl/routing';
import { locales, defaultLocale } from '@/i18n/config';

export const routing = defineRouting({
  locales,
  defaultLocale,
  localePrefix: 'always',
});

export const { Link, redirect, usePathname, useRouter } = createNavigation(routing);

/**
 * Helper function to create locale-aware href
 * Use this when you need to construct href strings programmatically
 * (e.g., for window.location.href or non-React contexts)
 * Single-locale app (Indonesian only): always prefixes `/id`.
 */
export function createLocaleHref(path: string, locale = 'id'): string {
  if (path.startsWith(`/${locale}/`) || path === `/${locale}`) {
    return path;
  }
  return `/${locale}${path.startsWith('/') ? path : `/${path}`}`;
}
