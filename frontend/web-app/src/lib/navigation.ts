import { createNavigation } from 'next-intl/navigation';
import { defineRouting } from 'next-intl/routing';
import { locales, defaultLocale } from '@/i18n/config';

export const routing = defineRouting({
  locales,
  defaultLocale,
  localePrefix: 'as-needed',
});

// Locale-aware navigation utilities
export const { Link, redirect, usePathname, useRouter } = createNavigation(routing);

/**
 * Helper function to create locale-aware href
 * Use this when you need to construct href strings programmatically
 * (e.g., for window.location.href or non-React contexts)
 */
export function createLocaleHref(path: string, locale: string): string {
  // If path already starts with locale, return as-is
  if (path.startsWith(`/${locale}/`) || path === `/${locale}`) {
    return path;
  }
  // For default locale with 'as-needed' prefix, only add locale if needed
  // next-intl will handle the actual routing
  return `/${locale}${path.startsWith('/') ? path : `/${path}`}`;
}
