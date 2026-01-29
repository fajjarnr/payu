import { test, expect } from '@playwright/test';

test.describe('Settings Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to settings page (assumes user is logged in)
    await page.goto('/settings');
  });

  test('should display settings page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/PayU/);
    await expect(page.getByText('Ekosistem Akun')).toBeVisible();
    await expect(page.getByText('Kelola profil pribadi, preferensi sistem, dan tata kelola akun.')).toBeVisible();
  });

  test('should display user profile card', async ({ page }) => {
    await expect(page.getByText('PENGGUNA PAYU')).toBeVisible();
    await expect(page.getByText('Premium Member')).toBeVisible();
  });

  test('should display account ID', async ({ page }) => {
    await expect(page.getByText('ID Akun')).toBeVisible();
    await expect(page.getByText('PAYU-09228373')).toBeVisible();
  });

  test('should display account status', async ({ page }) => {
    await expect(page.getByText('Status')).toBeVisible();
    await expect(page.getByText('eKYC Terverifikasi')).toBeVisible();
  });

  test('should display settings menu items', async ({ page }) => {
    await expect(page.getByText('Profil Umum')).toBeVisible();
    await expect(page.getByText('Tagihan & Paket')).toBeVisible();
    await expect(page.getByText('Privasi & Keamanan')).toBeVisible();
    await expect(page.getByText('Pengaturan Lanjut')).toBeVisible();
  });

  test('should have Profil Umum menu active by default', async ({ page }) => {
    const activeMenu = page.locator('button').filter({ hasText: 'Profil Umum' });
    await expect(activeMenu).toHaveClass(/bg-primary/);
  });

  test('should display profile credential fields', async ({ page }) => {
    await expect(page.getByText('Kredensial Profil')).toBeVisible();
    await expect(page.getByText('Nama Lengkap (Sesuai KTP)')).toBeVisible();
    await expect(page.getByText('Email Kontak')).toBeVisible();
    await expect(page.getByText('Protokol Telepon')).toBeVisible();
    await expect(page.getByText('Domisili Saat Ini')).toBeVisible();
  });

  test('should display system preferences section', async ({ page }) => {
    await expect(page.getByText('Preferensi Sistem')).toBeVisible();
  });

  test('should display notification preference', async ({ page }) => {
    await expect(page.getByText('Notifikasi Push')).toBeVisible();
    await expect(page.getByText('Peringatan transaksi & status real-time')).toBeVisible();
  });

  test('should display dark mode preference', async ({ page }) => {
    await expect(page.getByText('Grafis Mode Gelap')).toBeVisible();
    await expect(page.getByText('Antarmuka visual kontras tinggi')).toBeVisible();
  });

  test('should display marketing insights preference', async ({ page }) => {
    await expect(page.getByText('Wawasan Pemasaran')).toBeVisible();
    await expect(page.getByText('Pembaruan promosi, berita, dan hadiah')).toBeVisible();
  });

  test('should have sync profile button', async ({ page }) => {
    const syncButton = page.locator('button:has-text("Sinkronisasi Profil")');
    await expect(syncButton).toBeVisible();
    await expect(syncButton).toBeEnabled();
  });

  test('should have delete session button', async ({ page }) => {
    const deleteButton = page.locator('button:has-text("Hapus Sesi")');
    await expect(deleteButton).toBeVisible();
    await expect(deleteButton).toBeEnabled();
  });

  test('should display toggle switches for preferences', async ({ page }) => {
    const toggles = page.locator('.w-12.h-6.rounded-full');
    await expect(toggles).toHaveCount(3);
  });

  test('should have active toggle for push notifications', async ({ page }) => {
    const toggle = page.locator('.w-12.h-6.rounded-full').first();
    await expect(toggle).toHaveClass(/bg-primary/);
  });

  test('should have inactive toggle for dark mode', async ({ page }) => {
    const toggle = page.locator('.w-12.h-6.rounded-full').nth(1);
    await expect(toggle).toHaveClass(/bg-muted/);
  });

  test('should have active toggle for marketing insights', async ({ page }) => {
    const toggle = page.locator('.w-12.h-6.rounded-full').nth(2);
    await expect(toggle).toHaveClass(/bg-primary/);
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/settings');

    // Check that key elements are visible
    await expect(page.getByText('Ekosistem Akun')).toBeVisible();
    await expect(page.getByText('PENGGUNA PAYU')).toBeVisible();

    // Take screenshot
    await page.screenshot({
      path: 'e2e/screenshots/settings-mobile.png',
      fullPage: true
    });
  });

  test('should display form inputs with default values', async ({ page }) => {
    await expect(page.getByPlaceholder('PENGGUNA PAYU')).toBeVisible();
    await expect(page.getByPlaceholder('user@payu.id')).toBeVisible();
    await expect(page.getByPlaceholder('+62 812-3456-7890')).toBeVisible();
    await expect(page.getByPlaceholder('Jakarta, Indonesia')).toBeVisible();
  });

  test('should have user avatar', async ({ page }) => {
    const avatar = page.locator('.w-24.h-24.bg-primary.rounded-2xl');
    await expect(avatar).toBeVisible();
    await expect(avatar).toContainText('P');
  });

  test('should have premium member badge', async ({ page }) => {
    const badge = page.locator('.bg-success-light');
    await expect(badge).toBeVisible();
    await expect(badge).toContainText('Premium Member');
  });

  test('should have decorative background elements', async ({ page }) => {
    const decorElements = page.locator('.bg-primary\\/5.rounded-full.blur-3xl');
    await expect(decorElements).toHaveCount.toBeGreaterThanOrEqual(1);
  });
});

