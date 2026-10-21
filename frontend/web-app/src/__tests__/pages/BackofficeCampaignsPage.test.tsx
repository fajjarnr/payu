import React from "react";
import { screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { vi } from "vitest";
import CampaignsPage from "@/app/[locale]/backoffice/campaigns/page";
import { renderWithIntl } from "@/__tests__/utils/test-utils";

vi.mock("@/lib/navigation", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/navigation")>();
  return {
    ...actual,
    Link: ({ children, href }: { children: React.ReactNode; href: string }) => (
      <a href={href}>{children}</a>
    ),
  };
});

const promoState: {
  data: unknown[];
  isLoading: boolean;
  error: unknown;
  refetch: () => void;
} = { data: [], isLoading: false, error: null, refetch: vi.fn() };

vi.mock("@/hooks/useRewards", () => ({
  useCreatePromotion: () => ({
    mutate: vi.fn(),
    mutateAsync: vi.fn(),
    isPending: false,
  }),
  useActivePromotions: () => promoState,
}));

describe("campaigns error branch", () => {
  beforeEach(() => {
    promoState.data = [];
    promoState.isLoading = false;
    promoState.error = null;
    vi.clearAllMocks();
  });

  it("renders an error alert with retry, distinct from the empty state", () => {
    promoState.error = new Error("boom");
    renderWithIntl(<CampaignsPage />);
    expect(screen.getByTestId("campaigns-error")).toBeInTheDocument();
    expect(
      screen.queryByText("No campaigns found"),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /coba lagi/i }));
    expect(promoState.refetch).toHaveBeenCalled();
  });

  it("keeps the plain empty text when there is no error", () => {
    renderWithIntl(<CampaignsPage />);
    expect(screen.getByText("No campaigns found")).toBeInTheDocument();
    expect(screen.queryByTestId("campaigns-error")).not.toBeInTheDocument();
  });
});
