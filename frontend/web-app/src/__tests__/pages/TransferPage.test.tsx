import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import TransferPage from "@/app/[locale]/transfer/page";
import messages from "../../../messages/id.json";

vi.mock("@/components/DashboardLayout", () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

vi.mock("@/stores/authStore", () => ({
  useAuthStore: (selector?: (s: Record<string, unknown>) => unknown) => {
    const state = {
      accountId: "acc_123",
      user: { id: "user_1", username: "budi" },
      isAuthenticated: true,
    };
    return selector ? selector(state) : state;
  },
}));

vi.mock("@/stores/uiStore", () => ({
  useUIStore: (selector?: (s: Record<string, unknown>) => unknown) => {
    const state = { addToast: vi.fn() };
    return selector ? selector(state) : state;
  },
}));

vi.mock("@/hooks/useBeneficiaries", () => ({
  useBeneficiaries: () => ({
    data: [
      {
        id: "ben_1",
        bankCode: "PAYU",
        accountNumber: "1001001002",
        accountName: "Audit Penerima",
        nickname: "Audit Penerima",
        status: "ACTIVE",
      },
    ],
    isLoading: false,
  }),
  useCreateBeneficiary: () => ({
    mutate: (vars: unknown, opts: { onSuccess?: () => void }) =>
      opts.onSuccess?.(),
    mutateAsync: Promise.resolve(),
    isPending: false,
  }),
}));

// TRF-J-003: `settle = false` models the window between the POST leaving the
// browser and the success screen replacing the review — the confirm button is
// mounted and enabled again while the mutation is still in flight.
const transferMock = { settle: true };

const mutateMock = vi.fn((_vars: unknown, opts: { onSuccess?: () => void }) => {
  if (transferMock.settle) opts.onSuccess?.();
});

vi.mock("@/hooks/useTransactions", () => ({
  useInitiateTransfer: () => ({
    mutate: mutateMock,
    mutateAsync: vi.fn(),
    isPending: false,
  }),
}));

function renderWithIntl(ui: React.ReactNode) {
  return render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <NextIntlClientProvider locale="id" messages={messages}>
        {ui}
      </NextIntlClientProvider>
    </QueryClientProvider>,
  );
}

describe("TransferPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    transferMock.settle = true;
  });

  it("should render within DashboardLayout", () => {
    renderWithIntl(<TransferPage />);
    expect(screen.getByTestId("dashboard-layout")).toBeInTheDocument();
  });

  it("should render transfer type options", () => {
    renderWithIntl(<TransferPage />);
    const transferOptions = screen.getAllByText("Transfer Instan");
    expect(transferOptions.length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("BI-FAST").length).toBeGreaterThanOrEqual(1);
  });

  it("should render scheduling options", () => {
    renderWithIntl(<TransferPage />);
    expect(screen.getByText("Sekarang")).toBeInTheDocument();
    expect(screen.getByText("Terjadwal")).toBeInTheDocument();
    expect(screen.getByText("Berulang")).toBeInTheDocument();
  });

  it("shows the success screen after submitting from review", async () => {
    const user = userEvent.setup();
    renderWithIntl(<TransferPage />);

    await user.type(screen.getByLabelText("Akun Penerima"), "1001001002");
    const amount = screen.getByLabelText("Jumlah");
    await user.click(amount);
    await user.keyboard("25000");

    await user.click(screen.getByRole("button", { name: /Tinjau Ringkasan/ }));
    expect(screen.getByText("Audit Penerima")).toBeInTheDocument();

    await user.click(
      screen.getByRole("button", { name: /Otorisasi Transfer/ }),
    );

    expect(await screen.findByText("Transfer Berhasil!")).toBeInTheDocument();
  });

  it("fires a single transfer POST when confirm is double-clicked (TRF-J-003)", async () => {
    const user = userEvent.setup();
    // The mutation has not settled yet: the review screen is still mounted and
    // the confirm button is enabled again for a frame.
    transferMock.settle = false;

    renderWithIntl(<TransferPage />);

    await user.type(screen.getByLabelText("Akun Penerima"), "1001001002");
    const amount = screen.getByLabelText("Jumlah");
    await user.click(amount);
    await user.keyboard("25000");
    await user.click(screen.getByRole("button", { name: /Tinjau Ringkasan/ }));

    const confirm = await screen.findByTestId("confirm-transfer-button");
    // A second click inside the window where the confirm button is enabled
    // again must not fire a second transfer POST.
    await user.click(confirm);
    await user.click(confirm);

    expect(mutateMock).toHaveBeenCalledTimes(1);
  });
});
