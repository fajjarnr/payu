// PayU Platform - Smoke Test
// ===========================
// Minimal load test to verify platform functionality
// Run: k6 run smoke-test.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URLS, THRESHOLDS } from './config.js';

export const options = {
  stages: [
    { duration: '30s', target: 1 }, // 1 user for 30 seconds
  ],
  thresholds: THRESHOLDS,
  tags: {
    testType: 'smoke',
    environment: 'dev'
  }
};

export default function () {
  const gatewayUrl = BASE_URLS.gateway;
  const keycloakUrl = BASE_URLS.keycloak;

  // Test 1: Gateway health endpoint (Quarkus uses /q/health)
  const gatewayHealth = http.get(`${gatewayUrl}/q/health`, {
    tags: { endpoint: 'gateway-health' }
  });

  check(gatewayHealth, {
    'gateway health status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    'gateway response time < 500ms': (r) => r.timings.duration < 500
  });

  // Test 2: Keycloak OIDC Discovery (Critical for auth)
  const oidcDiscovery = http.get(
    `${keycloakUrl}/realms/payu/.well-known/openid-configuration`,
    { tags: { endpoint: 'keycloak-oidc' } }
  );

  check(oidcDiscovery, {
    'keycloak oidc status is 200': (r) => r.status === 200,
    'keycloak response time < 1000ms': (r) => r.timings.duration < 1000,
    'keycloak returns valid json': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.issuer && body.token_endpoint;
      } catch (e) {
        return false;
      }
    }
  });

    // Test 3: Keycloak token endpoint (real auth test)
    const tokenEndpoint = http.post(
      `${keycloakUrl}/realms/payu/protocol/openid-connect/token`,
      {
        grant_type: 'password',
        client_id: 'payu-backend',
        client_secret: 'payu-backend-secret-2026',
        username: 'customer1',
        password: 'P@ssw0rd123'
      },
      {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        tags: { endpoint: 'keycloak-token' }
      }
    );

    check(tokenEndpoint, {
      'token endpoint returns 200 with access_token': (r) => {
        if (r.status !== 200) return false;
        try {
          const body = JSON.parse(r.body);
          return body.access_token !== undefined;
        } catch (e) {
          return false;
        }
      },
      'token endpoint time < 2000ms': (r) => r.timings.duration < 2000
    });

  // Test 4: Core services via Gateway (public endpoints)
  const services = [
    { path: '/api/v1/accounts/public/health', name: 'account-service' },
    { path: '/api/v1/wallets/public/health', name: 'wallet-service' },
    { path: '/api/v1/transactions/public/health', name: 'transaction-service' }
  ];

  for (const service of services) {
    const response = http.get(`${gatewayUrl}${service.path}`, {
      tags: { endpoint: service.name }
    });

    check(response, {
      [`${service.name} responds`]: (r) => r.status === 200 || r.status === 401 || r.status === 404,
      [`${service.name} time < 1000ms`]: (r) => r.timings.duration < 1000
    });
  }

  sleep(1);
}

// Setup - runs once before tests
export function setup() {
  console.log('=== PayU Platform Smoke Test ===');
  console.log(`Gateway: ${BASE_URLS.gateway}`);
  console.log(`Keycloak: ${BASE_URLS.keycloak}`);

  // Verify platform is accessible (Quarkus gateway uses /q/health)
  const healthCheck = http.get(`${BASE_URLS.gateway}/q/health`);
  console.log(`Gateway health check status: ${healthCheck.status}`);

  return { startTime: new Date().toISOString() };
}

// Teardown - runs once after tests
export function teardown(data) {
  console.log('=== Smoke Test Complete ===');
  console.log(`Started: ${data.startTime}`);
  console.log(`Ended: ${new Date().toISOString()}`);
}
