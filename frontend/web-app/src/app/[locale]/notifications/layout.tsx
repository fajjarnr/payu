import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Notifikasi | PayU Digital Banking',
  description: 'Lihat semua notifikasi transaksi, promo, dan informasi penting akun Anda.',
};

export default function NotificationsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
