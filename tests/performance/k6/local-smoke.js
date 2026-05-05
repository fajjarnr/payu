/**
 * PayU Local Smoke Test — k6 Load Testing
 *
 * Usage:
 *   podman compose run --rm k6 run /tests/local-smoke.js
 *
 * This is a minimal smoke test targeting the local gateway-service.
 * Adjust thresholds and endpoints as services come online.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },  // Ramp up
    { duration: '1m', target: 10 },   // Steady state
    { duration: '30s', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://gateway-service:8080';

export default function () {
  const res = http.get(`${BASE_URL}/q/health`);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
