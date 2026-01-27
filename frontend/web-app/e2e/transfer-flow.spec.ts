import { test, expect } from '@playwright/test';

test.describe('Transfer Flow', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/transfer');
  });

  test('should display transfer page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Transfer Instan')).toBeVisible();
    await expect(page.getByText('Kirim dana secara aman dalam hitungan detik')).toBeVisible();
  });

  test('should display all transfer types', async ({ page }) => {
    await expect(page.getByText('Transfer Instan')).toBeVisible();
    await expect(page.getByText('BI-FAST')).toBeVisible();
    await expect(page.getByText('SKN')).toBeVisible();
    await expect(page.getByText('RTGS')).toBeVisible();
  });

  test('should select transfer type', async ({ page }) => {
    await page.click('button:has-text("BI-FAST")');
    await expect(page.locator('.border-primary').first()).toBeVisible();
  });

  test('should display schedule options', async ({ page }) => {
    await expect(page.getByText('Sekarang')).toBeVisible();
    await expect(page.getByText('Terjadwal')).toBeVisible();
    await expect(page.getByText('Berulang')).toBeVisible();
  });

  test('should show date picker when scheduled transfer selected', async ({ page }) => {
    await page.click('button:has-text("Terjadwal")');
    await expect(page.getByLabel('Tanggal Transfer')).toBeVisible();
  });

  test('should show recurring inputs when recurring transfer selected', async ({ page }) => {
    await page.click('button:has-text("Berulang")');
    await expect(page.getByPlaceholder('1-31')).toBeVisible();
    await expect(page.getByPlaceholder('1-12')).toBeVisible();
  });

  test('should display favorite contacts', async ({ page }) => {
    await expect(page.getByText('Penerima Favorit')).toBeVisible();
    await expect(page.getByText('Anya')).toBeVisible();
    await expect(page.getByText('Budi')).toBeVisible();
    await expect(page.getByText('Citra')).toBeVisible();
    await expect(page.getByText('Dodi')).toBeVisible();
  });

  test('should select contact from favorites', async ({ page }) => {
    await page.click('text=Anya');
    const input = page.getByPlaceholder('Masukkan ID Akun atau Nomor Rekening');
    await expect(input).toHaveValue('acc-any123');
  });

  test('should validate transfer amount', async ({ page }) => {
    await page.click('button:has-text("Tinjau Ringkasan Transfer")');

    const toast = page.locator('[role="alert"], .text-warning');
    await expect(toast).toContainText('Silakan pilih penerima');
  });

  test('should show review page with valid data', async ({ page }) => {
    await page.fill('input[placeholder="Masukkan ID Akun atau Nomor Rekening"]', 'acc-any123');
    await page.fill('input[placeholder="0"]', '50000');

    await page.click('button:has-text("Tinjau Ringkasan Transfer")');

    await expect(page.getByText('Tinjau Transfer')).toBeVisible();
    await expect(page.getByText('Rp 50.000')).toBeVisible();
  });

  test('should display transfer fee information', async ({ page }) => {
    await page.click('button:has-text("BI-FAST")');
    await expect(page.getByText('Rp 5.000')).toBeVisible();
  });

  test('should return to form from review page', async ({ page }) => {
    await page.fill('input[placeholder="Masukkan ID Akun atau Nomor Rekening"]', 'acc-any123');
    await page.fill('input[placeholder="0"]', '50000');

    await page.click('button:has-text("Tinjau Ringkasan Transfer")');
    const backButton = page.locator('button:has-text("Tinjau Transfer")').locator('..').locator('button').first();
    await backButton.click();

    await expect(page.getByText('Transfer Instan')).toBeVisible();
  });

  test('should allow adding memo to transaction', async ({ page }) => {
    await page.fill('input[placeholder="Apa tujuan transfer ini?"]', 'Test transfer');
    const memoInput = page.getByPlaceholder('Apa tujuan transfer ini?');
    await expect(memoInput).toHaveValue('Test transfer');
  });

  test('should show help section', async ({ page }) => {
    await expect(page.getByText('Bantuan?')).toBeVisible();
    await expect(page.getByText('Hubungi Kami')).toBeVisible();
  });
});
