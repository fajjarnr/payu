import { notFound } from 'next/navigation';
import { NextIntlClientProvider, useMessages } from 'next-intl';
import { getMessages, setRequestLocale } from 'next-intl/server';
import { locales } from '@/i18n/config';
import "../globals.css";
import Providers from "../providers";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { EmergencyAlert } from "@/components/cms";

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
    <html lang={locale} suppressHydrationWarning>
      <head>
        <link rel="icon" href="/favicon.ico" />
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link href="https://fonts.googleapis.com/css2?family=Google+Sans:ital,opsz,wght@0,17..18,400..700;1,17..18,400..700&display=swap" rel="stylesheet" />
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
