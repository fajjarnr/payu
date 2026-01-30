# Grafana Dashboard Patterns

PayU uses Grafana as the primary visualization layer for observability.
Dashboards should be provisioned as code (JSON/ConfigMap) rather than manually created via UI.

## 1. Golden Signals Dashboard Pattern

The standard layout for any microservice dashboard.

### Variable Strategy
Always use variables to make dashboard reusable across Namespace/Service.

```json
"templating": {
  "list": [
    {
      "name": "namespace",
      "query": "label_values(up, namespace)",
      "type": "query",
      "datasource": "Prometheus"
    },
    {
      "name": "service",
      "query": "label_values(up{namespace=\"$namespace\"}, service)",
      "type": "query",
      "datasource": "Prometheus"
    }
  ]
}
```

### Core Panels (PromQL)

| Panel Title | Visualization | PromQL Query |
| :--- | :--- | :--- |
| **Request Rate** | Time Series | `sum(rate(http_server_requests_seconds_count{namespace="$namespace", service="$service"}[1m]))` |
| **Error Rate %** | Stat | `sum(rate(http_server_requests_seconds_count{status=~"5..", namespace="$namespace", service="$service"}[1m])) / sum(rate(http_server_requests_seconds_count{namespace="$namespace", service="$service"}[1m])) * 100` |
| **Latency P95** | Time Series | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{namespace="$namespace", service="$service"}[1m])) by (le))` |
| **Availability** | Gauge | `up{namespace="$namespace", service="$service"}` |

## 2. Provisioning (OpenShift)

In OpenShift/Strimzi, deploy dashboards as `ConfigMap` with a specific label.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: wallet-service-dashboard
  namespace: payu-monitoring
  labels:
    grafana_dashboard: "true" # Grafana Operator watches this label
data:
  wallet-service.json: |
    {
      "dashboard": {
        "title": "Wallet Service (PayU)",
        "panels": [ ... ]
      }
    }
```
