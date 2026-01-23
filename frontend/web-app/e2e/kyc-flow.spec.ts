import { test, expect } from '@playwright/test';

test.describe('KYC Onboarding Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/onboarding');
  });

  test('should display KYC verification page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Verifikasi eKYC')).toBeVisible();
    await expect(page.getByText('Unggah identitas resmi pemerintah (KTP)')).toBeVisible();
  });

  test('should navigate through KYC steps', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');
    
    await expect(page.getByText('Profil Akun')).toBeVisible();
    await expect(page.getByText('Nomor NIK (16 Digit)')).toBeVisible();
  });

  test('should validate NIK input format', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');
    
    const nikInput = page.getByPlaceholder('3200...');
    await nikInput.fill('123');
    
    const fullNameInput = page.getByPlaceholder('NAMA LENGKAP ANDA');
    await fullNameInput.fill('Test User');

    const emailInput = page.getByPlaceholder('nama@domain.com');
    await emailInput.fill('test@example.com');

    const usernameInput = page.getByPlaceholder('NAMA_PENGGUNA_UNIK');
    await usernameInput.fill('testuser');

    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');
    
    await expect(page.getByText('NIK harus 16 digit')).toBeVisible();
  });

  test('should show success message after valid submission', async ({ page }) => {
    await page.click('button:has-text("Mulai Proses Verifikasi")');
    
    await page.getByPlaceholder('3200...').fill('3201010101010001');
    await page.getByPlaceholder('NAMA LENGKAP ANDA').fill('Test User');
    await page.getByPlaceholder('nama@domain.com').fill('test@example.com');
    await page.getByPlaceholder('NAMA_PENGGUNA_UNIK').fill('testuser123');
    
    await page.click('button:has-text("Konfirmasi Pembuatan Akun")');
    
    await expect(page.getByText('Pendaftaran Berhasil')).toBeVisible();
  });

  test('should have secure encryption badge', async ({ page }) => {
    await expect(page.getByText('ENKRIPSI AMAN SESUAI STANDAR OJK & BI')).toBeVisible();
    await expect(page.locator('.text-bank-green')).toHaveCount(1);
  });

  test('should navigate back to login page', async ({ page }) => {
    const backButton = page.locator('a[href="/login"]');
    await expect(backButton).toBeVisible();
    await backButton.click();
    await expect(page).toHaveURL(/\/login/);
  });
});
