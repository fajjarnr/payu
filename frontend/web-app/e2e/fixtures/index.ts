/**
 * Real Playwright fixtures - no mocks, hits real backend (postgres + keycloak + gateway + web-app).
 * Requires: podman compose up -d (payu) or login-gate-compose.yml, users customer1/P@ssw0rd12345 in realm payu.
 * Override per environment: E2E_PASSWORD (dev cluster realm uses P@ssw0rd123),
 * E2E_USERNAME. Defaults keep podman-compose green.
 */

import { test as base, expect, Page } from '@playwright/test';

type PayUFixtures = {
  authPage: Page;
};

export const E2E_USERNAME = process.env.E2E_USERNAME ?? 'customer1';
export const E2E_PASSWORD = process.env.E2E_PASSWORD ?? 'P@ssw0rd12345';

async function performRealLogin(page: Page, username = E2E_USERNAME, password = E2E_PASSWORD) {
  await page.goto('/login');
  const oidcButton = page.getByRole('button', { name: /Masuk|Sign in|Log in/i });
  await expect(oidcButton).toBeVisible({ timeout: 10000 });
  await oidcButton.click();
  await page.waitForURL(/\/realms\/payu\/protocol\/openid-connect\/auth/, { timeout: 15000 });
  await page.getByRole('textbox', { name: /Username or email/i }).fill(username);
  await page.getByRole('textbox', { name: /Password/i }).fill(password);
  await page.getByRole('button', { name: /Sign In|Log in|Masuk/i }).click();
  await page.waitForURL('**/dashboard', { timeout: 20000 });
  await expect(page).not.toHaveURL(/\/login\?error=/);
}
export const test = base.extend<PayUFixtures>({
  authPage: async ({ page }, use) => {
    await performRealLogin(page);
    // eslint-disable-next-line react-hooks/rules-of-hooks
    await use(page);
  },
});

export { expect };

export async function gotoProtected(page: Page, path: string, username = E2E_USERNAME, password = E2E_PASSWORD) {
  await page.goto(path);
  if (page.url().includes('/login')) {
    await performRealLogin(page, username, password);
    await page.goto(path);
  }
  await page.waitForLoadState('domcontentloaded');
}

export async function performLogin(page: Page, username = E2E_USERNAME, password = E2E_PASSWORD) {
  await performRealLogin(page, username, password);
}
