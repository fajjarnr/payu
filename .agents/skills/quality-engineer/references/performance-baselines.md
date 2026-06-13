# Performance Testing Baselines

## Overview

Standard performance testing templates dan baseline untuk PayU services menggunakan **Gatling** (Java) dan **k6** (JavaScript).

---

## 🎯 SLA Targets

| Service | P50 Latency | P95 Latency | P99 Latency | Error Rate | TPS |
|---------|-------------|-------------|-------------|------------|-----|
| `account-service` | < 50ms | < 200ms | < 500ms | < 0.1% | 1000 |
| `wallet-service` | < 100ms | < 300ms | < 1s | < 0.01% | 500 |
| `transaction-service` | < 200ms | < 500ms | < 2s | < 0.001% | 200 |
| `auth-service` | < 100ms | < 300ms | < 500ms | < 0.1% | 2000 |

---

## 📊 Gatling Template (Java)

```java
// src/test/java/simulations/WalletServiceSimulation.java
package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class WalletServiceSimulation extends Simulation {

    // Configuration
    private static final String BASE_URL = System.getenv().getOrDefault("BASE_URL", "http://localhost:8080");
    private static final int USERS = Integer.parseInt(System.getenv().getOrDefault("USERS", "100"));
    private static final int DURATION = Integer.parseInt(System.getenv().getOrDefault("DURATION", "300"));

    // HTTP Protocol
    HttpProtocolBuilder httpProtocol = http
        .baseUrl(BASE_URL)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .header("Authorization", "Bearer ${token}");

    // Feeder for test data
    FeederBuilder<String> accountFeeder = csv("accounts.csv").random();

    // Scenarios
    ScenarioBuilder getBalance = scenario("Get Balance")
        .feed(accountFeeder)
        .exec(
            http("GET /v1/wallets/${accountId}/balance")
                .get("/v1/wallets/${accountId}/balance")
                .check(status().is(200))
                .check(jsonPath("$.available").exists())
        );

    ScenarioBuilder transfer = scenario("Transfer Money")
        .feed(accountFeeder)
        .exec(
            http("POST /v1/transfers")
                .post("/v1/transfers")
                .body(StringBody("""
                    {
                        "fromAccountId": "${accountId}",
                        "toAccountId": "ACC-RECEIVER-001",
                        "amount": 10000,
                        "currency": "IDR",
                        "idempotencyKey": "${randomUuid()}"
                    }
                    """))
                .check(status().in(200, 201))
                .check(jsonPath("$.transactionId").saveAs("txnId"))
        );

    // Load Profile
    {
        setUp(
            // Warm-up phase
            getBalance.injectOpen(
                rampUsers(USERS / 2).during(60)
            ),
            // Steady state
            getBalance.injectOpen(
                constantUsersPerSec(USERS).during(DURATION)
            ),
            // Transfer load (10% of reads)
            transfer.injectOpen(
                constantUsersPerSec(USERS / 10).during(DURATION)
            )
        )
        .protocols(httpProtocol)
        .assertions(
            global().responseTime().percentile(50).lt(100),
            global().responseTime().percentile(95).lt(300),
            global().responseTime().percentile(99).lt(1000),
            global().successfulRequests().percent().gt(99.9)
        );
    }
}
```

---

## 📊 k6 Template (JavaScript)

```javascript
// tests/performance/wallet-load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const balanceLatency = new Trend('balance_latency');
const transferLatency = new Trend('transfer_latency');

// Configuration
export const options = {
  scenarios: {
    // Smoke test
    smoke: {
      executor: 'constant-vus',
      vus: 5,
      duration: '1m',
      startTime: '0s',
    },
    // Load test
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },  // Ramp up
        { duration: '5m', target: 100 },  // Steady state
        { duration: '2m', target: 200 },  // Spike
        { duration: '5m', target: 100 },  // Back to normal
        { duration: '1m', target: 0 },    // Ramp down
      ],
      startTime: '1m',
    },
  },
  thresholds: {
    http_req_duration: ['p(50)<100', 'p(95)<300', 'p(99)<1000'],
    errors: ['rate<0.01'],
    balance_latency: ['p(95)<200'],
    transfer_latency: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.AUTH_TOKEN || 'test-token';

const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${TOKEN}`,
};

