import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithIntl } from "@/__tests__/utils/test-utils";
import SecurityPage from "@/app/[locale]/security/page";


vi.mock("@/lib/navigation", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/navigation")>()),
  Link: ({ children, href }: { children: React.ReactNode; href: string }) => (
    <a href={href}>{children}</a>
  ),
}));

vi.mock("@/components/DashboardLayout", () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

vi.mock("@/stores/authStore", () => ({
  useAuthStore: () => ({
    user: { id: "user_1" },
    isAuthenticated: true,
  }),
}));

vi.mock("@/hooks/useBiometrics", () => ({
  useBiometricRegistrations: () => ({
    data: [],
    isLoading: false,
  }),
  useRegisterBiometric: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useRevokeBiometric: () => ({ mutateAsync: vi.fn() }),
}));

describe("SecurityPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render within DashboardLayout", () => {
    renderWithIntl(<SecurityPage />);
    expect(screen.getByTestId("dashboard-layout")).toBeInTheDocument();
  });

  it("should render page title", () => {
    renderWithIntl(<SecurityPage />);
    expect(screen.getByText("Keamanan & Tata Kelola")).toBeInTheDocument();
  });

  it("should render biometric MFA section", () => {
    renderWithIntl(<SecurityPage />);
    expect(screen.getAllByText("MFA Biometrik").length).toBeGreaterThan(0);
  });

  it("should render device tokens section", () => {
    renderWithIntl(<SecurityPage />);
    expect(screen.getByText("Token Perangkat")).toBeInTheDocument();
  });

  it("should render authenticated sessions", () => {
    renderWithIntl(<SecurityPage />);
    expect(screen.getByText("Sesi Terautentikasi")).toBeInTheDocument();
  });

  it("should render panic protocol", () => {
    renderWithIntl(<SecurityPage />);
    expect(screen.getByText("Protokol Panic.")).toBeInTheDocument();
  });
});