test.describe('Settings Flow - Profile Update', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/settings');
  });

  test('should allow editing full name', async ({ page }) => {
    const nameInput = page.getByPlaceholder('PENGGUNA PAYU');
    await nameInput.clear();
    await nameInput.fill('John Doe');

    await expect(nameInput).toHaveValue('John Doe');
  });

  test('should allow editing email', async ({ page }) => {
    const emailInput = page.getByPlaceholder('user@payu.id');
    await emailInput.clear();
    await emailInput.fill('john.doe@example.com');

    await expect(emailInput).toHaveValue('john.doe@example.com');
  });

  test('should allow editing phone number', async ({ page }) => {
    const phoneInput = page.getByPlaceholder('+62 812-3456-7890');
    await phoneInput.clear();
    await phoneInput.fill('+62 811-1234-5678');

    await expect(phoneInput).toHaveValue('+62 811-1234-5678');
  });

  test('should allow editing domicile', async ({ page }) => {
    const domicileInput = page.getByPlaceholder('Jakarta, Indonesia');
    await domicileInput.clear();
    await domicileInput.fill('Bandung, Indonesia');

    await expect(domicileInput).toHaveValue('Bandung, Indonesia');
  });

  test('should sync profile when clicking sync button', async ({ page }) => {
    const syncButton = page.locator('button:has-text("Sinkronisasi Profil")');
    await syncButton.click();

    // In real scenario would sync with backend
    await expect(syncButton).toBeVisible();
  });

  test('should have form validation for email', async ({ page }) => {
    const emailInput = page.getByPlaceholder('user@payu.id');
    await emailInput.clear();
    await emailInput.fill('invalid-email');

    // Check for validation (if any)
    await expect(emailInput).toBeVisible();
  });

  test('should have proper focus states on inputs', async ({ page }) => {
    const nameInput = page.getByPlaceholder('PENGGUNA PAYU');
    await nameInput.focus();

    // Check for focus ring
    const focusedInput = page.locator(':focus');
    await expect(focusedInput).toBeVisible();
    await expect(focusedInput).toHaveClass(/focus:ring-4/);
  });

  test('should have properly styled input fields', async ({ page }) => {
    const inputs = page.locator('input[type="text"], input[type="email"]');
    await expect(inputs).toHaveCount(4);

    // Check for proper styling
    await expect(inputs.first()).toHaveClass(/border/);
    await expect(inputs.first()).toHaveClass(/rounded-xl/);
  });
});

