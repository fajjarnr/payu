import type { MetadataRoute } from 'next';
import { headers } from 'next/headers';

// WEB-004: read request headers to opt into dynamic rendering so the base URL
// is taken from the runtime environment instead of being baked in at build time.
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  try {
    headers();
  } catch {
    // Static generation / unit tests: no request context available.
  }
  const baseUrl = process.env.NEXT_PUBLIC_BASE_URL || 'https://payu.fajjjar.my.id';
  const lastModified = new Date();

  // Public-facing pages that should be indexed
  const publicRoutes = [
    '',
    '/login',
    '/forgot-password',
    '/onboarding',
  ];

  // Authenticated pages (lower priority, still indexable for SEO)
  const appRoutes = [
    '/dashboard',
    '/transactions',
    '/transfer',
    '/cards',
    '/bills',
    '/rewards',
    '/investments',
    '/lending',
    '/exchange',
    '/pockets',
    '/split-bill',
    '/notifications',
    '/settings',
    '/support',
    '/analytics',
    '/scheduled-transfers',
    '/qris',
  ];

  const locales = ['id', 'en'];

  const entries: MetadataRoute.Sitemap = [];

  for (const locale of locales) {
    for (const route of publicRoutes) {
      entries.push({
        url: `${baseUrl}/${locale}${route}`,
        lastModified,
        changeFrequency: 'weekly',
        priority: route === '' ? 1.0 : 0.8,
      });
    }

    for (const route of appRoutes) {
      entries.push({
        url: `${baseUrl}/${locale}${route}`,
        lastModified,
        changeFrequency: 'daily',
        priority: 0.6,
      });
    }
  }

  return entries;
}
