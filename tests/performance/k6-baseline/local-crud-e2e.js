// PayU Local CRUD E2E Performance Test
// ======================================
// Verified against running local environment (podman compose)
// All 15 services responding 200 through gateway
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// Local config
const GATEWAY = __ENV.GATEWAY_URL || 'http://gateway-service:8080';
const ACCT_ID = __ENV.ACCOUNT_ID || '574e8801-a8da-4272-b6ff-493cd9b71685';

// Local test user (customer1 with ADMIN/USER roles)
const TEST_USER = {
  username: __ENV.TEST_USER || 'customer1',
  password: __ENV.TEST_PASS || 'Test1234!'
};

// Service metrics
const svcDuration = new Trend('crud_service_duration');
const svcErrorRate = new Rate('crud_service_error_rate');

// Stage config
export const options = {
  stages: [
    { duration: '5s', target: 1 },   // warm up
    { duration: '10s', target: 3 },  // ramp up
    { duration: '10s', target: 3 },  // steady
    { duration: '5s', target: 0 },   // cool down
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    http_req_failed: ['rate<0.1'],
  }
};

// ===== AUTH =====
function getToken() {
  const payload = JSON.stringify({
    username: TEST_USER.username,
    password: TEST_USER.password,
  });
  const resp = http.post(`${GATEWAY}/api/v1/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  
  check(resp, { 'login: 200': (r) => r.status === 200 });
  
  try {
    const body = JSON.parse(resp.body);
    return body.data?.access_token || body.access_token || '';
  } catch { return ''; }
}

// ===== READ ENDPOINTS =====
function testRead(token, path) {
  const resp = http.get(`${GATEWAY}${path}`, {
    headers: { 'Authorization': `Bearer ${token}` },
  });
  svcDuration.add(resp.timings.duration);
  svcErrorRate.add(resp.status >= 400 ? 1 : 0);
  return resp;
}

// ===== CREATE =====
function testCreate(token, path, body) {
  const ik = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  const resp = http.post(`${GATEWAY}${path}`, JSON.stringify(body), {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      'Idempotency-Key': ik,
    },
  });
  svcDuration.add(resp.timings.duration);
  svcErrorRate.add(resp.status >= 500 ? 1 : 0);
  return resp;
}

// ===== MAIN TEST =====
export default function () {
  const token = getToken();
  if (!token) { console.error('No token, skipping iteration'); return; }

  // ===== READ: All 15 working services =====
  group('READ - All Services', () => {
    const reads = [
      '/api/v1/accounts',
      `/api/v1/wallets/${ACCT_ID}/balance`,
      '/api/v1/transactions',
      '/api/v1/billers',
      '/api/v1/notifications',
      '/api/v1/fx',
      '/api/v1/statements',
      '/api/v1/promotions',
      '/api/v1/support',
      '/api/v1/lending',
      '/api/v1/investments',
      '/api/v1/partners',
      '/api/v1/disputes',
      '/api/v1/backoffice',
      '/api/v1/integration',
    ];

    for (const path of reads) {
      const r = testRead(token, path);
      check(r, { [`READ ${path}`]: (res) => res.status === 200 });
      sleep(0.1);
    }
  });

  // ===== CREATE: Transfer =====
  group('CREATE - Transfer', () => {
    const r = testCreate(token, '/api/v1/transactions/transfer', {
      senderAccountId: ACCT_ID,
      recipientAccountNumber: '1234567890',
      amount: 10,
      type: 'INTERNAL_TRANSFER',
      description: 'k6-crud-test',
    });
    check(r, { 'CREATE transfer': (res) => res.status < 500 });
  });

  // ===== CREATE: Dispute =====
  group('CREATE - Dispute', () => {
    const r = testCreate(token, '/api/v1/disputes', {
      transactionId: '932db19b-704e-4649-a82c-d69ecf0956ff',
      reason: 'k6-test',
      description: 'k6-crud-dispute',
    });
    check(r, { 'CREATE dispute': (res) => res.status < 500 });
  });

  sleep(1);
}

// ===== SETUP: Verify connectivity =====
export function setup() {
  console.log('=== PayU Local CRUD E2E ===');
  console.log(`Gateway: ${GATEWAY}`);
  
  const health = http.get(`${GATEWAY}/q/health`);
  console.log(`Gateway health: ${health.status}`);
  
  return { healthy: health.status === 200 };
}

// ===== TEARDOWN =====
export function teardown(data) {
  console.log(`Test complete. Gateway healthy: ${data.healthy}`);
}
