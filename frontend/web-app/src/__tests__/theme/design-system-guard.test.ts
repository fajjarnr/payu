import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';
import { describe, expect, it } from 'vitest';

/**
 * Design-system guard — the rules in docs/ANT_GUIDELINES.md enforced
 * mechanically, so a new component cannot quietly reintroduce them.
 *
 * Source-level checks (not rendered output): jsdom applies no CSS-in-JS, so
 * colour and layout rules can only be asserted on the source.
 */

const SRC = join(process.cwd(), 'src');

function sourceFiles(dir: string): string[] {
  const out: string[] = [];
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      if (entry === '__tests__') continue;
      out.push(...sourceFiles(full));
    } else if (/\.tsx?$/.test(entry)) {
      out.push(full);
    }
  }
  return out;
}

// The theme module and the chart palette own the raw values (both are
// documented palettes); the global error page renders outside the antd
// provider (no CSS vars available) and the layout metadata carries the
// browser chrome colour.
const HEX_ALLOWLIST = [
  'src/theme/tokens.ts',
  'src/lib/chart-palette.ts',
  'src/app/global-error.tsx',
  'src/app/[locale]/layout.tsx',
];

const FILES = sourceFiles(SRC).filter(
  (file) => !HEX_ALLOWLIST.includes(relative(process.cwd(), file))
);

const RADIUS_SCALE = new Set([2, 4, 6, 8, 12, 16, 9999]);

describe('design-system guard', () => {
  it('finds the source files', () => {
    expect(FILES.length).toBeGreaterThan(80);
  });

  for (const file of FILES) {
    const rel = relative(process.cwd(), file);
    const source = readFileSync(file, 'utf8');

    it(`${rel} uses theme tokens, not raw hex colours`, () => {
      const hexes = [...source.matchAll(/#[0-9a-fA-F]{6}\b/g)].map((m) => ({
        line: source.slice(0, m.index).split('\n').length,
        value: m[0],
      }));
      expect(hexes).toEqual([]);
    });

    it(`${rel} dims text with a solid colour, not with opacity`, () => {
      // `opacity` on a text element compounds with its colour: 0.6 white on the
      // card gradient measured 2.58:1. Decorative art may keep a low opacity.
      const offenders = [...source.matchAll(/style=\{\{([\s\S]*?)\}\}/g)]
        .filter((m) => /\bcolor:\s/.test(m[1]) && /opacity:\s*(0\.[5-9]|1)\b/.test(m[1]))
        .map((m) => source.slice(0, m.index).split('\n').length);
      expect(offenders).toEqual([]);
    });

    it(`${rel} keeps border radii on the scale`, () => {
      const offenders = [...source.matchAll(/borderRadius:\s*(\d+)/g)]
        .filter((m) => !RADIUS_SCALE.has(Number(m[1])))
        .map((m) => `${source.slice(0, m.index).split('\n').length}: ${m[1]}`);
      expect(offenders).toEqual([]);
    });
  }
});
