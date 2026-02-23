# PayU Platform - K6 Baseline Performance Tests

This directory contains comprehensive K6 baseline performance tests for all PayU microservices, following enterprise-grade load testing standards.

## Overview

These tests establish performance baselines for each service by executing CRUD (Create, Read, Update, Delete) operations against the actual database, measuring response times, throughput, and error rates.

## Architecture

```
k6-baseline/
├── config/
│   └── baseline-config.js          # Shared configuration, thresholds, SLAs
├── lib/
│   ├── auth-helper.js              # Authentication utilities
│   └── crud-helper.js              # Generic CRUD operation helpers
├── core-services/                   # Core banking services tests
│   ├── account-service-crud.js
│   ├── auth-service-crud.js
│   ├── wallet-service-crud.js
│   └── transaction-service-crud.js
├── financial-services/              # Financial products tests
│   ├── investment-service-crud.js
│   ├── lending-service-crud.js
│   ├── fx-service-crud.js
│   ├── billing-service-crud.js
│   └── statement-service-crud.js
├── supporting-services/             # Supporting infrastructure tests
│   ├── notification-service-crud.js
│   ├── partner-service-crud.js
│   ├── promotion-service-crud.js
│   ├── support-service-crud.js
│   ├── compliance-service-crud.js
│   ├── backoffice-service-crud.js
│   ├── cms-service-crud.js
│   ├── ab-testing-service-crud.js
│   ├── api-portal-service-crud.js
│   ├── kyc-service-crud.js
│   └── analytics-service-crud.js
├── run-all-baseline-tests.sh        # Bash runner script
├── unified-baseline-runner.js       # Unified K6 runner
└── README.md                        # This file
```

## Prerequisites

