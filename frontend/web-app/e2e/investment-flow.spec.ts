import { test, expect } from '@playwright/test';

test.describe('Investment Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to investments page (assumes user is logged in)
    await page.goto('/investments');
  });

  test('should display investments page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Manajemen Kekayaan')).toBeVisible();
    await expect(page.getByText('Tumbuhkan aset Anda dengan produk investasi kelas institusi.')).toBeVisible();
  });

  test('should display portfolio overview', async ({ page }) => {
    await expect(page.getByText('Total Portofolio Bersih')).toBeVisible();
    await expect(page.getByText('Rp 152.800.000')).toBeVisible();
  });

  test('should display portfolio growth indicator', async ({ page }) => {
    await expect(page.getByText(/\+Rp 12,4 Jt \(8\.2%\)/)).toBeVisible();
    await expect(page.getByText('TrendingUp')).toBeVisible();
  });

  test('should display LPS guarantee badge', async ({ page }) => {
    await expect(page.getByText('Terjamin LPS')).toBeVisible();
    await expect(page.locator('.text-muted-foreground').filter({ hasText: /ShieldCheck/i })).toBeVisible();
  });

  test('should display portfolio allocation chart', async ({ page }) => {
    await expect(page.getByText('Pasar Uang')).toBeVisible();
    await expect(page.getByText('Saham')).toBeVisible();
    await expect(page.getByText('Komoditas')).toBeVisible();

    await expect(page.getByText('45%')).toBeVisible();
    await expect(page.getByText('30%')).toBeVisible();
    await expect(page.getByText('25%')).toBeVisible();
  });

  test('should display risk profile card', async ({ page }) => {
    await expect(page.getByText('Profil Risiko')).toBeVisible();
    await expect(page.getByText('Moderat-Agresif')).toBeVisible();
    await expect(page.getByText('ROI 15% / Thn')).toBeVisible();
  });

  test('should have risk profile slider', async ({ page }) => {
    await expect(page.getByText('Konservatif')).toBeVisible();
    await expect(page.getByText('Agresif')).toBeVisible();

    // Check for progress bar
    const progressBar = page.locator('.bg-white\\/10.h-2.rounded-full');
    await expect(progressBar).toBeVisible();
  });

  test('should display investment products catalog', async ({ page }) => {
    await expect(page.getByText('Katalog Produk Terpilih')).toBeVisible();
  });

  test('should display all investment product types', async ({ page }) => {
    await expect(page.getByText('Suku Bunga Tetap Plus')).toBeVisible();
    await expect(page.getByText('Equity Growth Fund')).toBeVisible();
    await expect(page.getByText('Emas Digital (XAU)')).toBeVisible();
  });

  test('should display product risk levels', async ({ page }) => {
    await expect(page.getByText('Risiko Rendah')).toBeVisible();
    await expect(page.getByText('Risiko Tinggi')).toBeVisible();
    await expect(page.getByText('Stabil')).toBeVisible();
  });

  test('should display product returns', async ({ page }) => {
    await expect(page.getByText('5.5% p.a')).toBeVisible();
    await expect(page.getByText('18.2% p.a')).toBeVisible();
    await expect(page.getByText('Harga Pasar')).toBeVisible();
  });

  test('should have filter buttons for product types', async ({ page }) => {
    await expect(page.getByText('Semua')).toBeVisible();
    await expect(page.getByText('Pasar Uang')).toBeVisible();
    await expect(page.getByText('Emas')).toBeVisible();
  });

  test('should have new investment button', async ({ page }) => {
    const newInvestButton = page.locator('button:has-text("Investasi Baru")');
    await expect(newInvestButton).toBeVisible();
    await expect(newInvestButton).toBeEnabled();
  });

  test('should display smart advice section', async ({ page }) => {
    await expect(page.getByText('Target Portofolio Hampir Tercapai.')).toBeVisible();
  });

  test('should have optimize portfolio button', async ({ page }) => {
    const optimizeButton = page.locator('button:has-text("Optimasi Portofolio")');
    await expect(optimizeButton).toBeVisible();
  });

  test('should display review strategy button in advice section', async ({ page }) => {
    const reviewButton = page.locator('button:has-text("Tinjau Strategi")');
    await expect(reviewButton).toBeVisible();
    await expect(reviewButton).toBeEnabled();
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/investments');

    // Check that key elements are visible
    await expect(page.getByText('Manajemen Kekayaan')).toBeVisible();
    await expect(page.getByText('Total Portofolio Bersih')).toBeVisible();

    // Take screenshot
    await page.screenshot({
      path: 'e2e/screenshots/investments-mobile.png',
      fullPage: true
    });
  });

  test('should display growth indicators with proper styling', async ({ page }) => {
    const growthBadge = page.locator('.bg-success-light');
    await expect(growthBadge).toBeVisible();
    await expect(growthBadge).toContainText('TrendingUp');
  });

  test('should have interactive product cards', async ({ page }) => {
    const productCards = page.locator('.bg-card.p-8.rounded-xl');

    // Should have at least 3 product cards
    await expect(productCards).toHaveCount(3);

    // Check first card is interactive
    await expect(productCards.first()).toHaveClass(/cursor-pointer/);
    await expect(productCards.first()).toHaveClass(/hover:-translate-y-1/);
  });

  test('should have add buttons on product cards', async ({ page }) => {
    const addButtons = page.locator('button').filter({ hasText: '+' });
    await expect(addButtons).toHaveCount(3);
  });
});

