# LokiStack for Centralized Log Management

## Overview

LokiStack is an OpenShift-native implementation of Loki, a horizontally-scalable, highly-available, multi-tenant log aggregation system inspired by Prometheus. This deployment provides centralized log management for the PayU Digital Banking Platform.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      OpenShift Cluster                          │
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │   PayU Apps  │    │   PayU Apps  │    │   PayU Apps  │      │
│  │  (Services)  │    │  (Services)  │    │  (Services)  │      │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘      │
│         │                   │                   │              │
│         └───────────────────┼───────────────────┘              │
│                             │                                   │
│                    ┌────────▼────────┐                         │
│                    │  Vector Agent    │                         │
│                    │  (Log Collector) │                         │
│                    └────────┬─────────┘                         │
│                             │                                   │
│                    ┌────────▼────────┐                         │
│                    │ClusterLogForward│                         │
│                    │      er         │                         │
│                    └────────┬────────┘                         │
│                             │                                   │
│  ┌──────────────────────────▼──────────────────────────┐      │
│  │              LokiStack (openshift-logging)          │      │
│  │                                                       │      │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐           │      │
│  │  │ Gateway  │  │ Distrib  │  │ Ingester │           │      │
│  │  └──────────┘  └──────────┘  └──────────┘           │      │
│  │       │              │              │                 │      │
│  │       └──────────────┼──────────────┘                 │      │
│  │                      │                               │      │
│  │  ┌──────────┐  ┌─────▼──────┐  ┌──────────┐         │      │
│  │  │  Querier │  │ QueryFrnt │  │  Ruler   │         │      │
│  │  └──────────┘  └────────────┘  └──────────┘         │      │
│  │                       │                                │      │
│  │              ┌────────▼────────┐                       │      │
│  │              │   S3 Storage    │                       │      │
│  │              │   (Object Store) │                      │      │
│  │              └─────────────────┘                       │      │
│  └───────────────────────────────────────────────────────┘      │
│                                                                  │
│  ┌──────────────┐                                              │
│  │   Grafana    │ ◄─── Query logs via UI                       │
│  └──────────────┘                                              │
└────────────────────────────────────────────────────────────────┘
```

## Components

### 1. **LokiStack** (Operator)
- **Gateway**: HTTP ingress point for Loki API
- **Distributor**: Handles incoming log streams
- **Ingester**: Processes and stores log data
- **Querier**: Executes LogQL queries
- **QueryFrontend**: Queues and accelerates queries
- **Ruler**: Evaluates alert rules
- **Compactor**: Manages index compaction

### 2. **ClusterLogForwarder**
- Forwards logs from OpenShift to LokiStack
- Supports: Application logs, Infrastructure logs, Audit logs
- Uses Vector as the log collector

### 3. **ClusterLogging**
- Manages log collection pipeline
- Configures log collection for all namespaces

### 4. **Storage**
- Object storage (S3-compatible) for log data
- Configurable retention period (default: 30 days)

## Deployment

### Prerequisites

- OpenShift 4.10 or higher
- `oc` CLI installed and configured
- S3-compatible object storage (e.g., OpenShift Data Foundation)
- Appropriate permissions to create namespaces and CRDs

### Quick Start

```bash
# Set S3 credentials (optional - can be set later)
export S3_ACCESS_KEY="your-access-key"
export S3_SECRET_KEY="your-secret-key"

# Deploy LokiStack
./scripts/deploy_lokistack.sh deploy

# Check status
./scripts/deploy_lokistack.sh status
```

### Manual Deployment

```bash
# 1. Create namespaces
oc apply -f infrastructure/openshift/base/logging/logging-namespace.yaml

# 2. Deploy operators
oc apply -f infrastructure/openshift/base/logging/logging-operator.yaml

# 3. Wait for operators to be ready
oc wait --for=condition=ready pod -l name=loki-operator -n openshift-logging --timeout=300s

