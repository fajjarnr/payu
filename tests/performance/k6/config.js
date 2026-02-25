// PayU Platform - k6 Load Test Configuration
// ===========================================

export const BASE_URLS = {
  gateway: 'https://gateway-dev.payu.fajjjar.my.id',
  keycloak: 'https://keycloak-dev.payu.fajjjar.my.id',
  webApp: 'https://dev.payu.fajjjar.my.id'
};

// Test thresholds based on DORA Elite metrics
export const THRESHOLDS = {
  // Response time thresholds
  http_req_duration: [
    { threshold: 'p(95)<500', abortOnFail: false }, // 95% under 500ms
    { threshold: 'p(99)<1000', abortOnFail: false }, // 99% under 1s
    { threshold: 'avg<300', abortOnFail: false }     // average under 300ms
  ],
  // Error rate threshold (less than 1% for production readiness)
  http_req_failed: ['rate<0.01'],
  // Throughput
  http_reqs: ['rate>100']
};

// Load test stages (ramp up, sustain, ramp down)
export const LOAD_STAGES = {
  smoke: [
    { duration: '1m', target: 1 } // 1 user, 1 minute
  ],
  load: [
    { duration: '2m', target: 10 },   // Ramp up to 10 users
    { duration: '5m', target: 50 },   // Ramp up to 50 users
    { duration: '10m', target: 100 }, // Sustain 100 users
    { duration: '5m', target: 50 },   // Ramp down
    { duration: '2m', target: 10 },   // Ramp down
    { duration: '1m', target: 0 }     // Cool down
  ],
  stress: [
    { duration: '2m', target: 50 },    // Ramp up
    { duration: '5m', target: 200 },   // Ramp up to 200 users
    { duration: '10m', target: 500 },  // Ramp up to 500 users
    { duration: '10m', target: 1000 }, // Peak load 1000 users
    { duration: '5m', target: 500 },   // Ramp down
    { duration: '5m', target: 200 },
    { duration: '2m', target: 50 },
    { duration: '1m', target: 0 }
  ],
  spike: [
    { duration: '1m', target: 10 },
    { duration: '30s', target: 1000 }, // Sudden spike
    { duration: '5m', target: 1000 },  // Sustain
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
