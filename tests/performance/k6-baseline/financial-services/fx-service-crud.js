// PayU FX Service - CRUD Baseline Performance Test
// ==================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const fxMetrics = {
  getRatesDuration: new Trend('fx_get_rates_duration'),
  getRateByPairDuration: new Trend('fx_get_rate_pair_duration'),
  convertCurrencyDuration: new Trend('fx_convert_duration'),
  createRateAlertDuration: new Trend('fx_create_alert_duration'),
  listAlertsDuration: new Trend('fx_list_alerts_duration'),
  updateAlertDuration: new Trend('fx_update_alert_duration'),
  deleteAlertDuration: new Trend('fx_delete_alert_duration'),
  getHistoricalRatesDuration: new Trend('fx_historical_rates_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'fx-service',
    testType: 'baseline-crud'
  }
};

// Currency pairs for testing
const CURRENCY_PAIRS = [
  { from: 'IDR', to: 'USD' },
  { from: 'IDR', to: 'EUR' },
  { from: 'IDR', to: 'SGD' },
  { from: 'IDR', to: 'JPY' },
  { from: 'USD', to: 'IDR' }
];

// Test data generators
function generateConvertData() {
  const pair = CURRENCY_PAIRS[Math.floor(Math.random() * CURRENCY_PAIRS.length)];
  return {
    fromCurrency: pair.from,
    toCurrency: pair.to,
    amount: Math.floor(100000 + Math.random() * 9900000)
  };
}

function generateRateAlertData(uniqueId) {
  const pair = CURRENCY_PAIRS[Math.floor(Math.random() * CURRENCY_PAIRS.length)];
  return {
    name: `Alert ${uniqueId}`,
    fromCurrency: pair.from,
    toCurrency: pair.to,
    targetRate: (14000 + Math.random() * 2000).toFixed(2),
    alertType: Math.random() > 0.5 ? 'ABOVE' : 'BELOW',
    notificationMethod: 'PUSH'
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

  let alertId = null;

  group('FX Service - CRUD Operations', () => {

    // ===== READ: Get All Exchange Rates =====
    group('READ: Get All Rates', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.fx}/rates`, { base: 'IDR' }, auth.token);
      fxMetrics.getRatesDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Rate by Currency Pair =====
    group('READ: Get Rate by Pair', () => {
      const pair = CURRENCY_PAIRS[Math.floor(Math.random() * CURRENCY_PAIRS.length)];

      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.fx}/rates/${pair.from}/${pair.to}`, auth.token);
      fxMetrics.getRateByPairDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Convert Currency =====
    group('CREATE: Convert Currency', () => {
      const convertData = generateConvertData();

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.fx}/convert`, convertData, auth.token);
      fxMetrics.convertCurrencyDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Historical Rates =====
    group('READ: Historical Rates', () => {
      const pair = CURRENCY_PAIRS[Math.floor(Math.random() * CURRENCY_PAIRS.length)];

      const startTime = Date.now();
      const result = list(
        `${SERVICE_ENDPOINTS.fx}/rates/${pair.from}/${pair.to}/historical`,
        { period: '7d', interval: '1d' },
        auth.token
      );
      fxMetrics.getHistoricalRatesDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Create Rate Alert =====
    group('CREATE: Create Rate Alert', () => {
      const alertData = generateRateAlertData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.fx}/alerts`, alertData, auth.token);
      fxMetrics.createRateAlertDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        alertId = result.body.alertId || result.body.id;
        console.log(`Rate alert created: ${alertId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Rate Alerts =====
    group('READ: List Alerts', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.fx}/alerts`, { page: 0, size: 10 }, auth.token);
      fxMetrics.listAlertsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (alertId) {
      // ===== UPDATE: Update Alert =====
      group('UPDATE: Update Alert', () => {
        const updateData = {
          targetRate: (15000 + Math.random() * 2000).toFixed(2),
          active: true
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.fx}/alerts/${alertId}`, updateData, auth.token);
        fxMetrics.updateAlertDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== DELETE: Delete Alert =====
      group('DELETE: Delete Alert', () => {
        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.fx}/alerts/${alertId}/deactivate`, {}, auth.token);
        fxMetrics.deleteAlertDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('FX Service CRUD Baseline Test - Starting');
  console.log('=========================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.fx}/health`);
  console.log(`FX Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'fx-service-crud'
  };
}

export function teardown(data) {
  console.log('\n=========================================');
  console.log('FX Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
