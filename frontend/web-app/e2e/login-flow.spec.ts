import { test, expect } from '@playwright/test';

test.describe('Login Flow', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('should display login page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Selamat Datang.')).toBeVisible();
    await expect(page.getByText('Portal Perbankan Digital Aman')).toBeVisible();
  });

  test('should display logo with pulse animation', async ({ page }) => {
    const logo = page.locator('.h-24.w-24.mx-auto');
    await expect(logo).toBeVisible();
    await expect(logo).toHaveText(/U/);

    const pulseDot = page.locator('.animate-pulse');
    await expect(pulseDot).toBeVisible();
  });

  test('should have username and password fields', async ({ page }) => {
    await expect(page.getByPlaceholder('Username atau ID Akun')).toBeVisible();
    await expect(page.getByPlaceholder('••••••••••••')).toBeVisible();
  });

  test('should have forgot password link', async ({ page }) => {
    const forgotLink = page.getByText('Lupa / Riset Akses ?');
    await expect(forgotLink).toBeVisible();
    await expect(forgotLink).toHaveAttribute('href', '#');
  });

  test('should validate required fields', async ({ page }) => {
    await page.click('button[type="submit"]');

    // Check for validation errors
    const usernameError = page.locator('p:has-text("username")').or(page.locator('.text-red-500'));
    await expect(usernameError).toBeVisible();
  });

  test('should show validation error for invalid credentials format', async ({ page }) => {
    await page.fill('input[placeholder="Username atau ID Akun"]', 'ab');
    await page.fill('input[placeholder="••••••••••••"]', '123');

    await page.click('button[type="submit"]');

    // Wait for validation
    await page.waitForTimeout(500);

    // Check that we're still on login page (validation failed)
    await expect(page.getByText('Selamat Datang.')).toBeVisible();
  });

  test('should allow typing in username field', async ({ page }) => {
    const usernameInput = page.getByPlaceholder('Username atau ID Akun');
    await usernameInput.fill('testuser');
    await expect(usernameInput).toHaveValue('testuser');
  });

  test('should allow typing in password field', async ({ page }) => {
    const passwordInput = page.getByPlaceholder('••••••••••••');
    await passwordInput.fill('password123');
    await expect(passwordInput).toHaveValue('password123');
  });

  test('should mask password input', async ({ page }) => {
    const passwordInput = page.getByPlaceholder('••••••••••••');
    await passwordInput.fill('mypassword');
    await expect(passwordInput).toHaveValue('mypassword');

    // Verify input type is password
    const inputType = await passwordInput.getAttribute('type');
    expect(inputType).toBe('password');
  });

  test('should have register link', async ({ page }) => {
    const registerLink = page.getByText('Buat Akun Baru');
    await expect(registerLink).toBeVisible();
    await expect(registerLink).toHaveAttribute('href', '/onboarding');
  });

  test('should navigate to registration page', async ({ page }) => {
    await page.click('text=Buat Akun Baru');
    await expect(page).toHaveURL(/\/onboarding/);
    await expect(page.getByText('Verifikasi eKYC')).toBeVisible();
  });

  test('should show loading state during login', async ({ page }) => {
    await page.fill('input[placeholder="Username atau ID Akun"]', 'testuser');
    await page.fill('input[placeholder="••••••••••••"]', 'password123');

    // Click submit and immediately check for loading state
    const submitPromise = page.click('button[type="submit"]');

    // Check for loading text (should appear briefly)
    await expect(page.getByText('Memvalidasi Akun...')).toBeVisible();

    await submitPromise;
  });

  test('should show protocol version', async ({ page }) => {
    await expect(page.getByText('Protokol Autentikasi v1.4.2-IND')).toBeVisible();
  });

  test('should have proper form labels', async ({ page }) => {
    await expect(page.getByText('Pengenal Kredensial (Username)')).toBeVisible();
    await expect(page.getByText('Kunci Kata Sandi (Password)')).toBeVisible();
  });

  test('should handle login error gracefully', async ({ page }) => {
    // Mock login failure by using invalid credentials
    await page.fill('input[placeholder="Username atau ID Akun"]', 'invaliduser');
    await page.fill('input[placeholder="••••••••••••"]', 'wrongpassword');

    // Click submit
    await page.click('button[type="submit"]');

    // Wait for API response
    await page.waitForTimeout(2000);

    // Check for error alert (this is what the actual page shows on error)
    page.on('dialog', dialog => {
      expect(dialog.message()).toContain('Login gagal');
      dialog.accept();
    });
  });

  test('should have accessible form controls', async ({ page }) => {
    const usernameInput = page.getByPlaceholder('Username atau ID Akun');
    const passwordInput = page.getByPlaceholder('••••••••••••');

    // Check for proper attributes
    await expect(usernameInput).toBeVisible();
    await expect(passwordInput).toBeVisible();

    // Check that inputs are focusable
    await usernameInput.focus();
    await expect(usernameInput).toBeFocused();

    await passwordInput.focus();
    await expect(passwordInput).toBeFocused();
  });

  test('should have proper styling on focus', async ({ page }) => {
    const usernameInput = page.getByPlaceholder('Username atau ID Akun');
    await usernameInput.focus();

    // Check for ring effect (focus ring)
    const focusedElement = page.locator(':focus');
    await expect(focusedElement).toBeVisible();
  });

  test('should submit form with valid credentials format', async ({ page }) => {
    await page.fill('input[placeholder="Username atau ID Akun"]', 'validuser123');
    await page.fill('input[placeholder="••••••••••••"]', 'ValidPass123!');

    // Form should be submittable
    const submitButton = page.locator('button[type="submit"]');
    await expect(submitButton).toBeEnabled();

    // Note: Actual login will fail in test environment, but form submission should work
    await page.click('button[type="submit"]');

    // Wait for loading state
    await expect(page.getByText('Memvalidasi Akun...')).toBeVisible();
  });

  test('should have consistent branding', async ({ page }) => {
    // Check for green color scheme (bank-green)
    const logo = page.locator('.bg-bank-green');
    await expect(logo).toBeVisible();

    const submitButton = page.locator('button[type="submit"]');
    await expect(submitButton).toHaveClass(/bg-bank-green/);
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });

    // Refresh page with new viewport
    await page.goto('/login');

    // Check that elements are still visible and properly sized
    await expect(page.getByText('Selamat Datang.')).toBeVisible();
    await expect(page.getByPlaceholder('Username atau ID Akun')).toBeVisible();

    // Take screenshot for visual regression
    await page.screenshot({
      path: 'e2e/screenshots/login-mobile.png',
      fullPage: true
    });
  });

  test('should take screenshot on test failure', async ({ page }) => {
    // This test will fail intentionally to demonstrate screenshot capture
    await expect(page.getByText('This text does not exist')).toBeVisible();
  });
});

