import { test, expect } from '@playwright/test';

test.describe('Bill Pay Flow', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/bills');
  });

  test('should display bill payment page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Tagihan & Top-up')).toBeVisible();
    await expect(page.getByText('Bayar tagihan utilitas dan top up dompet digital Anda secara instan')).toBeVisible();
  });

  test('should display all biller categories', async ({ page }) => {
    await expect(page.getByText('Pulsa')).toBeVisible();
    await expect(page.getByText('Listrik (PLN)')).toBeVisible();
    await expect(page.getByText('Air (PDAM)')).toBeVisible();
    await expect(page.getByText('Internet/TV')).toBeVisible();
    await expect(page.getByText('Saldo Kartu')).toBeVisible();
    await expect(page.getByText('BPJS')).toBeVisible();
    await expect(page.getByText('TV Kabel')).toBeVisible();
    await expect(page.getByText('Game Voucher')).toBeVisible();
  });

  test('should show real-time processing badge', async ({ page }) => {
    await expect(page.getByText('Penyelesaian Real-time 24/7')).toBeVisible();
  });

  test('should navigate to biller payment page', async ({ page }) => {
    await page.click('text=Listrik (PLN)');
    
    await expect(page.getByText('Bayar Listrik (PLN)')).toBeVisible();
    await expect(page.getByText('Penyedia Layanan')).toBeVisible();
  });

  test('should display biller specific fields', async ({ page }) => {
    await page.click('text=Pulsa');
    
    await expect(page.getByLabel('ID Pelanggan / Nomor Rekening')).toBeVisible();
    await expect(page.getByText('Jumlah Pembayaran (IDR)')).toBeVisible();
  });

  test('should validate required fields for payment', async ({ page }) => {
    await page.click('text=Listrik (PLN)');
    await page.click('button:has-text("Konfirmasi & Bayar Sekarang")');
    
    await expect(page.getByText('Silakan isi semua bidang yang diperlukan')).toBeVisible();
  });

  test('should show currency prefix in amount field', async ({ page }) => {
    await page.click('text=Air (PDAM)');
    
    await expect(page.locator('input[type="number"]').first()).toBeVisible();
    await expect(page.getByText('Rp')).toBeVisible();
  });

  test('should allow navigation back from biller page', async ({ page }) => {
    await page.click('text=Listrik (PLN)');
    
    const backButton = page.locator('button').filter({ hasText: '←' }).first();
    await backButton.click();
    
    await expect(page.getByText('Tagihan & Top-up')).toBeVisible();
  });

  test('should display security message', async ({ page }) => {
    await page.click('text=BPJS');
    
    await expect(page.getByText('Transaksi aman terenkripsi oleh Infrastruktur Protokol PayU')).toBeVisible();
  });

  test('should show processing state during payment', async ({ page }) => {
    await page.click('text=Pulsa');
    await page.fill('input[placeholder="Masukkan ID unik Anda"]', '08123456789');
    await page.fill('input[placeholder="0"]', '50000');
    
    const payButton = page.getByText('Konfirmasi & Bayar Sekarang');
    await payButton.click();
    
    await expect(page.getByText('Sedang Memproses Pembayaran...')).toBeVisible();
  });

  test('should display empty state for recent bills', async ({ page }) => {
    await expect(page.getByText('Aktivitas Terakhir')).toBeVisible();
    await expect(page.getByText('Pembayaran tagihan terakhir Anda akan muncul di sini')).toBeVisible();
  });

  test('should have add more option for billers', async ({ page }) => {
    await expect(page.getByText('Lainnya')).toBeVisible();
    await expect(page.getByRole('button', { name: /Lainnya/ })).toBeVisible();
  });

  test('should show partner badge for billers', async ({ page }) => {
    await page.click('text=Listrik (PLN)');
    
    await expect(page.getByText('Mitra Pembayaran Resmi')).toBeVisible();
  });
});
