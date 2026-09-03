/**
 * PayU E2E — full money journey (real backend, no mocks).
 *
 * register(validation) -> login OIDC -> transfer submit -> balance delta -> history.
 * Requires: dev cluster reachable at PLAYWRIGHT_BASE_URL (default local 3001),
 * Keycloak user customer1 with account_id claim mapped to a funded wallet,
 * recipient acc-bud456 with a wallet. Amounts deterministic; memo unique per run.
 */
import { test, expect } from './fixtures';
import type { Page } from '@playwright/test';
import { waitForPageStable, extractCurrencyAmount } from './utils';

// E2E-RLS-001: gateway honors X-E2E-Test only with GATEWAY_RATE_LIMIT_TEST_MODE
// in dev/test profiles (prod ignores). BFF forwards it (allowlist).
test.use({ extraHTTPHeaders: { 'X-E2E-Test': 'true' } });

const RECIPIENT = 'acc-bud456';
const AMOUNT = 10000;
const MEMO = `E2E journey ${Date.now()}`;

/** Skip on infra outage (429/502/503/504); fail on 500/unexpected 4xx. */
function skipOnInfra(status: number): void {
  if ([429, 502, 503, 504].includes(status)) {
    test.skip(true, `infra ${status}`);
  }
}

async function readMainBalance(authPage: Page): Promise<number> {
  await authPage.goto('/dashboard');
  const card = authPage.locator('[data-testid="primary-balance-card"]');
  await expect(card).toBeVisible({ timeout: 15000 });
  const text = await card.locator('h2').innerText();
  return extractCurrencyAmount(text);
}

test.describe('PayU E2E — full money journey (real)', () => {
  test('register blocks short NIK (REG-VAL-001)', async ({ page }) => {
    await page.goto('/onboarding');
    await page.waitForLoadState('domcontentloaded');
    await page.click('button:has-text("Lanjut ke Profil Data")');
    await page.getByPlaceholder(/16 digit/).fill('123');
    await page.getByPlaceholder(/Sesuai KTP/).fill('Test User');
    await page.getByPlaceholder(/nama@email.com/).fill('test@example.com');
    await page.getByPlaceholder(/unik & mudah diingat/).fill('testuser');
    await page.click('button:has-text("Konfirmasi Pendaftaran")');
    await expect(page.locator('.text-red-500').first()).toBeVisible({ timeout: 10000 });
  });

  test('login lands on dashboard with balance card (LOGIN-J-001)', async ({ authPage }) => {
    await authPage.goto('/dashboard');
    await expect(authPage.locator('[data-testid="primary-balance-card"]')).toBeVisible({ timeout: 15000 });
    await expect(authPage.getByText('Saldo Utama')).toBeVisible();
  });

  test('transfer submits and debits exact amount (TRF-J-001)', async ({ authPage }) => {
    const before = await readMainBalance(authPage);

    await authPage.goto('/transfer');
    await waitForPageStable(authPage);
    await expect(authPage.locator('[data-testid="recipient-account-input"]')).toBeVisible({ timeout: 15000 });

    await authPage.locator('[data-testid="recipient-account-input"]').fill(RECIPIENT);
    await authPage.locator('[data-testid="amount-input"]').fill(String(AMOUNT));
    await authPage.locator('[data-testid="description-input"]').fill(MEMO).catch(() => {});
    await authPage.locator('[data-testid="review-transfer-button"]').click();
    await expect(authPage.getByText('Tinjau Transfer')).toBeVisible({ timeout: 10000 });
    await expect(authPage.getByText(/Rp\s*10\.000/)).toBeVisible({ timeout: 10000 });

    const post = authPage.waitForResponse(
      (resp) => resp.url().includes('/api/v1/transactions/transfer') && resp.request().method() === 'POST',
      { timeout: 20000 },
    );
    await authPage.locator('[data-testid="confirm-transfer-button"]').click();
    const response = await post;
    skipOnInfra(response.status());
    expect(response.status()).toBeLessThan(300);

    await expect(authPage.getByText(/Transfer berhasil|berhasil/i).first()).toBeVisible({ timeout: 15000 });

    const after = await readMainBalance(authPage);
    expect(before - after).toBe(AMOUNT);
  });

  test('history shows the journey transfer (TRF-J-002)', async ({ authPage }) => {
    await authPage.goto('/transactions');
    await waitForPageStable(authPage);
    await expect(authPage.getByText(MEMO).first()).toBeVisible({ timeout: 15000 });
  });

  test('double confirm fires a single transfer POST (TRF-J-003)', async ({ authPage }) => {
    await authPage.goto('/transfer');
    await waitForPageStable(authPage);
    await expect(authPage.locator('[data-testid="recipient-account-input"]')).toBeVisible({ timeout: 15000 });

    await authPage.locator('[data-testid="recipient-account-input"]').fill(RECIPIENT);
    await authPage.locator('[data-testid="amount-input"]').fill('5000');
    await authPage.locator('[data-testid="review-transfer-button"]').click();
    await expect(authPage.locator('[data-testid="confirm-transfer-button"]')).toBeVisible({ timeout: 10000 });

    let posts = 0;
    authPage.on('request', (req) => {
      if (req.url().includes('/api/v1/transactions/transfer') && req.method() === 'POST') posts += 1;
    });
    const firstPost = authPage.waitForResponse(
      (resp) => resp.url().includes('/api/v1/transactions/transfer') && resp.request().method() === 'POST',
      { timeout: 20000 },
    );
    const confirm = authPage.locator('[data-testid="confirm-transfer-button"]');
    await confirm.click();
    await confirm.click({ force: true }).catch(() => {});
    await firstPost;
    await authPage.waitForTimeout(1500);
    expect(posts).toBe(1);
  });
});
