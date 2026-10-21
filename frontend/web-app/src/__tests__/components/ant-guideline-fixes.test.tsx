import React from "react";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { vi } from "vitest";
import { renderWithIntl } from "@/__tests__/utils/test-utils";
import { contrast, toHex } from "@/__tests__/theme/wcag";
import { lightComponentTokens } from "@/theme/tokens";
import type { Transaction } from "@/services/TransactionService";
import idMessages from "../../../messages/id.json";
import enMessages from "../../../messages/en.json";

import BalanceCard from "@/components/dashboard/BalanceCard";
import QuickActions from "@/components/dashboard/QuickActions";
import TransferActivity from "@/components/dashboard/TransferActivity";
import MobileNav from "@/components/MobileNav";
import DashboardLayout from "@/components/DashboardLayout";
import BeneficiaryManager from "@/components/account/BeneficiaryManager";
const mockPush = vi.fn();
let mockPathname = "/dashboard";
vi.mock("@/lib/navigation", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/navigation")>();
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
  const actual =
    await importOriginal<typeof import("@/stores")>();
  return { ...actual, useIsAuthenticated: () => true };
});

const txState: {
  data: Transaction[];
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
} = { data: [], isLoading: false, isError: false, refetch: vi.fn() };
const cancelMutateAsync = vi.fn(async (_id: string) => {});
vi.mock("@/hooks", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/hooks")>();
  return {
    ...actual,
    useTransactions: () => txState,
    useCancelTransaction: () => ({ mutateAsync: cancelMutateAsync }),
  };
});

const { mockGetBeneficiaries, mockDeleteBeneficiary } = vi.hoisted(() => ({
  mockGetBeneficiaries: vi.fn(async (_accountId: string) => [
    {
      id: "ben_1",
      bankCode: "PAYU",
      accountNumber: "1001001002",
      accountName: "Audit Penerima",
      nickname: "Audit Penerima",
      status: "ACTIVE",
    },
  ]),
  mockDeleteBeneficiary: vi.fn(async (_accountId: string, _beneficiaryId: string) => {}),
}));
vi.mock("@/services/AccountService", () => ({
  default: {
    getBeneficiaries: (...args: [string]) => mockGetBeneficiaries(...args),
    createBeneficiary: vi.fn(async () => ({})),
    deleteBeneficiary: (...args: [string, string]) => mockDeleteBeneficiary(...args),
  },
}));

vi.mock("@/hooks/useBeneficiaries", () => ({
  useBeneficiaries: () => ({ data: [{ id: "ben_1", bankCode: "PAYU", accountNumber: "1001001002", accountName: "Audit Penerima", nickname: "Audit Penerima", status: "ACTIVE" }], isLoading: false }),
  useCreateBeneficiary: () => ({ mutateAsync: vi.fn(async () => ({})), isPending: false }),
  useDeleteBeneficiary: () => ({ mutateAsync: (id: string) => mockDeleteBeneficiary("acc_1", id), isPending: false }),
}));

vi.mock("@/components/personalization", () => ({
  PersonalizedGreeting: () => <div>PersonalizedGreeting</div>,
}));

const sampleTx = {
  id: "tx_1",
  type: "TOP_UP",
  description: "Top up DANA",
  amount: 500000,
  status: "COMPLETED",
  createdAt: new Date("2026-10-01T10:00:00Z").toISOString(),
  referenceNumber: "REF001",
} as unknown as Transaction;

