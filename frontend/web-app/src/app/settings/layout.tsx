import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Pengaturan Akun",
  description: "Kelola profil pribadi, preferensi regional, dan pemicu notifikasi Anda.",
  robots: {
    index: false,
    follow: false,
  },
};

export default function SettingsPageLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
