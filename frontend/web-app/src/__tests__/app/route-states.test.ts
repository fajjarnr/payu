import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

const APP_DIR = join(process.cwd(), 'src/app/[locale]');

/** Every route segment that ships an error.tsx / loading.tsx. */
function routeStateFiles(name: 'error.tsx' | 'loading.tsx'): string[] {
  const found: string[] = [];
  const walk = (dir: string) => {
    for (const entry of readdirSync(dir)) {
      const full = join(dir, entry);
      if (statSync(full).isDirectory()) {
        walk(full);
      } else if (entry === name) {
        found.push(full);
      }
    }
  };
  walk(APP_DIR);
  return found;
}

// Zero-list stems from docs/ANT_GUIDELINES.md §6 A-01.
const STEMS =
  /bank-green|bank-emerald|bg-muted|text-muted-foreground|border-border|bg-card|opacity-60/;

describe('route states delegate to the shared atoms', () => {
  const errors = routeStateFiles('error.tsx');
  const loadings = routeStateFiles('loading.tsx');

  it('covers every route segment that has an error boundary', () => {
    expect(errors.length).toBeGreaterThan(30);
  });

  it('covers every route segment that has a loading state', () => {
    expect(loadings.length).toBeGreaterThan(30);
  });

  for (const file of errors) {
    it(`${file.replace(process.cwd(), '')} uses the shared RouteError`, () => {
      const source = readFileSync(file, 'utf8');
      expect(source).toContain('@/components/RouteError');
      expect(source).not.toMatch(STEMS);
    });
  }

  for (const file of loadings) {
    it(`${file.replace(process.cwd(), '')} uses the shared LoadingState`, () => {
      const source = readFileSync(file, 'utf8');
      expect(source).toContain('@/components/LoadingState');
      expect(source).not.toMatch(STEMS);
    });
  }
});
