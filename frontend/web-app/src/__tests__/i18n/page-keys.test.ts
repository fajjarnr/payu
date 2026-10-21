import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import idMessages from '../../../messages/id.json';
import enMessages from '../../../messages/en.json';

const APP_DIR = join(process.cwd(), 'src/app/[locale]');

type Catalog = Record<string, unknown>;

function pageFiles(dir: string): string[] {
  const out: string[] = [];
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      out.push(...pageFiles(full));
    } else if (entry === 'page.tsx') {
      out.push(full);
    }
  }
  return out;
}

function resolve(catalog: Catalog, key: string): unknown {
  return key
    .split('.')
    .reduce<unknown>(
      (node, part) =>
        node && typeof node === 'object'
          ? (node as Record<string, unknown>)[part]
          : undefined,
      catalog
    );
}

function hasKey(catalog: Catalog, key: string): boolean {
  if (resolve(catalog, key) !== undefined) return true;
  // A key with a dynamic segment (e.g. `months.${month}`) resolves as a
  // prefix: at least one entry must live under it.
  const prefix = key.split('.').slice(0, -1).join('.');
  const parent = prefix ? resolve(catalog, prefix) : catalog;
  return Boolean(parent && typeof parent === 'object');
}

/** Namespace + literal keys used by a page, from `t("x")` / `t('x')` calls. */
function usedKeys(source: string): { namespace: string; key: string }[] {
  const namespaces = [...source.matchAll(/useTranslations\(\s*["']([^"']+)["']\s*\)/g)].map(
    (m) => m[1]
  );
  const namespace = namespaces[0];
  if (!namespace) return [];
  const keys = [...source.matchAll(/\bt\(\s*["']([^"']+)["']/g)].map((m) => m[1]);
  // Template literals: keep the static prefix (`months.${month}` -> `months.`).
  const dynamic = [...source.matchAll(/\bt\(\s*`([^`$]*)\$\{/g)].map((m) => `${m[1]}`);
  return [...keys, ...dynamic].map((key) => ({ namespace, key }));
}

describe('customer pages use existing message keys', () => {
  const files = pageFiles(APP_DIR);

  it('finds the customer route files', () => {
    expect(files.length).toBeGreaterThan(30);
  });

  for (const file of files) {
    const rel = file.replace(process.cwd(), '');
    const source = readFileSync(file, 'utf8');
    const usages = usedKeys(source);
    if (usages.length === 0) continue;

    it(`${rel} resolves every t() key in id and en`, () => {
      const missing: string[] = [];
      for (const { namespace, key } of usages) {
        for (const [locale, catalog] of [
          ['id', idMessages as Catalog],
          ['en', enMessages as Catalog],
        ] as const) {
          const scoped = resolve(catalog, namespace) as Catalog | undefined;
          if (!scoped || !hasKey(scoped, key)) {
            missing.push(`${locale}:${namespace}.${key}`);
          }
        }
      }
      expect(missing).toEqual([]);
    });
  }
});
