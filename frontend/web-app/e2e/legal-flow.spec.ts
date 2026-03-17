/**
 * E2E Tests: Legal Pages (/legal/terms, /legal/privacy)
 * Covers BUG-TEST-111 and BUG-TEST-112
 */
import { test, expect } from './fixtures';
import { waitForPageStable } from './utils';

// ============================================================
// BUG-TEST-111: /legal/terms
// ============================================================
test.describe('Terms and Conditions Page', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/legal/terms');
    await waitForPageStable(page);
  });

  test('should display terms page heading', async ({ authPage: page }) => {
    await expect(page.getByText('Syarat dan Ketentuan')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Versi 1.0 - Terakhir diperbarui: Januari 2026')).toBeVisible();
  });

  test('should display all 6 sections', async ({ authPage: page }) => {
    await expect(page.getByText('1. Penerimaan Ketentuan')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('2. Deskripsi Layanan')).toBeVisible();
    await expect(page.getByText('3. Tanggung Jawab Pengguna')).toBeVisible();
    await expect(page.getByText('4. Privasi dan Keamanan')).toBeVisible();
    await expect(page.getByText('5. Batasan Tanggung Jawab')).toBeVisible();
    await expect(page.getByText('6. Perubahan Ketentuan')).toBeVisible();
  });

  test('should display section content', async ({ authPage: page }) => {
    await expect(page.getByText('Dengan mengakses dan menggunakan layanan PayU')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('PayU menyediakan platform perbankan digital')).toBeVisible();
  });

  test('should display contact footer', async ({ authPage: page }) => {
    await expect(page.getByText('support@payu.fajjjar.my.id')).toBeVisible({ timeout: 10000 });
  });
});

// ============================================================
// BUG-TEST-112: /legal/privacy
// ============================================================
test.describe('Privacy Policy Page', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/legal/privacy');
    await waitForPageStable(page);
  });

  test('should display privacy page heading', async ({ authPage: page }) => {
    await expect(page.getByText('Kebijakan Privasi')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Versi 1.0 - Terakhir diperbarui: Januari 2026')).toBeVisible();
  });

  test('should display all 6 sections', async ({ authPage: page }) => {
    await expect(page.getByText('1. Pengumpulan Informasi')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('2. Penggunaan Informasi')).toBeVisible();
    await expect(page.getByText('3. Keamanan Data')).toBeVisible();
    await expect(page.getByText('4. Berbagi Informasi')).toBeVisible();
    await expect(page.getByText('5. Hak Pengguna')).toBeVisible();
    await expect(page.getByText('6. Kepatuhan Regulasi')).toBeVisible();
  });

  test('should display section content', async ({ authPage: page }) => {
    await expect(page.getByText('Kami mengumpulkan informasi yang Anda berikan secara langsung')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('menerapkan standar keamanan industri yang ketat')).toBeVisible();
  });

  test('should display privacy contact footer', async ({ authPage: page }) => {
    await expect(page.getByText('privacy@payu.fajjjar.my.id')).toBeVisible({ timeout: 10000 });
  });
});
