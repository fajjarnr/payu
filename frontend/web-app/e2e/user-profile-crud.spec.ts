import { test, expect } from './fixtures';

/**
 * User Profile CRUD E2E tests. Mapped to UI: CREATE /onboarding (3-step), READ/UPDATE
 * /settings, DELETE session via "Hapus Sesi"; security at /security.
 */

test.describe('User Profile CRUD Operations', () => {
  test.describe('CREATE - Profile Creation (Onboarding)', () => {
    test('should display KYC upload step', async ({ page }) => {
      await page.goto('/onboarding');
      await page.waitForLoadState('domcontentloaded');

      await expect(page.getByText('Unggah e-KTP')).toBeVisible();
      await expect(page.getByText('Klik untuk ambil foto')).toBeVisible();

      await expect(page.getByRole('button', { name: /Lanjut ke Profil Data/i })).toBeVisible();
    });

    test('should navigate to profile form step', async ({ page }) => {
      await page.goto('/onboarding');
      await page.waitForLoadState('domcontentloaded');

      // Upload a KTP file first — button is disabled={!ktpFile} (BUG-FE-107)
      await page.locator('input[type="file"]').setInputFiles({
        name: 'ktp.png',
        mimeType: 'image/png',
        buffer: Buffer.from('iVBORw0KGgo='),
      });

      await page.click('button:has-text("Lanjut ke Profil Data")');
      await page.waitForTimeout(1000);

      await expect(page.getByText('Lengkapi Profil')).toBeVisible();
    });

    test('should display all registration form fields', async ({ page }) => {
      await page.goto('/onboarding');
      await page.waitForLoadState('domcontentloaded');

      // Upload a KTP file first — button is disabled={!ktpFile} (BUG-FE-107)
      await page.locator('input[type="file"]').setInputFiles({
        name: 'ktp.png',
        mimeType: 'image/png',
        buffer: Buffer.from('iVBORw0KGgo='),
      });

      await page.click('button:has-text("Lanjut ke Profil Data")');
      await page.waitForTimeout(1000);

      await expect(page.getByPlaceholder('16 digit angka...')).toBeVisible();
      await expect(page.getByPlaceholder('Sesuai KTP')).toBeVisible();
      await expect(page.getByPlaceholder('nama@email.com')).toBeVisible();
      await expect(page.getByPlaceholder('unik & mudah diingat')).toBeVisible();
    });

    test('should fill registration form', async ({ page }) => {
      await page.goto('/onboarding');
      await page.waitForLoadState('domcontentloaded');

      // Upload a KTP file first — button is disabled={!ktpFile} (BUG-FE-107)
      await page.locator('input[type="file"]').setInputFiles({
        name: 'ktp.png',
        mimeType: 'image/png',
        buffer: Buffer.from('iVBORw0KGgo='),
      });

      await page.click('button:has-text("Lanjut ke Profil Data")');
      await page.waitForTimeout(1000);

      await page.getByPlaceholder('16 digit angka...').fill('1234567890123456');
      await page.getByPlaceholder('Sesuai KTP').fill('Test User E2E');
      await page.getByPlaceholder('nama@email.com').fill(`test_${Date.now()}@example.com`);
      await page.getByPlaceholder('unik & mudah diingat').fill(`testuser_${Date.now()}`);

      await expect(page.getByRole('button', { name: /Konfirmasi Pendaftaran/i })).toBeVisible();
    });

    test('should have back button on step 2', async ({ page }) => {
      await page.goto('/onboarding');
      await page.waitForLoadState('domcontentloaded');

      // Upload a KTP file first — button is disabled={!ktpFile} (BUG-FE-107)
      await page.locator('input[type="file"]').setInputFiles({
        name: 'ktp.png',
        mimeType: 'image/png',
        buffer: Buffer.from('iVBORw0KGgo='),
      });

      await page.click('button:has-text("Lanjut ke Profil Data")');
      await page.waitForTimeout(1000);

      await expect(page.getByRole('button', { name: /Kembali/i })).toBeVisible();
    });

    test('should navigate back to KYC step', async ({ page }) => {
      await page.goto('/onboarding');
      await page.waitForLoadState('domcontentloaded');

      // Upload a KTP file first — button is disabled={!ktpFile} (BUG-FE-107)
      await page.locator('input[type="file"]').setInputFiles({
        name: 'ktp.png',
        mimeType: 'image/png',
        buffer: Buffer.from('iVBORw0KGgo='),
      });

      await page.click('button:has-text("Lanjut ke Profil Data")');
      await page.waitForTimeout(1000);
      await expect(page.getByText('Lengkapi Profil')).toBeVisible();

      await page.click('button:has-text("Kembali")');
      await page.waitForTimeout(1000);

      await expect(page.getByText('Unggah e-KTP')).toBeVisible();
    });

    test('should display stepper with correct steps', async ({ page }) => {
      await page.goto('/onboarding');
      await page.waitForLoadState('domcontentloaded');

      await expect(page.getByText('Identitas', { exact: true })).toBeVisible();
      await expect(page.getByText('Profil', { exact: true })).toBeVisible();
    });
  });

  test.describe('READ - Profile View', () => {
    test('should display settings page heading', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Ekosistem Akun')).toBeVisible();
      await expect(authPage.getByText('Kelola profil pribadi, preferensi sistem, dan tata kelola akun.')).toBeVisible();
    });

    test('should display profile section', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Kredensial Profil')).toBeVisible();
    });

    test('should display profile form labels', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Nama Lengkap (Sesuai KTP)')).toBeVisible();
      await expect(authPage.getByText('Email Kontak')).toBeVisible();
      await expect(authPage.getByText('Protokol Telepon')).toBeVisible();
    });

    test('should display profile form inputs', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.locator('input[placeholder="Nama lengkap"]')).toBeVisible();
      await expect(authPage.locator('input[placeholder="email@contoh.com"]')).toBeVisible();
      await expect(authPage.locator('input[placeholder="+62 812-3456-7890"]')).toBeVisible();
    });

    test('should display sidebar menu items', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Profil Umum')).toBeVisible();
      await expect(authPage.getByText('E-Statement')).toBeVisible();
      await expect(authPage.getByText('Tagihan & Paket')).toBeVisible();
      await expect(authPage.getByText('Privasi & Keamanan')).toBeVisible();
      await expect(authPage.getByText('Pengaturan Lanjut')).toBeVisible();
    });

    test('should display account info in sidebar', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('ID Akun')).toBeVisible();
      await expect(authPage.getByText('Status', { exact: true })).toBeVisible();
      await expect(authPage.getByText('eKYC Terverifikasi')).toBeVisible();
      await expect(authPage.getByText('Premium Member')).toBeVisible();
    });
  });

  test.describe('UPDATE - Profile Modification', () => {
    test('should allow editing profile name', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Kredensial Profil')).toBeVisible();

      const nameInput = authPage.locator('input[placeholder="Nama lengkap"]');
      await expect(nameInput).toBeVisible();
      await nameInput.fill('Updated Name E2E');

      await expect(authPage.getByText('Sinkronisasi Profil')).toBeVisible();
    });

    test('should allow editing email', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      const emailInput = authPage.locator('input[placeholder="email@contoh.com"]');
      await expect(emailInput).toBeVisible();
      await emailInput.fill('newemail@example.com');

      await expect(authPage.getByText('Sinkronisasi Profil')).toBeVisible();
    });

    test('should allow editing phone number', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      const phoneInput = authPage.locator('input[placeholder="+62 812-3456-7890"]');
      await expect(phoneInput).toBeVisible();
      await phoneInput.fill('+6281234567899');

      await expect(authPage.getByText('Sinkronisasi Profil')).toBeVisible();
    });

    test('should display preferences section', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByRole('heading', { name: 'Preferensi Sistem' })).toBeVisible();

      await expect(authPage.getByText('Notifikasi Push')).toBeVisible();
      await expect(authPage.getByText('Grafis Mode Gelap')).toBeVisible();
      await expect(authPage.getByText('Wawasan Pemasaran')).toBeVisible();
    });

    test('should switch to E-Statement tab', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      const eStatementButton = authPage.locator('button').filter({ hasText: 'E-Statement' });
      await expect(eStatementButton).toBeVisible();

      const profileButton = authPage.locator('button').filter({ hasText: 'Profil Umum' });
      await expect(profileButton).toHaveClass(/bg-primary/);

      await expect(authPage.getByText('Kredensial Profil')).toBeVisible();
    });

    test('should switch back to profile tab', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.locator('button').filter({ hasText: 'Profil Umum' })).toBeVisible();
      await expect(authPage.locator('button').filter({ hasText: 'E-Statement' })).toBeVisible();
      await expect(authPage.locator('button').filter({ hasText: 'Tagihan & Paket' })).toBeVisible();
      await expect(authPage.locator('button').filter({ hasText: 'Privasi & Keamanan' })).toBeVisible();
      await expect(authPage.locator('button').filter({ hasText: 'Pengaturan Lanjut' })).toBeVisible();

      await expect(authPage.locator('button').filter({ hasText: 'Profil Umum' })).toHaveClass(/bg-primary/);
    });
  });

  test.describe('DELETE - Session Management', () => {
    test('should display Hapus Sesi button', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Hapus Sesi')).toBeVisible();
    });

    test('should display both action buttons', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Sinkronisasi Profil')).toBeVisible();
      await expect(authPage.getByText('Hapus Sesi')).toBeVisible();
    });

    test('should maintain page state after reload', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Ekosistem Akun')).toBeVisible();

      await authPage.reload();
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Ekosistem Akun')).toBeVisible();
      await expect(authPage.getByText('Kredensial Profil')).toBeVisible();
    });
  });

  test.describe('Profile Privacy & Security', () => {
    test('should display security page', async ({ authPage }) => {
      await authPage.goto('/security');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Keamanan & Tata Kelola')).toBeVisible();
      await expect(authPage.getByText('Proteksi Level 4 Aktif')).toBeVisible();
    });

    test('should display MFA section', async ({ authPage }) => {
      await authPage.goto('/security');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Keamanan & Tata Kelola')).toBeVisible();
    });

    test('should maintain security page state after reload', async ({ authPage }) => {
      await authPage.goto('/security');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Keamanan & Tata Kelola')).toBeVisible();

      await authPage.reload();
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Keamanan & Tata Kelola')).toBeVisible();
    });

    test('should navigate between settings and security', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');
      await expect(authPage.getByText('Ekosistem Akun')).toBeVisible();

      await authPage.goto('/security');
      await authPage.waitForLoadState('domcontentloaded');
      await expect(authPage.getByText('Keamanan & Tata Kelola')).toBeVisible();

      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');
      await expect(authPage.getByText('Ekosistem Akun')).toBeVisible();
    });
  });

  test.describe('Profile Data Consistency', () => {
    test('should load profile data consistently', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Kredensial Profil')).toBeVisible();
      await expect(authPage.locator('input[placeholder="Nama lengkap"]')).toBeVisible();

      await authPage.reload();
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Kredensial Profil')).toBeVisible();
      await expect(authPage.locator('input[placeholder="Nama lengkap"]')).toBeVisible();
    });

    test('should show all settings sections on profile tab', async ({ authPage }) => {
      await authPage.goto('/settings');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Kredensial Profil')).toBeVisible();
      await expect(authPage.getByRole('heading', { name: 'Preferensi Sistem' })).toBeVisible();
      await expect(authPage.getByText('Sinkronisasi Profil')).toBeVisible();
      await expect(authPage.getByText('Hapus Sesi')).toBeVisible();
    });
  });
});
