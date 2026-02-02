/**
 * E2E Test Utilities
 *
 * Common helper functions for Playwright E2E tests
 */

import { Page, Locator } from '@playwright/test';

/**
 * Wait for page to be stable (no network requests for 500ms)
 */
export async function waitForPageStable(page: Page, timeout = 5000): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout });
}

/**
 * Wait for animations to complete
 */
export async function waitForAnimations(page: Page): Promise<void> {
  await page.waitForTimeout(300);
}

/**
 * Safe tab switching with proper wait
 */
export async function switchTab(page: Page, tabSelector: string): Promise<void> {
  await page.click(tabSelector);
  await waitForAnimations(page);
  await page.waitForTimeout(200);
}

/**
 * Format currency for comparison (handles various formats)
 */
export function normalizeCurrency(text: string): string {
  return text
    .replace(/\s+/g, '') // Remove all spaces
    .replace(/[Rp\$]/g, '') // Remove currency symbols
    .replace(/[,.]/g, match => match === '.' ? '' : '.'); // Normalize separators
}

/**
 * Check if element is visible and has text
 */
export async function isTextVisible(page: Page, text: string): Promise<boolean> {
  try {
    const element = page.getByText(text);
    await element.waitFor({ state: 'visible', timeout: 5000 });
    return true;
  } catch {
    return false;
  }
}

/**
 * Get currency amount from text
 */
export function extractCurrencyAmount(text: string): number {
  const cleaned = text.replace(/[Rp\s]/g, '').replace(/\./g, '').replace(/,/g, '.');
  return parseFloat(cleaned) || 0;
}

/**
 * Wait for modal/dialog to be visible
 */
export async function waitForModal(page: Page, selector = '[role="dialog"]'): Promise<Locator> {
  const modal = page.locator(selector);
  await modal.waitFor({ state: 'visible', timeout: 5000 });
  return modal;
}

/**
 * Fill form with data
 */
export async function fillForm(page: Page, data: Record<string, string>): Promise<void> {
  for (const [field, value] of Object.entries(data)) {
    const input = page.getByPlaceholder(field).or(page.getByLabel(field)).or(page.locator(`[name="${field}"]`));
    await input.waitFor({ state: 'visible', timeout: 5000 });
    await input.fill(value);
  }
}

/**
 * Safe click with retry
 */
export async function safeClick(page: Page, selector: string, maxRetries = 3): Promise<void> {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const element = page.locator(selector);
      await element.waitFor({ state: 'visible', timeout: 5000 });
      await element.click({ timeout: 5000 });
      return;
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      await page.waitForTimeout(500);
    }
  }
}

/**
 * Login helper - performs login with given credentials
 */
export async function login(page: Page, username: string, password: string): Promise<void> {
  await page.goto('/login');
  await waitForPageStable(page);

  await page.fill('input[placeholder="username123"]', username);
  await page.fill('input[placeholder="••••••••"]', password);
  await page.click('button[type="submit"]');

  // Wait for navigation or error
  await page.waitForTimeout(1000);
}

/**
 * Navigate to page with authentication
 * (Note: This would need actual auth implementation)
 */
export async function navigateAsAuth(page: Page, path: string): Promise<void> {
  // For now, just navigate - auth would be handled via API or session storage
  await page.goto(path);
  await waitForPageStable(page);
}

/**
 * Check for accessibility violations (for non-critical issues)
 */
export interface AccessibilityViolation {
  id: string;
  impact: string;
  description: string;
}

/**
 * Filter out minor accessibility issues that are acceptable in test environment
 */
export function filterMinorA11yIssues(violations: any[]): AccessibilityViolation[] {
  // Filter out color-contrast issues as they are design-related, not functional
  return violations.filter(v => v.id !== 'color-contrast');
}

/**
 * Mobile viewport helper
 */
export async function setMobileViewport(page: Page): Promise<void> {
  await page.setViewportSize({ width: 375, height: 667 });
}

/**
 * Tablet viewport helper
 */
export async function setTabletViewport(page: Page): Promise<void> {
  await page.setViewportSize({ width: 768, height: 1024 });
}

/**
 * Desktop viewport helper
 */
export async function setDesktopViewport(page: Page): Promise<void> {
  await page.setViewportSize({ width: 1920, height: 1080 });
}
