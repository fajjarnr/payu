import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Riwayat Transaksi | PayU Digital Banking',
  description: 'Lihat riwayat transaksi lengkap, filter berdasarkan tanggal, dan kelola mutasi rekening Anda.',
};

export default function TransactionsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
