# K6 Load Testing Execution Runbook

## Status Saat Ini
- **K6**: ✅ Installed (v1.6.1)
- **OpenShift Endpoint**: ⏳ Waiting for cluster provision
- **Podman Local**: ❌ Not installed

---

## Quick Start (Pilih Salah Satu)

### Option A: Run Against OpenShift (Recommended)

Jalankan setelah cluster OpenShift ready:

```bash
cd /home/ubuntu/payu/tests/performance/k6

# 1. Smoke Test (1 user, 30 detik)
k6 run smoke-test.js

# 2. Load Test (~25 menit, 100 users)
k6 run load-test.js

# 3. Stress Test (~40 menit, 1000 users) - untuk breaking point
k6 run stress-test.js --out json=stress-results.json
```

### Option B: Run Against Local Podman

Jalankan setelah Podman terinstall dan infrastructure running:

```bash
# 1. Start infrastructure
cd /home/ubuntu/payu/infrastructure/local-podman
podman compose up -d

# 2. Tunggu health check
sleep 60

# 3. Run tests dengan config local
cd /home/ubuntu/payu/tests/performance/k6

# Copy config local ke config.js (backup dulu)
cp config.js config.openshift.js
cp config.local.js config.js

# Run tests
k6 run smoke-test.js
k6 run load-test.js
k6 run stress-test.js

# Restore config
cp config.openshift.js config.js
```

---

## Test Specifications

### 1. Smoke Test (smoke-test.js)
| Parameter | Value |
|-----------|-------|
| Duration | 30 detik |
| Virtual Users | 1 |
| Purpose | Verifikasi platform accessible |

**Endpoints Tested:**
- Gateway health: `/actuator/health`
- Keycloak OIDC: `/auth/realms/payu/.well-known/openid-configuration`
- Keycloak Token: `/auth/realms/payu/protocol/openid-connect/token`
- Account Service: `/api/v1/accounts/public/health`
- Wallet Service: `/api/v1/wallets/public/health`
- Transaction Service: `/api/v1/transactions/public/health`

### 2. Load Test (load-test.js)
| Parameter | Value |
|-----------|-------|
| Total Duration | ~25 menit |
| Max VU | 100 users |
| Thresholds | p95 < 500ms, p99 < 1000ms, Error < 1% |

**Stages:**
1. Ramp up: 2m → 10 users
2. Ramp up: 5m → 50 users
3. Sustain: 10m @ 100 users
4. Ramp down: 5m → 50 users
5. Ramp down: 2m → 10 users
6. Cool down: 1m → 0 users

### 3. Stress Test (stress-test.js)
| Parameter | Value |
|-----------|-------|
| Total Duration | ~40 menit |
| Max VU | 1000 users |
| Thresholds | p95 < 5000ms, p99 < 10000ms, Error < 50% |

**Stages:**
1. Ramp up: 2m → 50 users
2. Ramp up: 5m → 200 users
3. Ramp up: 10m → 500 users
4. Peak: 10m @ 1000 users
5. Ramp down: 5m → 500 users
6. Ramp down: 5m → 200 users
7. Ramp down: 2m → 50 users
8. Cool down: 1m → 0 users

---

## Metrics Yang Di-Capture

| Metric | Description | Target |
|--------|-------------|--------|
| http_req_duration | Response time | p95 < 500ms |
| http_req_failed | Error rate | < 1% (load), < 50% (stress) |
| http_reqs | Requests per second | > 100 RPS |
| auth_success_rate | Auth success % | > 95% |
| api_response_time | API response trend | Monitor degradation |

---

## Expected Results Interpretation

### Capacity Planning

| Load Level | Expected | Action jika Failed |
|------------|----------|-------------------|
| 50 users | < 200ms | Check DB connection pool |
| 100 users | < 500ms | Scale gateway replicas |
| 200 users | Acceptable | Add Kafka partitions |
| 500 users | Degradation | Enable circuit breakers |
| 1000 users | Breaking point | Implement rate limiting |

### SLO/SLA Definition

Berdasarkan hasil test, definisikan:
- **SLO Latency**: p95 < 500ms untuk 100 concurrent users
- **SLO Error Rate**: < 0.1% untuk load normal
- **SLA Availability**: 99.9% uptime
- **Capacity Threshold**: Scale trigger pada 70% dari breaking point

---

