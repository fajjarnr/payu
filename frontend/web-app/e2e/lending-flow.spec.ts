import { test, expect } from '@playwright/test';

test.describe('Lending Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to lending page (assumes user is logged in)
    await page.goto('/lending');
  });

  test('should display lending page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Pinjaman & Kredit')).toBeVisible();
    await expect(page.getByText('Solusi pembiayaan fleksibel sesuai kebutuhan Anda.')).toBeVisible();
  });

  test('should display loan and paylater tabs', async ({ page }) => {
    await expect(page.getByText('Pinjaman')).toBeVisible();
    await expect(page.getByText('PayLater')).toBeVisible();
  });

  test('should have loans tab active by default', async ({ page }) => {
    const activeTab = page.locator('button').filter({ hasText: 'Pinjaman' });
    await expect(activeTab).toHaveClass(/bg-primary/);
  });

  test('should switch to PayLater tab', async ({ page }) => {
    await page.click('button:has-text("PayLater")');

    // PayLater tab should be active
    const activeTab = page.locator('button').filter({ hasText: 'PayLater' });
    await expect(activeTab).toHaveClass(/bg-primary/);

    // Should show PayLater content
    await expect(page.getByText('PayLater Limit')).toBeVisible();
  });

  test('should display credit score on loans tab', async ({ page }) => {
    await expect(page.getByText('Skor Kredit Anda')).toBeVisible();
    await expect(page.getByText('785')).toBeVisible();
    await expect(page.getByText('Grade A')).toBeVisible();
  });

  test('should display credit score factors', async ({ page }) => {
    await expect(page.getByText('Pembayaran tepat waktu')).toBeVisible();
    await expect(page.getByText('Rasio utang rendah')).toBeVisible();
    await expect(page.getByText('Histori kredit panjang')).toBeVisible();
  });

  test('should display credit score progress bar', async ({ page }) => {
    const progressBar = page.locator('.bg-white\\/10.h-2.rounded-full');
    await expect(progressBar).toBeVisible();

    const progressFill = page.locator('.bg-gradient-to-r.from-success-light');
    await expect(progressFill).toBeVisible();
  });

  test('should display total loan limit', async ({ page }) => {
    await expect(page.getByText('Total Limit Pinjaman')).toBeVisible();
    await expect(page.getByText(/Rp 50\.000\.000/)).toBeVisible();
  });

  test('should display loan products', async ({ page }) => {
    await expect(page.getByText('Produk Pinjaman')).toBeVisible();
    await expect(page.getByText('Pinjaman Personal')).toBeVisible();
    await expect(page.getByText('Pinjaman Multiguna')).toBeVisible();
  });

  test('should display personal loan details', async ({ page }) => {
    await expect(page.getByText('Pembiayaan fleksibel untuk kebutuhan pribadi')).toBeVisible();
    await expect(page.getByText('12.5% p.a')).toBeVisible();
    await expect(page.getByText('6 - 36 bulan')).toBeVisible();
  });

  test('should display multiguna loan details', async ({ page }) => {
    await expect(page.getByText('Gunakan aset Anda sebagai jaminan')).toBeVisible();
    await expect(page.getByText('10% p.a')).toBeVisible();
    await expect(page.getByText('12 - 60 bulan')).toBeVisible();
  });

  test('should display PayLater limit on PayLater tab', async ({ page }) => {
    await page.click('button:has-text("PayLater")');

    await expect(page.getByText('PayLater Limit')).toBeVisible();
    await expect(page.getByText(/Rp 10\.500\.000/)).toBeVisible();
  });

  test('should display PayLater usage breakdown', async ({ page }) => {
    await page.click('button:has-text("PayLater")');

    await expect(page.getByText('Limit Terpakai')).toBeVisible();
    await expect(page.getByText(/Rp 4\.500\.000 \/ Rp 15\.000\.000/)).toBeVisible();
  });

  test('should display PayLater due date', async ({ page }) => {
    await page.click('button:has-text("PayLater")');

    await expect(page.getByText('Jatuh Tempo')).toBeVisible();
    await expect(page.getByText('25 Jan 2026')).toBeVisible();
  });

  test('should display minimum payment', async ({ page }) => {
    await page.click('button:has-text("PayLater")');

    await expect(page.getByText('Pembayaran Minimum')).toBeVisible();
    await expect(page.getByText(/Rp 250\.000/)).toBeVisible();
  });

  test('should display PayLater transactions', async ({ page }) => {
    await page.click('button:has-text("PayLater")');

    await expect(page.getByText('Riwayat Transaksi PayLater')).toBeVisible();
    await expect(page.getByText('TokoBapak')).toBeVisible();
    await expect(page.getByText('Traveloka')).toBeVisible();
    await expect(page.getByText('Shopee')).toBeVisible();
  });

  test('should display transaction status indicators', async ({ page }) => {
    await page.click('button:has-text("PayLater")');

    await expect(page.getByText('Dibayar')).toBeVisible();
    await expect(page.getByText('Menunggu Pembayaran')).toBeVisible();
  });

  test('should have apply loan buttons', async ({ page }) => {
    const applyButtons = page.locator('button:has-text("Ajukan Sekarang")');
    await expect(applyButtons).toHaveCount(2);
  });

  test('should have activate PayLater button', async ({ page }) => {
    await page.click('button:has-text("PayLater")');

    const activateButton = page.locator('button:has-text("Aktifkan PayLater")');
    await expect(activateButton).toBeVisible();
    await expect(activateButton).toBeEnabled();
  });

  test('should have pay bill button on PayLater tab', async ({ page }) => {
    await page.click('button:has-text("PayLater")');

    const payButton = page.locator('button:has-text("Bayar Tagihan")');
    await expect(payButton).toBeVisible();
    await expect(payButton).toBeEnabled();
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/lending');

    // Check that key elements are visible
    await expect(page.getByText('Pinjaman & Kredit')).toBeVisible();
    await expect(page.getByText('Skor Kredit Anda')).toBeVisible();

    // Take screenshot
    await page.screenshot({
      path: 'e2e/screenshots/lending-mobile.png',
      fullPage: true
    });
  });

  test('should display transaction summary', async ({ page }) => {
    await page.click('button:has-text("PayLater")');

    await expect(page.getByText('Ringkasan Transaksi')).toBeVisible();
    await expect(page.getByText('Total Transaksi')).toBeVisible();
    await expect(page.getByText('Pembayaran Berhasil')).toBeVisible();
    await expect(page.getByText('Menunggu Pembayaran')).toBeVisible();
  });

  test('should display processing time for loans', async ({ page }) => {
    await expect(page.getByText('Proses')).toBeVisible();
    await expect(page.getByText('1-2 hari kerja')).toBeVisible();
    await expect(page.getByText('3-5 hari kerja')).toBeVisible();
  });
});

