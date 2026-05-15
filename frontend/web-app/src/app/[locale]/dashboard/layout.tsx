import { SilentRefreshProvider } from '@/components/auth/SilentRefreshProvider';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Dashboard | PayU Digital Banking',
  description: 'Kelola keuangan Anda dengan mudah. Lihat saldo, transaksi terbaru, dan ringkasan finansial.',
};

/**
 * Dashboard layout — Forces dynamic rendering for all dashboard routes.
 *
 * Without this, Next.js statically pre-renders the dashboard at build time
 * (x-nextjs-prerender: 1, cache-control: s-maxage=31536000).
 * This means the middleware auth check would be bypassed by any CDN or
 * proxy cache sitting in front of the app.
 *
 * `force-dynamic` ensures every request goes through the middleware
 * and the page is rendered fresh with the current auth context.
 */
export const dynamic = 'force-dynamic';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <SilentRefreshProvider>{children}</SilentRefreshProvider>;
}

