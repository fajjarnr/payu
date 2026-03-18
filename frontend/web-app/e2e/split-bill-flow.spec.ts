/**
 * E2E Tests: Split Bill Flow (/split-bill)
 * Covers BUG-TEST-092
 */
import { test, expect } from './fixtures';
import { waitForPageStable, waitForAnimations } from './utils';

test.describe('Split Bill Flow', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/split-bill');
    await waitForPageStable(page);
  });

  test('should display split bill page with heading', async ({ authPage: page }) => {
    await expect(page.getByRole('heading', { name: 'Split Bill', exact: true })).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Bagi tagihan dengan teman, keluarga, atau rekan kerja secara adil.')).toBeVisible();
  });

  test('should display stats cards', async ({ authPage: page }) => {
    await expect(page.getByText('Aktif')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Lunas')).toBeVisible();
    await expect(page.getByText('Total')).toBeVisible();
  });

  test('should display new split bill button', async ({ authPage: page }) => {
    await expect(page.getByText('Split Bill Baru')).toBeVisible({ timeout: 10000 });
  });

  test('should show empty state or active bills section', async ({ authPage: page }) => {
    const hasActiveBills = await page.getByText('Split Bill Aktif').isVisible().catch(() => false);
    const hasEmptyState = await page.getByText('Belum ada Split Bill').isVisible().catch(() => false);
    expect(hasActiveBills || hasEmptyState).toBeTruthy();
  });

  test('should open create modal when clicking new button', async ({ authPage: page }) => {
    await page.getByText('Split Bill Baru').click();
    await waitForAnimations(page);
    await expect(page.getByText('Buat Split Bill Baru')).toBeVisible({ timeout: 10000 });
    await expect(page.getByPlaceholder('Makan siang, nonton bareng...')).toBeVisible();
    await expect(page.getByPlaceholder('150000')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Buat', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Batal' })).toBeVisible();
  });
});
