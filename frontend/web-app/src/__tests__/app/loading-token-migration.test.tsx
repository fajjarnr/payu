import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const LOADING_FILES = [
  "src/app/[locale]/bills/loading.tsx",
  "src/app/[locale]/dashboard/loading.tsx",
  "src/app/[locale]/investments/loading.tsx",
  "src/app/[locale]/lending/loading.tsx",
  "src/app/[locale]/transfer/loading.tsx",
];

const STEMS =
  /bank-green|bank-emerald|bg-muted|text-muted-foreground|border-border|bg-card|opacity-60/;

describe("loading skeleton token migration (A-01)", () => {
  for (const file of LOADING_FILES) {
    it(`${file} contains no legacy theme stems`, () => {
      const source = readFileSync(join(process.cwd(), file), "utf8");
      expect(source).not.toMatch(STEMS);
    });
  }
});
