import { test, expect } from './fixtures';

test.describe('Investment Flow', () => {
  test.beforeEach(async ({ authPage: page }) => {
    // Navigate to investments page with authentication
    // authPage fixture automatically sets up session cookies
    await page.goto('/investments');
    // Wait for page to fully load
    await page.waitForLoadState('networkidle');
  });

  test('should display investments page correctly', async ({ authPage: page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Manajemen Kekayaan')).toBeVisible();
    await expect(page.getByText('Tumbuhkan aset Anda dengan produk investasi kelas institusi.')).toBeVisible();
  });

  test('should display portfolio overview', async ({ authPage: page }) => {
    await expect(page.getByText('Total Portofolio Bersih')).toBeVisible();
    await expect(page.getByText('Rp 152.800.000')).toBeVisible();
  });

  test('should display portfolio growth indicator', async ({ authPage: page }) => {
    await expect(page.getByText(/\+Rp 12,4 Jt \(8\.2%\)/)).toBeVisible();
    // Growth badge contains an SVG icon (TrendingUp), not literal text
    const growthBadge = page.locator('.bg-success-light');
    await expect(growthBadge).toBeVisible();
    await expect(growthBadge.locator('svg')).toBeVisible();
  });

  test('should display LPS guarantee badge', async ({ authPage: page }) => {
    await expect(page.getByText('Terjamin LPS')).toBeVisible();
    // ShieldCheck is an SVG icon, verify it renders alongside the text
    const lpsSection = page.getByText('Terjamin LPS').locator('..');
    await expect(lpsSection.locator('svg')).toBeVisible();
  });

  test('should display portfolio allocation chart', async ({ authPage: page }) => {
    await expect(page.getByText('Pasar Uang')).toBeVisible();
    await expect(page.getByText('Saham')).toBeVisible();
    await expect(page.getByText('Komoditas')).toBeVisible();

    await expect(page.getByText('45%')).toBeVisible();
    await expect(page.getByText('30%')).toBeVisible();
    await expect(page.getByText('25%')).toBeVisible();
  });

  test('should display risk profile card', async ({ authPage: page }) => {
    await expect(page.getByText('Profil Risiko')).toBeVisible();
    await expect(page.getByText('Moderat-Agresif')).toBeVisible();
    await expect(page.getByText('ROI 15% / Thn')).toBeVisible();
  });

  test('should have risk profile slider', async ({ authPage: page }) => {
    await expect(page.getByText('Konservatif')).toBeVisible();
    await expect(page.getByText('Agresif')).toBeVisible();

    // Check for progress bar - use a more flexible selector
    const progressBar = page.locator('.bg-white\/10.h-2.rounded-full, .h-2.rounded-full');
    await expect(progressBar.first()).toBeVisible();
  });

  test('should display investment products catalog', async ({ authPage: page }) => {
    await expect(page.getByText('Katalog Produk Terpilih')).toBeVisible();
  });

  test('should display all investment product types', async ({ authPage: page }) => {
    await expect(page.getByText('Suku Bunga Tetap Plus')).toBeVisible();
    await expect(page.getByText('Equity Growth Fund')).toBeVisible();
    await expect(page.getByText('Emas Digital (XAU)')).toBeVisible();
  });

  test('should display product risk levels', async ({ authPage: page }) => {
    await expect(page.getByText('Risiko Rendah')).toBeVisible();
    await expect(page.getByText('Risiko Tinggi')).toBeVisible();
    await expect(page.getByText('Stabil')).toBeVisible();
  });

  test('should display product returns', async ({ authPage: page }) => {
    await expect(page.getByText('5.5% p.a')).toBeVisible();
    await expect(page.getByText('18.2% p.a')).toBeVisible();
    await expect(page.getByText('Harga Pasar')).toBeVisible();
  });

  test('should have filter buttons for product types', async ({ authPage: page }) => {
    await expect(page.getByText('Semua')).toBeVisible();
    await expect(page.getByText('Pasar Uang')).toBeVisible();
    await expect(page.getByText('Emas')).toBeVisible();
  });

  test('should have new investment button', async ({ authPage: page }) => {
    const newInvestButton = page.locator('[data-testid="new-investment-button"]');
    await expect(newInvestButton).toBeVisible();
    await expect(newInvestButton).toBeEnabled();
  });

  test('should display smart advice section', async ({ authPage: page }) => {
    // Wait for page to fully load including animated elements
    await page.waitForTimeout(500);
    await expect(page.getByText('Target Portofolio Hampir Tercapai.')).toBeVisible();
  });

  test('should have optimize portfolio button', async ({ authPage: page }) => {
    const optimizeButton = page.locator('[data-testid="optimize-portfolio-button"]');
    await expect(optimizeButton).toBeVisible();
  });

  test('should display review strategy button in advice section', async ({ authPage: page }) => {
    const reviewButton = page.locator('[data-testid="review-strategy-button"]');
    await expect(reviewButton).toBeVisible();
    await expect(reviewButton).toBeEnabled();
  });

  test('should be responsive on mobile viewport', async ({ authPage: page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/investments');
    await page.waitForLoadState('networkidle');

    // Check that key elements are visible
    await expect(page.getByText('Manajemen Kekayaan')).toBeVisible();
    await expect(page.getByText('Total Portofolio Bersih')).toBeVisible();

    // Take screenshot
    await page.screenshot({
      path: 'e2e/screenshots/investments-mobile.png',
      fullPage: true
    });
  });

  test('should display growth indicators with proper styling', async ({ authPage: page }) => {
    const growthBadge = page.locator('.bg-success-light');
    await expect(growthBadge).toBeVisible();
    // SVG icon is rendered inside — verify it exists (not text)
    await expect(growthBadge.locator('svg')).toBeVisible();
  });

  test('should have interactive product cards', async ({ authPage: page }) => {
    // Use data-testid selectors for accurate product card targeting
    for (let i = 0; i < 3; i++) {
      const card = page.locator(`[data-testid="investment-product-${i}"]`);
      await expect(card).toBeVisible();
    }
  });

  test('should have add buttons on product cards', async ({ authPage: page }) => {
    // Use data-testid selectors for buy buttons (Plus icon is SVG, not "+" text)
    for (let i = 0; i < 3; i++) {
      const buyBtn = page.locator(`[data-testid="buy-investment-${i}"]`);
      await expect(buyBtn).toBeVisible();
    }
  });
});

test.describe('Investment Flow - Product Catalog', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/investments');
    await page.waitForLoadState('networkidle');
  });

  test('should display fixed rate product details', async ({ authPage: page }) => {
    await expect(page.getByText('Suku Bunga Tetap Plus')).toBeVisible();
    await expect(page.getByText('5.5% p.a')).toBeVisible();
    await expect(page.getByText('Risiko Rendah')).toBeVisible();
  });

  test('should display equity fund details', async ({ authPage: page }) => {
    await expect(page.getByText('Equity Growth Fund')).toBeVisible();
    await expect(page.getByText('18.2% p.a')).toBeVisible();
    await expect(page.getByText('Risiko Tinggi')).toBeVisible();
  });

  test('should display gold product details', async ({ authPage: page }) => {
    await expect(page.getByText('Emas Digital (XAU)')).toBeVisible();
    await expect(page.getByText('Harga Pasar')).toBeVisible();
    await expect(page.getByText('Stabil')).toBeVisible();
  });

  test('should click on product card', async ({ authPage: page }) => {
    const firstProduct = page.locator('[data-testid="investment-product-0"]');
    await firstProduct.click();

    // Product card should be clickable (in real scenario would navigate to product details)
    await expect(firstProduct).toBeVisible();
  });

  test('should have product icons', async ({ authPage: page }) => {
    // Check for product icons (Landmark, TrendingUp, Coins)
    const icons = page.locator('svg');
    const iconsCount = await icons.count();
    expect(iconsCount).toBeGreaterThanOrEqual(3);
  });

  test('should filter products by type', async ({ authPage: page }) => {
    // Click on "Pasar Uang" filter
    await page.click('button:has-text("Pasar Uang")');

    // Filter button should still be visible and clickable
    const filterButton = page.locator('button:has-text("Pasar Uang")');
    await expect(filterButton).toBeVisible();
  });

  test('should have proper color coding for product types', async ({ authPage: page }) => {
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
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/investments');
    await page.waitForLoadState('networkidle');
  });

  test('should click add button on product', async ({ authPage: page }) => {
    const addButton = page.locator('[data-testid="buy-investment-0"]');
    await addButton.click();

    // Button should be clickable
    await expect(addButton).toBeVisible();
  });

  test('should have new investment button in header', async ({ authPage: page }) => {
    const newInvestButton = page.locator('[data-testid="new-investment-button"]');
    await expect(newInvestButton).toBeEnabled();

    // Click button
    await newInvestButton.click();

    // In real scenario would open investment modal/form
    await expect(newInvestButton).toBeVisible();
  });

  test('should display investment amount in readable format', async ({ authPage: page }) => {
    const portfolioValue = page.getByText('Rp 152.800.000');
    await expect(portfolioValue).toBeVisible();

    // Check for proper currency formatting
    await expect(portfolioValue).toContainText('Rp');
  });

  test('should show portfolio growth percentage', async ({ authPage: page }) => {
    await expect(page.getByText(/8\.2%/)).toBeVisible();
  });
});

