import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

/**
 * Accessibility Audit E2E Tests
 *
 * These tests use axe-core via @axe-core/playwright to perform
 * automated accessibility audits on key pages of the PayU application.
 *
 * WCAG 2.1 Level AA Compliance is the target standard.
 *
 * @see https://www.w3.org/WAI/WCAG21/Understanding/
 * @see https://github.com/dequelabs/axe-core-npm/tree/develop/packages/playwright
 */

test.describe('Accessibility Audit - @a11y', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.describe('Login Page', () => {
    test('should not have any automatically detectable accessibility issues', async ({ page }) => {
      await page.goto('/login');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });

    test('should have proper color contrast', async ({ page }) => {
      await page.goto('/login');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withRules(['color-contrast'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });

    test('should have proper form labels', async ({ page }) => {
      await page.goto('/login');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withRules(['label', 'aria-required-attr'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });

    test('should have proper heading structure', async ({ page }) => {
      await page.goto('/login');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withRules(['heading-order'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });

    test('should have keyboard accessible elements', async ({ page }) => {
      await page.goto('/login');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withRules([
          'keyboard',
          'focus-order-semantics',
          'tabindex',
          'region'
        ])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });
  });

  test.describe('Registration Page', () => {
    test('should not have any automatically detectable accessibility issues', async ({ page }) => {
      await page.goto('/onboarding');

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
        .analyze();

      expect(accessibilityScanResults.violations).toEqual([]);
    });
  });

  test.describe('Keyboard Navigation', () => {
    test('should have logical tab order on login page', async ({ page }) => {
      await page.goto('/login');

      // Get all focusable elements
      const focusableElements = await page.locator(
        'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
      ).all();

      expect(focusableElements.length).toBeGreaterThan(0);

      // Test tab navigation
      await page.keyboard.press('Tab');
      const firstFocused = await page.locator(':focus').textContent().catch(() => null);
      expect(firstFocused).toBeTruthy();

      // Continue tabbing through all focusable elements
      for (let i = 0; i < focusableElements.length; i++) {
        await page.keyboard.press('Tab');
        const focused = await page.locator(':focus').isVisible().catch(() => false);
        expect(focused).toBe(true);
      }
    });

    test('should submit form with Enter key', async ({ page }) => {
      await page.goto('/login');

      // Fill in the form
      await page.fill('input[placeholder="Username atau ID Akun"]', 'testuser');
      await page.fill('input[placeholder="••••••••••••"]', 'password123');

      // Press Enter to submit
      await page.keyboard.press('Enter');

      // Should show loading state (form was submitted)
      await expect(page.getByText('Memvalidasi Akun...')).toBeVisible();
    });
  });

  test.describe('Screen Reader Support', () => {
    test('should have proper ARIA landmarks', async ({ page }) => {
      await page.goto('/login');

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

      const lang = await page.locator('html').getAttribute('lang');
      expect(lang).toBeTruthy();
    });
  });

  test.describe('Focus Management', () => {
    test('should have visible focus indicators', async ({ page }) => {
      await page.goto('/login');

      // Get all interactive elements
      const interactiveElements = await page.locator(
        'button, a, input, textarea, select, [tabindex]:not([tabindex="-1"])'
      ).all();

      for (const element of interactiveElements.slice(0, 5)) { // Test first 5 elements
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

        // Element should have some visual focus indicator
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
      // Set mobile viewport
      await page.setViewportSize({ width: 375, height: 667 });
      await page.goto('/login');

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

      const accessibilityScanResults = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa'])
        // Exclude elements that are known to have issues (e.g., third-party widgets)
        .exclude('.third-party-widget')
        .analyze();

      // Report violations but don't fail the test for minor issues
      if (accessibilityScanResults.violations.length > 0) {
        // Intentional console.log for accessibility reporting - suppressing lint
        console.log(`Accessibility violations on ${name}:`,
          accessibilityScanResults.violations.map((v) => ({
            rule: v.id,
            impact: v.impact,
            description: v.description,
            nodes: v.nodes.length
          }))
        );
      }

      // Only fail for critical or serious violations
      const criticalViolations = accessibilityScanResults.violations.filter(
        (v) => v.impact === 'critical' || v.impact === 'serious'
      );

      expect(criticalViolations).toEqual([]);
    });
  }
});