## Output & Reporting

### JSON Output (untuk analisis)
```bash
k6 run load-test.js --out json=results.json
```

### Summary ke CHANGELOG.md
```markdown
## [Load Testing Results] - YYYY-MM-DD

### Environment
- Target: OpenShift Dev / Local Podman
- Services: 21 backend + web-app

### Results
| Test | Duration | Max VU | p95 Latency | Error Rate | Status |
|------|----------|--------|-------------|------------|--------|
| Smoke | 30s | 1 | X ms | X% | ✅/❌ |
| Load | 25m | 100 | X ms | X% | ✅/❌ |
| Stress | 40m | 1000 | X ms | X% | ✅/❌ |

### Findings
- [Finding 1]
- [Finding 2]

### Recommendations
- [Recommendation 1]
```

---

## Troubleshooting

### Issue: Endpoint unreachable
```bash
# Test manual
curl -v https://gateway-dev.payu.fajjjar.my.id/actuator/health

# Check DNS
nslookup gateway-dev.payu.fajjjar.my.id
```

### Issue: K6 not found
```bash
# Verify install
k6 version

# Re-install jika perlu
sudo apt-get install -y k6
```

### Issue: High error rate
- Check Keycloak user exists: customer1/password123
- Verify service health: `oc get pods -n payu-dev`
- Check gateway logs: `oc logs -f deployment/gateway-service -n payu-dev`

---

## CRUD Tests (NEW - Best Practice)

### Why CRUD Load Testing?

Best practice load testing mencakup **full CRUD operations**, bukan hanya health checks:
- **Real-world load**: Transfer, create pocket (bukan hanya ping)
- **Write vs Read**: Performance berbeda (write lebih berat)
- **Database contention**: Terjadi saat concurrent CRUD
- **Data consistency**: Harus valid di bawah load

### CRUD Test Files

| Test | Duration | Max VU | Coverage |
|------|----------|--------|----------|
| `crud-load-test.js` | ~25m | 100 | Full CRUD (Account, Wallet, Transaction, Card) |
| `crud-stress-test.js` | ~40m | 1000 | CRUD to breaking point |
| `crud-data-consistency-test.js` | ~25m | 50 | Consistency validation |

### CRUD Operations Coverage

```
Account:   CREATE (register) → READ (profile) → UPDATE (profile)
Wallet:    CREATE (pocket) → READ (balance) → UPDATE (credit/freeze) → DELETE (close)
Transaction: CREATE (transfer) → READ (history/details)
Card:      CREATE (virtual) → READ (details) → UPDATE (freeze/unfreeze)
```

### Run CRUD Tests

```bash
cd /home/ubuntu/payu/tests/performance/k6

# Quick CRUD smoke
./run-all-tests.sh --smoke

# CRUD tests only (~90 min)
./run-all-tests.sh --crud

# Individual tests
k6 run crud-load-test.js          # 25 min
k6 run crud-stress-test.js        # 40 min
k6 run crud-data-consistency-test.js  # 25 min

# With JSON output
k6 run crud-load-test.js --out json=crud-results.json
```

### CRUD Metrics

| Metric | Target (Load) | Target (Stress) |
|--------|---------------|-----------------|
| crud_create_success | >95% | >80% |
| crud_read_success | >99% | >90% |
| crud_update_success | >95% | >80% |
| crud_delete_success | >90% | >70% |
| read_after_write_consistency | >99% | - |
| transaction_atomicity | >99.9% | - |

### CRUD Test Documentation

- **Full Guide**: [CRUD_TESTS_GUIDE.md](./CRUD_TESTS_GUIDE.md)
- **Library API**: See `lib/` directory for reusable functions

---

## Next Steps Checklist

- [ ] OpenShift cluster fully provisioned
- [ ] Services deployed dan running (36/36 pods)
- [ ] Keycloak realm 'payu' configured dengan user test
- [ ] Gateway route accessible via HTTPS
- [ ] Run smoke test (validate setup)
- [ ] Run **CRUD smoke test** (validate CRUD operations)
- [ ] Run **CRUD load test** (25 menit)
- [ ] Run **CRUD stress test** (40 menit)
- [ ] Run **data consistency test** (25 menit)
- [ ] Analyze results
- [ ] Define SLO/SLA
- [ ] Update capacity planning docs
- [ ] Add results ke CHANGELOG.md
