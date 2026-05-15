import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Pembayaran Tagihan | PayU Digital Banking',
  description: 'Bayar tagihan listrik, air, internet, dan lainnya dengan cepat dan aman.',
};

export default function BillsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
