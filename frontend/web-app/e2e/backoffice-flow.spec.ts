/**
 * E2E Tests: Backoffice Pages (/backoffice/*)
 * Covers BUG-TEST-099 through BUG-TEST-109 (11 backoffice pages)
 */
import { test, expect } from './fixtures';
import { waitForPageStable } from './utils';

// ============================================================
// BUG-TEST-099: /backoffice dashboard (Command Center)
// ============================================================
test.describe('Backoffice Dashboard', () => {
  test('should display command center heading', async ({ authPage: page }) => {
    await page.goto('/backoffice');
    await waitForPageStable(page);
    await expect(page.locator('main').getByText('Command Center')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Sistem orkestrasi internal PayU Digital Banking.')).toBeVisible();
  });

  test('should display stat cards', async ({ authPage: page }) => {
    await page.goto('/backoffice');
    await waitForPageStable(page);
    await expect(page.locator('main').getByText('Total Customers')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Active Sessions')).toBeVisible();
    await expect(page.locator('main').getByText('Security Alerts')).toBeVisible();
  });

  test('should display quick link cards', async ({ authPage: page }) => {
    await page.goto('/backoffice');
    await waitForPageStable(page);
    await expect(page.locator('main').getByText('KYC Reviews')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Fraud Monitoring')).toBeVisible();
    await expect(page.locator('main').getByText('Customer Ops')).toBeVisible();
    await expect(page.locator('main').getByText('CMS Content')).toBeVisible();
    await expect(page.locator('main').getByText('A/B Testing')).toBeVisible();
    await expect(page.locator('main').getByText('Audit Logs')).toBeVisible();
  });

  test('should navigate to KYC page from quick link', async ({ authPage: page }) => {
    await page.goto('/backoffice');
    await waitForPageStable(page);
    await page.locator('main').getByText('KYC Reviews').click();
    await expect(page).toHaveURL(/.*\/backoffice\/kyc/);
  });
});

// ============================================================
// BUG-TEST-100: /backoffice/kyc
// ============================================================
test.describe('Backoffice KYC Reviews', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/backoffice/kyc');
    await waitForPageStable(page);
  });

  test('should display KYC reviews page', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('KYC Reviews')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Review verifikasi identitas dan dokumen nasabah.')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Cari nasabah atau nomor dokumen...')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.locator('main').getByRole('columnheader', { name: 'Nasabah' })).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByRole('columnheader', { name: 'Dokumen' })).toBeVisible();
    await expect(page.locator('main').getByRole('columnheader', { name: 'Status' })).toBeVisible();
    await expect(page.locator('main').getByRole('columnheader', { name: 'Aksi' })).toBeVisible();
  });

  test('should display data or loading state', async ({ authPage: page }) => {
    const hasData = await page.locator('main').getByText('Review').nth(1).isVisible().catch(() => false);
    const isLoading = await page.locator('main').getByText('Memuat data...').isVisible().catch(() => false);
    const isEmpty = await page.locator('main').getByText('Tidak ada review ditemukan').isVisible().catch(() => false);
    expect(hasData || isLoading || isEmpty).toBeTruthy();
  });

  test('should display pagination controls', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Sebelumnya')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Selanjutnya')).toBeVisible();
  });
});

// ============================================================
// BUG-TEST-101: /backoffice/fraud
// ============================================================
test.describe('Backoffice Fraud Monitoring', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/backoffice/fraud');
    await waitForPageStable(page);
  });

  test('should display fraud monitoring page', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Fraud Monitoring')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Sistem deteksi risiko dan investigasi kecurangan transaksi.')).toBeVisible();
  });

  test('should display search and filters', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Cari kasus...')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Semua Risiko')).toBeVisible();
    await expect(page.locator('main').getByText('Semua Status')).toBeVisible();
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Risiko')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Tipe Kecurangan')).toBeVisible();
    await expect(page.locator('main').getByText('Jumlah')).toBeVisible();
  });

  test('should display data or loading state', async ({ authPage: page }) => {
    const hasData = await page.locator('main').getByText('Detail').first().isVisible().catch(() => false);
    const isLoading = await page.locator('main').getByText('Memuat data...').isVisible().catch(() => false);
    const isEmpty = await page.locator('main').getByText('Tidak ada kasus ditemukan').isVisible().catch(() => false);
    expect(hasData || isLoading || isEmpty).toBeTruthy();
  });
});

// ============================================================
// BUG-TEST-102: /backoffice/partners
// ============================================================
test.describe('Backoffice Partner Management', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/backoffice/partners');
    await waitForPageStable(page);
  });

  test('should display partner management stat cards', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Total Partners')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Active Merchants')).toBeVisible();
    await expect(page.locator('main').getByText('Pending Apps')).toBeVisible();
    await expect(page.locator('main').getByText('SNAP BI Volume')).toBeVisible();
  });

  test('should display action buttons', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Manage API Keys')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Register New Partner')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Search partners by name or ID...')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Partner Org')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Type')).toBeVisible();
    await expect(page.locator('main').getByText('API Integration')).toBeVisible();
  });
});

// ============================================================
// BUG-TEST-103: /backoffice/customers
// ============================================================
test.describe('Backoffice Customer Operations', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/backoffice/customers');
    await waitForPageStable(page);
  });

  test('should display customer operations page', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Customer Operations')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Kelola tiket dukungan, keluhan, dan bantuan nasabah.')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Cari tiket atau ID nasabah...')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('No. Tiket')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Prioritas')).toBeVisible();
  });

  test('should display data or loading state', async ({ authPage: page }) => {
    const hasData = await page.locator('main').getByText('Buka').first().isVisible().catch(() => false);
    const isLoading = await page.locator('main').getByText('Memuat data...').isVisible().catch(() => false);
    const isEmpty = await page.locator('main').getByText('Tidak ada tiket ditemukan').isVisible().catch(() => false);
    expect(hasData || isLoading || isEmpty).toBeTruthy();
  });
});

