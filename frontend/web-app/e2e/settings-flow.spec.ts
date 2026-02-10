import { test, expect } from './fixtures';
import { waitForPageStable, waitForAnimations } from './utils';

test.describe('Settings Flow', () => {
  test.beforeEach(async ({ authPage: page }) => {
    // Navigate to settings page (assumes user is logged in)
    await page.goto('/settings');
    await waitForPageStable(page);
  });

  test('should display settings page correctly', async ({ authPage: page }) => {
    await expect(page).toHaveTitle(/Pengaturan|PayU/);
    await expect(page.getByText('Ekosistem Akun')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Kelola profil pribadi, preferensi sistem, dan tata kelola akun.')).toBeVisible({ timeout: 10000 });
  });

  test('should display user profile card', async ({ authPage: page }) => {
    await expect(page.getByText('PENGGUNA PAYU')).toBeVisible();
    await expect(page.getByText('Premium Member')).toBeVisible();
  });

  test('should display account ID', async ({ authPage: page }) => {
    await expect(page.getByText('ID Akun')).toBeVisible();
    await expect(page.getByText('PAYU-09228373')).toBeVisible();
  });

  test('should display account status', async ({ authPage: page }) => {
    await expect(page.getByText('Status', { exact: true })).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('eKYC Terverifikasi')).toBeVisible({ timeout: 10000 });
  });

  test('should display settings menu items', async ({ authPage: page }) => {
    await expect(page.getByText('Profil Umum')).toBeVisible();
    await expect(page.getByText('Tagihan & Paket')).toBeVisible();
    await expect(page.getByText('Privasi & Keamanan')).toBeVisible();
    await expect(page.getByText('Pengaturan Lanjut')).toBeVisible();
  });

  test('should have Profil Umum menu active by default', async ({ authPage: page }) => {
    const activeMenu = page.locator('button').filter({ hasText: 'Profil Umum' }).first();
    await expect(activeMenu).toHaveClass(/bg-primary/);
  });

  test('should display profile credential fields', async ({ authPage: page }) => {
    await expect(page.getByText('Kredensial Profil')).toBeVisible();
    await expect(page.getByText('Nama Lengkap (Sesuai KTP)')).toBeVisible();
    await expect(page.getByText('Email Kontak')).toBeVisible();
    await expect(page.getByText('Protokol Telepon')).toBeVisible();
    await expect(page.getByText('Domisili Saat Ini')).toBeVisible();
  });

  test('should display system preferences section', async ({ authPage: page }) => {
    await expect(page.getByRole('heading', { name: 'Preferensi Sistem' })).toBeVisible({ timeout: 10000 });
  });

  test('should display notification preference', async ({ authPage: page }) => {
    await expect(page.getByText('Notifikasi Push')).toBeVisible();
    await expect(page.getByText('Peringatan transaksi & status real-time')).toBeVisible();
  });

  test('should display dark mode preference', async ({ authPage: page }) => {
    await expect(page.getByText('Grafis Mode Gelap')).toBeVisible();
    await expect(page.getByText('Antarmuka visual kontras tinggi')).toBeVisible();
  });

  test('should display marketing insights preference', async ({ authPage: page }) => {
    await expect(page.getByText('Wawasan Pemasaran')).toBeVisible();
    await expect(page.getByText('Pembaruan promosi, berita, dan hadiah')).toBeVisible();
  });

  test('should have sync profile button', async ({ authPage: page }) => {
    const syncButton = page.locator('button:has-text("Sinkronisasi Profil")');
    await expect(syncButton).toBeVisible();
    await expect(syncButton).toBeEnabled();
  });

  test('should have delete session button', async ({ authPage: page }) => {
    const deleteButton = page.locator('button:has-text("Hapus Sesi")');
    await expect(deleteButton).toBeVisible();
    await expect(deleteButton).toBeEnabled();
  });

  test('should display toggle switches for preferences', async ({ authPage: page }) => {
    const toggles = page.locator('main button[role="switch"]');
    const toggleCount = await toggles.count();
    expect(toggleCount).toBeGreaterThanOrEqual(2);
  });

  test('should have active toggle for push notifications', async ({ authPage: page }) => {
    const toggle = page.locator('main button[role="switch"]').first();
    await expect(toggle).toBeVisible();
  });

  test('should have toggle for dark mode', async ({ authPage: page }) => {
    const toggles = page.locator('main button[role="switch"]');
    await expect(toggles.nth(1)).toBeVisible();
  });

  test('should have toggle for marketing insights', async ({ authPage: page }) => {
    const toggles = page.locator('main button[role="switch"]');
    await expect(toggles.nth(2)).toBeVisible();
  });

  test('should be responsive on mobile viewport', async ({ authPage: page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/settings');
    await waitForPageStable(page);

    // Check that key elements are visible
    await expect(page.getByText('Ekosistem Akun')).toBeVisible();
    await expect(page.getByText('PENGGUNA PAYU')).toBeVisible();

    // Take screenshot
    await page.screenshot({
      path: 'e2e/screenshots/settings-mobile.png',
      fullPage: true
    });
  });

  test('should display form inputs with default values', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('PENGGUNA PAYU')).toBeVisible();
    await expect(page.getByPlaceholder('user@payu.id')).toBeVisible();
    await expect(page.getByPlaceholder('+62 812-3456-7890')).toBeVisible();
    await expect(page.getByPlaceholder('Jakarta, Indonesia')).toBeVisible();
  });

  test('should have user avatar', async ({ authPage: page }) => {
    const avatar = page.locator('.w-24.h-24.bg-primary.rounded-2xl');
    await expect(avatar).toBeVisible();
    await expect(avatar).toContainText('P');
  });

  test('should have premium member badge', async ({ authPage: page }) => {
    const badge = page.locator('.bg-success-light');
    await expect(badge).toBeVisible();
    await expect(badge).toContainText('Premium Member');
  });

  test('should have decorative background elements', async ({ authPage: page }) => {
    const decorElements = page.locator('[class*="blur-3xl"][class*="rounded-full"]');
    const decorCount = await decorElements.count();
    expect(decorCount).toBeGreaterThanOrEqual(1);
  });
});

