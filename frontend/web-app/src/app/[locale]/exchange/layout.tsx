import type { Metadata } from "next";
import { SilentRefreshProvider } from '@/components/auth/SilentRefreshProvider';

export const metadata: Metadata = {
  title: "FX Exchange",
  description: "Currency exchange with real-time rates. Convert between IDR, USD, EUR, SGD, JPY, GBP, AUD, and CNY.",
  robots: {
    index: false,
    follow: false,
  },
};

export default function ExchangePageLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <SilentRefreshProvider>{children}</SilentRefreshProvider>;
}
