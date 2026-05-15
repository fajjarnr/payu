import type { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
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