# 4. Configure storage secrets
# Edit infrastructure/openshift/base/logging/lokistack-storage-secret.yaml
# with your S3 credentials
oc apply -f infrastructure/openshift/base/logging/lokistack-storage-secret.yaml

# 5. Deploy LokiStack
oc apply -f infrastructure/openshift/base/logging/lokistack.yaml

# 6. Wait for LokiStack to be ready
oc wait --for=condition=ready lokistack loki -n openshift-logging --timeout=600s

# 7. Deploy ClusterLogging
oc apply -f infrastructure/openshift/base/logging/clusterlogging.yaml

# 8. Deploy ClusterLogForwarder
oc apply -f infrastructure/openshift/base/logging/clusterlogforwarder.yaml

# 9. Deploy alert rules and route
oc apply -f infrastructure/openshift/base/logging/loki-alert-rules.yaml
oc apply -f infrastructure/openshift/base/logging/loki-route.yaml
```

## Configuration

### S3 Storage

Update `lokistack-storage-secret.yaml` with your S3-compatible storage credentials:

```yaml
stringData:
  endpoint: http://s3.openshift-storage.svc  # Your S3 endpoint
  access_key_id: ${S3_ACCESS_KEY}             # Your access key
  access_key_secret: ${S3_SECRET_KEY}        # Your secret key
  bucketnames: payu-loki                      # Bucket name
  region: us-east-1                           # S3 region
```

### Log Retention

Modify retention settings in `lokistack.yaml`:

```yaml
spec:
  limits:
    global:
      retention:
        days: 30  # Change to desired retention period
```

### Ingestion Limits

Adjust ingestion limits based on your needs:

```yaml
spec:
  limits:
    global:
      ingestion:
        ingestionBurstSize: 16MB
        ingestionRate: 16MB
```

### Alert Rules

Customize alert rules in `loki-alert-rules.yaml`. Default alerts include:

- **HighErrorRate**: Error rate exceeds threshold
- **HighLatency**: 95th percentile latency exceeds threshold
- **DatabaseConnectionError**: Database connection errors detected
- **ServiceDown**: No logs received from service

## Usage

### Querying Logs via CLI

```bash
# Port-forward to Loki gateway
oc port-forward -n openshift-logging svc/loki-gateway-http 3100:3100

# Query logs using curl
curl -s -G http://localhost:3100/loki/api/v1/query_range \
  --data-urlencode 'query={app_kubernetes_io_part_of="payu"}' \
  --data-urlencode 'start=2024-01-01T00:00:00Z' \
  --data-urlencode 'end=2024-01-02T00:00:00Z' \
  --data-urlencode 'limit=100'
```

### Querying Logs via Grafana

1. Access Grafana via OpenShift Console or Route
2. Navigate to Explore
3. Select Loki datasource
4. Write LogQL queries, e.g.:
   ```logql
   {app_kubernetes_io_part_of="payu", level="error"} | logfmt
   ```

### LogQL Examples

```logql
# All PayU application logs
{app_kubernetes_io_part_of="payu"}

# Error logs from account-service
{namespace="payu-prod", app="account-service", level="error"}

# 5xx errors with latency
{level="error"} |= "5xx" | unwrap duration

# Top 10 error messages
topk(10, sum by (message) (count_over_time({level="error"}[5m])))

# Logs containing specific pattern
{app_kubernetes_io_part_of="payu"} |= "database"

# Service downtime (no logs in last 5m)
absent({namespace="payu-prod", app="account-service"}[5m])
```

## Monitoring

### LokiStack Status

```bash
# Check LokiStack health
oc get lokistack loki -n openshift-logging -o yaml

# Check pods
oc get pods -n openshift-logging -l app.kubernetes.io/name=loki

