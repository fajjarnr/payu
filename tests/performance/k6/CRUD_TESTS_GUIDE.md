# PayU K6 CRUD Load Testing Guide

## Overview

This guide covers the comprehensive K6 CRUD load testing suite for the PayU Digital Banking Platform. Unlike basic health check tests, these tests exercise full Create, Read, Update, and Delete operations across all core services.

---

## Why CRUD Load Testing?

### Best Practice Rationale

| Aspect | Health Check Tests | CRUD Tests |
|--------|-------------------|------------|
| **Real-world Load** | Synthetic pings | Actual business operations |
| **Database Impact** | Minimal (cached) | Real queries (JOINs, indexes) |
| **Write vs Read** | Read-only | Mixed R/W (different performance) |
| **Contention** | None | Concurrent updates, locks |
| **Data Consistency** | Not tested | Verified under load |

### What We Test

```
┌─────────────────────────────────────────────────────────────────┐
│                     CRUD Operations Coverage                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Account Service        Wallet Service       Transaction         │
│  ├── CREATE (register)  ├── CREATE (pocket)  ├── CREATE (xfer)   │
│  ├── READ (profile)     ├── READ (balance)   ├── READ (history)  │
│  └── UPDATE (profile)   ├── UPDATE (credit)  └── READ (details)  │
│                         ├── UPDATE (status)                      │
│                         └── DELETE (close)                       │
│                                                                  │
│  Card Service                                                      │
│  ├── CREATE (virtual)                                             │
│  ├── READ (list/details)                                          │
│  └── UPDATE (freeze/unfreeze)                                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Test Suite Structure

```
tests/performance/k6/
├── lib/                              # Reusable libraries
│   ├── auth.js                       # Authentication helpers
│   ├── wallet.js                     # Wallet/Pocket CRUD
│   ├── transaction.js                # Transaction CRUD
│   └── card.js                       # Card CRUD
│
├── smoke-test.js                     # Basic health check (30s)
├── load-test.js                      # Basic load test (25min)
├── stress-test.js                    # Basic stress test (40min)
│
├── crud-load-test.js                 # CRUD load test (25min)
├── crud-stress-test.js               # CRUD stress test (40min)
├── crud-data-consistency-test.js     # Consistency test (25min)
│
├── config.js                         # OpenShift endpoints
├── config.local.js                   # Local endpoints
├── run-all-tests.sh                  # Automated runner
├── RUNBOOK.md                        # Execution guide
└── CRUD_TESTS_GUIDE.md               # This file
```

---

## Quick Start

### 1. Prerequisites

```bash
# K6 must be installed
k6 version

# Verify endpoints
./run-all-tests.sh --smoke
```

### 2. Run Individual CRUD Tests

```bash
cd /home/ubuntu/payu/tests/performance/k6

# Quick CRUD verification (2 min)
k6 run crud-load-test.js --tag testType=smoke-quick

# Full CRUD load test (25 min)
k6 run crud-load-test.js

# CRUD stress test (40 min) - breaking point
k6 run crud-stress-test.js

# Data consistency test (25 min)
k6 run crud-data-consistency-test.js
```

### 3. Run All Tests via Runner

```bash
# All tests including CRUD (~120 min)
./run-all-tests.sh

# CRUD tests only (~90 min)
./run-all-tests.sh --crud

# Consistency test only (~25 min)
./run-all-tests.sh --consistency

