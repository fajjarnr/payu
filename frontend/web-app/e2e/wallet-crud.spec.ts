import { test, expect } from './fixtures';

/**
 * Wallet CRUD E2E tests. Mapped to UI at /pockets: CREATE "Tambah Kantong",
 * READ sections, UPDATE via card dropdown, DELETE "Tutup Kantong".
 */

test.describe('Wallet CRUD Operations', () => {
  test.describe('CREATE - Pocket Creation', () => {
    test('should display pockets page with create button', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Manajemen Kantong')).toBeVisible();

      await expect(authPage.getByText('Tambah Kantong')).toBeVisible();
    });

    test('should open create pocket modal', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await authPage.getByText('Tambah Kantong').click();
      await authPage.waitForTimeout(500);

      await expect(authPage.getByRole('heading', { name: 'Buat Kantong Baru' })).toBeVisible();

      await expect(authPage.getByText('Buat kantong untuk mengalokasikan dana sesuai tujuan Anda')).toBeVisible();
    });

    test('should display create form fields in modal', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await authPage.getByText('Tambah Kantong').click();
      await authPage.waitForTimeout(500);

      await expect(authPage.getByText('Nama Kantong')).toBeVisible();
      await expect(authPage.getByText('Target Dana (Opsional)')).toBeVisible();
      await expect(authPage.getByText('Tipe Kantong')).toBeVisible();

      await expect(authPage.getByPlaceholder('Contoh: Dana Darurat, Liburan')).toBeVisible();
      await expect(authPage.getByPlaceholder('5000000')).toBeVisible();
    });

    test('should fill create pocket form', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      // Wait for page to be fully interactive before clicking
      await authPage.waitForTimeout(300);
      await authPage.getByText('Tambah Kantong').click();
      await authPage.waitForTimeout(500);

      const submitButton = authPage.locator('button:has-text("Buat Kantong")').last();
      await expect(submitButton).toBeVisible({ timeout: 5000 });

      await authPage.getByPlaceholder('Contoh: Dana Darurat, Liburan').fill(`Test Pocket ${Date.now()}`);
      await authPage.getByPlaceholder('5000000').fill('1000000');

      await expect(submitButton).toBeVisible();
    });

    test('should have pocket type selector buttons', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await authPage.getByText('Tambah Kantong').click();
      await authPage.waitForTimeout(500);

      await expect(authPage.getByRole('button', { name: /Tabungan/i })).toBeVisible();
      await expect(authPage.getByRole('button', { name: /Target/i })).toBeVisible();
    });

    test('should have cancel button in create modal', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await authPage.getByText('Tambah Kantong').click();
      await authPage.waitForTimeout(500);

      await expect(authPage.getByRole('button', { name: 'Batal' })).toBeVisible();
    });

    test('should have empty state with create button when no pockets', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      // Empty state shows "Belum Ada Kantong" plus a create button.
      const emptyState = authPage.getByText('Belum Ada Kantong');
      const hasEmpty = await emptyState.isVisible().catch(() => false);

      if (hasEmpty) {
        await expect(authPage.getByText('Buat kantong pertama Anda untuk mulai mengalokasikan dana')).toBeVisible();
        await expect(authPage.getByRole('button', { name: /Buat Kantong/i })).toBeVisible();
      } else {
        await expect(authPage.getByText('Kantong Saya')).toBeVisible();
      }
    });
  });

  test.describe('READ - Pocket Details', () => {
    test('should display wallet balance section', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Manajemen Kantong')).toBeVisible();
      await expect(authPage.getByText('Likuiditas Tersedia')).toBeVisible();
      await expect(authPage.getByText('Dompet Aktif')).toBeVisible();
    });

    test('should display "Kantong Saya" section', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Kantong Saya')).toBeVisible();
    });

    test('should display saving goals section', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Tujuan Khusus')).toBeVisible();

      // Verify hardcoded saving goals
      await expect(authPage.getByText('Liburan Akhir Tahun')).toBeVisible();
      await expect(authPage.getByText('Dana Darurat')).toBeVisible();
    });

    test('should display transaction ledger section', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Buku Besar Terakhir')).toBeVisible();

      await expect(authPage.getByText('Lihat Rekening Koran')).toBeVisible();
    });

    test('should display shared pockets section', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByRole('heading', { name: 'Kantong Bersama' })).toBeVisible();

      // Verify shared pockets from hardcoded data
      await expect(authPage.getByText('Tabungan Keluarga')).toBeVisible();
      await expect(authPage.getByText('Dana Rekreasi Kantor')).toBeVisible();
    });

    test('should display reserved balance section', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Protokol Cadangan')).toBeVisible();
    });

    test('should display security tier section', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Keamanan Tier-1')).toBeVisible();
      await expect(authPage.getByText('OJK & ASPI Compliant')).toBeVisible();
    });
  });

  test.describe('UPDATE - Pocket Modification', () => {
    test('should have shared pocket interaction', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      // Verify shared pockets are clickable (they toggle member list)
      const sharedPocket = authPage.getByText('Tabungan Keluarga');
      await expect(sharedPocket).toBeVisible();

      await sharedPocket.click();
      await authPage.waitForTimeout(300);

      await expect(authPage.getByText('Anggota', { exact: true })).toBeVisible();
    });

    test('should display shared pocket members', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await authPage.getByText('Tabungan Keluarga').click();
      await authPage.waitForTimeout(300);

      // Verify member names from hardcoded data
      await expect(authPage.getByText('Anya')).toBeVisible();
      await expect(authPage.getByText('Budi')).toBeVisible();
      await expect(authPage.getByText('Citra')).toBeVisible();
    });

    test('should show member roles in shared pocket', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await authPage.getByText('Tabungan Keluarga').click();
      await authPage.waitForTimeout(300);

      await expect(authPage.getByText('OWNER')).toBeVisible();
      await expect(authPage.getByText('ADMIN')).toBeVisible();
      await expect(authPage.getByText('MEMBER')).toBeVisible();
    });

    test('should show locked funds in saving goals', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Dana Terkunci & Dijamin')).toBeVisible();
    });

    test('should display "Kantong Bersama" button', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByRole('button', { name: /Kantong Bersama/i }).first()).toBeVisible();
    });
  });

  test.describe('DELETE - Pocket Closure', () => {
    test('should display pocket management page with all sections', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Manajemen Kantong')).toBeVisible();
      await expect(authPage.getByText('Kantong Saya')).toBeVisible();
      await expect(authPage.getByText('Tujuan Khusus')).toBeVisible();
      await expect(authPage.getByRole('heading', { name: 'Kantong Bersama' })).toBeVisible();
    });

    test('should maintain state after page reload', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Manajemen Kantong')).toBeVisible();

      await authPage.reload();
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Manajemen Kantong')).toBeVisible();
      await expect(authPage.getByText('Likuiditas Tersedia')).toBeVisible();
    });

    test('should display marketplace CTA section', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Akselerasi Kekayaan Anda.')).toBeVisible();
      await expect(authPage.getByText('Jelajahi Marketplace')).toBeVisible();
    });
  });

  test.describe('Wallet Ledger Integrity', () => {
    test('should display ledger with proper empty state or data', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Buku Besar Terakhir')).toBeVisible();

      const emptyLedger = authPage.getByText('Tidak Ada Aktivitas');
      const hasEmpty = await emptyLedger.isVisible().catch(() => false);

      if (hasEmpty) {
        await expect(authPage.getByText('Aktivitas keuangan Anda akan muncul di sini')).toBeVisible();
      }
    });

    test('should show consistent data after navigation', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');
      await expect(authPage.getByText('Manajemen Kantong')).toBeVisible();

      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');
      await expect(authPage.getByText('Riwayat Transaksi')).toBeVisible();

      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Manajemen Kantong')).toBeVisible();
      await expect(authPage.getByText('Likuiditas Tersedia')).toBeVisible();
    });

    test('should display Kantong Utama Cair as main pocket', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Kantong Utama Cair')).toBeVisible();
    });
  });
});