// ============================================================
// BUG-TEST-104: /backoffice/compliance
// ============================================================
test.describe('Backoffice Compliance / Audit Logs', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/backoffice/compliance');
    await waitForPageStable(page);
  });

  test('should display compliance stat cards', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Security Score')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('High Risk Events')).toBeVisible();
    await expect(page.locator('main').getByText('Regulatory Status')).toBeVisible();
  });

  test('should display action buttons', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Export Audit Report')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('More Filters')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Filter by User, IP, or Resource...')).toBeVisible({ timeout: 10000 });
  });

  test('should display audit table headers', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Event ID')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Event Type')).toBeVisible();
    await expect(page.locator('main').getByText('Risk Level')).toBeVisible();
  });

  test('should display real-time status footer', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Real-time Audit Stream Active')).toBeVisible({ timeout: 10000 });
  });
});

// ============================================================
// BUG-TEST-105: /backoffice/cms
// ============================================================
test.describe('Backoffice CMS Content', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/backoffice/cms');
    await waitForPageStable(page);
  });

  test('should display CMS stat cards', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Total Content')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Active Now')).toBeVisible();
    await expect(page.locator('main').getByText('Scheduled')).toBeVisible();
    await expect(page.locator('main').getByText('Pending Review')).toBeVisible();
  });

  test('should display content type filter tabs', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('ALL')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('BANNER')).toBeVisible();
    await expect(page.locator('main').getByText('PROMO')).toBeVisible();
    await expect(page.locator('main').getByText('ALERT')).toBeVisible();
    await expect(page.locator('main').getByText('POPUP')).toBeVisible();
  });

  test('should display new content button', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('New Content')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Content')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Priority')).toBeVisible();
    await expect(page.locator('main').getByText('Actions')).toBeVisible();
  });
});

// ============================================================
// BUG-TEST-106: /backoffice/campaigns
// ============================================================
test.describe('Backoffice Campaign Management', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/backoffice/campaigns');
    await waitForPageStable(page);
  });

  test('should display campaign stat cards', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Total Budget')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Total Rewards Sent')).toBeVisible();
    await expect(page.locator('main').getByText('Active Campaigns')).toBeVisible();
    await expect(page.locator('main').getByText('Conversion Lift')).toBeVisible();
  });

  test('should display action buttons', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Performance Report')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Launch New Campaign')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Search campaigns...')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Campaign Name')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Rewards Sent')).toBeVisible();
    await expect(page.locator('main').getByText('Budget Spent')).toBeVisible();
  });
});

// ============================================================
// BUG-TEST-107: /backoffice/broadcast
// ============================================================
test.describe('Backoffice Broadcast Messaging', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/backoffice/broadcast');
    await waitForPageStable(page);
  });

  test('should display broadcast stat cards', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Broadcasts Sent')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Total Messages')).toBeVisible();
    await expect(page.locator('main').getByText('Avg Open Rate')).toBeVisible();
    await expect(page.locator('main').getByText('Unsubscribe Rate')).toBeVisible();
  });

  test('should display action buttons', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Targeting Rules')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Create Broadcast')).toBeVisible();
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Broadcast Title')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Channels')).toBeVisible();
    await expect(page.locator('main').getByText('Engagement')).toBeVisible();
  });

  test('should display footer status', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Multi-channel Delivery Engine Active')).toBeVisible({ timeout: 10000 });
  });
});

// ============================================================
// BUG-TEST-108: /backoffice/fx-rates
// ============================================================
test.describe('Backoffice FX Rates Admin', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/backoffice/fx-rates');
    await waitForPageStable(page);
  });

  test('should display FX rates stat cards', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Active Currencies')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Auto-Sync Provider')).toBeVisible();
    await expect(page.locator('main').getByText('Manual Overrides')).toBeVisible();
    await expect(page.locator('main').getByText('Market Status')).toBeVisible();
  });

  test('should display action buttons', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Rate History')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Sync All Rates')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Search currency pairs...')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Currency Pair')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Current Rate')).toBeVisible();
    await expect(page.locator('main').getByText('Spread (%)')).toBeVisible();
    await expect(page.locator('main').getByText('Sync Mode')).toBeVisible();
  });

  test('should display market connector status', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Market Connector Status:')).toBeVisible({ timeout: 10000 });
  });
});

// ============================================================
// BUG-TEST-109: /backoffice/ab-testing
// ============================================================
test.describe('Backoffice A/B Testing', () => {
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/backoffice/ab-testing');
    await waitForPageStable(page);
  });

  test('should display A/B testing stat cards', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Active Experiments')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Total Variations')).toBeVisible();
    await expect(page.locator('main').getByText('Users Exposed')).toBeVisible();
    await expect(page.locator('main').getByText('Winning Variations')).toBeVisible();
  });

  test('should display filter tabs', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('ALL')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('ACTIVE')).toBeVisible();
    await expect(page.locator('main').getByText('COMPLETED')).toBeVisible();
    await expect(page.locator('main').getByText('DRAFT')).toBeVisible();
  });

  test('should display create experiment button', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Create Experiment')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.locator('main').getByText('Experiment')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('main').getByText('Traffic')).toBeVisible();
    await expect(page.locator('main').getByText('Audience')).toBeVisible();
    await expect(page.locator('main').getByText('Start Date')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Search experiments...')).toBeVisible({ timeout: 10000 });
  });
});
