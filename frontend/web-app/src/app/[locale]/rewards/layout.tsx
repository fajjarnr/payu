import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Rewards & Loyalty | PayU Digital Banking',
  description: 'Kumpulkan poin loyalitas, cashback, dan manfaatkan program referral untuk keuntungan lebih.',
};

export default function RewardsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
