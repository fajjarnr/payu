import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Bantuan & Dukungan | PayU Digital Banking',
  description: 'Hubungi tim dukungan kami, buat tiket bantuan, atau temukan jawaban di FAQ.',
};

export default function SupportLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
