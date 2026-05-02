// PayU Platform - Load Test
// =========================
// Sustained load test to validate platform performance
// Run: k6 run load-test.js

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URLS, THRESHOLDS, LOAD_STAGES, TEST_USERS } from './config.js';

// Custom metrics
const authSuccessRate = new Rate('auth_success_rate');
const apiResponseTime = new Trend('api_response_time');
const errorCounter = new Counter('errors');

export const options = {
  stages: LOAD_STAGES.load,
  thresholds: THRESHOLDS,
  tags: {
    testType: 'load',
    environment: 'dev'
  }
};

export default function () {
  const gatewayUrl = BASE_URLS.gateway;
  const keycloakUrl = BASE_URLS.keycloak;

  // Pick random user for auth tests
  const user = TEST_USERS[Math.floor(Math.random() * TEST_USERS.length)];

  group('Authentication Flow', () => {
    // Get token from Keycloak
    const tokenResponse = http.post(
      `${keycloakUrl}/realms/payu/protocol/openid-connect/token`,
      {
        grant_type: 'password',
        client_id: 'payu-backend',
        username: user.username,
        password: user.password
      },
      {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        tags: { endpoint: 'auth-login' }
      }
    );

    let authSuccess = false;
    let token = null;
    if (tokenResponse.status === 200) {
      try {
        const body = JSON.parse(tokenResponse.body);
        token = body.access_token;
        authSuccess = token !== undefined;
      } catch (e) {
        authSuccess = false;
      }
    }
    authSuccessRate.add(authSuccess);
    apiResponseTime.add(tokenResponse.timings.duration);

    check(tokenResponse, {
      'login returns 200 with access_token': (r) => {
        if (r.status !== 200) return false;
        try {
          const body = JSON.parse(r.body);
          return body.access_token !== undefined;
        } catch (e) {
          return false;
        }
      },
      'login time < 2000ms': (r) => r.timings.duration < 2000
    });

    if (!authSuccess) {
      errorCounter.add(1, { type: 'auth_failed', status: tokenResponse.status });
    }
  });

  group('Core Services Health', () => {
    const services = [
      { path: '/api/v1/accounts/public/health', name: 'account-health' },
      { path: '/api/v1/wallets/public/health', name: 'wallet-health' },
      { path: '/api/v1/transactions/public/health', name: 'transaction-health' },
      { path: '/api/v1/auth/public/health', name: 'auth-health' }
    ];

    for (const service of services) {
      const response = http.get(`${gatewayUrl}${service.path}`, {
        tags: { endpoint: service.name }
      });

      apiResponseTime.add(response.timings.duration);

      check(response, {
        [`${service.name} responds`]: (r) =>
          r.status === 200 || r.status === 401 || r.status === 404,
        [`${service.name} time < 1000ms`]: (r) => r.timings.duration < 1000
      });

      if (response.status >= 500) {
        errorCounter.add(1, { type: 'server_error', endpoint: service.name });
      }
    }
  });

  group('Public API Endpoints', () => {
    // Test public endpoints that don't require auth
    const publicEndpoints = [
      { path: '/api/v1/fx/rates', name: 'fx-rates' },
      { path: '/api/v1/investments/funds', name: 'investment-funds' }
    ];

    for (const endpoint of publicEndpoints) {
      const response = http.get(`${gatewayUrl}${endpoint.path}`, {
        tags: { endpoint: endpoint.name }
      });

      apiResponseTime.add(response.timings.duration);

      check(response, {
        [`${endpoint.name} responds`]: (r) =>
          r.status === 200 || r.status === 401 || r.status === 404
      });
    }
  });

  sleep(Math.random() * 2 + 1); // Random sleep 1-3 seconds
}

export function setup() {
  console.log('=== PayU Platform Load Test ===');
  console.log(`Target: ${BASE_URLS.gateway}`);
  console.log(`Duration: ~25 minutes`);
  console.log(`Max VUs: 100`);

  // Quick health check
  const health = http.get(`${BASE_URLS.keycloak}/realms/payu/.well-known/openid-configuration`);
  if (health.status !== 200) {
    console.error('WARNING: Keycloak not accessible. Auth tests may fail.');
  }

  return {
    startTime: new Date().toISOString(),
    targetUrl: BASE_URLS.gateway
  };
}

export function teardown(data) {
  console.log('=== Load Test Complete ===');
  console.log(`Target: ${data.targetUrl}`);
  console.log(`Started: ${data.startTime}`);
  console.log(`Ended: ${new Date().toISOString()}`);
}
