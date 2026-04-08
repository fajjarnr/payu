// PayU Platform - k6 Load Test Configuration (Local Environment)
// ================================================================

const gatewayUrl = __ENV.GATEWAY_URL || 'http://localhost:8080';
const keycloakUrl = __ENV.KEYCLOAK_URL || 'http://localhost:8099';
const webAppUrl = __ENV.WEB_APP_URL || 'http://localhost:3000';
const minRequestRate = __ENV.K6_MIN_REQUEST_RATE;

export const BASE_URLS = {
  gateway: gatewayUrl,
  keycloak: keycloakUrl,
  webApp: webAppUrl
};

// Test thresholds based on DORA Elite metrics
export const THRESHOLDS = {
  // Response time thresholds
  http_req_duration: [
    { threshold: 'p(95)<500', abortOnFail: false },
    { threshold: 'p(99)<1000', abortOnFail: false },
    { threshold: 'avg<300', abortOnFail: false }
  ],
  // Error rate threshold (less than 1% for production readiness)
  http_req_failed: ['rate<0.01']
};

if (minRequestRate) {
  THRESHOLDS.http_reqs = [`rate>${minRequestRate}`];
}

// Load test stages (ramp up, sustain, ramp down)
export const LOAD_STAGES = {
  smoke: [
    { duration: __ENV.K6_SMOKE_DURATION || '30s', target: Number(__ENV.K6_SMOKE_TARGET || 1) }
  ],
  load: [
    { duration: '2m', target: 10 },
    { duration: '5m', target: 50 },
    { duration: '10m', target: 100 },
    { duration: '5m', target: 50 },
    { duration: '2m', target: 10 },
    { duration: '1m', target: 0 }
  ],
  stress: [
    { duration: '2m', target: 50 },
    { duration: '5m', target: 200 },
    { duration: '10m', target: 500 },
    { duration: '10m', target: 1000 },
    { duration: '5m', target: 500 },
    { duration: '5m', target: 200 },
    { duration: '2m', target: 50 },
    { duration: '1m', target: 0 }
  ],
  spike: [
    { duration: '1m', target: 10 },
    { duration: '30s', target: 1000 },
    { duration: '5m', target: 1000 },
    { duration: '1m', target: 0 }
  ]
};

export const FEATURE_FLAGS = {
  enableTransactions: __ENV.K6_ENABLE_TRANSACTIONS === '1',
  enableCardCrud: __ENV.K6_ENABLE_CARD_CRUD !== '0'
};

export const SESSION_SETTINGS = {
  tokenRefreshIntervalMs: Number(__ENV.K6_TOKEN_REFRESH_INTERVAL_MS || 240000),
  walletReadyMaxAttempts: Number(__ENV.K6_WALLET_READY_ATTEMPTS || 30),
  walletReadySleepSeconds: Number(__ENV.K6_WALLET_READY_SLEEP_SECONDS || 0.5)
};

// Test users for authentication tests
export const TEST_USERS = [
  { username: 'customer1', password: __ENV.K6_EXISTING_TEST_PASSWORD || 'TestPassword123!' },
  { username: 'customer2', password: __ENV.K6_EXISTING_TEST_PASSWORD || 'TestPassword123!' },
  { username: 'customer3', password: __ENV.K6_EXISTING_TEST_PASSWORD || 'TestPassword123!' },
  { username: 'customer4', password: __ENV.K6_EXISTING_TEST_PASSWORD || 'TestPassword123!' }
];
