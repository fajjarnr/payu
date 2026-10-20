/**
 * Real Playwright fixtures - no mocks, hits real backend (postgres + keycloak + gateway + web-app).
 * Requires: podman compose up -d (payu) or login-gate-compose.yml, users customer1/P@ssw0rd123 in realm payu.
 * Override per environment: E2E_PASSWORD (dev cluster realm uses P@ssw0rd123),
 * E2E_USERNAME. Defaults keep podman-compose green.
 */

import { test as base, expect, Page } from '@playwright/test';

type PayUFixtures = {
  authPage: Page;
};

export const E2E_USERNAME = process.env.E2E_USERNAME ?? 'customer1';
export const E2E_PASSWORD = process.env.E2E_PASSWORD ?? 'P@ssw0rd123';

async function performRealLogin(page: Page, username = E2E_USERNAME, password = E2E_PASSWORD) {
  await page.goto('/login');
  const oidcButton = page.getByRole('button', { name: /Masuk|Sign in|Log in/i });
  await expect(oidcButton).toBeVisible({ timeout: 10000 });
  // The sign-in button's onClick is a React handler, so a click that lands before
  // hydration does nothing (the page stays on /login). The login form sets
  // document.title from a client effect, so the title is the hydration signal.
  await expect(page).toHaveTitle(/PayU Digital Banking$/, { timeout: 15000 });
  await oidcButton.click();
  // The redirect chain login -> BFF /api/auth/authorize -> Keycloak authorize must
  // complete before the URL changes. Wait for the navigation to commit rather than
  // for Keycloak's full page load, the assertions below auto-wait for the
  // elements they need.
  await page.waitForURL(/\/realms\/payu\/protocol\/openid-connect\/auth/, {
    waitUntil: 'commit',
    timeout: 30000,
  });
  await page.getByRole('textbox', { name: /Username or email/i }).fill(username);
  await page.getByRole('textbox', { name: /Password/i }).fill(password);
  await page.getByRole('button', { name: /Sign In|Log in|Masuk/i }).click();

  // Handle Keycloak "Update Password" page (temporary password on first login)
  const updatePassword = page.getByText(/Update Password|Perbarui Password/i);
  if (await updatePassword.isVisible().catch(() => false)) {
    const newPassword = process.env.E2E_NEW_PASSWORD ?? 'Dev-customer1-new-2026';
    await page.locator('#new-password').fill(newPassword);
    await page.locator('#confirm-password').fill(newPassword);
    await page.getByRole('button', { name: /Submit|Kirim/i }).click();
  }

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
