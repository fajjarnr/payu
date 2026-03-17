/**
 * E2E Tests: Analytics / Financial Intelligence (/analytics)
 * Covers BUG-TEST-093
 */
import { test, expect } from './fixtures';
import { waitForPageStable } from './utils';

test.describe('Analytics Page Flow', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/analytics');
    await waitForPageStable(page);
  });

  test('should display analytics page with heading', async ({ authPage: page }) => {
    await expect(page.getByText('Intelijen Keuangan')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Wawasan mendalam tentang kebiasaan pengeluaran dan pertumbuhan kekayaan Anda.')).toBeVisible();
  });

  test('should display income and expense stats', async ({ authPage: page }) => {
    await expect(page.getByText('Total Pemasukan')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Total Pengeluaran')).toBeVisible();
    await expect(page.getByText('Tabungan Bulanan')).toBeVisible();
    await expect(page.getByText('ROI Investasi')).toBeVisible();
  });

  test('should display spending trajectory chart section', async ({ authPage: page }) => {
    await expect(page.getByText('Trajektori Pengeluaran')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Analisis arus kas harian periode ini')).toBeVisible();
  });

  test('should display chart legends', async ({ authPage: page }) => {
    await expect(page.getByText('Masuk')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Keluar')).toBeVisible();
  });

  test('should display spending breakdown section', async ({ authPage: page }) => {
    await expect(page.getByText('Rincian Pengeluaran')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Total Keluar')).toBeVisible();
  });

  test('should display connection status indicator', async ({ authPage: page }) => {
    const isLive = await page.getByText('Live Update').isVisible().catch(() => false);
    const isOffline = await page.getByText('Offline').isVisible().catch(() => false);
    expect(isLive || isOffline).toBeTruthy();
  });

  test('should display AI savings suggestion', async ({ authPage: page }) => {
    await expect(page.getByText('Siap untuk menabung otomatis?')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Terapkan Optimasi')).toBeVisible();
  });

  test('should display date selector', async ({ authPage: page }) => {
    await expect(page.getByText('Januari 2026')).toBeVisible({ timeout: 10000 });
  });
});
