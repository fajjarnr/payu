import React from "react";
import { screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import TransferActivity from "@/components/dashboard/TransferActivity";
import { renderWithIntl } from "@/__tests__/utils/test-utils";

describe("TransferActivity", () => {
  it("renders transfer activity section", () => {
    renderWithIntl(<TransferActivity />);

    expect(screen.getByText("Aktivitas Terakhir")).toBeInTheDocument();
    expect(screen.getByText("Kirim Cepat")).toBeInTheDocument();
  });

  it("renders empty transaction state when account is unavailable", () => {
    renderWithIntl(<TransferActivity />);

    expect(screen.getAllByText("Belum Ada Transaksi").length).toBeGreaterThanOrEqual(1);
  });

  it("does not fabricate transaction amounts", () => {
    renderWithIntl(<TransferActivity />);

    expect(screen.queryByText(/Rp 7.500.000/i)).not.toBeInTheDocument();
  });

  it("renders quick transfer section with icons", () => {
    renderWithIntl(<TransferActivity />);

    expect(screen.getByText("Kategori Favorit")).toBeInTheDocument();
    expect(screen.getByText("Bank")).toBeInTheDocument();
    expect(screen.getByText("E-Wallet")).toBeInTheDocument();
    expect(screen.getByText("Tagihan")).toBeInTheDocument();
  });

  it("stacks quick-transfer strip above full-width ledger", () => {
    const { container } = renderWithIntl(<TransferActivity />);

    const section = screen.getByTestId("transfer-activity-section");
    expect(section).toHaveClass("ant-space-vertical");

    const cards = container.querySelectorAll(
      '[data-testid="quick-transfer-card"], [data-testid="recent-activity-card"]',
    );
    expect(cards).toHaveLength(2);

    const desktopTable = container.querySelector(".md\\:block");
    expect(desktopTable).toBeInTheDocument();
    const cardView = container.querySelector(".md\\:hidden");
    expect(cardView).toBeInTheDocument();
  });

  it("shows action buttons at bottom", () => {
    renderWithIntl(<TransferActivity />);

    expect(screen.getByText("Ulangi Transfer Terakhir")).toBeInTheDocument();
    expect(screen.getByText("Riwayat Lengkap")).toBeInTheDocument();
    expect(screen.getByText("Kirim Sekarang")).toBeInTheDocument();
  });

  it("does not render ambiguous numbered contacts (A-03)", () => {
    renderWithIntl(<TransferActivity />);

    expect(screen.queryByText("Kontak Terbaru")).not.toBeInTheDocument();
    expect(screen.queryAllByRole("button", { name: /Kontak \d/ })).toHaveLength(0);
  });

  it("displays transfer categories", () => {
    renderWithIntl(<TransferActivity />);

    expect(screen.getByText("Bank")).toBeInTheDocument();
    expect(screen.getByText("E-Wallet")).toBeInTheDocument();
  });
});
