import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Analitik Keuangan | PayU Digital Banking',
  description: 'Analisis pengeluaran, pemasukan, dan tren keuangan Anda secara real-time.',
};

export default function AnalyticsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
