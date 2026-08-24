/**
 * PayU Universal Smoke Test — k6 Load Testing
 *
 * Probes Spring Boot (/actuator/health), Quarkus (/q/health), FastAPI (/health),
 * Next.js (/api/health), or root endpoint for any PayU microservice.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 5 },
    { duration: '10s', target: 5 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1500'],
    // ponytail: dev 0.90 allows transient 503 during rolling update; prod gates use 0.99 via separate strict profile
    checks: ['rate>0.90'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://gateway-service:8080';

// ponytail: simple readiness wait (10s) avoids racing a still-starting pod after argocd-sync reports Progressing
export function setup() {
  const maxAttempts = 6;
  for (let i = 0; i < maxAttempts; i++) {
    const r = http.get(`${BASE_URL}/actuator/health/liveness`);
    if (r.status === 200) return;
    const q = http.get(`${BASE_URL}/q/health/live`);
    if (q.status === 200) return;
    const h = http.get(`${BASE_URL}/health`);
    if (h.status === 200) return;
    const a = http.get(`${BASE_URL}/api/health`);
    if (a.status === 200) return;
    sleep(5);
  }
}

export default function () {
  const endpoints = [
    '/actuator/health/liveness',
    '/actuator/health',
    '/q/health/live',
    '/q/health',
    '/health',
    '/api/health',
    '/'
  ];

  let res = null;
  for (const ep of endpoints) {
    res = http.get(`${BASE_URL}${ep}`);
    if (res.status >= 200 && res.status < 500) {
      break;
    }
  }

  check(res, {
    'service is reachable and responding': (r) => r && r.status > 0 && r.status < 500,
  });

  sleep(0.5);
}
