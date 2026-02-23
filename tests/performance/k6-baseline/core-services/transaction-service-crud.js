// PayU Transaction Service - CRUD Baseline Performance Test
// ==========================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const transactionMetrics = {
  createTransferDuration: new Trend('transaction_create_transfer_duration'),
  getTransactionDuration: new Trend('transaction_get_transaction_duration'),
  listTransactionsDuration: new Trend('transaction_list_transactions_duration'),
  getHistoryDuration: new Trend('transaction_get_history_duration'),
  updateStatusDuration: new Trend('transaction_update_status_duration'),
  cancelTransactionDuration: new Trend('transaction_cancel_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'transaction-service',
    testType: 'baseline-crud'
  }
};

// Test data generators
function generateTransferData(uniqueId) {
  const transferTypes = ['P2P', 'BANK_TRANSFER', 'BI_FAST', 'QRIS'];
  return {
    amount: Math.floor(10000 + Math.random() * 990000),
    currency: 'IDR',
    description: `Test transfer ${uniqueId}`,
    transferType: transferTypes[Math.floor(Math.random() * transferTypes.length)],
    sourceAccountId: `src-${uniqueId}`,
    destinationAccountId: `dst-${uniqueId}`,
    destinationBankCode: `BANK${Math.floor(100 + Math.random() * 900)}`,
    destinationAccountNumber: `${Math.floor(1000000000 + Math.random() * 9000000000)}`
  };
}

function generateQRISData(uniqueId) {
  return {
    amount: Math.floor(1000 + Math.random() * 99000),
    currency: 'IDR',
    description: `QRIS payment ${uniqueId}`,
    qrisCode: `QR${Math.floor(1000000000000 + Math.random() * 9000000000000)}`,
    merchantId: `MERCH${Math.floor(1000 + Math.random() * 9000)}`
  };
}

// Main test scenario
export default function () {
  const uniqueId = `${__VU}-${__ITER}-${Date.now()}`;

  // Login first
  const auth = login(__VU % 5);
  if (!auth || !auth.token) {
    console.error('Login failed, skipping test');
    return;
  }

  let transactionId = null;

  group('Transaction Service - CRUD Operations', () => {

    // ===== CREATE: Create Transfer =====
    group('CREATE: Create Transfer', () => {
      const transferData = generateTransferData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.transaction}/transfers`, transferData, auth.token);
      transactionMetrics.createTransferDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        transactionId = result.body.transactionId || result.body.id;
        console.log(`Transaction created: ${transactionId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Transactions =====
    group('READ: List Transactions', () => {
      const startTime = Date.now();
      const result = list(SERVICE_ENDPOINTS.transaction, { page: 0, size: 10 }, auth.token);
      transactionMetrics.listTransactionsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Transaction History =====
    group('READ: Get History', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.transaction}/history`, { period: '30d' }, auth.token);
      transactionMetrics.getHistoryDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (transactionId) {
      // ===== READ: Get Transaction Detail =====
      group('READ: Get Transaction', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.transaction}/${transactionId}`, auth.token);
        transactionMetrics.getTransactionDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Update Transaction Status =====
      group('UPDATE: Update Status', () => {
        const statusData = {
          status: 'COMPLETED',
          remarks: 'Test completion'
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.transaction}/${transactionId}/status`, statusData, auth.token);
        transactionMetrics.updateStatusDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Cancel Transaction =====
      group('UPDATE: Cancel Transaction', () => {
        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.transaction}/${transactionId}/cancel`, { reason: 'Test cancel' }, auth.token);
        transactionMetrics.cancelTransactionDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

// Setup function
export function setup() {
  console.log('Transaction Service CRUD Baseline Test - Starting');
  console.log('==================================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.transaction}/health`);
  console.log(`Transaction Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'transaction-service-crud'
  };
}

// Teardown function
export function teardown(data) {
  console.log('\n==================================================');
  console.log('Transaction Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
