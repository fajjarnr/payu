/**
 * Real Playwright fixtures - no mocks, hits real backend (postgres + keycloak + gateway + web-app).
 * Requires: podman compose up -d (payu) or login-gate-compose.yml, users customer1/P@ssw0rd12345 in realm payu.
 */

import { test as base, expect, Page } from '@playwright/test';

type PayUFixtures = {
  authPage: Page;
};

async function performRealLogin(page: Page, username = 'customer1', password = 'P@ssw0rd12345') {
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

export async function gotoProtected(page: Page, path: string, username = 'customer1', password = 'P@ssw0rd12345') {
  await page.goto(path);
  if (page.url().includes('/login')) {
    await performRealLogin(page, username, password);
    await page.goto(path);
  }
  await page.waitForLoadState('domcontentloaded');
}

export async function performLogin(page: Page, username = 'customer1', password = 'P@ssw0rd12345') {
  await performRealLogin(page, username, password);
}
