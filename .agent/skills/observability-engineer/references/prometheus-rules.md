# Prometheus Alerting Rules (OpenShift)

Standard alerting rules for PayU microservices, deployable via `PrometheusRule` Custom Resource.

## 1. Golden Signals (HTTP/API)

Monitor the 4 Golden Signals: Latency, Traffic, Errors, and Saturation.

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: api-alerts
  namespace: payu-monitoring
spec:
  groups:
  - name: api.rules
    rules:
    # 🚨 ERROR RATE > 5%
    - alert: HighErrorRate
      expr: |
        sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) 
        / 
        sum(rate(http_server_requests_seconds_count[5m])) > 0.05
      for: 2m
      labels:
        severity: critical
      annotations:
        summary: "High Error Rate on {{ $labels.service }}"
        description: "Error rate is {{ $value | humanizePercentage }}."

    # ⚠️ LATENCY P95 > 2s
    - alert: HighLatency
      expr: |
        histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, service)) > 2
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High Latency on {{ $labels.service }}"
        description: "P95 Latency is {{ $value | humanizeDuration }}."

    # 🚨 SATURATION (Thread Pool)
    - alert: ConnectionPoolSaturation
      expr: |
        hikaricp_connections_active / hikaricp_connections_max > 0.9
      for: 1m
      labels:
        severity: critical
      annotations:
        summary: "DB Connection Pool Saturated"
        description: "Pool usage > 90% on {{ $labels.service }}."
```

## 2. JVM & Infrastructure

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: jvm-alerts
  namespace: payu-monitoring
spec:
  groups:
  - name: jvm.rules
    rules:
    # ⚠️ HEAP USAGE > 85%
    - alert: HighHeapUsage
      expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High Heap Usage on {{ $labels.service }}"
        description: "Heap is {{ $value | humanizePercentage }} full."

    # 🚨 GC LOG JAM (Long Pauses)
    - alert: LongGCPauses
      expr: rate(jvm_gc_pause_seconds_sum[1m]) > 1
      for: 2m
      labels:
        severity: critical
      annotations:
        summary: "GC Stalled"
        description: "GC is spending > 1s/min in pauses."
```

## 3. Recording Rules (Pre-computation)

Use these to speed up dashboard queries for heavy aggregations.

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: recording-rules
spec:
  groups:
  - name: aggregation.rules
    rules:
    - record: job:http_requests:rate5m
      expr: sum by (job, method, uri) (rate(http_server_requests_seconds_count[5m]))
```