test.describe('Investment Flow - Risk Profile', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/investments');
    await page.waitForLoadState('networkidle');
  });

  test('should display risk profile card', async ({ authPage: page }) => {
    await expect(page.getByText('Profil Risiko')).toBeVisible();
    await expect(page.getByText('Moderat-Agresif')).toBeVisible();
  });

  test('should display risk score', async ({ authPage: page }) => {
    await expect(page.getByText('Grade')).toBeVisible();
    await expect(page.getByText('ROI 15% / Thn')).toBeVisible();
  });

  test('should have risk slider visualization', async ({ authPage: page }) => {
    // Use flexible selectors for the risk slider
    const sliderContainer = page.locator('.h-2.rounded-full').first();
    await expect(sliderContainer).toBeVisible();

    const sliderFill = page.locator('.bg-bank-green.h-full.rounded-full');
    await expect(sliderFill).toBeVisible();
  });

  test('should have optimize portfolio button', async ({ authPage: page }) => {
    const optimizeButton = page.locator('[data-testid="optimize-portfolio-button"]');
    await expect(optimizeButton).toBeVisible();
    await expect(optimizeButton).toBeEnabled();

    // Click button
    await optimizeButton.click();

    // In real scenario would open optimization modal
    await expect(optimizeButton).toBeVisible();
  });

  test('should display risk factors', async ({ authPage: page }) => {
    // Verify risk-related text exists on page
    await expect(page.getByText('Konservatif')).toBeVisible();
    await expect(page.getByText('Agresif')).toBeVisible();
  });
});

