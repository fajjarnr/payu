import type * as React from "react";
import type * as NavigationModule from "@/lib/navigation";
import { screen } from "@testing-library/react";
import { vi } from "vitest";
import { renderWithIntl } from "@/__tests__/utils/test-utils";
import MobileNav from "@/components/MobileNav";
import TargetedPromos, { QuickPromoBanner } from "@/components/personalization/TargetedPromos";
import SegmentedOffers from "@/components/personalization/SegmentedOffers";
import { ErrorBoundary } from "@/components/ErrorBoundary";

vi.mock("@/lib/navigation", async (importOriginal) => ({
  ...(await importOriginal<typeof NavigationModule>()),
  useRouter: () => ({ back: vi.fn() }),
  usePathname: () => "/dashboard",
}));

vi.mock("@/stores", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/stores")>();
  return { ...actual, useIsAuthenticated: () => true };
});

vi.mock("@/hooks/useSegmentedOffers", () => ({
  useSegmentedOffers: () => ({
    offers: [
      {
        id: "offer1",
        title: "Cashback 10%",
        description: "Cashback for every purchase",
        offerType: "CASHBACK",
        value: 10,
        percentage: 10,
        promoCode: "CASH10",
        validFrom: "2024-01-01",
        validUntil: "2099-12-31",
        minTransaction: 50000,
      },
    ],
    cashbackOffers: [
      {
        id: "offer1",
        title: "Cashback 10%",
        description: "Cashback for every purchase",
        offerType: "CASHBACK",
        value: 10,
        percentage: 10,
        promoCode: "CASH10",
        validFrom: "2024-01-01",
        validUntil: "2099-12-31",
        minTransaction: 50000,
      },
    ],
    isLoading: false,
    error: null,
    totalCount: 1,
  }),
}));

const BANNED = [
  "bank-green",
  "bank-emerald",
  "bg-muted",
  "text-muted-foreground",
  "border-border",
  "bg-card",
  "opacity-60",
];

function expectNoLegacy(container: HTMLElement) {
  const html = container.innerHTML;
  for (const stem of BANNED) {
    expect(html, `legacy stem "${stem}"`).not.toContain(stem);
  }
}

function Thrower(): React.ReactNode {
  throw new Error("boom");
}

describe("ANT leftover A-01 migration", () => {
  it("MobileNav renders no legacy token-bypass classes", () => {
    const { container } = renderWithIntl(<MobileNav />);
    expectNoLegacy(container);
  });

  it("TargetedPromos + QuickPromoBanner render no legacy classes", () => {
    const { container, unmount } = renderWithIntl(<TargetedPromos maxPromos={1} />);
    expect(screen.getByText("Promo Untuk Anda")).toBeInTheDocument();
    expectNoLegacy(container);
    unmount();
    const banner = renderWithIntl(<QuickPromoBanner />);
    expect(banner.container.innerHTML).toContain("Cashback 10%");
    expectNoLegacy(banner.container);
  });

  it("SegmentedOffers renders no legacy classes", () => {
    const { container } = renderWithIntl(<SegmentedOffers maxOffers={1} />);
    expect(screen.getByText("Penawaran Spesial Untuk Anda")).toBeInTheDocument();
    expectNoLegacy(container);
  });

  it("ErrorBoundary fallback renders no legacy classes", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => {});
    const { container } = renderWithIntl(
      <ErrorBoundary>
        <Thrower />
      </ErrorBoundary>,
    );
    expect(screen.getByText("Terjadi Kesalahan")).toBeInTheDocument();
    expectNoLegacy(container);
    spy.mockRestore();
  });
});
