import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import TransferActivity from "@/components/dashboard/TransferActivity";
import { renderWithIntl } from "@/__tests__/utils/test-utils";
import type { Transaction } from "@/services/TransactionService";

const txState: {
  data: Transaction[];
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
} = { data: [], isLoading: false, isError: false, refetch: vi.fn() };

vi.mock("@/hooks", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/hooks")>();
  return {
    ...actual,
    useTransactions: () => txState,
    useCancelTransaction: () => ({ mutateAsync: vi.fn(async () => {}) }),
  };
});
vi.mock("@/stores/authStore", () => ({
  useAuthStore: (selector: (s: { accountId: string | null }) => unknown) =>
    selector({ accountId: "acc-mine" }),
}));

const inboundTransfer = {
  id: "tx-inbound",
  type: "INTERNAL_TRANSFER",
  description: "Kirim dari Budi",
  amount: 250000,
  status: "COMPLETED",
  createdAt: new Date("2026-10-01T10:00:00Z").toISOString(),
  referenceNumber: "REF-INBOUND-001",
  senderAccountId: "acc-other",
  recipientAccountId: "acc-mine",
} as unknown as Transaction;

describe("TransferActivity credit classifier", () => {
  beforeEach(() => {
    txState.data = [];
    txState.isLoading = false;
    txState.isError = false;
    vi.clearAllMocks();
  });

  it("signs an inbound transfer as credit, not just TOP_UP", () => {
    txState.data = [inboundTransfer];
    const { container } = renderWithIntl(<TransferActivity />);
    const row = container.querySelector(
      '[data-testid="transfer-row-tx-inbound"]',
    );
    expect(row?.textContent).toMatch(/\+Rp/);
    expect(row?.textContent).not.toMatch(/-Rp/);
  });

  it("hides the full reference number from the row", () => {
    txState.data = [inboundTransfer];
    renderWithIntl(<TransferActivity />);
    expect(screen.queryByText("REF-INBOUND-001")).not.toBeInTheDocument();
  });

  it("names the mobile cancel button with description and amount", () => {
    txState.data = [
      { ...inboundTransfer, status: "PENDING" } as Transaction,
    ];
    renderWithIntl(<TransferActivity />);
    const cancel = screen.getByRole("button", {
      name: /Batalkan Kirim dari Budi/,
    });
    expect(cancel.getAttribute("aria-label")).toMatch(/250/);
  });

  it("mobile amounts carry a color cue, matching desktop", () => {
    txState.data = [inboundTransfer];
    const { container } = renderWithIntl(<TransferActivity />);
    const card = container.querySelector(
      '[data-testid="transfer-card-mobile-tx-inbound"]',
    );
    const amount = Array.from(card?.querySelectorAll("span") ?? []).find(
      (el) => el.textContent?.includes("250"),
    );
    expect(amount?.style.color).toContain("success-text-active");
  });
});
