# PayU Performance Load Testing

Performance load testing suite for PayU Digital Banking Platform using [Gatling](https://gatling.io/).

## Overview

This test suite simulates realistic load patterns on the PayU platform with multiple scenarios:

- **Login Simulation** - Authentication service performance
- **Transfer Simulation** - BI-FAST transfer operations
- **QRIS Payment Simulation** - QR code payment processing
- **Balance Query Simulation** - Wallet balance and transaction history queries
- **All Services Simulation** - Comprehensive mixed workload

## Load Test Profile

| Parameter | Value |
|-----------|-------|
| **Concurrent Users** | Ramp-up from 10 to 1000 users |
| **Ramp-up Duration** | 5 minutes |
| **Sustained Load** | 10 minutes at 1000 users |
| **Total Test Duration** | 15 minutes |
| **Throughput Target** | 1000+ requests/second |

## Performance Assertions

| Metric | Target |
|--------|--------|
| **p95 Response Time** | < 1s for critical operations |
| **p99 Response Time** | < 2s for all operations |
| **Max Response Time** | < 5s for all operations |
| **Success Rate** | > 99% for all operations |

## Prerequisites

1. **Java 21** installed and configured
2. **Maven 3.9+** installed
3. Target services running and accessible
4. Test data prepared in `src/test/resources/data/`

## Project Structure

```
tests/performance/
├── pom.xml                                           # Maven configuration
├── README.md                                         # This file
├── src/
│   └── test/
│       ├── scala/
│       │   └── id/payu/simulations/
│       │       ├── LoginSimulation.scala            # Login performance test
│       │       ├── TransferSimulation.scala         # Transfer performance test
│       │       ├── QRISPaymentSimulation.scala      # QRIS payment performance test
│       │       ├── BalanceQuerySimulation.scala     # Balance query performance test
│       │       └── AllServicesSimulation.scala      # Comprehensive test
│       └── resources/
│           ├── gatling.conf                         # Gatling configuration
│           └── data/
│               ├── users.csv                        # Test user credentials
│               └── accounts.csv                     # Test account data
└── target/
    └── gatling/
        └── results/                                 # Test reports
```

## Installation

1. **Navigate to performance tests directory:**
   ```bash
   cd /home/ubuntu/payu/tests/performance
   ```

2. **Install dependencies:**
   ```bash
   mvn clean install
   ```

## Running Tests

### Run All Services (Default)

```bash
mvn gatling:test
```

### Run Individual Scenarios

**Login Simulation:**
```bash
mvn gatling:test -Plogin
```

**Transfer Simulation:**
```bash
mvn gatling:test -Ptransfer
```

**QRIS Payment Simulation:**
```bash
mvn gatling:test -Pqris
```

**Balance Query Simulation:**
```bash
mvn gatling:test -Pbalance
```

### Custom Configuration

**Specify custom base URL:**
```bash
mvn gatling:test -DbaseUrl=https://api.payu.id
```

**Specify individual service URLs:**
```bash
mvn gatling:test \
  -DbaseUrl=https://api.payu.id \
  -DauthUrl=https://auth.payu.id \
  -DtransactionUrl=https://transaction.payu.id \
  -DwalletUrl=https://wallet.payu.id
```

**Run with custom simulation:**
```bash
mvn gatling:test -Dgatling.simulationClass=id.payu.simulations.LoginSimulation
```

### Run Specific Simulation Class

```bash
mvn -Dgatling.simulationClass=id.payu.simulations.TransferSimulation gatling:test
```

## Test Data

### Users CSV Format

```csv
username,password,auth_token,account_number
user001,password123,token_user_001_acc001,ACC001001
...
```

### Accounts CSV Format

```csv
account_number,balance,account_type,customer_id
ACC001001,5000000,SALARY,user001
...
```

## Viewing Reports

After test execution, Gatling generates HTML reports:

1. **Report Location:**
   ```
   target/gatling/results/<simulation-timestamp>/index.html
   ```

2. **View Report:**
   ```bash
   # Open in browser
   open target/gatling/results/<simulation-timestamp>/index.html

   # Or serve via HTTP
   cd target/gatling/results/<simulation-timestamp> && python3 -m http.server 8000
   # Then navigate to http://localhost:8000
   ```

3. **Report Contents:**
   - Global statistics
   - Request/response times (percentiles)
   - Response time distribution
   - Requests per second
   - Errors and failures
   - Active users over time

## Configuration

### Gatling Configuration (`src/test/resources/gatling.conf`)

Edit this file to customize:

- **Timeouts** - HTTP request timeouts
- **Thread pool** - Virtual user thread settings
- **SSL/TLS** - Security protocols and cipher suites
- **HTTP/2** - Enable/disable HTTP/2
- **Cache** - HTTP caching behavior

### JVM Configuration

Adjust JVM memory in `pom.xml`:

```xml
<jvmArgs>
  <jvmArg>-Xms1024m</jvmArg>
  <jvmArg>-Xmx2048m</jvmArg>
</jvmArgs>
```

For larger loads, increase heap size:

```xml
<jvmArgs>
  <jvmArg>-Xms4096m</jvmArg>
  <jvmArg>-Xmx8192m</jvmArg>
  <jvmArg>-XX:+UseG1GC</jvmArg>
  <jvmArg>-XX:MaxGCPauseMillis=200</jvmArg>
</jvmArgs>
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Performance Tests

on:
  schedule:
    - cron: '0 2 * * *'  # Run daily at 2 AM
  workflow_dispatch:

jobs:
  performance-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run Performance Tests
        run: |
          cd tests/performance
          mvn gatling:test
      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: gatling-results
          path: tests/performance/target/gatling/results/
```

### GitLab CI Example

```yaml
performance-test:
  stage: performance
  image: maven:3.9-eclipse-temurin-21
  script:
    - cd tests/performance
    - mvn gatling:test
  artifacts:
    paths:
      - tests/performance/target/gatling/results/
    expire_in: 1 week
  only:
    - schedules
    - main
```

## Best Practices

### 1. Test Environment

- **Always** run performance tests against a dedicated test environment
- **Never** run against production
- Ensure test data matches production data volume
- Monitor resource usage (CPU, memory, disk I/O) during tests

### 2. Test Execution

- Run tests during off-peak hours
- Ensure no other load tests are running concurrently
- Monitor system logs for errors during test execution
- Stop tests if error rate exceeds 5%

### 3. Result Analysis

- Compare results against baseline
- Investigate any response time degradation
- Check error logs for failed requests
- Profile slow endpoints

### 4. Continuous Improvement

- Update assertions based on actual requirements
- Add new scenarios as features are added
- Maintain test data freshness
- Document performance regressions

## Troubleshooting

### Out of Memory

**Symptom:** `java.lang.OutOfMemoryError: Java heap space`

**Solution:**
```bash
export MAVEN_OPTS="-Xmx4096m -XX:+UseG1GC"
mvn gatling:test
```

### Connection Timeout

**Symptom:** `io.netty.channel.ConnectTimeoutException`

**Solution:**
- Verify target services are running
- Check firewall rules
- Increase timeout in `gatling.conf`

### Too Many Open Files

**Symptom:** `java.io.IOException: Too many open files`

**Solution:**
```bash
ulimit -n 65536
mvn gatling:test
```

### High Error Rate

**Symptom:** Success rate < 99%

**Solution:**
- Check service health
- Verify test data validity
- Review authentication tokens
- Check rate limiting

## Performance Tuning

### Database

- Enable connection pooling
- Use read replicas for balance queries
- Index frequently queried fields
- Optimize slow queries

### Caching

- Enable Redis caching for balance queries
- Cache user sessions
- Use CDN for static resources

### Application

- Tune thread pool sizes
- Enable HTTP/2
- Use compression for large payloads
- Implement circuit breakers

## Load Testing Environments

| Environment | Base URL | Purpose |
|-------------|----------|---------|
| **Local** | `http://localhost:8080` | Development testing |
| **Staging** | `https://staging-api.payu.id` | Pre-production validation |
| **Load Test** | `https://loadtest-api.payu.id` | Dedicated performance testing |

## Metrics to Monitor

During performance tests, monitor:

- **Application Metrics:**
  - Response times (p50, p95, p99)
  - Error rate
  - Throughput (requests/second)
  - Active concurrent users

- **Infrastructure Metrics:**
  - CPU utilization
  - Memory usage
  - Disk I/O
  - Network bandwidth

- **Database Metrics:**
  - Connection pool usage
  - Query execution time
  - Lock contention
  - Transaction throughput

## Support

For questions or issues:
- **Performance Issues:** performance-team@payu.id
- **Test Infrastructure:** qa-team@payu.id
- **Application Issues:** backend-team@payu.id

## References

- [Gatling Documentation](https://gatling.io/docs/gatling/)
- [Gatling Maven Plugin](https://gatling.io/docs/gatling/reference/current/extensions/maven_plugin/)
- [Performance Testing Best Practices](https://gatling.io/docs/gatling/tutorials/advanced/)

---

*Last Updated: January 2026*