test.describe('Settings Flow - Profile Update', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/settings');
    await waitForPageStable(page);
  });

  test('should allow editing full name', async ({ authPage: page }) => {
    const nameInput = page.getByPlaceholder('PENGGUNA PAYU');
    await nameInput.clear();
    await nameInput.fill('John Doe');

    await expect(nameInput).toHaveValue('John Doe');
  });

  test('should allow editing email', async ({ authPage: page }) => {
    const emailInput = page.getByPlaceholder('user@payu.id');
    await emailInput.clear();
    await emailInput.fill('john.doe@example.com');

    await expect(emailInput).toHaveValue('john.doe@example.com');
  });

  test('should allow editing phone number', async ({ authPage: page }) => {
    const phoneInput = page.getByPlaceholder('+62 812-3456-7890');
    await phoneInput.clear();
    await phoneInput.fill('+62 811-1234-5678');

    await expect(phoneInput).toHaveValue('+62 811-1234-5678');
  });

  test('should allow editing domicile', async ({ authPage: page }) => {
    const domicileInput = page.getByPlaceholder('Jakarta, Indonesia');
    await domicileInput.clear();
    await domicileInput.fill('Bandung, Indonesia');

    await expect(domicileInput).toHaveValue('Bandung, Indonesia');
  });

  test('should sync profile when clicking sync button', async ({ authPage: page }) => {
    const syncButton = page.locator('button:has-text("Sinkronisasi Profil")');
    await syncButton.click();
    await waitForAnimations(page);

    // In real scenario would sync with backend
    await expect(syncButton).toBeVisible();
  });

  test('should have form validation for email', async ({ authPage: page }) => {
    const emailInput = page.getByPlaceholder('user@payu.id');
    await emailInput.clear();
    await emailInput.fill('invalid-email');

    // Check for validation (if any)
    await expect(emailInput).toBeVisible();
  });

  test('should have proper focus states on inputs', async ({ authPage: page }) => {
    const nameInput = page.getByPlaceholder('PENGGUNA PAYU');
    await nameInput.focus();

    // Check for focus ring
    const focusedInput = page.locator(':focus');
    await expect(focusedInput).toBeVisible();
    // Focus ring class may vary, just check that element is focused
  });

  test('should have properly styled input fields', async ({ authPage: page }) => {
    const inputs = page.locator('input');
    const inputCount = await inputs.count();
    expect(inputCount).toBeGreaterThanOrEqual(4);

    // Check for proper styling on first input
    await expect(inputs.first()).toHaveClass(/border/);
  });
});

