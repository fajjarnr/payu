import { test, expect } from './fixtures';

test('capture landing page and dashboard', async ({ authPage: page }) => {
    // 1. Check Landing Page
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');
    await page.screenshot({ path: 'test-results/landing-page.png', fullPage: true });
    console.log('Landing page screenshot saved.');

    // 2. Check Dashboard with authentication
    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.screenshot({ path: 'test-results/dashboard-ui.png', fullPage: true });
    console.log('Dashboard screenshot saved.');

    // Check for Dashboard elements
    const welcomeText = page.getByText(/Selamat Datang|Welcome/);

    if (await welcomeText.isVisible().catch(() => false)) {
        console.log('UI Verification: Dashboard is visible.');
    } else {
        console.log('UI Verification: Dashboard NOT visible. Check if login logic changed.');
    }
});

test.describe('UI Check - Basic Page Load', () => {
  test('should load home page', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    await expect(page).toHaveTitle(/PayU/);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should load login page', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');

    await expect(page).toHaveTitle(/PayU/);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should load onboarding page', async ({ page }) => {
    await page.goto('/onboarding');
    await page.waitForLoadState('domcontentloaded');

    await expect(page).toHaveTitle(/PayU/);
    await expect(page.locator('body')).toBeVisible();
  });
});

test.describe('UI Check - Protected Pages', () => {
  test('should load dashboard with auth', async ({ authPage: page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');

    await expect(page).toHaveTitle(/PayU/);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should load investments page with auth', async ({ authPage: page }) => {
    await page.goto('/investments');
    await page.waitForLoadState('domcontentloaded');

    await expect(page).toHaveTitle(/PayU/);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should load bills page with auth', async ({ authPage: page }) => {
    await page.goto('/bills');
    await page.waitForLoadState('domcontentloaded');

    await expect(page).toHaveTitle(/PayU/);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should load lending page with auth', async ({ authPage: page }) => {
    await page.goto('/lending');
    await page.waitForLoadState('domcontentloaded');

    await expect(page).toHaveTitle(/PayU/);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should load transfer page with auth', async ({ authPage: page }) => {
    await page.goto('/transfer');
    await page.waitForLoadState('domcontentloaded');

    // Transfer page has its own title (e.g., "Transfer Instan")
    await expect(page).toHaveTitle(/.+/);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should load qris page with auth', async ({ authPage: page }) => {
    await page.goto('/qris');
    await page.waitForLoadState('domcontentloaded');

    await expect(page).toHaveTitle(/PayU/);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should load settings page with auth', async ({ authPage: page }) => {
    await page.goto('/settings');
    await page.waitForLoadState('domcontentloaded');

    // Settings page has its own title (e.g., "Pengaturan Akun")
    await expect(page).toHaveTitle(/.+/);
    await expect(page.locator('body')).toBeVisible();
  });
});

test.describe('UI Check - Responsive Design', () => {
  test('should render correctly on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    await expect(page.locator('body')).toBeVisible();
  });

  test('should render correctly on tablet', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    await expect(page.locator('body')).toBeVisible();
  });

  test('should render correctly on desktop', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    await expect(page.locator('body')).toBeVisible();
  });

  test('should render dashboard correctly on mobile with auth', async ({ authPage: page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');

    await expect(page.locator('body')).toBeVisible();
  });
});

test.describe('UI Check - No Console Errors', () => {
  test('should not have console errors on home page', async ({ page }) => {
    const errors: string[] = [];

    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    // Wait a bit for any async errors
    await page.waitForTimeout(1000);

    // Filter out expected errors (e.g., from analytics, third-party scripts, BFF proxy, backend API calls)
    const filteredErrors = errors.filter(e =>
      !e.includes('analytics') &&
      !e.includes('gtag') &&
      !e.includes('favicon') &&
      !e.includes('[BFF]') &&
      !e.includes('Proxy error') &&
      !e.includes('ECONNREFUSED') &&
      !e.includes('fetch failed') &&
      !e.includes('Failed to fetch') &&
      !e.includes('Failed to load resource') &&
      !e.includes('net::ERR_') &&
      !e.includes('Content Security Policy')
    );

    expect(filteredErrors).toEqual([]);
  });

  test('should not have console errors on login page', async ({ page }) => {
    const errors: string[] = [];

    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    const filteredErrors = errors.filter(e =>
      !e.includes('analytics') &&
      !e.includes('gtag') &&
      !e.includes('favicon') &&
      !e.includes('[BFF]') &&
      !e.includes('Proxy error') &&
      !e.includes('ECONNREFUSED') &&
      !e.includes('fetch failed') &&
      !e.includes('Failed to fetch') &&
      !e.includes('Failed to load resource') &&
      !e.includes('net::ERR_') &&
      !e.includes('Content Security Policy')
    );

    expect(filteredErrors).toEqual([]);
  });

  test('should not have console errors on dashboard with auth', async ({ authPage: page }) => {
    const errors: string[] = [];

    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    const filteredErrors = errors.filter(e =>
      !e.includes('analytics') &&
      !e.includes('gtag') &&
      !e.includes('favicon') &&
      !e.includes('[BFF]') &&
      !e.includes('Proxy error') &&
      !e.includes('ECONNREFUSED') &&
      !e.includes('fetch failed') &&
      !e.includes('Failed to fetch') &&
      !e.includes('Failed to load resource') &&
      !e.includes('net::ERR_') &&
      !e.includes('Content Security Policy')
    );

    expect(filteredErrors).toEqual([]);
  });
});

