/**
 * Authentication fixtures for Playwright E2E tests
 *
 * These utilities help tests bypass authentication or set up authenticated sessions.
 * The middleware protects routes like /investments, /dashboard, etc.
 */

import { Page, BrowserContext } from '@playwright/test';

/**
 * Mock authentication cookies to bypass login for E2E tests
 * This simulates a logged-in user session
 */
export async function setupAuthCookies(context: BrowserContext) {
  // Set mock session cookies that the middleware checks for
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
    {
      name: 'payu_session',
      value: 'mock-session-for-e2e-tests',
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
  ]);
}

/**
 * Navigate to a protected route with authentication
 * Use this instead of page.goto() for protected pages
 */
export async function gotoWithAuth(page: Page, context: BrowserContext, path: string) {
  await setupAuthCookies(context);
  await page.goto(path);
  // Wait for the page to be fully loaded
  await page.waitForLoadState('domcontentloaded');
}

/**
 * Login helper that performs actual login flow
 * Use this for testing authentication flows
 */
export async function performLogin(page: Page, phone: string = '+6281234567890', pin: string = '123456') {
  await page.goto('/login');
  await page.fill('input[name="phone"]', phone);
  await page.fill('input[name="pin"]', pin);
  await page.click('button[type="submit"]');
  // Wait for redirect to dashboard
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

/**
 * Check if user is authenticated (has access to protected routes)
 */
export async function isAuthenticated(page: Page): Promise<boolean> {
  const cookies = await page.context().cookies();
  return cookies.some(cookie =>
    cookie.name === 'accessToken' ||
    cookie.name === 'refreshToken' ||
    cookie.name === 'payu_session'
  );
}
