import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

const SRC_DIR = join(process.cwd(), 'src');

function sourceFiles(dir: string): string[] {
  const out: string[] = [];
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      out.push(...sourceFiles(full));
    } else if (/\.(tsx|ts)$/.test(entry) && !entry.endsWith('.d.ts')) {
      out.push(full);
    }
  }
  return out;
}

// The legacy palette in globals.css (`--background`, `--primary`, `--card`, …)
// is a second source of truth beside the antd theme. Components must read
// `token.*` / `var(--ant-color-*)` instead — the aliases are not theme aware
// and the emerald ones fail AA as text.
const LEGACY_VARS =
  /var\(--(background|foreground|primary|primary-dark|primary-light|secondary|muted|muted-foreground|card|card-foreground|accent|destructive|success|success-light|border|ring|text-primary|text-secondary|text-disabled|surface|surface-dim|color-)/;

describe('no legacy CSS colour variables in components', () => {
  const files = sourceFiles(SRC_DIR);

  it('finds the source files', () => {
    expect(files.length).toBeGreaterThan(50);
  });

  for (const file of files) {
    const rel = file.replace(process.cwd(), '');
    const source = readFileSync(file, 'utf8');
    if (!LEGACY_VARS.test(source)) continue;

    it(`${rel} uses antd tokens, not globals.css aliases`, () => {
      const offenders = source
        .split('\n')
        .map((line, index) => ({ line: index + 1, text: line }))
        .filter(({ text }) => LEGACY_VARS.test(text))
        .map(({ line, text }) => `${line}: ${text.trim()}`);
      expect(offenders).toEqual([]);
    });
  }
});
