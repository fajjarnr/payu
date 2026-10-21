import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import DashboardLayout from "@/components/DashboardLayout";
import MobileNav from "@/components/MobileNav";
import { renderWithIntl } from "@/__tests__/utils/test-utils";

let mockPathname = "/dashboard";
const mockPush = vi.fn();
vi.mock("@/lib/navigation", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/lib/navigation")>();
  return {
    ...actual,
    usePathname: () => mockPathname,
    useRouter: () => ({
      push: mockPush,
      replace: vi.fn(),
      prefetch: vi.fn(),
      back: vi.fn(),
    }),
  };
});

vi.mock("@/stores", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/stores")>();
  return { ...actual, useIsAuthenticated: () => true };
});

vi.mock("@/components/personalization", () => ({
  PersonalizedGreeting: () => <div>PersonalizedGreeting</div>,
}));

describe("shell active-state matching", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPathname = "/dashboard";
  });

  it("DashboardLayout: child route highlights its parent menu item", () => {
    mockPathname = "/transactions/abc123";
    renderWithIntl(
      <DashboardLayout username="Test User">
        <div>Child</div>
      </DashboardLayout>,
    );
    const history = screen.getByRole("menuitem", {
      name: "Riwayat Transaksi",
    });
    expect(history).toHaveClass("ant-menu-item-selected");
  });

  it("DashboardLayout: dashboard keeps its special-case highlight", () => {
    mockPathname = "/dashboard";
    renderWithIntl(
      <DashboardLayout username="Test User">
        <div>Child</div>
      </DashboardLayout>,
    );
    const item = screen.getByRole("menuitem", { name: "Dasbor" });
    expect(item).toHaveClass("ant-menu-item-selected");
  });

  it("MobileNav: child route highlights its parent tab", () => {
    mockPathname = "/transfer/confirm";
    renderWithIntl(<MobileNav />);
    const transferLink = screen.getByText("Transfer").closest("a");
    expect(transferLink).toHaveAttribute("aria-current", "page");
  });

  it("MobileNav: dashboard keeps its special-case highlight", () => {
    mockPathname = "/dashboard";
    renderWithIntl(<MobileNav />);
    const link = screen.getByText("Dasbor").closest("a");
    expect(link).toHaveAttribute("aria-current", "page");
  });
});

describe("shell landmarks and targets", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPathname = "/dashboard";
  });

  it("DashboardLayout: sidebar menu is wrapped in a labelled nav landmark", () => {
    const { container } = renderWithIntl(
      <DashboardLayout username="Test User">
        <div>Child</div>
      </DashboardLayout>,
    );
    const siderNav = container.querySelector(
      ".ant-layout-sider nav[aria-label]",
    );
    expect(siderNav).not.toBeNull();
    expect(siderNav?.querySelector(".ant-menu")).not.toBeNull();
  });

  it("DashboardLayout: hamburger trigger is 44x44", () => {
    renderWithIntl(
      <DashboardLayout username="Test User">
        <div>Child</div>
      </DashboardLayout>,
    );
    const trigger = screen.getByTestId("mobile-menu-trigger");
    expect(trigger.style.width).toBe("44px");
    expect(trigger.style.height).toBe("44px");
  });

  it("DashboardLayout: exactly one logo landmark while the drawer is closed", () => {
    renderWithIntl(
      <DashboardLayout username="Test User">
        <div>Child</div>
      </DashboardLayout>,
    );
    expect(screen.getAllByLabelText(/PayU.+beranda/i)).toHaveLength(1);
  });

  it("DashboardLayout: drawer copy of the logo stays out of the a11y tree when open", () => {
    renderWithIntl(
      <DashboardLayout username="Test User">
        <div>Child</div>
      </DashboardLayout>,
    );
    fireEvent.click(screen.getByTestId("mobile-menu-trigger"));
    // Both visuals render, but only one remains an accessible brand link.
    expect(
      screen.getAllByRole("link", { name: /PayU.+beranda/i }),
    ).toHaveLength(1);
  });
});
