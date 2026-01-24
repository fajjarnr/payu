# OpenShift Service Mesh Configuration

> Istio-based service mesh configuration for PayU Digital Banking Platform on Red Hat OpenShift 4.20+

## Overview

This directory contains the complete Service Mesh (Istio) configuration for the PayU Digital Banking Platform. The service mesh provides:

- **Zero Trust Security**: mTLS (mutual TLS) for all service-to-service communication
- **Traffic Management**: Load balancing, circuit breaking, and traffic splitting
- **Observability**: Distributed tracing, metrics, and logging
- **Resilience**: Automatic retries, timeouts, and fault injection

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         OPENSHIFT SERVICE MESH                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    ISTIO CONTROL PLANE (istio-system)                │   │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐     │   │
│  │  │   Istiod   │  │  Ingress   │  │   Egress   │  │   Citadel  │     │   │
│  │  │  (Pilot)   │  │  Gateway   │  │  Gateway   │  │    (CA)    │     │   │
│  │  └────────────┘  └────────────┘  └────────────┘  └────────────┘     │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                          │
│  ┌─────────────────────────────────┼──────────────────────────────────────┐  │
│  │                                 │                                      │  │
│  │  ┌─────────────┐    ┌──────────▼──────────┐    ┌─────────────┐       │  │
│  │  │   Account   │    │   Gateway Service   │    │  Wallet     │       │  │
│  │  │   Service   │◄───┤                    ├───►│   Service   │       │  │
│  │  │  (mTLS)     │    │      (mTLS)         │    │   (mTLS)    │       │  │
│  │  └─────────────┘    └─────────────────────┘    └─────────────┘       │  │
│  │                                                                      │  │
│  │  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐              │  │
│  │  │    Auth     │    │ Transaction │    │  Billing    │              │  │
│  │  │   Service   │    │   Service   │    │   Service   │              │  │
│  │  │   (mTLS)    │    │    (mTLS)   │    │    (mTLS)   │              │  │
│  │  └─────────────┘    └─────────────┘    └─────────────┘              │  │
│  │                                                                      │  │
│  │  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐              │  │
│  │  │    KYC      │    │   Notify    │    │ Analytics   │              │  │
│  │  │   Service   │    │   Service   │    │   Service   │              │  │
│  │  │   (mTLS)    │    │    (mTLS)   │    │    (mTLS)   │              │  │
│  │  └─────────────┘    └─────────────┘    └─────────────┘              │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│  All services communicate via mTLS (encrypted, authenticated)              │
│  Envoy sidecar proxy injected into each pod                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Files

| File | Description |
|------|-------------|
| `control-plane.yaml` | ServiceMeshControlPlane, ServiceMeshMemberRoll, HPA, PDB |
| `gateway.yaml` | Ingress Gateway, VirtualServices, AuthorizationPolicy, JWT |
| `destination-rules.yaml` | DestinationRules, traffic policies, load balancing |
| `peer-authentication.yaml` | PeerAuthentication, AuthorizationPolicies, RequestAuthN |

## Prerequisites

Before deploying the service mesh, ensure:

1. **OpenShift 4.20+** is installed and running
2. **OpenShift Service Mesh Operator** is installed from OperatorHub
3. **Sufficient Resources**:
   - Control Plane: 2 CPU, 4 GB RAM minimum
   - Per Service: +0.1 CPU, +128 MB RAM (sidecar proxy)
4. **Network**:
   - Pod network allows communication between all namespaces
   - Service Mesh is configured for your network plugin (OpenShift SDN/OVN)

## Installation

### Step 1: Install OpenShift Service Mesh Operator

```bash
# Create namespace for operators
oc create namespace openshift-operators

# Install Service Mesh Operator via OperatorHub or CLI
oc apply -f https://operatorhub.io/install/openshift-service-matrix.yaml

# Verify operator is ready
oc get pods -n openshift-operators
```

### Step 2: Deploy Service Mesh Control Plane

```bash
# Apply control plane configuration
oc apply -f infrastructure/openshift/service-mesh/control-plane.yaml

# Wait for Istio control plane to be ready
oc wait --for=condition=ready pod -l app=istiod -n istio-system --timeout=300s

# Verify control plane components
oc get pods -n istio-system
```

Expected output:
```
NAME                          READY   STATUS    RESTARTS   AGE
istiod-xxx                    1/1     Running   0          2m
istio-ingressgateway-xxx-xxx  1/1     Running   0          2m
istio-egressgateway-xxx       1/1     Running   0          2m
```