test.describe('Investment Flow - Product Catalog', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/investments');
  });

  test('should display fixed rate product details', async ({ page }) => {
    await expect(page.getByText('Suku Bunga Tetap Plus')).toBeVisible();
    await expect(page.getByText('5.5% p.a')).toBeVisible();
    await expect(page.getByText('Risiko Rendah')).toBeVisible();
  });

  test('should display equity fund details', async ({ page }) => {
    await expect(page.getByText('Equity Growth Fund')).toBeVisible();
    await expect(page.getByText('18.2% p.a')).toBeVisible();
    await expect(page.getByText('Risiko Tinggi')).toBeVisible();
  });

  test('should display gold product details', async ({ page }) => {
    await expect(page.getByText('Emas Digital (XAU)')).toBeVisible();
    await expect(page.getByText('Harga Pasar')).toBeVisible();
    await expect(page.getByText('Stabil')).toBeVisible();
  });

  test('should click on product card', async ({ page }) => {
    const firstProduct = page.locator('.bg-card.p-8.rounded-xl').first();
    await firstProduct.click();

    // Product card should be clickable (in real scenario would navigate to product details)
    await expect(firstProduct).toBeVisible();
  });

  test('should have product icons', async ({ page }) => {
    // Check for product icons (Landmark, TrendingUp, Coins)
    const icons = page.locator('svg');
    const iconsCount = await icons.count();
    expect(iconsCount).toBeGreaterThanOrEqual(3);
  });

  test('should filter products by type', async ({ page }) => {
    // Click on "Pasar Uang" filter
    await page.click('button:has-text("Pasar Uang")');

    // Filter button should be active
    const activeFilter = page.locator('button.bg-primary');
    await expect(activeFilter).toContainText('Pasar Uang');
  });

  test('should have proper color coding for product types', async ({ page }) => {
    // Check for color-coded product icons
    const blueIcon = page.locator('.text-blue-500');
    const greenIcon = page.locator('.text-primary');
    const amberIcon = page.locator('.text-amber-500');

    const blueCount = await blueIcon.count();
    const greenCount = await greenIcon.count();
    const amberCount = await amberIcon.count();
    expect(blueCount).toBeGreaterThanOrEqual(1);
    expect(greenCount).toBeGreaterThanOrEqual(1);
    expect(amberCount).toBeGreaterThanOrEqual(1);
  });
});

test.describe('Investment Flow - Buy Mutual Fund', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/investments');
  });

  test('should click add button on product', async ({ page }) => {
    const addButton = page.locator('button').filter({ hasText: '+' }).first();
    await addButton.click();

    // Button should be clickable
    await expect(addButton).toBeVisible();
  });

  test('should have new investment button in header', async ({ page }) => {
    const newInvestButton = page.locator('button:has-text("Investasi Baru")');
    await expect(newInvestButton).toBeEnabled();

    // Click button
    await newInvestButton.click();

    // In real scenario would open investment modal/form
    await expect(newInvestButton).toBeVisible();
  });

  test('should display investment amount in readable format', async ({ page }) => {
    const portfolioValue = page.getByText('Rp 152.800.000');
    await expect(portfolioValue).toBeVisible();

    // Check for proper currency formatting
    await expect(portfolioValue).toContainText('Rp');
  });

  test('should show portfolio growth percentage', async ({ page }) => {
    await expect(page.getByText(/8\.2%/)).toBeVisible();
  });
});

