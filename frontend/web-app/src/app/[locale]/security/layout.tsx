import type { Metadata } from "next";

export const metadata: Metadata = {
 title: "Keamanan & MFA",
 description: "Kelola pengaturan keamanan akun Anda, termasuk autentikasi dua faktor dan preferensi keamanan.",
 robots: {
  index: false,
  follow: false,
 },
};

export default function SecurityPageLayout({
 children,
}: {
 children: React.ReactNode;
}) {
 return children;
}
