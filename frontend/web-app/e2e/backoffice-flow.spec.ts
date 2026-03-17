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
    await expect(page.getByText('Command Center')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Sistem orkestrasi internal PayU Digital Banking.')).toBeVisible();
  });

  test('should display stat cards', async ({ authPage: page }) => {
    await page.goto('/backoffice');
    await waitForPageStable(page);
    await expect(page.getByText('Total Customers')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Active Sessions')).toBeVisible();
    await expect(page.getByText('Security Alerts')).toBeVisible();
  });

  test('should display quick link cards', async ({ authPage: page }) => {
    await page.goto('/backoffice');
    await waitForPageStable(page);
    await expect(page.getByText('KYC Reviews')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Fraud Monitoring')).toBeVisible();
    await expect(page.getByText('Customer Ops')).toBeVisible();
    await expect(page.getByText('CMS Content')).toBeVisible();
    await expect(page.getByText('A/B Testing')).toBeVisible();
    await expect(page.getByText('Audit Logs')).toBeVisible();
  });

  test('should navigate to KYC page from quick link', async ({ authPage: page }) => {
    await page.goto('/backoffice');
    await waitForPageStable(page);
    await page.getByText('KYC Reviews').click();
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
    await expect(page.getByText('KYC Reviews')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Review verifikasi identitas dan dokumen nasabah.')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Cari nasabah atau nomor dokumen...')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.getByText('Nasabah')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Dokumen')).toBeVisible();
    await expect(page.getByText('Status')).toBeVisible();
    await expect(page.getByText('Aksi')).toBeVisible();
  });

  test('should display data or loading state', async ({ authPage: page }) => {
    const hasData = await page.getByText('Review').nth(1).isVisible().catch(() => false);
    const isLoading = await page.getByText('Memuat data...').isVisible().catch(() => false);
    const isEmpty = await page.getByText('Tidak ada review ditemukan').isVisible().catch(() => false);
    expect(hasData || isLoading || isEmpty).toBeTruthy();
  });

  test('should display pagination controls', async ({ authPage: page }) => {
    await expect(page.getByText('Sebelumnya')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Selanjutnya')).toBeVisible();
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
    await expect(page.getByText('Fraud Monitoring')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Sistem deteksi risiko dan investigasi kecurangan transaksi.')).toBeVisible();
  });

  test('should display search and filters', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Cari kasus...')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Semua Risiko')).toBeVisible();
    await expect(page.getByText('Semua Status')).toBeVisible();
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.getByText('Risiko')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Tipe Kecurangan')).toBeVisible();
    await expect(page.getByText('Jumlah')).toBeVisible();
  });

  test('should display data or loading state', async ({ authPage: page }) => {
    const hasData = await page.getByText('Detail').first().isVisible().catch(() => false);
    const isLoading = await page.getByText('Memuat data...').isVisible().catch(() => false);
    const isEmpty = await page.getByText('Tidak ada kasus ditemukan').isVisible().catch(() => false);
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
    await expect(page.getByText('Total Partners')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Active Merchants')).toBeVisible();
    await expect(page.getByText('Pending Apps')).toBeVisible();
    await expect(page.getByText('SNAP BI Volume')).toBeVisible();
  });

  test('should display action buttons', async ({ authPage: page }) => {
    await expect(page.getByText('Manage API Keys')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Register New Partner')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Search partners by name or ID...')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.getByText('Partner Org')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Type')).toBeVisible();
    await expect(page.getByText('API Integration')).toBeVisible();
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
    await expect(page.getByText('Customer Operations')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Kelola tiket dukungan, keluhan, dan bantuan nasabah.')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Cari tiket atau ID nasabah...')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.getByText('No. Tiket')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Prioritas')).toBeVisible();
  });

  test('should display data or loading state', async ({ authPage: page }) => {
    const hasData = await page.getByText('Buka').first().isVisible().catch(() => false);
    const isLoading = await page.getByText('Memuat data...').isVisible().catch(() => false);
    const isEmpty = await page.getByText('Tidak ada tiket ditemukan').isVisible().catch(() => false);
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
    await expect(page.getByText('Security Score')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('High Risk Events')).toBeVisible();
    await expect(page.getByText('Regulatory Status')).toBeVisible();
  });

  test('should display action buttons', async ({ authPage: page }) => {
    await expect(page.getByText('Export Audit Report')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('More Filters')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Filter by User, IP, or Resource...')).toBeVisible({ timeout: 10000 });
  });

  test('should display audit table headers', async ({ authPage: page }) => {
    await expect(page.getByText('Event ID')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Event Type')).toBeVisible();
    await expect(page.getByText('Risk Level')).toBeVisible();
  });

  test('should display real-time status footer', async ({ authPage: page }) => {
    await expect(page.getByText('Real-time Audit Stream Active')).toBeVisible({ timeout: 10000 });
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
    await expect(page.getByText('Total Content')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Active Now')).toBeVisible();
    await expect(page.getByText('Scheduled')).toBeVisible();
    await expect(page.getByText('Pending Review')).toBeVisible();
  });

  test('should display content type filter tabs', async ({ authPage: page }) => {
    await expect(page.getByText('ALL')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('BANNER')).toBeVisible();
    await expect(page.getByText('PROMO')).toBeVisible();
    await expect(page.getByText('ALERT')).toBeVisible();
    await expect(page.getByText('POPUP')).toBeVisible();
  });

  test('should display new content button', async ({ authPage: page }) => {
    await expect(page.getByText('New Content')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.getByText('Content')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Priority')).toBeVisible();
    await expect(page.getByText('Actions')).toBeVisible();
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
    await expect(page.getByText('Total Budget')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Total Rewards Sent')).toBeVisible();
    await expect(page.getByText('Active Campaigns')).toBeVisible();
    await expect(page.getByText('Conversion Lift')).toBeVisible();
  });

  test('should display action buttons', async ({ authPage: page }) => {
    await expect(page.getByText('Performance Report')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Launch New Campaign')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Search campaigns...')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.getByText('Campaign Name')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Rewards Sent')).toBeVisible();
    await expect(page.getByText('Budget Spent')).toBeVisible();
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
    await expect(page.getByText('Broadcasts Sent')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Total Messages')).toBeVisible();
    await expect(page.getByText('Avg Open Rate')).toBeVisible();
    await expect(page.getByText('Unsubscribe Rate')).toBeVisible();
  });

  test('should display action buttons', async ({ authPage: page }) => {
    await expect(page.getByText('Targeting Rules')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Create Broadcast')).toBeVisible();
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.getByText('Broadcast Title')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Channels')).toBeVisible();
    await expect(page.getByText('Engagement')).toBeVisible();
  });

  test('should display footer status', async ({ authPage: page }) => {
    await expect(page.getByText('Multi-channel Delivery Engine Active')).toBeVisible({ timeout: 10000 });
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
    await expect(page.getByText('Active Currencies')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Auto-Sync Provider')).toBeVisible();
    await expect(page.getByText('Manual Overrides')).toBeVisible();
    await expect(page.getByText('Market Status')).toBeVisible();
  });

  test('should display action buttons', async ({ authPage: page }) => {
    await expect(page.getByText('Rate History')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Sync All Rates')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Search currency pairs...')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.getByText('Currency Pair')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Current Rate')).toBeVisible();
    await expect(page.getByText('Spread (%)')).toBeVisible();
    await expect(page.getByText('Sync Mode')).toBeVisible();
  });

  test('should display market connector status', async ({ authPage: page }) => {
    await expect(page.getByText('Market Connector Status:')).toBeVisible({ timeout: 10000 });
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
    await expect(page.getByText('Active Experiments')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Total Variations')).toBeVisible();
    await expect(page.getByText('Users Exposed')).toBeVisible();
    await expect(page.getByText('Winning Variations')).toBeVisible();
  });

  test('should display filter tabs', async ({ authPage: page }) => {
    await expect(page.getByText('ALL')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('ACTIVE')).toBeVisible();
    await expect(page.getByText('COMPLETED')).toBeVisible();
    await expect(page.getByText('DRAFT')).toBeVisible();
  });

  test('should display create experiment button', async ({ authPage: page }) => {
    await expect(page.getByText('Create Experiment')).toBeVisible({ timeout: 10000 });
  });

  test('should display table headers', async ({ authPage: page }) => {
    await expect(page.getByText('Experiment')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Traffic')).toBeVisible();
    await expect(page.getByText('Audience')).toBeVisible();
    await expect(page.getByText('Start Date')).toBeVisible();
  });

  test('should display search input', async ({ authPage: page }) => {
    await expect(page.getByPlaceholder('Search experiments...')).toBeVisible({ timeout: 10000 });
  });
});
