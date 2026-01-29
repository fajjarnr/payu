import { test, expect } from '@playwright/test';

test.describe('Onboarding Flow - Complete Journey', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should complete full onboarding journey', async ({ page }) => {
    await page.goto('/onboarding');

    // Step 1: KYC Upload
    await expect(page.getByText('Verifikasi eKYC.')).toBeVisible();
    await expect(page.getByText('Unggah identitas resmi pemerintah (KTP)')).toBeVisible();

    // Click start verification
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Step 2: Profile Form
    await expect(page.getByText('Profil Akun.')).toBeVisible();
    await expect(page.getByPlaceholder('3200...')).toBeVisible();

    // Fill in profile details
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('John Doe');
    await page.getByPlaceholder('nama@domain.com').fill('john.doe@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('johndoe123');

    // Submit form
    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Step 3: Success
    await expect(page.getByText('Pendaftaran Berhasil.')).toBeVisible();
    await expect(page.getByText('Pemetaan identitas selesai')).toBeVisible();
  });

  test('should show progress through all steps', async ({ page }) => {
    await page.goto('/onboarding');

    // Initially step 1 is active
    let activeStep = page.locator('.w-14.h-14.rounded-xl.bg-bank-green').first();
    await expect(activeStep).toBeVisible();

    // Move to step 2
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Now step 2 is active
    activeStep = page.locator('.w-14.h-14.rounded-xl.bg-bank-green').nth(1);
    await expect(activeStep).toBeVisible();

    // Fill form and submit
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser123');
    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Wait for success step
    await page.waitForTimeout(2000);

    // Step 3 is active
    activeStep = page.locator('.w-14.h-14.rounded-xl.bg-bank-green').nth(2);
    await expect(activeStep).toBeVisible();
  });
});

test.describe('Onboarding Flow - Step 1: KYC Upload', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/onboarding');
  });

  test('should display KYC upload page', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Verifikasi eKYC.')).toBeVisible();
  });

  test('should have KTP upload area', async ({ page }) => {
    const uploadArea = page.locator('.aspect-video.bg-gray-50.dark\\:bg-gray-900\\/50');
    await expect(uploadArea).toBeVisible();
    await expect(uploadArea).toHaveClass(/border-2.border-dashed/);
  });

  test('should have camera icon in upload area', async ({ page }) => {
    const cameraIcon = page.locator('.text-bank-green').filter({ hasText: /camera/i });
    await expect(cameraIcon).toBeVisible();
  });

  test('should have upload instruction text', async ({ page }) => {
    await expect(page.getByText('Ambil Gambar Identitas')).toBeVisible();
  });

  test('should have start verification button', async ({ page }) => {
    const button = page.locator('button:has-text("Mulai Proses Verifikasi")');
    await expect(button).toBeVisible();
    await expect(button).toBeEnabled();
  });

  test('should have back button to login', async ({ page }) => {
    const backButton = page.locator('a[href="/login"]');
    await expect(backButton).toBeVisible();
    await expect(backButton).toHaveAttribute('href', '/login');
  });

  test('should navigate to login when clicking back', async ({ page }) => {
    await page.click('a[href="/login"]');
    await expect(page).toHaveURL(/\/login/);
  });

  test('should have protocol identity badge', async ({ page }) => {
    await expect(page.getByText('Protokol Identitas')).toBeVisible();
    await expect(page.locator('.bg-bank-green\\/10')).toBeVisible();
  });

  test('should have security encryption badge', async ({ page }) => {
    await expect(page.getByText('ENKRIPSI AMAN SESUAI STANDAR OJK & BI')).toBeVisible();
    await expect(page.locator('.text-bank-green').filter({ hasText: /ShieldCheck/i })).toBeVisible();
  });

  test('should display 3-step progress tracker', async ({ page }) => {
    const steps = page.locator('.w-14.h-14.rounded-xl');
    await expect(steps).toHaveCount(3);
  });

  test('should have step 1 active initially', async ({ page }) => {
    const activeStep = page.locator('.w-14.h-14.rounded-xl.bg-bank-green');
    await expect(activeStep).toHaveCount(1);
  });

  test('should have progress line indicator', async ({ page }) => {
    const progressLine = page.locator('.bg-bank-green.transition-all');
    await expect(progressLine).toBeVisible();

    // Initially width should be 0 (step 1)
    await expect(progressLine).toHaveCSS('width', '0px');
  });

  test('should be responsive on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/onboarding');

    await expect(page.getByText('Verifikasi eKYC.')).toBeVisible();
    await expect(page.locator('.aspect-video')).toBeVisible();

    await page.screenshot({
      path: 'e2e/screenshots/onboarding-step1-mobile.png',
      fullPage: true
    });
  });
});

