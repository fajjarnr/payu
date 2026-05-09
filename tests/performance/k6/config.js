// PayU Platform - k6 Load Test Configuration
// ===========================================

const gatewayUrl = __ENV.GATEWAY_URL || 'http://gateway-service:8080';
const keycloakUrl = __ENV.KEYCLOAK_URL || 'http://payu-keycloak-service.payu-sso.svc.cluster.local:8080';
const webAppUrl = __ENV.WEB_APP_URL || 'http://web-app:3000';
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
    { threshold: 'p(95)<500', abortOnFail: false }, // 95% under 500ms
    { threshold: 'p(99)<1000', abortOnFail: false }, // 99% under 1s
    { threshold: 'avg<300', abortOnFail: false }     // average under 300ms
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
    { duration: '2m', target: 10 },   // Ramp up to 10 users
    { duration: '5m', target: 50 },   // Ramp up to 50 users
    { duration: '10m', target: 100 }, // Sustain 100 users
    { duration: '5m', target: 50 },   // Ramp down
    { duration: '2m', target: 10 },   // Ramp down
    { duration: '1m', target: 0 }     // Cool down
  ],
  stress: [
    { duration: '2m', target: 50 },    // Ramp up
    { duration: '5m', target: 200 },   // Ramp up to 200 users
    { duration: '10m', target: 500 },  // Ramp up to 500 users
    { duration: '10m', target: 1000 }, // Peak load 1000 users
    { duration: '5m', target: 500 },   // Ramp down
    { duration: '5m', target: 200 },
    { duration: '2m', target: 50 },
    { duration: '1m', target: 0 }
  ],
  spike: [
    { duration: '1m', target: 10 },
    { duration: '30s', target: 1000 }, // Sudden spike
    { duration: '5m', target: 1000 },  // Sustain
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
// NOTE: These must match users seeded by scripts/keycloak-seeder.sh
export const TEST_USERS = [
  { username: 'customer2', password: __ENV.K6_EXISTING_TEST_PASSWORD || 'P@ssw0rd123' },
  { username: 'customer1', password: __ENV.K6_EXISTING_TEST_PASSWORD || 'P@ssw0rd123' },
  { username: 'admin', password: __ENV.K6_EXISTING_TEST_PASSWORD || 'P@ssw0rd123' }
];