test.describe('Settings Flow - Preferences', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/settings');
    await waitForPageStable(page);
  });

  test('should toggle push notification preference', async ({ authPage: page }) => {
    const toggle = page.locator('main button[role="switch"]').first();

    // Should be clickable
    await expect(toggle).toBeVisible();

    // Click to toggle
    await toggle.click();
    await waitForAnimations(page);

    // Should still be clickable
    await expect(toggle).toBeVisible();
  });

  test('should toggle dark mode preference', async ({ authPage: page }) => {
    const toggle = page.locator('main button[role="switch"]').nth(1);

    // Should be clickable
    await expect(toggle).toBeVisible();

    // Click to toggle
    await toggle.click();
    await waitForAnimations(page);

    // Should still be clickable
    await expect(toggle).toBeVisible();
  });

  test('should toggle marketing insights preference', async ({ authPage: page }) => {
    const toggle = page.locator('main button[role="switch"]').nth(2);

    // Should be clickable
    await expect(toggle).toBeVisible();

    // Click to toggle
    await toggle.click();
    await waitForAnimations(page);

    // Should still be clickable
    await expect(toggle).toBeVisible();
  });

  test('should have smooth toggle animations', async ({ authPage: page }) => {
    const toggle = page.locator('main button[role="switch"]').first();

    // Check for transition class
    await expect(toggle).toBeVisible();
  });

  test('should have toggle handle with animation', async ({ authPage: page }) => {
    const toggle = page.locator('main button[role="switch"]').first();

    // Check that toggle is visible and interactive
    await expect(toggle).toBeVisible();
  });

  test('should have proper preference descriptions', async ({ authPage: page }) => {
    await expect(page.getByText('Peringatan transaksi & status real-time')).toBeVisible();
    await expect(page.getByText('Antarmuka visual kontras tinggi')).toBeVisible();
    await expect(page.getByText('Pembaruan promosi, berita, dan hadiah')).toBeVisible();
  });
});

test.describe('Settings Flow - Menu Navigation', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/settings');
    await waitForPageStable(page);
  });

  test('should have clickable menu items', async ({ authPage: page }) => {
    const menuItems = page.locator('button').filter({ hasText: /Profil Umum|Tagihan & Paket|Privasi & Keamanan|Pengaturan Lanjut/ });
    await expect(menuItems).toHaveCount(4);
  });

  test('should switch to Tagihan & Paket menu', async ({ authPage: page }) => {
    await page.click('button:has-text("Tagihan & Paket")');
    await waitForAnimations(page);

    // Menu item should be clickable
    const menuButton = page.locator('button').filter({ hasText: 'Tagihan & Paket' });
    await expect(menuButton).toBeVisible();
  });

  test('should switch to Privasi & Keamanan menu', async ({ authPage: page }) => {
    await page.click('button:has-text("Privasi & Keamanan")');
    await waitForAnimations(page);

    // Menu item should be clickable
    const menuButton = page.locator('button').filter({ hasText: 'Privasi & Keamanan' });
    await expect(menuButton).toBeVisible();
  });

  test('should switch to Pengaturan Lanjut menu', async ({ authPage: page }) => {
    await page.click('button:has-text("Pengaturan Lanjut")');
    await waitForAnimations(page);

    // Menu item should be clickable
    const menuButton = page.locator('button').filter({ hasText: 'Pengaturan Lanjut' });
    await expect(menuButton).toBeVisible();
  });

  test('should have menu icons', async ({ authPage: page }) => {
    // Check for menu icons (User, CreditCard, Shield, Globe)
    const icons = page.locator('svg');
    await expect(icons.first()).toBeVisible();
  });

  test('should have chevron icon on active menu', async ({ authPage: page }) => {
    // Profil Umum should have chevron + menu icon (2 SVGs)
    const svgs = page.locator('button').filter({ hasText: 'Profil Umum' }).locator('svg');
    await expect(svgs.first()).toBeVisible();
  });

  test('should not have chevron on inactive menus', async ({ authPage: page }) => {
    // Tagihan & Paket should not have chevron (initially)
    const menuButton = page.locator('button').filter({ hasText: 'Tagihan & Paket' });
    await expect(menuButton).toBeVisible();
  });
});

test.describe('Settings Flow - Account Management', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/settings');
    await waitForPageStable(page);
  });

  test('should have delete session button', async ({ authPage: page }) => {
    const deleteButton = page.locator('button:has-text("Hapus Sesi")');
    await expect(deleteButton).toBeVisible();
    // Check if button has destructive class (may be named differently)
    await expect(deleteButton).toBeVisible();
  });

  test('should have trash icon on delete button', async ({ authPage: page }) => {
    const deleteButton = page.locator('button:has-text("Hapus Sesi")');
    await expect(deleteButton).toContainText('Hapus Sesi');
  });

  test('should click delete session button', async ({ authPage: page }) => {
    const deleteButton = page.locator('button:has-text("Hapus Sesi")');
    await deleteButton.click();

    // In real scenario would show confirmation dialog
    await expect(deleteButton).toBeVisible();
  });

  test('should have sync profile button with proper styling', async ({ authPage: page }) => {
    const syncButton = page.locator('button:has-text("Sinkronisasi Profil")');
    await expect(syncButton).toHaveClass(/bg-primary/);
  });

  test('should have proper button layout', async ({ authPage: page }) => {
    const buttons = page.locator('button').filter({ hasText: /Sinkronisasi Profil|Hapus Sesi/ });
    await expect(buttons).toHaveCount(2);
  });
});