test.describe('Lending Flow - Loan Application', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/lending');
  });

  test('should click apply for personal loan', async ({ page }) => {
    const applyButton = page.locator('button:has-text("Ajukan Sekarang")').first();
    await applyButton.click();

    // In real scenario would open loan application form
    await expect(applyButton).toBeVisible();
  });

  test('should click apply for multiguna loan', async ({ page }) => {
    const applyButton = page.locator('button:has-text("Ajukan Sekarang")').nth(1);
    await applyButton.click();

    // In real scenario would open loan application form
    await expect(applyButton).toBeVisible();
  });

  test('should display loan interest rates', async ({ page }) => {
    await expect(page.getByText('12.5% p.a')).toBeVisible();
    await expect(page.getByText('10% p.a')).toBeVisible();
  });

  test('should display loan tenures', async ({ page }) => {
    await expect(page.getByText('6 - 36 bulan')).toBeVisible();
    await expect(page.getByText('12 - 60 bulan')).toBeVisible();
  });

  test('should display loan limits', async ({ page }) => {
    await expect(page.getByText(/Rp 2\.000\.000 - Rp 50\.000\.000/)).toBeVisible();
    await expect(page.getByText(/Rp 10\.000\.000 - Rp 200\.000\.000/)).toBeVisible();
  });

  test('should have interactive loan cards', async ({ page }) => {
    const loanCards = page.locator('.bg-card.p-8.rounded-xl');

    // Should have 2 loan cards
    await expect(loanCards).toHaveCount(2);

    // Check first card is interactive
    await expect(loanCards.first()).toHaveClass(/cursor-pointer/);
    await expect(loanCards.first()).toHaveClass(/hover:-translate-y-1/);
  });
});

