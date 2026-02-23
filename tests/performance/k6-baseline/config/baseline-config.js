// PayU Platform - K6 Baseline Performance Test Configuration
// ============================================================
// Configuration for establishing performance baselines per service

export const BASE_URLS = {
  gateway: 'https://gateway-payu-dev.apps.payu.ocp.fajjjar.my.id',
  keycloak: 'https://keycloak-payu-dev.apps.payu.ocp.fajjjar.my.id',
  webApp: 'https://payu-dev.apps.payu.ocp.fajjjar.my.id'
};

// Service endpoints mapping
export const SERVICE_ENDPOINTS = {
  // Core Services
  account: '/api/v1/accounts',
  auth: '/api/v1/auth',
  wallet: '/api/v1/wallets',
  transaction: '/api/v1/transactions',

  // Financial Services
  investment: '/api/v1/investments',
  lending: '/api/v1/lending',
  fx: '/api/v1/fx',
  billing: '/api/v1/billers',
  statement: '/api/v1/statements',

  // Supporting Services
  notification: '/api/v1/notifications',
  partner: '/api/v1/partners',
  promotion: '/api/v1/promotions',
  support: '/api/v1/support',
  compliance: '/api/v1/compliance',
  backoffice: '/api/v1/backoffice',
  cms: '/api/v1/contents',
  abTesting: '/api/v1/experiments',
  apiPortal: '/api/v1/docs',
  kyc: '/api/v1/kyc',
  analytics: '/api/v1/analytics'
};

// Baseline test thresholds (production grade)
export const BASELINE_THRESHOLDS = {
  // Response time SLIs
  http_req_duration: [
    { threshold: 'p(50)<100', abortOnFail: false },   // 50% under 100ms
    { threshold: 'p(95)<300', abortOnFail: false },   // 95% under 300ms
    { threshold: 'p(99)<500', abortOnFail: false },   // 99% under 500ms
    { threshold: 'avg<200', abortOnFail: false }      // average under 200ms
  ],
  // Error rate (strict for baseline)
  http_req_failed: ['rate<0.001'],                    // 0.1% error rate
  // Throughput baseline
  http_reqs: ['rate>50'],
  // CRUD specific thresholds
  crud_create_duration: ['p(95)<400'],
  crud_read_duration: ['p(95)<200'],
  crud_update_duration: ['p(95)<400'],
  crud_delete_duration: ['p(95)<300']
};

// Baseline load profile (moderate load for baseline establishment)
export const BASELINE_STAGES = [
  { duration: '30s', target: 5 },     // Warm up
  { duration: '2m', target: 10 },     // Baseline load
  { duration: '5m', target: 20 },     // Sustained baseline
  { duration: '2m', target: 10 },     // Ramp down
  { duration: '30s', target: 0 }      // Cool down
];

// Test users for authentication
export const TEST_USERS = [
  { username: 'customer1', password: 'password123' },
  { username: 'customer2', password: 'password123' },
  { username: 'admin', password: 'admin123' }
];

// Content types
export const CONTENT_TYPE = {
  JSON: { 'Content-Type': 'application/json' },
  FORM: { 'Content-Type': 'application/x-www-form-urlencoded' }
};

// Common check functions
export const CHECKS = {
  isStatus200: (response) => response.status === 200,
  isStatus201: (response) => response.status === 201,
  isStatus204: (response) => response.status === 204,
  isStatus2xx: (response) => response.status >= 200 && response.status < 300,
  isStatusSuccess: (response) => [200, 201, 202, 204].includes(response.status),
  hasValidJson: (response) => {
    try {
      JSON.parse(response.body);
      return true;
    } catch {
      return false;
    }
  },
  responseTimeFast: (response) => response.timings.duration < 300,
  responseTimeMedium: (response) => response.timings.duration < 500
};

// Service-specific expected response times (ms)
export const SERVICE_SLA = {
  // Core services - strict SLAs
  account: { p50: 100, p95: 300, p99: 500 },
  auth: { p50: 80, p95: 200, p99: 400 },
  wallet: { p50: 100, p95: 300, p99: 500 },
  transaction: { p50: 150, p95: 400, p99: 800 },

  // Financial services
  investment: { p50: 150, p95: 400, p99: 800 },
  lending: { p50: 200, p95: 500, p99: 1000 },
  fx: { p50: 100, p95: 300, p99: 500 },
  billing: { p50: 150, p95: 400, p99: 800 },
  statement: { p50: 200, p95: 500, p99: 1000 },

  // Supporting services
  notification: { p50: 100, p95: 300, p99: 500 },
  partner: { p50: 150, p95: 400, p99: 800 },
  promotion: { p50: 100, p95: 300, p99: 500 },
  support: { p50: 150, p95: 400, p99: 800 },
  compliance: { p50: 200, p95: 500, p99: 1000 },
  backoffice: { p50: 150, p95: 400, p99: 800 },
  cms: { p50: 100, p95: 300, p99: 500 },
  abTesting: { p50: 100, p95: 300, p99: 500 },
  apiPortal: { p50: 100, p95: 300, p99: 500 },
  kyc: { p50: 200, p95: 500, p99: 1000 },
  analytics: { p50: 300, p95: 800, p99: 1500 }
};
