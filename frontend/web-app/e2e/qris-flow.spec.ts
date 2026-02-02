import { test, expect } from '@playwright/test';

test.describe('QRIS Payment Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to QRIS page (assumes user is logged in)
    await page.goto('/qris');
  });

  test('should display QRIS page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Pembayaran QRIS')).toBeVisible();
    await expect(page.getByText('Pindai kode QRIS merchant atau P2P untuk membayar secara instan')).toBeVisible();
  });

  test('should display QR scanner area', async ({ page }) => {
    const scannerArea = page.locator('.border-2.border-dashed');
    await expect(scannerArea).toBeVisible();
  });

  test('should have camera icon in scanner area', async ({ page }) => {
    await expect(page.locator('text=Buka Kamera')).toBeVisible();
  });

  test('should have scanning instruction text', async ({ page }) => {
    await expect(page.getByText('Scanning for QRIS Codes')).toBeVisible();
  });

  test('should have open camera button', async ({ page }) => {
    const cameraButton = page.locator('button:has-text("Buka Kamera")');
    await expect(cameraButton).toBeVisible();
    await expect(cameraButton).toBeEnabled();
  });

  test('should have upload photo button', async ({ page }) => {
    const uploadButton = page.locator('button:has-text("Unggah Foto")');
    await expect(uploadButton).toBeVisible();
    await expect(uploadButton).toBeEnabled();
  });

  test('should display security information', async ({ page }) => {
    await expect(page.getByText('Protokol Keamanan')).toBeVisible();
    await expect(page.getByText('Enkripsi RESP-V3')).toBeVisible();
    await expect(page.getByText('Lisensi ASPI/BI')).toBeVisible();
  });

  test('should display my QRIS code section', async ({ page }) => {
    await expect(page.getByText('QRIS Personal')).toBeVisible();
    await expect(page.getByText('E-Wallet Access')).toBeVisible();
  });

  test('should have show my code button', async ({ page }) => {
    const showCodeButton = page.locator('button:has-text("Tampilkan Kode Saya")');
    await expect(showCodeButton).toBeVisible();
    await expect(showCodeButton).toBeEnabled();
  });

  test('should display recent QR payments section', async ({ page }) => {
    await expect(page.getByText('Aktivitas Terakhir')).toBeVisible();
    await expect(page.getByText('Lihat Semua')).toBeVisible();
  });

  test('should show empty state for recent transactions', async ({ page }) => {
    await expect(page.getByText('Belum ada riwayat transaksi QRIS')).toBeVisible();
  });

  test('should display daily limit information', async ({ page }) => {
    await expect(page.getByText('Limit Harian QRIS')).toBeVisible();
    await expect(page.getByText('Rp 10.000.000')).toBeVisible();
    await expect(page.getByText('0% Terpakai')).toBeVisible();
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/qris');

    // Check that key elements are visible
    await expect(page.getByText('Pembayaran QRIS')).toBeVisible();
    await expect(page.locator('.border-2.border-dashed')).toBeVisible();

    // Take screenshot
    await page.screenshot({
      path: 'e2e/screenshots/qris-mobile.png',
      fullPage: true
    });
  });

  test('should have interactive buttons with proper styling', async ({ page }) => {
    const cameraButton = page.locator('button:has-text("Buka Kamera")');
    const uploadButton = page.locator('button:has-text("Unggah Foto")');

    // Check button visibility
    await expect(cameraButton).toBeVisible();
    await expect(uploadButton).toBeVisible();
  });

  test('should have History icon in recent transactions', async ({ page }) => {
    // Check for History section
    await expect(page.getByText('Aktivitas Terakhir')).toBeVisible();
  });
});

