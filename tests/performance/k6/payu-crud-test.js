import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 30 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.05'],
    checks: ['rate>0.95'],
  },
};

// All configuration from environment variables (populated via ConfigMap/Secret)
const GATEWAY_URL = __ENV.GATEWAY_URL;
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL;
const WEBAPP_URL = __ENV.WEBAPP_URL;
const KEYCLOAK_CLIENT_ID = __ENV.KEYCLOAK_CLIENT_ID;
const KEYCLOAK_CLIENT_SECRET = __ENV.KEYCLOAK_CLIENT_SECRET;
const KEYCLOAK_REALM = __ENV.KEYCLOAK_REALM;
const TEST_USERNAME = __ENV.TEST_USERNAME;
const TEST_PASSWORD = __ENV.TEST_PASSWORD;

function validateConfig() {
  const required = [
    ['GATEWAY_URL', GATEWAY_URL],
    ['KEYCLOAK_URL', KEYCLOAK_URL],
    ['KEYCLOAK_CLIENT_ID', KEYCLOAK_CLIENT_ID],
    ['KEYCLOAK_CLIENT_SECRET', KEYCLOAK_CLIENT_SECRET],
    ['TEST_USERNAME', TEST_USERNAME],
    ['TEST_PASSWORD', TEST_PASSWORD],
  ];

  for (const [name, value] of required) {
    if (!value) {
      throw new Error(`Required environment variable ${name} is not set. Please configure via ConfigMap/Secret.`);
    }
  }
}

export function setup() {
  validateConfig();

  console.log('=== PayU Realistic E2E CRUD Test ===');
  console.log(`Gateway: ${GATEWAY_URL}`);
  console.log(`Keycloak: ${KEYCLOAK_URL}`);
  console.log(`Realm: ${KEYCLOAK_REALM}`);
  console.log(`Client ID: ${KEYCLOAK_CLIENT_ID}`);
  console.log(`Test User: ${TEST_USERNAME}`);

  // Single login in setup to avoid rate limiting
  const loginRes = http.post(
    `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`,
    {
      grant_type: 'password',
      client_id: KEYCLOAK_CLIENT_ID,
      client_secret: KEYCLOAK_CLIENT_SECRET,
      username: TEST_USERNAME,
      password: TEST_PASSWORD,
    },
    {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      tags: { endpoint: 'auth-login' },
    }
  );

  const loginSuccess = check(loginRes, {
    'setup login status is 200': (r) => r.status === 200,
    'setup login returns access_token': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.access_token !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  if (!loginSuccess) {
    console.error(`Setup login failed: ${loginRes.status} - ${loginRes.body}`);
    return { token: null };
  }

  const token = JSON.parse(loginRes.body).access_token;
  console.log(`Setup login successful, token: ${token.length} chars`);

  return { token };
}

export default function (data) {
  if (!data.token) {
    console.error('No token available, skipping iteration');
    return;
  }

  const token = data.token;
  const authHeaders = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };

  // === BACKEND: Health with auth context ===
  const healthRes = http.get(`${GATEWAY_URL}/q/health`, {
    headers: authHeaders,
    tags: { endpoint: 'gateway-health' },
  });

  check(healthRes, {
    'health is 200': (r) => r.status === 200,
    'health < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.3);

  // === BACKEND: GET profile (READ) ===
  const profileRes = http.get(`${GATEWAY_URL}/api/v1/auth/validate`, {
    headers: authHeaders,
    tags: { endpoint: 'auth-profile' },
  });

  check(profileRes, {
    'profile read is 200': (r) => r.status === 200,
    'profile read < 1000ms': (r) => r.timings.duration < 1000,
  });

  sleep(0.3);

  // === BACKEND: GET wallet balance (READ) ===
  const walletRes = http.get(`${GATEWAY_URL}/api/v1/wallets`, {
    headers: authHeaders,
    tags: { endpoint: 'wallet-balance' },
  });

  check(walletRes, {
    'wallet read is 200/201/404': (r) => r.status === 200 || r.status === 201 || r.status === 404,
    'wallet read < 1000ms': (r) => r.timings.duration < 1000,
  });

  sleep(0.3);

  // === BACKEND: GET pockets (READ) ===
  const pocketsRes = http.get(`${GATEWAY_URL}/api/v1/wallets/pockets`, {
    headers: authHeaders,
    tags: { endpoint: 'pockets-list' },
  });

  check(pocketsRes, {
    'pockets read is 200/404': (r) => r.status === 200 || r.status === 404,
    'pockets read < 1000ms': (r) => r.timings.duration < 1000,
  });

  sleep(0.3);

  // === BACKEND: Simulate CREATE (POST to accounts) ===
  const createPayload = JSON.stringify({
    email: `user-${randomString(8)}@payu.test`,
    phoneNumber: `+628${randomIntBetween(100000000, 999999999)}`,
    fullName: `Test User ${randomString(5)}`,
    password: 'SecurePass123!',
  });

  const createRes = http.post(`${GATEWAY_URL}/api/v1/accounts`, createPayload, {
    headers: authHeaders,
    tags: { endpoint: 'account-create' },
  });

  check(createRes, {
    'CREATE responds (201/403/409)': (r) =>
      r.status === 201 || r.status === 200 || r.status === 403 || r.status === 409,
    'CREATE < 2s': (r) => r.timings.duration < 2000,
  });

  sleep(0.5);

  // === FRONTEND: Web-app home page ===
  const webappUrl = WEBAPP_URL || 'https://web-app-payu-dev.apps.payu.ocp.fajjjar.my.id';
  const webappRes = http.get(webappUrl);

  check(webappRes, {
    'WEBAPP home is 200': (r) => r.status === 200,
    'WEBAPP home < 2s': (r) => r.timings.duration < 2000,
  });

  sleep(1);
}

export function teardown(data) {
  console.log('=== Test Complete ===');
}
