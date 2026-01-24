# Runbook: SLO Availability Breach

## Alert Information
- **Alert Name**: `SLOAvailabilityBreached`
- **Severity**: Critical
- **SLO Target**: 99.9% availability (43.2 minutes downtime per month)
- **Team**: Platform Team

## Summary
Service availability has dropped below the 99.9% threshold over a 30-day rolling window. This is a critical incident that requires immediate attention.

## Initial Diagnosis

### 1. Check Affected Services
```bash
# Get all services with availability issues
oc get pods -n payu-prod -l app=payu --field-selector=status.phase!=Running

# Check service availability metrics
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=avg_over_time(rate(up{job=~"payu-.*"}[30d]))' | jq '.data.result[] | {service: .metric.job, availability: .value[1]}'
```

### 2. Identify Root Cause
```bash
# Check for recent pod restarts
oc describe pods -n payu-prod -l app=payu | grep -A 10 "Restart Count"

# Check for OOMKilled events
oc get events -n payu-prod --field-selector reason=OOMKilling

# Check node health
oc get nodes
oc describe nodes | grep -A 5 "Conditions:"
```

## Troubleshooting Steps

### Step 1: Verify Infrastructure Health
```bash
# Check PostgreSQL connectivity
oc exec -n payu-prod -it $(oc get pods -n payu-prod -l app=postgresql -o name | head -1) -- \
  pg_isready -U postgres

# Check Redis connectivity
oc exec -n payu-prod -it $(oc get pods -n payu-prod -l app=redis -o name | head -1) -- \
  redis-cli ping

# Check Kafka connectivity
oc exec -n payu-prod -it $(oc get pods -n payu-prod -l app=kafka -o name | head -1) -- \
  kafka-broker-api-versions --bootstrap-server localhost:9092
```

### Step 2: Check Application Logs
```bash
# Get logs from failing pods
oc logs -n payu-prod $(oc get pods -n payu-prod -l app=payu,service=account-service -o name | head -1) --tail=100

# Check for error patterns
oc logs -n payu-prod $(oc get pods -n payu-prod -l app=payu,service=account-service -o name | head -1) | \
  grep -i "error\|exception\|failed" | tail -20
```

### Step 3: Verify Resource Availability
```bash
# Check resource quotas
oc describe quota -n payu-prod

# Check resource usage
oc top nodes
oc top pods -n payu-prod

# Check for resource constraints
oc get pods -n payu-prod -o json | jq '.items[] | select(.status.phase!="Running") | .metadata.name'
```

## Resolution Strategies

### Strategy 1: Scale Up Resources
```bash
# Increase replicas for affected services
oc scale dc/account-service -n payu-prod --replicas=5
oc scale dc/transaction-service -n payu-prod --replicas=5

# Verify scaling
oc get pods -n payu-prod -w
```

### Strategy 2: Restart Affected Services
```bash
# Rollout restart for stuck deployments
oc rollout restart dc/account-service -n payu-prod
oc rollout status dc/account-service -n payu-prod

# Monitor the restart
oc logs -f -n payu-prod $(oc get pods -n payu-prod -l app=payu,service=account-service -o name | head -1)
```

### Strategy 3: Adjust Resource Limits
```bash
# Update resource requests/limits
oc set resources dc/account-service -n payu-prod \
  --requests=cpu=1000m,memory=2Gi \
  --limits=cpu=2000m,memory=4Gi

# Apply changes
oc rollout latest dc/account-service -n payu-prod
```

### Strategy 4: Enable Additional Capacity
```bash
# Scale up cluster nodes (if using cluster autoscaler)
oc autoscale dc/account-service -n payu-prod --min=3 --max=10 --cpu-percent=70

# Enable cluster autoscaler (if not already enabled)
oc edit clusterresourcequota
```

## Verification

### Confirm Service Recovery
```bash
# Run smoke tests
for service in account-service transaction-service wallet-service; do
  curl -f http://$service.payu-prod.svc.cluster.local:80/actuator/health || echo "FAIL: $service"
done

# Check availability metrics
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=avg(rate(up{job=~"payu-.*"}[5m]))' | jq '.data.result[0].value[1]'
```

### Monitor Error Budget Recovery
```bash
# Calculate current error budget
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=(1 - avg(rate(up{job=~"payu-.*"}[30d]))) / 0.001 * 100' | jq '.data.result[0].value[1]'
```

## Prevention

### Long-term Fixes
1. **Implement Circuit Breakers**: Add resilience patterns to prevent cascading failures
2. **Optimize Resource Allocation**: Use VPA to right-size resource requests
3. **Improve Monitoring**: Add earlier warning thresholds for proactive intervention
4. **Capacity Planning**: Regular reviews of resource utilization and growth trends

### Related Alerts
- `ErrorBudgetExhausted`
- `ServiceDown`
- `HighMemoryUsage`
- `CriticalMemoryUsage`

## Escalation
- **Level 1**: Platform Team on-call (via PagerDuty)
- **Level 2**: Platform Engineering Lead (if not resolved in 30 minutes)
- **Level 3**: CTO/VP Engineering (if not resolved in 1 hour)

## Metrics to Monitor During Incident
- `up{job=~"payu-.*"}`
- `payu_slo_composite_score`
- `http_server_requests_seconds_count{status=~"5.."}`
- `jvm_memory_used_bytes{area="heap"}`
- `rate(container_cpu_usage_seconds_total[5m])`

## Notes
- Document all actions taken in the incident timeline
- Update this runbook with any new findings or solutions
- Conduct a post-incident review after resolution
