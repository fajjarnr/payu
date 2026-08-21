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
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://gateway-service:8080';

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
    if (res.status === 200) {
      break;
    }
  }

  check(res, {
    'service is reachable and responding': (r) => r && r.status < 500,
  });

  sleep(0.5);
}
