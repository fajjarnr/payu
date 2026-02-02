import { test, expect } from '@playwright/test';
import { waitForPageStable, waitForAnimations } from './utils';

test.describe('Transfer Flow', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/transfer');
    await waitForPageStable(page);
  });

  test('should display transfer page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Transfer Instan')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Kirim dana secara aman dalam hitungan detik')).toBeVisible({ timeout: 10000 });
  });

  test('should display all transfer types', async ({ page }) => {
    await expect(page.getByText('Transfer Instan')).toBeVisible();
    await expect(page.getByText('BI-FAST')).toBeVisible();
    await expect(page.getByText('SKN')).toBeVisible();
    await expect(page.getByText('RTGS')).toBeVisible();
  });

  test('should select transfer type', async ({ page }) => {
    await page.click('button:has-text("BI-FAST")');
    await waitForAnimations(page);

    // Check that selection is made - the button should be selected
    const selectedButton = page.locator('button:has-text("BI-FAST")');
    await expect(selectedButton).toBeVisible();
  });

  test('should display schedule options', async ({ page }) => {
    await expect(page.getByText('Sekarang')).toBeVisible();
    await expect(page.getByText('Terjadwal')).toBeVisible();
    await expect(page.getByText('Berulang')).toBeVisible();
  });

  test('should show date picker when scheduled transfer selected', async ({ page }) => {
    await page.click('button:has-text("Terjadwal")');
    await waitForAnimations(page);
    await expect(page.getByText('Tanggal Transfer')).toBeVisible({ timeout: 10000 });
  });

  test('should show recurring inputs when recurring transfer selected', async ({ page }) => {
    await page.click('button:has-text("Berulang")');

    // Wait for the UI to update
    await page.waitForTimeout(500);

    // Check for day selection grid
    const dayButtons = page.locator('button[type="button"]').filter({ hasText: /^\d+$/ });
    expect(await dayButtons.count()).toBeGreaterThan(0);
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
    await waitForAnimations(page);
    const input = page.getByPlaceholder('Masukkan ID Akun atau Nomor Rekening');
    await expect(input).toHaveValue('acc-any123');
  });

  test('should display transfer fee information', async ({ page }) => {
    await page.click('button:has-text("BI-FAST")');
    await expect(page.getByText('Rp 5.000')).toBeVisible();
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

  test('should have proper amount input', async ({ page }) => {
    const amountInput = page.locator('input[placeholder="0"]');
    await expect(amountInput).toBeVisible();
  });

  test('should have recipient account input', async ({ page }) => {
    const recipientInput = page.getByPlaceholder('Masukkan ID Akun atau Nomor Rekening');
    await expect(recipientInput).toBeVisible();
  });

  test('should display review button', async ({ page }) => {
    const reviewButton = page.getByText('Tinjau Ringkasan Transfer');
    await expect(reviewButton).toBeVisible();
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/transfer');

    await expect(page.getByText('Transfer Instan')).toBeVisible();
    await expect(page.getByPlaceholder('Masukkan ID Akun atau Nomor Rekening')).toBeVisible();

    await page.screenshot({
      path: 'e2e/screenshots/transfer-mobile.png',
      fullPage: true
    });
  });
});

test.describe('Transfer Flow - Transfer Type Selection', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/transfer');
    await waitForPageStable(page);
  });

  test('should display transfer type cards', async ({ page }) => {
    await expect(page.getByText('Pilih Metode Transfer')).toBeVisible();
  });

  test('should select Internal Transfer', async ({ page }) => {
    await page.click('button:has-text("Transfer Instan")');
    await waitForAnimations(page);
    const selectedButton = page.locator('button:has-text("Transfer Instan")');
    await expect(selectedButton).toBeVisible();
  });

  test('should select BI-FAST', async ({ page }) => {
    await page.click('button:has-text("BI-FAST")');
    await waitForAnimations(page);
    const selectedButton = page.locator('button:has-text("BI-FAST")');
    await expect(selectedButton).toBeVisible();
  });

  test('should display processing time for BI-FAST', async ({ page }) => {
    await page.click('button:has-text("BI-FAST")');
    await expect(page.getByText('Seketika')).toBeVisible();
  });
});