### Step 3: Deploy Gateway Configuration

```bash
# Apply gateway and virtual services
oc apply -f infrastructure/openshift/service-mesh/gateway.yaml

# Verify gateway is ready
oc get pods -n istio-system -l app=istio-ingressgateway
```

### Step 4: Deploy Destination Rules

```bash
# Apply destination rules and traffic policies
oc apply -f infrastructure/openshift/service-mesh/destination-rules.yaml

# Verify destination rules
oc get destinationrules -A
```

### Step 5: Deploy Peer Authentication

```bash
# Apply mTLS and authorization policies
oc apply -f infrastructure/openshift/service-mesh/peer-authentication.yaml

# Verify peer authentication
oc get peerauthentication -A
```

### Step 6: Enable Sidecar Injection

```bash
# Enable automatic sidecar injection for namespaces
oc label namespace payu-prod maistra.io/member-of=istio-system
oc label namespace payu-uat maistra.io/member-of=istio-system
oc label namespace payu-sit maistra.io/member-of=istio-system
oc label namespace payu-dev maistra.io/member-of=istio-system

# Verify namespace membership
oc get namespace -L maistra.io/member-of
```

### Step 7: Restart Services to Inject Sidecars

```bash
# Restart deployments to inject Envoy sidecar
oc rollout restart deployment/account-service -n payu-prod
oc rollout restart deployment/auth-service -n payu-prod
oc rollout restart deployment/transaction-service -n payu-prod
oc rollout restart deployment/wallet-service -n payu-prod
oc rollout restart deployment/gateway-service -n payu-prod

# Verify sidecar injection (2 containers per pod)
oc get pods -n payu-prod
```

Expected output:
```
account-service-xxx-xxx   2/2     Running   0          1m
auth-service-xxx-xxx      2/2     Running   0          1m
```

## Configuration

### Control Plane Settings

| Setting | Value | Description |
|---------|-------|-------------|
| **Version** | v2.6 | Red Hat OpenShift Service Mesh version |
| **mTLS Mode** | STRICT | Enforce mTLS for production |
| **Tracing** | Jaeger | 10% sampling rate |
| **Replicas** | 2 | High availability |

### Gateway Settings

| Setting | Value | Description |
|---------|-------|-------------|
| **Type** | LoadBalancer | External access |
| **HTTP** | Port 80 → Redirect to HTTPS |
| **HTTPS** | Port 443 → Main ingress |
| **TLS Mode** | SIMPLE | Server-side TLS |

### mTLS Modes

| Mode | Environment | Description |
|------|-------------|-------------|
| **STRICT** | prod, uat, preprod | Only mTLS connections allowed |
| **PERMISSIVE** | dev, sit | Allow both mTLS and plain text |
| **DISABLE** | External services | No mTLS (simulators) |

## Verification

### Verify mTLS is Working

```bash
# Check peer authentication
oc get peerauthentication -n payu-prod

# Check mTLS between services
oc exec -it account-service-xxx -n payu-prod -c istio-proxy \
  -- openssl s_client -connect auth-service:8082 -alpn istio

# View proxy configuration
oc exec -it account-service-xxx -n payu-prod -c istio-proxy \
  -- pilot-agent request GET config
```

### Verify Traffic Routing

```bash
# Check virtual services
oc get virtualservices -A

# Check destination rules
oc get destinationrules -A

# Test routing
curl -k https://api.payu.local/health
```

### Verify Observability

```bash
# Check metrics
oc exec -it account-service-xxx -n payu-prod -c istio-proxy \
  -- curl -s localhost:15090/stats/prometheus | grep istio

# View traces in Jaeger
# Port forward to Jaeger
oc port-forward -n istio-system svc/jaeger-query 16686:16686
# Open browser: http://localhost:16686
```

## Operations

### View Service Mesh Dashboard

```bash
# Port forward to Kiali dashboard
oc port-forward -n istio-system svc/kiali 20001:20001
# Open browser: http://localhost:20001
```

### View Service Graph

```bash
# Install Kiali (if not installed)
oc apply -f https://raw.githubusercontent.com/istio/istio/release-1.23/manifests/charts/base/crds/crd-gen.yaml

# Access Kiali dashboard at http://localhost:20001
# Navigate to "Graph" to see service topology
```

### Troubleshooting

#### Issue: Sidecar Not Injected

