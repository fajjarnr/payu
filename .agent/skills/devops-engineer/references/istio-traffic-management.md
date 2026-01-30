# Istio Traffic Management (OpenShift Service Mesh)

Reference templates for configuring traffic routing, resilience, and security using Istio CRDs.

## 1. Resilience Patterns

### Circuit Breaker (`DestinationRule`)
Protect services from cascading failures by ejecting unhealthy instances.

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: payment-service-circuit-breaker
spec:
  host: payment-service
  trafficPolicy:
    connectionPool:
      http:
        http1MaxPendingRequests: 100
        maxRequestsPerConnection: 10
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 100 # Eject all bad pods if necessary
```

### Retry & Timeout (`VirtualService`)
Configure automatic retries for transient failures (e.g., network blips).

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: transaction-retry
spec:
  hosts:
    - transaction-service
  http:
    - route:
        - destination:
            host: transaction-service
      timeout: 5s # Total time allowed
      retries:
        attempts: 3
        perTryTimeout: 1s
        retryOn: connect-failure,503,gateway-error
```

## 2. Release Strategies

### Canary Deployment (Weighted Routing)
Shift traffic gradually from stable to canary version.

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: wallet-canary
spec:
  hosts:
    - wallet-service
  http:
    - route:
        - destination:
            host: wallet-service
            subset: stable
          weight: 90
        - destination:
            host: wallet-service
            subset: canary
          weight: 10
```

### Traffic Mirroring (Dark Launch)
Duplicate production traffic to a new version for testing without user impact.

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: notification-mirror
spec:
  hosts:
    - notification-service
  http:
    - route:
        - destination:
            host: notification-service
            subset: v1
      mirror:
        host: notification-service
        subset: v2 # New version receives copy of traffic
      mirrorPercentage:
         value: 100
```

## 3. Ingress Configuration

### Secure Gateway (`Gateway`)
Expose services to the internet via HTTPS.

```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: payu-gateway
spec:
  selector:
    istio: ingressgateway # Default OpenShift Ingress
  servers:
    - port:
        number: 443
        name: https
        protocol: HTTPS
      tls:
        mode: SIMPLE
        credentialName: payu-tls-cert
      hosts:
        - "api.payu.com"
```
