import { Page, BrowserContext, expect } from '@playwright/test';

/**
 * Real authentication - no mocks, hits Keycloak via OIDC PKCE.
 * Requires running: postgres + keycloak (8099) + auth-service + gateway + web-app (3001) + DB with users customer1/admin.
 * Users: customer1 / P@ssw0rd12345, admin / AdminP@ss12345 (see payu-realm-export.json / kcadm.sh).
 */

export async function performRealLogin(page: Page, username = 'customer1', password = 'P@ssw0rd12345') {
  await page.goto('/login');
  // OIDC button - no local phone/pin form
  const oidcButton = page.getByRole('button', { name: /Masuk|Sign in|Log in/i });
  await expect(oidcButton).toBeVisible({ timeout: 10000 });
  await oidcButton.click();
  // Keycloak login page
  await page.waitForURL(/\/realms\/payu\/protocol\/openid-connect\/auth/, { timeout: 15000 });
  await page.getByRole('textbox', { name: /Username or email/i }).fill(username);
  await page.getByRole('textbox', { name: /Password/i }).fill(password);
  await page.getByRole('button', { name: /Sign In|Log in|Masuk/i }).click();
  // Should land on dashboard with httpOnly cookies
  await page.waitForURL('**/dashboard', { timeout: 20000 });
  // Verify cookies set via BFF (httpOnly, not visible to JS, but subsequent requests succeed)
  await expect(page).not.toHaveURL(/\/login\?error=/);
}

export async function gotoWithRealAuth(page: Page, path: string, username = 'customer1', password = 'P@ssw0rd12345') {
  // Try direct goto, if redirected to login then perform real login
  await page.goto(path);
  if (page.url().includes('/login')) {
    await performRealLogin(page, username, password);
    await page.goto(path);
  }
  await page.waitForLoadState('domcontentloaded');
}

export async function isAuthenticated(page: Page): Promise<boolean> {
  const cookies = await page.context().cookies();
  return cookies.some(c => c.name === 'accessToken' || c.name === 'refreshToken');
}

// Backward compat - deprecated mock helper, now delegates to real login
export async function setupAuthCookies(context: BrowserContext) {
  throw new Error('setupAuthCookies mock removed - use performRealLogin with real Keycloak (customer1/P@ssw0rd12345). No mocks allowed.');
}

export async function gotoWithAuth(page: Page, context: BrowserContext, path: string) {
  return gotoWithRealAuth(page, path);
}