```bash
# Check if namespace is labeled
oc get namespace payu-prod -L maistra.io/member-of

# Check webhook configuration
oc get mutatingwebhookconfigurations | grep istio

# Manual injection (for debugging)
istioctl kube-inject -f deployment.yaml | oc apply -f -
```

#### Issue: mTLS Connection Failures

```bash
# Check peer authentication mode
oc get peerauthentication -n payu-prod -o yaml

# Check destination rule TLS mode
oc get destinationrules -n payu-prod -o yaml

# View proxy logs
oc logs -f account-service-xxx -n payu-prod -c istio-proxy
```

#### Issue: Gateway Not Routing

```bash
# Check gateway status
oc get gateway -n istio-system

# Check virtual services
oc get virtualservices -n istio-system

# View gateway logs
oc logs -f istio-ingressgateway-xxx -n istio-system
```

## Traffic Management

### Traffic Splitting (Canary Deployment)

```yaml
# Update VirtualService to split traffic
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: account-service-canary
spec:
  http:
    - route:
        - destination:
            host: account-service
            subset: v1
          weight: 90  # 90% to version 1
        - destination:
            host: account-service
            subset: v2
          weight: 10  # 10% to version 2 (canary)
```

### Fault Injection

```yaml
# Inject delays for testing
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: account-service-fault
spec:
  http:
    - fault:
        delay:
          percentage:
            value: 10
          fixedDelay: 5s
      route:
        - destination:
            host: account-service
```

## Security Best Practices

1. **Always use STRICT mTLS** in production environments
2. **Implement least privilege** with AuthorizationPolicies
3. **Rotate certificates** regularly (Citadel does this automatically)
4. **Monitor mTLS connections** for security anomalies
5. **Use network policies** alongside service mesh policies
6. **Enable audit logging** for all security events

## Performance Tuning

### Control Plane Resources

```yaml
# For high-traffic clusters, increase Istiod resources
spec:
  istio:
    pilot:
      resources:
        requests:
          cpu: 1000m
          memory: 4Gi
        limits:
          cpu: 2000m
          memory: 8Gi
```

### Sidecar Resources

```yaml
# Adjust sidecar proxy resources per service
spec:
  template:
    metadata:
      annotations:
        sidecar.istio.io/proxyCPU: "200m"
        sidecar.istio.io/proxyMemory: "256Mi"
        sidecar.istio.io/proxyCPULimit: "500m"
        sidecar.istio.io/proxyMemoryLimit: "512Mi"
```

## Monitoring and Observability

### Metrics

Service mesh exposes metrics at `http://localhost:15090/stats/prometheus`:

- **Request metrics**: Total requests, error rate, latency
- **Connection metrics**: Active connections, mTLS status
- **Service metrics**: Service-to-service calls, retries

### Traces

Distributed traces are sent to Jaeger:

```bash
# Query traces by trace ID
curl -s http://jaeger-query:16686/api/traces/trace-id

# View traces in Kiali
# Navigate to "Tracing" in Kiali dashboard
```

### Logs

Access logs are formatted as JSON:

```json
{
  "start_time": "2026-01-24T10:00:00Z",
  "method": "POST",
  "path": "/v1/accounts",
  "response_code": 201,
  "duration": "45ms",
  "upstream_service": "account-service"
}
```

## Backup and Recovery

### Backup Configuration

```bash
# Export all service mesh resources
oc get servicemeshcontrolplane -n istio-system -o yaml > backup-smcp.yaml
oc get serviceMeshMemberRoll -n istio-system -o yaml > backup-smmr.yaml
oc get gateway,destinationrule,virtualservice -A -o yaml > backup-mesh-config.yaml
```

### Restore Configuration

```bash
# Restore from backup
oc apply -f backup-smcp.yaml
oc apply -f backup-smmr.yaml
oc apply -f backup-mesh-config.yaml
```

## References

- [Red Hat OpenShift Service Mesh Documentation](https://docs.openshift.com/container-platform/4.20/service_mesh/index.html)
- [Istio Documentation](https://istio.io/latest/docs/)
- [Istio Security Best Practices](https://istio.io/latest/docs/concepts/security/)
- [OpenShift Service Mesh Architecture](https://www.redhat.com/en/topics/microservices/what-is-a-service-mesh)

## Support

For issues or questions:

- **Platform Issues**: platform-team@payu.id
- **Architecture**: architect@payu.id
- **Security**: security@payu.id

---

**Version**: 1.0.0
**Last Updated**: January 2026
**Maintained By**: Platform Team PayU
