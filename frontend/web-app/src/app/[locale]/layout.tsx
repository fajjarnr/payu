import { notFound } from 'next/navigation';
import { NextIntlClientProvider } from 'next-intl';
import { getMessages, setRequestLocale } from 'next-intl/server';
import { locales } from '@/i18n/config';
import { Inter, Outfit } from 'next/font/google';
import "../globals.css";
import Providers from "../providers";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import EmergencyAlert from "@/components/cms/EmergencyAlert";

const inter = Inter({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-inter',
});

const outfit = Outfit({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-outfit',
});

// WEB-CSP-002: per-request CSP nonces (src/proxy.ts) require dynamic rendering
// on every route — statically prerendered pages bake inline flight scripts
// WITHOUT the nonce, the CSP `script-src 'nonce-…'` directive blocks them, and
// hydration dies app-wide ("Error: Connection closed.", blank step content on
// /onboarding). Layout-level config cascades to every child page/route.
export const dynamic = 'force-dynamic';
export const dynamicParams = false;

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

export const metadata = {
  title: 'PayU Digital Banking | Masa Depan Finansial Anda',
  description: 'Platform digital banking standalone yang aman, cepat, dan transparan.',
};

export default async function RootLayout({
  children,
  params
}: Readonly<{
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}>) {
  const { locale } = await params;

  if (!locales.includes(locale as (typeof locales)[number])) {
    notFound();
  }

  // Enable static rendering
  setRequestLocale(locale);

  const messages = await getMessages({ locale });

  return (
    <html lang={locale} className={`${inter.variable} ${outfit.variable}`} suppressHydrationWarning>
      <head>
        <link rel="icon" href="/favicon.ico" />
      </head>
      <body
        className="antialiased bg-background overflow-x-hidden"
      >
        <NextIntlClientProvider messages={messages} locale={locale}>
          <ErrorBoundary>
            <Providers>
              {/* Emergency Alert Banner */}
              <EmergencyAlert />
              {children}
            </Providers>
          </ErrorBoundary>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