# Check logs
oc logs -n openshift-logging deployment/loki-ingester
```

### Metrics

LokiStack exposes Prometheus metrics:

- `loki_distributor_lines_received_total`: Total log lines received
- `loki_ingester_streams_created_total`: Total streams created
- `loki_querier_queries_total`: Total queries executed
- `loki_request_duration_seconds`: Request latency

Access metrics:

```bash
oc port-forward -n openshift-logging svc/loki-gateway-http 3100:3100
curl http://localhost:3100/metrics
```

## Troubleshooting

### Logs Not Appearing

1. Check ClusterLogForwarder status:
   ```bash
   oc get clusterlogforwarder payu-log-forwarder -n openshift-logging -o yaml
   ```

2. Check Vector logs:
   ```bash
   oc logs -n openshift-logging -l component=collector
   ```

3. Verify LokiStack is healthy:
   ```bash
   oc get lokistack loki -n openshift-logging
   ```

### High Memory Usage

Increase resource limits in `lokistack.yaml`:

```yaml
spec:
  template:
    ingester:
      replicas: 2
      resources:
        requests:
          cpu: 100m
          memory: 512Mi
        limits:
          cpu: 1000m
          memory: 2Gi
```

### Slow Queries

1. Add more querier replicas
2. Increase query parallelism:
   ```yaml
   spec:
     limits:
       maxQueryParallelism: 32
   ```

### Storage Issues

1. Check S3 connectivity:
   ```bash
   oc logs -n openshift-logging deployment/loki-ingester | grep -i s3
   ```

2. Verify storage secret credentials
3. Check bucket permissions

## Security

### TLS

- LokiStack gateway is exposed via OpenShift Route with TLS termination
- Internal communication uses TLS certificates

### RBAC

- ServiceAccount: `loki-promtail`
- ClusterRole: Read access to pods, namespaces, nodes
- ClusterRoleBinding: Binds role to service account

### Secrets

- `loki-storage`: S3 storage credentials
- `loki-token`: Authentication token for log forwarding

## Scaling

### Horizontal Scaling

Increase replicas in `lokistack.yaml`:

```yaml
spec:
  template:
    ingester:
      replicas: 3  # Increase for higher throughput
    querier:
      replicas: 2  # Increase for parallel queries
```

### Size Tiers

Change LokiStack size:

- `1x.small`: Small deployments
- `1x.medium`: Medium deployments
- `1x.large`: Large deployments

```yaml
spec:
  size: 1x.medium
```

## Backup & Recovery

### Backup

LokiStack data is stored in S3. Configure S3 bucket versioning and lifecycle policies:

```yaml
# Example S3 lifecycle policy
{
  "Rules": [
    {
      "Id": "LokiLogRetention",
      "Status": "Enabled",
      "Expiration": {
        "Days": 90
      }
    }
  ]
}
```

### Recovery

To restore from backup:

1. Create new S3 bucket with backup data
2. Update `lokistack-storage-secret.yaml` with new bucket
3. Redeploy LokiStack

## Testing

Run LokiStack tests:

```bash
# Run all LokiStack tests
pytest tests/infrastructure/test_lokistack.py -v

# Run specific test
pytest tests/infrastructure/test_lokistack.py::TestLokiStackInfrastructure::test_lokistack_yaml_valid -v

# Run with coverage
pytest tests/infrastructure/test_lokistack.py --cov=. --cov-report=html
```

## Cleanup

```bash
# Delete LokiStack
./scripts/deploy_lokistack.sh delete

# Or manually
oc delete -f infrastructure/openshift/base/logging/
```

## References

- [Loki Documentation](https://grafana.com/docs/loki/latest/)
- [LokiStack Operator](https://docs.openshift.com/container-platform/4.12/logging/cluster-logging-loki.html)
- [LogQL Reference](https://grafana.com/docs/loki/latest/query/)
- [OpenShift Logging](https://docs.openshift.com/container-platform/4.12/logging/)

## Support

For issues or questions:
- Check LokiStack logs in OpenShift
- Review LokiStack status and conditions
- Consult OpenShift documentation
- Check PayU operations runbook

---

**Last Updated:** January 23, 2026
