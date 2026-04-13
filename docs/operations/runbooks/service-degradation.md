# Runbook: Service Degradation

## Alert Information
- **Alert Name**: `ServiceDegradation`
- **Severity**: Warning
- **Degradation Threshold**: Response time > 2x baseline or error rate > 1%
- **Team**: Platform Team

## Summary
Service is experiencing degraded performance. The service is still operational but experiencing:
- Increased response times (latency)
- Elevated error rates
- Resource contention
- Slow database queries

## Initial Diagnosis

### 1. Check Service Health

```bash
# Check service health endpoint
curl http://account-service.payu.svc:8080/actuator/health

# Check all services health
for service in account-service transaction-service wallet-service; do
  echo "=== $service ==="
  curl -s http://${service}.payu.svc:8080/actuator/health | jq '.status'
done
```

### 2. Check Response Times

```bash
# Get current p95 latency
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))' | jq '.data.result[]'
```

### 3. Check Error Rates

```bash
# Get 5xx error rate
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))' | jq '.data.result[0].value[1]'
```

## Troubleshooting Steps

### Step 1: Identify Bottlenecks

```bash
# Check CPU usage
oc top pods -n payu -l app=account-service

# Check memory usage
oc exec -n payu $(oc get pods -n payu -l app=account-service -o name | head -1) -- \
  free -h

# Check thread pool usage
curl -s http://account-service.payu.svc:8080/actuator/metrics/tomcat.threads.busy | jq '.measurements[0].value'
```

### Step 2: Check Database Performance

```bash
# Check database connections
oc exec -n payu postgres-0 -- \
  psql -U payu -c "SELECT count(*) FROM pg_stat_activity WHERE datname='payu_account';"

# Check slow queries
oc exec -n payu postgres-0 -- \
  psql -U payu -c "SELECT query, mean_exec_time FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"
```

### Step 3: Check External Dependencies

```bash
# Check Kafka connectivity
oc exec -n payu $(oc get pods -n payu -l app=account-service -o name | head -1) -- \
  curl -s http://kafka.payu.svc:9092

# Check Redis connectivity
oc exec -n payu $(oc get pods -n payu -l app=account-service -o name | head -1) -- \
  redis-cli -h redis.payu.svc ping
```

## Resolution Strategies

### Strategy 1: Scale Up Resources

```bash
# Increase replicas
oc scale dc/account-service -n payu --replicas=5

# Increase resource limits
oc set resources dc/account-service -n payu \
  --requests=cpu=500m,memory=1Gi \
  --limits=cpu=2000m,memory=4Gi
```

### Strategy 2: Enable Caching

```bash
# Enable Redis cache
oc set env dc/account-service -n payu \
  CACHE_ENABLED=true \
  CACHE_TTL=300

# Rollout restart to apply changes
oc rollout restart dc/account-service -n payu
```

### Strategy 3: Optimize Database Queries

```bash
# Check for missing indexes
oc exec -n payu postgres-0 -- \
  psql -U payu -d payu_account -c "SELECT schemaname, tablename, indexname FROM pg_indexes WHERE indexname NOT LIKE 'pg_toast%';"

# Create missing indexes via migration
# (This should be done through Flyway migrations, not directly)
```

### Strategy 4: Enable Circuit Breaker

```bash
# Configure circuit breaker for external calls
oc patch dc/account-service -n payu --type=json \
  -p='[{"op": "add", "path": "/spec/template/spec/containers/0/env/-", "value": {"name": "CIRCUIT_BREAKER_ENABLED", "value": "true"}}]'

# Configure failure threshold
oc patch dc/account-service -n payu --type=json \
  -p='[{"op": "add", "path": "/spec/template/spec/containers/0/env/-", "value": {"name": "CIRCUIT_BREAKER_FAILURE_THRESHOLD", "value": "50"}}]'
```

## Verification

### Confirm Recovery

```bash
# Check response times
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{service="account-service"}[5m])) by (le)' | jq '.data.result[]'

# Check error rates
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=sum(rate(http_server_requests_seconds_count{service="account-service",status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count{service="account-service"}[5m]))' | jq '.data.result[0].value[1]'

# Run smoke test
curl -f http://account-service.payu.svc:8080/actuator/health
```

## Prevention

### Long-term Fixes

1. **Implement Auto-scaling**: Configure HPA based on CPU/memory
2. **Add Caching Layer**: Redis for frequently accessed data
3. **Optimize Queries**: Add database indexes, use prepared statements
4. **Connection Pooling**: Tune database connection pool size
5. **Async Processing**: Offload heavy tasks to background workers

### Auto-scaling Example

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: account-service-hpa
  namespace: payu
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: DeploymentConfig
    name: account-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

## Related Runbooks
- [SLO Availability Breach](./slo-availability.md)
- [High Error Rate](./high-error-rate.md)
- [Database Connection Pool Exhaustion](./database-connection-pool.md)

## Escalation
1. **Immediate**: Platform Team on-call
2. **15 minutes**: Platform Engineering Lead (if not improving)

## Metrics to Monitor
- `http_server_requests_seconds` (response time)
- `http_server_requests_seconds_count{status=~"5.."}` (error rate)
- `jvm_memory_used_bytes` (heap usage)
- `hikaricp_connections_active` (DB connections)
- `tomcat_threads_busy` (thread pool usage)
