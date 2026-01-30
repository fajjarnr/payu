# Runbook: High Error Rate

## Alert Information
- **Alert Name**: `HighErrorRate`
- **Severity**: Critical
- **Error Rate Threshold**: > 1% (5xx errors)
- **Team**: Platform Team

## Summary
Service is experiencing a high rate of 5xx errors. This indicates a critical issue that needs immediate attention.

## Initial Diagnosis

### 1. Check Error Rate by Service

```bash
# Get error rate for all services
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service)' | jq '.data.result[]'
```

### 2. Check Error Breakdown

```bash
# Get error count by status code
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (status)' | jq '.data.result[]'
```

### 3. Identify Affected Endpoints

```bash
# Get top 10 endpoints with most errors
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=topk(10, sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (endpoint))' | jq '.data.result[]'
```

## Troubleshooting Steps

### Step 1: Check Application Logs

```bash
# Get recent error logs
oc logs -n payu-prod $(oc get pods -n payu-prod -l app=account-service -o name | head -1) \
  --tail=500 | grep -i "error\|exception" | tail -50

# Follow logs in real-time
oc logs -f -n payu-prod $(oc get pods -n payu-prod -l app=account-service -o name | head -1)
```

### Step 2: Check for Common Issues

#### Database Connection Issues

```bash
# Check database connectivity
oc exec -n payu-prod postgres-0 -- \
  psql -U payu -c "SELECT count(*) FROM pg_stat_activity WHERE datname='payu_account';"

# Check for connection pool exhaustion
curl -s http://account-service.payu-prod.svc:8080/actuator/metrics/hikaricp.connections.active | jq '.measurements[0].value'
```

#### Out of Memory Issues

```bash
# Check for OOMKilled events
oc get events -n payu-prod --field-selector reason=OOMKilling --sort-by='.lastTimestamp'

# Check heap memory usage
curl -s http://account-service.payu-prod.svc:8080/actuator/metrics/jvm_memory_used_bytes{area="heap"} | jq '.measurements[0].value'
```

#### Thread Pool Exhaustion

```bash
# Check thread pool usage
curl -s http://account-service.payu-prod.svc:8080/actuator/metrics/tomcat.threads.busy | jq '.measurements[0].value'
curl -s http://account-service.payu-prod.svc:8080/actuator/metrics/tomcat.threads.config.max | jq '.measurements[0].value'
```

## Resolution Strategies

### Strategy 1: Quick Fix - Restart Service

```bash
# Rollout restart for affected service
oc rollout restart dc/account-service -n payu-prod

# Monitor restart progress
oc rollout status dc/account-service -n payu-prod

# Watch logs during restart
oc logs -f -n payu-prod $(oc get pods -n payu-prod -l app=account-service -o name | head -1)
```

### Strategy 2: Scale Up Resources

```bash
# Increase replicas
oc scale dc/account-service -n payu-prod --replicas=5

# Increase resource limits
oc set resources dc/account-service -n payu-prod \
  --limits=cpu=2000m,memory=4Gi \
  --requests=cpu=1000m,memory=2Gi
```

### Strategy 3: Database Connection Pool Tuning

```bash
# Increase connection pool size
oc patch dc/account-service -n payu-prod --type=json \
  -p='[{"op": "replace", "path": "/spec/template/spec/containers/0/env/1/value", "value": "50"}]'

# Configure HikariCP settings
oc set env dc/account-service -n payu-prod \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=50 \
  SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=10
```

### Strategy 4: Enable Fallback Mode

```bash
# Enable circuit breaker
oc set env dc/account-service -n payu-prod \
  RESILIENCE4J_CIRCUITBREAKER_ENABLED=true \
  RESILIENCE4J_CIRCUITBREAKER_FAILURE_THRESHOLD=50

# Enable retry with fallback
oc set env dc/account-service -n payu-prod \
  RESILIENCE4J_RETRY_ENABLED=true \
  RESILIENCE4J_RETRY_MAX_ATTEMPTS=3
```

## Verification

### Confirm Error Rate Reduction

```bash
# Monitor error rate in real-time
watch -n 5 'curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d "query=sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))" | jq ".data.result[0].value[1]"'
```

### Run Health Checks

```bash
# Run comprehensive health checks
for service in account-service transaction-service wallet-service; do
  echo "=== $service ==="
  curl -f http://$service.payu-prod.svc.cluster.local:8080/actuator/health
  echo ""
done
```

## Prevention

### Implement Circuit Breaker Pattern

```yaml
resilience4j:
  circuitbreaker:
    instances:
      backendService:
        register-health-indicator: true
        sliding-window-size: 100
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 10
```

### Implement Retry Logic

```yaml
resilience4j:
  retry:
    instances:
      backendService:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
```

### Connection Pool Configuration

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      idle-timeout: 30000
      connection-timeout: 30000
      validation-timeout: 5000
```

## Related Runbooks
- [Service Degradation](./service-degradation.md)
- [SLO Availability Breach](./slo-availability.md)
- [Database Connection Pool Exhaustion](./database-connection-pool.md)

## Escalation
1. **Immediate**: Platform Team on-call
2. **15 minutes**: Platform Engineering Lead (if not resolved)

## Metrics to Monitor
- `http_server_requests_seconds_count{status=~"5.."}` (5xx errors)
- `http_server_requests_seconds_count{status=~"4.."}` (4xx errors)
- `hikaricp_connections_active` (active DB connections)
- `hikaricp_connections_pending` (pending DB connections)
- `jvm_memory_used_bytes{area="heap"}` (heap usage)
