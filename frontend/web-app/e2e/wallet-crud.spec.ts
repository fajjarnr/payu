import { test, expect } from './fixtures';

/**
 * Wallet CRUD E2E Tests
 * Tests Create, Read, Update, Delete operations for Wallet entity
 */

test.describe('Wallet CRUD Operations', () => {
  test.describe('CREATE - Wallet Creation', () => {
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
      await authPage.goto('/wallets');
      await authPage.waitForLoadState('networkidle');
    });

    test('should create new wallet', async ({ authPage }) => {
      // Click create wallet button
      await authPage.click('button:has-text("Buat Dompet Baru")');

      // Fill wallet details
      await authPage.fill('input[name="walletName"]', 'Tabungan Liburan');
      await authPage.selectOption('select[name="walletType"]', 'SAVINGS');
      await authPage.fill('input[name="initialBalance"]', '1000000');

      // Submit
      await authPage.click('button[type="submit"]');

      // Verify wallet created
      await expect(authPage.getByText('Dompet berhasil dibuat')).toBeVisible();
      await expect(authPage.getByText('Tabungan Liburan')).toBeVisible();
    });

    test('should create multiple wallets', async ({ authPage }) => {
      // Create first wallet
      await authPage.click('button:has-text("Buat Dompet Baru")');
      await authPage.fill('input[name="walletName"]', 'Wallet 1');
      await authPage.fill('input[name="initialBalance"]', '500000');
      await authPage.click('button[type="submit"]');

      await authPage.waitForTimeout(1000);

      // Create second wallet
      await authPage.click('button:has-text("Buat Dompet Baru")');
      await authPage.fill('input[name="walletName"]', 'Wallet 2');
      await authPage.fill('input[name="initialBalance"]', '1000000');
      await authPage.click('button[type="submit"]');

      // Verify both wallets exist
      await expect(authPage.getByText('Wallet 1')).toBeVisible();
      await expect(authPage.getByText('Wallet 2')).toBeVisible();
    });

    test('should validate wallet name is required', async ({ authPage }) => {
      await authPage.click('button:has-text("Buat Dompet Baru")');
      await authPage.fill('input[name="walletName"]', '');
      await authPage.click('button[type="submit"]');

      await expect(authPage.getByText('Nama dompet wajib diisi')).toBeVisible();
    });

    test('should set initial balance to zero if not specified', async ({ authPage }) => {
      await authPage.click('button:has-text("Buat Dompet Baru")');
      await authPage.fill('input[name="walletName"]', 'Empty Wallet');
      // Don't fill initial balance
      await authPage.click('button[type="submit"]');

      // Verify wallet created with zero balance
      await expect(authPage.getByText('Empty Wallet')).toBeVisible();
      await expect(authPage.getByText('Rp 0')).toBeVisible();
    });
  });

  test.describe('READ - Wallet Details', () => {
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
      await authPage.goto('/wallets');
      await authPage.waitForLoadState('networkidle');
    });

    test('should display wallet list', async ({ authPage }) => {
      // Verify wallets container
      await expect(authPage.locator('[data-testid="wallet-list"]')).toBeVisible();

      // Verify each wallet shows balance
      const walletBalances = authPage.locator('[data-testid="wallet-balance"]');
      const count = await walletBalances.count();
      expect(count).toBeGreaterThan(0);
    });

    test('should show wallet details on click', async ({ authPage }) => {
      // Click on first wallet
      await authPage.locator('[data-testid="wallet-card"]').first().click();

      // Verify wallet details page
      await expect(authPage.getByText('Detail Dompet')).toBeVisible();
      await expect(authPage.getByText('Riwayat Transaksi')).toBeVisible();
      await expect(authPage.getByText('Saldo Saat Ini')).toBeVisible();
    });

    test('should display wallet transaction history', async ({ authPage }) => {
      // Open wallet details
      await authPage.locator('[data-testid="wallet-card"]').first().click();

      // Verify transaction list
      await expect(authPage.locator('[data-testid="transaction-list"]')).toBeVisible();

      // Transactions should show amount, type, and date
      const transactions = authPage.locator('[data-testid="transaction-item"]');
      if (await transactions.count() > 0) {
        await expect(transactions.first().locator('[data-testid="transaction-amount"]')).toBeVisible();
      }
    });

    test('should calculate total balance correctly', async ({ authPage }) => {
      // Get all wallet balances
      const balances = await authPage.locator('[data-testid="wallet-balance"]').allTextContents();

      // Calculate expected total
      let total = 0;
      for (const balance of balances) {
        const numeric = parseInt(balance.replace(/[^0-9]/g, ''));
        total += numeric;
      }

      // Verify total displayed matches
      const displayedTotal = await authPage.locator('[data-testid="total-balance"]').textContent();
      const displayedTotalNumeric = parseInt(displayedTotal?.replace(/[^0-9]/g, '') || '0');

      expect(displayedTotalNumeric).toBe(total);
    });
  });

  test.describe('UPDATE - Wallet Modification', () => {
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
      await authPage.goto('/wallets');
      await authPage.waitForLoadState('networkidle');
    });

    test('should update wallet name', async ({ authPage }) => {
      // Click edit on first wallet
      await authPage.locator('[data-testid="wallet-edit-btn"]').first().click();

      // Update name
      await authPage.fill('input[name="walletName"]', 'Updated Wallet Name');
      await authPage.click('button:has-text("Simpan")');

      // Verify update
      await expect(authPage.getByText('Dompet berhasil diperbarui')).toBeVisible();
      await expect(authPage.getByText('Updated Wallet Name')).toBeVisible();
    });

    test('should update wallet description', async ({ authPage }) => {
      await authPage.locator('[data-testid="wallet-edit-btn"]').first().click();

      await authPage.fill('textarea[name="description"]', 'Updated description for this wallet');
      await authPage.click('button:has-text("Simpan")');

      await expect(authPage.getByText('Dompet berhasil diperbarui')).toBeVisible();
    });

    test('should not allow negative balance', async ({ authPage }) => {
      // Try to create wallet with negative balance
      await authPage.click('button:has-text("Buat Dompet Baru")');
      await authPage.fill('input[name="walletName"]', 'Test Wallet');
      await authPage.fill('input[name="initialBalance"]', '-1000');
      await authPage.click('button[type="submit"]');

      // Should show validation error
      await expect(authPage.getByText('Saldo tidak boleh negatif')).toBeVisible();
    });

    test('should archive wallet instead of delete', async ({ authPage }) => {
      // Click archive on wallet
      await authPage.locator('[data-testid="wallet-archive-btn"]').first().click();

      // Confirm archive
      await authPage.click('button:has-text("Ya, Arsipkan")');

      // Verify wallet archived
      await expect(authPage.getByText('Dompet berhasil diarsipkan')).toBeVisible();

      // Check archived wallets section
      await authPage.click('text=Dompet Diarsipkan');
      await expect(authPage.locator('[data-testid="archived-wallet"]')).toBeVisible();
    });
  });

  test.describe('DELETE - Wallet Removal', () => {
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
      await authPage.goto('/wallets');
      await authPage.waitForLoadState('networkidle');
    });

    test('should prevent deletion of wallet with balance', async ({ authPage }) => {
      // Try to delete wallet with balance
      await authPage.locator('[data-testid="wallet-delete-btn"]').first().click();

      // Should show error about balance
      await expect(authPage.getByText('Tidak dapat menghapus dompet dengan saldo'))
        .toBeVisible();
    });

    test('should allow deletion of empty wallet', async ({ authPage }) => {
      // Create empty wallet first
      await authPage.click('button:has-text("Buat Dompet Baru")');
      await authPage.fill('input[name="walletName"]', 'Empty Wallet To Delete');
      await authPage.fill('input[name="initialBalance"]', '0');
      await authPage.click('button[type="submit"]');

      await authPage.waitForTimeout(1000);

      // Find and delete the empty wallet
      const deleteBtn = authPage.locator('[data-testid="wallet-delete-btn"]').last();
      await deleteBtn.click();

      // Confirm deletion
      await authPage.click('button:has-text("Ya, Hapus")');

      // Verify deletion
      await expect(authPage.getByText('Dompet berhasil dihapus')).toBeVisible();
    });

    test('should require confirmation for wallet deletion', async ({ authPage }) => {
      await authPage.locator('[data-testid="wallet-delete-btn"]').first().click();

      // Cancel deletion
      await authPage.click('button:has-text("Batal")');

      // Wallet should still exist
      await expect(authPage.locator('[data-testid="wallet-card"]')).toBeVisible();
    });
  });

  test.describe('Wallet Ledger Integrity', () => {
    test('should maintain accurate ledger after multiple operations', async ({ authPage, context }) => {
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
      await authPage.goto('/wallets');
      await authPage.waitForLoadState('networkidle');

      // Create wallet with initial balance
      await authPage.click('button:has-text("Buat Dompet Baru")');
      await authPage.fill('input[name="walletName"]', 'Ledger Test Wallet');
      await authPage.fill('input[name="initialBalance"]', '1000000');
      await authPage.click('button[type="submit"]');

      await expect(authPage.getByText('Dompet berhasil dibuat')).toBeVisible();

      // The ledger should show:
      // 1. Initial credit of 1,000,000
      // 2. Running balance should equal current balance

      // Open wallet details
      await authPage.getByText('Ledger Test Wallet').click();

      // Verify initial transaction recorded
      await expect(authPage.getByText('Saldo Awal')).toBeVisible();
      await expect(authPage.getByText('Rp 1.000.000')).toBeVisible();
    });

    test('should prevent double spending', async ({ authPage, context }) => {
      // This test verifies the database constraint prevents overspending
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
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('networkidle');

      // Try to transfer more than balance
      await authPage.fill('input[name="destinationAccount"]', '1234567890');
      await authPage.fill('input[name="amount"]', '999999999'); // Very large amount
      await authPage.fill('input[name="description"]', 'Test transfer');

      await authPage.click('button[type="submit"]');

      // Should show insufficient balance error
      await expect(authPage.getByText('Saldo tidak mencukupi')).toBeVisible();
    });
  });
});
</parameter name="