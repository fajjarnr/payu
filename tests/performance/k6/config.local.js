// PayU Platform - k6 Load Test Configuration (Local Environment)
// ================================================================

export const BASE_URLS = {
  gateway: 'http://localhost:8080',
  keycloak: 'http://localhost:8099',
  webApp: 'http://localhost:3000'
};

// Test thresholds based on DORA Elite metrics
export const THRESHOLDS = {
  // Response time thresholds
  http_req_duration: [
    { threshold: 'p(95)<500', abortOnFail: false },
    { threshold: 'p(99)<1000', abortOnFail: false },
    { threshold: 'avg<300', abortOnFail: false }
  ],
  // Error rate threshold (less than 1% for production readiness)
  http_req_failed: ['rate<0.01'],
  // Throughput
  http_reqs: ['rate>100']
};

// Load test stages (ramp up, sustain, ramp down)
export const LOAD_STAGES = {
  smoke: [
    { duration: '1m', target: 1 }
  ],
  load: [
    { duration: '2m', target: 10 },
    { duration: '5m', target: 50 },
    { duration: '10m', target: 100 },
    { duration: '5m', target: 50 },
    { duration: '2m', target: 10 },
    { duration: '1m', target: 0 }
  ],
  stress: [
    { duration: '2m', target: 50 },
    { duration: '5m', target: 200 },
    { duration: '10m', target: 500 },
    { duration: '10m', target: 1000 },
    { duration: '5m', target: 500 },
    { duration: '5m', target: 200 },
    { duration: '2m', target: 50 },
    { duration: '1m', target: 0 }
  ],
  spike: [
    { duration: '1m', target: 10 },
    { duration: '30s', target: 1000 },
    { duration: '5m', target: 1000 },
    { duration: '1m', target: 0 }
  ]
};

// Test users for authentication tests
export const TEST_USERS = [
  { username: 'customer1', password: 'password123' },
  { username: 'customer2', password: 'password123' },
  { username: 'customer3', password: 'password123' },
  { username: 'customer4', password: 'password123' }
];