# Against local environment
./run-all-tests.sh --local --crud
```

---

## Test Specifications

### CRUD Load Test (`crud-load-test.js`)

| Parameter | Value |
|-----------|-------|
| Duration | ~25 minutes |
| Max VU | 100 users |
| Stages | 6 (ramp up, sustain, ramp down) |
| Success Threshold | CREATE >95%, READ >99%, UPDATE >95%, DELETE >90% |

**Operations Flow per VU:**
```javascript
1. Login → Get Profile (READ)
2. Update Profile (UPDATE)
3. List Wallets (READ)
4. Create Pocket (CREATE)
5. Credit Pocket (UPDATE)
6. Get Pocket Details (READ)
7. Freeze → Unfreeze Pocket (UPDATE x2)
8. Close Pocket (DELETE)
9. Create Transfer (CREATE)
10. Get Transaction Details (READ)
11. List Transaction History (READ)
12. Create Virtual Card (CREATE)
13. Freeze → Unfreeze Card (UPDATE x2)
```

### CRUD Stress Test (`crud-stress-test.js`)

| Parameter | Value |
|-----------|-------|
| Duration | ~40 minutes |
| Max VU | 1000 users |
| Stages | 8 (progressive load to breaking point) |

**Weighted Operations (Real-world Mix):**
- 40% - Read operations (profile, wallets, history)
- 25% - Create operations (pockets)
- 20% - Transfer operations (high impact)
- 15% - Card operations

### Data Consistency Test (`crud-data-consistency-test.js`)

| Test | Description | Target |
|------|-------------|--------|
| **Read-After-Write** | Create pocket → immediately read | >99% consistency |
| **Transaction Atomicity** | Transfer → verify ID retrieval | >99.9% atomicity |
| **Concurrent Updates** | Multiple rapid credits → verify sum | No race conditions |

---

## CRUD Library API

### Authentication (`lib/auth.js`)

```javascript
import { login, registerUser, getProfile, updateProfile } from './lib/auth.js';

// Login and get token
const token = login(keycloakUrl, 'customer1', 'password123');

// Register new user
const result = registerUser(gatewayUrl, {
  username: 'newuser',
  email: 'new@example.com',
  password: 'pass123',
  fullName: 'New User',
  phoneNumber: '081234567890',
  nik: '1234567890123456'
});

// Get profile
const profile = getProfile(gatewayUrl, token);

// Update profile
const success = updateProfile(gatewayUrl, token, {
  fullName: 'Updated Name'
});
```

### Wallet/Pocket (`lib/wallet.js`)

```javascript
import { createPocket, creditPocket, closePocket } from './lib/wallet.js';

// Create pocket
const result = createPocket(gatewayUrl, token, {
  name: 'My Pocket',
  description: 'Savings',
  currency: 'IDR',
  targetAmount: 1000000
});

// Credit pocket
const success = creditPocket(gatewayUrl, token, pocketId, 100000);

// Get pocket details
const pocket = getPocket(gatewayUrl, token, pocketId);

// Update status (freeze/unfreeze)
updatePocketStatus(gatewayUrl, token, pocketId, 'FROZEN');
updatePocketStatus(gatewayUrl, token, pocketId, 'ACTIVE');

// Close pocket
closePocket(gatewayUrl, token, pocketId);
```

### Transaction (`lib/transaction.js`)

```javascript
import { createTransfer, getTransactionHistory } from './lib/transaction.js';

// Create transfer
const result = createTransfer(gatewayUrl, token, {
  sourceWalletId: 'wallet-123',
  destinationAccountId: 'customer2',
  amount: 50000,
  description: 'Payment'
});

// Get transaction history
const history = getTransactionHistory(gatewayUrl, token, {
  page: 0,
  size: 10
});

// Get transaction details
const details = getTransactionDetails(gatewayUrl, token, transactionId);

// Cancel pending transaction
cancelTransaction(gatewayUrl, token, transactionId);
```

### Card (`lib/card.js`)

```javascript
import { createVirtualCard, freezeCard } from './lib/card.js';

// Create virtual card
const result = createVirtualCard(gatewayUrl, token, {
  cardHolderName: 'John Doe',
  dailyLimit: 5000000,
  monthlyLimit: 50000000,
  walletId: 'wallet-123'
});

// Freeze/unfreeze
freezeCard(gatewayUrl, token, cardId);
unfreezeCard(gatewayUrl, token, cardId);

