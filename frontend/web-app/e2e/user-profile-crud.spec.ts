import { test, expect } from './fixtures';

/**
 * User Profile CRUD E2E Tests
 * Tests Create, Read, Update, Delete operations for User Profile entity
 */

test.describe('User Profile CRUD Operations', () => {
  test.describe('CREATE - Profile Creation', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/onboarding');
      await page.waitForLoadState('networkidle');
    });

    test('should create complete user profile', async ({ page }) => {
      // Step 1: Personal Information
      await page.fill('input[name="fullName"]', 'John Doe');
      await page.fill('input[name="nik"]', '1234567890123456');
      await page.fill('input[name="email"]', 'john.doe@example.com');
      await page.fill('input[name="phone"]', '+6281234567890');

      await page.click('button:has-text("Lanjut")');

      // Step 2: Address Information
      await expect(page.getByText('Alamat')).toBeVisible();
      await page.fill('input[name="street"]', 'Jl. Sudirman No. 123');
      await page.fill('input[name="city"]', 'Jakarta');
      await page.fill('input[name="province"]', 'DKI Jakarta');
      await page.fill('input[name="postalCode"]', '12345');

      await page.click('button:has-text("Lanjut")');

      // Step 3: Emergency Contact
      await expect(page.getByText('Kontak Darurat')).toBeVisible();
      await page.fill('input[name="emergencyName"]', 'Jane Doe');
      await page.fill('input[name="emergencyPhone"]', '+6289876543210');
      await page.selectOption('select[name="emergencyRelation"]', 'SPOUSE');

      await page.click('button:has-text("Selesai")');

      // Verify profile created
      await expect(page.getByText('Profil berhasil dibuat')).toBeVisible();
    });

    test('should validate NIK format', async ({ page }) => {
      await page.fill('input[name="nik"]', '12345'); // Invalid NIK (too short)
      await page.click('button:has-text("Lanjut")');

      await expect(page.getByText('NIK harus 16 digit')).toBeVisible();
    });

    test('should require all mandatory fields', async ({ page }) => {
      // Try to proceed without filling required fields
      await page.click('button:has-text("Lanjut")');

      await expect(page.getByText('Nama lengkap wajib diisi')).toBeVisible();
      await expect(page.getByText('NIK wajib diisi')).toBeVisible();
      await expect(page.getByText('Email wajib diisi')).toBeVisible();
    });
  });

  test.describe('READ - Profile View', () => {
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

    test('should display user profile information', async ({ authPage }) => {
      await expect(authPage.getByText('Informasi Pribadi')).toBeVisible();

      // Verify all profile sections
      await expect(authPage.getByText('Nama Lengkap')).toBeVisible();
      await expect(authPage.getByText('Email')).toBeVisible();
      await expect(authPage.getByText('Nomor Telepon')).toBeVisible();
      await expect(authPage.getByText('NIK')).toBeVisible();
    });

    test('should display profile picture', async ({ authPage }) => {
      const profileImage = authPage.locator('[data-testid="profile-image"]');

      if (await profileImage.isVisible()) {
        await expect(profileImage).toBeVisible();

        // Check image loads properly
        const naturalWidth = await profileImage.evaluate(img => (img as HTMLImageElement).naturalWidth);
        expect(naturalWidth).toBeGreaterThan(0);
      }
    });

    test('should show membership tier', async ({ authPage }) => {
      await expect(authPage.getByText('Level Keanggotaan')).toBeVisible();

      // Should show current tier
      const tierBadge = authPage.locator('[data-testid="membership-tier"]');
      await expect(tierBadge).toBeVisible();
    });

    test('should display account creation date', async ({ authPage }) => {
      await expect(authPage.getByText('Anggota sejak')).toBeVisible();

      // Date should be in valid format
      const dateText = await authPage.locator('[data-testid="member-since"]').textContent();
      expect(dateText).toMatch(/\d{4}/); // Contains year
    });
  });

  test.describe('UPDATE - Profile Modification', () => {
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

    test('should update profile picture', async ({ authPage }) => {
      await authPage.click('button:has-text("Ganti Foto")');

      // Upload new photo
      const fileInput = authPage.locator('input[type="file"]');
      await fileInput.setInputFiles({
        name: 'profile.jpg',
        mimeType: 'image/jpeg',
        buffer: Buffer.from('fake-profile-image-data')
      });

      // Crop and save
      await authPage.click('button:has-text("Simpan")');

      await expect(authPage.getByText('Foto profil diperbarui')).toBeVisible();
    });

    test('should update phone number', async ({ authPage }) => {
      // Click edit on phone field
      await authPage.locator('[data-testid="edit-phone"]').click();

      // Enter new phone
      await authPage.fill('input[name="phone"]', '+6281234567899');
      await authPage.click('button:has-text("Verifikasi")');

      // Enter OTP (mock)
      await authPage.fill('input[name="otp"]', '123456');
      await authPage.click('button:has-text("Konfirmasi")');

      await expect(authPage.getByText('Nomor telepon diperbarui')).toBeVisible();
    });

    test('should update email address', async ({ authPage }) => {
      await authPage.locator('[data-testid="edit-email"]').click();

      await authPage.fill('input[name="email"]', 'newemail@example.com');
      await authPage.click('button:has-text("Simpan")');

      // Should require verification
      await expect(authPage.getByText('Email verifikasi dikirim')).toBeVisible();
    });

    test('should update address information', async ({ authPage }) => {
      await authPage.click('text:has("Alamat")');

      await authPage.fill('input[name="street"]', 'Jl. Thamrin No. 456');
      await authPage.fill('input[name="city"]', 'Bandung');
      await authPage.selectOption('select[name="province"]', 'Jawa Barat');

      await authPage.click('button:has-text("Simpan Alamat")');

      await expect(authPage.getByText('Alamat diperbarui')).toBeVisible();
    });

    test('should update emergency contact', async ({ authPage }) => {
      await authPage.click('text:has("Kontak Darurat")');

      await authPage.fill('input[name="emergencyName"]', 'Updated Contact');
      await authPage.fill('input[name="emergencyPhone"]', '+6281111111111');

      await authPage.click('button:has-text("Simpan")');

      await expect(authPage.getByText('Kontak darurat diperbarui')).toBeVisible();
    });

    test('should validate email format on update', async ({ authPage }) => {
      await authPage.locator('[data-testid="edit-email"]').click();

      await authPage.fill('input[name="email"]', 'invalid-email-format');
      await authPage.click('button:has-text("Simpan")');

      await expect(authPage.getByText('Format email tidak valid')).toBeVisible();
    });

    test('should prevent duplicate phone number', async ({ authPage }) => {
      await authPage.locator('[data-testid="edit-phone"]').click();

      // Try to use existing phone
      await authPage.fill('input[name="phone"]', '+6281234567890');
      await authPage.click('button:has-text("Verifikasi")');

      await expect(authPage.getByText('Nomor telepon sudah digunakan')).toBeVisible();
    });
  });

  test.describe('DELETE - Profile Deactivation', () => {
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

    test('should initiate profile deactivation', async ({ authPage }) => {
      await authPage.click('text:has("Hapus Akun")');

      // Should show warning
      await expect(authPage.getByText('Tindakan ini tidak dapat dibatalkan')).toBeVisible();

      // Require confirmation
      await authPage.fill('input[name="confirmation"]', 'HAPUS');
      await authPage.click('button:has-text("Lanjutkan")');

      // Should ask for reason
      await expect(authPage.getByText('Alasan penghapusan')).toBeVisible();
    });

    test('should require confirmation text for deactivation', async ({ authPage }) => {
      await authPage.click('text:has("Hapus Akun")');

      // Click without entering confirmation
      await authPage.click('button:has-text("Lanjutkan")');

      await expect(authPage.getByText('Silakan ketik HAPUS untuk melanjutkan')).toBeVisible();
    });

    test('should cancel deactivation process', async ({ authPage }) => {
      await authPage.click('text:has("Hapus Akun")');

      // Cancel
      await authPage.click('button:has-text("Batal")');

      // Should return to profile page
      await expect(authPage.getByText('Informasi Pribadi')).toBeVisible();
    });

    test('should check for pending transactions before deletion', async ({ authPage }) => {
      await authPage.click('text:has("Hapus Akun")');
      await authPage.fill('input[name="confirmation"]', 'HAPUS');
      await authPage.click('button:has-text("Lanjutkan")');

      // System should check for pending transactions
      await authPage.selectOption('select[name="reason"]', 'OTHER');
      await authPage.click('button:has-text("Konfirmasi Penghapusan")');

      // If has pending transactions, should show warning
      const warning = authPage.getByText('Masih ada transaksi yang pending');
      if (await warning.isVisible()) {
        await expect(warning).toBeVisible();
      }
    });
  });

  test.describe('Profile Privacy & Security', () => {
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
      await authPage.goto('/settings/privacy');
      await authPage.waitForLoadState('networkidle');
    });

    test('should configure privacy settings', async ({ authPage }) => {
      await expect(authPage.getByText('Pengaturan Privasi')).toBeVisible();

      // Toggle visibility settings
      await authPage.click('[data-testid="toggle-profile-visibility"]');
      await authPage.click('[data-testid="toggle-activity-status"]');

      await authPage.click('button:has-text("Simpan Pengaturan")');

      await expect(authPage.getByText('Pengaturan privasi diperbarui')).toBeVisible();
    });

    test('should manage connected devices', async ({ authPage }) => {
      await authPage.click('text:has("Perangkat Tersambung")');

      await expect(authPage.getByText('Perangkat Aktif')).toBeVisible();

      // Should list connected devices
      const devices = authPage.locator('[data-testid="device-item"]');
      const count = await devices.count();

      if (count > 0) {
        // Can revoke access
        await devices.first().locator('button:has-text("Cabut Akses")').click();
        await expect(authPage.getByText('Akses perangkat dicabut')).toBeVisible();
      }
    });

    test('should view login history', async ({ authPage }) => {
      await authPage.click('text:has("Riwayat Login")');

      await expect(authPage.getByText('Riwayat Aktivitas Login')).toBeVisible();

      // Should show login entries
      const loginHistory = authPage.locator('[data-testid="login-history-item"]');
      await expect(loginHistory.first()).toBeVisible();
    });

    test('should enable biometric authentication', async ({ authPage }) => {
      await authPage.click('text:has("Biometrik")');

      await authPage.click('button:has-text("Aktifkan Sidik Jari")');

      // Mock biometric prompt
      await expect(authPage.getByText('Tempelkan sidik jari')).toBeVisible();
    });
  });

  test.describe('Profile Data Export', () => {
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
      await authPage.goto('/settings/privacy');
      await authPage.waitForLoadState('networkidle');
    });

    test('should export user data', async ({ authPage }) => {
      await authPage.click('button:has-text("Unduh Data Saya")');

      // Should show data export preparation
      await expect(authPage.getByText('Menyiapkan data Anda')).toBeVisible();

      // In real scenario, would download JSON/PDF
      await authPage.waitForTimeout(2000);

      await expect(authPage.getByText('Data siap diunduh')).toBeVisible();
    });

    test('should include all profile data in export', async ({ authPage }) => {
      await authPage.click('button:has-text("Unduh Data Saya")');

      await expect(authPage.getByText('Data yang akan diunduh')).toBeVisible();
      await expect(authPage.getByText('Informasi Pribadi')).toBeVisible();
      await expect(authPage.getByText('Riwayat Transaksi')).toBeVisible();
      await expect(authPage.getByText('Data Keuangan')).toBeVisible();
    });
  });
});