test.describe('Login Flow - Success Path', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should complete successful login journey', async ({ page }) => {
    await page.goto('/login');

    // Fill in credentials
    await page.fill('input[placeholder="Username atau ID Akun"]', 'testuser');
    await page.fill('input[placeholder="••••••••••••"]', 'password123');

    // Submit form
    await page.click('button[type="submit"]');

    // In a real scenario with valid credentials, user would be redirected
    // For now, we just verify the form submission process
    await expect(page.getByText('Memvalidasi Akun...')).toBeVisible();
  });
});

test.describe('Login Flow - Accessibility', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should have proper heading hierarchy', async ({ page }) => {
    await page.goto('/login');

    // Check for main heading
    const h1 = page.locator('h1');
    await expect(h1).toBeVisible();
    await expect(h1).toContainText('Selamat Datang');
  });

  test('should have keyboard navigation support', async ({ page }) => {
    await page.goto('/login');

    // Tab through form elements
    await page.keyboard.press('Tab');
    let focused = await page.locator(':focus').textContent();
    expect(focused).toBe('U'); // Logo is first focusable

    await page.keyboard.press('Tab');
    focused = await page.locator(':focus').getAttribute('placeholder');
    expect(focused).toBe('Username atau ID Akun');
  });

  test('should submit form with Enter key', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[placeholder="Username atau ID Akun"]', 'testuser');
    await page.fill('input[placeholder="••••••••••••"]', 'password123');

    // Press Enter on password field
    await page.keyboard.press('Enter');

    // Form should submit
    await expect(page.getByText('Memvalidasi Akun...')).toBeVisible();
  });
});
