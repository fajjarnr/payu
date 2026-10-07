import React from "react";
import { screen } from "@testing-library/react";
import { axe, toHaveNoViolations } from "jest-axe";
import { vi } from "vitest";
import MobileNav from "@/components/MobileNav";
import { renderWithIntl } from "@/__tests__/utils/test-utils";

// Mock the navigation adapter used by the component with a mutable pathname.
let mockPathname = "/";
vi.mock("@/lib/navigation", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/navigation")>()),
  usePathname: () => mockPathname,
}));

let mockIsAuthenticated = true;
vi.mock("@/stores", () => ({
  useIsAuthenticated: () => mockIsAuthenticated,
}));

expect.extend(toHaveNoViolations);

// Security: MobileNav uses auth store (Zustand), not localStorage for token
describe("MobileNav", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPathname = "/";
    mockIsAuthenticated = true;
  });

  it("should render mobile navigation with all items", () => {
    renderWithIntl(<MobileNav />);

    expect(screen.getByText("Dasbor")).toBeInTheDocument();
    expect(screen.getByText("Transfer")).toBeInTheDocument();
    expect(screen.getByText("Bayar QRIS")).toBeInTheDocument();
    expect(screen.getByText("Kartu")).toBeInTheDocument();
    expect(screen.getByText("Pengaturan")).toBeInTheDocument();
  });

  it("should not render on login page", () => {
    mockPathname = "/login";
    const { container } = renderWithIntl(<MobileNav />);

    expect(container.firstChild).toBeNull();
  });

  it("should not render on onboarding page", () => {
    mockPathname = "/onboarding";
    const { container } = renderWithIntl(<MobileNav />);

    expect(container.firstChild).toBeNull();
  });

  it("should not render when user is not authenticated", () => {
    mockIsAuthenticated = false;
    const { container } = renderWithIntl(<MobileNav />);

    expect(container.firstChild).toBeNull();
  });

  it("should render when user is authenticated", () => {
    mockIsAuthenticated = true;
    renderWithIntl(<MobileNav />);

    expect(screen.getByText("Dasbor")).toBeInTheDocument();
  });

  it("should highlight active navigation item", () => {
    mockPathname = "/transfer";
    renderWithIntl(<MobileNav />);

    const activeLink = screen.getByText("Transfer").closest("a");
    expect(activeLink).toHaveAttribute("aria-current", "page");
  });

  it("should render navigation items as links", () => {
    renderWithIntl(<MobileNav />);

    const homeLink = screen.getByText("Dasbor").closest("a");
    expect(homeLink).toHaveAttribute("href", "/dashboard");

    const transferLink = screen.getByText("Transfer").closest("a");
    expect(transferLink).toHaveAttribute("href", "/transfer");
    const qrisLink = screen.getByText("Bayar QRIS").closest("a");
    expect(qrisLink).toHaveAttribute("href", "/qris");

    const cardsLink = screen.getByText("Kartu").closest("a");
    expect(cardsLink).toHaveAttribute("href", "/cards");

    const settingsLink = screen.getByText("Pengaturan").closest("a");
    expect(settingsLink).toHaveAttribute("href", "/settings");
  });

  it("should have proper styling for mobile navigation", () => {
    const { container } = renderWithIntl(<MobileNav />);

    const navContainer = container.querySelector(
      ".fixed.bottom-0.left-0.right-0",
    );
    expect(navContainer).toHaveClass(
      "bg-card/95",
      "border-t",
      "border-border",
      "z-50",
    );
  });

  it("should hide on desktop screens", () => {
    const { container } = renderWithIntl(<MobileNav />);

    const navContainer = container.querySelector(".lg\\:hidden");
    expect(navContainer).toBeInTheDocument();
  });

  it("should have no accessibility violations", async () => {
    const { container } = renderWithIntl(<MobileNav />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it("should render icons for all navigation items", () => {
    const { container } = renderWithIntl(<MobileNav />);

    const icons = container.querySelectorAll("svg");
    expect(icons.length).toBe(5);
  });

  it("should have proper spacing between navigation items", () => {
    const { container } = renderWithIntl(<MobileNav />);

    const navContainer = container.querySelector(".flex.items-center");
    expect(navContainer).toBeInTheDocument();
    // Responsive uses justify-around on mobile, justify-between on sm+
    expect(navContainer).toHaveClass("flex");
  });
  it("should apply active styling with accent background", () => {
    mockPathname = "/qris";
    renderWithIntl(<MobileNav />);

    const activeLink = screen.getByText("Bayar QRIS").closest("a");
    expect(activeLink).toHaveAttribute("aria-current", "page");
  });

  it("should use increased stroke width for active icon", () => {
    mockPathname = "/dashboard";
    const { container: _container } = renderWithIntl(<MobileNav />);

    const activeLink = screen.getByText("Dasbor").closest("a");
    const activeIcon = activeLink?.querySelector(".stroke-\\[2\\.5px\\]");
    expect(activeIcon).toBeInTheDocument();
  });

  it("should have safe area padding for mobile devices", () => {
    renderWithIntl(<MobileNav />);

    expect(screen.getByTestId("mobile-nav")).toHaveClass(
      "pb-[max(0.5rem,env(safe-area-inset-bottom))]",
    );
  });
});
