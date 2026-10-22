import { readFileSync, readdirSync, statSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

/**
 * One content box for the whole app.
 *
 * The shell (DashboardLayout) owns the column; pages that re-implement it
 * (their own max-width or horizontal padding) are what made the grid jump
 * between pages — the statements page ran 1024px inside a 1200px shell and
 * the notifications title sat 25px inside the content edge.
 */
const appRoot = join(__dirname, "..", "..", "app", "[locale]");
const componentsRoot = join(__dirname, "..", "..", "components");

function pageFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) return pageFiles(full);
    return entry === "page.tsx" ? [full] : [];
  });
}

/** A page-level column narrower than the app box: inline or Tailwind. */
const OWN_BOX = /maxWidth:\s*(9[0-9]{2}|1[0-9]{3})\b|max-w-\[(9[0-9]{2}|1[0-9]{3})px\]/;

describe("app content box", () => {
  it("the shell owns the content box", () => {
    const src = readFileSync(join(componentsRoot, "DashboardLayout.tsx"), "utf8");
    expect(src).toContain("max-w-[1440px]");
    expect(src).toContain("px-4");
    expect(src).toContain("sm:px-6");
  });

  it("the loading skeleton uses the same box as the shell", () => {
    const src = readFileSync(join(componentsRoot, "LoadingState.tsx"), "utf8");
    expect(src).toContain("maxWidth: 1440");
  });

  it("no page re-implements the content box", () => {
    for (const file of pageFiles(appRoot)) {
      const src = readFileSync(file, "utf8");
      const match = src.match(OWN_BOX);
      expect(match?.[0], file).toBeUndefined();
    }
  });
});
