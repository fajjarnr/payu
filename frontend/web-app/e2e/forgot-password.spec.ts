import { test, expect } from './fixtures';

test.describe('Forgot Password (WEB-003)', () => {
  test('accessible without authentication — no redirect to login', async ({ page }) => {
    const response = await page.goto('/forgot-password');
    expect(response?.status()).toBe(200);
    await expect(page).toHaveURL(/forgot-password/);
    await expect(page.getByRole('heading', { name: /lupa password/i })).toBeVisible();
    await expect(page.locator('input#email')).toBeVisible();
  });
});
