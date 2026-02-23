// PayU Partner Service - CRUD Baseline Performance Test
// ========================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const partnerMetrics = {
  registerPartnerDuration: new Trend('partner_register_duration'),
  getPartnersDuration: new Trend('partner_get_list_duration'),
  getPartnerDetailDuration: new Trend('partner_get_detail_duration'),
  updatePartnerDuration: new Trend('partner_update_duration'),
  verifyPartnerDuration: new Trend('partner_verify_duration'),
  getPartnerServicesDuration: new Trend('partner_services_duration'),
  addServiceDuration: new Trend('partner_add_service_duration'),
  getTransactionsDuration: new Trend('partner_transactions_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'partner-service',
    testType: 'baseline-crud'
  }
};

// Partner types
const PARTNER_TYPES = ['BANK', 'MERCHANT', 'BILLER', 'PAYMENT_GATEWAY', 'FINANCIAL_INSTITUTION'];
const BUSINESS_CATEGORIES = ['RETAIL', 'E_COMMERCE', 'UTILITIES', 'TRANSPORTATION', 'FOOD_BEVERAGE'];

// Test data generators
function generatePartnerData(uniqueId) {
  return {
    name: `Partner ${uniqueId}`,
    type: PARTNER_TYPES[Math.floor(Math.random() * PARTNER_TYPES.length)],
    businessCategory: BUSINESS_CATEGORIES[Math.floor(Math.random() * BUSINESS_CATEGORIES.length)],
    email: `partner_${uniqueId}@partner.test`,
    phone: `+6281${Math.floor(10000000 + Math.random() * 90000000)}`,
    address: {
      street: `Street ${uniqueId}`,
      city: 'Jakarta',
      province: 'DKI Jakarta',
      postalCode: '10000'
    },
    taxId: `09.${Math.floor(100 + Math.random() * 900)}.${Math.floor(100 + Math.random() * 900)}.${Math.floor(1 + Math.random() * 9)}-${Math.floor(100 + Math.random() * 900)}.${Math.floor(1000 + Math.random() * 9000)}`
  };
}

function generateServiceData(uniqueId) {
  return {
    serviceCode: `SRV_${uniqueId}`,
    serviceName: `Service ${uniqueId}`,
    serviceType: ['PAYMENT', 'TRANSFER', 'INQUIRY', 'SETTLEMENT'][Math.floor(Math.random() * 4)],
    feePercentage: (0.5 + Math.random() * 2).toFixed(2),
    minFee: 1000,
    maxFee: 25000
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

  let partnerId = null;

  group('Partner Service - CRUD Operations', () => {

    // ===== CREATE: Register Partner =====
    group('CREATE: Register Partner', () => {
      const partnerData = generatePartnerData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.partner}`, partnerData, auth.token);
      partnerMetrics.registerPartnerDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        partnerId = result.body.partnerId || result.body.id;
        console.log(`Partner registered: ${partnerId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Partners =====
    group('READ: List Partners', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.partner}`, { page: 0, size: 10 }, auth.token);
      partnerMetrics.getPartnersDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (partnerId) {
      // ===== READ: Get Partner Detail =====
      group('READ: Get Partner Detail', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.partner}/${partnerId}`, auth.token);
        partnerMetrics.getPartnerDetailDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Update Partner =====
      group('UPDATE: Update Partner', () => {
        const updateData = {
          name: `Updated Partner ${Date.now()}`,
          phone: `+6281${Math.floor(10000000 + Math.random() * 90000000)}`,
          status: 'ACTIVE'
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.partner}/${partnerId}`, updateData, auth.token);
        partnerMetrics.updatePartnerDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Verify Partner =====
      group('UPDATE: Verify Partner', () => {
        const verifyData = {
          verified: true,
          verificationNotes: 'Verified during baseline test'
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.partner}/${partnerId}/verify`, verifyData, auth.token);
        partnerMetrics.verifyPartnerDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== READ: Get Partner Services =====
      group('READ: Get Partner Services', () => {
        const startTime = Date.now();
        const result = list(`${SERVICE_ENDPOINTS.partner}/${partnerId}/services`, {}, auth.token);
        partnerMetrics.getPartnerServicesDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== CREATE: Add Partner Service =====
      group('CREATE: Add Service', () => {
        const serviceData = generateServiceData(uniqueId);

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.partner}/${partnerId}/services`, serviceData, auth.token);
        partnerMetrics.addServiceDuration.add(Date.now() - startTime);

        sleep(0.5);
      });

      // ===== READ: Get Partner Transactions =====
      group('READ: Get Transactions', () => {
        const startTime = Date.now();
        const result = list(`${SERVICE_ENDPOINTS.partner}/${partnerId}/transactions`, { period: '30d' }, auth.token);
        partnerMetrics.getTransactionsDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('Partner Service CRUD Baseline Test - Starting');
  console.log('==============================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.partner}/health`);
  console.log(`Partner Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'partner-service-crud'
  };
}

export function teardown(data) {
  console.log('\n==============================================');
  console.log('Partner Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
