import { test, expect } from './fixtures';

test.describe('Login Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');
  });

  test('should display login page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Selamat Datang Kembali')).toBeVisible();
    await expect(page.getByText('Masuk ke dashboard finansial Anda')).toBeVisible();
  });

  test('should have branding panel on desktop', async ({ page }) => {
    const brandingPanel = page.locator('aside[aria-label="Branding"]');
    await expect(brandingPanel).toBeVisible();
    await expect(page.getByText('Platform Perbankan Digital Masa Depan')).toBeVisible();
  });

  test('should render the OIDC sign-in button and NO local password form (LOGIN-003)', async ({ page }) => {
    await expect(page.getByTestId('login-submit-button')).toBeVisible();
    await expect(page.getByPlaceholder('username123')).toHaveCount(0);
    await expect(page.getByPlaceholder('••••••••')).toHaveCount(0);
  });

  test('should have register link', async ({ page }) => {
    const registerLink = page.getByText('Daftar Sekarang');
    await expect(registerLink).toBeVisible();
    await expect(registerLink).toHaveAttribute('href', '/onboarding');
  });

  test('should navigate to registration page', async ({ page }) => {
    await page.click('text=Daftar Sekarang');
    await expect(page).toHaveURL(/\/onboarding/);
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');

    await expect(page.getByText('Selamat Datang Kembali')).toBeVisible();
    await expect(page.getByTestId('login-submit-button')).toBeVisible();

    await page.screenshot({
      path: 'e2e/screenshots/login-mobile.png',
      fullPage: true
    });
  });

  test('should display security features on branding panel', async ({ page }) => {
    await expect(page.getByText('Keamanan Tingkat Enterprise')).toBeVisible();
    await expect(page.getByText('Enkripsi End-to-End Standar Militer')).toBeVisible();
    await expect(page.getByText('Monitoring Transaksi Real-time AI')).toBeVisible();
  });

  test('should have "or" divider between sign-in and register link', async ({ page }) => {
    await expect(page.getByText('Atau')).toBeVisible();
  });

  test('should have no account text with register link', async ({ page }) => {
    await expect(page.getByText('Belum memiliki akun?')).toBeVisible();
    await expect(page.getByText('Daftar Sekarang')).toBeVisible();
  });
});

test.describe('Login Flow - OIDC PKCE journey (LOGIN-003)', () => {
  test('sign-in button redirects to the BFF authorize endpoint', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');

    // Intercept the Keycloak authorize redirect to stop the flow at the IdP
    await page.route('**/realms/payu/protocol/openid-connect/auth?**', (route) =>
      route.fulfill({ status: 200, contentType: 'text/html', body: '<html><body>Keycloak login page</body></html>' }),
    );

    await page.click('[data-testid="login-submit-button"]');

    // The BFF must redirect to Keycloak's authorization endpoint
    await page.waitForURL('**/realms/payu/protocol/openid-connect/auth?**', { timeout: 10000 });
    const url = new URL(page.url());
    expect(url.searchParams.get('client_id')).toBe('payu-web-app');
    expect(url.searchParams.get('response_type')).toBe('code');
    expect(url.searchParams.get('code_challenge_method')).toBe('S256');
    expect(url.searchParams.get('code_challenge')).toMatch(/^[A-Za-z0-9_-]{43}$/);
    expect(url.searchParams.get('redirect_uri')).toContain('/api/auth/callback');
  });

  test('callback with mismatched CSRF state never reaches the gateway', async ({ page }) => {
    await page.route('**/api/v1/auth/callback', () => { throw new Error('must not be called'); });

    // Drive the callback URL directly with a state that does not match the cookie
    await page.goto('/login');
    await page.evaluate(() => {
      document.cookie = 'oidc_state=expected-state-1234567890; path=/';
      document.cookie = 'pkce_verifier=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789; path=/';
    });
    await page.goto('/api/auth/callback?code=x&state=attacker-state-123456789');
    await page.waitForURL('**/login?error=invalid_state**', { timeout: 10000 });
  });

  test('real login lands on the dashboard with httpOnly cookies (LOGIN-001)', async ({ page }) => {
    // Requires the local podman stack (Keycloak on :8099 with the seeded realm).
    // The seeded users carry a temporary password, so the first login shows
    // Keycloak's "update password" page; update it and continue.
    const newPassword = 'Dev-customer1-new-2026';
    await page.goto('/login');
    await page.click('[data-testid="login-submit-button"]');
    await page.waitForURL('**/realms/payu/protocol/openid-connect/auth?**', { timeout: 15000 });

    if (await page.getByRole('button', { name: 'Sign In' }).isVisible().catch(() => false)) {
      await page.getByRole('textbox', { name: 'Username or email' }).fill('customer1');
      await page.getByRole('textbox', { name: 'Password' }).fill('P@ssw0rd12345');
      await page.getByRole('button', { name: 'Sign In' }).click();
    }

    // Handle the temporary-password update page if shown (fresh realm import).
    const updatePassword = page.getByText(/Update Password|Perbarui Password/i);
    if (await updatePassword.isVisible().catch(() => false)) {
      await page.locator('#new-password').fill(newPassword);
      await page.locator('#confirm-password').fill(newPassword);
      await page.getByRole('button', { name: /Submit|Kirim/i }).click();
    }

    await page.waitForURL('**/dashboard', { timeout: 20000 });
    const cookies = await page.context().cookies();
    const accessCookie = cookies.find((c) => c.name === 'accessToken');
    expect(accessCookie).toBeDefined();
    expect(accessCookie?.httpOnly).toBe(true);
    expect(accessCookie?.sameSite).toBe('Strict');
    const refreshCookie = cookies.find((c) => c.name === 'refreshToken');
    expect(refreshCookie?.httpOnly).toBe(true);
    // The PKCE verifier must be consumed (deleted)
    expect(cookies.find((c) => c.name === 'pkce_verifier')).toBeUndefined();
  });
});

test.describe('Login Flow - Accessibility', () => {
  test('should have proper heading hierarchy', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');

    const h1 = page.locator('h1');
    await expect(h1).toBeVisible();
    await expect(h1).toContainText('Platform Perbankan');
  });

  test('should have keyboard navigation support', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');

    const signInButton = page.getByTestId('login-submit-button');
    const registerLink = page.getByTestId('register-link');

    await expect(signInButton).toBeVisible();
    await expect(registerLink).toBeVisible();

    await signInButton.focus();
    await expect(signInButton).toBeFocused();

    await page.keyboard.press('Tab');
    await page.waitForTimeout(150);
    await expect(registerLink).toBeFocused();
  });
});
