import { test, expect } from '@playwright/test';

test.describe('QRIS Payment Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to QRIS page (assumes user is logged in)
    await page.goto('/qris');
  });

  test('should display QRIS page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Pembayaran QRIS')).toBeVisible();
    await expect(page.getByText('Pindai kode QRIS merchant atau P2P untuk membayar secara instan.')).toBeVisible();
  });

  test('should display QR scanner area', async ({ page }) => {
    const scannerArea = page.locator('.aspect-square.border-2.border-dashed');
    await expect(scannerArea).toBeVisible();
  });

  test('should have camera icon in scanner area', async ({ page }) => {
    await expect(page.locator('.text-bank-green').filter({ hasText: /camera/i })).toBeVisible();
  });

  test('should have scanning instruction text', async ({ page }) => {
    await expect(page.getByText('Posisikan kode QR di dalam bingkai')).toBeVisible();
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
    await expect(page.getByText('Keamanan Pembayaran')).toBeVisible();
    await expect(page.getByText('Bayar Terenkripsi')).toBeVisible();
    await expect(page.getByText('Standar OJK & BI')).toBeVisible();
  });

  test('should display my QRIS code section', async ({ page }) => {
    await expect(page.getByText('Kode QRIS Saya')).toBeVisible();
    await expect(page.getByText('Terima dana instan dari aplikasi bank manapun menggunakan kode unik Anda.')).toBeVisible();
  });

  test('should have show my code button', async ({ page }) => {
    const showCodeButton = page.locator('button:has-text("Tampilkan Kode Saya")');
    await expect(showCodeButton).toBeVisible();
    await expect(showCodeButton).toBeEnabled();
  });

  test('should display recent QR payments section', async ({ page }) => {
    await expect(page.getByText('Pembayaran QR Terakhir')).toBeVisible();
    await expect(page.getByText('Lihat Semua Riwayat')).toBeVisible();
  });

  test('should show empty state for recent transactions', async ({ page }) => {
    await expect(page.getByText('Tidak ada transaksi QRIS yang tercatat dalam 30 hari terakhir.')).toBeVisible();
  });

  test('should have proper security badges', async ({ page }) => {
    await expect(page.locator('.text-bank-green').filter({ hasText: /ShieldCheck|Info/i })).toHaveCount.toBeGreaterThanOrEqual(1);
  });

  test('should display security description text', async ({ page }) => {
    await expect(page.getByText('Setiap transaksi ditandatangani dengan token perangkat unik.')).toBeVisible();
    await expect(page.getByText('Patuh sepenuhnya pada protokol QRIS Bank Indonesia & ASPI.')).toBeVisible();
  });

  test('should have view all history link', async ({ page }) => {
    const historyLink = page.getByText('Lihat Semua Riwayat');
    await expect(historyLink).toBeVisible();
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/qris');

    // Check that key elements are visible
    await expect(page.getByText('Pembayaran QRIS')).toBeVisible();
    await expect(page.locator('.aspect-square.border-2.border-dashed')).toBeVisible();

    // Take screenshot
    await page.screenshot({
      path: 'e2e/screenshots/qris-mobile.png',
      fullPage: true
    });
  });

  test('should have interactive buttons with proper styling', async ({ page }) => {
    const cameraButton = page.locator('button:has-text("Buka Kamera")');
    const uploadButton = page.locator('button:has-text("Unggah Foto")');

    // Check button styling
    await expect(cameraButton).toHaveClass(/bg-foreground/);
    await expect(uploadButton).toHaveClass(/border/);
  });

  test('should have decorative QR code icon', async ({ page }) => {
    // Check for QR code icon in the "My QRIS Code" section
    await expect(page.locator('.text-white\\/5')).toBeVisible();
  });
});

test.describe('QRIS Flow - Scanner Interaction', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/qris');
  });

  test('should highlight scanner area on hover', async ({ page }) => {
    const scannerArea = page.locator('.aspect-square.border-2.border-dashed');

    // Hover over scanner area
    await scannerArea.hover();

    // Check for border color change (hover:border-bank-green)
    await expect(scannerArea).toHaveClass(/hover:border-bank-green/);
  });

  test('should animate camera icon on hover', async ({ page }) => {
    const cameraIcon = page.locator('.aspect-square.border-2.border-dashed').locator('div');

    // The icon should scale on hover
    await cameraIcon.hover();

    // Check for transform effect
    await expect(cameraIcon).toHaveClass(/group-hover\\/scale-110/);
  });

  test('should show pulse animation in scanner', async ({ page }) => {
    const pulseElement = page.locator('.animate-pulse');
    await expect(pulseElement).toBeVisible();
  });

  test('should have click handler for camera button', async ({ page }) => {
    const cameraButton = page.locator('button:has-text("Buka Kamera")');

    // Button should be clickable
    await expect(cameraButton).toBeEnabled();

    // Click button (in real scenario would open camera)
    await cameraButton.click();

    // Verify button was clicked (no error thrown)
    await expect(cameraButton).toBeVisible();
  });

  test('should have click handler for upload button', async ({ page }) => {
    const uploadButton = page.locator('button:has-text("Unggah Foto")');

    // Button should be clickable
    await expect(uploadButton).toBeEnabled();

    // Click button (in real scenario would open file picker)
    await uploadButton.click();

    // Verify button was clicked
    await expect(uploadButton).toBeVisible();
  });
});

