import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Pinjaman | PayU Digital Banking',
  description: 'Ajukan pinjaman, pantau cicilan, dan kelola kredit Anda dengan mudah dan transparan.',
};

export default function LendingLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
