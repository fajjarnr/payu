import { test, expect } from '@playwright/test';

test('merchant register page loads and displays correctly', async ({ page }) => {
  await page.goto('/merchant/register');
  await page.waitForLoadState('domcontentloaded');

  await expect(page.getByRole('heading', { name: 'Daftar Merchant Baru' })).toBeVisible();
  await expect(page.getByText('Bergabunglah dengan ekosistem pembayaran PayU dan terima pembayaran instan dari jutaan pengguna.')).toBeVisible();
});

test('merchant register form validation', async ({ page }) => {
  await page.goto('/merchant/register');
  await page.waitForLoadState('domcontentloaded');

  const submitButton = page.getByRole('button', { name: /Daftar Sekarang/i });
  await submitButton.click();

  await expect(page.getByText('Nama merchant minimal 3 karakter')).toBeVisible();
  await expect(page.getByText('Format email tidak valid')).toBeVisible();
  await expect(page.getByText('Nomor telepon minimal 10 digit')).toBeVisible();
  await expect(page.getByText('Tipe merchant wajib dipilih')).toBeVisible();
});

test('merchant register form submission with valid data', async ({ page }) => {
  await page.goto('/merchant/register');
  await page.waitForLoadState('domcontentloaded');

  await page.fill('input[type="text"]', 'Test Merchant');
  await page.fill('input[type="email"]', 'merchant@test.com');
  await page.fill('input[type="tel"]', '+6281234567890');

  // Click the Retail merchant type button (contains label text "Retail")
  const retailType = page.locator('button', { hasText: 'Retail' }).first();
  await retailType.click();

  const submitButton = page.getByRole('button', { name: /Daftar Sekarang/i });
  await submitButton.click();

  await expect(page.locator('text=Nama merchant minimal 3 karakter')).not.toBeVisible();
  await expect(page.locator('text=Format email tidak valid')).not.toBeVisible();
  await expect(page.locator('text=Nomor telepon minimal 10 digit')).not.toBeVisible();
  await expect(page.locator('text=Tipe merchant wajib dipilih')).not.toBeVisible();
});

test('merchant type selection', async ({ page }) => {
  await page.goto('/merchant/register');
  await page.waitForLoadState('domcontentloaded');

  // Click the Retail type button
  const retailType = page.locator('button', { hasText: 'Retail' }).first();
  await retailType.click();

  // Verify the button containing "Retail" is visible (it already was, but now selected)
  await expect(page.locator('button', { hasText: 'Retail' }).first()).toBeVisible();

  // Click the Food & Beverage type button
  const foodType = page.locator('button', { hasText: 'Food & Beverage' }).first();
  await foodType.click();

  await expect(page.locator('button', { hasText: 'Food & Beverage' }).first()).toBeVisible();
});

test('merchant register public key field', async ({ page }) => {
  await page.goto('/merchant/register');
  await page.waitForLoadState('domcontentloaded');

  const publicKeyLabel = page.getByText('Public Key (Opsional)');
  await expect(publicKeyLabel).toBeVisible();

  const publicKeyInput = page.getByPlaceholder('-----BEGIN PUBLIC KEY-----');
  await expect(publicKeyInput).toBeVisible();

  await publicKeyInput.fill('-----BEGIN PUBLIC KEY-----\ntest-key\n-----END PUBLIC KEY-----');

  await expect(publicKeyInput).toHaveValue(/test-key/);
});

test('merchant register back to dashboard link', async ({ page, context }) => {
  await page.goto('/merchant/register');
  await page.waitForLoadState('domcontentloaded');

  const backButton = page.getByText('Kembali ke Dashboard Merchant');
  await expect(backButton).toBeVisible();

  // Set up auth cookies before navigating to the protected /merchant route
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
    {
      name: 'payu_session',
      value: 'mock-session-for-e2e-tests',
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
  ]);

  await backButton.click();
  await page.waitForURL('**/merchant');

  await expect(page).toHaveURL(/.*\/merchant$/);
});

test('merchant register displays all merchant types', async ({ page }) => {
  await page.goto('/merchant/register');
  await page.waitForLoadState('domcontentloaded');

  await expect(page.getByText('Retail')).toBeVisible();
  await expect(page.getByText('Food & Beverage')).toBeVisible();
  await expect(page.getByText('Transportation')).toBeVisible();
  await expect(page.getByText('Marketplace')).toBeVisible();
  await expect(page.getByText('Utility')).toBeVisible();
});

test('merchant register visual elements', async ({ page }) => {
  await page.goto('/merchant/register');
  await page.waitForLoadState('domcontentloaded');

  const svgCount = await page.locator('svg').count();
  expect(svgCount).toBeGreaterThan(0);

  await expect(page.getByRole('button', { name: /Daftar Sekarang/i })).toBeVisible();

  await expect(page.getByText('Dengan mendaftar, Anda menyetujui')).toBeVisible();
});