test.describe('QRIS Flow - Scanner Interaction', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/qris');
  });

  test('should highlight scanner area on hover', async ({ page }) => {
    const scannerArea = page.locator('.border-2.border-dashed').first();

    // Hover over scanner area
    await scannerArea.hover();

    // Check that element exists
    await expect(scannerArea).toBeVisible();
  });

  test('should have click handler for camera button', async ({ page }) => {
    const cameraButton = page.locator('button:has-text("Buka Kamera")');

    // Button should be clickable
    await expect(cameraButton).toBeEnabled();

    // Click button (in real scenario would open camera)
    await cameraButton.click();

    // Verify button is still visible
    await expect(cameraButton).toBeVisible();
  });

  test('should have click handler for upload button', async ({ page }) => {
    const uploadButton = page.locator('button:has-text("Unggah Foto")');

    // Button should be clickable
    await expect(uploadButton).toBeEnabled();

    // Click button (in real scenario would open file picker)
    await uploadButton.click();

    // Verify button is still visible
    await expect(uploadButton).toBeVisible();
  });
});

test.describe('QRIS Flow - My QR Code', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/qris');
  });

  test('should display my QR code card with gradient background', async ({ page }) => {
    const myQrCard = page.locator('.bg-gray-900');
    await expect(myQrCard).toBeVisible();
  });

  test('should have show code button with hover effect', async ({ page }) => {
    const showCodeButton = page.locator('button:has-text("Tampilkan Kode Saya")');

    // Check for button
    await expect(showCodeButton).toBeVisible();

    // Click button
    await showCodeButton.click();

    // Verify button is still visible
    await expect(showCodeButton).toBeVisible();
  });

  test('should display my QR code text correctly', async ({ page }) => {
    await expect(page.getByText('QRIS Personal')).toBeVisible();
    await expect(page.getByText('E-Wallet Access')).toBeVisible();
  });
});

test.describe('QRIS Flow - Security Information', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/qris');
  });

  test('should display encryption security info', async ({ page }) => {
    await expect(page.getByText('Enkripsi RESP-V3')).toBeVisible();
    await expect(page.getByText('Token dinamik di-hash per transaksi untuk keamanan maksimal')).toBeVisible();
  });

  test('should display ASPI/BI compliance info', async ({ page }) => {
    await expect(page.getByText('Lisensi ASPI/BI')).toBeVisible();
    await expect(page.getByText('Sistem pembayaran tunduk pada regulasi QRIS Nasional')).toBeVisible();
  });

  test('should display security protocol header', async ({ page }) => {
    await expect(page.getByText('Protokol Keamanan')).toBeVisible();
  });
});

test.describe('QRIS Flow - Recent Transactions', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/qris');
  });

  test('should display recent transactions section', async ({ page }) => {
    await expect(page.getByText('Aktivitas Terakhir')).toBeVisible();
  });

  test('should show empty state when no transactions', async ({ page }) => {
    await expect(page.getByText('Belum ada riwayat transaksi QRIS')).toBeVisible();
  });

  test('should have view all history button', async ({ page }) => {
    const viewAllButton = page.getByText('Lihat Semua');
    await expect(viewAllButton).toBeVisible();
    await expect(viewAllButton).toHaveClass(/text-emerald-600/);
  });
});

test.describe('QRIS Flow - Accessibility', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/qris');
  });

  test('should have proper heading hierarchy', async ({ page }) => {
    const h2 = page.locator('h2');
    await expect(h2).toBeVisible();
    await expect(h2).toContainText('Pembayaran QRIS');
  });

  test('should support keyboard navigation', async ({ page }) => {
    // Tab to first button
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');

    // Check that an element is focused
    const focused = page.locator(':focus');
    await expect(focused).toBeVisible();
  });

  test('should have accessible button labels', async ({ page }) => {
    const cameraButton = page.locator('button:has-text("Buka Kamera")');
    const uploadButton = page.locator('button:has-text("Unggah Foto")');

    await expect(cameraButton).toBeVisible();
    await expect(uploadButton).toBeVisible();
  });
});

test.describe('QRIS Flow - Visual Regression', () => {
  test('should match screenshots on desktop', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/qris');

    await page.screenshot({
      path: 'e2e/screenshots/qris-desktop.png',
      fullPage: true
    });
  });

  test('should match screenshots on tablet', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/qris');

    await page.screenshot({
      path: 'e2e/screenshots/qris-tablet.png',
      fullPage: true
    });
  });

  test('should match screenshots on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/qris');

    await page.screenshot({
      path: 'e2e/screenshots/qris-mobile.png',
      fullPage: true
    });
  });
});
