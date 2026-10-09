import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { UserEvent } from "@testing-library/user-event";
import "@testing-library/jest-dom";
import SplitBillPage from "@/app/[locale]/split-bill/page";
import { notify } from "@/lib/notify";

const createSplitBillMutate = vi.fn();
const splitBillsData = vi.fn();

vi.mock("@/components/DashboardLayout", () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

vi.mock("@/stores/authStore", () => ({
  useAuthStore: (selector?: (s: Record<string, unknown>) => unknown) => {
    const state = {
      user: { id: "user_1", username: "budi" },
      accountId: "acct_creator",
      isAuthenticated: true,
    };
    return selector ? selector(state) : state;
  },
}));

vi.mock("@/lib/notify", () => ({
  notify: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
}));

// The barrel re-exports this module, so mocking it covers the page's import.
vi.mock("@/hooks/useSplitBill", () => ({
  useSplitBills: () => ({ data: splitBillsData(), isLoading: false }),
  useSplitBill: () => ({ data: null, isLoading: false }),
  useCreateSplitBill: () => ({
    mutate: createSplitBillMutate,
    isPending: false,
  }),
  useAcceptSplitBill: () => ({ mutateAsync: vi.fn() }),
  useDeclineSplitBill: () => ({ mutateAsync: vi.fn() }),
  useSplitBillPayment: () => ({ mutateAsync: vi.fn() }),
  useSettleSplitBill: () => ({ mutateAsync: vi.fn() }),
  useAddParticipant: () => ({ mutateAsync: vi.fn() }),
  useActivateSplitBill: () => ({ mutateAsync: vi.fn() }),
}));

vi.mock("@/services/TransactionService", () => ({
  SplitBillParticipant: {},
}));

async function openCreateForm(user: UserEvent) {
  await user.click(screen.getByRole("button", { name: /Split Bill Baru/ }));
}

describe("SplitBillPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    splitBillsData.mockReturnValue([]);
  });

  it("warns instead of silently returning when the description is missing", async () => {
    const user = userEvent.setup();
    render(<SplitBillPage />);
    await openCreateForm(user);

    await user.click(screen.getByRole("button", { name: "Buat" }));

    expect(notify.warning).toHaveBeenCalledWith(
      "Masukkan nama/deskripsi split bill",
    );
    expect(createSplitBillMutate).not.toHaveBeenCalled();
  });

  it("warns when the total amount is missing", async () => {
    const user = userEvent.setup();
    render(<SplitBillPage />);
    await openCreateForm(user);

    await user.type(screen.getByLabelText("Deskripsi"), "Makan siang");
    await user.click(screen.getByRole("button", { name: "Buat" }));

    expect(notify.warning).toHaveBeenCalledWith("Masukkan total tagihan");
    expect(createSplitBillMutate).not.toHaveBeenCalled();
  });

  it("warns when no participant row is complete", async () => {
    const user = userEvent.setup();
    render(<SplitBillPage />);
    await openCreateForm(user);

    await user.type(screen.getByLabelText("Deskripsi"), "Makan siang");
    await user.type(screen.getByLabelText("Total Tagihan"), "150000");
    await user.click(screen.getByRole("button", { name: "Buat" }));

    expect(notify.warning).toHaveBeenCalledWith(
      "Tambahkan minimal satu peserta dengan ID akun, nomor rekening, dan nama",
    );
    expect(createSplitBillMutate).not.toHaveBeenCalled();
  });

  it("creates the bill once description, amount and a participant are valid", async () => {
    const user = userEvent.setup();
    render(<SplitBillPage />);
    await openCreateForm(user);

    await user.type(screen.getByLabelText("Deskripsi"), "Makan siang");
    await user.type(screen.getByLabelText("Total Tagihan"), "150000");
    await user.type(screen.getByLabelText("ID akun peserta 1"), "acct_2");
    await user.type(
      screen.getByLabelText("Nomor rekening peserta 1"),
      "1001001002",
    );
    await user.type(screen.getByLabelText("Nama peserta 1"), "Budi");
    await user.click(screen.getByRole("button", { name: "Buat" }));

    expect(notify.warning).not.toHaveBeenCalled();
    await waitFor(() => expect(createSplitBillMutate).toHaveBeenCalled());
    const [request] = createSplitBillMutate.mock.calls[0];
    expect(request.title).toBe("Makan siang");
    expect(request.totalAmount).toBe("150000");
  });

  it("shows DRAFT bills instead of hiding them from the list (FE-SPLIT-001)", async () => {
    splitBillsData.mockReturnValue([
      {
        id: "bill_1",
        description: "Audit Split Bill",
        totalAmount: "300000",
        currency: "IDR",
        status: "DRAFT",
        createdAt: new Date().toISOString(),
        participants: [],
      },
    ]);

    render(<SplitBillPage />);

    expect(await screen.findByText("Audit Split Bill")).toBeInTheDocument();
    expect(screen.getByText("Draf")).toBeInTheDocument();
  });

  it("should render within DashboardLayout", () => {
    render(<SplitBillPage />);
    expect(screen.getByTestId("dashboard-layout")).toBeInTheDocument();
  });

  it("should render page title", () => {
    render(<SplitBillPage />);
    expect(screen.getByText("Split Bill")).toBeInTheDocument();
  });

  it("should render create split bill button", () => {
    render(<SplitBillPage />);
    expect(screen.getByText("Split Bill Baru")).toBeInTheDocument();
  });

  it("should submit a split bill with non-empty participants (FE-SPLIT-001)", async () => {
    const user = userEvent.setup();
    render(<SplitBillPage />);
    await openCreateForm(user);

    await user.type(screen.getByLabelText("ID akun peserta 1"), "acct_p1");
    await user.type(
      screen.getByLabelText("Nomor rekening peserta 1"),
      "1001001",
    );
    await user.type(screen.getByLabelText("Nama peserta 1"), "Andi");
    await user.click(screen.getByRole("button", { name: /Tambah Peserta/ }));
    await user.type(screen.getByLabelText("ID akun peserta 2"), "acct_p2");
    await user.type(
      screen.getByLabelText("Nomor rekening peserta 2"),
      "1001002",
    );
    await user.type(screen.getByLabelText("Nama peserta 2"), "Budi");
    await user.type(
      screen.getByPlaceholderText("Makan siang, nonton bareng..."),
      "Makan siang",
    );
    await user.type(screen.getByPlaceholderText("150000"), "300000");

    await user.click(screen.getByRole("button", { name: "Buat" }));

    expect(createSplitBillMutate).toHaveBeenCalledTimes(1);
    const request = createSplitBillMutate.mock.calls[0][0];
    expect(request.title).toBe("Makan siang");
    expect(request.participants.length).toBe(2);
    expect(request.participants[0].accountId).toBe("acct_p1");
    expect(request.participants[0].accountNumber).toBe("1001001");
    expect(request.participants[0].accountName).toBe("Andi");
    expect(request.participants[0].amountOwed).toBe("150000");
    expect(request.participants[1].amountOwed).toBe("150000");
  });
});
