import type { MetadataRoute } from 'next';
import { headers } from 'next/headers';

// WEB-004: dynamic rendering so the runtime base URL is used (see sitemap.ts).
export default async function robots(): Promise<MetadataRoute.Robots> {
  try {
    headers();
  } catch {
    // Static generation / unit tests: no request context available.
  }
  const baseUrl = process.env.NEXT_PUBLIC_BASE_URL || 'https://payu.fajjjar.my.id';

  return {
    rules: [
      {
        userAgent: '*',
        allow: '/',
        disallow: ['/api/', '/backoffice/', '/onboarding/'],
      },
    ],
    sitemap: `${baseUrl}/sitemap.xml`,
  };
}
