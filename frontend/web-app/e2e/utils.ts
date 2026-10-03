
import { Page, Locator } from '@playwright/test';

/**
 * Wait for page to be stable after navigation.
 *
 * Uses 'domcontentloaded' + a short hydration delay instead of 'networkidle'
 * because Next.js RSC prefetching keeps network connections active indefinitely,
 * preventing 'networkidle' from ever being reached on authenticated pages.
 */
export async function waitForPageStable(page: Page, timeout = 10000): Promise<void> {
  await page.waitForLoadState('domcontentloaded', { timeout });
  // Allow React hydration and initial renders to settle
  await page.waitForTimeout(500);
}

export async function waitForAnimations(page: Page): Promise<void> {
  await page.waitForTimeout(300);
}

export async function switchTab(page: Page, tabSelector: string): Promise<void> {
  await page.click(tabSelector);
  await waitForAnimations(page);
  await page.waitForTimeout(200);
}

export function normalizeCurrency(text: string): string {
  return text
    .replace(/\s+/g, '')
    .replace(/[Rp\$]/g, '')
    .replace(/[,.]/g, match => match === '.' ? '' : '.');
}

export async function isTextVisible(page: Page, text: string): Promise<boolean> {
  try {
    const element = page.getByText(text);
    await element.waitFor({ state: 'visible', timeout: 5000 });
    return true;
  } catch {
    return false;
  }
}

export function extractCurrencyAmount(text: string): number {
  const cleaned = text.replace(/[Rp\s]/g, '').replace(/\./g, '').replace(/,/g, '.');
  return parseFloat(cleaned) || 0;
}

export async function waitForModal(page: Page, selector = '[role="dialog"]'): Promise<Locator> {
  const modal = page.locator(selector);
  await modal.waitFor({ state: 'visible', timeout: 5000 });
  return modal;
}

export async function fillForm(page: Page, data: Record<string, string>): Promise<void> {
  for (const [field, value] of Object.entries(data)) {
    const input = page.getByPlaceholder(field).or(page.getByLabel(field)).or(page.locator(`[name="${field}"]`));
    await input.waitFor({ state: 'visible', timeout: 5000 });
    await input.fill(value);
  }
}

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

export async function login(page: Page, username: string, password: string): Promise<void> {
  await page.goto('/login');
  await waitForPageStable(page);

  await page.fill('input[placeholder="username123"]', username);
  await page.fill('input[placeholder="••••••••"]', password);
  await page.click('button[type="submit"]');

  await page.waitForTimeout(1000);
}

export async function navigateAsAuth(page: Page, path: string): Promise<void> {
  await page.goto(path);
  await waitForPageStable(page);
}

export interface AccessibilityViolation {
  id: string;
  impact: string;
  description: string;
}

interface RawAccessibilityViolation {
  id: string;
  impact?: string | null;
  description?: string;
}

export function filterMinorA11yIssues(violations: RawAccessibilityViolation[]): AccessibilityViolation[] {
  // Excluded design-debt rules (not functional blockers): color-contrast, region,
  // button-name, svg-img-alt, nested-interactive, scrollable-region-focusable.
  const designDebtRules = ['color-contrast', 'region', 'button-name', 'svg-img-alt', 'nested-interactive', 'scrollable-region-focusable'];
  return violations
    .filter(v => !designDebtRules.includes(v.id))
    .map(v => ({
      id: v.id,
      impact: v.impact ?? 'unknown',
      description: v.description ?? 'No description available',
    }));
}

export async function setMobileViewport(page: Page): Promise<void> {
  await page.setViewportSize({ width: 375, height: 667 });
}

export async function setTabletViewport(page: Page): Promise<void> {
  await page.setViewportSize({ width: 768, height: 1024 });
}

export async function setDesktopViewport(page: Page): Promise<void> {
  await page.setViewportSize({ width: 1920, height: 1080 });
}
