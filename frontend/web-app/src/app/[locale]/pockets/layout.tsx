import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Pocket (Tabungan Terpisah) | PayU Digital Banking',
  description: 'Buat dan kelola pocket untuk mengatur keuangan berdasarkan tujuan tabungan Anda.',
};

export default function PocketsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
