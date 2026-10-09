import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import CompliancePage from "@/app/[locale]/backoffice/compliance/page";

const searchAuditReports = vi.fn(async () => []);
const getFailedAccess = vi.fn(async () => []);

vi.mock("@/components/DashboardLayout", () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

// Real hooks run against a mocked service so the `enabled` gate is what is
// under test, not a hand-written hook stub.
vi.mock("@/services/ComplianceService", () => ({
  default: {
    searchAuditReports: (...args: unknown[]) =>
      searchAuditReports(...(args as [])),
    getFailedAccess: (...args: unknown[]) => getFailedAccess(...(args as [])),
  },
}));

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <CompliancePage />
    </QueryClientProvider>,
  );
}

describe("CompliancePage", () => {
  beforeEach(() => {
    searchAuditReports.mockClear();
    getFailedAccess.mockClear();
  });

  it("does not query audit reports without a search parameter (BACKOFFICE-RBAC-001)", async () => {
    renderPage();

    // The controller rejects a parameterless search with 400 — the query must
    // stay parked instead of surfacing that as an error state.
    await waitFor(() => expect(getFailedAccess).toHaveBeenCalled());
    expect(searchAuditReports).not.toHaveBeenCalled();
  });

  it("instructs the user to enter a Transaction ID or Merchant ID", async () => {
    renderPage();

    expect(
      await screen.findByText(
        "Masukkan Transaction ID atau Merchant ID untuk mencari laporan audit",
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(
        "Akses ditolak: butuh role COMPLIANCE_OFFICER atau ADMIN",
      ),
    ).not.toBeInTheDocument();
  });

  it("searches by transaction id when a UUID is submitted", async () => {
    const user = userEvent.setup();
    renderPage();

    const input = screen.getByPlaceholderText(
      /Transaction ID atau Merchant ID/,
    );
    await user.type(input, "550e8400-e29b-41d4-a716-446655440000{Enter}");

    await waitFor(() =>
      expect(searchAuditReports).toHaveBeenCalledWith({
        transactionId: "550e8400-e29b-41d4-a716-446655440000",
      }),
    );
  });

  it("searches by merchant id for a non-UUID term", async () => {
    const user = userEvent.setup();
    renderPage();

    const input = screen.getByPlaceholderText(
      /Transaction ID atau Merchant ID/,
    );
    await user.type(input, "MERCHANT-001{Enter}");

    await waitFor(() =>
      expect(searchAuditReports).toHaveBeenCalledWith({
        merchantId: "MERCHANT-001",
      }),
    );
  });
});
