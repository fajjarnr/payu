// PayU Billing Service - CRUD Baseline Performance Test
// =======================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const billingMetrics = {
  getBillersDuration: new Trend('billing_get_billers_duration'),
  getBillerCategoriesDuration: new Trend('billing_categories_duration'),
  inquiryDuration: new Trend('billing_inquiry_duration'),
  payBillDuration: new Trend('billing_pay_duration'),
  getPaymentHistoryDuration: new Trend('billing_history_duration'),
  getScheduledPaymentsDuration: new Trend('billing_scheduled_duration'),
  createScheduledPaymentDuration: new Trend('billing_create_scheduled_duration'),
  cancelScheduledPaymentDuration: new Trend('billing_cancel_scheduled_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'billing-service',
    testType: 'baseline-crud'
  }
};

// Biller types for testing
const BILLER_TYPES = ['PLN', 'PDAM', 'INTERNET', 'TV_CABLE', 'BPJS', 'PBB'];

// Test data generators
function generateInquiryData() {
  const billerType = BILLER_TYPES[Math.floor(Math.random() * BILLER_TYPES.length)];
  return {
    billerType: billerType,
    customerNumber: `${Math.floor(1000000000 + Math.random() * 9000000000)}`,
    productCode: `${billerType}_PREPAID`
  };
}

function generatePaymentData(inquiryData) {
  return {
    billerType: inquiryData.billerType,
    customerNumber: inquiryData.customerNumber,
    amount: Math.floor(50000 + Math.random() * 450000),
    paymentMethod: 'WALLET',
    productCode: inquiryData.productCode
  };
}

function generateScheduledPaymentData(uniqueId) {
  const billerType = BILLER_TYPES[Math.floor(Math.random() * BILLER_TYPES.length)];
  return {
    name: `Scheduled ${uniqueId}`,
    billerType: billerType,
    customerNumber: `${Math.floor(1000000000 + Math.random() * 9000000000)}`,
    amount: Math.floor(100000 + Math.random() * 400000),
    frequency: ['DAILY', 'WEEKLY', 'MONTHLY'][Math.floor(Math.random() * 3)],
    startDate: new Date(Date.now() + 86400000).toISOString().split('T')[0]
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

  let scheduledPaymentId = null;

  group('Billing Service - CRUD Operations', () => {

    // ===== READ: Get Biller Categories =====
    group('READ: Get Categories', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.billing}/categories`, {}, auth.token);
      billingMetrics.getBillerCategoriesDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Billers by Category =====
    group('READ: Get Billers', () => {
      const category = BILLER_TYPES[Math.floor(Math.random() * BILLER_TYPES.length)];

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.billing}/billers`, { category: category }, auth.token);
      billingMetrics.getBillersDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Bill Inquiry =====
    let inquiryData = null;
    group('CREATE: Bill Inquiry', () => {
      inquiryData = generateInquiryData();

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.billing}/inquiry`, inquiryData, auth.token);
      billingMetrics.inquiryDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

    // ===== CREATE: Pay Bill =====
    if (inquiryData) {
      group('CREATE: Pay Bill', () => {
        const paymentData = generatePaymentData(inquiryData);

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.billing}/pay`, paymentData, auth.token);
        billingMetrics.payBillDuration.add(Date.now() - startTime);

        sleep(0.5);
      });
    }

    // ===== READ: Payment History =====
    group('READ: Payment History', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.billing}/history`, { period: '30d' }, auth.token);
      billingMetrics.getPaymentHistoryDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Scheduled Payment =====
    group('CREATE: Scheduled Payment', () => {
      const scheduledData = generateScheduledPaymentData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.billing}/scheduled`, scheduledData, auth.token);
      billingMetrics.createScheduledPaymentDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        scheduledPaymentId = result.body.scheduledPaymentId || result.body.id;
        console.log(`Scheduled payment created: ${scheduledPaymentId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Scheduled Payments =====
    group('READ: List Scheduled', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.billing}/scheduled`, { page: 0, size: 10 }, auth.token);
      billingMetrics.getScheduledPaymentsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (scheduledPaymentId) {
      // ===== UPDATE: Cancel Scheduled Payment =====
      group('UPDATE: Cancel Scheduled', () => {
        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.billing}/scheduled/${scheduledPaymentId}/cancel`, {}, auth.token);
        billingMetrics.cancelScheduledPaymentDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('Billing Service CRUD Baseline Test - Starting');
  console.log('===============================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.billing}/health`);
  console.log(`Billing Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'billing-service-crud'
  };
}

export function teardown(data) {
  console.log('\n===============================================');
  console.log('Billing Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
