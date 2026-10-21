import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import BalanceCard from "@/components/dashboard/BalanceCard";
import { renderWithIntl } from "@/__tests__/utils/test-utils";

vi.mock("@/hooks/useUserSegment", () => ({
  useUserSegment: () => ({
    currentTier: undefined,
    isVIP: false,
    currentMembership: undefined,
    progressToNext: undefined,
    nextTier: undefined,
    totalScore: 0,
  }),
}));

vi.mock("@/stores/authStore", () => ({
  useAuthStore: vi.fn((selector) => {
    const state = {
      user: { id: "test-user", fullName: "Test User" },
      token: null,
      refreshToken: null,
      accountId: null,
      isAuthenticated: false,
      setAuth: vi.fn(),
      setUser: vi.fn(),
      setToken: vi.fn(),
      logout: vi.fn(),
      clearAuth: vi.fn(),
    };
    return selector ? selector(state) : state;
  }),
}));

describe("BalanceCard delta sign branching", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders a negative primary delta without a '+' prefix", () => {
    renderWithIntl(<BalanceCard balance={1000000} percentage={-5} />);
    expect(screen.queryByText(/\+-5%/)).not.toBeInTheDocument();
    expect(screen.getByText("-5%")).toBeInTheDocument();
  });

  it("renders a positive primary delta with a '+' prefix", () => {
    renderWithIntl(<BalanceCard balance={1000000} percentage={5} />);
    expect(screen.getByText("+5%")).toBeInTheDocument();
  });

  it("renders a negative net-worth delta without a '+' prefix", () => {
    renderWithIntl(
      <BalanceCard balance={1000000} netWorth={2000000} netWorthChange={-3} />,
    );
    const card = screen.getByTestId("net-worth-card");
    expect(card.textContent).not.toMatch(/\+-3%/);
    expect(card.textContent).toMatch(/-3%/);
  });

  it("net-worth delta text does not use the low-contrast primary color", () => {
    const { container } = renderWithIntl(
      <BalanceCard balance={1000000} netWorth={2000000} netWorthChange={3} />,
    );
    const card = container.querySelector('[data-testid="net-worth-card"]');
    const delta = Array.from(card?.querySelectorAll("span") ?? []).find(
      (el) => el.textContent === "+3%",
    );
    expect(delta).toBeDefined();
    expect(delta?.style.color).not.toBe("var(--ant-color-primary)");
  });
});

describe("BalanceCard loading branch", () => {
  it("renders a skeleton while loading instead of money", () => {
    renderWithIntl(<BalanceCard balance={1000000} isLoading />);
    expect(screen.getByTestId("balance-card-skeleton")).toBeInTheDocument();
    expect(screen.queryByText("Rp 1.000.000")).not.toBeInTheDocument();
  });
});
