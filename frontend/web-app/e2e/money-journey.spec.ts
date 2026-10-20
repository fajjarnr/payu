/**
 * Full money journey against the real backend: register → OIDC login → transfer →
 * balance delta → history. Requires the dev cluster and customer1 on a funded wallet.
 */
import { test, expect } from './fixtures';
import type { Page } from '@playwright/test';
import { waitForPageStable, extractCurrencyAmount } from './utils';

// E2E-RLS-001: gateway honors X-E2E-Test only with GATEWAY_RATE_LIMIT_TEST_MODE
// in dev/test profiles (prod ignores). BFF forwards it (allowlist).
test.use({ extraHTTPHeaders: { 'X-E2E-Test': 'true' } });

const RECIPIENT = '1001001002';
const AMOUNT = 10000;
const MEMO = `E2E journey ${Date.now()}`;

/** Skip on infra outage (429/502/503/504), and on 422 only when the body proves
 * AML velocity throttling; any other 422/500 is a product failure. */
async function skipOnInfra(status: number, body: () => Promise<string>): Promise<void> {
  if ([429, 502, 503, 504].includes(status)) {
    test.skip(true, `infra ${status}`);
  }
  if (status === 422 && (await body()).includes('AML_VELOCITY_LIMIT_EXCEEDED')) {
    test.skip(true, 'infra AML velocity budget exhausted');
  }
}

// Seeded sender (wallet-test-data.sql + Keycloak accountId claim). Reads the API
// directly for the delta proof; the dashboard card caches 30s via React Query.
const SENDER = '750e8400-e29b-41d4-a716-446655440001';

async function readMainBalance(authPage: Page): Promise<number> {
  const raw: string = await authPage.evaluate(async (id: string) => {
    const res = await fetch(`/api/v1/wallets/${id}/balance`, { credentials: 'include' });
    return res.text();
  }, SENDER);
  const body = JSON.parse(raw) as { data?: { balance?: string }; balance?: string };
  return extractCurrencyAmount(String(body.data?.balance ?? body.balance ?? '0'));
}

/** Counts the sender's transactions carrying the given description.
 * Same in-page BFF fetch pattern as readMainBalance (no new API client). */
async function countTransactionsByDescription(authPage: Page, description: string): Promise<number> {
  const raw: string = await authPage.evaluate(async ({ id, description }) => {
    const res = await fetch(`/api/v1/transactions?accountId=${id}`, { credentials: 'include' });
    const body = (await res.json()) as { data?: Array<{ description?: string }> };
    return String((body.data ?? []).filter((t) => t.description === description).length);
  }, { id: SENDER, description });
  return Number(raw);
}
test.describe.configure({ retries: 0 }); // transfers burn the 5tx/10min AML budget — no retry amplification

