import type { NextConfig } from "next";
import createNextIntlPlugin from 'next-intl/plugin';

const withNextIntl = createNextIntlPlugin();

const nextConfig: NextConfig = {
  output: 'standalone',
  serverExternalPackages: ['pino', 'pino-pretty'],
  // Fix Next 16 + isomorphic-dompurify@3.3.0 ESM/CommonJS interop bug:
  // html-encoding-sniffer uses require() on @exodus/bytes/encoding-lite.js
  // which is now a pure ESM module. Transpile the chain so webpack handles it.
  transpilePackages: ['isomorphic-dompurify', 'html-encoding-sniffer', '@exodus/bytes'],
  // Gateway rewrite REMOVED — BFF proxy at /api/v1/[...path] handles forwarding
  // with httpOnly cookie → Bearer token conversion (P0-SEC-001)
  experimental: {
    serverActions: {
      allowedOrigins: [
        'payu.fajjjar.my.id',
        '*.payu.fajjjar.my.id',
        'payu.ocp.fajjjar.my.id',
        '*.payu.ocp.fajjjar.my.id',
        'ocp.fajjjar.my.id',
        '*.ocp.fajjjar.my.id',
        'payu.id',
        '*.payu.id',
        'payu.co.id',
        '*.payu.co.id',
        'localhost:3000'
      ]
    }
  },
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'cdn.payu.fajjjar.my.id',
      },
      {
        protocol: 'https',
        hostname: 'assets.payu.fajjjar.my.id',
      },
      {
        protocol: 'https',
        hostname: 'payu.fajjjar.my.id',
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
  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          // AUDIT-064: CSP moved to middleware.ts for per-request nonce support
          {
            key: 'X-Frame-Options',
            value: 'DENY'
          },
          {
            key: 'X-Content-Type-Options',
            value: 'nosniff'
          },
          {
            key: 'Strict-Transport-Security',
            value: 'max-age=31536000; includeSubDomains; preload'
          },
          {
            key: 'Referrer-Policy',
            value: 'strict-origin-when-cross-origin'
          },
          {
            key: 'Permissions-Policy',
            value: 'camera=(), microphone=(), geolocation=(), interest-cohort=()'
          },
          {
            key: 'X-XSS-Protection',
            value: '1; mode=block'
          }
        ]
      }
    ];
  }
};

export default withNextIntl(nextConfig);
