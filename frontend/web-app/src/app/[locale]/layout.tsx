import { notFound } from 'next/navigation';
import { NextIntlClientProvider } from 'next-intl';
import { getMessages } from 'next-intl/server';
import { locales } from '@/i18n/config';
import { Inter, Geist_Mono } from "next/font/google";
import "../globals.css";
import Providers from "../providers";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { EmergencyAlert } from "@/components/cms";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  weight: ["300", "400", "500", "600", "700"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const dynamicParams = false;

export function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

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

  const messages = await getMessages();

  return (
    <html lang={locale} suppressHydrationWarning>
      <head>
        <link rel="icon" href="/favicon.ico" />
      </head>
      <body
        className={`${inter.variable} ${geistMono.variable} antialiased bg-background overflow-x-hidden`}
      >
        <NextIntlClientProvider messages={messages}>
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