- [k6](https://k6.io/docs/get-started/installation/) (v0.45+)
- Access to PayU API Gateway
- Valid test user credentials

## Quick Start

### Run Individual Service Test

```bash
# Core Services
k6 run core-services/wallet-service-crud.js
k6 run core-services/transaction-service-crud.js

# Financial Services
k6 run financial-services/lending-service-crud.js

# Supporting Services
k6 run supporting-services/notification-service-crud.js
```

### Run All Tests Using Bash Script

```bash
# Run all tests sequentially
./run-all-baseline-tests.sh

# Run specific group
./run-all-baseline-tests.sh -g core
./run-all-baseline-tests.sh -g financial
./run-all-baseline-tests.sh -g supporting

# Run specific services
./run-all-baseline-tests.sh -s wallet,transaction,auth

# Run with custom duration
./run-all-baseline-tests.sh -d 5m

# Run in parallel (use with caution)
./run-all-baseline-tests.sh -p -g core
```

### Run Using Unified Runner

```bash
# Test all services
k6 run unified-baseline-runner.js

# Test specific services
k6 run unified-baseline-runner.js --env SERVICES=wallet,transaction

# Test by group
k6 run unified-baseline-runner.js --env SERVICES=account,auth,wallet,transaction
```

## Configuration

### Baseline Thresholds (SLAs)

| Metric | Target | Abort on Fail |
|:-------|:------:|:-------------:|
| p(50) | < 100ms | No |
| p(95) | < 300ms | No |
| p(99) | < 500ms | No |
| avg | < 200ms | No |
| Error Rate | < 0.1% | Yes |

### Load Profile

```javascript
BASELINE_STAGES = [
  { duration: '30s', target: 5 },     // Warm up
  { duration: '2m', target: 10 },     // Baseline load
  { duration: '5m', target: 20 },     // Sustained baseline
  { duration: '2m', target: 10 },     // Ramp down
  { duration: '30s', target: 0 }      // Cool down
]
```

### Service-Specific SLAs

| Service | p50 | p95 | p99 |
|:--------|:---:|:---:|:---:|
| auth | 80ms | 200ms | 400ms |
| wallet | 100ms | 300ms | 500ms |
| transaction | 150ms | 400ms | 800ms |
| lending | 200ms | 500ms | 1000ms |
| analytics | 300ms | 800ms | 1500ms |

See `config/baseline-config.js` for complete SLA definitions.

## Test Structure

Each test file follows this pattern:

```javascript
// 1. Service-specific metrics
const serviceMetrics = {
  operationNameDuration: new Trend('service_operation_duration'),
  // ...
};

// 2. Test configuration
export const options = {
  stages: BASELINE_STAGES,
  thresholds: BASELINE_THRESHOLDS
};

// 3. Test data generators
function generateTestData(uniqueId) {
  return { /* ... */ };
}

// 4. Main test scenario
export default function () {
  const auth = login(__VU % 5);

  group('Service - CRUD Operations', () => {
    // CREATE operation
    group('CREATE: Operation Name', () => {
      const result = create(endpoint, payload, auth.token);
      serviceMetrics.operationNameDuration.add(duration);
    });

    // READ operation
    group('READ: Operation Name', () => {
      const result = read(endpoint, auth.token);
      // ...
    });

    // UPDATE operation
    group('UPDATE: Operation Name', () => {
      const result = update(endpoint, payload, auth.token);
      // ...
    });

    // DELETE operation
    group('DELETE: Operation Name', () => {
      const result = del(endpoint, auth.token);
      // ...
    });
  });
}

// 5. Setup/Teardown
export function setup() { /* ... */ }
export function teardown(data) { /* ... */ }
```

## Metrics Collected

### Standard Metrics
- `http_req_duration` - Request duration
- `http_req_failed` - Failed request rate
- `http_reqs` - Request rate

### CRUD-Specific Metrics
- `crud_create_duration` - Create operation latency
- `crud_read_duration` - Read operation latency
- `crud_update_duration` - Update operation latency
- `crud_delete_duration` - Delete operation latency

### Service-Specific Metrics
Each service defines custom metrics for its unique operations:
- `wallet_credit_duration`
- `transaction_transfer_duration`
- `lending_apply_loan_duration`
- etc.

## Test Output

### Console Output
```
     ✓ create: status 201 or 200
     ✓ create: valid JSON response
     ✓ read: status 200
     ✓ update: status 200 or 204

     checks.........................: 100.00% ✓ 1250      ✗ 0
     data_received..................: 2.5 MB  8.3 kB/s
     data_sent......................: 1.2 MB  4.0 kB/s
     http_req_blocked...............: avg=1.23ms   min=0s      med=0s      max=150ms
     http_req_connecting............: avg=0.89ms   min=0s      med=0s      max=120ms
     http_req_duration..............: avg=145.23ms min=12ms    med=89ms    max=2.5s
       { expected_response:true }...: avg=145.23ms min=12ms    med=89ms    max=2.5s
     http_req_failed................: 0.00%   ✓ 0         ✗ 1250
     http_req_receiving.............: avg=0.45ms   min=0s      med=0.3ms   max=15ms
     http_req_sending...............: avg=0.12ms   min=0s      med=0.1ms   max=5ms
     http_req_waiting...............: avg=144.66ms min=12ms    med=88ms    max=2.5s
     http_reqs......................: 1250    4.17/s
```

### JSON Output
Run with `--out json=results.json` for detailed analysis.

## Interpreting Results

### Pass Criteria
1. All checks pass (status codes, JSON validity)
2. Error rate < 0.1%
3. Response times meet SLA targets:
   - p50 < 100ms (core services)
   - p95 < 300ms (core services)
   - p99 < 500ms (core services)

### Baseline Establishment
Run tests 3-5 times to establish stable baselines. Variations > 10% between runs warrant investigation.

## Troubleshooting

### "Login failed" errors
- Verify Keycloak is running and accessible
- Check test user credentials in `config/baseline-config.js`

### High error rates
- Check service health endpoints
- Verify database connectivity
- Review rate limiting settings

### Timeout errors
- Increase test duration for slower services (analytics, statements)
- Check network connectivity to gateway

## Continuous Integration

Add to CI pipeline:

```yaml
# .github/workflows/baseline-tests.yml
name: Baseline Performance Tests

on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  workflow_dispatch:

jobs:
  baseline:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup k6
        uses: grafana/setup-k6-action@v1
      - name: Run baseline tests
        run: ./run-all-baseline-tests.sh -g core
      - name: Upload results
        uses: actions/upload-artifact@v3
        with:
          name: baseline-results
          path: results/
```

## Contributing

When adding new service tests:

1. Create file in appropriate directory
2. Follow existing test structure
3. Define service-specific metrics
4. Add to `run-all-baseline-tests.sh` service lists
5. Update this README

## References

- [k6 Documentation](https://k6.io/docs/)
- [k6 Metrics](https://k6.io/docs/using-k6/metrics/)
- [Performance Testing Best Practices](https://k6.io/docs/testing-guides/)
