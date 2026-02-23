// PayU Backoffice Service - CRUD Baseline Performance Test
// ===========================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const backofficeMetrics = {
  getDashboardMetricsDuration: new Trend('backoffice_dashboard_duration'),
  getUsersDuration: new Trend('backoffice_get_users_duration'),
  getUserDetailDuration: new Trend('backoffice_user_detail_duration'),
  updateUserStatusDuration: new Trend('backoffice_update_user_duration'),
  getTransactionsDuration: new Trend('backoffice_transactions_duration'),
  getAuditLogsDuration: new Trend('backoffice_audit_logs_duration'),
  getSystemHealthDuration: new Trend('backoffice_system_health_duration'),
  generateReportDuration: new Trend('backoffice_generate_report_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'backoffice-service',
    testType: 'baseline-crud'
  }
};

// User statuses and roles
const USER_STATUSES = ['ACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION', 'CLOSED'];
const USER_ROLES = ['CUSTOMER', 'MERCHANT', 'ADMIN', 'SUPPORT'];
const REPORT_TYPES = ['USER_ACTIVITY', 'TRANSACTION_SUMMARY', 'AUDIT_LOG', 'SYSTEM_PERFORMANCE'];

// Main test scenario
export default function () {
  const uniqueId = `${__VU}-${__ITER}-${Date.now()}`;

  const auth = login(__VU % 5);
  if (!auth || !auth.token) {
    console.error('Login failed, skipping test');
    return;
  }

  group('Backoffice Service - CRUD Operations', () => {

    // ===== READ: Get Dashboard Metrics =====
    group('READ: Dashboard Metrics', () => {
      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.backoffice}/dashboard/metrics`, auth.token);
      backofficeMetrics.getDashboardMetricsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get System Health =====
    group('READ: System Health', () => {
      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.backoffice}/system/health`, auth.token);
      backofficeMetrics.getSystemHealthDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Users =====
    group('READ: Get Users', () => {
      const params = {
        page: 0,
        size: 20,
        status: USER_STATUSES[Math.floor(Math.random() * USER_STATUSES.length)]
      };

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.backoffice}/users`, params, auth.token);
      backofficeMetrics.getUsersDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get User Detail =====
    group('READ: Get User Detail', () => {
      const userId = `USR${Math.floor(100000 + Math.random() * 900000)}`;

      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.backoffice}/users/${userId}`, auth.token);
      backofficeMetrics.getUserDetailDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== UPDATE: Update User Status =====
    group('UPDATE: Update User Status', () => {
      const userId = `USR${Math.floor(100000 + Math.random() * 900000)}`;
      const updateData = {
        status: USER_STATUSES[Math.floor(Math.random() * USER_STATUSES.length)],
        reason: 'Status update during baseline test',
        updatedBy: 'admin'
      };

      const startTime = Date.now();
      const result = update(`${SERVICE_ENDPOINTS.backoffice}/users/${userId}/status`, updateData, auth.token);
      backofficeMetrics.updateUserStatusDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Transactions =====
    group('READ: Get Transactions', () => {
      const params = {
        page: 0,
        size: 20,
        startDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
        endDate: new Date().toISOString()
      };

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.backoffice}/transactions`, params, auth.token);
      backofficeMetrics.getTransactionsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Audit Logs =====
    group('READ: Get Audit Logs', () => {
      const params = {
        page: 0,
        size: 50,
        action: ['CREATE', 'UPDATE', 'DELETE', 'LOGIN'][Math.floor(Math.random() * 4)],
        startDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString()
      };

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.backoffice}/audit-logs`, params, auth.token);
      backofficeMetrics.getAuditLogsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Generate Report =====
    group('CREATE: Generate Report', () => {
      const reportData = {
        reportType: REPORT_TYPES[Math.floor(Math.random() * REPORT_TYPES.length)],
        startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
        endDate: new Date().toISOString(),
        format: 'PDF',
        filters: {
          userRole: USER_ROLES[Math.floor(Math.random() * USER_ROLES.length)],
          minAmount: 1000000
        }
      };

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.backoffice}/reports`, reportData, auth.token);
      backofficeMetrics.generateReportDuration.add(Date.now() - startTime);

      sleep(0.5);
    });

  });

  sleep(1);
}

export function setup() {
  console.log('Backoffice Service CRUD Baseline Test - Starting');
  console.log('=================================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.backoffice}/health`);
  console.log(`Backoffice Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'backoffice-service-crud'
  };
}

export function teardown(data) {
  console.log('\n=================================================');
  console.log('Backoffice Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
