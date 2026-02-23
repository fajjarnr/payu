// PayU Analytics Service - CRUD Baseline Performance Test
// ==========================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const analyticsMetrics = {
  getUserInsightsDuration: new Trend('analytics_user_insights_duration'),
  getTransactionAnalyticsDuration: new Trend('analytics_txn_analytics_duration'),
  getFraudScoreDuration: new Trend('analytics_fraud_score_duration'),
  createReportDuration: new Trend('analytics_create_report_duration'),
  getReportsDuration: new Trend('analytics_get_reports_duration'),
  getDashboardDataDuration: new Trend('analytics_dashboard_duration'),
  queryEventsDuration: new Trend('analytics_query_events_duration'),
  predictTrendDuration: new Trend('analytics_predict_trend_duration'),
  exportDataDuration: new Trend('analytics_export_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'analytics-service',
    testType: 'baseline-crud'
  }
};

// Report types and metrics
const REPORT_TYPES = ['TRANSACTION_SUMMARY', 'USER_BEHAVIOR', 'FRAUD_ANALYSIS', 'REVENUE_REPORT'];
const TIME_RANGES = ['1d', '7d', '30d', '90d', '1y'];

// Test data generators
function generateReportData(uniqueId) {
  return {
    name: `Report ${uniqueId}`,
    type: REPORT_TYPES[Math.floor(Math.random() * REPORT_TYPES.length)],
    timeRange: TIME_RANGES[Math.floor(Math.random() * TIME_RANGES.length)],
    filters: {
      minAmount: 100000,
      maxAmount: 10000000,
      status: ['COMPLETED', 'PENDING'],
      channels: ['MOBILE', 'WEB']
    },
    format: ['JSON', 'CSV', 'PDF'][Math.floor(Math.random() * 3)],
    scheduled: Math.random() > 0.7,
    scheduleConfig: {
      frequency: 'DAILY',
      emailRecipients: [`admin${uniqueId}@payu.test`]
    }
  };
}

function generateFraudCheckData(uniqueId) {
  return {
    transactionId: `TXN${Math.floor(1000000 + Math.random() * 9000000)}`,
    userId: `USR${Math.floor(100000 + Math.random() * 900000)}`,
    amount: Math.floor(100000 + Math.random() * 9900000),
    currency: 'IDR',
    sourceIp: `192.168.${Math.floor(1 + Math.random() * 254)}.${Math.floor(1 + Math.random() * 254)}`,
    deviceFingerprint: `fp_${uniqueId}`,
    merchantId: `MERCH${Math.floor(1000 + Math.random() * 9000)}`,
    transactionType: ['TRANSFER', 'PAYMENT', 'WITHDRAWAL'][Math.floor(Math.random() * 3)]
  };
}

function generateEventQueryData() {
  const now = new Date();
  return {
    eventTypes: ['page_view', 'button_click', 'transaction_initiated', 'transaction_completed'],
    startTime: new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString(),
    endTime: now.toISOString(),
    filters: {
      platform: 'MOBILE',
      appVersion: '1.0.0'
    },
    aggregations: ['count', 'sum', 'avg'],
    groupBy: ['event_type', 'hour']
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

  let reportId = null;

  group('Analytics Service - CRUD Operations', () => {

    // ===== READ: Get User Insights =====
    group('READ: Get User Insights', () => {
      const params = {
        userId: `USR${Math.floor(100000 + Math.random() * 900000)}`,
        period: '30d'
      };

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.analytics}/user-insights`, params, auth.token);
      analyticsMetrics.getUserInsightsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Transaction Analytics =====
    group('READ: Transaction Analytics', () => {
      const params = {
        period: TIME_RANGES[Math.floor(Math.random() * TIME_RANGES.length)],
        granularity: 'day'
      };

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.analytics}/transactions`, params, auth.token);
      analyticsMetrics.getTransactionAnalyticsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Get Fraud Score =====
    group('CREATE: Get Fraud Score', () => {
      const fraudData = generateFraudCheckData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.analytics}/fraud-score`, fraudData, auth.token);
      analyticsMetrics.getFraudScoreDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

    // ===== READ: Get Dashboard Data =====
    group('READ: Dashboard Data', () => {
      const params = {
        dashboard: 'executive',
        dateRange: '30d'
      };

      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.analytics}/dashboard`, auth.token);
      analyticsMetrics.getDashboardDataDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Create Report =====
    group('CREATE: Create Report', () => {
      const reportData = generateReportData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.analytics}/reports`, reportData, auth.token);
      analyticsMetrics.createReportDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        reportId = result.body.reportId || result.body.id;
        console.log(`Report created: ${reportId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List Reports =====
    group('READ: List Reports', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.analytics}/reports`, {}, auth.token);
      analyticsMetrics.getReportsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Query Events =====
    group('CREATE: Query Events', () => {
      const queryData = generateEventQueryData();

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.analytics}/events/query`, queryData, auth.token);
      analyticsMetrics.queryEventsDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

    // ===== CREATE: Predict Trend =====
    group('CREATE: Predict Trend', () => {
      const predictData = {
        metric: 'transaction_volume',
        forecastPeriod: '7d',
        historicalDataRange: '90d'
      };

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.analytics}/predict`, predictData, auth.token);
      analyticsMetrics.predictTrendDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

    if (reportId) {
      // ===== READ: Export Data =====
      group('READ: Export Report', () => {
        const params = {
          format: 'CSV'
        };

        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.analytics}/reports/${reportId}/export`, auth.token);
        analyticsMetrics.exportDataDuration.add(Date.now() - startTime);

        sleep(0.5);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('Analytics Service CRUD Baseline Test - Starting');
  console.log('=================================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.analytics}/health`);
  console.log(`Analytics Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'analytics-service-crud'
  };
}

export function teardown(data) {
  console.log('\n=================================================');
  console.log('Analytics Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
