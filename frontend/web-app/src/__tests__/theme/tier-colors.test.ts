import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { contrast } from './wcag';

// VIP tier colors render as text and icon color in VIPBadge
// (components/personalization/VIPBadge.tsx: style={{ color: tierColor }}),
// on the antd card surface in whichever theme is active. The hook returns
// CSS variables so each theme resolves its own measured shade.

const css = readFileSync(resolve(process.cwd(), 'src/app/globals.css'), 'utf8');

function block(marker: string): Record<string, string> {
  const start = css.indexOf(marker);
  expect(start, `${marker} block exists`).toBeGreaterThan(-1);
  const end = css.indexOf('}', start);
  const body = css.slice(start, end);
  const vars: Record<string, string> = {};
  for (const m of body.matchAll(/(--[a-z0-9-]+):\s*([^;]+);/g)) vars[m[1]] = m[2].trim();
  return vars;
}

const TIERS = ['bronze', 'silver', 'gold', 'platinum', 'diamond', 'vip', 'standard'] as const;

describe('VIP tier colors (CSS variables in globals.css)', () => {
  it('light mode: every tier color passes 4.5:1 as text on #FFFFFF', () => {
    const vars = block(':root {');
    for (const tier of TIERS) {
      const value = vars[`--tier-${tier}`];
      expect(value, `--tier-${tier} defined in :root`).toBeDefined();
      const ratio = contrast(value, '#FFFFFF');
      expect(ratio, `--tier-${tier} (${value}) on white is ${ratio.toFixed(2)}:1`).toBeGreaterThanOrEqual(4.5);
    }
  });

  it('dark mode: every tier color passes 4.5:1 as text on #141414', () => {
    const vars = block('.dark {');
    for (const tier of TIERS) {
      const value = vars[`--tier-${tier}`];
      expect(value, `--tier-${tier} defined in .dark`).toBeDefined();
      const ratio = contrast(value, '#141414');
      expect(ratio, `--tier-${tier} (${value}) on #141414 is ${ratio.toFixed(2)}:1`).toBeGreaterThanOrEqual(4.5);
    }
  });

  it('accent token passes 4.5:1 under its white foreground', () => {
    const vars = block('@theme inline {');
    const ratio = contrast(vars['--color-accent-foreground'], vars['--color-accent']);
    expect(
      ratio,
      `--color-accent-foreground on --color-accent (${vars['--color-accent']}) is ${ratio.toFixed(2)}:1`,
    ).toBeGreaterThanOrEqual(4.5);
  });
});