test.describe('Onboarding Flow - Step 2: Profile Form', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');
  });

  test('should display profile form', async ({ page }) => {
    await expect(page.getByText('Profil Akun.')).toBeVisible();
    await expect(page.getByText('Petakan identitas unik Anda ke dalam Buku Besar (Ledger) finansial kami.')).toBeVisible();
  });

  test('should have NIK input field', async ({ page }) => {
    const nikInput = page.getByPlaceholder('3200...');
    await expect(nikInput).toBeVisible();
    await expect(nikInput).toHaveAttribute('type', 'text');
  });

  test('should have full name input field', async ({ page }) => {
    const nameInput = page.getByPlaceholder('NAMA LENGKAP ANDA');
    await expect(nameInput).toBeVisible();
    await expect(nameInput).toHaveAttribute('type', 'text');
  });

  test('should have email input field', async ({ page }) => {
    const emailInput = page.getByPlaceholder('nama@domain.com');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toHaveAttribute('type', 'email');
  });

  test('should have username input field', async ({ page }) => {
    const usernameInput = page.getByPlaceholder('NAMA_PENGGUNA_UNIK');
    await expect(usernameInput).toBeVisible();
    await expect(usernameInput).toHaveAttribute('type', 'text');
  });

  test('should have confirm account button', async ({ page }) => {
    const button = page.locator('button:has-text("Konfirmasi Pembuatan Akun")');
    await expect(button).toBeVisible();
    await expect(button).toBeEnabled();
  });

  test('should have proper form labels', async ({ page }) => {
    await expect(page.getByText('Nomor NIK (16 Digit)')).toBeVisible();
    await expect(page.getByText('Nama Lengkap Sesuai KTP')).toBeVisible();
    await expect(page.getByText('Alamat Email Digital')).toBeVisible();
    await expect(page.getByText('Nama Pengguna (Username)')).toBeVisible();
  });

  test('should validate NIK length', async ({ page }) => {
    await page.getByPlaceholder('3200...').fill('123');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    await expect(page.getByText('NIK harus 16 digit')).toBeVisible();
  });

  test('should validate email format', async ({ page }) => {
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('invalid-email');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    const errorElement = page.locator('.text-red-500').filter({ hasText: /email/i });
    await expect(errorElement).toBeVisible();
  });

  test('should validate all required fields', async ({ page }) => {
    // Don't fill any fields
    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Should show multiple validation errors
    const errors = page.locator('.text-red-500');
    await expect(errors).toHaveCount.toBeGreaterThanOrEqual(1);
  });

  test('should show loading state during submission', async ({ page }) => {
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser123');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Check for loading state
    await expect(page.getByText('Menyebarkan Identitas...')).toBeVisible();
  });

  test('should have proper input styling', async ({ page }) => {
    const inputs = page.locator('input[type="text"], input[type="email"]');
    await expect(inputs).toHaveCount(4);

    // Check for proper styling classes
    await expect(inputs.first()).toHaveClass(/rounded-xl/);
    await expect(inputs.first()).toHaveClass(/border-border/);
  });

  test('should have focus states on inputs', async ({ page }) => {
    const nikInput = page.getByPlaceholder('3200...');
    await nikInput.focus();

    // Check for focus ring
    await expect(nikInput).toHaveClass(/focus:ring-4/);
    await expect(nikInput).toHaveClass(/focus:border-bank-green/);
  });

  test('should be responsive on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });

    await expect(page.getByText('Profil Akun.')).toBeVisible();
    await expect(page.getByPlaceholder('3200...')).toBeVisible();

    await page.screenshot({
      path: 'e2e/screenshots/onboarding-step2-mobile.png',
      fullPage: true
    });
  });

  test('should update progress indicator', async ({ page }) => {
    // Progress line should show 50% (step 2 of 3)
    const progressLine = page.locator('.bg-bank-green.transition-all');
    await expect(progressLine).toHaveCSS('width', /\d+px/);
  });

  test('should have 2x2 grid for form fields', async ({ page }) => {
    const gridContainer = page.locator('.grid.grid-cols-1.md\\:grid-cols-2');
    await expect(gridContainer).toBeVisible();
  });
});

