// PayU API Portal Service - CRUD Baseline Performance Test
// ===========================================================
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASELINE_THRESHOLDS, BASELINE_STAGES, SERVICE_ENDPOINTS, BASE_URLS } from '../config/baseline-config.js';
import { login, getAuthHeaders } from '../lib/auth-helper.js';
import { create, read, update, list } from '../lib/crud-helper.js';

// Service-specific metrics
const apiPortalMetrics = {
  getApiDocsDuration: new Trend('apiportal_get_docs_duration'),
  getApiSpecDuration: new Trend('apiportal_get_spec_duration'),
  createApiKeyDuration: new Trend('apiportal_create_key_duration'),
  getApiKeysDuration: new Trend('apiportal_get_keys_duration'),
  revokeApiKeyDuration: new Trend('apiportal_revoke_key_duration'),
  getUsageStatsDuration: new Trend('apiportal_usage_stats_duration'),
  testEndpointDuration: new Trend('apiportal_test_endpoint_duration'),
  getSandboxDataDuration: new Trend('apiportal_sandbox_data_duration')
};

// Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS,
  tags: {
    service: 'api-portal-service',
    testType: 'baseline-crud'
  }
};

// API categories
const CATEGORIES = ['PAYMENT', 'WALLET', 'TRANSFER', 'IDENTITY', 'NOTIFICATION'];
const ENVIRONMENTS = ['SANDBOX', 'PRODUCTION'];

// Test data generators
function generateApiKeyData(uniqueId) {
  return {
    name: `API Key ${uniqueId}`,
    description: `Test API key created during baseline testing ${uniqueId}`,
    scopes: ['read:transactions', 'write:transfers', 'read:wallet'],
    environment: ENVIRONMENTS[Math.floor(Math.random() * ENVIRONMENTS.length)],
    rateLimit: Math.floor(100 + Math.random() * 900),
    allowedIps: ['0.0.0.0/0'],
    expiryDays: Math.floor(30 + Math.random() * 60)
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

  let apiKeyId = null;

  group('API Portal Service - CRUD Operations', () => {

    // ===== READ: Get API Documentation =====
    group('READ: Get API Docs', () => {
      const params = {
        category: CATEGORIES[Math.floor(Math.random() * CATEGORIES.length)]
      };

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.apiPortal}/apis`, params, auth.token);
      apiPortalMetrics.getApiDocsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get API Specification =====
    group('READ: Get API Spec', () => {
      const service = ['wallet', 'transaction', 'account'][Math.floor(Math.random() * 3)];

      const startTime = Date.now();
      const result = read(`${SERVICE_ENDPOINTS.apiPortal}/apis/${service}/spec`, auth.token);
      apiPortalMetrics.getApiSpecDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Create API Key =====
    group('CREATE: Create API Key', () => {
      const keyData = generateApiKeyData(uniqueId);

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.apiPortal}/keys`, keyData, auth.token);
      apiPortalMetrics.createApiKeyDuration.add(Date.now() - startTime);

      if (result.success && result.body) {
        apiKeyId = result.body.keyId || result.body.id;
        console.log(`API Key created: ${apiKeyId}`);
      }

      sleep(0.5);
    });

    // ===== READ: List API Keys =====
    group('READ: List API Keys', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.apiPortal}/keys`, {}, auth.token);
      apiPortalMetrics.getApiKeysDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Usage Statistics =====
    group('READ: Get Usage Stats', () => {
      const params = {
        period: '30d'
      };

      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.apiPortal}/usage`, params, auth.token);
      apiPortalMetrics.getUsageStatsDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== CREATE: Test Endpoint =====
    group('CREATE: Test Endpoint', () => {
      const testData = {
        endpoint: '/api/v1/wallets/balance',
        method: 'GET',
        headers: {
          'Accept': 'application/json'
        },
        body: null
      };

      const startTime = Date.now();
      const result = create(`${SERVICE_ENDPOINTS.apiPortal}/sandbox/test`, testData, auth.token);
      apiPortalMetrics.testEndpointDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    // ===== READ: Get Sandbox Data =====
    group('READ: Get Sandbox Data', () => {
      const startTime = Date.now();
      const result = list(`${SERVICE_ENDPOINTS.apiPortal}/sandbox/data`, {}, auth.token);
      apiPortalMetrics.getSandboxDataDuration.add(Date.now() - startTime);

      sleep(0.3);
    });

    if (apiKeyId) {
      // ===== UPDATE: Revoke API Key =====
      group('UPDATE: Revoke API Key', () => {
        const revokeData = {
          reason: 'Test revocation during baseline'
        };

        const startTime = Date.now();
        const result = update(`${SERVICE_ENDPOINTS.apiPortal}/keys/${apiKeyId}/revoke`, revokeData, auth.token);
        apiPortalMetrics.revokeApiKeyDuration.add(Date.now() - startTime);

        sleep(0.3);
      });
    }

  });

  sleep(1);
}

export function setup() {
  console.log('API Portal Service CRUD Baseline Test - Starting');
  console.log('=================================================');

  const healthCheck = http.get(`${BASE_URLS.gateway}${SERVICE_ENDPOINTS.apiPortal}/health`);
  console.log(`API Portal Service Health: ${healthCheck.status}`);

  return {
    startTime: Date.now(),
    testName: 'api-portal-service-crud'
  };
}

export function teardown(data) {
  console.log('\n=================================================');
  console.log('API Portal Service CRUD Baseline Test - Complete');
  console.log(`Duration: ${(Date.now() - data.startTime) / 1000}s`);
}
