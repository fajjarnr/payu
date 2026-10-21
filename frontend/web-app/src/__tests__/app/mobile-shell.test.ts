import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

const read = (rel: string) => readFileSync(join(process.cwd(), rel), 'utf8');

/**
 * Mobile-first rules that jsdom cannot measure (flex shrinking, media
 * queries) — guarded at the source level so a refactor cannot silently
 * squeeze the only nav control on a 360px screen again.
 */
describe('mobile shell rules', () => {
  it('the header keeps the menu button at its 44px target size', () => {
    const layout = read('src/components/DashboardLayout.tsx');
    const trigger = layout.slice(
      layout.indexOf('data-testid="mobile-menu-trigger"')
    );
    expect(trigger).toMatch(/width: 44/);
    expect(trigger).toMatch(/height: 44/);
    // Without this the flex row shrinks the button to 22px (< the 24px floor).
    expect(trigger).toMatch(/flexShrink: 0/);
  });

  it('the header drops the secondary controls below sm so the row still fits', () => {
    const layout = read('src/components/DashboardLayout.tsx');
    expect(layout).toMatch(/hidden sm:contents/);
  });

  it('the brand column is hidden below lg and laid out as a flex column above it', () => {
    const css = read('src/app/globals.css');
    expect(css).toMatch(/\.payu-lg-only\s*\{\s*display: none/);
    expect(css).toMatch(
      /@media \(min-width: 992px\)[\s\S]*?\.payu-lg-only\s*\{\s*display: flex/
    );
    // The column's own inline style must not set display (an inline style
    // beats the class and the panel would show on mobile again).
    for (const file of [
      'src/app/[locale]/login/login-form.tsx',
      'src/app/[locale]/onboarding/page.tsx',
    ]) {
      const source = read(file);
      expect(source).toContain('payu-lg-only');
      const column = source.slice(source.indexOf('payu-lg-only'));
      expect(column.slice(0, 600)).not.toMatch(/display: "flex"/);
    }
  });
});
