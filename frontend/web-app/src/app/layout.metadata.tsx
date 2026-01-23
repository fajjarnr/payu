import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Dashboard",
  description: "Kelola keuangan Anda dengan wawasan real-time dan fitur perbankan modern.",
  robots: {
    index: false,
    follow: false,
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
