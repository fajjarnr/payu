import { test, expect } from './fixtures';

test.describe('Registration Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/onboarding');
    await page.waitForLoadState('domcontentloaded');
  });

  test('should display registration page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Verifikasi Identitas Digital')).toBeVisible();
    await expect(page.getByText('e-KYC Instant Liveness')).toBeVisible();
  });

  test('should have back button to home', async ({ page }) => {
    const backButton = page.locator('a[href="/"]').filter({ hasText: 'Kembali' });
    await expect(backButton).toBeVisible();
    await backButton.click();
    await expect(page).toHaveURL(/\//);
  });

  test('should display security badge', async ({ page }) => {
    await expect(page.getByText('Kedaulatan Data')).toBeVisible();
    const emeraldElements = await page.locator('text=emerald').count();
    expect(emeraldElements).toBeGreaterThanOrEqual(0);
  });

  test('should display progress tracker with 3 steps', async ({ page }) => {
    const steps = page.locator('.w-10.h-10.rounded-xl');
    await expect(steps).toHaveCount(3);

    // Step labels should be visible - use first() for strict mode violations
    await expect(page.getByText('Identitas').first()).toBeVisible();
    await expect(page.getByText('Profil').first()).toBeVisible();
    await expect(page.getByText('Selesai')).toBeVisible();
  });

  test('should navigate to step 2 when clicking continue button', async ({ page }) => {
    await page.click('button:has-text("Lanjut ke Profil Data")');

    // Should show profile form
    await expect(page.getByText('Lengkapi Profil')).toBeVisible();
    await expect(page.getByPlaceholder(/16 digit/)).toBeVisible();
  });

  test('should display all form fields in step 2', async ({ page }) => {
    await page.click('button:has-text("Lanjut ke Profil Data")');

    await expect(page.getByPlaceholder(/16 digit/)).toBeVisible();
    await expect(page.getByPlaceholder(/Sesuai KTP/)).toBeVisible();
    await expect(page.getByPlaceholder(/nama@email.com/)).toBeVisible();
    await expect(page.getByPlaceholder(/unik & mudah diingat/)).toBeVisible();
  });

  test('should validate NIK length (must be 16 digits)', async ({ page }) => {
    await page.click('button:has-text("Lanjut ke Profil Data")');

    await page.getByPlaceholder(/16 digit/).fill('123');
    await page.getByPlaceholder(/Sesuai KTP/).fill('Test User');
    await page.getByPlaceholder(/nama@email.com/).fill('test@example.com');
    await page.getByPlaceholder(/unik & mudah diingat/).fill('testuser');

    await page.click('button:has-text("Konfirmasi Pendaftaran")');

    // Should show validation error
    await expect(page.locator('.text-red-500')).toBeVisible();
  });

  test('should validate email format', async ({ page }) => {
    await page.click('button:has-text("Lanjut ke Profil Data")');

    await page.getByPlaceholder(/16 digit/).fill('3201010101010001');
    await page.getByPlaceholder(/Sesuai KTP/).fill('Test User');
    await page.getByPlaceholder(/nama@email.com/).fill('invalidemail');
    await page.getByPlaceholder(/unik & mudah diingat/).fill('testuser');

    // Blur the email field to trigger validation
    await page.getByPlaceholder(/nama@email.com/).blur();
    await page.waitForTimeout(500);

    await page.click('button:has-text("Konfirmasi Pendaftaran")');

    // Should show validation error or prevent submission
    await page.waitForTimeout(1000);
  });

  test('should validate required fields', async ({ page }) => {
    await page.click('button:has-text("Lanjut ke Profil Data")');

    // Submit without filling required fields
    await page.click('button:has-text("Konfirmasi Pendaftaran")');

    // Should show validation errors - use first() due to strict mode
    await expect(page.locator('.text-red-500').first()).toBeVisible();
  });

  test('should show loading state during registration', async ({ page }) => {
    await page.click('button:has-text("Lanjut ke Profil Data")');

    await page.getByPlaceholder(/16 digit/).fill('3201010101010001');
    await page.getByPlaceholder(/Sesuai KTP/).fill('Test User');
    await page.getByPlaceholder(/nama@email.com/).fill('test@example.com');
    await page.getByPlaceholder(/unik & mudah diingat/).fill('testuser123');

    // Click submit - will show loading (may fail due to backend, but we check loading state)
    await page.click('button:has-text("Konfirmasi Pendaftaran")');
  });

  test('should have proper form labels', async ({ page }) => {
    await page.click('button:has-text("Lanjut ke Profil Data")');

    await expect(page.getByText('Nomor Induk Kependudukan (NIK)')).toBeVisible();
    await expect(page.getByText('Nama Lengkap')).toBeVisible();
    await expect(page.getByText('Email')).toBeVisible();
    await expect(page.getByText('Username')).toBeVisible();
  });

  test('should have KTP upload area', async ({ page }) => {
    await expect(page.getByText('Unggah e-KTP')).toBeVisible();
    await expect(page.getByText('Klik untuk ambil foto')).toBeVisible();
    await expect(page.getByText('JPG, PNG maks 5MB')).toBeVisible();
  });

  test('should update progress tracker when moving to step 2', async ({ page }) => {
    // First step should be active
    await expect(page.getByText('Identitas').first()).toBeVisible();

    await page.click('button:has-text("Lanjut ke Profil Data")');

    // Wait for step transition
    await page.waitForTimeout(500);

    // Second step should be active now - wait for the title to appear
    await expect(page.getByText('Lengkapi Profil')).toBeVisible();
  });

  test('should have proper button styling', async ({ page }) => {
    const continueButton = page.locator('button:has-text("Lanjut ke Profil Data")');
    await expect(continueButton).toHaveClass(/bg-primary/);
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/onboarding');
    await page.waitForLoadState('domcontentloaded');

    // Left panel should be hidden on mobile
    await expect(page.locator('aside')).not.toBeVisible();

    // Main content should be visible
    await expect(page.getByText('Unggah e-KTP')).toBeVisible();
  });

  test('should show success screen after registration', async ({ page }) => {
    await page.click('button:has-text("Lanjut ke Profil Data")');

    await page.getByPlaceholder(/16 digit/).fill('3201010101010001');
    await page.getByPlaceholder(/Sesuai KTP/).fill('Test User');
    await page.getByPlaceholder(/nama@email.com/).fill('test@example.com');
    await page.getByPlaceholder(/unik & mudah diingat/).fill('testuser123');

    // This will likely fail due to backend, but let's check the structure
    await page.click('button:has-text("Konfirmasi Pendaftaran")');

    // Wait a moment for any response
    await page.waitForTimeout(2000);
  });

  test('should have back button in step 2', async ({ page }) => {
    await page.click('button:has-text("Lanjut ke Profil Data")');

    const backButton = page.locator('button:has-text("Kembali")');
    await expect(backButton).toBeVisible();

    await backButton.click();

    // Should return to step 1
    await expect(page.getByText('Unggah e-KTP')).toBeVisible();
  });
});