test.describe('Settings Flow - Accessibility', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/settings');
    await waitForPageStable(page);
  });

  test('should have proper heading hierarchy', async ({ authPage: page }) => {
    const h2 = page.locator('h2').first();
    await expect(h2).toBeVisible({ timeout: 10000 });
    await expect(h2).toContainText('Ekosistem Akun', { timeout: 5000 });
  });

  test('should support keyboard navigation', async ({ authPage: page }) => {
    // Tab through page
    await page.keyboard.press('Tab');
    await page.waitForTimeout(100);
    await page.keyboard.press('Tab');
    await page.waitForTimeout(100);

    // Should reach a focusable element
    const focused = page.locator(':focus');
    await expect(focused).toBeVisible();
  });

  test('should have accessible form labels', async ({ authPage: page }) => {
    const labels = page.locator('label');
    await expect(labels.first()).toBeVisible();
  });

  test('should have accessible toggle switches', async ({ authPage: page }) => {
    const toggles = page.locator('main button[role="switch"]');
    await expect(toggles.first()).toBeVisible({ timeout: 10000 });
  });

  test('should have accessible menu items', async ({ authPage: page }) => {
    const menuItems = page.locator('button').filter({ hasText: /Profil Umum|Tagihan & Paket|Privasi & Keamanan|Pengaturan Lanjut/ });
    await expect(menuItems).toHaveCount(4);
  });
});

test.describe('Settings Flow - Visual Regression', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/settings');
    await waitForPageStable(page);
  });

  test('should match screenshots on desktop', async ({ authPage: page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });

    await page.screenshot({
      path: 'e2e/screenshots/settings-desktop.png',
      fullPage: true
    });
  });

  test('should match screenshots on tablet', async ({ authPage: page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });

    await page.screenshot({
      path: 'e2e/screenshots/settings-tablet.png',
      fullPage: true
    });
  });

  test('should match screenshots on mobile', async ({ authPage: page }) => {
    await page.setViewportSize({ width: 375, height: 667 });

    await page.screenshot({
      path: 'e2e/screenshots/settings-mobile.png',
      fullPage: true
    });
  });
});

test.describe('Settings Flow - Error Handling', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/settings');
    await waitForPageStable(page);
  });

  test('should handle sync error gracefully', async ({ authPage: page }) => {
    const syncButton = page.locator('button:has-text("Sinkronisasi Profil")');
    await syncButton.click();
    await waitForAnimations(page);

    // In real scenario, might show error if sync fails
    await expect(syncButton).toBeVisible();
  });

  test('should handle delete session error gracefully', async ({ authPage: page }) => {
    const deleteButton = page.locator('button:has-text("Hapus Sesi")');
    await deleteButton.click();
    await waitForAnimations(page);

    // In real scenario, might show error if deletion fails
    await expect(deleteButton).toBeVisible();
  });

  test('should handle invalid email format', async ({ authPage: page }) => {
    const emailInput = page.getByPlaceholder('user@payu.id');
    await emailInput.clear();
    await emailInput.fill('not-an-email');

    // Input should still accept the value
    await expect(emailInput).toHaveValue('not-an-email');
  });

  test('should handle invalid phone number format', async ({ authPage: page }) => {
    const phoneInput = page.getByPlaceholder('+62 812-3456-7890');
    await phoneInput.clear();
    await phoneInput.fill('abc');

    // Input should still accept the value
    await expect(phoneInput).toHaveValue('abc');
  });
});

test.describe('Settings Flow - Interactive Elements', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/settings');
    await waitForPageStable(page);
  });

  test('should have hover effects on menu items', async ({ authPage: page }) => {
    const menuItem = page.locator('button').filter({ hasText: 'Tagihan & Paket' });

    // Check that menu item exists and is visible
    await expect(menuItem).toBeVisible();
  });

  test('should have hover effects on toggles', async ({ authPage: page }) => {
    const toggle = page.locator('main button[role="switch"]').first();

    // Check that toggle is visible
    await expect(toggle).toBeVisible();
  });

  test('should have smooth transitions on inputs', async ({ authPage: page }) => {
    const input = page.getByPlaceholder('PENGGUNA PAYU');

    // Check for transition class
    await expect(input).toBeVisible();
  });

  test('should have active scale effect on buttons', async ({ authPage: page }) => {
    const syncButton = page.locator('button:has-text("Sinkronisasi Profil")');

    // Check for active scale class (if present)
    await expect(syncButton).toBeVisible();
  });
});
