import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const root = join(__dirname, "..", "..", "app", "[locale]");
const files = [
  "exchange/page.tsx",
  "merchant/page.tsx",
  "merchant/register/page.tsx",
  "onboarding/page.tsx",
  "rewards/page.tsx",
  "settings/page.tsx",
  "split-bill/page.tsx",
  "transfer/page.tsx",
  "pockets/page.tsx",
  "login/login-form.tsx",
];
const STEM = /bank-green|bank-emerald|color-bank-green|bg-muted|text-muted-foreground|border-border|bg-card|opacity-60|hsl\(145|bg-background|text-foreground/;

describe("pages token migration (source-text)", () => {
  for (const f of files) {
    it(`${f} has zero legacy stems`, () => {
      const src = readFileSync(join(root, f), "utf8");
      expect(STEM.test(src)).toBe(false);
    });
  }

  it("login register-link uses AA-safe ink text, not emerald (F-01)", () => {
    const src = readFileSync(join(root, "login/login-form.tsx"), "utf8");
    const block = src.slice(src.indexOf('data-testid="register-link"'), src.indexOf('data-testid="register-link"') + 600);
    expect(block).toContain("token.colorText");
    expect(block).not.toMatch(/colorPrimary|#00A87E|primary-dark/);
  });
});
