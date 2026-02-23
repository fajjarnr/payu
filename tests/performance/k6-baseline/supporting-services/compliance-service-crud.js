// PayU Compliance Service - CRUD Baseline Performance Test
// ===========================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const complianceMetrics = {
  createAmlCheckDuration: new Trend('compliance_aml_check_duration'),
  getAmlChecksDuration: new Trend('compliance_get_aml_duration'),
  getAmlDetailDuration: new Trend('compliance_aml_detail_duration'),
  submitReportDuration: new Trend('compliance_submit_report_duration'),
  getReportsDuration: new Trend('compliance_get_reports_duration'),
  getRiskProfileDuration: new Trend('compliance_risk_profile_duration'),
  updateRiskProfileDuration: new Trend('compliance_update_risk_duration'),
  validateTransactionDuration: new Trend('compliance_validate_txn_duration'),
  auditLogQueryDuration: new Trend('compliance_audit_query_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'compliance-service',
    testType: 'baseline-crud'
  }
};

// AML check types and risk levels
const CHECK_TYPES = ['TRANSACTION_MONITORING', 'SANCTIONS_SCREENING', 'PEP_CHECK', 'CUSTOMER_DUE_DILIGENCE'];
const RISK_LEVELS = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const REPORT_TYPES = ['SUSPICIOUS_ACTIVITY', 'LARGE_CASH_TRANSACTION', 'CROSS_BORDER', 'TERRORIST_FINANCING'];

// Test data generators
function generateAmlCheckData(uniqueId) {
  return {
    checkType: CHECK_TYPES[Math.floor(Math.random() * CHECK_TYPES.length)],
    entityType: Math.random() > 0.5 ? 'INDIVIDUAL' : 'CORPORATE',
    entityId: `ENTITY${Math.floor(100000 + Math.random() * 900000)}`,
    transactionId: `TXN${Math.floor(1000000 + Math.random() * 9000000)}`,
    amount: Math.floor(10000000 + Math.random() * 990000000),
    currency: 'IDR',
    sourceCountry: 'ID',
    destinationCountry: ['ID', 'SG', 'US', 'JP'][Math.floor(Math.random() * 4)]
  };
}

function generateReportData(uniqueId) {
  return {
    reportType: REPORT_TYPES[Math.floor(Math.random() * REPORT_TYPES.length)],
    subjectId: `SUBJECT${Math.floor(100000 + Math.random() * 900000)}`,
    description: `Suspicious activity detected during test ${uniqueId}`,
    involvedAmount: Math.floor(100000000 + Math.random() * 900000000),
    currency: 'IDR',
    relatedTransactions: [
      `TXN${Math.floor(1000000 + Math.random() * 9000000)}`,
      `TXN${Math.floor(1000000 + Math.random() * 9000000)}`
    ]
  };
}

function generateRiskProfileData() {
  return {
    riskLevel: RISK_LEVELS[Math.floor(Math.random() * RISK_LEVELS.length)],
    riskFactors: ['High transaction volume', 'Cross border activity', 'New customer'],
    reviewDate: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString()
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

  let amlCheckId = null;

  group('Compliance Service - CRUD Operations', () => {

    // ===== CREATE: AML Check =====
    group('CREATE: AML Check', () => {
      const amlData = generateAmlCheckData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.compliance}/aml-checks`, amlData, auth.token);
      complianceMetrics.createAmlCheckDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        amlCheckId = result.body.checkId || result.body.id;
        console.log(`AML check created: ${amlCheckId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List AML Checks =====
    group('READ: List AML Checks', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.compliance}/aml-checks`, { status: 'PENDING' }, auth.token);
      complianceMetrics.getAmlChecksDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (amlCheckId) {
      // ===== READ: Get AML Check Detail =====
      group('READ: Get AML Detail', () => {
        const startTime = Date.now();
        const result = read(`${SERVICE_ENDPOINTS.compliance}/aml-checks/${amlCheckId}`, auth.token);
        complianceMetrics.getAmlDetailDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

    // ===== CREATE: Submit Compliance Report =====
    group('CREATE: Submit Report', () => {
      const reportData = generateReportData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.compliance}/reports`, reportData, auth.token);
      complianceMetrics.submitReportDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

    // ===== READ: Get Reports =====
    group('READ: Get Reports', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.compliance}/reports`, { period: '30d' }, auth.token);
      complianceMetrics.getReportsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Risk Profile =====
    group('READ: Get Risk Profile', () => {
      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.compliance}/risk-profile`, auth.token);
      complianceMetrics.getRiskProfileDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== UPDATE: Update Risk Profile =====
    group('UPDATE: Update Risk Profile', () => {
      const riskData = generateRiskProfileData();

      const startTime = Date.now();
      const result = update(`${SERVICE_ENDPOINTS.compliance}/risk-profile`, riskData, auth.token);
      complianceMetrics.updateRiskProfileDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Validate Transaction =====
    group('CREATE: Validate Transaction', () => {
      const validationData = {
        transactionId: `TXN${Math.floor(1000000 + Math.random() * 9000000)}`,
        amount: Math.floor(1000000 + Math.random() * 99000000),
        currency: 'IDR',
        sourceAccount: `ACC${Math.floor(10000000 + Math.random() * 90000000)}`,
        destinationAccount: `ACC${Math.floor(10000000 + Math.random() * 90000000)}`,
        transactionType: 'TRANSFER'
      };

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.compliance}/validate-transaction`, validationData, auth.token);
      complianceMetrics.validateTransactionDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

    // ===== READ: Query Audit Logs =====
    group('READ: Query Audit Logs', () => {
      const startTime = Date.now();
      const result = list(
        `${SERVICE_ENDPOINTS.compliance}/audit-logs`,
        { startDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(), limit: 50 },
        auth.token
      );
      complianceMetrics.auditLogQueryDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

  });

  sleep(1);
}

export function setup() {
  console.log('Compliance Service CRUD Baseline Test - Starting');
  console.log('=================================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.compliance}/health`);
  console.log(`Compliance Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'compliance-service-crud'
  };
}

export function teardown(data) {
  console.log('\n=================================================');
  console.log('Compliance Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
