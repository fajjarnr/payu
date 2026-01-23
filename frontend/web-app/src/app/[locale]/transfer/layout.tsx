import type { Metadata } from "next";

export const metadata: Metadata = {
 title: "Transfer Instan",
 description: "Kirim uang dengan aman kepada siapa saja, di mana saja dengan berbagai metode transfer.",
 robots: {
  index: false,
  follow: false,
 },
};

export default function TransferPageLayout({
 children,
}: {
 children: React.ReactNode;
}) {
 return children;
}
