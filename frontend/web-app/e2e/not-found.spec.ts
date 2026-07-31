import { test, expect } from './fixtures';

test.describe('Unknown paths (WEB-005)', () => {
  test('returns 404 instead of redirecting to login', async ({ page }) => {
    const response = await page.goto('/this-path-does-not-exist-xyz');
    expect(response?.status()).toBe(404);
  });
});
