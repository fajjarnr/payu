# Runbook: Error Budget Exhaustion

## Alert Information
- **Alert Name**: `ErrorBudgetExhausted`
- **Severity**: Critical
- **Error Budget Threshold**: Less than 10% remaining
- **Team**: Platform Team

## Summary
The error budget for the 99.9% availability SLO is nearly exhausted. This is a critical situation that requires immediate action to prevent further service degradation and SLO breach.

## Understanding Error Budget

### What is Error Budget?
Error budget represents the allowable downtime for a service based on its SLO:
- **SLO**: 99.9% availability
- **Error Budget**: 0.1% (43.2 minutes per month)
- **Current Status**: Less than 10% remaining (< 4.3 minutes)

### Calculate Current Error Budget
```bash
# Current availability
current_availability=$(curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=avg(rate(up{job=~"payu-.*"}[30d]))' | jq -r '.data.result[0].value[1]')

# Error budget remaining
echo "scale=4; (1 - $current_availability) / 0.001 * 100" | bc
```

## Immediate Actions

### 1. Stop Non-Essential Changes
```bash
# Pause all deployments
oc patch deploymentconfig -n payu-prod -l app=payu -p '{"spec":{"paused":true}}'

# Cancel running rollouts
oc rollout cancel dc/account-service -n payu-prod
oc rollout cancel dc/transaction-service -n payu-prod
```

### 2. Scale Up Services
```bash
# Increase replicas for better fault tolerance
for dc in $(oc get dc -n payu-prod -l app=payu -o name); do
  oc scale -n payu-prod $dc --replicas=5
done

# Verify scaling
oc get pods -n payu-prod -w
```

### 3. Enable Enhanced Monitoring
```bash
# Add additional scraping targets
oc edit configmap prometheus-config -n openshift-monitoring

# Increase alert frequency
oc edit prometheusrule slo-alert-rules -n openshift-monitoring
```

## Diagnostic Steps

### Identify Error Sources
```bash
# Check error rates by service
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=sum(rate(http_server_requests_seconds_count{status=~"5.."}[1h])) by (job)' | jq '.data.result[]'

# Check 50x errors
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=sum(rate(http_server_requests_seconds_count{status=~"5.."}[1h]))' | jq '.data.result[0].value[1]'

# Check timeouts
oc logs -n payu-prod --all-containers=true | grep -i "timeout\|connection refused" | tail -50
```

### Analyze Error Patterns
```bash
# Get error breakdown by endpoint
curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d 'query=topk(10, sum(rate(http_server_requests_seconds_count{status=~"5.."}[1h])) by (endpoint))' | jq '.data.result[]'

# Check for specific error types
oc get events -n payu-prod --field-selector reason=Error --sort-by='.lastTimestamp'
```

## Recovery Strategies

### Strategy 1: Reduce Load
```bash
# Enable rate limiting
oc patch dc/account-service -n payu-prod --type=json \
  -p='[{"op": "add", "path": "/spec/template/spec/containers/0/env/-", "value": {"name": "RATE_LIMIT_ENABLED", "value": "true"}}]'

# Reduce batch job frequency
oc scale cronjob/batch-processing-job -n payu-prod --replicas=0
```

### Strategy 2: Improve Fault Tolerance
```bash
# Increase resource limits
for dc in $(oc get dc -n payu-prod -l app=payu -o name); do
  oc set resources -n payu-prod $dc \
    --limits=cpu=2000m,memory=4Gi \
    --requests=cpu=1000m,memory=2Gi
done

# Enable pod disruption budgets
oc apply -f - <<EOF
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: payu-services-pdb
  namespace: payu-prod
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: payu
EOF
```

### Strategy 3: Optimize Dependencies
```bash
# Increase connection pool sizes
oc patch dc/account-service -n payu-prod --type=json \
  -p='[{"op": "replace", "path": "/spec/template/spec/containers/0/env/1/value", "value": "50"}]'

# Enable caching
oc patch dc/account-service -n payu-prod --type=json \
  -p='[{"op": "add", "path": "/spec/template/spec/containers/0/env/-", "value": {"name": "CACHE_ENABLED", "value": "true"}}]'
```

## Verification

### Monitor Error Budget Recovery
```bash
# Watch error budget in real-time
watch -n 5 'curl -s http://prometheus.payu.svc:9090/api/v1/query \
  -d "query=(1 - avg(rate(up{job=~\"payu-.*\"}[30d]))) / 0.001 * 100" | jq -r ".data.result[0].value[1]"'
```

### Check Service Health
```bash
# Run comprehensive health checks
for service in account-service transaction-service wallet-service auth-service; do
  echo "Checking $service..."
  curl -f http://$service.payu-prod.svc.cluster.local:80/actuator/health
  echo ""
done
```

## Post-Incident Actions

### 1. Conduct Incident Review
- Schedule post-incident review within 24 hours
- Identify root cause and contributing factors
- Document timeline of events and actions taken

### 2. Update Runbooks
- Incorporate lessons learned
- Add new troubleshooting steps if needed
- Update contact information and escalation paths

### 3. Implement Prevention Measures
```bash
# Add earlier warning thresholds
oc apply -f - <<EOF
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: error-budget-early-warning
  namespace: openshift-monitoring
spec:
  groups:
    - name: error_budget_early_warning
      rules:
        - alert: ErrorBudgetWarning
          expr: (1 - avg(rate(up{job=~"payu-.*"}[30d]))) / 0.001 * 100 < 20
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "Error budget below 20% - proactive action required"
EOF
```

### 4. Improve Monitoring
- Add more granular SLO tracking
- Implement real-time error budget monitoring dashboards
- Set up automated responses for error budget warnings

## Metrics to Track
- `payu_slo_composite_score`
- `1 - avg(rate(up{job=~"payu-.*"}[30d]))` (Error burn rate)
- `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))`
- `avg(rate(http_server_requests_seconds_count{status=~"5.."}[1h])) / sum(rate(http_server_requests_seconds_count[1h]))`

## Related Runbooks
- [SLO Availability Breach](./slo-availability.md)
- [Service Degradation](./service-degradation.md)
- [High Error Rate](./high-error-rate.md)

## Escalation Path
1. **Immediate**: Platform Team on-call (PagerDuty: Critical)
2. **15 minutes**: Platform Engineering Lead
3. **30 minutes**: CTO/VP Engineering

## Communication
- Update incident channel: `#incidents`
- Notify stakeholders: `#platform-updates`
- Send status page update if customer-facing