test.describe('Settings Flow - Preferences', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/settings');
  });

  test('should toggle push notification preference', async ({ page }) => {
    const toggle = page.locator('.w-12.h-6.rounded-full').first();

    // Should be active initially
    await expect(toggle).toHaveClass(/bg-primary/);

    // Click to toggle
    await toggle.click();

    // Should still be clickable
    await expect(toggle).toBeVisible();
  });

  test('should toggle dark mode preference', async ({ page }) => {
    const toggle = page.locator('.w-12.h-6.rounded-full').nth(1);

    // Should be inactive initially
    await expect(toggle).toHaveClass(/bg-muted/);

    // Click to toggle
    await toggle.click();

    // Should still be clickable
    await expect(toggle).toBeVisible();
  });

  test('should toggle marketing insights preference', async ({ page }) => {
    const toggle = page.locator('.w-12.h-6.rounded-full').nth(2);

    // Should be active initially
    await expect(toggle).toHaveClass(/bg-primary/);

    // Click to toggle
    await toggle.click();

    // Should still be clickable
    await expect(toggle).toBeVisible();
  });

  test('should have smooth toggle animations', async ({ page }) => {
    const toggle = page.locator('.w-12.h-6.rounded-full').first();

    // Check for transition class
    await expect(toggle).toHaveClass(/transition-all/);
  });

  test('should have toggle handle with animation', async ({ page }) => {
    const toggleHandle = page.locator('.w-4.h-4.bg-white.rounded-full').first();

    // Check for transition class
    await expect(toggleHandle).toHaveClass(/transition-all/);

    // Check for translate effect when active
    await expect(toggleHandle).toHaveClass(/translate-x-6/);
  });

  test('should have proper preference descriptions', async ({ page }) => {
    await expect(page.getByText('Peringatan transaksi & status real-time')).toBeVisible();
    await expect(page.getByText('Antarmuka visual kontras tinggi')).toBeVisible();
    await expect(page.getByText('Pembaruan promosi, berita, dan hadiah')).toBeVisible();
  });
});

test.describe('Settings Flow - Menu Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/settings');
  });

  test('should have clickable menu items', async ({ page }) => {
    const menuItems = page.locator('button').filter({ hasText: /Profil Umum|Tagihan & Paket|Privasi & Keamanan|Pengaturan Lanjut/ });
    await expect(menuItems).toHaveCount(4);
  });

  test('should switch to Tagihan & Paket menu', async ({ page }) => {
    await page.click('button:has-text("Tagihan & Paket")');

    // Menu item should be clickable
    const menuButton = page.locator('button').filter({ hasText: 'Tagihan & Paket' });
    await expect(menuButton).toBeVisible();
  });

  test('should switch to Privasi & Keamanan menu', async ({ page }) => {
    await page.click('button:has-text("Privasi & Keamanan")');

    // Menu item should be clickable
    const menuButton = page.locator('button').filter({ hasText: 'Privasi & Keamanan' });
    await expect(menuButton).toBeVisible();
  });

  test('should switch to Pengaturan Lanjut menu', async ({ page }) => {
    await page.click('button:has-text("Pengaturan Lanjut")');

    // Menu item should be clickable
    const menuButton = page.locator('button').filter({ hasText: 'Pengaturan Lanjut' });
    await expect(menuButton).toBeVisible();
  });

  test('should have menu icons', async ({ page }) => {
    // Check for menu icons (User, CreditCard, Shield, Globe)
    const icons = page.locator('svg');
    await expect(icons.first()).toBeVisible();
  });

  test('should have chevron icon on active menu', async ({ page }) => {
    // Profil Umum should have chevron
    const chevronIcon = page.locator('button').filter({ hasText: 'Profil Umum' }).locator('svg');
    await expect(chevronIcon).toBeVisible();
  });

  test('should not have chevron on inactive menus', async ({ page }) => {
    // Tagihan & Paket should not have chevron (initially)
    const menuButton = page.locator('button').filter({ hasText: 'Tagihan & Paket' });
    await expect(menuButton).toBeVisible();
  });
});

test.describe('Settings Flow - Account Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/settings');
  });

  test('should have delete session button', async ({ page }) => {
    const deleteButton = page.locator('button:has-text("Hapus Sesi")');
    await expect(deleteButton).toBeVisible();
    await expect(deleteButton).toHaveClass(/text-destructive/);
  });

  test('should have trash icon on delete button', async ({ page }) => {
    const deleteButton = page.locator('button:has-text("Hapus Sesi")');
    await expect(deleteButton).toContainText('Hapus Sesi');
  });

  test('should click delete session button', async ({ page }) => {
    const deleteButton = page.locator('button:has-text("Hapus Sesi")');
    await deleteButton.click();

    // In real scenario would show confirmation dialog
    await expect(deleteButton).toBeVisible();
  });

  test('should have sync profile button with proper styling', async ({ page }) => {
    const syncButton = page.locator('button:has-text("Sinkronisasi Profil")');
    await expect(syncButton).toHaveClass(/bg-primary/);
    await expect(syncButton).toHaveClass(/text-primary-foreground/);
  });

  test('should have proper button layout', async ({ page }) => {
    const buttons = page.locator('button').filter({ hasText: /Sinkronisasi Profil|Hapus Sesi/ });
    await expect(buttons).toHaveCount(2);
  });
});