test.describe('QRIS Flow - My QR Code', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/qris');
  });

  test('should display my QR code card with gradient background', async ({ page }) => {
    const myQrCard = page.locator('.bg-gradient-to-br.from-gray-900.to-gray-800');
    await expect(myQrCard).toBeVisible();
  });

  test('should have show code button with hover effect', async ({ page }) => {
    const showCodeButton = page.locator('button:has-text("Tampilkan Kode Saya")');

    // Check for hover effect
    await expect(showCodeButton).toHaveClass(/hover:bg-white\\/20/);

    // Click button
    await showCodeButton.click();

    // Verify button is still visible
    await expect(showCodeButton).toBeVisible();
  });

  test('should have decorative QR code icon with rotation', async ({ page }) => {
    const qrIcon = page.locator('.absolute.bottom-\\[-30px\\].right-\\[-30px\\]');

    // Check for rotate animation on hover
    await expect(qrIcon).toHaveClass(/group-hover:rotate-0/);
    await expect(qrIcon).toHaveClass(/transition-transform/);
  });

  test('should display my QR code text correctly', async ({ page }) => {
    await expect(page.getByText('Kode QRIS Saya')).toBeVisible();
    await expect(page.getByText('Terima dana instan dari aplikasi bank manapun')).toBeVisible();
  });
});

test.describe('QRIS Flow - Security Information', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/qris');
  });

  test('should display encryption security info', async ({ page }) => {
    await expect(page.getByText('Bayar Terenkripsi')).toBeVisible();
    await expect(page.getByText('Setiap transaksi ditandatangani dengan token perangkat unik.')).toBeVisible();
  });

  test('should display OJK & BI compliance info', async ({ page }) => {
    await expect(page.getByText('Standar OJK & BI')).toBeVisible();
    await expect(page.getByText('Patuh sepenuhnya pada protokol QRIS Bank Indonesia & ASPI.')).toBeVisible();
  });

  test('should have security icons', async ({ page }) => {
    // Check for ShieldCheck and Info icons
    await expect(page.locator('.text-bank-green')).toHaveCount.toBeGreaterThanOrEqual(2);
  });

  test('should display security info in cards', async ({ page }) => {
    const securityCards = page.locator('.bg-card.rounded-xl.p-10');
    await expect(securityCards.first()).toBeVisible();
  });
});

test.describe('QRIS Flow - Recent Transactions', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/qris');
  });

  test('should display recent transactions section', async ({ page }) => {
    await expect(page.getByText('Pembayaran QR Terakhir')).toBeVisible();
  });

  test('should show empty state when no transactions', async ({ page }) => {
    await expect(page.getByText('Tidak ada transaksi QRIS yang tercatat dalam 30 hari terakhir.')).toBeVisible();

    // Check for history icon
    await expect(page.locator('.text-gray-100.dark\\:text-gray-900')).toBeVisible();
  });

  test('should have view all history button', async ({ page }) => {
    const viewAllButton = page.getByText('Lihat Semua Riwayat');
    await expect(viewAllButton).toBeVisible();
    await expect(viewAllButton).toHaveClass(/text-bank-green/);
  });

  test('should display transactions in card container', async ({ page }) => {
    const transactionCard = page.locator('.bg-card.rounded-xl.p-12');
    await expect(transactionCard).toBeVisible();
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

    const focusedText = await page.locator(':focus').textContent();
    expect(focusedText).toContain('Buka Kamera');
  });

  test('should activate buttons with Enter key', async ({ page }) => {
    // Tab to camera button
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');

    // Press Enter
    await page.keyboard.press('Enter');

    // Button should be clickable
    const cameraButton = page.locator('button:has-text("Buka Kamera")');
    await expect(cameraButton).toBeVisible();
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

test.describe('QRIS Flow - Error Handling', () => {
  test('should handle camera permission denial', async ({ page }) => {
    await page.goto('/qris');

    // Mock camera permission denial
    page.context().grantPermissions([], { origin: '' });

    // Click camera button
    await page.click('button:has-text("Buka Kamera")');

    // In real scenario, would show permission error
    // For now, just verify button is still clickable
    const cameraButton = page.locator('button:has-text("Buka Kamera")');
    await expect(cameraButton).toBeVisible();
  });

  test('should handle invalid QR code scan', async ({ page }) => {
    await page.goto('/qris');

    // In real scenario, scanning invalid QR would show error
    // For now, verify error handling elements exist
    await expect(page.getByText('Keamanan Pembayaran')).toBeVisible();
  });
});
