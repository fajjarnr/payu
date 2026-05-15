import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Kartu Virtual | PayU Digital Banking',
  description: 'Kelola kartu virtual Anda. Atur limit, bekukan kartu, dan kontrol keamanan transaksi.',
};

export default function CardsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
