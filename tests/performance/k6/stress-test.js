// PayU Platform - Stress Test
// ============================
// Push platform to breaking point to find limits
// Run: k6 run stress-test.js

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URLS, THRESHOLDS, LOAD_STAGES } from './config.js';

// Custom metrics
const errorRate = new Rate('error_rate');
const responseTimeP95 = new Trend('response_time_p95');
const requestsPerSecond = new Counter('requests_per_second');

export const options = {
  stages: LOAD_STAGES.stress,
  thresholds: {
    // Relaxed thresholds for stress test
    http_req_duration: ['p(95)<5000', 'p(99)<10000'],
    http_req_failed: ['rate<0.15'] // Allow up to 15% errors during peak stress
  },
  tags: {
    testType: 'stress',
    environment: 'dev'
  }
};

export default function () {
  const gatewayUrl = BASE_URLS.gateway;
  const keycloakUrl = BASE_URLS.keycloak;

  group('High Load - Gateway', () => {
    const responses = http.batch([
      ['GET', `${gatewayUrl}/q/health`, null, { tags: { endpoint: 'gateway-health' } }],
      ['GET', `${gatewayUrl}/api/v1/accounts/public/health`, null, { tags: { endpoint: 'account-health' } }],
      ['GET', `${gatewayUrl}/api/v1/wallets/public/health`, null, { tags: { endpoint: 'wallet-health' } }],
      ['GET', `${gatewayUrl}/api/v1/transactions/public/health`, null, { tags: { endpoint: 'transaction-health' } }]
    ]);

    for (const response of responses) {
      const success = response.status < 500;
      errorRate.add(!success);
      responseTimeP95.add(response.timings.duration);
      requestsPerSecond.add(1);

      check(response, {
        'response received': (r) => r.status !== 0,
        'no server error (5xx)': (r) => r.status < 500
      });
    }
  });

  group('High Load - Keycloak', () => {
    // Multiple concurrent auth requests
    const authResponses = http.batch([
      ['GET', `${keycloakUrl}/realms/payu/.well-known/openid-configuration`, null, { tags: { endpoint: 'oidc-discovery' } }],
      ['GET', `${keycloakUrl}/realms/payu/.well-known/openid-configuration`, null, { tags: { endpoint: 'oidc-discovery' } }],
      ['GET', `${keycloakUrl}/realms/payu/.well-known/openid-configuration`, null, { tags: { endpoint: 'oidc-discovery' } }]
    ]);

    for (const response of authResponses) {
      errorRate.add(response.status >= 500);
      responseTimeP95.add(response.timings.duration);
      requestsPerSecond.add(1);

      check(response, {
        'keycloak responds': (r) => r.status === 200 || r.status === 429 || r.status === 503
      });
    }
  });

  // Minimal sleep to maximize load
  sleep(Math.random() * 0.5);
}

export function setup() {
  console.log('=== PayU Platform Stress Test ===');
  console.log('WARNING: This test will push the platform to its limits!');
  console.log(`Max VUs: 1000`);
  console.log(`Duration: ~40 minutes`);
  console.log('');
  console.log('Monitoring checkpoints:');
  console.log('  - 50 users: Expected stable');
  console.log('  - 200 users: Watch response times');
  console.log('  - 500 users: May see degradation');
  console.log('  - 1000 users: Finding breaking point');

  return {
    startTime: new Date().toISOString(),
    maxVUs: 1000
  };
}

export function teardown(data) {
  console.log('=== Stress Test Complete ===');
  console.log(`Max VUs attempted: ${data.maxVUs}`);
  console.log(`Started: ${data.startTime}`);
  console.log(`Ended: ${new Date().toISOString()}`);
  console.log('');
  console.log('Review metrics to find:');
  console.log('  - Breaking point (where errors spike)');
  console.log('  - Degradation point (where response times increase)');
  console.log('  - Recovery behavior (after ramp down)');
}