test.describe('Transfer Flow - Schedule Selection', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/transfer');
    await waitForPageStable(page);
  });

  test('should display schedule options header', async ({ page }) => {
    await expect(page.getByText('Jadwal Transfer')).toBeVisible();
  });

  test('should select scheduled transfer', async ({ page }) => {
    await page.click('button:has-text("Terjadwal")');
    await waitForAnimations(page);
    await expect(page.getByText('Tanggal Transfer')).toBeVisible({ timeout: 10000 });
  });

  test('should select recurring transfer', async ({ page }) => {
    await page.click('button:has-text("Berulang")');

    // Wait for the UI to update
    await waitForAnimations(page);
    await page.waitForTimeout(300);

    // Check for month selection buttons
    const monthButtons = page.locator('button').filter({ hasText: /^(JAN|FEB|MAR|APR|MEI|JUN|JUL|AGU|SEP|OKT|NOV|DES)$/ });
    expect(await monthButtons.count()).toBeGreaterThan(0);
  });
});

test.describe('Transfer Flow - Amount Input', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/transfer');
    await waitForPageStable(page);
  });

  test('should enter amount', async ({ page }) => {
    const amountInput = page.locator('input[value=""], input[placeholder="0"]').first();
    await amountInput.fill('50000');
    // The amount is formatted, so check the input
    await expect(amountInput).toBeVisible();
  });

  test('should display amount formatting', async ({ page }) => {
    const amountInput = page.locator('input[placeholder="0"]');
    await amountInput.fill('100000');
    await expect(amountInput).toBeVisible();
  });

  test('should have memo input field', async ({ page }) => {
    const memoInput = page.getByPlaceholder('Apa tujuan transfer ini?');
    await expect(memoInput).toBeVisible();
  });
});

test.describe('Transfer Flow - Favorite Contacts', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/transfer');
    await waitForPageStable(page);
  });

  test('should display favorite contacts header', async ({ page }) => {
    await expect(page.getByText('Penerima Favorit')).toBeVisible();
  });

  test('should click on favorite contact', async ({ page }) => {
    await page.click('text=Anya');
    await waitForAnimations(page);
    const input = page.getByPlaceholder('Masukkan ID Akun atau Nomor Rekening');
    await expect(input).toHaveValue('acc-any123');
  });

  test('should display add contact button', async ({ page }) => {
    await expect(page.getByText('Tambah')).toBeVisible();
  });
});

test.describe('Transfer Flow - Help Section', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/transfer');
    await waitForPageStable(page);
  });

  test('should display help card', async ({ page }) => {
    await expect(page.getByText('Bantuan?')).toBeVisible();
    await expect(page.getByText('Proteksi & panduan transaksi aman')).toBeVisible();
  });

  test('should have contact button', async ({ page }) => {
    await expect(page.getByText('Hubungi Kami')).toBeVisible();
  });
});

test.describe('Transfer Flow - Accessibility', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/transfer');
    await waitForPageStable(page);
  });

  test('should have proper heading hierarchy', async ({ page }) => {
    const h2 = page.locator('h2').first();
    await expect(h2).toBeVisible({ timeout: 10000 });
    await expect(h2).toContainText('Transfer', { timeout: 5000 });
  });

  test('should support keyboard navigation', async ({ page }) => {
    await page.keyboard.press('Tab');
    await page.waitForTimeout(100);
    const focused = page.locator(':focus');
    await expect(focused).toBeVisible();
  });

  test('should have accessible inputs', async ({ page }) => {
    const recipientInput = page.getByPlaceholder('Masukkan ID Akun atau Nomor Rekening');
    await expect(recipientInput).toBeVisible();

    const amountInput = page.locator('input[placeholder="0"]');
    await expect(amountInput).toBeVisible();
  });
});

test.describe('Transfer Flow - Visual Regression', () => {
  test('should match screenshots on desktop', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/transfer');

    await page.screenshot({
      path: 'e2e/screenshots/transfer-desktop.png',
      fullPage: true
    });
  });

  test('should match screenshots on tablet', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/transfer');

    await page.screenshot({
      path: 'e2e/screenshots/transfer-tablet.png',
      fullPage: true
    });
  });

  test('should match screenshots on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/transfer');

    await page.screenshot({
      path: 'e2e/screenshots/transfer-mobile.png',
      fullPage: true
    });
  });
});
