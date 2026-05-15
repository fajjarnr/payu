import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Investasi | PayU Digital Banking',
  description: 'Kelola portofolio investasi emas dan reksa dana Anda. Pantau performa dan lakukan transaksi.',
};

export default function InvestmentsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