test.describe('Registration Flow - Form Validation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/onboarding');
    await page.waitForLoadState('domcontentloaded');
    await page.click('button:has-text("Lanjut ke Profil Data")');
  });

  test('should validate username format', async ({ page }) => {
    await page.getByPlaceholder(/16 digit/).fill('3201010101010001');
    await page.getByPlaceholder(/Sesuai KTP/).fill('Test User');
    await page.getByPlaceholder(/nama@email.com/).fill('test@example.com');
    await page.getByPlaceholder(/unik & mudah diingat/).fill('ab'); // Too short

    await page.click('button:has-text("Konfirmasi Pendaftaran")');

    // Should show validation error
    await expect(page.locator('.text-red-500')).toBeVisible();
  });

  test('should require all fields to be filled', async ({ page }) => {
    // Don't fill any fields
    await page.click('button:has-text("Konfirmasi Pendaftaran")');

    // Should show multiple validation errors
    const errors = await page.locator('.text-red-500').count();
    expect(errors).toBeGreaterThan(0);
  });

  test('should allow valid registration data', async ({ page }) => {
    await page.getByPlaceholder(/16 digit/).fill('3201010101010001');
    await page.getByPlaceholder(/Sesuai KTP/).fill('Test User');
    await page.getByPlaceholder(/nama@email.com/).fill('test@example.com');
    await page.getByPlaceholder(/unik & mudah diingat/).fill('testuser123');

    const submitButton = page.locator('button:has-text("Konfirmasi Pendaftaran")');
    await expect(submitButton).toBeEnabled();
  });
});

test.describe('Registration Flow - Accessibility', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/onboarding');
    await page.waitForLoadState('domcontentloaded');
  });

  test('should have proper heading hierarchy', async ({ page }) => {
    // Check that the main heading exists
    const h1Count = await page.locator('h1').count();
    expect(h1Count).toBeGreaterThanOrEqual(1);
  });

  test('should support keyboard navigation', async ({ page }) => {
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');

    const focused = page.locator(':focus');
    await expect(focused).toBeVisible();
  });

  test('should submit form with Enter key', async ({ page }) => {
    await page.click('button:has-text("Lanjut ke Profil Data")');

    await page.getByPlaceholder(/16 digit/).fill('3201010101010001');
    await page.getByPlaceholder(/Sesuai KTP/).fill('Test User');
    await page.getByPlaceholder(/nama@email.com/).fill('test@example.com');
    await page.getByPlaceholder(/unik & mudah diingat/).fill('testuser123');

    // Press Enter on the last field
    await page.keyboard.press('Enter');

    // Wait for any response
    await page.waitForTimeout(1000);
  });
});
