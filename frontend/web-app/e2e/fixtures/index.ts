/**
 * Extended Playwright test fixtures for PayU E2E tests
 *
 * These fixtures automatically handle authentication for protected routes.
 * The middleware.ts requires session cookies for routes like /investments, /dashboard, etc.
 *
 * NOTE: This file uses Playwright's fixture pattern which uses a `use` function
 * parameter that ESLint incorrectly flags as a React Hook. The react-hooks/rules-of-hooks
 * rule is disabled for this file since these are Playwright fixtures, not React components.
 */

/* eslint-disable react-hooks/rules-of-hooks */

import { test as base, expect, Page, BrowserContext } from '@playwright/test';

// Extend the base test with custom fixtures
type PayUFixtures = {
  // Auto-authenticated page for protected routes
  authPage: Page;
};

type PayUWorkerFixtures = {
  // Setup for each worker
  context: BrowserContext;
};

/**
 * Setup authentication cookies for protected routes
 */
async function setupAuthCookies(context: BrowserContext) {
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
 * Extended test with authentication support
 *
 * Usage:
 *   import { test, expect } from './fixtures';
 *
 *   test.describe('Protected Route', () => {
 *     test('should access protected page', async ({ authPage }) => {
 *       await authPage.goto('/investments');
 *       // Test is already authenticated!
 *     });
 *   });
 */
export const test = base.extend<PayUFixtures, PayUWorkerFixtures>({
  // Automatically set up auth for each test
  authPage: async ({ page, context }, use) => {
    await setupAuthCookies(context);
    await use(page);
  },

  // Ensure context is properly set up
  context: async ({ browser }, use) => {
    const context = await browser.newContext();
    await use(context);
    await context.close();
  },
});

// Re-export expect
export { expect };

/**
 * Helper to navigate to protected routes with auth
 */
export async function gotoProtected(page: Page, context: BrowserContext, path: string) {
  await setupAuthCookies(context);
  await page.goto(path);
  await page.waitForLoadState('networkidle');
}

/**
 * Helper to perform login flow
 */
export async function performLogin(
  page: Page,
  phone: string = '+6281234567890',
  pin: string = '123456'
) {
  await page.goto('/login');
  await page.fill('input[name="phone"]', phone);
  await page.fill('input[name="pin"]', pin);
  await page.click('button[type="submit"]');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}