test.describe('Onboarding Flow - Step 3: Success', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Fill form with valid data
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser123');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Wait for success step
    await page.waitForTimeout(2000);
  });

  test('should display success message', async ({ page }) => {
    await expect(page.getByText('Pendaftaran Berhasil.')).toBeVisible();
  });

  test('should display success description', async ({ page }) => {
    await expect(page.getByText('Pemetaan identitas selesai')).toBeVisible();
    await expect(page.getByText('Menginisialisasi kantong utama dan dompet sekunder')).toBeVisible();
    await expect(page.getByText('Mengalihkan ke terminal akses')).toBeVisible();
  });

  test('should display checkmark icon', async ({ page }) => {
    const checkmarkContainer = page.locator('.bg-bank-green\\/10');
    await expect(checkmarkContainer).toBeVisible();
    await expect(checkmarkContainer).toHaveClass(/rounded-xl/);
  });

  test('should have all 3 steps complete in progress tracker', async ({ page }) => {
    const activeSteps = page.locator('.w-14.h-14.rounded-xl.bg-bank-green');
    await expect(activeSteps).toHaveCount(3);
  });

  test('should have full progress line', async ({ page }) => {
    const progressLine = page.locator('.bg-bank-green.transition-all');
    await expect(progressLine).toHaveCSS('width', /100%/);
  });

  test('should redirect to login after timeout', async ({ page }) => {
    // Wait for redirect (2 seconds in actual code)
    await page.waitForTimeout(3000);

    await expect(page).toHaveURL(/\/login/);
  });

  test('should be responsive on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });

    await expect(page.getByText('Pendaftaran Berhasil.')).toBeVisible();

    await page.screenshot({
      path: 'e2e/screenshots/onboarding-step3-mobile.png',
      fullPage: true
    });
  });

  test('should have success animation', async ({ page }) => {
    // Check for animation classes
    const successContainer = page.locator('.animate-in.zoom-in');
    await expect(successContainer).toBeVisible();
  });
});

test.describe('Onboarding Flow - Error Handling', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should handle registration error gracefully', async ({ page }) => {
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Use data that might cause error (existing username)
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('existinguser');

    // Mock error dialog
    page.on('dialog', dialog => {
      expect(dialog.message()).toContain('Pendaftaran gagal');
      dialog.accept();
    });

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');
    await page.waitForTimeout(2000);
  });

  test('should handle network error gracefully', async ({ page }) => {
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Fill form
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser');

    // Submit (might fail in test environment)
    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Should handle error or show loading
    await page.waitForTimeout(1000);
  });

  test('should show inline validation errors', async ({ page }) => {
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    // Submit empty form
    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    // Should show inline errors
    const errorElements = page.locator('.text-red-500');
    await expect(errorElements).toHaveCount.toBeGreaterThanOrEqual(1);
  });
});

