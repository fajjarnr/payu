import { test, expect } from '@playwright/test';

test('capture landing page and dashboard', async ({ page }) => {
    // 1. Check Landing Page
    await page.goto('http://localhost:3000');
    await page.screenshot({ path: 'landing-page.png', fullPage: true });
    console.log('Landing page screenshot saved.');

    // 2. Attempt to check Dashboard by mocking localStorage
    // We need a mock token and user to trigger the Dashboard view in Home component
    await page.evaluate(() => {
        localStorage.setItem('token', 'mock-token');
        localStorage.setItem('user', JSON.stringify({ username: 'TestUser' }));
        localStorage.setItem('accountId', 'test-account-id');
    });

    await page.reload();
    await page.waitForTimeout(2000); // Wait for components to render
    await page.screenshot({ path: 'dashboard-ui.png', fullPage: true });
    console.log('Dashboard screenshot saved.');

    // Check for Sidebar/Dashboard elements
    const sidebar = page.locator('aside');
    const welcomeText = page.getByText('Welcome, TestUser!');

    if (await welcomeText.isVisible()) {
        console.log('UI Verification: Dashboard is visible with correct username.');
    } else {
        console.log('UI Verification: Dashboard NOT visible. Check if login logic changed.');
    }
});
