# Service Level Objectives (SLO) Implementation

PayU uses Google SRE-style SLOs with Burn Rate Alerting.
Based on "The SRE Workbook", Chapter 5.

## 1. Compliance Recording Rules

Pre-compute SLI ratios over 28 days (standard window).

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: slo-recording-rules
  namespace: payu-monitoring
spec:
  groups:
  - name: slo.recording
    interval: 2m
    rules:
    # Availability: (Total - 5xx) / Total
    - record: sli:availability:ratio_28d
      expr: |
        sum(rate(http_server_requests_seconds_count{status!~"5.."}[28d]))
        /
        sum(rate(http_server_requests_seconds_count[28d]))

    # Latency: (Requests < 500ms) / Total
    - record: sli:latency:ratio_28d
      expr: |
        sum(rate(http_server_requests_seconds_bucket{le="0.5"}[28d]))
        /
        sum(rate(http_server_requests_seconds_count[28d]))

    # Error Budget Spending Speed (Burn Rate)
    # 1.0 = burning exactly at limit. 10.0 = burning 10x faster (danger).
    - record: slo:availability:burn_rate_1h
      expr: |
        (1 - sum(rate(http_server_requests_seconds_count{status!~"5.."}[1h])) / sum(rate(http_server_requests_seconds_count[1h])))
        /
        (1 - 0.999) # Target 99.9%
```

## 2. Multi-Window Alerting (The "Google" Way)

Alert ONLY when budget is burning fast in BOTH short window (5m) and long window (1h).
Prevents flaky alerts.

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: slo-alerts
spec:
  groups:
  - name: slo.alerts
    rules:
    # Critical: 2% of budget consumed in 1 hour (14.4x burn rate)
    - alert: FastBurnErrorBudget
      expr: |
        (
          slo:availability:burn_rate_1h > 14.4
          and
          slo:availability:burn_rate_5m > 14.4
        )
      labels:
        severity: critical
      annotations:
        summary: "Burning Error Budget Fast (14x)"
        description: "At this rate, 99.9% SLO will be breached in 2 days."
```

## 3. Best Practices
1.  **Don't alert on SLI dip**: Just because availability drops to 98% for 1 minute doesn't mean you wake up. Only alert if *Error Budget* is threatened.
2.  **Tag SLOs**: Add `slo: "true"` label to critical services (`wallet-service`, `transaction-service`).
3.  **Dashboard**: Visualize "Budget Remaining" (e.g., "100 errors left this month").