test.describe('PayU E2E — full money journey (real)', () => {
  test('register blocks short NIK (REG-VAL-001)', async ({ page }) => {
    await page.goto('/onboarding');
    await page.waitForLoadState('domcontentloaded');
    // Gates on KTP upload — attach a 1px PNG to enable continue.
    const png = Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
      'base64',
    );
    await page.locator('input[type="file"]').first().setInputFiles({ name: 'ktp.png', mimeType: 'image/png', buffer: png });
    await page.click('button:has-text("Lanjut ke Profil Data")');
    await page.getByPlaceholder(/16 digit/).fill('123');
    await page.getByPlaceholder(/Sesuai KTP/).fill('Test User');
    await page.getByPlaceholder(/nama@email.com/).fill('test@example.com');
    await page.getByPlaceholder(/unik & mudah diingat/).fill('testuser');
    await page.click('button:has-text("Konfirmasi Pendaftaran")');
    // AntD renders the zod validation message under the field (not a .text-red-500
    // span). Message source: src/types/index.ts -> nik rule.
    await expect(page.getByText('NIK must be exactly 16 digits')).toBeVisible({ timeout: 10000 });
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
    await authPage.getByLabel('Nominal Transfer').fill(String(AMOUNT));
    await authPage.locator('[data-testid="description-input"]').fill(MEMO).catch(() => {});
    await authPage.locator('[data-testid="review-transfer-button"]').click();
    // The review view has no "Tinjau Transfer" title; it shows the amount summary
    await expect(authPage.getByText('Jumlah Transfer')).toBeVisible({ timeout: 10000 });
    await expect(authPage.getByText(/Rp\s*10\.000/)).toBeVisible({ timeout: 10000 });

    const post = authPage.waitForResponse(
      (resp) => resp.url().includes('/api/v1/transactions/transfer') && resp.request().method() === 'POST',
      { timeout: 20000 },
    );
    await authPage.locator('[data-testid="confirm-transfer-button"]').click();
    const response = await post;
    await skipOnInfra(response.status(), () => response.text().catch(() => ''));
    expect(response.status()).toBeLessThan(300);

    // A settled mutation swaps the review view for the success screen
    // (h2 "Transfer Berhasil!"), it does not return to the review form.
    await expect(
      authPage.getByRole('heading', { name: 'Transfer Berhasil!' }),
    ).toBeVisible({ timeout: 15000 });
    // The balance delta is the real proof that the money moved.
    const after = await readMainBalance(authPage);
    expect(before - after).toBe(AMOUNT);
  });

  test('history shows the journey transfer (TRF-J-002)', async ({ authPage }) => {
    const memo = `E2E history ${Date.now()}`;
    await authPage.goto('/transfer');
    await waitForPageStable(authPage);
    await authPage.locator('[data-testid="recipient-account-input"]').fill(RECIPIENT);
    await authPage.getByLabel('Nominal Transfer').fill('5000');
    await authPage.locator('[data-testid="description-input"]').fill(memo).catch(() => {});
    await authPage.locator('[data-testid="review-transfer-button"]').click();
    await expect(authPage.locator('[data-testid="confirm-transfer-button"]')).toBeVisible({ timeout: 10000 });
    const post = authPage.waitForResponse(
      (resp) => resp.url().includes('/api/v1/transactions/transfer') && resp.request().method() === 'POST',
      { timeout: 20000 },
    );
    await authPage.locator('[data-testid="confirm-transfer-button"]').click();
    const response = await post;
    await skipOnInfra(response.status(), () => response.text().catch(() => ''));
    expect(response.status()).toBeLessThan(300);

    await authPage.goto('/transactions');
    await waitForPageStable(authPage);
    await expect(authPage.getByText(memo).first()).toBeVisible({ timeout: 15000 });
  });

  test('double confirm fires a single transfer POST (TRF-J-003)', async ({ authPage }) => {
    // TRF-J-003 needs a payload the gateway schema accepts: `description` is
    // required with minLength 1, and a schema-rejected POST answers 400, which
    // releases the submit latch in the page's onError handler — the second
    // click then fired a second POST and the test turned flaky. With a valid
    // description the first POST is accepted, the latch stays engaged, and
    // exactly one transfer (one DEBIT + one CREDIT) is created.
    const before = await readMainBalance(authPage);
    const beforeCount = await countTransactionsByDescription(authPage, MEMO);

    await authPage.goto('/transfer');
    await waitForPageStable(authPage);
    await expect(authPage.locator('[data-testid="recipient-account-input"]')).toBeVisible({ timeout: 15000 });

    await authPage.locator('[data-testid="recipient-account-input"]').fill(RECIPIENT);
    await authPage.getByLabel('Nominal Transfer').fill('5000');
    await authPage.locator('[data-testid="description-input"]').fill(MEMO).catch(() => {});
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
    const response = await firstPost;
    await skipOnInfra(response.status(), () => response.text().catch(() => ''));
    await authPage.waitForTimeout(1500);
    expect(posts).toBe(1);

    // Server-side invariant: exactly ONE new transaction, and the money moved
    // exactly once (the ledger holds one DEBIT + one CREDIT for it).
    const afterCount = await countTransactionsByDescription(authPage, MEMO);
    expect(afterCount - beforeCount).toBe(1);
    const after = await readMainBalance(authPage);
    expect(before - after).toBe(5000);
  });
});