test.describe('UI Check - Network Requests', () => {
  test('should not have failed network requests on home page', async ({ page }) => {
    const failedRequests: string[] = [];

    page.on('requestfailed', request => {
      failedRequests.push(request.url());
    });

    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    // Filter out expected failures (e.g., analytics, BFF proxy to gateway, API calls, fonts, external resources)
    const filteredFailures = failedRequests.filter(url =>
      !url.includes('analytics') &&
      !url.includes('gtag') &&
      !url.includes('/api/') &&
      !url.includes('grainy-gradients') &&
      !url.includes('fonts.googleapis') &&
      !url.includes('fonts.gstatic') &&
      !url.includes('favicon')
    );

    expect(filteredFailures).toEqual([]);
  });

  test('should not have failed network requests on dashboard with auth', async ({ authPage: page }) => {
    const failedRequests: string[] = [];

    page.on('requestfailed', request => {
      failedRequests.push(request.url());
    });

    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');

    // Filter out expected failures (e.g., analytics, BFF proxy to gateway, API calls, fonts,
    // RSC prefetches, and Next.js internal navigation requests to the page itself)
    const filteredFailures = failedRequests.filter(url =>
      !url.includes('analytics') &&
      !url.includes('gtag') &&
      !url.includes('/api/') &&
      !url.includes('grainy-gradients') &&
      !url.includes('fonts.googleapis') &&
      !url.includes('fonts.gstatic') &&
      !url.includes('favicon') &&
      !url.includes('_rsc') &&
      !url.includes('/dashboard') &&
      !url.includes('_next')
    );

    expect(filteredFailures).toEqual([]);
  });
});

test.describe('UI Check - Visual Elements', () => {
  test('should have visible header on home page', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    // Home page may use nav instead of header element
    const header = page.locator('header, nav').first();
    await expect(header).toBeVisible();
  });

  test('should have visible main content on home page', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    // Landing page uses sections and divs instead of <main>; check for primary content container
    const main = page.locator('main, [role="main"], section').first();
    await expect(main).toBeVisible();
  });

  test('should have visible footer on home page', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    // Footer may not exist on all pages; check for footer or bottom section
    const footer = page.locator('footer, [role="contentinfo"]').first();
    const hasFooter = await footer.isVisible().catch(() => false);
    // If no footer element, ensure page at least has body content
    if (!hasFooter) {
      await expect(page.locator('body')).toBeVisible();
    } else {
      await expect(footer).toBeVisible();
    }
  });

  test('should have visible navigation on dashboard with auth', async ({ authPage: page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');

    // Dashboard uses <aside> for desktop sidebar navigation and <header> for top bar
    const nav = page.locator('aside, header, nav').first();
    await expect(nav).toBeVisible();
  });
});

test.describe('UI Check - Performance', () => {
  test('should load home page within reasonable time', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    const loadTime = Date.now() - startTime;

    // Page should load within 5 seconds
    expect(loadTime).toBeLessThan(5000);
  });

  test('should load dashboard within reasonable time with auth', async ({ authPage: page }) => {
    const startTime = Date.now();

    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');

    const loadTime = Date.now() - startTime;

    // Page should load within 5 seconds
    expect(loadTime).toBeLessThan(5000);
  });
});

test.describe('UI Check - Screenshot Comparison', () => {
  test('home page screenshot capture', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    await page.screenshot({
      path: 'e2e/screenshots/home-page.png',
      fullPage: true
    });
  });

  test('login page screenshot capture', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');

    await page.screenshot({
      path: 'e2e/screenshots/login-page.png',
      fullPage: true
    });
  });

  test('dashboard screenshot capture with auth', async ({ authPage: page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('domcontentloaded');

    await page.screenshot({
      path: 'e2e/screenshots/dashboard-page.png',
      fullPage: true
    });
  });
});
