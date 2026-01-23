import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import Providers from "./providers";
import { ErrorBoundary } from "@/components/ErrorBoundary";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: {
    default: "PayU - Perbankan Digital Modern",
    template: "%s | PayU"
  },
  description: "Platform perbankan digital modern dengan solusi keuangan komprehensif untuk kebutuhan pribadi dan bisnis Anda.",
  keywords: ["perbankan digital", "transfer uang", "dompet digital", "investasi", "QRIS", "fintech Indonesia"],
  authors: [{ name: "PayU Team" }],
  creator: "PayU",
  publisher: "PayU",
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
  metadataBase: new URL(process.env.NEXT_PUBLIC_BASE_URL || 'https://payu.id'),
  openGraph: {
    type: 'website',
    locale: 'id_ID',
    url: process.env.NEXT_PUBLIC_BASE_URL || 'https://payu.id',
    title: 'PayU - Perbankan Digital Modern',
    description: 'Platform perbankan digital modern dengan solusi keuangan komprehensif.',
    siteName: 'PayU',
    images: [
      {
        url: '/og-image.png',
        width: 1200,
        height: 630,
        alt: 'PayU - Perbankan Digital',
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'PayU - Perbankan Digital Modern',
    description: 'Platform perbankan digital modern dengan solusi keuangan komprehensif.',
    images: ['/og-image.png'],
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-video-preview': -1,
      'max-image-preview': 'large',
      'max-snippet': -1,
    },
  },
  verification: {
    google: process.env.NEXT_PUBLIC_GOOGLE_SITE_VERIFICATION,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="id">
      <head>
        <link rel="icon" href="/favicon.ico" />
      </head>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased bg-background overflow-x-hidden`}
      >
        <ErrorBoundary>
          <Providers>
            {children}
          </Providers>
        </ErrorBoundary>
      </body>
    </html>
  );
}