// Update limits
updateCardLimits(gatewayUrl, token, cardId, {
  dailyLimit: 10000000,
  monthlyLimit: 100000000
});
```

---

## Metrics Explained

### Performance Metrics

| Metric | Description | Target (Load) | Target (Stress) |
|--------|-------------|---------------|-----------------|
| `http_req_duration` | Response time | p95 < 500ms | p95 < 5000ms |
| `http_req_failed` | Error rate | < 1% | < 50% |
| `crud_create_duration` | CREATE op time | < 500ms | < 2000ms |
| `crud_read_duration` | READ op time | < 300ms | < 1000ms |
| `crud_update_duration` | UPDATE op time | < 500ms | < 1500ms |
| `crud_delete_duration` | DELETE op time | < 500ms | < 1500ms |

### Business Metrics

| Metric | Description | Use Case |
|--------|-------------|----------|
| `transfer_amount_total` | Total transferred | Capacity planning |
| `pocket_created_total` | Pockets created | Resource tracking |
| `card_created_total` | Cards created | Resource tracking |

### Consistency Metrics

| Metric | Description | Target |
|--------|-------------|--------|
| `read_after_write_consistency` | % reads finding written data | > 99% |
| `transaction_atomicity` | % atomic transactions | > 99.9% |
| `concurrent_update_consistency` | No race conditions | 100% |

---

## Analyzing Results

### Example Output Analysis

```bash
# Run test with JSON output
k6 run crud-load-test.js --out json=results.json

# Or use InfluxDB/Grafana integration
k6 run crud-load-test.js --out influxdb=http://localhost:8086/k6
```

### Key Indicators

**Good Performance:**
```
✓ crud_create_success: 97.5%  (>95% target)
✓ crud_read_success: 99.8%    (>99% target)
✓ crud_update_success: 96.2%  (>95% target)
✓ http_req_duration: p95=420ms (<500ms target)
```

**Warning Signs:**
```
⚠ crud_create_success: 88%     (<95% target)
⚠ http_req_duration: p95=850ms (>500ms target)
⚠ consistency_errors: 15       (>10 threshold)
```

**Breaking Point Detected:**
```
✗ http_req_failed: 45%         (approaching 50%)
✗ crud_create_success: 65%     (<80% stress threshold)
✗ http_req_duration: p95=8s    (>5s threshold)
```

---

## Troubleshooting

### Issue: High Error Rate on CREATE

**Possible Causes:**
- Database connection pool exhausted
- Duplicate key violations (username/email)
- Foreign key constraint failures

**Debug:**
```bash
# Check database connections
kubectl exec -it postgres-pod -- psql -c "SELECT count(*) FROM pg_stat_activity;"

# Check application logs
kubectl logs -f deployment/account-service
```

### Issue: Read-After-Write Inconsistency

**Possible Causes:**
- Read replicas lag
- Caching issues
- Transaction isolation level

**Fix:**
- Verify read-from-primary for critical reads
- Check cache invalidation
- Review transaction boundaries

### Issue: Transfer Failures

**Possible Causes:**
- Insufficient balance
- Idempotency key conflicts
- Wallet not found

**Debug:**
```bash
# Check wallet balance
curl -H "Authorization: Bearer $TOKEN" \
  $GATEWAY/api/v1/wallets/$WALLET_ID
```

---

## Integration with CI/CD

### GitLab CI Example

```yaml
load-test:
  stage: performance
  image: grafana/k6:latest
  script:
    - k6 run --out json=results.json tests/performance/k6/crud-load-test.js
  artifacts:
    paths:
      - results.json
  only:
    - main
```

### Threshold Gates

```javascript
// In test options
thresholds: {
  'crud_create_success': ['rate>0.95'],  // Fail CI if <95%
  'crud_read_success': ['rate>0.99'],
  'http_req_duration': ['p(95)<500'],
  'consistency_errors': ['count<5']      // Max 5 errors
}
```

---

## Next Steps

1. **Run smoke tests** to verify setup
2. **Execute load tests** to establish baseline
3. **Analyze results** for bottlenecks
4. **Define SLOs** based on metrics
5. **Set up CI/CD integration** for regression testing
6. **Schedule periodic stress tests** for capacity planning

---

## References

- [K6 Documentation](https://k6.io/docs/)
- [Performance Testing Best Practices](https://k6.io/docs/testing-guides/running-large-tests/)
- [PayU Architecture](../../docs/architecture/ARCHITECTURE.md)
- [API Documentation](../../backend/api-portal-service/)
