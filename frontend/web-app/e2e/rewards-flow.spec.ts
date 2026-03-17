/**
 * E2E Tests: Rewards & Gamification (/rewards)
 * Covers BUG-TEST-096
 */
import { test, expect } from './fixtures';
import { waitForPageStable, waitForAnimations } from './utils';

test.describe('Rewards Flow', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/rewards');
    await waitForPageStable(page);
  });

  test('should display rewards page with heading', async ({ authPage: page }) => {
    await expect(page.getByText('Rewards & Gamifikasi')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Kumpulkan poin, dapatkan cashback, dan raih lebih banyak keuntungan.')).toBeVisible();
  });

  test('should display three tabs', async ({ authPage: page }) => {
    await expect(page.getByText('Poin Loyalty')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Cashback')).toBeVisible();
    await expect(page.getByText('Referral')).toBeVisible();
  });

  test('should display points tab content by default', async ({ authPage: page }) => {
    await expect(page.getByText('Saldo Poin')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Total Diperoleh')).toBeVisible();
    await expect(page.getByText('Total Ditukar')).toBeVisible();
  });

  test('should display earn points guide', async ({ authPage: page }) => {
    await expect(page.getByText('Cara Mendapatkan Poin')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Transaksi Rutin')).toBeVisible();
    await expect(page.getByText('Login Harian')).toBeVisible();
    await expect(page.getByText('Referral Teman')).toBeVisible();
    await expect(page.getByText('Event Khusus')).toBeVisible();
  });

  test('should display points history section', async ({ authPage: page }) => {
    await expect(page.getByText('Riwayat Poin')).toBeVisible({ timeout: 10000 });
  });

  test('should switch to cashback tab', async ({ authPage: page }) => {
    await page.getByText('Cashback').click();
    await waitForAnimations(page);
    await expect(page.getByText('Total Cashback')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Dikreditkan')).toBeVisible();
    await expect(page.getByText('Menunggu')).toBeVisible();
  });

  test('should display cashback history', async ({ authPage: page }) => {
    await page.getByText('Cashback').click();
    await waitForAnimations(page);
    await expect(page.getByText('Riwayat Cashback')).toBeVisible({ timeout: 10000 });
  });

  test('should display active promotions in cashback tab', async ({ authPage: page }) => {
    await page.getByText('Cashback').click();
    await waitForAnimations(page);
    await expect(page.getByText('Promosi Aktif')).toBeVisible({ timeout: 10000 });
  });

  test('should switch to referral tab', async ({ authPage: page }) => {
    await page.getByText('Referral').click();
    await waitForAnimations(page);
    await expect(page.getByText('Kode Referral Anda')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Bagikan Link Referral')).toBeVisible();
  });

  test('should display referral summary', async ({ authPage: page }) => {
    await page.getByText('Referral').click();
    await waitForAnimations(page);
    await expect(page.getByText('Ringkasan Referral')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Berhasil Bergabung')).toBeVisible();
    await expect(page.getByText('Menunggu Konfirmasi')).toBeVisible();
    await expect(page.getByText('Total Penghasilan')).toBeVisible();
  });
});
