import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NextIntlClientProvider } from "next-intl";
import idMessages from "../../../messages/id.json";
import CardsPage from "@/app/[locale]/cards/page";
import TargetedPromos from "@/components/personalization/TargetedPromos";
import SegmentedOffers from "@/components/personalization/SegmentedOffers";

vi.mock("@/components/DashboardLayout", () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

vi.mock("@/hooks/useCards", () => ({
  useCards: () => ({
    data: [
      {
        id: "card_1",
        cardNumber: "****-****-****-1234",
        cardType: "VIRTUAL",
        status: "ACTIVE",
        dailyLimit: 10000000,
        expiryDate: "12/28",
      },
    ],
    isLoading: false,
  }),
  useFreezeCard: () => ({ mutateAsync: vi.fn() }),
  useUnfreezeCard: () => ({ mutateAsync: vi.fn() }),
  useCreateCard: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDeleteCard: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUpdateCard: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

const offerWithCurrency = {
  id: "offer-fx",
  title: "Cashback Rupiah",
  description: "Cashback bernilai rupiah",
  segmentId: "seg1",
  segmentTier: "VIP",
  offerType: "CASHBACK",
  value: 50000,
  currency: "IDR",
  promoCode: "CB50",
  validFrom: "2024-01-01",
  validUntil: "2029-12-31",
  terms: [],
  minTransaction: 100000,
  isActive: true,
  createdAt: "2024-01-01",
};

vi.mock("@/hooks/useSegmentedOffers", () => ({
  useSegmentedOffers: () => ({
    offers: [offerWithCurrency],
    cashbackOffers: [offerWithCurrency],
    discountOffers: [],
    rewardOffers: [],
    freeTransferOffers: [],
    isLoading: false,
    error: null,
    totalCount: 1,
  }),
}));

vi.mock("@/stores/authStore", () => ({
  useAuthStore: vi.fn((selector) => {
    const state = {
      user: { id: "user_1", fullName: "Test User" },
      accountId: "acct_1",
      isAuthenticated: true,
    };
    return selector ? selector(state) : state;
  }),
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <NextIntlClientProvider locale="id" messages={idMessages}>
        {children}
      </NextIntlClientProvider>
    </QueryClientProvider>
  );
  Wrapper.displayName = "QueryClientWrapper";
  return Wrapper;
};

describe("cards mask toggle a11y", () => {
  it("announces pressed state with an accessible label", () => {
    render(<CardsPage />, { wrapper: createWrapper() });
    const toggle = screen.getByRole("button", { name: /detail kartu/i });
    expect(toggle).toHaveAttribute("aria-pressed", "false");
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute("aria-pressed", "true");
  });
});

describe("offer currency unification", () => {
  it("TargetedPromos routes currency values through the shared helper", () => {
    const { container } = render(<TargetedPromos maxPromos={1} />, {
      wrapper: createWrapper(),
    });
    // lib formatCurrency with symbol override: "IDR\u00A050.000".
    expect(container.textContent).toContain("IDR 50.000");
    expect(container.textContent).toContain("Rp 100.000");
  });

  it("SegmentedOffers routes currency values through the shared helper", () => {
    const { container } = render(<SegmentedOffers maxOffers={1} />, {
      wrapper: createWrapper(),
    });
    expect(container.textContent).toContain("IDR 50.000");
    expect(container.textContent).toContain("Rp 100.000");
  });
});
