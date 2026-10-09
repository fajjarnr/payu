import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import Home from "@/app/[locale]/dashboard/page";

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  wrapper.displayName = "TestWrapper";
  return wrapper;
};

vi.mock("next-intl", () => ({
  useTranslations: () => (key: string) => {
    const map: Record<string, string> = {
      "common.user": "User",
      futureTitle: "Start Investing",
      futureDesc: "Grow your wealth",
      startInvesting: "Invest Now",
    };
    return map[key] || key;
  },
  useLocale: () => "id",
}));

vi.mock("@/components/DashboardLayout", () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

const { metricsMock, cashFlowMock, trendsMock } = vi.hoisted(() => ({
  metricsMock: vi.fn((..._args: unknown[]) => ({ isLoading: false })),
  cashFlowMock: vi.fn((..._args: unknown[]) => ({
    data: undefined,
    isLoading: false,
  })),
  trendsMock: vi.fn((..._args: unknown[]) => ({ isLoading: false })),
}));

vi.mock("@/hooks", async (importOriginal) => {
  const actual = (await importOriginal()) as Record<string, unknown>;
  return {
    ...actual,
    useLogout: () => ({ mutateAsync: vi.fn(), isPending: false }),
    useBalance: () => ({
      data: { balance: 15000000, formattedBalance: "Rp 15.000.000" },
      isLoading: false,
      error: null,
    }),
    useUserMetrics: (...args: [unknown]) => metricsMock(...args),
    useCashFlow: (...args: [unknown]) => cashFlowMock(...args),
    useSpendingTrends: (...args: [unknown]) => trendsMock(...args),
  };
});

vi.mock("@/stores/authStore", () => ({
  useAuthStore: (selector: (s: unknown) => unknown) =>
    selector({
      user: { id: "user_1", username: "budi", fullName: "Budi Santoso" },
      accountId: "acct_1",
      isAuthenticated: true,
    }),
}));

vi.mock("@/components/dashboard", () => ({
  BalanceCard: () => <div data-testid="balance-card">Balance Card</div>,
  QuickActions: () => <div data-testid="quick-actions">Quick Actions</div>,
  TransferActivity: () => (
    <div data-testid="transfer-activity">Transfer Activity</div>
  ),
}));
vi.mock("@/components/dashboard/BalanceCard", () => ({
  default: () => <div data-testid="balance-card">Balance Card</div>,
}));
vi.mock("@/components/dashboard/QuickActions", () => ({
  default: () => <div data-testid="quick-actions">Quick Actions</div>,
}));
vi.mock("@/components/cms/BannerCarousel", () => ({
  default: () => <div data-testid="banner-carousel">Banners</div>,
}));
vi.mock("@/components/cms/PromoPopup", () => ({
  default: () => <div data-testid="promo-popup">Promo</div>,
}));
vi.mock("@/components/personalization/SegmentedOffers", () => ({
  default: () => <div data-testid="segmented-offers">Offers</div>,
}));
vi.mock("@/lib/a11y", () => ({
  SkipLink: () => <div>Skip</div>,
}));

vi.mock("@/lib/navigation", () => ({
  Link: ({
    children,
    ...props
  }: {
    children: React.ReactNode;
    href: string;
  }) => <a {...props}>{children}</a>,
}));

vi.mock("next/dynamic", () => ({
  default: () => () => <div data-testid="dynamic-component">Dynamic</div>,
}));

describe("DashboardPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render within DashboardLayout", () => {
    render(<Home />, { wrapper: createWrapper() });
    expect(screen.getByTestId("dashboard-layout")).toBeInTheDocument();
  });

  it("should render balance card", () => {
    render(<Home />, { wrapper: createWrapper() });
    expect(screen.getByTestId("balance-card")).toBeInTheDocument();
  });

  it("queries analytics by accountId, the event key (FE-AUDIT-006)", () => {
    render(<Home />, { wrapper: createWrapper() });
    expect(metricsMock).toHaveBeenCalledWith("acct_1");
    expect(cashFlowMock).toHaveBeenCalledWith("acct_1");
    expect(trendsMock).toHaveBeenCalledWith("acct_1");
  });

  it("should render quick actions", () => {
    render(<Home />, { wrapper: createWrapper() });
    expect(screen.getByTestId("quick-actions")).toBeInTheDocument();
  });

  it("shows recent activity for the anomaly check", () => {
    render(<Home />, { wrapper: createWrapper() });
    // next/dynamic is mocked — the lazy activity slot renders the stub.
    expect(screen.getByTestId("dynamic-component")).toBeInTheDocument();
  });

  it("keeps the promo popup out of the first paint", () => {
    render(<Home />, { wrapper: createWrapper() });
    expect(screen.queryByTestId("promo-popup")).not.toBeInTheDocument();
  });

  it("keeps secondary sections off home", () => {
    render(<Home />, { wrapper: createWrapper() });
    expect(screen.queryByText("Start Investing")).not.toBeInTheDocument();
    expect(screen.queryByTestId("banner-carousel")).not.toBeInTheDocument();
    expect(screen.queryByTestId("segmented-offers")).not.toBeInTheDocument();
  });
});
