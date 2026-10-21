import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { renderWithIntl } from "../utils/test-utils";
import { SkipLink } from "@/lib/a11y";

// Files owned by AntSweepShell. The per-route error pages are covered by
// src/__tests__/app/route-states.test.ts (they now delegate to RouteError).
const SHELL_FILES = [
  "src/app/global-error.tsx",
  "src/app/[locale]/layout.tsx",
  "src/services/StatementService.ts",
  "src/components/DashboardLayout.tsx",
  "src/components/personalization/PersonalizedGreeting.tsx",
  "src/components/personalization/VIPBadge.tsx",
  "src/lib/a11y.tsx",
];

// Zero-list stems from docs/ANT_GUIDELINES.md §6 A-01.
const STEMS =
  /bank-green|bank-emerald|bg-muted|text-muted-foreground|border-border|bg-card|opacity-60/;

describe("shell token migration (A-01)", () => {
  for (const file of SHELL_FILES) {
    it(`${file} contains no legacy theme stems`, () => {
      const source = readFileSync(join(process.cwd(), file), "utf8");
      expect(source).not.toMatch(STEMS);
    });
  }

  it("SkipLink keeps sr-only → visible focus behavior without legacy classes", () => {
    const { container } = renderWithIntl(<SkipLink />);
    const link = container.querySelector("a");
    expect(link).toBeTruthy();
    expect(link?.textContent).toContain("Lewati ke konten utama");
    // sr-only by default, revealed on focus — no legacy bg-primary-dark/text-surface
    expect(link?.className).toMatch(/sr-only/);
    expect(link?.className).toMatch(/focus:not-sr-only/);
    expect(link?.className).not.toMatch(STEMS);
    expect(link?.style.backgroundColor).toBe("");
    // focus style hook survives: a <style> rule switches colors on :focus-visible
    const style = document.querySelector("style[data-skiplink-focus]");
    expect(style?.textContent).toMatch(/focus-visible/);
  });
});
