/**
 * E2E Tests: Scheduled Transfers (/scheduled-transfers)
 * Covers BUG-TEST-094
 */
import { test, expect } from './fixtures';
import { waitForPageStable } from './utils';

test.describe('Scheduled Transfers Flow', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/scheduled-transfers');
    await waitForPageStable(page);
  });

  test('should display scheduled transfers page with heading', async ({ authPage: page }) => {
    await expect(page.getByText('Transfer Terjadwal')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Kelola dan pantau transfer berulang Anda.')).toBeVisible();
  });

  test('should display stats cards', async ({ authPage: page }) => {
    await expect(page.getByText('Total Transfer')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Aktif')).toBeVisible();
    await expect(page.getByText('Dijeda')).toBeVisible();
    await expect(page.getByText('Selesai')).toBeVisible();
  });

  test('should display new transfer button', async ({ authPage: page }) => {
    await expect(page.getByText('Transfer Baru')).toBeVisible({ timeout: 10000 });
  });

  test('should show transfer list or empty state', async ({ authPage: page }) => {
    const hasList = await page.getByText('Daftar Transfer Terjadwal').isVisible().catch(() => false);
    const hasEmpty = await page.getByText('Belum ada transfer terjadwal').isVisible().catch(() => false);
    expect(hasList || hasEmpty).toBeTruthy();
  });

  test('should navigate to transfer page on new transfer click', async ({ authPage: page }) => {
    await page.getByText('Transfer Baru').click();
    await expect(page).toHaveURL(/.*\/transfer/);
  });
});
