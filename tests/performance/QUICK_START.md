# Quick Start Guide - PayU Performance Load Testing

## Prerequisites

```bash
# Check Java version (requires Java 21)
java -version

# Check Maven version
mvn -version
```

## Installation

```bash
cd /home/ubuntu/payu/tests/performance
mvn clean install
```

## Running Tests

### Option 1: Using the convenience script (Recommended)

```bash
# Run all simulations
./run-performance-tests.sh

# Run specific simulation
./run-performance-tests.sh -s login
./run-performance-tests.sh -s transfer
./run-performance-tests.sh -s qris
./run-performance-tests.sh -s balance

# Run against different environment
./run-performance-tests.sh -u https://staging-api.payu.id
```

### Option 2: Using Maven

```bash
# Run all services simulation
mvn gatling:test

# Run individual simulations
mvn gatling:test -Plogin
mvn gatling:test -Ptransfer
mvn gatling:test -Pqris
mvn gatling:test -Pbalance

# With custom URL
mvn gatling:test -DbaseUrl=https://api.payu.id
```

### Option 3: Using Gradle

```bash
# Run all services simulation
./gradlew gatlingRun

# Run individual simulations
./gradlew runLoginSimulation
./gradlew runTransferSimulation
./gradlew runQRISPaymentSimulation
./gradlew runBalanceQuerySimulation
./gradlew runAllServicesSimulation
```

### Option 4: Using Docker

```bash
# Build and run
docker-compose up gatling

# Run with monitoring (Grafana + Prometheus)
docker-compose --profile monitoring up

# Run specific simulation
docker-compose run -e SIMULATION=id.payu.simulations.LoginSimulation gatling
```

## Test Scenarios

| Scenario | Description | Users | Duration |
|----------|-------------|-------|----------|
| **Login** | Authentication service performance | 10-1000 | 15 min |
| **Transfer** | BI-FAST transfer operations | 10-1000 | 15 min |
| **QRIS** | QRIS payment processing | 10-1000 | 15 min |
| **Balance** | Balance query performance | 10-1000 | 15 min |
| **All** | Comprehensive mixed workload | 10-1000 | 15 min |

## Viewing Results

```bash
# Results are located at:
# target/gatling/results/<timestamp>/index.html

# Open latest report
open target/gatling/results/$(ls -t target/gatling/results/ | head -1)/index.html

# Or serve via HTTP
cd target/gatling/results/$(ls -t | head -1) && python3 -m http.server 8000
```

## Performance Targets

| Metric | Target |
|--------|--------|
| p95 Response Time | < 1s |
| p99 Response Time | < 2s |
| Max Response Time | < 5s |
| Success Rate | > 99% |

## Troubleshooting

### Services not accessible
```bash
curl http://localhost:8080/actuator/health
```

### Out of memory
```bash
export MAVEN_OPTS="-Xmx4096m"
mvn gatling:test
```

### Port conflicts
```bash
# Kill existing Java processes
pkill -f gatling
```

## CI/CD Integration

```yaml
# GitHub Actions
- name: Run Performance Tests
  run: |
    cd tests/performance
    mvn gatling:test
```

## Support

- **Performance Issues:** performance-team@payu.id
- **Test Infrastructure:** qa-team@payu.id

---

For detailed documentation, see [README.md](README.md)
