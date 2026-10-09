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

const mutateMock = vi.fn((_vars: unknown, opts: { onSuccess?: () => void }) => {
  opts.onSuccess?.();
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

    await user.type(
      screen.getByLabelText("Nomor Rekening Penerima"),
      "1001001002",
    );
    const amount = screen.getByLabelText("Nominal Transfer");
    await user.click(amount);
    await user.keyboard("25000");

    await user.click(screen.getByRole("button", { name: /Tinjau Ringkasan/ }));
    expect(screen.getByText("Audit Penerima")).toBeInTheDocument();

    await user.click(
      screen.getByRole("button", { name: /Otorisasi Transfer/ }),
    );

    expect(await screen.findByText("Transfer Berhasil!")).toBeInTheDocument();
  });
});
