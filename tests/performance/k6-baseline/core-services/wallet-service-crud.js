// PayU Wallet Service - CRUD Baseline Performance Test
// =====================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, del, list } from '../lib/crud-helper.js';

// Service-specific metrics
const walletMetrics = {
  createPocketDuration: new Trend('wallet_create_pocket_duration'),
  getWalletDuration: new Trend('wallet_get_wallet_duration'),
  getPocketDuration: new Trend('wallet_get_pocket_duration'),
  listPocketsDuration: new Trend('wallet_list_pockets_duration'),
  creditDuration: new Trend('wallet_credit_duration'),
  debitDuration: new Trend('wallet_debit_duration'),
  updatePocketDuration: new Trend('wallet_update_pocket_duration'),
  closePocketDuration: new Trend('wallet_close_pocket_duration'),
  getBalanceDuration: new Trend('wallet_get_balance_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'wallet-service',
    testType: 'baseline-crud'
  }
};

// Test data generators
function generatePocketData(uniqueId) {
  const pocketTypes = ['SAVINGS', 'INVESTMENT', 'BILLS', 'TRAVEL', 'SHOPPING'];
  return {
    name: `Pocket ${uniqueId}`,
    type: pocketTypes[Math.floor(Math.random() * pocketTypes.length)],
    description: `Test pocket created during baseline test ${uniqueId}`,
    targetAmount: Math.floor(1000000 + Math.random() * 9000000),
    currency: 'IDR'
  };
}

function generateCreditData() {
  return {
    amount: Math.floor(10000 + Math.random() * 990000),
    currency: 'IDR',
    description: `Credit transaction ${Date.now()}`,
    source: 'TEST_TOPUP'
  };
}

function generateDebitData() {
  return {
    amount: Math.floor(1000 + Math.random() * 9000),
    currency: 'IDR',
    description: `Debit transaction ${Date.now()}`,
    destination: 'TEST_PAYMENT'
  };
}

function generateUpdatePocketData() {
  return {
    name: `Updated Pocket ${Date.now()}`,
    description: 'Updated during baseline test',
    targetAmount: Math.floor(2000000 + Math.random() * 8000000)
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

  let pocketId = null;

  group('Wallet Service - CRUD Operations', () => {

    // ===== CREATE: Create Pocket =====
    group('CREATE: Create Pocket', () => {
      const pocketData = generatePocketData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.wallet}/pockets`, pocketData, auth.token);
      walletMetrics.createPocketDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        pocketId = result.body.pocketId || result.body.id;
        console.log(`Pocket created: ${pocketId}`);
      }

      sleep(0.5);
    });

    // ===== READ: Get Wallet =====
    group('READ: Get Wallet', () => {
      const startTime = Date.now();
      const result = read(SERVICE_ENDPOINTS.wallet, auth.token);
      walletMetrics.getWalletDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Balance =====
    group('READ: Get Balance', () => {
      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.wallet}/balance`, auth.token);
      walletMetrics.getBalanceDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: List Pockets =====
    group('READ: List Pockets', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.wallet}/pockets`, { page: 0, size: 10 }, auth.token);
      walletMetrics.listPocketsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (pocketId) {
      // ===== READ: Get Pocket by ID =====
      group('READ: Get Pocket', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.wallet}/pockets/${pocketId}`, auth.token);
        walletMetrics.getPocketDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== CREATE: Credit Pocket =====
      group('CREATE: Credit Pocket', () => {
        const creditData = generateCreditData();

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.wallet}/pockets/${pocketId}/credit`, creditData, auth.token);
        walletMetrics.creditDuration.add(Date.now() - startTime);

        sleep(0.5);
      });

      // ===== CREATE: Debit Pocket =====
      group('CREATE: Debit Pocket', () => {
        const debitData = generateDebitData();

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.wallet}/pockets/${pocketId}/debit`, debitData, auth.token);
        walletMetrics.debitDuration.add(Date.now() - startTime);

        sleep(0.5);
      });

      // ===== UPDATE: Update Pocket =====
      group('UPDATE: Update Pocket', () => {
        const updateData = generateUpdatePocketData();

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.wallet}/pockets/${pocketId}`, updateData, auth.token);
        walletMetrics.updatePocketDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== UPDATE: Close Pocket =====
      group('UPDATE: Close Pocket', () => {
        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.wallet}/pockets/${pocketId}/close`, {}, auth.token);
        walletMetrics.closePocketDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

// Setup function
export function setup() {
  console.log('Wallet Service CRUD Baseline Test - Starting');
  console.log('=============================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.wallet}/health`);
  console.log(`Wallet Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'wallet-service-crud'
  };
}

// Teardown function
export function teardown(data) {
  console.log('\n=============================================');
  console.log('Wallet Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