// Shared test data
const accounts = JSON.parse(open('./data/accounts.json'));

export default function () {
  const account = accounts[Math.floor(Math.random() * accounts.length)];

  // 90% reads, 10% writes
  if (Math.random() < 0.9) {
    getBalance(account.id);
  } else {
    transfer(account.id);
  }

  sleep(1);
}

function getBalance(accountId) {
  const start = Date.now();
  const res = http.get(`${BASE_URL}/v1/wallets/${accountId}/balance`, { headers });
  
  balanceLatency.add(Date.now() - start);
  
  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'has balance': (r) => r.json('available') !== undefined,
  });
  
  errorRate.add(!success);
}

function transfer(fromAccountId) {
  const start = Date.now();
  const payload = JSON.stringify({
    fromAccountId,
    toAccountId: 'ACC-RECEIVER-001',
    amount: 10000,
    currency: 'IDR',
    idempotencyKey: `k6-${Date.now()}-${Math.random()}`,
  });

  const res = http.post(`${BASE_URL}/v1/transfers`, payload, { headers });
  
  transferLatency.add(Date.now() - start);
  
  const success = check(res, {
    'status is 200 or 201': (r) => [200, 201].includes(r.status),
    'has transaction ID': (r) => r.json('transactionId') !== undefined,
  });
  
  errorRate.add(!success);
}

// HTML report generation
export function handleSummary(data) {
  return {
    'reports/summary.html': htmlReport(data),
    'reports/summary.json': JSON.stringify(data),
  };
}
```

---

## 🏃 Running Performance Tests

### Gatling (CI/CD)

```bash
# Run with Maven
mvn gatling:test -Dgatling.simulationClass=simulations.WalletServiceSimulation

# Run with environment variables
USERS=200 DURATION=600 BASE_URL=https://staging.payu.fajjjar.my.id mvn gatling:test
```

### k6 (CI/CD)

```bash
# Run locally
k6 run tests/performance/wallet-load-test.js

# Run with cloud output
k6 run --out cloud tests/performance/wallet-load-test.js

# Run in Docker
docker run -i grafana/k6 run - < tests/performance/wallet-load-test.js
```

---

## 📈 Baseline Results

### Production Baseline (Captured: 2026-01-30)

| Endpoint | P50 | P95 | P99 | TPS | Error % |
|----------|-----|-----|-----|-----|---------|
| GET /wallets/{id}/balance | 45ms | 120ms | 280ms | 1200 | 0.02% |
| POST /transfers | 180ms | 420ms | 890ms | 180 | 0.008% |
| GET /accounts/{id} | 35ms | 80ms | 150ms | 2000 | 0.01% |
| POST /auth/token | 65ms | 150ms | 320ms | 3000 | 0.05% |

### Performance Budget

```yaml
# .github/performance-budget.yml
endpoints:
  - path: /v1/wallets/*/balance
    p95_max_ms: 200
    error_rate_max: 0.1
    
  - path: /v1/transfers
    p95_max_ms: 500
    error_rate_max: 0.01
    
  - path: /v1/accounts/*
    p95_max_ms: 100
    error_rate_max: 0.1
```

---

## 🔄 Performance Test Schedule

| Test Type | Frequency | Duration | Environment |
|-----------|-----------|----------|-------------|
| Smoke Test | Every PR | 2 min | Staging |
| Load Test | Daily | 15 min | Staging |
| Stress Test | Weekly | 1 hour | Performance |
| Soak Test | Monthly | 8 hours | Performance |
| Chaos + Load | Quarterly | 4 hours | Staging |
