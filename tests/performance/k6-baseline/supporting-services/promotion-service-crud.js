// PayU Promotion Service - CRUD Baseline Performance Test
// ==========================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const promotionMetrics = {
  createCampaignDuration: new Trend('promo_create_campaign_duration'),
  getCampaignsDuration: new Trend('promo_get_campaigns_duration'),
  getCampaignDetailDuration: new Trend('promo_get_campaign_detail_duration'),
  updateCampaignDuration: new Trend('promo_update_campaign_duration'),
  activateCampaignDuration: new Trend('promo_activate_duration'),
  createVoucherDuration: new Trend('promo_create_voucher_duration'),
  redeemVoucherDuration: new Trend('promo_redeem_voucher_duration'),
  validatePromoDuration: new Trend('promo_validate_duration'),
  getUserPromosDuration: new Trend('promo_user_promos_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'promotion-service',
    testType: 'baseline-crud'
  }
};

// Promotion types
const PROMO_TYPES = ['DISCOUNT', 'CASHBACK', 'POINTS', 'FREE_SHIPPING', 'BUNDLE'];
const DISCOUNT_TYPES = ['PERCENTAGE', 'FIXED_AMOUNT', 'TIERED'];

// Test data generators
function generateCampaignData(uniqueId) {
  const now = new Date();
  const endDate = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);

  return {
    name: `Campaign ${uniqueId}`,
    description: `Test promotion campaign ${uniqueId}`,
    type: PROMO_TYPES[Math.floor(Math.random() * PROMO_TYPES.length)],
    discountType: DISCOUNT_TYPES[Math.floor(Math.random() * DISCOUNT_TYPES.length)],
    discountValue: Math.floor(10 + Math.random() * 40),
    maxDiscount: Math.floor(25000 + Math.random() * 75000),
    minTransaction: Math.floor(50000 + Math.random() * 450000),
    startDate: now.toISOString(),
    endDate: endDate.toISOString(),
    totalQuota: Math.floor(100 + Math.random() * 900),
    maxUsagePerUser: Math.floor(1 + Math.random() * 5)
  };
}

function generateVoucherData(uniqueId) {
  return {
    code: `VOUCHER${uniqueId}`,
    description: `Test voucher ${uniqueId}`,
    type: 'SINGLE_USE',
    discountAmount: Math.floor(10000 + Math.random() * 40000),
    minTransaction: Math.floor(50000 + Math.random() * 100000),
    expiryDays: Math.floor(7 + Math.random() * 23)
  };
}

function generateRedemptionData(code) {
  return {
    voucherCode: code,
    transactionAmount: Math.floor(100000 + Math.random() * 400000),
    merchantId: `MERCH${Math.floor(1000 + Math.random() * 9000)}`
  };
}

// Main test scenario
export default function () {
  const uniqueId = `${__VU}-${__ITER}-${Date.now()}`;

  const auth = login(__VU % 5);
  if (!auth || !auth.token) {
    console.error('Login failed, skipping test');
    return;
  }

  let campaignId = null;
  let voucherCode = null;

  group('Promotion Service - CRUD Operations', () => {

    // ===== CREATE: Create Campaign =====
    group('CREATE: Create Campaign', () => {
      const campaignData = generateCampaignData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.promotion}/campaigns`, campaignData, auth.token);
      promotionMetrics.createCampaignDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        campaignId = result.body.campaignId || result.body.id;
        console.log(`Campaign created: ${campaignId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Campaigns =====
    group('READ: List Campaigns', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.promotion}/campaigns`, { status: 'ACTIVE' }, auth.token);
      promotionMetrics.getCampaignsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (campaignId) {
      // ===== READ: Get Campaign Detail =====
      group('READ: Get Campaign Detail', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.promotion}/campaigns/${campaignId}`, auth.token);
        promotionMetrics.getCampaignDetailDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Update Campaign =====
      group('UPDATE: Update Campaign', () => {
        const updateData = {
          name: `Updated Campaign ${Date.now()}`,
          totalQuota: Math.floor(500 + Math.random() * 500)
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.promotion}/campaigns/${campaignId}`, updateData, auth.token);
        promotionMetrics.updateCampaignDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Activate Campaign =====
      group('UPDATE: Activate Campaign', () => {
        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.promotion}/campaigns/${campaignId}/activate`, {}, auth.token);
        promotionMetrics.activateCampaignDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

    // ===== CREATE: Create Voucher =====
    group('CREATE: Create Voucher', () => {
      const voucherData = generateVoucherData(uniqueId.substring(0, 8));
      voucherCode = voucherData.code;

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.promotion}/vouchers`, voucherData, auth.token);
      promotionMetrics.createVoucherDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

    // ===== READ: Get User Promotions =====
    group('READ: Get User Promos', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.promotion}/my-promos`, {}, auth.token);
      promotionMetrics.getUserPromosDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Validate Promo =====
    group('CREATE: Validate Promo', () => {
      const validationData = {
        promoCode: voucherCode || 'TESTCODE',
        transactionAmount: Math.floor(100000 + Math.random() * 400000)
      };

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.promotion}/validate`, validationData, auth.token);
      promotionMetrics.validatePromoDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (voucherCode) {
      // ===== CREATE: Redeem Voucher =====
      group('CREATE: Redeem Voucher', () => {
        const redemptionData = generateRedemptionData(voucherCode);

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.promotion}/redeem`, redemptionData, auth.token);
        promotionMetrics.redeemVoucherDuration.add(Date.now() - startTime);

        sleep(0.5);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('Promotion Service CRUD Baseline Test - Starting');
  console.log('================================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.promotion}/health`);
  console.log(`Promotion Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'promotion-service-crud'
  };
}

export function teardown(data) {
  console.log('\n================================================');
  console.log('Promotion Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
