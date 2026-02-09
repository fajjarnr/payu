import type { NextConfig } from "next";
import createNextIntlPlugin from 'next-intl/plugin';

const withNextIntl = createNextIntlPlugin();

const nextConfig: NextConfig = {
  output: 'standalone',
  // Gateway rewrite REMOVED — BFF proxy at /api/v1/[...path] handles forwarding
  // with httpOnly cookie → Bearer token conversion (P0-SEC-001)
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: '*.payu.id',
      },
      {
        protocol: 'https',
        hostname: 'cdn.payu.id',
      },
      {
        protocol: 'https',
        hostname: 'images.unsplash.com',
      },
      {
        protocol: 'https',
        hostname: 'avatars.githubusercontent.com',
      },
    ],
  },
};

export default withNextIntl(nextConfig);
