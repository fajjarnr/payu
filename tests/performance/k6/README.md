# PayU Platform - k6 Load Testing

Load testing suite for the PayU Digital Banking Platform using [k6](https://k6.io/).

## Quick Start

```bash
# Install k6
curl -sSL "https://github.com/grafana/k6/releases/download/v0.48.0/k6-v0.48.0-linux-amd64.tar.gz" | tar -xz
sudo mv k6-v0.48.0-linux-amd64/k6 /usr/local/bin/

# Run smoke test (1 user, 30 seconds)
k6 run smoke-test.js

# Run load test (up to 100 users, ~25 minutes)
k6 run load-test.js

# Run stress test (up to 1000 users, ~40 minutes)
k6 run stress-test.js
```

## Test Types

### 1. Smoke Test (`smoke-test.js`)
**Purpose**: Verify platform is functional
- Duration: 30 seconds
- Users: 1 concurrent
- Endpoints: Gateway health, Keycloak OIDC, Core services

**Run**:
```bash
k6 run smoke-test.js
```

### 2. Load Test (`load-test.js`)
**Purpose**: Validate performance under expected load
- Duration: ~25 minutes
- Users: Ramp up to 100 concurrent
- Stages:
  - 2m: Ramp to 10 users
  - 5m: Ramp to 50 users
  - 10m: Sustain 100 users
  - 5m: Ramp down to 50
  - 2m: Ramp down to 10
  - 1m: Cool down

**Run**:
```bash
k6 run load-test.js
```

### 3. Stress Test (`stress-test.js`)
**Purpose**: Find breaking point and platform limits
- Duration: ~40 minutes
- Users: Ramp up to 1000 concurrent
- Max load: 1000 concurrent users

**Run**:
```bash
k6 run stress-test.js
```

## Target Endpoints

| Service | Endpoint | Purpose |
|:--------|:---------|:--------|
| Gateway | `/actuator/health` | Gateway health check |
| Keycloak | `/auth/realms/payu/.well-known/openid-configuration` | OIDC discovery |
| Keycloak | `/auth/realms/payu/protocol/openid-connect/token` | Authentication |
| Account | `/api/v1/accounts/actuator/health` | Account service health |
| Wallet | `/api/v1/wallets/actuator/health` | Wallet service health |
| Transaction | `/api/v1/transactions/actuator/health` | Transaction service health |

## Thresholds

Based on DORA Elite metrics:

| Metric | Threshold |
|:-------|:----------|
| p95 Response Time | < 500ms |
| p99 Response Time | < 1000ms |
| Average Response Time | < 300ms |
| Error Rate | < 1% |
| Throughput | > 100 RPS |

## Environment Variables

Override base URLs:

```bash
export GATEWAY_URL=https://gateway-dev.payu.fajjjar.my.id
export KEYCLOAK_URL=https://keycloak-dev.payu.fajjjar.my.id

k6 run -e GATEWAY_URL=$GATEWAY_URL -e KEYCLOAK_URL=$KEYCLOAK_URL smoke-test.js
```

## Interpreting Results

### Checkmarks (✓)
- Request completed within threshold
- No errors returned

### Warnings (!)
- Response time approaching threshold
- Error rate > 0.5%

### Failures (✗)
- Response time exceeded threshold
- Error rate > 1%
- Service returned 5xx error

## CI/CD Integration

```yaml
# .github/workflows/load-test.yml
name: Load Test
on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup k6
        run: |
          curl -sSL "https://github.com/grafana/k6/releases/download/v0.48.0/k6-v0.48.0-linux-amd64.tar.gz" | tar -xz
          sudo mv k6-v0.48.0-linux-amd64/k6 /usr/local/bin/
      - name: Run smoke test
        run: k6 run tests/performance/k6/smoke-test.js
```

## Troubleshooting

### Certificate Errors
If you see certificate validation errors:
```bash
export NODE_TLS_REJECT_UNAUTHORIZED=0
# Or in k6:
k6 run --insecure-skip-tls-verify smoke-test.js
```

### Rate Limiting
If tests fail with 429 (Too Many Requests):
- Reduce request rate with longer sleep times
- Check gateway rate limiting configuration

### Connection Timeouts
If tests timeout:
- Verify target endpoints are accessible
- Check firewall/security group rules
- Ensure VPN is connected if required

## References

- [k6 Documentation](https://k6.io/docs/)
- [k6 Metrics](https://k6.io/docs/using-k6/metrics/)
- [Performance Testing Best Practices](https://k6.io/docs/testing-guides/running-large-tests/)
