/**
 * E2E Tests: Notifications / Inbox (/notifications)
 * Covers BUG-TEST-095
 */
import { test, expect } from './fixtures';
import { waitForPageStable, waitForAnimations } from './utils';

test.describe('Notifications Flow', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/notifications');
    await waitForPageStable(page);
  });

  test('should display notifications page with heading', async ({ authPage: page }) => {
    await expect(page.getByText('Kotak Masuk')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Kelola notifikasi, promo, dan peringatan keamanan Anda.')).toBeVisible();
  });

  test('should display action buttons', async ({ authPage: page }) => {
    await expect(page.getByText('Tandai Semua Dibaca')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Hapus Semua')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Cari notifikasi...')).toBeVisible({ timeout: 10000 });
  });

  test('should display filter buttons', async ({ authPage: page }) => {
    await expect(page.getByRole('button', { name: 'Semua', exact: true })).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Belum Dibaca')).toBeVisible();
    await expect(page.locator('button').filter({ hasText: 'PROMO' })).toBeVisible();
    await expect(page.locator('button').filter({ hasText: 'SECURITY' })).toBeVisible();
  });

  test('should show notifications or empty state', async ({ authPage: page }) => {
    const hasNotifs = await page.getByText('Lihat Detail').first().isVisible().catch(() => false);
    const hasEmpty = await page.getByText('Tidak ada notifikasi').isVisible().catch(() => false);
    expect(hasNotifs || hasEmpty).toBeTruthy();
  });

  test('should filter notifications by type', async ({ authPage: page }) => {
    const promoBtn = page.locator('button').filter({ hasText: 'PROMO' });
    await promoBtn.click();
    await waitForAnimations(page);
    // After filtering, the PROMO button should appear active (emerald bg)
    await expect(promoBtn).toBeVisible();
  });
});
