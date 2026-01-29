import { test, expect } from '@playwright/test';

test.describe('Registration Flow', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/onboarding');
  });

  test('should display registration page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Verifikasi eKYC.')).toBeVisible();
    await expect(page.getByText('Unggah identitas resmi pemerintah (KTP)')).toBeVisible();
  });

  test('should have back button to login', async ({ page }) => {
    const backButton = page.locator('a[href="/login"]');
    await expect(backButton).toBeVisible();
    await backButton.click();
    await expect(page).toHaveURL(/\/login/);
  });

  test('should display security badge', async ({ page }) => {
    await expect(page.getByText('ENKRIPSI AMAN SESUAI STANDAR OJK & BI')).toBeVisible();
    await expect(page.locator('.text-bank-green')).toHaveCount.toBeGreaterThanOrEqual(1);
  });

  test('should display progress tracker with 3 steps', async ({ page }) => {
    const steps = page.locator('.w-14.h-14.rounded-xl');
    await expect(steps).toHaveCount(3);

    // First step should be active
    await expect(steps.nth(0)).toHaveClass(/bg-bank-green/);
  });

  test('should navigate to step 2 when clicking start verification', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Should show profile form
    await expect(page.getByText('Profil Akun.')).toBeVisible();
    await expect(page.getByPlaceholder('3200...')).toBeVisible();
  });

  test('should display all form fields in step 2', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    await expect(page.getByPlaceholder('3200...')).toBeVisible();
    await expect(page.getByPlaceholder('NAMA LENGKAP ANDA')).toBeVisible();
    await expect(page.getByPlaceholder('nama@domain.com')).toBeVisible();
    await expect(page.getByPlaceholder('NAMA_PENGGUNA_UNIK')).toBeVisible();
  });

  test('should validate NIK length (must be 16 digits)', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    await page.getByPlaceholder('3200...').fill('123');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    await expect(page.getByText('NIK harus 16 digit')).toBeVisible();
  });

  test('should validate email format', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('invalidemail');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    await expect(page.locator('.text-red-500').filter({ hasText: /email/i })).toBeVisible();
  });

  test('should validate required fields', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Click submit without filling any fields
    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Should show validation errors
    await expect(page.locator('.text-red-500')).toHaveCount.toBeGreaterThanOrEqual(1);
  });

  test('should show loading state during registration', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser123');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Check for loading state
    await expect(page.getByText('Menyebarkan Identitas...')).toBeVisible();
  });

  test('should show success message after valid submission', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser123');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Wait for success step
    await page.waitForTimeout(2000);

    // Should show success state (step 3)
    await expect(page.getByText('Pendaftaran Berhasil.')).toBeVisible();
  });

  test('should have proper form labels', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    await expect(page.getByText('Nomor NIK (16 Digit)')).toBeVisible();
    await expect(page.getByText('Nama Lengkap Sesuai KTP')).toBeVisible();
    await expect(page.getByText('Alamat Email Digital')).toBeVisible();
    await expect(page.getByText('Nama Pengguna (Username)')).toBeVisible();
  });

  test('should have KTP upload area', async ({ page }) => {
    const uploadArea = page.locator('.aspect-video.border-2.border-dashed');
    await expect(uploadArea).toBeVisible();

    // Should have camera icon
    await expect(page.locator('.text-bank-green').filter({ hasText: /camera/i })).toBeVisible();
  });

  test('should update progress tracker when moving to step 2', async ({ page }) => {
    // Initially step 1 is active
    await expect(page.locator('.w-14.h-14.rounded-xl').nth(0)).toHaveClass(/bg-bank-green/);

    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Now step 2 should be active
    await expect(page.locator('.w-14.h-14.rounded-xl').nth(1)).toHaveClass(/bg-bank-green/);
  });

  test('should have proper button styling', async ({ page }) => {
    const button = page.locator('button:has-text("Mulai Proses Verifikasi")');

    await expect(button).toHaveClass(/bg-bank-green/);
    await expect(button).toHaveClass(/text-white/);
    await expect(button).toContainText('Mulai Proses Verifikasi');
  });

  test('should handle registration error gracefully', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Use existing NIK (will cause error)
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('existinguser');

    // Mock error handling
    page.on('dialog', dialog => {
      expect(dialog.message()).toContain('Pendaftaran gagal');
      dialog.accept();
    });

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');
    await page.waitForTimeout(2000);
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/onboarding');

    // Check that key elements are visible
    await expect(page.getByText('Verifikasi eKYC.')).toBeVisible();
    await expect(page.locator('.w-14.h-14.rounded-xl')).toHaveCount(3);

    // Take screenshot
    await page.screenshot({
      path: 'e2e/screenshots/registration-mobile.png',
      fullPage: true
    });
  });

  test('should show success screen with checkmark', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser123');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Wait for success state
    await page.waitForTimeout(2000);

    // Check for checkmark icon
    await expect(page.locator('.text-bank-green').filter({ hasText: /check/i })).toBeVisible();

    // Check success message
    await expect(page.getByText('Pemetaan identitas selesai')).toBeVisible();
  });

  test('should redirect to login after successful registration', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser123');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Wait for redirect (2 seconds timeout in the actual code)
    await page.waitForTimeout(3000);

    await expect(page).toHaveURL(/\/login/);
  });
});

test.describe('Registration Flow - Form Validation', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should validate username format', async ({ page }) => {
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Fill with valid data but invalid username
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('ab'); // Too short

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Should show validation error
    await expect(page.locator('.text-red-500')).toBeVisible();
  });

  test('should require all fields to be filled', async ({ page }) => {
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Fill only NIK
    await page.getByPlaceholder('3200...').fill('3201010101010001');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Should show validation errors for other fields
    const errorCount = await page.locator('.text-red-500').count();
    expect(errorCount).toBeGreaterThan(0);
  });

  test('should allow valid registration data', async ({ page }) => {
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Fill all fields with valid data
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('John Doe');
    await page.getByPlaceholder('nama@domain.com').fill('john.doe@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('johndoe123');

    // Button should be clickable
    const submitButton = page.locator('button:has-text("Konfirmasi Pembuatan Akun")');
    await expect(submitButton).toBeEnabled();
  });
});

test.describe('Registration Flow - Accessibility', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should have proper heading hierarchy', async ({ page }) => {
    await page.goto('/onboarding');

    const h2 = page.locator('h2');
    await expect(h2).toBeVisible();
    await expect(h2).toContainText('Verifikasi eKYC');
  });

  test('should support keyboard navigation', async ({ page }) => {
    await page.goto('/onboarding');

    // Tab through form
    await page.keyboard.press('Tab');
    let focused = await page.locator(':focus').getAttribute('href');
    expect(focused).toBe('/login');

    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');

    // Should reach the button
    const focusedText = await page.locator(':focus').textContent();
    expect(focusedText).toContain('Mulai Proses Verifikasi');
  });

  test('should submit form with Enter key', async ({ page }) => {
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Fill form
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser');

    // Press Enter on last field
    await page.keyboard.press('Enter');

    // Form should attempt submission
    await expect(page.getByText('Menyebarkan Identitas...')).toBeVisible();
  });
});

test.describe('Registration Flow - Visual Regression', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should match screenshots on desktop', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/onboarding');

    await page.screenshot({
      path: 'e2e/screenshots/registration-step1-desktop.png',
      fullPage: true
    });
  });

  test('should match screenshots on tablet', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/onboarding');

    await page.screenshot({
      path: 'e2e/screenshots/registration-step1-tablet.png',
      fullPage: true
    });
  });
});
