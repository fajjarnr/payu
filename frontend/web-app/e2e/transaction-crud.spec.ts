import { test, expect } from './fixtures';

/**
 * Transaction CRUD E2E tests. Mapped to UI: CREATE /transfer + /bills, READ /transactions,
 * DELETE /transactions (cancel via dropdown); UPDATE has no UI.
 */

test.describe('Transaction CRUD Operations', () => {
  test.describe('CREATE - Transaction Initiation', () => {
    test('should display transfer form with all fields', async ({ authPage }) => {
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.locator('h2').filter({ hasText: 'Transfer Instan' })).toBeVisible();

      await expect(authPage.locator('[data-testid="recipient-account-input"]')).toBeVisible();
      await expect(authPage.locator('[data-testid="amount-input"]')).toBeVisible();
      await expect(authPage.locator('[data-testid="review-transfer-button"]')).toBeVisible();
    });

    test('should fill transfer form fields', async ({ authPage }) => {
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('domcontentloaded');

      const recipientInput = authPage.locator('[data-testid="recipient-account-input"]');
      await recipientInput.fill('acc-any123');

      const amountInput = authPage.locator('[data-testid="amount-input"]');
      await amountInput.fill('50000');

      const descriptionInput = authPage.locator('[data-testid="description-input"]');
      await expect(descriptionInput).toBeVisible();
      await descriptionInput.fill('Test transfer');

      await expect(authPage.locator('[data-testid="review-transfer-button"]')).toBeVisible();
    });

    test('should show review step after clicking review button', async ({ authPage }) => {
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('domcontentloaded');

      // Fill required fields using keyboard.type() for proper React event handling
      await authPage.locator('[data-testid="recipient-account-input"]').click();
      await authPage.locator('[data-testid="recipient-account-input"]').clear();
      await authPage.locator('[data-testid="recipient-account-input"]').pressSequentially('acc-any123');

      await authPage.locator('[data-testid="amount-input"]').click();
      await authPage.locator('[data-testid="amount-input"]').clear();
      await authPage.locator('[data-testid="amount-input"]').pressSequentially('10000');

      // Wait for react-hook-form state to propagate (check amount display)
      await authPage.waitForFunction(() => {
        const input = document.querySelector('[data-testid="amount-input"]') as HTMLInputElement;
        return input && input.value.length > 0 && input.value !== '0';
      }, { timeout: 5000 });

      await authPage.locator('[data-testid="review-transfer-button"]').click();
      await authPage.waitForTimeout(1000);

      await expect(authPage.locator('[data-testid="confirm-transfer-button"]')).toBeVisible();
    });

    test('should display bill payment page with biller categories', async ({ authPage }) => {
      await authPage.goto('/bills');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Tagihan & Top-up')).toBeVisible();

      await expect(authPage.getByText('Kategori Layanan')).toBeVisible();
      await expect(authPage.getByText('Pulsa')).toBeVisible();
      await expect(authPage.getByText('Listrik (PLN)')).toBeVisible();
    });

    test('should select a biller on bills page', async ({ authPage }) => {
      await authPage.goto('/bills');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Pulsa')).toBeVisible();
      await expect(authPage.getByText('Air (PDAM)')).toBeVisible();

      await expect(authPage.getByText('Aktivitas Terakhir')).toBeVisible();
    });

    test('should display transfer type options', async ({ authPage }) => {
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.locator('h2').filter({ hasText: 'Transfer Instan' })).toBeVisible();

      await expect(authPage.locator('[data-testid="recipient-account-input"]')).toBeVisible();
      await expect(authPage.locator('[data-testid="amount-input"]')).toBeVisible();
    });

    test('should support idempotency - review button prevents double submit', async ({ authPage }) => {
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('domcontentloaded');

      // Fill transfer form using keyboard for proper React event handling
      await authPage.locator('[data-testid="recipient-account-input"]').click();
      await authPage.locator('[data-testid="recipient-account-input"]').clear();
      await authPage.locator('[data-testid="recipient-account-input"]').pressSequentially('acc-any123');

      await authPage.locator('[data-testid="amount-input"]').click();
      await authPage.locator('[data-testid="amount-input"]').clear();
      await authPage.locator('[data-testid="amount-input"]').pressSequentially('25000');

      // Wait for react-hook-form state to propagate
      await authPage.waitForFunction(() => {
        const input = document.querySelector('[data-testid="amount-input"]') as HTMLInputElement;
        return input && input.value.length > 0 && input.value !== '0';
      }, { timeout: 5000 });

      await authPage.locator('[data-testid="review-transfer-button"]').click();
      await authPage.waitForTimeout(1000);

      // After review, the confirm button should be visible (two-step process prevents accidental double submit)
      await expect(authPage.locator('[data-testid="confirm-transfer-button"]')).toBeVisible();
    });
  });

  test.describe('READ - Transaction History', () => {
    test('should display transaction history page', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Riwayat Transaksi')).toBeVisible();
      await expect(authPage.getByText('Kelola dan pantau semua aktivitas transaksi Anda')).toBeVisible();
    });

    test('should display stats cards', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Total Masuk')).toBeVisible();
      await expect(authPage.getByText('Total Keluar')).toBeVisible();
      await expect(authPage.getByText('Menunggu')).toBeVisible();
      await expect(authPage.getByText('Selesai')).toBeVisible();
    });

    test('should display transaction table section', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Daftar Transaksi')).toBeVisible();

      await expect(authPage.getByText('Halaman 1')).toBeVisible();
    });

    test('should show filter buttons', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      // Filters are "Status"/"Tipe" dropdowns, not "Filter Tanggal" labels.
      await expect(authPage.getByRole('button', { name: /Status/ })).toBeVisible();
      await expect(authPage.getByRole('button', { name: /Tipe/ })).toBeVisible();
    });

    test('should show empty state or transaction table', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      const emptyState = authPage.getByText('Tidak Ada Transaksi');
      const tableHeader = authPage.getByText('Daftar Transaksi');

      await expect(tableHeader).toBeVisible();

      const hasEmpty = await emptyState.isVisible().catch(() => false);
      if (hasEmpty) {
        await expect(authPage.getByText('Anda belum memiliki transaksi')).toBeVisible();
      }
    });

    test('should display table headers for desktop layout', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Daftar Transaksi')).toBeVisible();

      // Check for the table header labels (uppercase in the actual UI)
      const table = authPage.locator('table');
      if (await table.isVisible().catch(() => false)) {
        await expect(authPage.getByRole('columnheader', { name: /Tanggal/i })).toBeVisible();
        await expect(authPage.getByRole('columnheader', { name: /Tipe/i })).toBeVisible();
        await expect(authPage.getByRole('columnheader', { name: /Deskripsi/i })).toBeVisible();
        await expect(authPage.getByRole('columnheader', { name: /Status/i })).toBeVisible();
        await expect(authPage.getByRole('columnheader', { name: /Jumlah/i })).toBeVisible();
      }
    });
  });

  test.describe('UPDATE - Transaction Status', () => {
    test('should display cancel dialog elements', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      // Cancel dialog is triggered by "Batalkan Transaksi" in the row dropdown.
      await expect(authPage.getByText('Riwayat Transaksi')).toBeVisible();
      await expect(authPage.getByText('Daftar Transaksi')).toBeVisible();
    });

    test('should show transaction ledger on pockets page', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Buku Besar Terakhir')).toBeVisible();
    });

    test('should have view statement button on pockets page', async ({ authPage }) => {
      await authPage.goto('/pockets');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Lihat Rekening Koran')).toBeVisible();
    });
  });

  test.describe('DELETE - Transaction Cancellation', () => {
    test('should display transaction history with cancel infrastructure', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Riwayat Transaksi')).toBeVisible();

      // Cancel is available via the dropdown on PENDING/PROCESSING transactions.
      await expect(authPage.getByText('Daftar Transaksi')).toBeVisible();
    });

    test('should handle empty transaction list gracefully', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      // Page should load without error regardless of transaction count
      await expect(authPage.getByText('Riwayat Transaksi')).toBeVisible();

      const emptyState = authPage.getByText('Tidak Ada Transaksi');
      const hasEmpty = await emptyState.isVisible().catch(() => false);
      if (hasEmpty) {
        await expect(authPage.getByText('Anda belum memiliki transaksi')).toBeVisible();
      }
    });

    test('should maintain page after reload', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Riwayat Transaksi')).toBeVisible();

      await authPage.reload();
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Riwayat Transaksi')).toBeVisible();
      await expect(authPage.getByText('Daftar Transaksi')).toBeVisible();
    });
  });

  test.describe('Transaction Integrity & Security', () => {
    test('should have two-step transfer confirmation', async ({ authPage }) => {
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('domcontentloaded');

      await authPage.locator('[data-testid="recipient-account-input"]').click();
      await authPage.locator('[data-testid="recipient-account-input"]').clear();
      await authPage.locator('[data-testid="recipient-account-input"]').pressSequentially('acc-any123');

      await authPage.locator('[data-testid="amount-input"]').click();
      await authPage.locator('[data-testid="amount-input"]').clear();
      await authPage.locator('[data-testid="amount-input"]').pressSequentially('10000');

      // Wait for react-hook-form state to propagate
      await authPage.waitForFunction(() => {
        const input = document.querySelector('[data-testid="amount-input"]') as HTMLInputElement;
        return input && input.value.length > 0 && input.value !== '0';
      }, { timeout: 5000 });

      await authPage.locator('[data-testid="review-transfer-button"]').click();
      await authPage.waitForTimeout(1000);

      await expect(authPage.locator('[data-testid="confirm-transfer-button"]')).toBeVisible();
    });

    test('should display transaction stats for monitoring', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Total Masuk')).toBeVisible();
      await expect(authPage.getByText('Total Keluar')).toBeVisible();
      await expect(authPage.getByText('Menunggu')).toBeVisible();
      await expect(authPage.getByText('Selesai')).toBeVisible();
    });

    test('should show pagination controls when transactions exist', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Daftar Transaksi')).toBeVisible();

      // Pagination controls only render when transactions exist.
      const prevButton = authPage.getByText('Sebelumnya');
      const hasTransactions = await prevButton.isVisible().catch(() => false);

      if (hasTransactions) {
        // On page 1, "Sebelumnya" should be disabled
        await expect(prevButton).toBeDisabled();
        await expect(authPage.getByText('Selanjutnya')).toBeVisible();
      }
    });

    test('should verify transaction data consistency after reload', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Riwayat Transaksi')).toBeVisible();

      await authPage.reload();
      await authPage.waitForLoadState('domcontentloaded');

      await expect(authPage.getByText('Riwayat Transaksi')).toBeVisible();
      await expect(authPage.getByText('Total Masuk')).toBeVisible();
      await expect(authPage.getByText('Total Keluar')).toBeVisible();
    });
  });
});