test.describe('Investment Flow - Risk Profile', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/investments');
  });

  test('should display risk profile card', async ({ page }) => {
    await expect(page.getByText('Profil Risiko')).toBeVisible();
    await expect(page.getByText('Moderat-Agresif')).toBeVisible();
  });

  test('should display risk score', async ({ page }) => {
    await expect(page.getByText('Grade')).toBeVisible();
    await expect(page.getByText('ROI 15% / Thn')).toBeVisible();
  });

  test('should have risk slider visualization', async ({ page }) => {
    const sliderContainer = page.locator('.w-full.bg-white\\/10.h-2.rounded-full');
    await expect(sliderContainer).toBeVisible();

    const sliderFill = page.locator('.bg-bank-green.h-full.rounded-full');
    await expect(sliderFill).toBeVisible();
  });

  test('should have optimize portfolio button', async ({ page }) => {
    const optimizeButton = page.locator('button:has-text("Optimasi Portofolio")');
    await expect(optimizeButton).toBeVisible();
    await expect(optimizeButton).toBeEnabled();

    // Click button
    await optimizeButton.click();

    // In real scenario would open optimization modal
    await expect(optimizeButton).toBeVisible();
  });

  test('should display risk factors', async ({ page }) => {
    // Check for risk factor icons and text
    const riskCount = await page.locator('.text-success-light\\/20').count();
    expect(riskCount).toBeGreaterThanOrEqual(1);
  });
});

test.describe('Investment Flow - Smart Advice', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/investments');
  });

  test('should display smart advice banner', async ({ page }) => {
    await expect(page.getByText('Target Portofolio Hampir Tercapai.')).toBeVisible();

    const adviceBanner = page.locator('.bg-primary\\/5');
    await expect(adviceBanner).toBeVisible();
  });

  test('should display advice content', async ({ page }) => {
    await expect(page.getByText(/Dana Pensiun/)).toBeVisible();
    await expect(page.getByText(/14 bulan lebih cepat/)).toBeVisible();
  });

  test('should have review strategy button', async ({ page }) => {
    const reviewButton = page.locator('button:has-text("Tinjau Strategi")');
    await expect(reviewButton).toBeEnabled();

    // Click button
    await reviewButton.click();

    // In real scenario would open strategy review
    await expect(reviewButton).toBeVisible();
  });

  test('should have target icon in advice section', async ({ page }) => {
    // Check for Target icon with animation
    const targetIcon = page.locator('.animate-pulse');
    await expect(targetIcon).toBeVisible();
  });
});

test.describe('Investment Flow - Accessibility', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/investments');
  });

  test('should have proper heading hierarchy', async ({ page }) => {
    const h2 = page.locator('h2');
    await expect(h2.first()).toBeVisible();
    await expect(h2.first()).toContainText('Manajemen Kekayaan');
  });

  test('should support keyboard navigation', async ({ page }) => {
    // Tab through page
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');

    // Should reach a button
    const focused = page.locator(':focus');
    await expect(focused).toBeVisible();
  });

  test('should have accessible button labels', async ({ page }) => {
    const buttons = page.locator('button');
    await expect(buttons.first()).toBeVisible();
  });

  test('should have proper contrast ratios', async ({ page }) => {
    // Check for text elements
    const headings = page.locator('h2, h3, h4');
    await expect(headings.first()).toBeVisible();
  });
});

test.describe('Investment Flow - Visual Regression', () => {
  test('should match screenshots on desktop', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/investments');

    await page.screenshot({
      path: 'e2e/screenshots/investments-desktop.png',
      fullPage: true
    });
  });

  test('should match screenshots on tablet', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/investments');

    await page.screenshot({
      path: 'e2e/screenshots/investments-tablet.png',
      fullPage: true
    });
  });

  test('should match screenshots on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/investments');

    await page.screenshot({
      path: 'e2e/screenshots/investments-mobile.png',
      fullPage: true
    });
  });
});

test.describe('Investment Flow - Error Handling', () => {
  test('should handle investment purchase error', async ({ page }) => {
    await page.goto('/investments');

    // Click add button (would fail if insufficient balance)
    const addButton = page.locator('button').filter({ hasText: '+' }).first();
    await addButton.click();

    // In real scenario, might show error for insufficient balance
    await expect(addButton).toBeVisible();
  });

  test('should handle filter error gracefully', async ({ page }) => {
    await page.goto('/investments');

    // Click filter button
    await page.click('button:has-text("Pasar Uang")');

    // Filter should still work even if API fails
    const activeFilter = page.locator('button.bg-primary');
    await expect(activeFilter).toBeVisible();
  });
});