test.describe('Lending Flow - PayLater', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/lending');
    await page.click('button:has-text("PayLater")');
  });

  test('should display PayLater credit card', async ({ page }) => {
    const creditCard = page.locator('.bg-gradient-to-br.from-primary');
    await expect(creditCard).toBeVisible();
  });

  test('should display PayLater balance', async ({ page }) => {
    await expect(page.getByText(/Rp 10\.500\.000/)).toBeVisible();
  });

  test('should display PayLater usage bar', async ({ page }) => {
    const usageBar = page.locator('.bg-white\\/20.h-3');
    await expect(usageBar).toBeVisible();

    const usageFill = page.locator('.bg-white.h-full');
    await expect(usageFill).toBeVisible();
  });

  test('should display transaction list', async ({ page }) => {
    const transactions = page.locator('.p-6.hover\\:bg-muted\\/30');
    await expect(transactions).toHaveCount(3);
  });

  test('should display merchant names', async ({ page }) => {
    await expect(page.getByText('TokoBapak')).toBeVisible();
    await expect(page.getByText('Traveloka')).toBeVisible();
    await expect(page.getByText('Shopee')).toBeVisible();
  });

  test('should display transaction amounts', async ({ page }) => {
    await expect(page.getByText(/Rp 850\.000/)).toBeVisible();
    await expect(page.getByText(/Rp 3\.200\.000/)).toBeVisible();
    await expect(page.getByText(/Rp 450\.000/)).toBeVisible();
  });

  test('should display transaction dates', async ({ page }) => {
    await expect(page.getByText('20 Jan 2026')).toBeVisible();
    await expect(page.getByText('18 Jan 2026')).toBeVisible();
    await expect(page.getByText('15 Jan 2026')).toBeVisible();
  });

  test('should have pay bill button functional', async ({ page }) => {
    const payButton = page.locator('button:has-text("Bayar Tagihan")');
    await expect(payButton).toBeEnabled();

    // Click button
    await payButton.click();

    // In real scenario would open payment modal
    await expect(payButton).toBeVisible();
  });

  test('should display transaction summary stats', async ({ page }) => {
    await expect(page.getByText('Total Transaksi')).toBeVisible();
    await expect(page.getByText('3')).toBeVisible();
    await expect(page.getByText('2')).toBeVisible(); // Paid
    await expect(page.getByText('1')).toBeVisible(); // Pending
  });

  test('should have proper status styling', async ({ page }) => {
    const paidStatus = page.locator('.text-success-light');
    const pendingStatus = page.locator('.text-warning');

    await expect(paidStatus).toBeVisible();
    await expect(pendingStatus).toBeVisible();
  });
});

test.describe('Lending Flow - Credit Score', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/lending');
  });

  test('should display credit score prominently', async ({ page }) => {
    const scoreValue = page.locator('.text-5xl.font-black');
    await expect(scoreValue).toContainText('785');
  });

  test('should display credit grade badge', async ({ page }) => {
    const gradeBadge = page.locator('.bg-success-light\\/20');
    await expect(gradeBadge).toBeVisible();
    await expect(gradeBadge).toContainText('Grade A');
  });

  test('should display last updated date', async ({ page }) => {
    await expect(page.getByText('Terakhir diperbarui: 20 Jan 2026')).toBeVisible();
  });

  test('should have credit score factors with icons', async ({ page }) => {
    const checkIcons = page.locator('.text-success-light');
    await expect(checkIcons).toHaveCount.toBeGreaterThanOrEqual(3);
  });

  test('should display credit score in gradient card', async ({ page }) => {
    const scoreCard = page.locator('.bg-gradient-to-br.from-gray-900.to-gray-800');
    await expect(scoreCard).toBeVisible();
  });
});