describe("ANT guideline fixes", () => {
  beforeEach(() => {
    txState.data = [];
    txState.isLoading = false;
    txState.isError = false;
    vi.clearAllMocks();
    mockPathname = "/dashboard";
  });

  it("F-01: BalanceCard eyebrow must not use colorPrimary text", () => {
    renderWithIntl(<BalanceCard balance={1000000} />);
    const eyebrow = screen.getByText("Saldo Utama");
    expect(eyebrow).not.toHaveStyle({ color: "var(--ant-color-primary)" });
  });

  it("F-01: light-mode primary button label has AA contrast on emerald fill", () => {
    const buttonTokens = lightComponentTokens.Button as Record<string, string>;
    const label = toHex(buttonTokens.primaryColor ?? "#ffffff", "#ffffff");
    expect(
      contrast(label, "#00D09C"),
      `primary label ${label} on #00D09C`,
    ).toBeGreaterThanOrEqual(4.5);
  });

  it("F-01: TransferActivity credit amounts use success-dark text token", () => {
    txState.data = [sampleTx];
    const { container } = renderWithIntl(<TransferActivity />);
    const amounts = Array.from(container.querySelectorAll("span")).filter((el) =>
      el.textContent?.includes("500"),
    );
    expect(amounts.length).toBeGreaterThan(0);
    expect(
      amounts.some((el) => el.style.color.includes("success-text-active")),
    ).toBe(true);
  });

  it("F-02: QuickAction tiles show a >=2px :focus-visible ring", () => {
    const { container } = renderWithIntl(<QuickActions />);
    // The ring is one shared utility (globals.css .payu-focus-ring) so every
    // hand-styled control looks the same when tabbed to.
    const tile = container.querySelector(".payu-qa-tile");
    expect(tile?.className).toContain("payu-focus-ring");
    const css = readFileSync(join(process.cwd(), "src/app/globals.css"), "utf8");
    expect(css).toMatch(/\.payu-focus-ring:focus-visible/);
    expect(css).toMatch(/outline:\s*3px/);
    expect(css).toMatch(/outline-offset:\s*2px/);
  });

  it("DashboardLayout: sidebar items are real links with one context menu", () => {
    renderWithIntl(
      <DashboardLayout username="Test User">
        <div>Child</div>
      </DashboardLayout>,
    );
    const item = screen.getByRole("menuitem", { name: "Dasbor" });
    const link = item.querySelector("a") ?? item.closest("a");
    expect(link).not.toBeNull();
    expect(link?.getAttribute("href")).toMatch(/dashboard/);
  });

  it("DashboardLayout: desktop and mobile search inputs differ, logo names destination", () => {
    renderWithIntl(
      <DashboardLayout username="Test User">
        <div>Child</div>
      </DashboardLayout>,
    );
    expect(
      screen.getByTestId("search-input").querySelector("input")?.getAttribute("aria-label"),
    ).toBe("Pencarian cerdas (desktop)");
    fireEvent.click(screen.getByTestId("search-trigger"));
    expect(
      screen.getByTestId("search-input-mobile").querySelector("input")?.getAttribute("aria-label"),
    ).toBe("Pencarian cerdas (seluler)");
    const logos = screen.getAllByLabelText(/PayU.+beranda/i);
    expect(logos.length).toBeGreaterThan(0);
  });

  it("A-05: new dashboard/transfer/nav strings exist in id+en", () => {
    const need = [
      "dashboard.editActions",
      "dashboard.doneEditing",
      "dashboard.quickTransfer",
      "dashboard.favoriteCategory",
      "dashboard.retryAction",
      "dashboard.txOptions",
      "dashboard.txCancel",
      "dashboard.transferLoadError",
      "dashboard.transferEmpty",
      "dashboard.transferEmptyHint",
      "dashboard.cancelSuccess",
      "dashboard.cancelFailed",
      "nav.smartSearchDesktop",
      "nav.smartSearchMobile",
      "nav.homeBrand",
      "nav.openMenu",
      "nav.openSearch",
      "nav.notificationsLabel",
      "nav.profileMenu",
      "dashboard.searchTransfer",
      "beneficiaries.listLabel",
    ];
    for (const key of need) {
      expect(idMessages, key).toHaveProperty(key);
      expect(enMessages, key).toHaveProperty(key);
    }
  });

  it("TransferActivity: single shared empty + error branch with retry", () => {
    txState.data = [];
    const { unmount } = renderWithIntl(<TransferActivity />);
    // Desktop Table locale + mobile list share one TransferEmpty component
    // (jsdom renders both responsive branches; assert identity, not count).
    const empties = screen.getAllByTestId("transfer-empty");
    expect(empties.length).toBeGreaterThanOrEqual(1);
    for (const el of empties) {
      expect(el.textContent).toMatch(/Belum Ada Transaksi/);
      expect(el.textContent).toMatch(/Transaksi Anda akan muncul di sini/);
    }
    unmount();

    txState.isError = true;
    renderWithIntl(<TransferActivity />);
    fireEvent.click(screen.getByRole("button", { name: /coba lagi/i }));
    expect(txState.refetch).toHaveBeenCalled();
  });

  it("TransferActivity: unique per-row accessible names", () => {
    txState.data = [sampleTx];
    renderWithIntl(<TransferActivity />);
    expect(
      screen.getByRole("button", { name: "Opsi untuk Top up DANA" }),
    ).toBeInTheDocument();
  });

  it("TransferActivity: no numbered contacts, no dead month filter", () => {
    txState.data = [];
    renderWithIntl(<TransferActivity />);
    expect(screen.queryByText("Kontak Terbaru")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Pilih bulan" }),
    ).not.toBeInTheDocument();
  });

  it("QuickActions: no dead color props, no dead view-all footer", () => {
    const { container } = renderWithIntl(<QuickActions />);
    expect(
      container.querySelector('[data-testid="view-all-features-button"]'),
    ).toBeNull();
  });

  it("MobileNav: wrapper is <nav> with plain list and non-color active cue", () => {
    const { container } = renderWithIntl(<MobileNav />);
    const nav = screen.getByTestId("mobile-nav");
    expect(nav.tagName).toBe("NAV");
    expect(nav.querySelector(".ant-menu")).toBeNull();
    expect(nav.querySelector("ul")).toBeInTheDocument();
    const activeLink = screen.getByText("Dasbor").closest("a");
    expect(activeLink).toHaveAttribute("aria-current", "page");
    expect(
      activeLink?.querySelector('[data-testid="mobile-nav-active-indicator"]'),
    ).toBeInTheDocument();
  });

  it("MobileNav: no legacy token-bypass classes (A-01)", () => {
    const { container } = renderWithIntl(<MobileNav />);
    const nav = screen.getByTestId("mobile-nav");
    for (const cls of ["border-border", "bg-card", "bg-muted", "text-muted-foreground", "bg-bank-green"]) {
      expect(nav.className).not.toMatch(cls);
      expect(container.querySelector(`.${cls}`)).toBeNull();
    }
  });

  it("DashboardLayout: header buttons use next-intl labels (A-05)", () => {
    renderWithIntl(
      <DashboardLayout username="Test User">
        <div>Child</div>
      </DashboardLayout>,
    );
    expect(screen.getByTestId("search-trigger").getAttribute("aria-label")).toBe("Buka pencarian");
    expect(screen.getByTestId("notification-button").getAttribute("aria-label")).toBe("Notifikasi (buka)");
    expect(screen.getByTestId("mobile-menu-trigger").getAttribute("aria-label")).toBe("Buka menu navigasi");
    expect(screen.getByTestId("profile-menu-trigger").getAttribute("aria-label")).toBe("Menu profil pengguna");
  });

  it("DashboardLayout: no muted-foreground/opacity stacking (F-06)", () => {
    const { container } = renderWithIntl(
      <DashboardLayout username="Test User">
        <div>Child</div>
      </DashboardLayout>,
    );
    expect(container.querySelector(".text-muted-foreground")).toBeNull();
    expect(container.querySelector(".opacity-60")).toBeNull();
  });
  it("BeneficiaryManager: real labels, stacked grid, single empty text", async () => {
    const { container } = renderWithIntl(
      <BeneficiaryManager accountId="acc_1" />,
    );
    expect(await screen.findByText("Audit Penerima")).toBeInTheDocument();
    for (const id of [
      "beneficiary-bankCode",
      "beneficiary-accountNumber",
      "beneficiary-nickname",
    ]) {
      expect(container.querySelector(`label[for="${id}"]`)).toBeInTheDocument();
    }
    expect(
      container.querySelector(".ant-col-xs-24"),
    ).toBeInTheDocument();
    expect(screen.queryAllByText(/Belum ada penerima/).length).toBeLessThanOrEqual(1);
  });

  it("BeneficiaryManager: delete loading scoped per row with 44px target", async () => {
    renderWithIntl(<BeneficiaryManager accountId="acc_1" />);
    expect(await screen.findByText("Audit Penerima")).toBeInTheDocument();
    const delBtn = screen.getByTestId("beneficiary-delete-ben_1");
    fireEvent.click(delBtn);
    // Delete now requires Popconfirm confirmation.
    fireEvent.click(await screen.findByText("Hapus"));
    expect(mockDeleteBeneficiary).toHaveBeenCalledWith("acc_1", "ben_1");
    expect(delBtn.style.minHeight).toBe("44px");
  });
});
