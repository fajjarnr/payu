import { test, expect } from '@playwright/test';

test('merchant register page loads and displays correctly', async ({ page }) => {
  await page.goto('http://localhost:3000/merchant/register');

  await expect(page.getByRole('heading', { name: 'DAFTAR MERCHANT BARU' })).toBeVisible();
  await expect(page.getByText('Bergabunglah dengan ekosistem pembayaran PayU dan terima pembayaran instan dari jutaan pengguna.')).toBeVisible();
});

test('merchant register form validation', async ({ page }) => {
  await page.goto('http://localhost:3000/merchant/register');

  const submitButton = page.getByRole('button', { name: /DAFTAR SEKARANG/i });
  await submitButton.click();

  await expect(page.getByText('Nama merchant minimal 3 karakter')).toBeVisible();
  await expect(page.getByText('Format email tidak valid')).toBeVisible();
  await expect(page.getByText('Nomor telepon minimal 10 digit')).toBeVisible();
  await expect(page.getByText('Tipe merchant wajib dipilih')).toBeVisible();
});

test('merchant register form submission with valid data', async ({ page }) => {
  await page.goto('http://localhost:3000/merchant/register');

  await page.fill('input[type="text"]', 'Test Merchant');
  await page.fill('input[type="email"]', 'merchant@test.com');
  await page.fill('input[type="tel"]', '+6281234567890');

  const retailType = page.getByText('RETAIL').first();
  await retailType.click();

  const submitButton = page.getByRole('button', { name: /DAFTAR SEKARANG/i });
  await submitButton.click();

  await expect(page.locator('text=Nama merchant minimal 3 karakter')).not.toBeVisible();
  await expect(page.locator('text=Format email tidak valid')).not.toBeVisible();
  await expect(page.locator('text=Nomor telepon minimal 10 digit')).not.toBeVisible();
  await expect(page.locator('text=Tipe merchant wajib dipilih')).not.toBeVisible();
});

test('merchant type selection', async ({ page }) => {
  await page.goto('http://localhost:3000/merchant/register');

  const retailType = page.getByText('Retail').first();
  await retailType.click();

  await expect(page.getByRole('button', { name: 'Retail' })).toBeVisible();

  const foodType = page.getByText('Food & Beverage').first();
  await foodType.click();

  await expect(page.getByRole('button', { name: 'Food & Beverage' })).toBeVisible();
});

test('merchant register public key field', async ({ page }) => {
  await page.goto('http://localhost:3000/merchant/register');

  const publicKeyLabel = page.getByText('PUBLIC KEY (OPSIONAL)');
  await expect(publicKeyLabel).toBeVisible();

  const publicKeyInput = page.getByPlaceholder('-----BEGIN PUBLIC KEY-----');
  await expect(publicKeyInput).toBeVisible();

  await publicKeyInput.fill('-----BEGIN PUBLIC KEY-----\ntest-key\n-----END PUBLIC KEY-----');

  await expect(publicKeyInput).toHaveValue(/test-key/);
});

test('merchant register back to dashboard link', async ({ page }) => {
  await page.goto('http://localhost:3000/merchant/register');

  const backButton = page.getByText('KEMBALI KE DASHBOARD MERCHANT');
  await expect(backButton).toBeVisible();

  await backButton.click();
  await page.waitForURL('**/merchant');

  await expect(page).toHaveURL(/.*\/merchant$/);
});

test('merchant register displays all merchant types', async ({ page }) => {
  await page.goto('http://localhost:3000/merchant/register');

  await expect(page.getByText('Retail')).toBeVisible();
  await expect(page.getByText('Food & Beverage')).toBeVisible();
  await expect(page.getByText('Transportation')).toBeVisible();
  await expect(page.getByText('Marketplace')).toBeVisible();
  await expect(page.getByText('Utility')).toBeVisible();
});

test('merchant register visual elements', async ({ page }) => {
  await page.goto('http://localhost:3000/merchant/register');

  const svgCount = await page.locator('svg').count();
  expect(svgCount).toBeGreaterThan(0);

  await expect(page.getByRole('button', { name: /DAFTAR SEKARANG/i })).toBeVisible();

  await expect(page.getByText('Dengan mendaftar, Anda menyetujui')).toBeVisible();
});