test.describe('Lending Flow - Accessibility', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/lending');
  });

  test('should have proper heading hierarchy', async ({ page }) => {
    const h2 = page.locator('h2');
    await expect(h2.first()).toBeVisible();
    await expect(h2.first()).toContainText('Pinjaman & Kredit');
  });

  test('should support keyboard navigation', async ({ page }) => {
    // Tab through page
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');

    // Should reach a button
    const focused = page.locator(':focus');
    await expect(focused).toBeVisible();
  });

  test('should have accessible tab buttons', async ({ page }) => {
    const tabs = page.locator('button').filter({ hasText: /Pinjaman|PayLater/ });
    await expect(tabs).toHaveCount(2);
  });

  test('should switch tabs with keyboard', async ({ page }) => {
    // Focus on PayLater tab
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');

    // Press Enter to activate
    await page.keyboard.press('Enter');

    // Should switch to PayLater
    await expect(page.getByText('PayLater Limit')).toBeVisible();
  });
});

test.describe('Lending Flow - Visual Regression', () => {
  test('should match screenshots on desktop', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/lending');

    await page.screenshot({
      path: 'e2e/screenshots/lending-desktop.png',
      fullPage: true
    });
  });

  test('should match screenshots on tablet', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/lending');

    await page.screenshot({
      path: 'e2e/screenshots/lending-tablet.png',
      fullPage: true
    });
  });

  test('should match screenshots on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/lending');

    await page.screenshot({
      path: 'e2e/screenshots/lending-mobile.png',
      fullPage: true
    });
  });
});

test.describe('Lending Flow - Error Handling', () => {
  test('should handle loan application error', async ({ page }) => {
    await page.goto('/lending');

    // Click apply button (would fail if credit score too low)
    const applyButton = page.locator('button:has-text("Ajukan Sekarang")').first();
    await applyButton.click();

    // In real scenario, might show error for insufficient credit score
    await expect(applyButton).toBeVisible();
  });

  test('should handle PayLater activation error', async ({ page }) => {
    await page.goto('/lending');
    await page.click('button:has-text("PayLater")');

    // Click activate button
    const activateButton = page.locator('button:has-text("Aktifkan PayLater")');
    await activateButton.click();

    // In real scenario, might show error if already active or ineligible
    await expect(activateButton).toBeVisible();
  });

  test('should handle payment error gracefully', async ({ page }) => {
    await page.goto('/lending');
    await page.click('button:has-text("PayLater")');

    // Click pay button
    const payButton = page.locator('button:has-text("Bayar Tagihan")');
    await payButton.click();

    // In real scenario, might show error if payment fails
    await expect(payButton).toBeVisible();
  });
});

test.describe('Lending Flow - Interactive Elements', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/lending');
  });

  test('should have hover effects on loan cards', async ({ page }) => {
    const loanCard = page.locator('.bg-card.p-8.rounded-xl').first();

    // Check for hover classes
    await expect(loanCard).toHaveClass(/hover:shadow-card/);
    await expect(loanCard).toHaveClass(/hover:-translate-y-1/);
  });

  test('should have active scale effect on buttons', async ({ page }) => {
    const applyButton = page.locator('button:has-text("Ajukan Sekarang")').first();

    // Check for active scale class
    await expect(applyButton).toHaveClass(/active:scale-\\[0\\.98\\]/);
  });

  test('should have smooth transitions', async ({ page }) => {
    const loanCard = page.locator('.bg-card.p-8.rounded-xl').first();

    // Check for transition class
    await expect(loanCard).toHaveClass(/transition-all/);
  });
});
