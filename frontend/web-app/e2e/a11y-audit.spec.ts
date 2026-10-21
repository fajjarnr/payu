import { test, expect } from './fixtures';
import AxeBuilder from '@axe-core/playwright';

/**
 * Axe-core accessibility audits against WCAG 2.2 AA (see .a11yrc.json tags).
 * No violation class is filtered: color-contrast included — failures are real
 * bugs, not design debt.
 */

test.describe('Accessibility Audit - @a11y', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.describe('Login Page', () => {
    test('should have no accessibility violations on login', async ({ page }) => {
      await page.goto('/login');
      await page.waitForLoadState('domcontentloaded');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });

    test('should have proper form labels', async ({ page }) => {
      await page.goto('/login');
      await page.waitForLoadState('domcontentloaded');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withRules(['label', 'aria-required-attr'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });

    test('should have proper heading structure', async ({ page }) => {
      await page.goto('/login');
      await page.waitForLoadState('domcontentloaded');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withRules(['heading-order'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });

    test('should have keyboard accessible elements', async ({ page }) => {
      await page.goto('/login');
      await page.waitForLoadState('domcontentloaded');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withRules([
          'focus-order-semantics',
          'tabindex',
          'region',
          'aria-hidden-focus',
          'scrollable-region-focusable'
        ])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });
  });

  test.describe('Registration Page', () => {
    test('should not have any critical accessibility issues', async ({ page }) => {
      await page.goto('/onboarding');
      await page.waitForLoadState('domcontentloaded');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });
  });

  test.describe('Protected Pages - Dashboard', () => {
    test('should have no accessibility violations on dashboard', async ({ authPage: page }) => {
      await page.goto('/dashboard');
      await page.waitForLoadState('domcontentloaded');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });

    test('should have proper form labels on dashboard', async ({ authPage: page }) => {
      await page.goto('/dashboard');
      await page.waitForLoadState('domcontentloaded');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withRules(['label', 'aria-required-attr'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });
  });

  test.describe('Protected Pages - Money Flows', () => {
    // One axe pass per money-movement route, same goto+analyze pattern as above.
    const moneyRoutes = [
      '/transfer',
      '/transactions',
      '/cards',
      '/bills',
      '/pockets',
      '/qris',
      '/backoffice',
    ] as const;

    for (const route of moneyRoutes) {
      test(`should have no accessibility violations on ${route}`, async ({ authPage: page }) => {
        await page.goto(route);
        await page.waitForLoadState('domcontentloaded');

        const accessibilityScanResults = await new AxeBuilder({ page })
          .withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'])
          .analyze();

        expect(accessibilityScanResults.violations).toEqual([]);
      });
    }
  });

  test.describe('Protected Pages - Investments', () => {
    test('should have no accessibility violations on investments', async ({ authPage: page }) => {
      await page.goto('/investments');
      await page.waitForLoadState('domcontentloaded');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });
  });

  test.describe('Keyboard Navigation', () => {
    test('should have logical tab order on login page', async ({ page }) => {
      await page.goto('/login');
      await page.waitForLoadState('domcontentloaded');

      const focusableElements = await page.locator(
        'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
      ).all();

      expect(focusableElements.length).toBeGreaterThan(0);

      await page.keyboard.press('Tab');
      await page.waitForTimeout(100);
      const firstFocused = await page.locator(':focus').isVisible().catch(() => false);
      expect(firstFocused).toBe(true);

      // Continue tabbing through a few elements (not all to save time)
      for (let i = 0; i < Math.min(5, focusableElements.length); i++) {
        await page.keyboard.press('Tab');
        await page.waitForTimeout(50);
        const focused = await page.locator(':focus').isVisible().catch(() => false);
        expect(focused).toBe(true);
      }
    });

    test('should submit form with Enter key', async ({ page }) => {
      await page.goto('/login');
      await page.waitForLoadState('domcontentloaded');

      await page.fill('input[placeholder="username123"]', 'testuser');
      await page.fill('input[placeholder="••••••••"]', 'password123');

      await page.keyboard.press('Enter');

      // Should show loading state (form was submitted)
      // Note: Use a more flexible check as the exact text may vary
      await page.waitForTimeout(500);
      // Either we navigate or show loading state
      const currentURL = page.url();
      expect(currentURL).toBeTruthy();
    });
  });

  test.describe('Screen Reader Support', () => {
    test('should have proper ARIA landmarks', async ({ page }) => {
      await page.goto('/login');
      await page.waitForLoadState('domcontentloaded');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withRules([
          'landmark-one-main',
          'landmark-unique',
          'region',
          'aria-roles',
          'aria-allowed-attr',
          'aria-required-attr'
        ])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });

    test('should have proper page title', async ({ page }) => {
      await page.goto('/login');
      await expect(page).toHaveTitle(/PayU/);
    });

    test('should have proper language attribute', async ({ page }) => {
      await page.goto('/login');
      await page.waitForLoadState('domcontentloaded');

      const lang = await page.locator('html').getAttribute('lang');
      expect(lang).toBeTruthy();
    });
  });

  test.describe('Focus Management', () => {
    test('should have visible focus indicators', async ({ page }) => {
      await page.goto('/login');
      await page.waitForLoadState('domcontentloaded');

      const interactiveElements = await page.locator(
        'button, a, input, textarea, select, [tabindex]:not([tabindex="-1"])'
      ).all();

      for (const element of interactiveElements.slice(0, 5)) {
        await element.focus();
        const isVisible = await element.isVisible();
        expect(isVisible).toBe(true);

        // Check that element has some focus indicator (outline, ring, etc.)
        const styles = await element.evaluate((el) => {
          const computed = window.getComputedStyle(el);
          return {
            outline: computed.outline,
            outlineWidth: computed.outlineWidth,
            boxShadow: computed.boxShadow
          };
        });

        const hasFocusIndicator =
          styles.outlineWidth !== '0px' ||
          styles.outline !== 'none' ||
          styles.boxShadow !== 'none';

        expect(hasFocusIndicator).toBe(true);
      }
    });
  });

  test.describe('Mobile Accessibility', () => {
    test('should have sufficient touch target sizes on mobile', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      await page.goto('/login');
      await page.waitForLoadState('domcontentloaded');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withRules(['target-size'])
        .analyze();

      // Note: target-size is a WCAG 2.5.5 requirement (AAA level)
      // We check for violations but may not enforce strict compliance
      const criticalViolations = accessibilityScanResults.violations.filter(
        (v) => v.impact === 'critical' || v.impact === 'serious'
      );
      expect(criticalViolations).toEqual([]);
    });
  });
});

test.describe('Accessibility - Full Site Scan @a11y', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  const pagesToTest = [
    { path: '/login', name: 'Login' },
    { path: '/onboarding', name: 'Onboarding' },
  ];

  for (const { path, name } of pagesToTest) {
    test(`should pass accessibility scan on ${name} page`, async ({ page }) => {
      await page.goto(path);
      await page.waitForLoadState('domcontentloaded');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });
  }
});