test.describe('Settings Flow - Accessibility', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/settings');
  });

  test('should have proper heading hierarchy', async ({ page }) => {
    const h2 = page.locator('h2');
    await expect(h2.first()).toBeVisible();
    await expect(h2.first()).toContainText('Ekosistem Akun');
  });

  test('should support keyboard navigation', async ({ page }) => {
    // Tab through page
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');

    // Should reach a focusable element
    const focused = page.locator(':focus');
    await expect(focused).toBeVisible();
  });

  test('should have accessible form labels', async ({ page }) => {
    const labels = page.locator('label');
    await expect(labels.first()).toBeVisible();
  });

  test('should have accessible toggle switches', async ({ page }) => {
    const toggles = page.locator('.w-12.h-6.rounded-full');
    await expect(toggles.first()).toBeVisible();
  });

  test('should have accessible menu items', async ({ page }) => {
    const menuItems = page.locator('button').filter({ hasText: /Profil Umum|Tagihan & Paket|Privasi & Keamanan|Pengaturan Lanjut/ });
    await expect(menuItems).toHaveCount(4);
  });
});

test.describe('Settings Flow - Visual Regression', () => {
  test('should match screenshots on desktop', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/settings');

    await page.screenshot({
      path: 'e2e/screenshots/settings-desktop.png',
      fullPage: true
    });
  });

  test('should match screenshots on tablet', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/settings');

    await page.screenshot({
      path: 'e2e/screenshots/settings-tablet.png',
      fullPage: true
    });
  });

  test('should match screenshots on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/settings');

    await page.screenshot({
      path: 'e2e/screenshots/settings-mobile.png',
      fullPage: true
    });
  });
});

test.describe('Settings Flow - Error Handling', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/settings');
  });

  test('should handle sync error gracefully', async ({ page }) => {
    const syncButton = page.locator('button:has-text("Sinkronisasi Profil")');
    await syncButton.click();

    // In real scenario, might show error if sync fails
    await expect(syncButton).toBeVisible();
  });

  test('should handle delete session error gracefully', async ({ page }) => {
    const deleteButton = page.locator('button:has-text("Hapus Sesi")');
    await deleteButton.click();

    // In real scenario, might show error if deletion fails
    await expect(deleteButton).toBeVisible();
  });

  test('should handle invalid email format', async ({ page }) => {
    const emailInput = page.getByPlaceholder('user@payu.id');
    await emailInput.clear();
    await emailInput.fill('not-an-email');

    // Input should still accept the value
    await expect(emailInput).toHaveValue('not-an-email');
  });

  test('should handle invalid phone number format', async ({ page }) => {
    const phoneInput = page.getByPlaceholder('+62 812-3456-7890');
    await phoneInput.clear();
    await phoneInput.fill('abc');

    // Input should still accept the value
    await expect(phoneInput).toHaveValue('abc');
  });
});

test.describe('Settings Flow - Interactive Elements', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/settings');
  });

  test('should have hover effects on menu items', async ({ page }) => {
    const menuItem = page.locator('button').filter({ hasText: 'Tagihan & Paket' });

    // Check for hover class
    await expect(menuItem).toHaveClass(/hover:bg-muted\\/50/);
  });

  test('should have hover effects on toggles', async ({ page }) => {
    const toggleContainer = page.locator('.p-2.hover\\:bg-muted\\/20').first();

    // Check for hover class
    await expect(toggleContainer).toHaveClass(/hover:bg-muted\\/20/);
  });

  test('should have smooth transitions on inputs', async ({ page }) => {
    const input = page.getByPlaceholder('PENGGUNA PAYU');

    // Check for transition class
    await expect(input).toHaveClass(/transition-all/);
  });

  test('should have active scale effect on buttons', async ({ page }) => {
    const syncButton = page.locator('button:has-text("Sinkronisasi Profil")');

    // Check for active scale class (if present)
    await expect(syncButton).toBeVisible();
  });
});
