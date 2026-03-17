/**
 * E2E Tests: Currency Exchange Flow (/exchange)
 * Covers BUG-TEST-091
 */
import { test, expect } from './fixtures';
import { waitForPageStable } from './utils';

test.describe('Exchange Flow', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/exchange');
    await waitForPageStable(page);
  });

  test('should display exchange page with heading and description', async ({ authPage: page }) => {
    await expect(page.getByText('Currency Exchange')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Real-time foreign exchange rates with competitive pricing.')).toBeVisible();
  });

  test('should display exchange calculator section', async ({ authPage: page }) => {
    await expect(page.getByText('Exchange Calculator')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('aria-label=From currency')).toBeVisible();
    await expect(page.locator('aria-label=To currency')).toBeVisible();
    await expect(page.locator('aria-label=Amount to exchange')).toBeVisible();
  });

  test('should display market status section', async ({ authPage: page }) => {
    await expect(page.getByText('Market Status')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Last Update')).toBeVisible();
    await expect(page.getByText('Supported Pairs')).toBeVisible();
  });

  test('should display exchange CTA button', async ({ authPage: page }) => {
    await expect(page.getByText('Exchange Currency Now')).toBeVisible({ timeout: 10000 });
  });

  test('should display recent exchanges section', async ({ authPage: page }) => {
    await expect(page.getByText('Recent Exchanges')).toBeVisible({ timeout: 10000 });
  });

  test('should display exchange information panel', async ({ authPage: page }) => {
    await expect(page.getByText('Exchange rates are updated every 60 seconds')).toBeVisible({ timeout: 10000 });
  });

  test('should have swap currencies button', async ({ authPage: page }) => {
    await expect(page.locator('button[aria-label="Swap currencies"]')).toBeVisible({ timeout: 10000 });
  });

  test('should show help section', async ({ authPage: page }) => {
    await expect(page.getByText('Need Help?')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Contact Support')).toBeVisible();
  });
});
