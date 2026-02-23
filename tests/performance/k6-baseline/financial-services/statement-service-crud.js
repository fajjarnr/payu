// PayU Statement Service - CRUD Baseline Performance Test
// ==========================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const statementMetrics = {
  generateStatementDuration: new Trend('statement_generate_duration'),
  getStatementsDuration: new Trend('statement_get_list_duration'),
  getStatementDetailDuration: new Trend('statement_get_detail_duration'),
  downloadStatementDuration: new Trend('statement_download_duration'),
  emailStatementDuration: new Trend('statement_email_duration'),
  getTransactionSummaryDuration: new Trend('statement_summary_duration'),
  scheduleStatementDuration: new Trend('statement_schedule_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'statement-service',
    testType: 'baseline-crud'
  }
};

// Statement types
const STATEMENT_TYPES = ['MONTHLY', 'QUARTERLY', 'ANNUAL', 'CUSTOM'];
const ACCOUNT_TYPES = ['SAVINGS', 'CHECKING', 'DEPOSIT', 'WALLET'];

// Test data generators
function generateStatementRequestData() {
  const now = new Date();
  const startDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const endDate = new Date(now.getFullYear(), now.getMonth(), 0);

  return {
    statementType: STATEMENT_TYPES[Math.floor(Math.random() * STATEMENT_TYPES.length)],
    accountType: ACCOUNT_TYPES[Math.floor(Math.random() * ACCOUNT_TYPES.length)],
    startDate: startDate.toISOString().split('T')[0],
    endDate: endDate.toISOString().split('T')[0],
    format: ['PDF', 'CSV', 'XLSX'][Math.floor(Math.random() * 3)]
  };
}

function generateScheduleStatementData(uniqueId) {
  return {
    name: `Scheduled Statement ${uniqueId}`,
    statementType: 'MONTHLY',
    accountType: ACCOUNT_TYPES[Math.floor(Math.random() * ACCOUNT_TYPES.length)],
    deliveryMethod: ['EMAIL', 'DOWNLOAD', 'BOTH'][Math.floor(Math.random() * 3)],
    email: `user${uniqueId}@payu.test`,
    scheduleDay: Math.floor(1 + Math.random() * 28)
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

  let statementId = null;

  group('Statement Service - CRUD Operations', () => {

    // ===== CREATE: Generate Statement =====
    group('CREATE: Generate Statement', () => {
      const statementData = generateStatementRequestData();

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.statement}/generate`, statementData, auth.token);
      statementMetrics.generateStatementDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        statementId = result.body.statementId || result.body.id;
        console.log(`Statement generated: ${statementId}`);
      }

      sleep(1);
    });

    // ===== READ: List Statements =====
    group('READ: List Statements', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.statement}`, { page: 0, size: 10 }, auth.token);
      statementMetrics.getStatementsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (statementId) {
      // ===== READ: Get Statement Detail =====
      group('READ: Get Statement Detail', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.statement}/${statementId}`, auth.token);
        statementMetrics.getStatementDetailDuration.add(Date.now() - startTime);

        sleep(0.3);
      });

      // ===== READ: Download Statement =====
      group('READ: Download Statement', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.statement}/${statementId}/download`, auth.token);
        statementMetrics.downloadStatementDuration.add(Date.now() - startTime);

        sleep(0.5);
      });

      // ===== CREATE: Email Statement =====
      group('CREATE: Email Statement', () => {
        const emailData = {
          email: `user${uniqueId}@payu.test`,
          subject: 'Your PayU Statement'
        };

        const startTime = Date.now();
        const result = create(`${SERVICE_ENDPOINTS.statement}/${statementId}/email`, emailData, auth.token);
        statementMetrics.emailStatementDuration.add(Date.now() - startTime);

        sleep(0.5);
      });
    }

    // ===== READ: Transaction Summary =====
    group('READ: Transaction Summary', () => {
      const now = new Date();
      const params = {
        period: '30d',
        year: now.getFullYear().toString(),
        month: (now.getMonth() + 1).toString()
      };

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.statement}/summary`, params, auth.token);
      statementMetrics.getTransactionSummaryDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Schedule Statement =====
    group('CREATE: Schedule Statement', () => {
      const scheduleData = generateScheduleStatementData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.statement}/schedule`, scheduleData, auth.token);
      statementMetrics.scheduleStatementDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

  });

  sleep(1);
}

export function setup() {
  console.log('Statement Service CRUD Baseline Test - Starting');
  console.log('================================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.statement}/health`);
  console.log(`Statement Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'statement-service-crud'
  };
}

export function teardown(data) {
  console.log('\n================================================');
  console.log('Statement Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
