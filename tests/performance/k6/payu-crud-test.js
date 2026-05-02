import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.05'],
    checks: ['rate>0.95'],
  },
};

const GATEWAY_URL = __ENV.GATEWAY_URL || 'http://gateway-service.payu-dev.svc.cluster.local:8080';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://payu-keycloak-service.payu-dev.svc.cluster.local:8080';

function getToken() {
  const res = http.post(
    `${KEYCLOAK_URL}/realms/payu/protocol/openid-connect/token`,
    {
      grant_type: 'password',
      client_id: 'payu-backend',
      client_secret: 'payu-backend-secret-2026',
      username: 'customer1',
      password: 'P@ssw0rd123',
    },
    {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      tags: { endpoint: 'auth-login' },
    }
  );

  const success = check(res, {
    'login status is 200': (r) => r.status === 200,
    'login returns access_token': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.access_token !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  if (!success) {
    console.error(`Login failed: ${res.status} - ${res.body}`);
    return null;
  }

  return JSON.parse(res.body).access_token;
}

export default function () {
  // === REALISTIC E2E: Login first ===
  const token = getToken();
  if (!token) {
    return;
  }

  const authHeaders = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };

  sleep(0.5);

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

  // === BACKEND: Simulate CREATE (POST to accounts - real auth required) ===
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

  // 201 = created, 409 = conflict (user exists), 403 = forbidden, 401 = unauthorized
  check(createRes, {
    'CREATE responds (201/403/409)': (r) =>
      r.status === 201 || r.status === 200 || r.status === 403 || r.status === 409,
    'CREATE < 2s': (r) => r.timings.duration < 2000,
  });

  sleep(0.5);

  // === FRONTEND: Web-app home page (via Route) ===
  const WEBAPP_URL = __ENV.WEBAPP_URL || 'https://web-app-payu-dev.apps.payu.ocp.fajjjar.my.id';
  const webappRes = http.get(WEBAPP_URL);

  check(webappRes, {
    'WEBAPP home is 200': (r) => r.status === 200,
    'WEBAPP home < 2s': (r) => r.timings.duration < 2000,
  });

  sleep(1);
}

export function setup() {
  console.log('=== PayU Realistic E2E CRUD Test ===');
  console.log(`Gateway: ${GATEWAY_URL}`);
  console.log(`Keycloak: ${KEYCLOAK_URL}`);
  console.log('Using customer1 / P@ssw0rd123');
  return { startTime: new Date().toISOString() };
}
