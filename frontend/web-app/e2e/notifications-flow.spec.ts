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
    // The type filter is an AntD <Segmented> (role=radiogroup), which renders one
    // radio per option (Semua / Belum Dibaca / Promo / Keamanan) — not buttons.
    // Scope to the radiogroup: the dashboard menu also has a "Keamanan" entry.
    const filters = page.getByRole('radiogroup');
    await expect(filters.getByText('Semua', { exact: true })).toBeVisible({ timeout: 10000 });
    await expect(filters.getByText('Belum Dibaca', { exact: true })).toBeVisible();
    await expect(filters.getByText('Promo', { exact: true })).toBeVisible();
    await expect(filters.getByText('Keamanan', { exact: true })).toBeVisible();
    // ALL is the default selection
    await expect(page.getByRole('radio', { name: 'Semua', exact: true })).toBeChecked();
  });

  test('should show notifications or empty state', async ({ authPage: page }) => {
    const hasNotifs = await page.getByText('Lihat Detail').first().isVisible().catch(() => false);
    const hasEmpty = await page.getByText('Tidak ada notifikasi').isVisible().catch(() => false);
    expect(hasNotifs || hasEmpty).toBeTruthy();
  });

  test('should filter notifications by type', async ({ authPage: page }) => {
    // Filter by PROMO: pick the Segmented option, then assert the selection took
    // effect and that no other notification type survives the filter.
    await page.getByRole('radiogroup').getByText('Promo', { exact: true }).click();
    await waitForAnimations(page);
    await expect(page.getByRole('radio', { name: 'Promo', exact: true })).toBeChecked();
    // Remaining rows carry their raw type as a tag (PROMO / SECURITY / ALERT / INFO)
    await expect(page.getByText(/^(SECURITY|ALERT|INFO)$/)).toHaveCount(0);
  });
});