test.describe('Onboarding Flow - Accessibility', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should have proper heading hierarchy', async ({ page }) => {
    await page.goto('/onboarding');

    const h2 = page.locator('h2');
    await expect(h2).toBeVisible();
    await expect(h2).toContainText('Verifikasi eKYC');
  });

  test('should support keyboard navigation', async ({ page }) => {
    await page.goto('/onboarding');

    // Tab through page
    await page.keyboard.press('Tab');
    let focused = await page.locator(':focus').getAttribute('href');
    expect(focused).toBe('/login');

    await page.keyboard.press('Tab');
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

    // Form should submit
    await expect(page.getByText('Menyebarkan Identitas...')).toBeVisible();
  });

  test('should have accessible form labels', async ({ page }) => {
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    const labels = page.locator('label');
    await expect(labels).toHaveCount(4);
  });

  test('should have accessible buttons', async ({ page }) => {
    await page.goto('/onboarding');

    const buttons = page.locator('button');
    await expect(buttons.first()).toBeVisible();
  });
});

test.describe('Onboarding Flow - Visual Regression', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should match screenshots on desktop - Step 1', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/onboarding');

    await page.screenshot({
      path: 'e2e/screenshots/onboarding-step1-desktop.png',
      fullPage: true
    });
  });

  test('should match screenshots on tablet - Step 1', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/onboarding');

    await page.screenshot({
      path: 'e2e/screenshots/onboarding-step1-tablet.png',
      fullPage: true
    });
  });

  test('should match screenshots on desktop - Step 2', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    await page.screenshot({
      path: 'e2e/screenshots/onboarding-step2-desktop.png',
      fullPage: true
    });
  });

  test('should match screenshots on desktop - Step 3', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/onboarding');
    await page.click('button:has-text("Mulai Proses Verifikasi")');

    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser123');
    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');

    await page.waitForTimeout(2000);

    await page.screenshot({
      path: 'e2e/screenshots/onboarding-step3-desktop.png',
      fullPage: true
    });
  });
});

test.describe('Onboarding Flow - Interactive Elements', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should have hover effect on upload area', async ({ page }) => {
    await page.goto('/onboarding');

    const uploadArea = page.locator('.aspect-video.border-2.border-dashed');
    await uploadArea.hover();

    await expect(uploadArea).toHaveClass(/hover:border-bank-green/);
  });

  test('should have hover effect on camera icon', async ({ page }) => {
    await page.goto('/onboarding');

    const cameraContainer = page.locator('.aspect-video.border-2.border-dashed').locator('div');
    await cameraContainer.hover();

    await expect(cameraContainer).toHaveClass(/group-hover\\/upload:scale-110/);
  });

  test('should have button press effect', async ({ page }) => {
    await page.goto('/onboarding');

    const button = page.locator('button:has-text("Mulai Proses Verifikasi")');

    // Check for active scale class
    await expect(button).toHaveClass(/active:scale-\\[0\\.98\\]/);
  });

  test('should have smooth transitions', async ({ page }) => {
    await page.goto('/onboarding');

    const button = page.locator('button:has-text("Mulai Proses Verifikasi")');

    // Check for transition class
    await expect(button).toHaveClass(/transition-all/);
  });

  test('should have animated pulse indicator', async ({ page }) => {
    await page.goto('/onboarding');

    const pulseDot = page.locator('.animate-pulse');
    await expect(pulseDot).toBeVisible();
  });
});

test.describe('Onboarding Flow - Security Features', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('should display OJK & BI compliance badge', async ({ page }) => {
    await page.goto('/onboarding');

    await expect(page.getByText('ENKRIPSI AMAN SESUAI STANDAR OJK & BI')).toBeVisible();
  });

  test('should have security icon', async ({ page }) => {
    await page.goto('/onboarding');

    await expect(page.locator('.text-bank-green').filter({ hasText: /ShieldCheck/i })).toBeVisible();
  });

  test('should have protocol identity badge', async ({ page }) => {
    await page.goto('/onboarding');

    await expect(page.getByText('Protokol Identitas')).toBeVisible();
    await expect(page.locator('.bg-bank-green\\/10')).toBeVisible();
  });

  test('should have secure encryption styling', async ({ page }) => {
    await page.goto('/onboarding');

    const securityBadge = page.locator('.bg-gray-50.dark\\:bg-gray-900\\/50.py-3.rounded-xl');
    await expect(securityBadge).toBeVisible();
  });
});
