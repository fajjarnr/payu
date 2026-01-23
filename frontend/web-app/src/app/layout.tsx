import type { Metadata } from "next";
import { Inter, Geist_Mono } from "next/font/google";
import "./globals.css";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  weight: ["300", "400", "500", "600", "700"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: {
    default: "PayU - Digital Banking",
    template: "%s | PayU"
  },
  description: "Modern digital banking platform with comprehensive financial solutions for your personal and business needs.",
  keywords: ["digital banking", "money transfer", "digital wallet", "investment", "QRIS", "Indonesian fintech"],
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
    title: 'PayU - Digital Banking',
    description: 'Modern digital banking platform with comprehensive financial solutions.',
    siteName: 'PayU',
    images: [
      {
        url: '/og-image.png',
        width: 1200,
        height: 630,
        alt: 'PayU - Digital Banking',
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'PayU - Digital Banking',
    description: 'Modern digital banking platform with comprehensive financial solutions.',
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
        className={`${inter.variable} ${geistMono.variable} antialiased bg-background overflow-x-hidden`}
      >
        {children}
      </body>
    </html>
  );
}
