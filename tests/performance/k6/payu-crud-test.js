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

const BACKEND_URL = __ENV.BACKEND_URL || 'http://account-service.payu-dev.svc.cluster.local:8080';
const WEBAPP_URL = __ENV.WEBAPP_URL || 'https://web-app-payu-dev.apps.payu.ocp.fajjjar.my.id';

export default function () {
  // === BACKEND: Health / Liveness (READ) ===
  const livenessRes = http.get(BACKEND_URL + '/actuator/health/liveness');
  check(livenessRes, {
    'BACKEND liveness is 200': function (r) { return r.status === 200; },
    'BACKEND liveness < 500ms': function (r) { return r.timings.duration < 500; },
  });

  sleep(0.3);

  // === BACKEND: Readiness (READ) ===
  const readinessRes = http.get(BACKEND_URL + '/actuator/health/readiness');
  check(readinessRes, {
    'BACKEND readiness is 200': function (r) { return r.status === 200; },
    'BACKEND readiness < 500ms': function (r) { return r.timings.duration < 500; },
  });

  sleep(0.3);

  // === BACKEND: Info (READ) ===
  const infoRes = http.get(BACKEND_URL + '/actuator/info');
  check(infoRes, {
    'BACKEND info is 200': function (r) { return r.status === 200; },
    'BACKEND info < 500ms': function (r) { return r.timings.duration < 500; },
  });

  sleep(0.3);

  // === BACKEND: Simulate CREATE (POST to accounts - expects 401 without auth) ===
  const headers = {
    'Content-Type': 'application/json',
    'X-Idempotency-Key': randomString(16),
  };
  const createPayload = JSON.stringify({
    email: 'user-' + randomString(8) + '@payu.test',
    phoneNumber: '+628' + randomIntBetween(100000000, 999999999),
    fullName: 'Test User ' + randomString(5),
    password: 'SecurePass123!',
  });
  const createRes = http.post(BACKEND_URL + '/api/v1/accounts', createPayload, { headers });
  check(createRes, {
    'BACKEND CREATE responds (201/401)': function (r) { return r.status === 201 || r.status === 200 || r.status === 401; },
    'BACKEND CREATE < 1s': function (r) { return r.timings.duration < 1000; },
  });

  sleep(0.3);

  // === FRONTEND: Web-app home page (READ via Route) ===
  const webappRes = http.get(WEBAPP_URL);
  check(webappRes, {
    'WEBAPP home is 200': function (r) { return r.status === 200; },
    'WEBAPP home < 2s': function (r) { return r.timings.duration < 2000; },
  });

  sleep(0.5);

  // === FRONTEND: Web-app static assets ===
  const staticRes = http.get(WEBAPP_URL + '/_next/static/chunks/main.js');
  check(staticRes, {
    'WEBAPP static responds': function (r) { return r.status === 200 || r.status === 404; },
    'WEBAPP static < 1s': function (r) { return r.timings.duration < 1000; },
  });

  sleep(1);
}
