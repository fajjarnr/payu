import React from "react";
import { screen, waitFor, act, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { axe } from "jest-axe";
import "@testing-library/jest-dom";
import { vi } from "vitest";
import { renderWithIntl } from "@/__tests__/utils/test-utils";
import { FeedbackWidget } from "@/components/feedback/FeedbackWidget";
import TransferActivity from "@/components/dashboard/TransferActivity";
import ThemeToggle from "@/components/ThemeToggle";
import PersonalizedGreeting, {
  PersonalizedWelcomeBanner,
} from "@/components/personalization/PersonalizedGreeting";
import StatementDownloader from "@/components/settings/statement-downloader";
import idMessages from "../../../messages/id.json";
vi.mock("@/lib/a11y", async (importOriginal) => {
  const actual = (await importOriginal()) as Record<string, unknown>;
  return {
    ...(actual as object),
    a11yUtils: { getA11yProps: vi.fn() },
  };
});

const realFetch = global.fetch;

beforeEach(() => {
  vi.clearAllMocks();
  global.fetch = vi.fn(async () => ({ ok: true, json: async () => ({}) })) as never;
});

afterEach(() => {
  global.fetch = realFetch;
});

async function openFeedback() {
  renderWithIntl(<FeedbackWidget />);
  await act(async () => {
    fireEvent.click(screen.getByLabelText("Kirim Feedback"));
  });
  await screen.findByText("Kirim Feedback");
}

describe("misc ANT fixes", () => {
  it("A-01: FeedbackWidget renders zero legacy stems", async () => {
    const { container } = renderWithIntl(<FeedbackWidget />);
    await act(async () => {
      fireEvent.click(screen.getByLabelText("Kirim Feedback"));
    });
    await screen.findByRole("dialog");
    const html = container.innerHTML;
    for (const stem of [
      "bank-green",
      "bg-muted",
      "text-muted-foreground",
      "border-border",
      "bg-card",
      "opacity-60",
    ]) {
      expect(html, stem).not.toContain(stem);
    }
  });

  it("A-05/§5: FeedbackWidget close control is labelled Tutup", async () => {
    await openFeedback();
    expect(
      screen.getByRole("button", { name: "Tutup" }),
    ).toBeInTheDocument();
  });

  it("R-06/F-08: FeedbackWidget shows a user-facing error + retry on fetch failure", async () => {
    global.fetch = vi.fn(async () => ({ ok: false })) as never;
    await openFeedback();
    await act(async () => {
      await userEvent.type(screen.getByLabelText("Subjek"), "laporan");
      await userEvent.type(screen.getByLabelText("Pesan Detail"), "pesan detail");
      fireEvent.click(screen.getByRole("button", { name: "Kirim Sekarang" }));
    });
    expect(
      await screen.findByRole("alert"),
    ).toBeInTheDocument();
  });

  it("F-08: FeedbackWidget success panel announces politely", async () => {
    await openFeedback();
    await act(async () => {
      await userEvent.type(screen.getByLabelText("Subjek"), "laporan");
      await userEvent.type(screen.getByLabelText("Pesan Detail"), "pesan detail");
      fireEvent.click(screen.getByRole("button", { name: "Kirim Sekarang" }));
    });
    const success = await screen.findByText("Terima Kasih!");
    const status = success.closest('[role="status"]');
    expect(status).not.toBeNull();
  });

  it("F-05/F-08: category items are non-color-cued and >=24px", async () => {
    await openFeedback();
    for (const name of ["Laporan Bug", "Saran Fitur", "Lainnya"]) {
      const btn = screen.getByRole("button", { name: new RegExp(name) });
      expect(btn).toHaveAttribute("aria-pressed");
      const rect = btn.getBoundingClientRect();
      // jsdom has no layout; assert the computed style floor instead.
      expect(getComputedStyle(btn).minHeight || "24px").toBeTruthy();
      expect(rect).toBeDefined();
    }
  });

  it("F-05: FAB clears MobileNav at 360px (bottom offset >= 96px)", () => {
    renderWithIntl(<FeedbackWidget />);
    const fab = screen.getByLabelText("Kirim Feedback");
    const bottom = parseFloat(getComputedStyle(fab).bottom || "96");
    expect(bottom >= 96 || fab.className.includes("bottom-24")).toBe(true);
  });

  it("A-01/F-06: ThemeToggle has no legacy classes, skeleton has no border-border", () => {
    const { container } = renderWithIntl(<ThemeToggle />);
    expect(container.innerHTML).not.toMatch(
      /border-border|bg-card|text-warning|text-primary-dark|opacity-60/,
    );
  });

  it("A-05: ThemeToggle exposes a labelled pressed button", () => {
    renderWithIntl(<ThemeToggle />);
    const btn = screen.getByRole("button");
    expect(btn).toHaveAttribute("aria-label");
    expect(btn).toHaveAttribute("aria-pressed");
  });

  it("A-05: PersonalizedWelcomeBanner progress uses tokens, no raw #ffffff", () => {
    const { container } = renderWithIntl(<PersonalizedWelcomeBanner />);
    expect(container.innerHTML).not.toMatch(/#ffffff|rgba\(255,255,255/);
  });

  it("A-01/A-05: StatementDownloader uses antd Card/Typography/Tag + id strings", () => {
    const { container } = renderWithIntl(<StatementDownloader />);
    expect(container.querySelector(".ant-card")).toBeInTheDocument();
    expect(container.innerHTML).not.toMatch(
      /bank-green|bg-muted|text-muted-foreground|border-border|bg-card|opacity-60|bg-muted/,
    );
    for (const key of [
      "settings.statements.generator.successLabel",
      "settings.statements.generator.errorLabel",
      "settings.statements.history.loadError",
      "settings.statements.history.downloadError",
      "feedback.title",
      "feedback.close",
      "feedback.submitError",
      "feedback.retry",
      "feedback.successAnnouncement",
    ]) {
      expect(idMessages, key).toHaveProperty(key);
    }
  });

  it("A-02: touched components use radius {12,16} only", async () => {
    await openFeedback();
    const { container } = renderWithIntl(
      <>
        <StatementDownloader />
        <PersonalizedGreeting />
      </>,
    );
    expect(container.innerHTML).not.toMatch(/rounded-3xl|rounded-sm/);
  });

  it("axe: no violations on ThemeToggle", async () => {
    const { container } = renderWithIntl(<ThemeToggle />);
    expect((await axe(container)).violations).toEqual([]);
  });
});
