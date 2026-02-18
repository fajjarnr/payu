import { test, expect } from './fixtures';

/**
 * Transaction CRUD E2E Tests
 * Tests Create, Read, Update (status), and Cancel operations for Transaction entity
 */

test.describe('Transaction CRUD Operations', () => {
  test.describe('CREATE - Transaction Initiation', () => {
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
    });

    test('should create transfer transaction', async ({ authPage }) => {
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('networkidle');

      // Fill transfer form
      await authPage.fill('input[name="destinationAccount"]', '1234567890');
      await authPage.fill('input[name="destinationBank"]', 'BANK_BCA');
      await authPage.fill('input[name="amount"]', '50000');
      await authPage.fill('input[name="description"]', 'Test transfer');

      // Submit
      await authPage.click('button[type="submit"]');

      // Confirm transfer
      await expect(authPage.getByText('Konfirmasi Transfer')).toBeVisible();
      await authPage.click('button:has-text("Konfirmasi")');

      // Enter PIN
      await authPage.fill('input[name="pin"]', '123456');
      await authPage.click('button:has-text("Konfirmasi PIN")');

      // Verify transaction created
      await expect(authPage.getByText('Transfer Berhasil')).toBeVisible();
      await expect(authPage.getByText('Rp 50.000')).toBeVisible();
    });

    test('should create QRIS payment transaction', async ({ authPage }) => {
      await authPage.goto('/pay/qris');
      await authPage.waitForLoadState('networkidle');

      // Scan or enter QRIS code
      await authPage.fill('input[name="qrisCode"]', 'TESTQRIS123456');
      await authPage.click('button:has-text("Scan")');

      // Verify merchant details
      await expect(authPage.getByText('Detail Merchant')).toBeVisible();

      // Confirm payment
      await authPage.click('button:has-text("Bayar")');
      await authPage.fill('input[name="pin"]', '123456');
      await authPage.click('button:has-text("Konfirmasi")');

      await expect(authPage.getByText('Pembayaran Berhasil')).toBeVisible();
    });

    test('should create virtual account payment', async ({ authPage }) => {
      await authPage.goto('/bills');
      await authPage.waitForLoadState('networkidle');

      // Select bill type
      await authPage.click('text:has("Listrik")');
      await authPage.fill('input[name="customerNumber"]', '123456789012');
      await authPage.click('button:has-text("Cek Tagihan")');

      // Verify bill details
      await expect(authPage.getByText('Detail Tagihan')).toBeVisible();

      // Pay bill
      await authPage.click('button:has-text("Bayar")');
      await authPage.fill('input[name="pin"]', '123456');
      await authPage.click('button:has-text("Konfirmasi")');

      await expect(authPage.getByText('Pembayaran Berhasil')).toBeVisible();
    });

    test('should validate minimum transfer amount', async ({ authPage }) => {
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('networkidle');

      await authPage.fill('input[name="destinationAccount"]', '1234567890');
      await authPage.fill('input[name="amount"]', '1000'); // Below minimum

      await authPage.click('button[type="submit"]');

      await expect(authPage.getByText('Minimum transfer Rp 10.000')).toBeVisible();
    });

    test('should validate sufficient balance', async ({ authPage }) => {
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('networkidle');

      await authPage.fill('input[name="destinationAccount"]', '1234567890');
      await authPage.fill('input[name="amount"]', '999999999999'); // Exceeds balance

      await authPage.click('button[type="submit"]');

      await expect(authPage.getByText('Saldo tidak mencukupi')).toBeVisible();
    });

    test('should require PIN for transaction confirmation', async ({ authPage }) => {
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('networkidle');

      await authPage.fill('input[name="destinationAccount"]', '1234567890');
      await authPage.fill('input[name="amount"]', '50000');
      await authPage.click('button[type="submit"]');
      await authPage.click('button:has-text("Konfirmasi")');

      // Try to submit without PIN
      await authPage.click('button:has-text("Konfirmasi PIN")');

      await expect(authPage.getByText('PIN wajib diisi')).toBeVisible();
    });

    test('should support idempotency for duplicate requests', async ({ authPage }) => {
      await authPage.goto('/transfer');
      await authPage.waitForLoadState('networkidle');

      await authPage.fill('input[name="destinationAccount"]', '1234567890');
      await authPage.fill('input[name="amount"]', '25000');

      // Submit twice quickly
      await authPage.click('button[type="submit"]');
      await authPage.waitForTimeout(100);
      await authPage.click('button[type="submit"]');

      // Should show duplicate transaction warning or handle gracefully
      await expect(authPage.getByText('Transaksi sedang diproses')).toBeVisible();
    });
  });

  test.describe('READ - Transaction History', () => {
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
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('networkidle');
    });

    test('should display transaction list', async ({ authPage }) => {
      await expect(authPage.locator('[data-testid="transaction-list"]')).toBeVisible();

      // Verify transaction items
      const transactions = authPage.locator('[data-testid="transaction-item"]');
      const count = await transactions.count();

      if (count > 0) {
        // Each transaction should show amount and type
        await expect(transactions.first().locator('[data-testid="txn-amount"]')).toBeVisible();
        await expect(transactions.first().locator('[data-testid="txn-type"]')).toBeVisible();
      }
    });

    test('should filter transactions by type', async ({ authPage }) => {
      // Click filter
      await authPage.click('button:has-text("Filter")');

      // Select transfer type
      await authPage.click('text:has("Transfer")');
      await authPage.click('button:has-text("Terapkan")');

      // Verify filtered results
      await authPage.waitForTimeout(500);
      const transactions = authPage.locator('[data-testid="transaction-item"]');

      // All visible transactions should be transfers
      for (let i = 0; i < await transactions.count(); i++) {
        const type = await transactions.nth(i).locator('[data-testid="txn-type"]').textContent();
        expect(type).toContain('Transfer');
      }
    });

    test('should filter transactions by date range', async ({ authPage }) => {
      await authPage.click('button:has-text("Filter")');

      // Set date range
      await authPage.fill('input[name="startDate"]', '2026-01-01');
      await authPage.fill('input[name="endDate"]', '2026-12-31');
      await authPage.click('button:has-text("Terapkan")');

      await expect(authPage.getByText('Menampilkan transaksi')).toBeVisible();
    });

    test('should search transactions by description', async ({ authPage }) => {
      await authPage.fill('input[name="search"]', 'transfer');
      await authPage.press('input[name="search"]', 'Enter');

      await authPage.waitForTimeout(500);

      // Results should contain search term
      const transactions = authPage.locator('[data-testid="transaction-item"]');
      for (let i = 0; i < Math.min(await transactions.count(), 3); i++) {
        const desc = await transactions.nth(i).textContent();
        expect(desc?.toLowerCase()).toContain('transfer');
      }
    });

    test('should display transaction details', async ({ authPage }) => {
      // Click first transaction
      await authPage.locator('[data-testid="transaction-item"]').first().click();

      // Verify detail view
      await expect(authPage.getByText('Detail Transaksi')).toBeVisible();
      await expect(authPage.getByText('ID Transaksi')).toBeVisible();
      await expect(authPage.getByText('Status')).toBeVisible();
      await expect(authPage.getByText('Waktu')).toBeVisible();
    });

    test('should support pagination for large history', async ({ authPage }) => {
      // Check if pagination exists
      const pagination = authPage.locator('[data-testid="pagination"]');

      if (await pagination.isVisible()) {
        // Click next page
        await authPage.click('button:has-text("Next")');
        await authPage.waitForTimeout(500);

        // Should show different transactions
        await expect(authPage.locator('[data-testid="transaction-item"]')).toBeVisible();
      }
    });
  });

  test.describe('UPDATE - Transaction Status', () => {
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
    });

    test('should mark transaction as favorite', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('networkidle');

      // Click star on first transaction
      await authPage.locator('[data-testid="txn-favorite-btn"]').first().click();

      // Verify marked as favorite
      await expect(authPage.locator('[data-testid="txn-favorite-btn"].active')).toBeVisible();
    });

    test('should add note to transaction', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('networkidle');

      // Open transaction details
      await authPage.locator('[data-testid="transaction-item"]').first().click();

      // Add note
      await authPage.click('button:has-text("Tambah Catatan")');
      await authPage.fill('textarea[name="note"]', 'Test note for this transaction');
      await authPage.click('button:has-text("Simpan")');

      await expect(authPage.getByText('Catatan disimpan')).toBeVisible();
    });

    test('should categorize transaction', async ({ authPage }) => {
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('networkidle');

      // Open transaction
      await authPage.locator('[data-testid="transaction-item"]').first().click();

      // Change category
      await authPage.click('button:has-text("Kategori")');
      await authPage.click('text:has("Makanan")');

      await expect(authPage.getByText('Kategori diperbarui')).toBeVisible();
    });
  });

  test.describe('DELETE - Transaction Cancellation', () => {
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
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('networkidle');
    });

    test('should cancel pending transaction', async ({ authPage }) => {
      // Find pending transaction
      const pendingTxn = authPage.locator('[data-testid="transaction-item"]').filter({
        hasText: 'Pending'
      }).first();

      if (await pendingTxn.isVisible()) {
        await pendingTxn.click();

        // Click cancel
        await authPage.click('button:has-text("Batalkan")');
        await authPage.click('button:has-text("Ya, Batalkan")');

        await expect(authPage.getByText('Transaksi dibatalkan')).toBeVisible();
      }
    });

    test('should not allow cancellation of completed transaction', async ({ authPage }) => {
      // Find completed transaction
      const completedTxn = authPage.locator('[data-testid="transaction-item"]').filter({
        hasText: 'Sukses'
      }).first();

      if (await completedTxn.isVisible()) {
        await completedTxn.click();

        // Cancel button should not exist or be disabled
        const cancelBtn = authPage.locator('button:has-text("Batalkan")');
        await expect(cancelBtn).toHaveCount(0);
      }
    });

    test('should require confirmation for cancellation', async ({ authPage }) => {
      const pendingTxn = authPage.locator('[data-testid="transaction-item"]').filter({
        hasText: 'Pending'
      }).first();

      if (await pendingTxn.isVisible()) {
        await pendingTxn.click();
        await authPage.click('button:has-text("Batalkan")');

        // Should show confirmation dialog
        await expect(authPage.getByText('Yakin ingin membatalkan?')).toBeVisible();

        // Cancel the cancellation
        await authPage.click('button:has-text("Tidak")');

        // Should still show transaction details
        await expect(authPage.getByText('Detail Transaksi')).toBeVisible();
      }
    });
  });

  test.describe('Transaction Integrity & Security', () => {
    test('should verify transaction immutable after completion', async ({ authPage, context }) => {
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
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('networkidle');

      // Find completed transaction
      const completedTxn = authPage.locator('[data-testid="transaction-item"]').filter({
        hasText: 'Sukses'
      }).first();

      if (await completedTxn.isVisible()) {
        await completedTxn.click();

        // Amount should not be editable
        const amountField = authPage.locator('input[name="amount"]');
        if (await amountField.isVisible()) {
          await expect(amountField).toBeDisabled();
        }

        // Status should show completed
        await expect(authPage.getByText('Sukses')).toBeVisible();
      }
    });

    test('should detect duplicate transaction attempts', async ({ authPage, context }) => {
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

      // Create first transaction
      await authPage.fill('input[name="destinationAccount"]', '9876543210');
      await authPage.fill('input[name="amount"]', '10000');
      await authPage.click('button[type="submit"]');

      // Try to create same transaction again immediately
      await authPage.goto('/transfer');
      await authPage.fill('input[name="destinationAccount"]', '9876543210');
      await authPage.fill('input[name="amount"]', '10000');
      await authPage.click('button[type="submit"]');

      // Should detect as duplicate
      await expect(authPage.getByText('Transaksi serupa terdeteksi')).toBeVisible();
    });

    test('should show transaction receipt', async ({ authPage, context }) => {
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
      await authPage.goto('/transactions');
      await authPage.waitForLoadState('networkidle');

      // Click on transaction
      await authPage.locator('[data-testid="transaction-item"]').first().click();

      // Download receipt
      await authPage.click('button:has-text("Unduh Resi")');

      // Verify receipt download started (in real scenario)
      await expect(authPage.getByText('Resi sedang diunduh')).toBeVisible();
    });
  });
});
