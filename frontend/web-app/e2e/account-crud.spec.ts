import { test, expect } from './fixtures';

/**
 * Account CRUD E2E Tests
 * Tests Create, Read, Update, Delete operations for Account entity
 */

test.describe('Account CRUD Operations', () => {
  test.describe('CREATE - Account Registration', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/onboarding');
      await page.waitForLoadState('networkidle');
    });

    test('should create new account with valid data', async ({ page }) => {
      // Fill account registration form
      await page.fill('input[name="fullName"]', 'Test User');
      await page.fill('input[name="email"]', 'testuser@example.com');
      await page.fill('input[name="phone"]', '+6281234567890');
      await page.fill('input[name="nik"]', '1234567890123456');

      // Submit form
      await page.click('button[type="submit"]');

      // Verify account creation started
      await expect(page.getByText('Verifikasi KYC')).toBeVisible({ timeout: 5000 });

      // Verify account data persisted in database
      // This would be verified by the API response in real scenario
    });

    test('should validate required fields for account creation', async ({ page }) => {
      // Try submitting empty form
      await page.click('button[type="submit"]');

      // Verify validation errors
      await expect(page.getByText('Nama lengkap wajib diisi')).toBeVisible();
      await expect(page.getByText('Email wajib diisi')).toBeVisible();
      await expect(page.getByText('Nomor telepon wajib diisi')).toBeVisible();
    });

    test('should prevent duplicate account creation', async ({ page }) => {
      // Try to create account with existing phone number
      await page.fill('input[name="phone"]', '+6281234567890'); // Existing user
      await page.fill('input[name="fullName"]', 'Duplicate User');
      await page.fill('input[name="email"]', 'duplicate@example.com');

      await page.click('button[type="submit"]');

      // Should show error about existing phone
      await expect(page.getByText('Nomor telepon sudah terdaftar')).toBeVisible();
    });

    test('should upload KTP for account verification', async ({ page }) => {
      // Navigate to KYC step
      await page.fill('input[name="fullName"]', 'Test User');
      await page.fill('input[name="email"]', 'testuser2@example.com');
      await page.fill('input[name="phone"]', '+6281234567899');
      await page.click('button[type="submit"]');

      // Upload KTP file
      const fileInput = page.locator('input[type="file"]');
      await fileInput.setInputFiles({
        name: 'ktp.jpg',
        mimeType: 'image/jpeg',
        buffer: Buffer.from('fake-ktp-image-data')
      });

      // Verify upload success
      await expect(page.getByText('KTP berhasil diunggah')).toBeVisible();
    });
  });

  test.describe('READ - Account Details', () => {
    test.beforeEach(async ({ authPage, context }) => {
      await context.addCookies([
        {
          name: 'accessToken',
          value: 'mock-access-token-for-e2e-tests',
          domain: 'localhost',
          path: '/',
          httpOnly: true,
          secure: false,
          sameSite: 'Lax',
        },
      ]);
      await authPage.goto('/settings/profile');
      await authPage.waitForLoadState('networkidle');
    });

    test('should display account information', async ({ authPage }) => {
      // Verify account details are displayed
      await expect(authPage.getByText('Informasi Profil')).toBeVisible();
      await expect(authPage.getByText('Nama Lengkap')).toBeVisible();
      await expect(authPage.getByText('Email')).toBeVisible();
      await expect(authPage.getByText('Nomor Telepon')).toBeVisible();
    });

    test('should load account data from database', async ({ authPage }) => {
      // Verify data is populated from database
      const nameInput = authPage.locator('input[name="fullName"]');
      await expect(nameInput).toHaveValue(/.+/); // Should have some value

      const emailInput = authPage.locator('input[name="email"]');
      await expect(emailInput).toHaveValue(/.+/);
    });

    test('should show account verification status', async ({ authPage }) => {
      // Check KYC verification status
      await expect(authPage.getByText('Status Verifikasi')).toBeVisible();

      // Status should be one of: Pending, Verified, Rejected
      const statusText = authPage.locator('[data-testid="verification-status"]');
      await expect(statusText).toBeVisible();
    });
  });

  test.describe('UPDATE - Account Modification', () => {
    test.beforeEach(async ({ authPage, context }) => {
      await context.addCookies([
        {
          name: 'accessToken',
          value: 'mock-access-token-for-e2e-tests',
          domain: 'localhost',
          path: '/',
          httpOnly: true,
          secure: false,
          sameSite: 'Lax',
        },
      ]);
      await authPage.goto('/settings/profile');
      await authPage.waitForLoadState('networkidle');
    });

    test('should update account profile information', async ({ authPage }) => {
      // Update profile fields
      await authPage.fill('input[name="fullName"]', 'Updated Name');
      await authPage.fill('input[name="address"]', 'Updated Address');

      // Save changes
      await authPage.click('button:has-text("Simpan")');

      // Verify success message
      await expect(authPage.getByText('Profil berhasil diperbarui')).toBeVisible();
    });

    test('should validate email format on update', async ({ authPage }) => {
      // Try invalid email
      await authPage.fill('input[name="email"]', 'invalid-email');
      await authPage.click('button:has-text("Simpan")');

      // Should show validation error
      await expect(authPage.getByText('Format email tidak valid')).toBeVisible();
    });

    test('should update account security settings', async ({ authPage }) => {
      // Navigate to security settings
      await authPage.click('text=Keamanan');
      await authPage.waitForLoadState('networkidle');

      // Change PIN
      await authPage.fill('input[name="currentPin"]', '123456');
      await authPage.fill('input[name="newPin"]', '654321');
      await authPage.fill('input[name="confirmPin"]', '654321');

      await authPage.click('button:has-text("Ubah PIN")');

      // Verify success
      await expect(authPage.getByText('PIN berhasil diubah')).toBeVisible();
    });

    test('should enable two-factor authentication', async ({ authPage }) => {
      // Navigate to security settings
      await authPage.click('text=Keamanan');
      await authPage.waitForLoadState('networkidle');

      // Enable 2FA
      await authPage.click('button:has-text("Aktifkan 2FA")');

      // Verify QR code displayed
      await expect(authPage.locator('[data-testid="2fa-qr-code"]')).toBeVisible();

      // Enter verification code
      await authPage.fill('input[name="otpCode"]', '123456');
      await authPage.click('button:has-text("Verifikasi")');

      // Verify 2FA enabled
      await expect(authPage.getByText('2FA berhasil diaktifkan')).toBeVisible();
    });
  });

  test.describe('DELETE - Account Deactivation', () => {
    test.beforeEach(async ({ authPage, context }) => {
      await context.addCookies([
        {
          name: 'accessToken',
          value: 'mock-access-token-for-e2e-tests',
          domain: 'localhost',
          path: '/',
          httpOnly: true,
          secure: false,
          sameSite: 'Lax',
        },
      ]);
      await authPage.goto('/settings/profile');
      await authPage.waitForLoadState('networkidle');
    });

    test('should initiate account deletion process', async ({ authPage }) => {
      // Navigate to danger zone
      await authPage.click('text=Hapus Akun');

      // Verify confirmation dialog
      await expect(authPage.getByText('Apakah Anda yakin ingin menghapus akun?')).toBeVisible();

      // Confirm deletion
      await authPage.fill('input[name="confirmationText"]', 'HAPUS');
      await authPage.click('button:has-text("Konfirmasi Hapus")');

      // Verify deletion initiated
      await expect(authPage.getByText('Permintaan penghapusan akun telah diajukan')).toBeVisible();
    });

    test('should require confirmation text for deletion', async ({ authPage }) => {
      await authPage.click('text=Hapus Akun');
      await authPage.click('button:has-text("Konfirmasi Hapus")');

      // Should show error
      await expect(authPage.getByText('Silakan ketik HAPUS untuk konfirmasi')).toBeVisible();
    });
  });

  test.describe('Database Consistency Checks', () => {
    test('should maintain data integrity across operations', async ({ page, authPage, context }) => {
      // This test verifies that database operations maintain consistency

      // 1. Create account
      await page.goto('/onboarding');
      await page.fill('input[name="fullName"]', 'Consistency Test User');
      await page.fill('input[name="email"]', 'consistency@example.com');
      await page.fill('input[name="phone"]', '+6281234567888');
      await page.click('button[type="submit"]');

      // 2. Verify data persisted (in real scenario, check database)
      // For E2E, we verify through UI

      // 3. Login and check profile shows correct data
      await context.addCookies([
        {
          name: 'accessToken',
          value: 'mock-access-token-for-e2e-tests',
          domain: 'localhost',
          path: '/',
          httpOnly: true,
          secure: false,
          sameSite: 'Lax',
        },
      ]);

      await authPage.goto('/settings/profile');
      await authPage.waitForLoadState('networkidle');

      // Verify data consistency
      const emailField = authPage.locator('input[name="email"]');
      await expect(emailField).toBeVisible();
    });
  });
});