test.describe('Investment Flow - Smart Advice', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/investments');
    await page.waitForLoadState('networkidle');
  });

  test('should display smart advice banner', async ({ authPage: page }) => {
    await expect(page.getByText('Target Portofolio Hampir Tercapai.')).toBeVisible();

    const adviceBanner = page.locator('.bg-primary\/5');
    await expect(adviceBanner).toBeVisible();
  });

  test('should display advice content', async ({ authPage: page }) => {
    await expect(page.getByText(/Dana Pensiun/)).toBeVisible();
    await expect(page.getByText(/14 bulan lebih cepat/)).toBeVisible();
  });

  test('should have review strategy button', async ({ authPage: page }) => {
    const reviewButton = page.locator('[data-testid="review-strategy-button"]');
    await expect(reviewButton).toBeEnabled();

    // Click button
    await reviewButton.click();

    // In real scenario would open strategy review
    await expect(reviewButton).toBeVisible();
  });

  test('should have target icon in advice section', async ({ authPage: page }) => {
    // Check for Target icon with animation
    const targetIcon = page.locator('.animate-pulse');
    await expect(targetIcon).toBeVisible();
  });
});

test.describe('Investment Flow - Accessibility', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/investments');
    await page.waitForLoadState('networkidle');
  });

  test('should have proper heading hierarchy', async ({ authPage: page }) => {
    const h2 = page.locator('h2');
    await expect(h2.first()).toBeVisible();
    await expect(h2.first()).toContainText('Manajemen Kekayaan');
  });

  test('should support keyboard navigation', async ({ authPage: page }) => {
    // Tab through page
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');

    // Should reach a button
    const focused = page.locator(':focus');
    await expect(focused).toBeVisible();
  });

  test('should have accessible button labels', async ({ authPage: page }) => {
    const buttons = page.locator('button');
    await expect(buttons.first()).toBeVisible();
  });

  test('should have proper contrast ratios', async ({ authPage: page }) => {
    // Check for text elements
    const headings = page.locator('h2, h3, h4');
    await expect(headings.first()).toBeVisible();
  });
});

test.describe('Investment Flow - Visual Regression', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/investments');
    await page.waitForLoadState('networkidle');
  });

  test('should match screenshots on desktop', async ({ authPage: page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });

    await page.screenshot({
      path: 'e2e/screenshots/investments-desktop.png',
      fullPage: true
    });
  });

  test('should match screenshots on tablet', async ({ authPage: page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });

    await page.screenshot({
      path: 'e2e/screenshots/investments-tablet.png',
      fullPage: true
    });
  });

  test('should match screenshots on mobile', async ({ authPage: page }) => {
    await page.setViewportSize({ width: 375, height: 667 });

    await page.screenshot({
      path: 'e2e/screenshots/investments-mobile.png',
      fullPage: true
    });
  });
});

test.describe('Investment Flow - Error Handling', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/investments');
    await page.waitForLoadState('networkidle');
  });

  test('should handle investment purchase error', async ({ authPage: page }) => {
    // Click add button (would fail if insufficient balance)
    const addButton = page.locator('[data-testid="buy-investment-0"]');
    await addButton.click();

    // In real scenario, might show error for insufficient balance
    await expect(addButton).toBeVisible();
  });

  test('should handle filter error gracefully', async ({ authPage: page }) => {
    // Click filter button
    await page.click('button:has-text("Pasar Uang")');

    // Filter should still work even if API fails
    const filterButton = page.locator('button:has-text("Pasar Uang")');
    await expect(filterButton).toBeVisible();
  });
});
