# PayU Platform - Lessons Learned & Troubleshooting Guide

## 🚀 Deployment & Release Engineering

### 18. High Availability Best Practices - HPA, PDB, and Multi-Replica (Feb 23, 2026)

*   **The Problem**: Single-replica deployments create single points of failure. When a node fails or during maintenance, services become unavailable. Without Pod Disruption Budgets (PDB), voluntary disruptions (node draining, upgrades) can cause total service outage. Without Horizontal Pod Autoscaler (HPA), services cannot handle traffic spikes.

*   **The Solution**: Implement the "Production HA Trio":
    1.  **Multiple Replicas**: Minimum 2 replicas for critical services (gateway, auth, transaction, account, wallet)
    2.  **Pod Disruption Budget (PDB)**: Ensure at least 1 pod remains during disruptions
    3.  **Horizontal Pod Autoscaler (HPA)**: Auto-scale based on CPU utilization (70% target)

*   **Implementation**:

    ```yaml
    # Deployment: 2 replicas with zero-downtime rolling update
    spec:
      replicas: 2
      strategy:
        type: RollingUpdate
        rollingUpdate:
          maxSurge: 1          # Allow 1 extra pod during update
          maxUnavailable: 0    # Never drop below desired replicas
      template:
        spec:
          terminationGracePeriodSeconds: 60  # Graceful shutdown time
          securityContext:
            runAsNonRoot: true
            seccompProfile:
              type: RuntimeDefault
    ```

    ```yaml
    # HPA: Scale 2-5 replicas based on CPU
    apiVersion: autoscaling/v2
    kind: HorizontalPodAutoscaler
    spec:
      scaleTargetRef:
        apiVersion: apps/v1
        kind: Deployment
        name: gateway-service
      minReplicas: 2
      maxReplicas: 5
      metrics:
        - type: Resource
          resource:
            name: cpu
            target:
              type: Utilization
              averageUtilization: 70
      behavior:
        scaleDown:
          stabilizationWindowSeconds: 300  # Wait 5 min before scaling down
        scaleUp:
          stabilizationWindowSeconds: 60   # Scale up faster
    ```

    ```yaml
    # PDB: Ensure high availability during disruptions
    apiVersion: policy/v1
    kind: PodDisruptionBudget
    spec:
      minAvailable: 1
      selector:
        matchLabels:
          app: gateway-service
    ```

*   **Key Lessons**:
    *   Critical services (gateway, auth, transaction, account, wallet) should have minimum 2 replicas
    *   Use `maxUnavailable: 0` to prevent capacity drop during rolling updates
    *   Set `terminationGracePeriodSeconds: 60` for Spring Boot to allow graceful shutdown
    *   HPA prevents manual intervention during traffic spikes
    *   PDB prevents accidental downtime during node maintenance
    *   Apply HPA/PDB to the same namespace as deployments: `oc apply -f hpa.yaml -n payu-dev`

*   **Verification Commands**:
    ```bash
    # Check replica status
    oc get deployment -n payu-dev

    # Check HPA status
    oc get hpa -n payu-dev

    # Check PDB status
    oc get pdb -n payu-dev

    # Verify pod distribution across nodes
    oc get pods -n payu-dev -o wide
    ```

### 15. OpenShift Image Registry and Kustomize Deployment (Feb 23, 2026)

*   **The Problem**: Deploying to OpenShift requires proper image registry configuration and Kustomize orchestration. Common issues include:
    *   Container images not accessible due to missing `defaultRoute` in image registry
    *   Pods stuck in `ImagePullBackOff` because images aren't pushed to the internal registry
    *   Inconsistent image tags between Kustomize overlays and actual built images
    *   Missing secrets (db-credentials, jwt-secret, redis-credentials) causing `CreateContainerConfigError`

*   **The Solution**: Follow the proper deployment sequence with registry configuration:

    ```bash
    # 1. Enable defaultRoute for OpenShift image registry
    oc patch configs.imageregistry.operator.openshift.io cluster --type=merge \
        -p '{"spec":{"defaultRoute":true}}'

    # 2. Get registry route
    REGISTRY=$(oc get route default-route -n openshift-image-registry -o jsonpath='{.spec.host}')

    # 3. Login to registry
    podman login -u kubeadmin -p $(oc whoami -t) $REGISTRY --tls-verify=false

    # 4. Tag and push images
    podman tag localhost/payu-<service>:1.3.0 $REGISTRY/payu-dev/<service>:1.3.0
    podman push $REGISTRY/payu-dev/<service>:1.3.0 --tls-verify=false

    # 5. Apply Kustomize in correct order
    oc apply -k infrastructure/openshift/operators/          # Operators
    oc apply -k infrastructure/openshift/infra/overlays/dev/ # Infrastructure
    oc apply -k infrastructure/openshift/overlays/dev/       # Applications
    ```

*   **Required Secrets for Application Startup**:
    ```bash
    # Database credentials
    oc create secret generic db-credentials \
        --from-literal=username=payu \
        --from-literal=password=<postgres-password>

    # JWT secrets
    oc create secret generic jwt-secret \
        --from-literal=JWT_SECRET=<256-bit-secret> \
        --from-literal=REFRESH_TOKEN_SECRET=<256-bit-secret>

    # Redis credentials (DataGrid/Infinispan)
    oc create secret generic redis-credentials \
        --from-literal=url="redis://developer:payu-cache-dev@payu-datagrid.payu-dev.svc:11222" \
        --from-literal=REDIS_PASSWORD=payu-cache-dev \
        --from-literal=REDIS_USERNAME=developer
    ```

*   **Key Lessons**:
    *   Always update image tags in `kustomization.yaml` before deployment
    *   Use `oc set image` to patch deployments if images change after initial deployment
    *   Check pod events with `oc describe pod` for detailed error messages
    *   Verify ImageStreams are created: `oc get is -n payu-dev`

### 14. Zero-Downtime Deployment Strategies (Feb 20, 2026)

*   **The Problem**: Traditional deployments cause service interruptions (503 errors, connection drops) when pods restart. For a financial platform, even brief downtime is unacceptable. Rolling updates have gray periods where old and new versions coexist, potentially causing data inconsistencies.

*   **The Solution**: Implement three deployment strategies with automated rollback capabilities:
    *   **Blue-Green Deployment**: Maintain two identical environments (blue=stable, green=new)
        *   Deploy new version to inactive environment
        *   Run full health verification before traffic switch
        *   Instant traffic switch via route patch (~10 seconds)
        *   Keep old version for immediate rollback (~30 seconds)
    *   **Canary Releases**: Progressive traffic shifting with monitoring
        *   Start with 10% traffic to new version
        *   Monitor error rates, latency, resource usage
        *   Progressive promotion: 10% → 25% → 50% → 75% → 100%
        *   Instant rollback on threshold breach
    *   **Rolling Deployment**: Native Kubernetes rolling update for low-risk changes

*   **Key Implementation Details**:
    ```bash
    # Blue-Green: Instant traffic switch
    oc patch route gateway-service -p '{"spec":{"to":{"name":"gateway-service-green"}}}'

    # Canary: Progressive traffic split
    oc patch virtualservice gateway-service --type='json' -p '[{
        "op": "replace",
        "path": "/spec/http/0/route",
        "value": [
            {"destination": {"host": "gateway-service"}, "weight": 90},
            {"destination": {"host": "gateway-service-canary"}, "weight": 10}
        ]
    }]'
    ```

*   **Database Migration Safety (Expand-Contract Pattern)**:
    *   **Phase 1 (Expand)**: Add new columns/tables, keep old ones
    *   **Phase 2 (Migrate)**: Backfill data, dual-write to both schemas
    *   **Phase 3 (Contract)**: Remove old columns in subsequent release
    *   Never modify existing columns in-place during deployment

*   **Automated Safety Checks**:
    *   Pre-deployment: Verify current version health, check DB compatibility
    *   During deployment: Monitor pod readiness, health endpoints, error rates
    *   Post-deployment: 5-minute monitoring window with automatic rollback
    *   Rollback triggers: >1% error rate, >500ms P95 latency, pod restarts >2

*   **Rollback Decision Matrix**:
    | Metric | Threshold | Action |
    |--------|-----------|--------|
    | Error Rate | > 1% | Immediate rollback |
    | P95 Latency | > 500ms | Immediate rollback |
    | CPU Usage | > 90% for 5min | Evaluate rollback |
    | Pod Restarts | > 2 | Immediate rollback |

*   **Result**: Zero-downtime deployment capability with <30 second rollback time, suitable for production financial services requiring 99.99% availability.

## 📊 Logging & Observability

### 12. Logging Standardization Across Polyglot Services (Feb 19, 2026)

*   **The Problem**: With 21 backend services using different technologies (Spring Boot, Quarkus, Python), logs had inconsistent formats, making aggregation and analysis in LokiStack difficult. Correlation IDs and trace IDs were not propagated consistently.
*   **The Standard**: Created unified logging approach with JSON format compatible with LokiStack and OpenTelemetry:
    *   **Spring Boot**: `logging-starter` shared module using Logstash Logback Encoder
    *   **Quarkus**: JSON logging configuration with standard MDC keys
    *   **Python**: `payu-logging` package using structlog with JSON output
*   **Key Components**:
    *   Standard MDC keys: `correlation_id`, `trace_id`, `span_id`, `service`
    *   JSON format with `timestamp`, `level`, `logger`, `message`, `mdc` fields
    *   Auto-configuration via Spring Boot starters or FastAPI middleware
*   **Integration Pattern**:
    ```xml
    <!-- Spring Boot: Add dependency only -->
    <dependency>
        <groupId>id.payu</groupId>
        <artifactId>logging-starter</artifactId>
    </dependency>
    ```
    ```python
    # Python: Initialize in main.py
    from payu_logging import init_logging, get_logger
    init_logging(service_name="kyc-service", json_format=True)
    ```
*   **Result**: All 21 services now produce consistent JSON logs with correlation tracking, enabling effective distributed tracing and centralized log analysis.

## 🛡️ Rate Limiting & API Protection

### 17. Gateway Rate Limiting Best Practices (Feb 23, 2026)

*   **The Problem**: Default rate limiting configurations are often too restrictive (5 req/min for auth) or too permissive, causing:
    *   Legitimate users blocked during login retries (too strict)
    *   Brute force attacks possible (too permissive)
    *   No differentiation between endpoint sensitivity levels
    *   Poor IP-based tracking behind proxies

*   **The Solution**: Implement differentiated rate limiting with best practices:

    ```yaml
    # application.yaml - Rate Limit Configuration
    gateway:
      rate-limit:
        enabled: true
        default:
          requests-per-minute: 100
          burst: 150
        endpoints:
          # Auth: Higher limits for login retries (users may mistype)
          auth:
            requests-per-minute: 30    # 1 attempt every 2 seconds
            burst: 50                   # Allow initial burst
          # OTP: Strict limits (security critical)
          otp:
            requests-per-minute: 5
            burst: 8
          # Financial: Moderate limits
          transfer:
            requests-per-minute: 10
            burst: 20
          # Read-only: Higher limits
          balance:
            requests-per-minute: 30
            burst: 50
    ```

*   **Key Implementation Details**:
    *   **Differentiated Windows**: Auth/OTP = 5 min, Default = 1 min
    *   **IP Extraction**: Support X-Forwarded-For and X-Real-IP headers
    *   **Sliding Window**: Use Redis INCR with TTL for atomic operations
    *   **Fail Open**: Allow requests if Redis is unavailable
    *   **Proper Headers**: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Window

*   **Rate Limit Categories**:
    | Category | Requests/Min | Burst | Window | Use Case |
    |----------|--------------|-------|--------|----------|
    | auth | 30 | 50 | 5 min | Login attempts |
    | otp | 5 | 8 | 5 min | OTP generation |
    | transfer | 10 | 20 | 1 min | Financial transactions |
    | balance | 30 | 50 | 1 min | Balance checks |
    | default | 100 | 150 | 1 min | General API |

*   **Code Implementation** (`RateLimitFilter.java`):
    ```java
    // Get real client IP considering proxies
    private String getClientIp(ContainerRequestContext ctx) {
        String forwarded = ctx.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = ctx.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return "unknown";
    }
    ```

*   **Result**: Balanced protection - prevents abuse while allowing legitimate user workflows.

## 🔐 Identity & Access Management

### 16. Keycloak User Seeding and Client Configuration (Feb 23, 2026)

*   **The Problem**: After deploying PayU to OpenShift, login fails with "Invalid credentials" even when users exist in the database. This is because:
    *   Keycloak admin password may differ from the one in secrets
    *   The required `payu-backend` client is not created by default
    *   Test users don't exist in Keycloak realm
    *   Web-app expects users to be available for immediate login

*   **The Solution**: Create a Keycloak user seeder script and configure the required client:

    ```bash
    #!/bin/bash
    # scripts/keycloak-seeder.sh

    KEYCLOAK_URL="https://keycloak-payu-dev.apps.payu.ocp.fajjjar.my.id"
    ADMIN_USER="admin"
    ADMIN_PASS=$(oc get secret keycloak-credentials -n payu-dev -o jsonpath='{.data.KEYCLOAK_ADMIN_PASSWORD}' | base64 -d)
    REALM="payu"

    # Get admin token
    ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/auth/realms/master/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "username=${ADMIN_USER}" \
      -d "password=${ADMIN_PASS}" \
      -d "grant_type=password" \
      -d "client_id=admin-cli" \
      | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

    # Create payu-backend client
    curl -s -X POST "${KEYCLOAK_URL}/auth/admin/realms/${REALM}/clients" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" \
      -d '{
        "clientId": "payu-backend",
        "name": "PayU Backend",
        "enabled": true,
        "clientAuthenticatorType": "client-secret",
        "secret": "payu-backend-secret",
        "redirectUris": ["*"],
        "webOrigins": ["*"],
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": true,
        "publicClient": false
      }'

    # Create test user
    curl -s -X POST "${KEYCLOAK_URL}/auth/admin/realms/${REALM}/users" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" \
      -d '{
        "username": "customer1",
        "email": "customer1@payu.id",
        "firstName": "Customer",
        "lastName": "One",
        "enabled": true,
        "emailVerified": true
      }'
    ```

*   **Test Credentials After Seeding**:
    | Username | Email | Password |
    |----------|-------|----------|
    | customer1 | customer1@payu.id | password123 |
    | customer2 | customer2@payu.id | password123 |
    | admin | admin@payu.id | admin123 |

*   **Verification Commands**:
    ```bash
    # Test direct Keycloak login
    curl -s -X POST "https://keycloak-payu-dev.apps.payu.ocp.fajjjar.my.id/auth/realms/payu/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "username=customer1" \
      -d "password=password123" \
      -d "grant_type=password" \
      -d "client_id=payu-backend" \
      -d "client_secret=payu-backend-secret"

    # Test via Gateway
    curl -s -X POST https://gateway-payu-dev.apps.payu.ocp.fajjjar.my.id/api/v1/auth/login \
      -H "Content-Type: application/json" \
      -H "X-Client-Id: web-app" \
      -d '{"username":"customer1","password":"password123"}'
    ```

*   **Key Lessons**:
    *   Always verify the correct admin password from `keycloak-credentials` secret
    *   The `payu-backend` client must be created before users can authenticate via API
    *   Use `directAccessGrantsEnabled: true` for password-based authentication
    *   Rate limiting may activate after multiple failed login attempts

### 20. Token Refresh and JWT Validation Issues (Feb 23, 2026)

*   **The Problem**: After successful login, users were stuck in a redirect loop because access tokens would expire, and refresh requests to `/api/v1/auth/refresh` returned HTTP 500. Additionally, backend services returned HTTP 401 despite valid Keycloak tokens.
*   **The Root Cause**:
    1.  **Auth Service**: Tried to rotate a custom refresh token locally via `RefreshTokenService` which caused internal errors because the token originated from Keycloak and couldn't be parsed correctly locally.
    2.  **Resource Servers (Wallet, Transaction, etc.)**: The `OIDC_ISSUER` environment variable was set to the default localhost URL inside the cluster, causing JWT signature validation to fail.
    3.  **Hikari Config**: `wallet-service` failed to execute read operations returning 500s because Hikari was incorrectly set to `auto-commit: true` while Spring JPA expects transaction boundaries to manage commits.
*   **The Fix**:
    1.  Directly pass the Keycloak refresh token back to Keycloak's token endpoint (`refreshTokenBlocking`) in `AuthController` and don't try to parse it locally.
    2.  Set `OIDC_ISSUER="http://keycloak-discovery:8080/auth/realms/payu"` on all backend resource server deployments (`oc set env ...`).
    3.  Ensure `auto-commit: false` is set in all `application-container.yml` database configurations for Spring Boot JPA compatibility.

## 🧪 Load Testing & Performance Engineering

### 13. K6 CRUD Load Testing Best Practices (Feb 20, 2026)

*   **The Problem**: Basic health check load tests (ping endpoints) don't reflect real-world usage. They miss critical performance characteristics like:
    *   Write vs read performance differences (writes are more expensive)
    *   Database contention under concurrent CREATE/UPDATE/DELETE operations
    *   Data consistency issues (read-after-write, transaction atomicity)
    *   Real-world bottlenecks in business logic

*   **The Solution**: Implement comprehensive CRUD load testing with K6:
    *   **Modular Library Architecture**: Reusable CRUD functions in `lib/` directory
        *   `lib/auth.js` - Login, register, profile CRUD
        *   `lib/wallet.js` - Wallet/pocket CRUD with credit/freeze/close
        *   `lib/transaction.js` - Transfer, history, QRIS operations
        *   `lib/card.js` - Virtual card CRUD with freeze/unfreeze
    *   **Test Coverage**:
        *   `crud-load-test.js` - 100 VU, 25min sustained load
        *   `crud-stress-test.js` - 1000 VU, 40min breaking point analysis
        *   `crud-data-consistency-test.js` - Consistency validation under load

*   **Key Patterns**:
    ```javascript
    // Reusable CRUD function with metrics
    export function createPocket(gatewayUrl, token, pocketData) {
        const startTime = Date.now();
        const response = http.post(url, payload, { headers });

        crudCreateDuration.add(Date.now() - startTime);
        const success = check(response, { 'status is 201': (r) => r.status === 201 });
        crudCreateSuccess.add(success);

        return { success, body: JSON.parse(response.body) };
    }
    ```

*   **Consistency Tests**:
    *   Read-after-write: Create resource → immediately read (should find)
    *   Transaction atomicity: Transfer with idempotency key → verify retrieval
    *   Concurrent updates: Multiple rapid credits → verify final balance

*   **Test Data Strategy**:
    *   Use unique identifiers per VU: `k6-${Date.now()}-${Math.random()}`

### 19. K6 Baseline Testing for All Microservices (Feb 23, 2026)

*   **The Problem**: Performance testing is often limited to a few critical services (auth, transaction) while supporting services (notification, support, compliance) are untested. This leads to:
    *   Undetected performance regressions in supporting services
    *   No established SLAs for service-level agreements
    *   Inconsistent load testing patterns across teams
    *   Difficulty identifying which services need optimization

*   **The Solution**: Create a comprehensive K6 baseline test suite covering all 22 PayU microservices with standardized patterns:
    *   **Directory Structure**:
        ```
        k6-baseline/
        ├── config/baseline-config.js          # Shared config, thresholds, SLAs
        ├── lib/auth-helper.js                 # Login, MFA, token refresh
        ├── lib/crud-helper.js                 # Generic CRUD operations
        ├── core-services/                      # 4 critical services
        ├── financial-services/                 # 5 financial products
        ├── supporting-services/                # 11 infrastructure services
        └── unified-baseline-runner.js         # Multi-service test runner
        ```
    *   **Standardized Test Structure** (every service follows same pattern):
        ```javascript
        // 1. Service-specific metrics
        const serviceMetrics = {
          operationNameDuration: new Trend('service_operation_duration'),
        };

        // 2. Test configuration
        export const options = {
          stages: BASELINE_STAGES,  // Shared load profile
          thresholds: BASELINE_THRESHOLDS,  // Shared SLAs
        };

        // 3. Test data generators
        function generateTestData(uniqueId) { return { /* ... */ }; }

        // 4. Main test scenario with grouped operations
        export default function () {
          group('Service - CRUD Operations', () => {
            group('CREATE: Operation', () => { /* ... */ });
            group('READ: Operation', () => { /* ... */ });
            group('UPDATE: Operation', () => { /* ... */ });
            group('DELETE: Operation', () => { /* ... */ });
          });
        }
        ```

*   **Key Implementation Details**:
    *   **SLA Thresholds** (production grade):
        ```javascript
        BASELINE_THRESHOLDS = {
          http_req_duration: [
            { threshold: 'p(50)<100', abortOnFail: false },
            { threshold: 'p(95)<300', abortOnFail: false },
            { threshold: 'p(99)<500', abortOnFail: false },
          ],
          http_req_failed: ['rate<0.001'],  // 0.1% error rate
        }
        ```
    *   **Load Profile** (5-stage):
        ```javascript
        BASELINE_STAGES = [
          { duration: '30s', target: 5 },     // Warm up
          { duration: '2m', target: 10 },     // Baseline load
          { duration: '5m', target: 20 },     // Sustained baseline
          { duration: '2m', target: 10 },     // Ramp down
          { duration: '30s', target: 0 },     // Cool down
        ]
        ```
    *   **Service-Specific SLAs**: Different targets based on service criticality
        *   Core (auth, wallet): p95 < 300ms
        *   Financial (lending, statement): p95 < 500ms
        *   Analytics: p95 < 800ms (complex queries expected)

*   **CRUD Helper Library** (`lib/crud-helper.js`):
    *   Generic `create()`, `read()`, `list()`, `update()`, `patch()`, `del()` functions
    *   Automatic metrics collection (duration, success rate)
    *   JSON parsing with error handling
    *   Health check utility
    ```javascript
    export function create(endpoint, payload, token, options = {}) {
      const startTime = new Date();
      const response = http.post(url, JSON.stringify(payload), { headers });
      crudMetrics.createDuration.add(new Date() - startTime);
      // ... checks and return
    }
    ```

*   **Running Baseline Tests**:
    ```bash
    # Individual service
    k6 run core-services/wallet-service-crud.js

    # Multiple services via unified runner
    k6 run unified-baseline-runner.js --env SERVICES=wallet,transaction,auth

    # All services
    k6 run unified-baseline-runner.js
    ```

*   **Best Practices**:
    *   Use `group()` for clear test reporting and organization
    *   Define service-specific metrics for granular monitoring
    *   Create realistic data generators matching production patterns
    *   Include setup/teardown with health checks
    *   Use `sleep()` between operations to simulate realistic user behavior
    *   Rotate through test users: `login(__VU % 5)` to distribute load
    *   Store entity IDs from CREATE operations for subsequent READ/UPDATE/DELETE tests

*   **Interpreting Results**:
    *   Pass criteria: All checks pass, error rate < 0.1%, response times meet SLAs
    *   Run 3-5 times to establish stable baselines (variations > 10% warrant investigation)
    *   Compare results against previous runs to detect performance regressions
    *   Use JSON output (`--out json=results.json`) for detailed analysis and CI integration
    *   Rotate through test users: `TEST_USERS[__VU % TEST_USERS.length]`
    *   Weighted operations (40% read, 25% create, 20% transfer, 15% card)

*   **Metrics to Track**:
    *   Operation-specific success rates: `crud_create_success`, `crud_read_success`
    *   Latency by operation type: `crud_create_duration`, `crud_read_duration`
    *   Business metrics: `transfer_amount_total`, `pocket_created_total`
    *   Consistency metrics: `read_after_write_consistency`, `transaction_atomicity`

*   **Result**: Comprehensive understanding of platform performance under realistic CRUD workloads, with data consistency validation and breaking point identification.

### 14. Disaster Recovery Testing on OpenShift (Feb 20, 2026)

*   **The Problem**: DR plans documented but never tested lead to false confidence. Without live testing, RTO/RPO targets are unverified assumptions, and recovery procedures may be outdated or incomplete.
*   **The Approach**: Create automated DR test scripts that can be run regularly to verify recovery procedures:
    *   **PostgreSQL Failover Test** (`scripts/dr-test-postgres-failover.sh`):
        *   Deletes primary pod to simulate failure
        *   Measures failover time (RTO verification)
        *   Verifies new primary accepts connections
        *   Confirms old primary recovers as standby
    *   **Kafka Broker Recovery Test** (`scripts/dr-test-kafka-failover.sh`):
        *   Tests broker pod failure scenarios
        *   Verifies topic integrity after recovery
        *   Measures message publishing/consumption continuity
*   **Key Learnings**:
    *   **RTO Measurement**: Automated timing from failure detection to service restoration
    *   **Data Integrity Checks**: Post-recovery verification of database consistency
    *   **Patroni Behavior**: Understanding automatic vs manual failover triggers
    *   **Operator Patterns**: Leveraging OpenShift operators (Crunchy PGO, AMQ Streams) for managed recovery
*   **DR Runbook Structure** (`docs/operations/DISASTER_RECOVERY.md`):
    *   Per-component RTO/RPO definitions
    *   Service priority tiers (P0 critical, P1 high, P2 medium, P3 low)
    *   Step-by-step recovery procedures with copy-paste commands
    *   Escalation matrix and incident response workflow
*   **Test Schedule**:
    *   Backup verification: Daily (automated)
    *   PostgreSQL failover: Weekly (manual)
    *   Kafka recovery: Weekly (manual)
    *   Full DR simulation: Quarterly
*   **Result**: Verified DR procedures with measurable RTO/RPO, automated test scripts for regression testing, and comprehensive runbook for incident response.

## 🐳 Containerization & Podman Compose

### 1. Podman-Compose Compatibility

* **Volume Syntax**: Current versions of `podman-compose` may fail with advanced Docker Compose volume types (like `type: persistent`). Use standard bind-mount or named volume syntax:

    ```yaml
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ```

* **Short-name Resolution**: Podman requires fully qualified image names to avoid interactive prompts. Always prepend `docker.io/library/` or `docker.io/` for official/public images.
* **Local Image Tagging**: Always provide an `image:` tag (e.g., `localhost/payu-service`) when using the `build` directive. This prevents Podman from using random hex IDs which makes referencing images easier.

### 2. Monorepo Build Contexts

* **The Shared Library Trap**: In a monorepo (like `backend/`), setting the build `context` to the service subfolder prevents access to shared siblings (e.g., `backend/shared/`).
* **The Fix**:
    1. Set `context` to the parent directory (e.g., `../../backend`).
    2. Set `dockerfile` to the relative path (e.g., `service-name/Dockerfile`).
    3. Update the `Dockerfile` to `COPY . .` from the root context.
    4. **Crucial**: Use Maven project selection flags `-pl :service-name -am` to build only the target service and its local dependencies.
    5. Update `COPY --from=build` paths to point into the service-specific `target` folder: `COPY --from=build /build/service-name/target/app.jar ...`

### 3. Permissions and Package Installation

* **UBI User Switching**: Red Hat UBI images often default to a non-root user (like `jboss` or `node`).
* **The Fix**: Always switch back to `USER root` before running `microdnf` or `dnf` to install packages (like `curl`), then switch back to the application user (e.g., `USER 185` or `USER 1001`).

    ```dockerfile
    USER root
    RUN microdnf install -y curl && microdnf clean all
    USER 185
    ```

### 4. Environment Variable Precision

* **Explicit over Implicit**: Even if `application.yml` has defaults, explicitly define `DB_URL`, `KAFKA_BROKERS`, and `REDIS_HOST` in `podman-compose.yml`.
* **Profile Activation**: Always set `SPRING_PROFILES_ACTIVE: container` to ensure container-specific configurations are loaded.
* **UBI9 Minimal & Curl**: The standard `curl` package conflicts with `curl-minimal` in UBI9 minimal images.
  * **The Fix**: Use `microdnf install -y curl-minimal` instead. If you must use full curl, you might need `--allowerasing` (though `curl-minimal` is usually sufficient for healthchecks).

### 5. Memory Limits & OOM Kills (Exit Code 137)

* **The Problem**: Java applications (especially Quarkus/Spring Boot) in containers may be killed by the OOM Killer (Exit Code 137) if the container memory limit is too tight compared to the JVM heap requirements.
* **The Symptom**: Container starts, runs for a few seconds/minutes, then exits silently or with "Killed". `podman ps` shows "Exited (137)".
* **The Fix**: Increase the `mem_limit` or `deploy.resources.limits.memory`.
  * **Example**: Updating `dukcapil-simulator` from `256M` to `512M` resolved startup crashes.
  * **Note**: JVM `MAX_RAM_PERCENTAGE` automatically adjusts heap size based on container limits, but overhead (metaspace, thread stacks, native memory) must also fit within the limit.

### 6. Port Standardization (Feb 2026 Mass Update)

*   **The Problem**: Managing 22 different internal ports (8001-8099) caused constant "unhealthy" statuses and broken gateways because of mismatches between `application.yml`, Dockerfiles, and `docker-compose` healthchecks.
*   **The Standard**: All 22 microservices (Java, Python, Quarkus) MUST listen on internal port **8080**.
*   **Why?**:
    *   **Convention over Configuration**: DNS-based service discovery (e.g., `http://service-name:8080`) is more reliable than remembering unique ports.
    *   **Cloud-Native Compliance**: Standard port for non-root containers in OpenShift/K8s.
    *   **Unified Monitoring**: Simple, consistent healthcheck and Prometheus scrape configs.
*   **The Implementation**:
    *   **Dockerfile**: Universal `EXPOSE 8080`.
    *   **Application**: Enforce `server.port=8080` or use `PORT` env var default.
    *   **Compose**: Use unique host ports (e.g., `8001:8080`) but always point healthcheck to `localhost:8080`.
    *   **Gateway**: Standardize all backend URLs to port 8080.

### 7. Environment vs. Persistence Mismatches

*   **The Problem**: Changing a password in `.env` (e.g., `POSTGRES_PASSWORD`) does **not** update the password of an existing, persistent database volume. The container starts, but applications fail to connect with "Password authentication failed".
*   **The Fix**:
    1.  **Reset**: Delete the volume (`podman volume rm ...`) to let it recreate with the new password (DATA LOSS WARNING).
    2.  **Sync**: Update `.env` to match the *actual* password currently used by the database (Safe).
    3.  **SQL**: Manually change the password via `ALTER USER` inside the database.

### 8. Python ML Containerization Strategy (Feb 4, 2026)
*   **The Problem**: Red Hat UBI9 Minimal images are excellent for security but lack system libraries required for ML/CV tasks (like OpenCV's dependency on `libGL.so.1` and `libgomp.so.1`).
*   **The Fix**: For services requiring heavy C-extensions (OpenCV, PyTorch, PaddleOCR), use `python:3.12-slim` (Debian-based) instead of UBI9. It simplifies installing system dependencies:
    ```dockerfile
    RUN apt-get update && apt-get install -y libgl1 libglib2.0-0 libgomp1 curl
    ```
*   **Performance Boost**: Switch from `pip` to `uv` (Astral) for package installation. Reduces build time for heavy ML libraries (PyTorch, Pandas) from 10m to 1.5m.
    ```dockerfile
    COPY --from=ghcr.io/astral-sh/uv:latest /uv /uv
    RUN /uv pip install --system --no-cache -r requirements.txt
    ```

### 9. Spring Boot Monorepo Build Pattern (Feb 4, 2026)
*   **The Problem**: Docker builds for services relying on local shared modules (`backend/shared/`) fail because the build context is often restricted to the service directory.
*   **The Fix**: "Decoupled Build" strategy.
    1.  **Build Artifacts on Host** (using root POM): `mvn -pl :service-name -am package`
    2.  **Copy Artifacts to Context**: `cp target/app.jar backend/service/target/`
    3.  **Simple Dockerfile**: `COPY target/app.jar /deployments/`
    This avoids complex Docker context juggling and leverages local Maven cache.

### 10. Pydantic Model Field Conflicts (Feb 4, 2026)
*   **The Problem**: Defining a class method named `success()` on a Pydantic model that has a field named `success` causes `AttributeError` at runtime. Pydantic v2 internals conflict with the method name.
*   **The Fix**: Rename factory methods to avoid colliding with field names. Use `create_success()` or `build_success()` instead of just `success()`.

### 11. ML Service Memory Limits (Feb 5, 2026)
*   **The Problem**: ML Services (KYC, Analytics) using PyTorch/PaddleOCR crash with "Killed" or Exit 137 immediately upon loading models if memory limit is too low (e.g., 512MB).
*   **The Fix**: Increase memory limits for ML containers.
    ```yaml
    resources:
      limits:
        memory: 2G  # Increased from 512M
      reservations:
        memory: 1G
    ```


## 🛠️ Build & Dependency Management

### 1. Multi-Module Project Dependencies

*   **GroupId Consistency**: In a multi-module Maven project where submodules are grouped (e.g., `backend/shared/`), ensure dependency references use the correct `groupId`.
    *   **Example**: `id.payu:api-commons` vs `id.payu.shared:api-commons`. An incorrect GroupId leads to build failures finding the artifact, even if the ArtifactId is correct.

### 2. Monorepo Scripting

*   **Context Path Traps**: When writing support scripts (Python/Bash) for a monorepo, do not rely solely on the `build context` path from `compose.yml` to check for file existence (like `pom.xml`).
    *   **Better Approach**: Resolve paths based on the `Dockerfile` location or explicitly handle the subdirectory structure.

### 3. Pact CLI Installation
*   **Correct Package Name**: Use `@pact-foundation/pact-cli` instead of the legacy `@subosito/pact-js-cli` to avoid "Package not found" errors during setup.

### 4. GPG Keyring Practices (Ubuntu 24.04+)
*   **Avoid `apt-key`**: The `apt-key` command is deprecated. Use `/etc/apt/keyrings` and `gpg --dearmor` for better security and compatibility.
*   **Example (Trivy/k6)**:
    ```bash
    wget -qO - https://.../public.key | sudo gpg --dearmor -o /etc/apt/keyrings/tool.gpg
    echo "deb [signed-by=/etc/apt/keyrings/tool.gpg] https://..." | sudo tee /etc/apt/sources.list.d/tool.list
    ```

## ☕ Java & Spring Boot

### 1. Naming Consistency (Entity vs Repo vs Test)

*   **The Issue**: Discrepancies between `userId` and `customerId` often lead to `cannot find symbol` or `BeanCreationException` during Flyway/JPA initialization.
*   **Lesson**: Standardize on `customerId` for all external-facing IDs across the platform.

### 2. Custom Annotations & Enums

*   **Inner Class Resolution**: When using custom annotations with inner enums (like `@Audited(level = AuditLevel.INFO)`), Java may fail to resolve the enum if not fully qualified or correctly imported.
*   **Correction**: Use `Audited.AuditLevel.INFO` to guarantee resolution.

### 4. Ambiguous Enum References (Swagger vs Security Starter)

*   **The Problem**: Importing `id.payu.security.annotation.Audited.Operation` can conflict with `io.swagger.v3.oas.annotations.Operation`, leading to `reference to Operation is ambiguous` compilation errors.
*   **The Fix**: Use semi-qualified names in annotations: `@Audited(operation = Audited.Operation.CREATE, ...)` instead of importing the inner enum directly.

### 5. Abstract Exception Instantiation in Tests

*   **The Problem**: Making a base domain exception `abstract` prevents direct instantiation in unit tests, leading to compilation errors.
*   **The Fix**: Either make the base exception concrete with a generic error code (e.g., `COMPLIANCE_GENERIC_ERROR`) or ensure tests always use a concrete subclass.

### 3. JPA Entity Architecture (Pragmatic Hexagonal)

*   **The Problem**: In a Hexagonal Architecture, repositories were extending `JpaRepository` using standard Domain Models (`ScheduledTransfer`, `Transaction`) that lacked `@Entity` annotations.
*   **The Symptom**: `UnsatisfiedDependencyException`: Not a managed type.
*   **The Fix**: Annotate the Domain Model class with `@Entity`, `@Table`, and `@Id`.
*   **Best Practice**: Ensure ALL classes used in `JpaRepository<T, ID>` are properly annotated entities.

### 4. Value Object Mapping

*   **The Problem**: `Money` Value Object (containing `amount` and `currency`) cannot be persisted directly without `@Embedded` or `AttributeConverter`.
*   **The Legacy Fix**: Using deprecated `amountValue` and `currencyCode` fields mapped with `@Column`, while marking the main `Money` object as `@Transient`.

### 5. JPA Boolean Naming

*   **The Issue**: Derived Query Methods (like `findByActiveTrue`) expect a field named `active`. If the field is `isActive`, the method must be `findByIsActiveTrue`.

## 🔄 CQRS & Architectural Refactoring (Feb 2026)

### 1. Mockito Mutation Trap (Capture-by-Reference)

*   **The Problem**: When testing services that mutate the same object across multiple repository `save()` calls (common in Saga flows), `verify(...).save(argThat(...))` or `ArgumentCaptor` will only show the **final state** of the object for all invocations.
*   **The Symptom**: A test checking if a transaction was saved as `PENDING` then `VALIDATING` fails because Mockito reports it was `VALIDATING` both times.
*   **The Fix**: Use a custom `Answer` to collect the object's state at the exact moment of invocation.

    ```java
    List<Status> capturedStatuses = new ArrayList<>();
    when(repository.save(any())).thenAnswer(inv -> {
        Transaction t = inv.getArgument(0);
        capturedStatuses.add(t.getStatus()); // Hand-copy state here
        return t;
    });
    // ... execution ...
    assertThat(capturedStatuses).containsExactly(Status.PENDING, Status.VALIDATING);
    ```

### 2. Controller Slice Test Isolation (JPA Interference)

*   **The Problem**: `@WebMvcTest` (slice test) attempts to load the full `@SpringBootApplication` context. If JPA annotations (`@EnableJpaRepositories`, `@EntityScan`) are on the main application class, the slice test will fail because it lacks `DataSource` and `EntityManager` beans.
*   **The Fix**: Move JPA-related annotations to a separate `@Configuration` class (e.g., `JpaConfig.java`). This allows `@WebMvcTest` to ignore JPA infra while still scanning your controller.
*   **Alternative**: Use `excludeAutoConfiguration` in the test annotation, but separating config is cleaner for monorepos.

### 3. Financial Precision in Assertions

*   **The Issue**: `BigDecimal` assertions with `isEqualTo()` fail if the scale is different (e.g., `100.0` vs `100.00`), even if the value is numerically identical.
*   **The Fix**: Always use `isEqualByComparingTo()` for `BigDecimal` comparisons in tests, especially when testing `Money` value objects.

### 4. Validation Regex & Special Characters

*   **The Issue**: Strict regex patterns for transaction descriptions (e.g., `^[a-zA-Z0-9 ]*$`) often block valid banking use cases like reference numbers containing `#` or `()`.
*   **Correction**: Update DTO validation patterns to include common symbols: `^[a-zA-Z0-9 #().]*$`.

## 🗄️ Database Management

### 1. Initialization Order

*   **Postgres Healthchecks**: A healthy Postgres container doesn't mean the databases in `init-db.sql` are ready.
*   **The Fix**: Update healthchecks to check a specific database: `pg_isready -U payu -d payu_account`.

### 2. Partitioning Limitations

*   **Hash Partitioning Defaults**: PostgreSQL (as of v16) does **not** support a `DEFAULT` partition for `HASH` partitioning strategies. Attempting to create one causes a migration failure.
    *   **The Fix**: Do not create a default partition for HASH strategies. Ensure the modulus/remainder coverage is complete (which it naturally is).
*   **Unique Constraints**: A unique constraint on a partitioned table **must include** all partitioning columns. Attempting to create a unique index on just the ID when partitioned by `account_id` will fail.
    *   **The Fix**: Add the partition key to the unique index definition: `CREATE UNIQUE INDEX ... ON table (id, partition_key)`.

### 3. Index Predicates & Immutability

*   **Mutable Functions in Indexes**: You cannot use `CURRENT_DATE`, `NOW()`, or `CURRENT_TIMESTAMP` in a `WHERE` clause of an index (partial index) because these functions are not IMMUTABLE.
    *   **The Fix**: Remove time-based filtering from the index definition or use a mechanism that doesn't rely on dynamic dates.

### 4. Podman Build Caching

*   **Stale Maven Layers**: Podman's layer caching is aggressive. If you update source code but the `mvn package` step is cached, old logic persists.
    *   **The Fix**: Use `podman build --no-cache` when debugging cryptic logic errors.
*   **Context Contamination**: Without a `.dockerignore` file, `COPY . .` copies `target/` directories from the host. If the host has stale compiled classes, they can contaminate the build.
    *   **The Fix**: Create `.dockerignore` excluding `**/target`. Clean host target (`rm -rf backend/*/target`) before critical builds.
*   **Compose Service Naming**: `podman-compose` can sometimes fail to map service names correctly or reuse existing containers.
    *   **Fallback**: Use `podman run` with explicit environment variables (`-e`) for reliable debugging.

### 5. Flyway Development

*   **Checksum Mismatches**: Changing a migration script after it has run locally causes checksum errors.
*   **The Strategy**: In dev/local environment, it is often faster to `DROP DATABASE` and let Flyway recreate it from scratch than to manually patch the `flyway_schema_history` table.

## 🛡️ Security & Configuration

### 1. Spring Bean Instantiation

*   **No-Args Constructor**: Beans instantiated by Spring (especially Filters or Interceptors that might be proxied) **must** have a no-args constructor available, even if they have final fields.
    *   **The Fix**: Remove `final` from fields and provide a protected/public no-args constructor to avoid `BeanInstantiationException`.

### 2. OAuth2 Configuration

*   **Silent Failures**: Missing `JwtDecoder` beans often manifest as `UnsatisfiedDependencyException` deep in the security chain.
    *   **The Fix**: Ensure `issuer-uri` or `jwk-set-uri` is explicitly defined in `application.yml` or a `JwtDecoder` bean is manually supplied.

### 3. Quarkus Startup Validation

*   **Mandatory Properties**: Quarkus performs strict validation on `@ConfigProperty`. If a property is defined but resolved to an empty string (e.g., via `${ENV:}`), it may fail with `NoSuchElementException`.
    *   **The Fix**: Always provide a non-empty fallback in `podman-compose.yml` for mandatory secrets or config keys:

        ```yaml
        WEBHOOK_PARTNER_1_SECRET: ${WEBHOOK_PARTNER_1_SECRET:-dummy_secret}
        ```

## 🏗️ Monorepo Infrastructure

### 1. Shared Library Env Var Mapping

*   **Custom Starters**: When using custom Spring Boot starters (like `cache-starter`), they often use specific property prefixes (e.g., `payu.cache.*`). Standard environment variables like `REDIS_HOST` might not be enough if the starter doesn't map them explicitly.
    *   **The Fix**: Double-check the `@ConfigurationProperties` prefix in the starter and provide matching env vars in `podman-compose.yml`:

        ```yaml
        PAYU_CACHE_REDIS_HOST: redis
        ```

### 2. Selective Maven Builds (Resource Optimization)

*   **The Problem**: Attempting to build the entire monorepo root in every service Dockerfile leads to "Too many open files" and extreme memory usage.
    *   **The Fix**: Use selective builds and project selection:

        ```dockerfile
        RUN mvn package -DskipTests -pl :service-name -am
        ```

### 4. Healthcheck Authentication (401 Unauthorized)

*   **The Problem**: Health endpoints (`/q/health` for Quarkus, `/actuator/health` for Spring Boot) may return `401 Unauthorized` if global security filters are too aggressive.
*   **The Fix (Quarkus)**: Ensure `quarkus.health.security.enabled=false` or explicitly permit the health path in your security configuration/filter.
*   **The Fix (Spring Boot)**: Ensure `management.endpoints.web.exposure.include=health` and that the security filter chain permits `/actuator/**`.
*   **Liveness Probes & Context Paths**:
    *   **Probes missing**: By default, Spring Boot does not expose `/actuator/health/liveness` unless `management.endpoint.health.probes.enabled=true`.
    *   **Context Path**: If `server.servlet.context-path` is set (e.g., `/compliance-service`), the healthcheck URL in `podman-compose.yml` MUST include it: `http://localhost:8087/compliance-service/actuator/health/liveness`.
    *   **401 in Spring Boot**: If `/actuator/health/liveness` returns 401 even if `/actuator/health` is permitted, ensure the `requestMatchers` use wildcards (`/actuator/**`) to cover sub-paths.

### 5. Misconfigured Service Labels (Spring Boot vs Quarkus)

*   **The Problem**: A service built with Spring Boot but configured in `docker-compose.yml` using Quarkus environment variables (e.g., `QUARKUS_DATASOURCE_JDBC_URL`) and healthchecks (`/q/health`) will fail to start or report as unhealthy.
*   **The Fix**: Ensure the configuration matches the framework:
    *   **Spring**: `SPRING_DATASOURCE_URL`, `actuator/health/liveness`.
    *   **Quarkus**: `QUARKUS_DATASOURCE_JDBC_URL`, `q/health`.

### 6. Vault Dev Mode Healthcheck

*   **The Problem**: `vault status` inside a container defaults to HTTPS, causing 401/error when Vault is running in `-dev` mode (HTTP).
*   **The Fix**: Explicitly set `VAULT_ADDR` in the healthcheck command:

    ```yaml
    healthcheck:
      test: ["CMD-SHELL", "VAULT_ADDR=http://127.0.0.1:8200 vault status || exit 1"]
    ```

### 7. Quarkus Uber-JAR Augmentation
*   **The Problem**: Duplicate files in dependencies (e.g., `META-INF/beans.xml` or custom resource files) can cause Quarkus build failures during the `buildUberJar` step.
*   **The Fix**: Exclude problematic duplicates or check for dependency conflicts. In most cases, ensuring the project structure follows standard Maven naming prevents resource collisions.

### 8. ArchUnit DSL Modernization
*   **The Problem**: Older ArchUnit syntax like `.or()` or `.and()` in `ClassesShould` chains may result in `cannot find symbol` errors in newer versions.
*   **The Fix**: Use the more explicit `.orShould()` and `.andShould()` methods to properly continue the rule chain. Use `shouldNot().dependOnClassesThat()` instead of `should().notDependOnClassesThat()`.

### 9. Financial Integrity & Optimistic Locking
*   **The Problem**: Concurrent financial operations (credits/debits) can lead to race conditions without proper locking.
*   **The Fix**: Add a `version` field to core domain entities (like `Account`) and use `@Version` (JPA) or manual checks in domain logic to enforce optimistic locking, as verified by P0 integrity tests.

## 🧪 Systematic Debugging

### 6. Spring Boot 3.4 Security & Public Endpoints (Feb 2026)

*   **The Problem**: Spring Security OAuth2 resource server configuration can intercept requests before permitAll() rules are evaluated, causing 401 errors even on public endpoints like `/actuator/health` and `/api/v1/accounts/register`.
*   **Root Cause**: When using `oauth2ResourceServer().jwt()`, Spring creates a filter chain that validates JWT tokens BEFORE the authorization rules (`permitAll()`) are checked.
*   **The Fix**: Use `WebSecurityCustomizer` bean to completely bypass Spring Security for specific paths:

    ```java
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/actuator/**")
                .requestMatchers("/api/v1/accounts/register")
                .requestMatchers("/api/v1/auth/login");
    }
    ```

*   **Note**: Spring will warn "This is not recommended" but this is necessary when OAuth2 resource server is enabled globally.
*   **Alternative**: Disable OAuth2 for specific paths using `securityMatcher()`.

### 8. PostgreSQL Connection Exhaustion (Feb 2026)

*   **The Problem**: Services fail to start with `FATAL: sorry, too many clients already` or `Connection is not available, request timed out after 30000ms (total=1, active=1, idle=0, waiting=0)`.
*   **Root Cause**:
    *   22 microservices × HikariCP pool (10 connections) = 220+ connections to PostgreSQL
    *   Default Crunchy Postgres `max_connections` = 100 (too low for PayU platform)
    *   pgBouncer default `max_client_conn` may also be limiting
*   **Immediate Fix** (when connections exhausted):
    1.  Scale down non-critical services temporarily:
        ```bash
        oc scale deployment ab-testing-service analytics-service \
            dukcapil-simulator bi-fast-simulator backoffice-service \
            --replicas=0 -n payu-dev
        ```
    2.  Restart failing service (e.g., `partner-service`)
    3.  Scale up services again after startup completes
*   **Fixed (DB-002)**: Increased PostgreSQL `max_connections` from 100 to 300 via Patroni dynamic configuration
*   **Also Tuned**: pgBouncer config with `max_client_conn: 1000`, `default_pool_size: 20`, `pool_mode: transaction`
*   **Crunchy Postgres Configuration**:
    ```yaml
    # infrastructure/openshift/infra/base/crunchy-postgres.yaml
    spec:
      patroni:
        dynamicConfiguration:
          postgresql:
            parameters:
              max_connections: 300
              max_prepared_transactions: 300
              shared_buffers: 256MB
              effective_cache_size: 768MB
      proxy:
        pgBouncer:
          config:
            max_client_conn: 1000
            default_pool_size: 20
            pool_mode: transaction
    ```
*   **Future Tuning**:
    *   Tune HikariCP pool sizes per service (reduce from 10 to 5 for non-critical services)
*   **Monitoring**:
    ```bash
    # Check active connections
    oc exec -it payu-postgres-instance1-dmb4-0 -- psql -U payu -c \
        "SELECT count(*) FROM pg_stat_activity;"

    # Check connection by database
    oc exec -it payu-postgres-instance1-dmb4-0 -- psql -U payu -c \
        "SELECT datname, count(*) FROM pg_stat_activity GROUP BY datname;"
    ```

### 7. Gateway Service URL Configuration (Feb 2026)

*   **The Problem**: Gateway proxying fails with "Connection refused: localhost/127.0.0.1:8081" even though service is running.
*   **Root Cause**: Default service URLs in `application.yaml` use `localhost:PORT` which doesn't resolve in container networks.
*   **The Fix**: Update default URLs to use service names from container network:

    ```yaml
    services:
      account-service:
        url: ${ACCOUNT_SERVICE_URL:http://account-service:8001}  # NOT localhost:8081
    ```

*   **Environment Variable Mismatch**: podman-compose.yml may set different variable names (e.g., `ROUTES_ACCOUNT_URL` vs `ACCOUNT_SERVICE_URL`). Ensure ENV variable names match config property names.

### 8. API Key vs JWT Authorization Layering (Feb 2026)

*   **The Problem**: Requests return "MISSING_API_KEY" even after JWT token is provided.
*   **Root Cause**: Multiple security filters are chained (ApiKeyValidationFilter → AuthorizationFilter). If API key validation is enabled, it blocks requests before JWT validation.
*   **The Fix**: Either:
    1.  Disable API key validation for dev: `gateway.api-keys.enabled=false`
    2.  Add public endpoints to API key bypass paths: `gateway.api-keys.bypass-paths=/api/v1/accounts/register`

## 🎨 Frontend & Design System

### 1. Cultural vs. Professional Aesthetics

*   **Observation**: Attempting to force cultural themes (e.g., "Wayang", "Javanese Philosophy") into a Fintech UI can clash with user expectations for "Premium" and "Trust".
*   **Lesson**: Users prefer standard international banking aesthetics (Clean, White, Sans-serif, Glassmorphism) for financial products. Use cultural elements very subtly or not at all if the goal is "Premium Global Standard".

### 2. Responsive Card Design (The "Golden Ratio" Fix)

*   **The Problem**: Fixed pixel widths (e.g., `w-[350px]`) for Credit Card components break on small mobile screens (iPhone SE) or look tiny on large desktops.
*   **The Fix**: Use `vw` (viewport width) units combined with `aspect-ratio` to maintain the ISO/IEC 7810 ID-1 standard.
    *   **Snippet**: `w-[85vw] max-w-[340px] aspect-[1.586]` ensures the card scales perfectly while maintaining the correct physical ratio. Update text sizes to be relative (`text-[3vw]`) to scale with the card.

### 3. Mobile Layout Stacking

*   **The Problem**: "Zig-zag" or staggered grid layouts that look dynamic on Desktop often break flow on Mobile, leading to overlapping or confusing content.
*   **The Fix**: Switch to `flex-col` to stack elements vertically on mobile. Crucially, add significant vertical padding (`py-16` or `py-20`) to containers to prevent content from being occluded by fixed headers or bottom navigation bars.

## 🐳 Containerization & Environment Setup (Feb 2026 Updates)

### 4. Podman Registry Configuration (Feb 4, 2026)

*   **The Problem**: Podman cannot pull images from Docker Hub, showing errors like "short-name 'postgres:16-alpine' did not resolve to an alias and no unqualified-search registries are defined".
*   **Root Cause**: `/etc/containers/registries.conf` has all registry configurations commented out by default for security reasons.
*   **The Fix**: Add Docker Hub to unqualified search registries:

    ```bash
    sudo bash -c 'echo "unqualified-search-registries = [\"docker.io\"]" >> /etc/containers/registries.conf.d/short-name.conf'
    ```

*   **Validation**: Run `podman pull postgres:16-alpine` to confirm images can now be pulled.

### 5. Maven JAR Build Before Container Image (Feb 4, 2026)


  # Step 2: Build container image
  podman build -f backend/account-service/Dockerfile -t payu_account-service backend/account-service
  ```

* **Note**: This two-step approach is more reliable than trying to run Maven inside the container build, especially for monorepo setups with shared dependencies.

### 6. Local Image Tagging for Podman Compose (Feb 4, 2026)

* **The Problem**: `podman-compose up` fails with "no such file or directory" even though images exist locally.
* **Root Cause**: Images are built as `localhost/payu_service:latest` but `podman-compose` references them as `payu_service:latest` (without the `localhost/` prefix).
* **The Fix**: Tag the local image to match the compose reference:

  ```bash
  podman tag localhost/payu_account-service:latest payu_account-service:latest
  podman compose up -d account-service
  ```

* **Best Practice**: When using `build:` directive in docker-compose.yml, always specify an explicit `image:` tag to avoid naming mismatches between `localhost/` prefixed images and compose references.

## ☕ Java & Spring Boot (Feb 2026 Updates)

### 7. Spring Security Wildcard Matchers for Public Endpoints (Feb 4, 2026)

* **The Problem**: Spring Security OAuth2 resource server configuration returns 401 for public endpoints even with `permitAll()` configuration and `WebSecurityCustomizer`.
* **Root Cause**: Exact path matchers in `securityMatcher()` may not match due to path normalization issues. Using `/api/v1/accounts/register` might not match while `/api/v1/accounts/**` will.
* **The Fix**: Use wildcard matchers for public filter chains with `@Order(1)`:

  ```java
  @Bean
  @Order(1)
  public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
      http
          .securityMatcher("/api/v1/accounts/**", "/api/v1/auth/**")  // Use wildcards!
          .csrf(csrf -> csrf.disable())
          .cors(cors -> cors.configurationSource(corsConfigurationSource()))
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .oauth2ResourceServer(oauth2 -> oauth2.disable());  // Explicitly disable for public endpoints
      return http.build();
  }
  ```

* **Why This Works**: Wildcard matchers ensure all subpaths are covered, and explicitly disabling OAuth2 resource server prevents JWT validation on public endpoints.
* **Note**: The JWT filter chain should have `@Order(2)` and should only match paths that require authentication.

### 8. Quarkus Redis Connection Format (Feb 4, 2026)

* **The Problem**: Quarkus gateway service fails to start with `NullPointerException: Cannot invoke "String.length()" because "ip" is null` when connecting to Redis.
* **Root Cause**: Vert.x Redis client (used by Quarkus) expects a specific URI format. Using `redis:6379` or `redis://redis:6379` can cause parsing issues.
* **The Fix**: Use the `redis://host:port` format in environment variables:

  ```yaml
  # docker-compose.yml
  environment:
    QUARKUS_REDIS_HOSTS: redis://redis:6379  # Include redis:// prefix
  ```

* **Why This Works**: The Vert.x Redis client URI parser expects the `redis://` scheme to properly parse the connection string. Without it, the client attempts to parse the string incorrectly and fails with NPE.

### 9. Gateway Authorization Configuration Mapping (Feb 4, 2026)

* **The Problem**: Quarkus configuration validation fails with "does not map to any root" error for `gateway.authorization.jwt-secret` even though the property is defined in `application.yaml`.
* **Root Cause**: SmallRye Config (used by Quarkus) requires all configuration properties to be mapped to a root interface in `@ConfigMapping` classes. The `AuthorizationFilter` was using `@ConfigProperty` directly, but the config mapping was rejecting unmapped properties.
* **The Fix**: Add the `AuthorizationConfig` interface to `GatewayConfig.java`:

  ```java
  @ConfigMapping(prefix = "gateway")
  public interface GatewayConfig {
      // ... other configs

      @WithName("authorization")
      AuthorizationConfig authorization();

      interface AuthorizationConfig {
          @WithDefault("true")
          boolean enabled();

          @WithName("jwt-secret")
          @WithDefault("dGVzdC1qd3Qtc2VjcmV0...")
          String jwtSecret();
      }
  }
  ```

* **Why This Works**: Adding the interface to the config mapping tells SmallRye Config that these properties are valid and expected, preventing validation failures.

## 🧪 E2E Testing (Feb 2026 Updates)

### 10. Playwright Installation for E2E Tests (Feb 4, 2026)

* **The Problem**: Running `npx playwright test` fails with "Cannot find module '@playwright/test'" even though Playwright is listed in `package.json`.
* **Root Cause**: The `@playwright/test` package needs to be installed locally in the project, and browsers need to be downloaded separately.
* **The Fix**: Install dependencies and browsers before running tests:

  ```bash
  # Install all npm dependencies including @playwright/test
  npm ci

  # Install Playwright browsers with system dependencies
  npx playwright install --with-deps
  ```

* **Note**: The `--with-deps` flag installs system-level dependencies (like libraries for Chromium, Firefox, WebKit) which are required for headless browser operation in Linux environments.

### 11. Standalone Quarkus Service Dockerfile Pattern (Feb 4, 2026)

* **The Problem**: Quarkus services built with `-pl :service-name -am` flag fail with "no such file or directory" when the service is a standalone module (not part of a multi-module parent POM structure).
* **Root Cause**: The `-pl` flag is designed for multi-module Maven projects where you need to specify which module to build. Standalone services should build the current directory without the `-pl` flag.
* **The Fix**: Remove the `-pl :service-name -am` flags and fix COPY paths:

  ```dockerfile
  # WRONG (for standalone services):
  RUN mvn package -DskipTests -Dquarkus.package.jar.type=fast-jar -pl :api-portal-service -am
  COPY --from=build --chown=185 /build/api-portal-service/target/quarkus-app/lib/ /deployments/lib/
  
  # CORRECT:
  RUN mvn package -DskipTests -Dquarkus.package.jar.type=fast-jar
  COPY --from=build --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
  ```

* **Services Affected**: api-portal-service, gateway-service

### 12. Spring Boot Service with Pre-built JAR Pattern (Feb 4, 2026)

* **The Problem**: Multi-module Maven build inside Docker fails when the parent POM is not accessible from the build context (subdirectory build).
* **Root Cause**: Dockerfile with `context: ./backend/service-name` cannot access `../pom.xml` for multi-module builds.
* **The Fix**: Build the JAR locally first, then use a simplified Dockerfile:

  ```bash
  # Step 1: Build JAR locally
  mvn -f backend/service-name/pom.xml clean package -DskipTests
  cp target/service-name-*.jar target/app.jar
  
  # Step 2: Use simplified Dockerfile
  FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2
  COPY target/app.jar /deployments/app.jar
  ```

* **Services Affected**: lending-service (and any service with complex multi-module dependencies)

### 13. PostgreSQL Password in Container Environment (Feb 4, 2026)

* **The Problem**: Spring Boot services fail with "FATAL: password authentication failed for user 'payu'" when connecting to PostgreSQL, even though the correct password is configured.
* **Root Cause**: The running PostgreSQL container was created with a different password than what's configured in `docker-compose.yml`. The environment variable `POSTGRES_PASSWORD` was set when the container was first created, and changing it in `docker-compose.yml` doesn't affect running containers.
* **The Fix**: Either:
  1. Recreate the PostgreSQL container with the correct password, OR
  2. Use the actual password from the running container when connecting services
  3. Check the actual password: `podman inspect payu-postgres | grep POSTGRES_PASSWORD`

* **Lesson**: PostgreSQL password is set at container creation time. Changing `docker-compose.yml` doesn't update running containers.

### 14. Context Path in Healthcheck URLs (Feb 4, 2026)

* **The Problem**: Healthcheck fails with 404 even though the service is running correctly.
* **Root Cause**: Services with `server.servlet.context-path` (like `/compliance-service`) require the context path in healthcheck URLs.
* **The Fix**: Include the context path in healthcheck configuration:

  ```yaml
  # compliance-service has context-path: /compliance-service
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/compliance-service/actuator/health/liveness"]
  
  # partner-service has no context-path (uses root)
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  ```

* **Services Affected**: compliance-service, any service with custom context-path


## 🐳 Container Orchestration & Environment Setup (Feb 2026 - Final)

### 15. Port 8080 Standardization Implementation (Feb 4, 2026)

* **Observation**: Different services were mapped to different host ports (8001, 8002, 8003, etc.) while all services internally listen on port 8080.
* **Implementation**: 
  - All services configured with `EXPOSE 8080` internally
  - Gateway service exposed on host port 8080 (standard API gateway port)
  - Other services mapped to unique host ports for development (8001-8014)
  - In production OpenShift, services use ClusterIP/Route - no port mapping needed
* **Benefit**: Standard internal port simplifies service discovery and configuration
* **Note**: Host port variation is development-only for local testing

### 16. E2E Test Execution in Container Environment (Feb 4, 2026)

* **Challenge**: Running full Playwright E2E suite takes 45-50 minutes in containerized environment
* **Root Cause**: Browser automation, container resource constraints, and parallel test execution
* **Optimization Strategies**:
  1. Use `--workers` flag to control parallel execution
  2. Run specific test suites instead of full suite during development
  3. Use `--project` flag to target specific browsers
  4. Consider using headless mode for faster execution
* **Test Result**: 238 test folders created before termination
* **Recommendation**: For CI/CD, use smoke tests for quick validation and full suite overnight

### 17. Image Tagging for Podman Compose (Feb 4, 2026)

* **The Problem**: `podman-compose` cannot find local images that were built with `localhost/` prefix
* **Root Cause**: Images are built as `localhost/payu_service:latest` but compose references `payu_service:latest`
* **The Fix**: Always tag local images to match compose reference:

  ```bash
  # Build creates localhost/payu_service:latest
  podman build -f service/Dockerfile -t payu_service service
  
  # Tag to match compose reference
  podman tag localhost/payu_service:latest payu_service:latest
  
  # Now compose can find it
  podman compose up -d service
  ```

* **Alternative**: Use explicit `image:` tag in docker-compose.yml to avoid naming conflicts

### 11. Port Collision Management (Feb 5, 2026)
* **The Problem**: Multiple services (Lending, Partner, KYC) were competing for host port 8010, causing container creation to fail silently or with "port already in use" errors during `podman compose up`.
* **The Fix**: Audited all services and aligned them strictly with the `.env` configuration template. Standardized host port mapping to avoid any overlap.
* **The Lesson**: In complex microservice environments, rely on central `.env` templates rather than hardcoded ports in `docker-compose.yml`.

### 12. Quarkus Fast-JAR Dockerfile Pattern (Feb 5, 2026)
* **The Problem**: Simulators and Notification services were failing with "no main manifest attribute" or failing to find `app.jar` because the Dockerfile was trying to run a standard JAR instead of the Quarkus specialized `quarkus-run.jar`.
* **The Fix**: Updated Dockerfiles to use multi-stage builds, copying the entire `target/quarkus-app/` directory and setting the entry point to `-jar /deployments/quarkus-run.jar`.
* **The Lesson**: Quarkus `fast-jar` (default) requires copying the entire `quarkus-app` structure, not just a single JAR.

### 13. Spring Boot OIDC Configuration (Feb 5, 2026)
* **The Problem**: Services like `support-service` and `backoffice-service` failed to start with `JwtDecoder` bean errors (`BeanCreationException`).
* **The Fix**: Explicitly added Keycloak OIDC issuer URLs to the `environment` section in `docker-compose.yml` to resolve JWT validation beans at startup.

### 14. Flyway Migration Synchronization (Feb 5, 2026)
* **The Problem**: `promotion-service` crashed because the `customer_segments` table was missing, even though the entity existed in the code.
* **The Fix**: Created the missing `V3__add_customer_segments.sql` migration script to reconcile the database schema with the JPA domain model.

### 15. Gateway Service Resource Limits (Feb 5, 2026)
* **The Problem**: `gateway-service` (Quarkus) experienced OOM (Exit 137) during high load or complex routing initialization with default 256MB limit.
* **The Fix**: Increased memory limits to 768MB (and 256MB reservation) to provide enough headroom for the Vert.x reactive stack.

### 16. Redis Configuration for Spring Services (Feb 5, 2026)
* **The Problem**: Spring Boot services using `cache-starter` fail to connect to Redis in container environments, showing "Connection refused: localhost:6379" errors even though Redis is running.
* **Root Cause**: Services using `payu.cache.redis.host` property don't automatically map standard `REDIS_HOST` environment variable. The custom cache-starter uses `PAYU_CACHE_REDIS_HOST` prefix.
* **The Fix**: Add `PAYU_CACHE_REDIS_HOST: redis` (or service DNS name) to docker-compose.yml for services using cache-starter:
  ```yaml
  lending-service:
    environment:
      PAYU_CACHE_REDIS_HOST: redis  # Maps to payu.cache.redis.host
  ```
* **Note**: Some services also need `spring.data.redis.host` or `REDIS_HOST` depending on configuration pattern.

### 17. Port Standardization Enforcement (Feb 5, 2026)
* **The Problem**: Services hardcoded to non-standard ports (e.g., `server.port=${PORT:8089}`) break standardization and cause healthcheck failures.
* **Root Cause**: Historical port assignments weren't cleaned up when standardizing to port 8080 across all services.
* **The Fix**: Audit `application.yml` for all services and ensure:
  ```yaml
  server:
    port: ${PORT:8080}  # ALL services must default to 8080
  ```
* **Impact**: Non-standard ports cause gateway routing failures and healthcheck mismatches.

### 18. Healthcheck Path Alignment (Feb 5, 2026)
* **The Problem**: Healthchecks in docker-compose.yml pointing to wrong ports (8089, 8090) fail even when services are healthy.
* **Root Cause**: Healthcheck URLs weren't updated when port standardization changed service ports.
* **The Fix**: After fixing source port in `application.yml`, update all healthcheck URLs to match:
  ```yaml
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health/liveness"]
  ```
* **Best Practice**: Healthcheck should always check `localhost:8080` for internal container port, regardless of external host port mapping.

### 19. Quarkus Parent POM Build Context (Feb 5, 2026)
* **The Problem**: Quarkus simulators in monorepo fail with "Parent POM not found" when building from service subdirectory context.
* **Root Cause**: Docker build context is service directory, but parent POM is at backend root.
* **The Fix**: Use backend root as build context and update COPY paths:
  ```dockerfile
  # Build from backend root to access parent POM
  COPY backend/pom.xml .
  COPY backend/simulators/qris-simulator/pom.xml simulators/qris-simulator/
  RUN mvn package -f simulators/qris-simulator/pom.xml -DskipTests
  ```
* **Alternative**: Pre-build JAR locally and use simplified Dockerfile (see Lesson 9).

## 🎨 Product Design Protocol (Feb 2026)

### 20. Jackson Deserialization with Keycloak Responses (Feb 5, 2026)
* **The Problem**: Auth-service login fails with `IllegalArgumentException` when deserializing Keycloak token response.
* **Root Cause**: Keycloak returns extra fields (`not-before-policy`, `refresh_expires_in`, `session_state`, `scope`) that aren't mapped in the `LoginResponse` record. Jackson fails on unknown properties in records by default.
* **The Fix**: Add `@JsonIgnoreProperties(ignoreUnknown = true)` to DTOs that deserialize external OAuth2/OIDC responses:
  ```java
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record LoginResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("refresh_token") String refreshToken,
      @JsonProperty("expires_in") long expiresIn,
      @JsonProperty("token_type") String tokenType
  ) {}
  ```
* **Note**: This is especially important for DTOs that map to third-party API responses (Keycloak, OAuth2 providers) where you don't control the response schema.

### 21. Environment Variable Naming Consistency (Feb 5, 2026)
* **The Problem**: Auth-service login fails with `IllegalArgumentException: Not enough variable values available to expand 'KEYCLOAK_URL'`.
* **Root Cause**: `application.yaml` referenced `${KEYCLOAK_URL}` but `docker-compose.yml` set `KEYCLOAK_SERVER_URL`. The WebClient received the literal string `${KEYCLOAK_URL}` and tried to expand it as a URI template.
* **The Fix**: Align environment variable names between `application.yaml` and `docker-compose.yml`:
  ```yaml
  # application.yaml
  keycloak:
    server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
  ```
* **Best Practice**: Use consistent variable naming across all configuration files. Prefer specific names like `KEYCLOAK_SERVER_URL` over generic `KEYCLOAK_URL`.

### 22. The "Startup Protocol" for Design (Feb 2026)
* **The Shift**: Moving from ad-hoc design improvements to a strict "Steve Jobs" persona protocol.
* **The Rule**: No design opinions are valid without first auditing: 1) Existing Design System, 2) PRD, and 3) Live App Responsiveness.
* **The Impact**: Prevents "design drift" where new features don't match the established "Premium Emerald" aesthetic.
* **Key Check**: "If an element can be removed without losing meaning, it must be removed."

### 23. Keycloak Admin Password Persistence (Feb 6, 2026)
* **The Problem**: Changing `KEYCLOAK_ADMIN_PASSWORD` in `docker-compose.yml` doesn't update existing Keycloak admin password.
* **Root Cause**: Keycloak stores admin credentials in PostgreSQL database. The `KEYCLOAK_ADMIN_PASSWORD` environment variable only sets the initial password on first startup. Once the database exists, changing the env var has no effect.
* **The Fix**: To reset admin password on existing installation:
  1. Access Keycloak Admin Console at http://localhost:8099
  2. Navigate to Users → admin → Credentials → Set password
  3. OR use kc.sh CLI: `podman exec payu-keycloak /opt/keycloak/bin/kc.sh import users` (requires restart)
* **Best Practice**: Document admin passwords securely and consider using external secret management (Vault, Sealed Secrets) for production.
* **Note**: For fresh installations, set `KEYCLOAK_ADMIN_PASSWORD` in docker-compose.yml before first startup.

### 24. Redis Environment Variables for Spring Boot Services (Feb 6, 2026)
* **The Problem**: Spring Boot services showing `DOWN` status in health checks despite all containers running healthy.
* **Symptoms**:
  - Health endpoint returns: `{"status":"DOWN","components":{"redis":{"status":"DOWN","details":{"error":"RedisConnectionFailureException"}}}}`
  - DeepHealthIndicator logs: "Redis health check failed: Unable to connect to Redis"
  - Redis container is healthy and responding to PING
* **Root Cause**: Services were missing `REDIS_HOST` and `PAYU_CACHE_REDIS_HOST` environment variables in `docker-compose.yml`. Without these, Spring Data Redis defaults to `localhost:6379` instead of the container network hostname `redis:6379`.
* **The Fix**: Add missing Redis environment variables to docker-compose.yml:
  ```yaml
  environment:
    REDIS_HOST: redis
    REDIS_PORT: 6379
    PAYU_CACHE_REDIS_HOST: redis
  ```
* **Verification**: After restarting services:
  ```bash
  curl -s http://localhost:8001/actuator/health/deepHealth | jq '.details.redis'
  # Returns: {"latency": "1ms", "response": "PONG"}
  ```
* **Best Practice**: Always explicitly define Redis connection parameters in container environments, even if application.yml has defaults. The `localhost` default only works for local development, not container networking.

### 25. Reset Keycloak Passwords Directly in Database (Feb 6, 2026)
* **The Problem**: Need to reset Keycloak admin or user passwords but don't have access to the Admin Console or current password is unknown.
* **Root Cause**: Keycloak stores passwords as PBKDF2-SHA256 hashes in the `credential` table. The `KEYCLOAK_ADMIN_PASSWORD` env var only works on first startup.
* **The Solution**: Generate PBKDF2-SHA256 hash and update database directly.
* **Python Script to Generate Hash**:
  ```python
  import hashlib
  import binascii
  import json

  password = "P@ssw0rd123"
  salt = "payusaltkey2024".encode('utf-8')
  iterations = 27500

  hashed = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
  hash_b64 = binascii.b2a_base64(hashed).decode('utf-8').strip()
  salt_b64 = binascii.b2a_base64(salt).decode('utf-8').strip()

  secret_data = json.dumps({"value": hash_b64, "salt": salt_b64, "additionalParameters": {}})
  credential_data = json.dumps({"hashIterations": iterations, "algorithm": "pbkdf2-sha256", "additionalParameters": {}})
  ```
* **SQL Update Command**:
  ```sql
  UPDATE credential
  SET SECRET_DATA = '<secret_data_json>',
      CREDENTIAL_DATA = '<credential_data_json>',
      TYPE = 'password'
  WHERE user_id = (SELECT id FROM user_entity WHERE username = 'admin');
  ```
* **Important**: After updating the database, restart Keycloak container to apply changes.
* **Best Practice**: Store the generated passwords securely and document the salt used for future reference.

### 26. OpenAPI Documentation Coverage Gap (Feb 6, 2026)
* **The Problem**: API documentation at `/api-docs` was incomplete, with only 15.6% of endpoints having `@Operation` annotations.
* **Root Cause**: Developers implemented REST endpoints without adding OpenAPI annotations, causing a gap between implemented and documented APIs.
* **Discovery Method**: Created `scripts/validate-openapi.py` to scan all controllers and compare `@RequestMapping` derivatives with `@Operation` annotations.
* **Findings**:
  - Total endpoints: 154 across 13 services
  - Documented: 24 (15.6%)
  - Undocumented: 130 (84.4%)
  - Services with 0% documentation: auth-service, fx-service, partner-service, account-service
  - Only billing-service had 100% coverage
* **The Fix**: Add `@Operation` annotations to all undocumented endpoints:
  ```java
  @Operation(
      summary = "Transfer funds between accounts",
      description = "Executes a transfer from source to destination account with idempotency support",
      tags = {"Transactions"},
      responses = {
          @ApiResponse(responseCode = "200", description = "Transfer successful"),
          @ApiResponse(responseCode = "400", description = "Invalid request"),
          @ApiResponse(responseCode = "409", description = "Insufficient funds")
      }
  )
  @PostMapping("/transfer")
  public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
      // ...
  }
  ```
* **Validation Script Usage**:
  ```bash
  # Run full validation
  ./scripts/validate-openapi.py

  # Validate single service
  ./scripts/validate-openapi.py --service transaction-service

  # Generate JSON report for CI/CD
  ./scripts/validate-openapi.py --json
  ```
* **CI/CD Integration**: Add validation to build pipeline to enforce documentation coverage threshold (e.g., minimum 80%).
* **Best Practice**: Require `@Operation` annotation in code review checklist for all new REST endpoints.
* **Achievement**: After fixing detection script and adding missing annotations, reached 100% coverage (154/154 endpoints).

### 27. Java Annotation Order Matters for OpenAPI Detection (Feb 6, 2026)
* **The Problem**: Validation script initially reported 15.6% coverage, but actual coverage was much higher.
* **Root Cause**: Java annotation order can vary between codebases. Two patterns exist:
  1. **Standard**: `@Operation` before `@GetMapping` (operation annotation first)
  2. **Reverse**: `@GetMapping` before `@Operation` (mapping annotation first)
* **The Fix**: Updated validation script to check both patterns - look back 15 lines AND look forward 20 lines for `@Operation` annotation.
* **Detection Logic**:
  ```python
  # Pattern 1: Look back (standard annotation order)
  for i in range(max(0, start_idx - 15), start_idx):
      if '@Operation' in lines[i]:
          return True, summary, tags

  # Pattern 2: Look forward (reverse annotation order)
  for i in range(start_idx, min(len(lines), start_idx + 20)):
      if '@Operation' in lines[i]:
          return True, summary, tags

  # Stop at method declaration
  if line.strip().startswith('public'):
      break
  ```
* **Result**: After fixing detection, coverage jumped from 15.6% to 96.1%. Only needed to add annotations to `ScheduledTransferController` (6 endpoints) to reach 100%.
* **Best Practice**: When writing validation scripts, account for different code style patterns within the same language. Always verify false positives by manual inspection.

### 28. Seed Data Alignment for Development (Feb 6, 2026)
* **The Problem**: Tests and documentation referenced test users (customer1, admin) but no automated seed data existed to create these users.
* **Root Cause**:
  - Keycloak realm was created manually, not via export/import
  - Database migrations only created schemas, not seed data
  - Tests used Faker to generate random users instead of deterministic fixtures
* **The Fix**: Created comprehensive seed data infrastructure:
  1. **Keycloak Realm Export** (`infrastructure/keycloak/payu-realm-export.json`):
     - Defines realm: `payu`
     - Roles: USER, ADMIN, BACKOFFICE, KYC_VERIFIED, PREMIUM
     - Users: customer1, customer2, admin, backoffice (all with password `P@ssw0rd123`)
     - Clients: payu-web-app, payu-backend, payu-mobile
  2. **Database Seed Migrations** (`V99__seed_test_data.sql`):
     - account-service: Users, profiles, accounts with initial balances
     - wallet-service: Wallets and ledger entries for test accounts
  3. **Seed Data Script** (`scripts/seed-data.sh`):
     - Initializes Keycloak realm via admin API
     - Runs database seed migrations
     - Verifies seed data was created
  4. **Idempotency Validation Test**:
     - Tests idempotency key reuse detection
     - Tests in-progress request detection
     - Tests fingerprint consistency
* **Test Credentials**:
  ```bash
  # Customer 1 (KYC verified, premium user)
  Username: customer1
  Password: P@ssw0rd123
  Email: customer1@payu.id
  NIK: 3201234567890001
  Initial Balance: Rp 10,000,000

  # Customer 2 (Basic user)
  Username: customer2
  Password: P@ssw0rd123

  # Admin
  Username: admin
  Password: P@ssw0rd123
  ```
* **Usage**:
  ```bash
  # Initialize all seed data
  ./scripts/seed-data.sh

  # Initialize only Keycloak
  ./scripts/seed-data.sh --keycloak

  # Initialize only database
  ./scripts/seed-data.sh --db

  # Verify seed data
  ./scripts/seed-data.sh --verify
  ```
* **Best Practice**: Store seed data in version control alongside migrations. Use V99 or similar high version number to ensure seed data runs after all schema migrations.

### 29. Flyway Seed Data Idempotency (Feb 6, 2026)
* **The Problem**: `V99__seed_test_data.sql` often fails on subsequent restarts in development environments due to unique constraint violations (e.g., duplicate usernames or IDs).
* **The Cause**: Even with `ON CONFLICT DO NOTHING`, standard seed data can fail if multiple unique constraints exist (e.g., both ID and Username) and only one is targeted.
* **The Fix**: Use a `DELETE` approach for deterministic seed data in development:
  ```sql
  DELETE FROM profiles WHERE user_id IN (SELECT id FROM users WHERE username IN ('customer1', 'admin'));
  DELETE FROM users WHERE username IN ('customer1', 'admin');
  -- Then run standard inserts
  ```
* **Lesson**: Deterministic `DELETE` then `INSERT` is more robust for dev-mode seed scripts than complex `ON CONFLICT` logic when multiple unique keys are involved.

### 30. Sequential Startup for Persistent Message Brokers (Feb 6, 2026)
* **The Problem**: Kafka fails to start with `KeeperErrorCode = NodeExists` if Zookeeper state is inconsistent or if both are restarted simultaneously after an abrupt shutdown.
* **The Symptom**: Kafka container exits with error during registration.
* **The Fix**: Perform a sequential reset:
  1. `podman stop kafka zookeeper`
  2. `podman start zookeeper`
  3. Wait for Zookeeper health (approx 5-10s)
  4. `podman start kafka`
* **Lesson**: Infrastructure dependencies in containerized environments sometimes require manual sequence synchronization during recovery from hard crashes.

### 31. Dockerfile COPY Pattern for Multi-Version JARs (Feb 6, 2026)
* **The Problem**: Maven builds create versioned JARs (e.g., `service-1.0.0-SNAPSHOT.jar`) but Dockerfiles expect consistent filenames like `target/app.jar`.
* **The Symptom**: Docker build fails with "COPY target/app.jar: no such file or directory" even though Maven build succeeded.
* **The Fix**: Use wildcard pattern in Dockerfile COPY instruction:
  ```dockerfile
  # Before (fails with versioned JARs):
  COPY target/app.jar /deployments/app.jar

  # After (works with any version):
  COPY target/*.jar /deployments/app.jar
  ```
* **Alternative**: Rename JAR after Maven build:
  ```bash
  cp target/service-1.0.0-SNAPSHOT.jar target/app.jar
  ```
* **Services Affected**: lending-service, ab-testing-service

### 32. Pre-Built JAR Pattern for Resource-Constrained Builds (Feb 6, 2026)
* **The Problem**: Building Maven projects inside Docker containers fails with "Too many open files" or extreme memory usage in resource-constrained environments.
* **The Symptom**: `mvn package` inside Dockerfile fails during dependency resolution or compilation with system errors.
* **The Fix**: Use "Decoupled Build" strategy:
  1. **Build JAR on Host**: `mvn -f backend/service/pom.xml clean package -DskipTests`
  2. **Use Simplified Dockerfile** that only copies the pre-built JAR
  3. **Temporarily Modify .dockerignore** to allow `target/` directory during build
* **Why This Works**: Avoids running Maven inside container, eliminates "Too many open files" errors, and leverages host Maven cache.
* **Services Affected**: notification-service (Quarkus), api-portal-service (Quarkus), lending-service (Spring Boot)

### 33. Quarkus Fast-JAR Directory Structure (Feb 6, 2026)
* **The Problem**: Quarkus services fail to start with ClassNotFoundException or "no main manifest attribute" when using incorrect COPY paths in Dockerfile.
* **Root Cause**: Quarkus `fast-jar` (default) creates a directory structure in `target/quarkus-app/`, not a single JAR file.
* **The Fix**: Copy the entire quarkus-app directory structure:
  ```dockerfile
  COPY target/quarkus-app/lib/ /deployments/lib/
  COPY target/quarkus-app/*.jar /deployments/
  COPY target/quarkus-app/app/ /deployments/app/
  ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/quarkus-run.jar"]
  ```
* **Wrong Pattern**: `COPY target/*.jar /deployments/app.jar` (only works for Spring Boot uber-jars)
* **Services Affected**: notification-service, api-portal-service, gateway-service

### 34. Port Conflict Detection in Docker Compose (Feb 6, 2026)
* **The Problem**: Multiple services competing for the same host port causes container startup failures.
* **The Symptom**: `podman-compose up` fails with "port already in use" or service unreachable.
* **The Fix**:
  - Audit all ports: `grep -E "^\s+- \"\d+:" docker-compose.yml | sort`
  - Check for lingering processes: `sudo lsof -i :8019`
  - Kill conflicting processes: `sudo kill -9 <PID>`
* **Best Practice**: Use central `.env` template for all service ports.
* **Services Affected**: lending-service, ab-testing-service (both initially tried to use 8019)

### 35. Redis Environment Variables for Spring Boot Services (Feb 6, 2026)
* **The Problem**: Spring Boot services using `cache-starter` fail health checks with Redis DOWN status even though Redis container is running.
* **Root Cause**: Missing `REDIS_HOST`, `REDIS_PORT`, and `PAYU_CACHE_REDIS_HOST` environment variables in docker-compose.yml. Services default to `localhost:6379` which doesn't work in container networks.
* **The Fix**: Explicitly add Redis connection env vars to docker-compose.yml:
  ```yaml
  billing-service:
    environment:
      REDIS_HOST: redis
      REDIS_PORT: 6379
      PAYU_CACHE_REDIS_HOST: redis
  ```
* **Verification**: After restart, health endpoint shows `"redis": { "status": "UP" }`
* **Services Affected**: billing-service, support-service, statement-service

### 36. Container Not Running - Missing Deployment (Feb 6, 2026)
* **The Problem**: Service shows "Connection refused" and container doesn't appear in `podman ps`. JAR file exists but container was never built/started.
* **Root Cause**: Service was configured in docker-compose.yml but container was never deployed (possibly skipped during initial startup).
* **The Fix**:
  1. Verify JAR exists: `ls backend/service/target/*.jar`
  2. Build and start: `podman-compose up -d service-name`
  3. Verify: `podman ps | grep service-name`
* **Lesson**: Always verify containers are actually running, not just configured.
* **Services Affected**: fx-service

### 37. Double Context Path in Spring Controllers (Feb 6, 2026)
* **The Problem**: All API endpoints return 404 even though service is running and healthy.
* **Root Cause**: Double context path prefix - `application.yml` sets `server.servlet.context-path: /fx-api` AND controller has `@RequestMapping("/fx-api/v1")`, resulting in `/fx-api/fx-api/v1` paths.
* **The Fix**: Remove duplicate context path from controller:
  ```java
  // Before (wrong):
  @RequestMapping("/fx-api/v1")
  public class FxController { }

  // After (correct):
  @RequestMapping("/v1")
  public class FxController { }
  ```
* **Verification**: Endpoints accessible at `/fx-api/v1/*` (single prefix)
* **Services Affected**: fx-service

---

## 🎭 Frontend Testing & E2E

### 1. Playwright E2E Authentication Fixtures (Feb 6, 2026)

* **The Problem**: All E2E tests failing with "element not found" errors. Tests expected "PayU" page title but got "Grafana" or were redirected to `/login`.
* **Root Cause**: `middleware.ts` requires authentication cookies (`accessToken`, `payu_session`) for protected routes (`/investments`, `/dashboard`, etc.). Tests navigated directly to protected pages without setting session cookies, causing redirects to login.
* **The Fix**: Create extended test fixtures with automatic authentication:
  ```typescript
  // e2e/fixtures/index.ts
  import { test as base, expect } from '@playwright/test';

  export const test = base.extend({
    authPage: async ({ page, context }, use) => {
      // Set mock session cookies before navigation
      await context.addCookies([
        {
          name: 'accessToken',
          value: 'mock-token-for-e2e',
          domain: 'localhost',
          path: '/',
          httpOnly: true,
          secure: false,
          sameSite: 'Lax',
        },
        {
          name: 'payu_session',
          value: 'mock-session-for-e2e',
          domain: 'localhost',
          path: '/',
          httpOnly: true,
          secure: false,
          sameSite: 'Lax',
        },
      ]);
      await use(page);
    },
  });
  export { expect };
  ```
* **Usage in Tests**:
  ```typescript
  import { test, expect } from './fixtures';

  test.describe('Investment Flow', () => {
    test.beforeEach(async ({ authPage: page }) => {
      await page.goto('/investments');
      await page.waitForLoadState('networkidle');
    });
    // Tests now have authenticated access!
  });
  ```
* **Key Insight**: When testing protected routes in apps with authentication middleware, always provide a way to bypass or mock authentication in E2E tests. Don't test the auth flow on every test - use fixtures for efficiency.
* **Files Affected**: All 12 E2E test files in `frontend/web-app/e2e/`

### 2. Playwright Port Configuration for Containerized Apps (Feb 6, 2026)

* **The Problem**: Playwright tests configured for `localhost:3000` but containerized web-app runs on `localhost:3001`. Tests fail with connection refused or wrong page content.
* **Root Cause**: `playwright.config.ts` had `webServer` command starting its own dev server on port 3000, conflicting with the already-running containerized app on port 3001.
* **The Fix**: Update `playwright.config.ts` to use containerized app:
  ```typescript
  export default defineConfig({
    use: {
      // Use containerized app port
      baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3001',
      locale: 'id', // Set default locale for i18n
    },
    // Disable webServer - use existing containerized app
    // webServer: {
    //   command: 'npm run dev',
    //   url: 'http://localhost:3000',
    // },
  });
  ```
* **Best Practice**: For containerized environments, disable Playwright's `webServer` and point `baseURL` to the running container. Use environment variables for flexibility.
* **Files Affected**: `frontend/web-app/playwright.config.ts`

### 3. E2E Test Stability with Network Idle (Feb 6, 2026)

* **The Problem**: Tests intermittently fail because assertions run before page is fully loaded (especially with client-side hydration).
* **Root Cause**: `page.goto()` returns when the initial HTML is loaded, but React/Next.js hydration and API calls may still be in progress.
* **The Fix**: Always wait for network idle after navigation:
  ```typescript
  test.beforeEach(async ({ authPage: page }) => {
    await page.goto('/investments');
    await page.waitForLoadState('networkidle'); // Wait for all network activity to settle
  });
  ```
* **Alternative Strategies**:
  - Wait for specific elements: `await page.waitForSelector('[data-testid="portfolio-value"]')`
  - Wait for API responses: `await page.waitForResponse('**/api/portfolio')`
* **Files Affected**: All E2E test files

### 4. Accessibility Testing with Axe - Color Contrast (Feb 6, 2026)

* **The Problem**: Axe accessibility scans failing with `color-contrast` violations on login and onboarding pages.
* **Root Cause**: UI design using subtle text colors that don't meet WCAG 2.1 AA contrast ratios (4.5:1 for normal text).
* **The Fix**: Update design tokens or accept known violations with justification:
  ```typescript
  const results = await axeBuilder()
    .options({
      rules: {
        'color-contrast': { enabled: false }, // Only if brand colors are non-negotiable
      },
    })
    .analyze();
  ```
* **Better Fix**: Update Tailwind config to ensure accessible color combinations:
  ```css
  /* Ensure minimum contrast ratios */
  .text-muted-foreground {
    color: hsl(var(--muted-foreground));
    /* Must have 4.5:1 contrast against background */
  }
  ```
* **Lesson**: Accessibility should be considered from design phase, not as an afterthought in testing.
* **Files Affected**: `frontend/web-app/e2e/a11y-audit.spec.ts`

---

## 🔒 Security Architecture Patterns (Feb 2026 Audit)

### 1. JWT Token Storage: localStorage vs httpOnly Cookies

* **The Problem**: `frontend/web-app/src/lib/api.ts` stores JWT tokens in `localStorage` (lines 15, 61) while `stores/authStore.ts` documentation explicitly states tokens must ONLY be in httpOnly cookies. The implementation contradicts its own security architecture.
* **Why localStorage is Dangerous**:
    * Any XSS attack can read `localStorage.getItem('token')` — stealing all user sessions
    * PCI-DSS Section 6.5.7 explicitly prohibits storing sensitive auth tokens in client-accessible storage
    * Unlike httpOnly cookies, JavaScript has full read/write access to localStorage
* **The Correct Pattern — BFF (Backend-for-Frontend)**:
    ```
    Browser → Next.js API Route (BFF) → Backend Service
                    ↕
           httpOnly cookie (token)
    ```
    1. Login request goes to Next.js API route (`/api/auth/login`)
    2. API route calls auth-service, receives JWT tokens
    3. API route sets httpOnly, Secure, SameSite=Strict cookie
    4. Browser NEVER sees the raw JWT token
    5. All subsequent API calls go through Next.js API routes which attach the token

* **Implementation Steps**:
    ```typescript
    // src/app/api/auth/login/route.ts (Next.js BFF)
    import { cookies } from 'next/headers';

    export async function POST(request: Request) {
      const body = await request.json();
      const authResponse = await fetch(`${process.env.AUTH_SERVICE_URL}/api/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const data = await authResponse.json();

      if (authResponse.ok) {
        const cookieStore = await cookies();
        cookieStore.set('accessToken', data.accessToken, {
          httpOnly: true,       // JavaScript cannot read this
          secure: process.env.NODE_ENV === 'production',
          sameSite: 'strict',   // CSRF protection
          maxAge: 900,          // 15 minutes
          path: '/',
        });
        cookieStore.set('refreshToken', data.refreshToken, {
          httpOnly: true,
          secure: process.env.NODE_ENV === 'production',
          sameSite: 'strict',
          maxAge: 604800,       // 7 days
          path: '/api/auth',    // Only sent to auth endpoints
        });
        return Response.json({ success: true });
      }
      return Response.json(data, { status: authResponse.status });
    }
    ```

    ```typescript
    // src/app/api/proxy/[...path]/route.ts (API Proxy)
    import { cookies } from 'next/headers';

    export async function GET(request: Request, { params }: { params: { path: string[] } }) {
      const cookieStore = await cookies();
      const token = cookieStore.get('accessToken')?.value;
      if (!token) return Response.json({ error: 'Unauthorized' }, { status: 401 });

      const backendUrl = `${process.env.GATEWAY_URL}/${params.path.join('/')}`;
      const response = await fetch(backendUrl, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });
      return Response.json(await response.json(), { status: response.status });
    }
    ```

    ```typescript
    // src/lib/api.ts (CORRECTED — No more localStorage)
    class ApiClient {
      private baseUrl = '/api/proxy'; // All calls go through Next.js BFF

      async get<T>(path: string): Promise<T> {
        const res = await fetch(`${this.baseUrl}${path}`, { credentials: 'include' });
        if (res.status === 401) {
          // Try refresh via BFF
          const refreshed = await fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' });
          if (refreshed.ok) return this.get<T>(path); // Retry
          window.location.href = '/login';
          throw new Error('Session expired');
        }
        return res.json();
      }
    }
    ```

* **Migration Checklist**:
    - [ ] Create `/api/auth/login`, `/api/auth/logout`, `/api/auth/refresh` API routes
    - [ ] Create `/api/proxy/[...path]` catch-all API route
    - [ ] Remove ALL `localStorage.getItem('token')` / `localStorage.setItem('token')` calls
    - [ ] Update `src/lib/api.ts` to use BFF proxy
    - [ ] Update `src/stores/authStore.ts` to remove token state
    - [ ] Update `middleware.ts` to read from cookies (already partially done)
    - [ ] Update E2E test fixtures to set httpOnly cookies

### 2. Encryption Key Derivation: SHA-256 vs PBKDF2

* **The Problem**: `security-starter`'s `EncryptionService` uses `MessageDigest.getInstance("SHA-256")` for key derivation. SHA-256 is a hash, not a KDF — it provides no protection against brute-force/dictionary attacks on weak keys.
* **Why This Matters**: A single SHA-256 computation takes microseconds. An attacker with the encrypted data can try billions of key guesses per second.
* **The Correct Pattern**:
    ```java
    // BEFORE (Weak — single SHA-256 hash)
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] keyBytes = digest.digest(masterKey.getBytes(StandardCharsets.UTF_8));

    // AFTER (Strong — PBKDF2 with 600,000 iterations)
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    byte[] salt = getSaltFromVault(); // Must be unique per deployment
    KeySpec spec = new PBEKeySpec(masterKey.toCharArray(), salt, 600_000, 256);
    SecretKey key = factory.generateSecret(spec);
    byte[] keyBytes = key.getEncoded();
    ```
* **Key Rotation Pattern**:
    ```java
    public class RotatingEncryptionService {
        private final Map<String, SecretKey> keyVersions;
        private final String currentVersion;

        public String encrypt(String plaintext) {
            byte[] encrypted = doEncrypt(plaintext, keyVersions.get(currentVersion));
            return currentVersion + ":" + Base64.encode(encrypted); // Prefix with version
        }

        public String decrypt(String ciphertext) {
            String[] parts = ciphertext.split(":", 2);
            String version = parts[0];
            SecretKey key = keyVersions.get(version);
            return doDecrypt(Base64.decode(parts[1]), key);
        }
    }
    ```
* **Best Practice**: Store encryption keys in HashiCorp Vault Transit backend, not in environment variables.

### 3. Shared Starter Integration Checklist

* **The Problem**: 4 Spring Boot services don't use shared starters (cms-service: 0/4, ab-testing-service: 1/4, investment-service: 2/4, statement-service: 1/4). This creates security and resilience blind spots.
* **Correct Integration Pattern**:
    ```xml
    <!-- service pom.xml — Add ALL 4 starters -->
    <dependencies>
        <!-- Security: JWT auth, PII encryption, audit logging -->
        <dependency>
            <groupId>id.payu</groupId>
            <artifactId>security-starter</artifactId>
        </dependency>
        <!-- Resilience: Circuit breaker, retry, bulkhead, rate limit -->
        <dependency>
            <groupId>id.payu</groupId>
            <artifactId>resilience-starter</artifactId>
        </dependency>
        <!-- Cache: Multi-layer L1 Caffeine + L2 Redis -->
        <dependency>
            <groupId>id.payu</groupId>
            <artifactId>cache-starter</artifactId>
        </dependency>
        <!-- Commons: ApiResponse, Money VO, GlobalExceptionHandler -->
        <dependency>
            <groupId>id.payu</groupId>
            <artifactId>api-commons</artifactId>
        </dependency>
    </dependencies>
    ```
* **Verification After Adding**:
    1. Service starts without `BeanCreationException` → Check auto-configuration
    2. `GET /actuator/health` shows `security`, `resilience`, `cache` components
    3. Unauth request to protected endpoint → 401 (security-starter working)
    4. Hit rate limit → 429 (resilience-starter working)
    5. Second identical request faster → Cache hit (cache-starter working)

### 4. Transactional Outbox Pattern Integration

* **The Problem**: `outbox-starter` is fully built (90% quality) but 0 services use it. Financial transactions publish Kafka events without transactional guarantees — if Kafka is down during a transaction, the event is lost silently.
* **Why This Is Critical for Banking**: A transfer that succeeds in the database but fails to publish a Kafka event means the wallet balance updates but the notification/audit trail is lost.
* **Correct Integration**:
    ```java
    // transaction-service: TransferService.java
    @Service
    @RequiredArgsConstructor
    public class TransferService {
        private final TransactionRepository transactionRepository;
        private final OutboxService outboxService; // From outbox-starter

        @Transactional // CRITICAL: Same transaction for DB write + outbox event
        public TransferResult execute(TransferCommand command) {
            Transaction tx = Transaction.create(command);
            transactionRepository.save(tx);

            // Event saved in same DB transaction — guaranteed delivery
            outboxService.createEvent(
                "transaction",                        // aggregate type
                tx.getId().toString(),                // aggregate ID
                "TransferCompleted",                  // event type
                "payu.transactions.completed",        // Kafka topic
                Map.of(                               // payload
                    "transactionId", tx.getId(),
                    "amount", tx.getAmount(),
                    "sourceAccount", tx.getSourceAccountId(),
                    "destAccount", tx.getDestAccountId()
                )
            );

            return TransferResult.success(tx);
        }
    }
    ```
    The `OutboxPublisher` (scheduled task from outbox-starter) polls `outbox_events` table and publishes to Kafka with retry. If Kafka is down, events accumulate in the table and are published when Kafka recovers — **zero event loss**.

* **Services That MUST Use Outbox**:
    - `transaction-service` — transfers, payments, QRIS
    - `wallet-service` — balance updates, ledger entries
    - `lending-service` — loan disbursements, repayments
    - `billing-service` — bill payments

### 5. Saga Orchestration Pattern Integration

* **The Problem**: `saga-starter` is fully built (85% quality) with orchestrator, compensation, recovery, and monitoring — but 0 services use it. `transaction-service` has handcrafted saga logic that doesn't leverage the shared implementation.
* **Correct Integration**:
    ```java
    // transaction-service: TransferSaga.java
    @Component
    @RequiredArgsConstructor
    public class TransferSaga {
        private final SagaOrchestrator orchestrator;

        public TransferResult execute(TransferCommand cmd) {
            SagaDefinition<TransferContext> saga = SagaDefinition.<TransferContext>builder()
                .step("validate")
                    .action(ctx -> validateAccounts(ctx))
                    .compensation(ctx -> {}) // No-op, validation is read-only
                .step("debit-source")
                    .action(ctx -> debitSourceWallet(ctx))
                    .compensation(ctx -> creditSourceWallet(ctx)) // Reverse debit
                .step("credit-destination")
                    .action(ctx -> creditDestinationWallet(ctx))
                    .compensation(ctx -> debitDestinationWallet(ctx)) // Reverse credit
                .step("notify")
                    .action(ctx -> sendNotification(ctx))
                    .compensation(ctx -> {}) // Non-critical, no compensation needed
                    .continueOnFailure(true) // Don't fail saga if notification fails
                .build();

            return orchestrator.execute(saga, new TransferContext(cmd));
            // If "credit-destination" fails, orchestrator automatically:
            // 1. Calls creditSourceWallet() to reverse the debit
            // 2. Logs the saga as COMPENSATED
            // 3. Returns failure result
        }
    }
    ```

### 6. Quarkus Services: Security Without Shared Starters

* **The Problem**: 3 Quarkus services (notification, gateway, api-portal) can't use Spring Boot starters. They currently have NO JWT validation and NO circuit breakers.
* **Option A — Quarkus-Native Equivalent**:
    ```java
    // gateway-service: SecurityFilter.java (Quarkus)
    @Provider
    @Priority(Priorities.AUTHENTICATION)
    public class JwtValidationFilter implements ContainerRequestFilter {
        @ConfigProperty(name = "mp.jwt.verify.publickey.location")
        String jwksUri;

        @Override
        public void filter(ContainerRequestContext ctx) {
            String authHeader = ctx.getHeaderString("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.abortWith(Response.status(401).entity(
                    Map.of("error", "MISSING_TOKEN")).build());
                return;
            }
            try {
                JsonWebToken jwt = validateToken(authHeader.substring(7));
                ctx.setProperty("jwt", jwt);
            } catch (Exception e) {
                ctx.abortWith(Response.status(401).entity(
                    Map.of("error", "INVALID_TOKEN")).build());
            }
        }
    }
    ```
    ```properties
    # application.properties (Quarkus MicroProfile JWT)
    mp.jwt.verify.publickey.location=${KEYCLOAK_JWKS_URL:http://keycloak:8080/realms/payu/protocol/openid-connect/certs}
    mp.jwt.verify.issuer=${KEYCLOAK_ISSUER:http://keycloak:8080/realms/payu}
    quarkus.smallrye-jwt.enabled=true
    ```
* **Option B — Migrate to Spring Boot** (recommended for long-term consistency):
    ```xml
    <!-- Change parent POM from Quarkus to Spring Boot parent -->
    <parent>
        <groupId>id.payu</groupId>
        <artifactId>payu-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    ```
    Benefits: Unified starters, same security model, consistent monitoring.
    Cost: Rewrite ~500 lines per service (JAX-RS → Spring MVC, CDI → Spring DI).

## 🏗️ Architecture Patterns (Feb 2026 Audit)

### 1. Hexagonal Architecture Enforcement

* **The Problem**: 8/19 Java services use flat packages (`controller/`, `service/`, `repository/`) instead of hexagonal (`adapter/web/`, `domain/port/in/`, `domain/port/out/`, `application/service/`). This violates the architectural standard documented in ADRs.
* **Why Hexagonal Matters for Banking**:
    * Domain logic is testable without Spring/DB/Kafka infrastructure
    * Swapping a payment provider (e.g., BI-FAST → SNAP) only changes adapter, not domain
    * ArchUnit rules in `archunit-starter` enforce this — but only if packages follow the convention
* **Standard Package Structure**:
    ```
    com.payu.{service}/
    ├── adapter/
    │   ├── web/            # REST Controllers (inbound)
    │   │   └── dto/        # Request/Response DTOs
    │   ├── persistence/    # JPA Repositories, Entities (outbound)
    │   │   ├── entity/     # JPA @Entity classes
    │   │   └── mapper/     # Entity ↔ Domain mappers
    │   ├── messaging/      # Kafka producers/consumers
    │   └── client/         # HTTP clients to other services
    ├── domain/
    │   ├── model/          # Domain objects (NO annotations)
    │   ├── port/
    │   │   ├── in/         # Use case interfaces (e.g., TransferUseCase)
    │   │   └── out/        # Repository port interfaces (e.g., TransactionPort)
    │   └── exception/      # Domain exceptions
    ├── application/
    │   ├── service/        # Use case implementations
    │   └── mapper/         # DTO ↔ Domain mappers
    └── infrastructure/
        └── config/         # Spring @Configuration, Security, etc.
    ```
* **Migration Strategy for Existing Services**:
    1. Create new packages following hexagonal convention
    2. Move classes one layer at a time (start with domain → then ports → then adapters)
    3. Run ArchUnit tests after each move to validate no violations
    4. Don't refactor logic — only move files and fix imports

### 2. Dual Config File Trap (application.yaml + application.yml)

* **The Problem**: 5 services have BOTH `application.yaml` and `application.yml`. Spring Boot loads both — **last one wins**, which depends on filesystem ordering. This creates silent, unpredictable configuration bugs.
* **The Fix**: Standardize to ONE file per service:
    ```bash
    # Audit: Find services with dual configs
    find backend -name "application.yaml" -exec dirname {} \; | while read dir; do
      if [ -f "$dir/application.yml" ]; then
        echo "DUAL CONFIG: $dir"
      fi
    done

    # Fix: Merge into application.yml and delete application.yaml
    # For each affected service:
    # 1. Compare both files: diff application.yaml application.yml
    # 2. Merge unique properties into application.yml
    # 3. Delete application.yaml
    # 4. Verify: mvn spring-boot:run (no config errors)
    ```
* **Services Affected**: investment-service, lending-service, compliance-service, cms-service, ab-testing-service

### 3. Docker Compose Port Conflict Detection

* **The Problem**: `api-portal-service` (8099:8080) and `keycloak` (8099:8080) both map to host port 8099 — they cannot run simultaneously.
* **Prevention Script**:
    ```bash
    # scripts/check-port-conflicts.sh
    #!/bin/bash
    echo "Checking for port conflicts in docker-compose.yml..."
    grep -oP '"\K\d+(?=:\d+")' docker-compose.yml | sort | uniq -d | while read port; do
      echo "🔴 CONFLICT: Host port $port is used by multiple services:"
      grep -n "\"$port:" docker-compose.yml
    done
    ```
* **The Fix**: Change api-portal-service to 8100:8080 in docker-compose.yml.

### 4. next.config.ts Remote Pattern Security

* **The Problem**: `remotePatterns: [{ hostname: '**' }]` allows loading images from ANY domain — potential SSRF vector.
* **The Correct Pattern**:
    ```typescript
    // next.config.ts
    const nextConfig: NextConfig = {
      images: {
        remotePatterns: [
          { protocol: 'https', hostname: 'cdn.payu.id' },
          { protocol: 'https', hostname: 'assets.payu.id' },
          { protocol: 'https', hostname: '*.payu.id' },
          // Dev only — remove in production:
          ...(process.env.NODE_ENV === 'development'
            ? [{ protocol: 'http' as const, hostname: 'localhost' }]
            : []),
        ],
      },
    };
    ```

## 🧪 Testing Patterns (Feb 2026 Audit)

### 1. Missing Integration Tests for Financial Services

* **The Problem**: `lending-service` and `fx-service` have ZERO integration tests. These are financial services where bugs mean money loss.
* **Minimum Integration Test Template**:
    ```java
    // lending-service: LoanDisbursementIntegrationTest.java
    @SpringBootTest
    @Testcontainers
    @ActiveProfiles("test")
    class LoanDisbursementIntegrationTest {

        @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payu_lending_test");

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Autowired private LendingController controller;
        @Autowired private LoanRepository loanRepository;

        @Test
        void shouldDisburseLoanAndCreateRepaymentSchedule() {
            // Given
            var request = new LoanApplicationRequest(
                "CUST-001", BigDecimal.valueOf(10_000_000), 12, "PERSONAL"
            );

            // When
            var response = controller.applyForLoan(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            var loan = loanRepository.findById(response.getBody().getLoanId());
            assertThat(loan).isPresent();
            assertThat(loan.get().getStatus()).isEqualTo(LoanStatus.PENDING_APPROVAL);
            assertThat(loan.get().getRepaymentSchedule()).hasSize(12); // Monthly installments
        }

        @Test
        void shouldRejectLoanExceedingCreditLimit() {
            var request = new LoanApplicationRequest(
                "CUST-001", BigDecimal.valueOf(999_999_999), 12, "PERSONAL"
            );

            assertThatThrownBy(() -> controller.applyForLoan(request))
                .isInstanceOf(LendingDomainException.class)
                .hasMessageContaining("exceeds credit limit");
        }
    }
    ```

### 2. Shared Starter Testing Pattern

* **The Problem**: `outbox-starter` (10 source files, 0 tests) and `saga-starter` (20 source files, 0 tests) are critical financial components with zero test coverage.
* **Outbox Starter Test Template**:
    ```java
    @SpringBootTest
    @Testcontainers
    class OutboxPublisherIntegrationTest {

        @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

        @Container
        static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

        @Autowired private OutboxService outboxService;
        @Autowired private OutboxRepository outboxRepository;

        @Test
        void shouldPublishEventToKafkaAfterDatabaseCommit() {
            // Given: Create an outbox event within a transaction
            outboxService.createEvent("transaction", "TX-001", "TransferCompleted",
                "payu.transactions.completed", Map.of("amount", 100000));

            // Then: Event exists in outbox table
            var events = outboxRepository.findUnpublishedEvents(100);
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getAggregateType()).isEqualTo("transaction");

            // When: Publisher runs (triggered by scheduler or manually)
            outboxPublisher.publishPendingEvents();

            // Then: Event marked as published
            var published = outboxRepository.findById(events.get(0).getId());
            assertThat(published.get().getPublishedAt()).isNotNull();
        }

        @Test
        void shouldRetryFailedEventsUpToMaxAttempts() {
            // ... test retry behavior
        }

        @Test
        void shouldNotPublishDuplicateEvents() {
            // ... test idempotency
        }
    }
    ```

### 3. Contract Testing Between Services

* **The Problem**: 22 microservices communicate without contract tests. API changes break consumers silently.
* **Recommended Pattern (Pact)**:
    ```java
    // wallet-service (provider) verifies contract with transaction-service (consumer)
    @Provider("wallet-service")
    @PactBroker(url = "${PACT_BROKER_URL}")
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    class WalletServicePactVerificationTest {

        @TestTemplate
        @ExtendWith(PactVerificationInvocationContextProvider.class)
        void pactVerificationTestTemplate(PactVerificationContext context) {
            context.verifyInteraction();
        }
    }
    ```
    ```java
    // transaction-service (consumer) defines expected contract
    @ExtendWith(PactConsumerTestExt.class)
    class WalletServiceConsumerPactTest {

        @Pact(provider = "wallet-service", consumer = "transaction-service")
        V4Pact debitWalletPact(PactDslWithProvider builder) {
            return builder
                .given("wallet WALLET-001 exists with balance 10000000")
                .uponReceiving("a debit request")
                .method("POST")
                .path("/api/v1/wallets/WALLET-001/debit")
                .body(new PactDslJsonBody()
                    .decimalType("amount", 100000.00)
                    .stringType("currency", "IDR")
                    .stringType("referenceId", "TX-001"))
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                    .stringType("walletId", "WALLET-001")
                    .decimalType("balance", 9900000.00))
                .toPact(V4Pact.class);
        }
    }
    ```
* **Critical Pairs to Test First**:
    1. transaction-service ↔ wallet-service (money movement)
    2. transaction-service ↔ account-service (account validation)
    3. lending-service ↔ wallet-service (loan disbursement)
    4. billing-service ↔ wallet-service (bill payment)

---

## 🔑 Encryption & Key Management (Feb 2026 - P3 Audit)

### 1. Key Rotation Pattern for AES-GCM EncryptionService (Feb 9, 2026)

* **The Problem**: `security-starter`'s `EncryptionService` used a single static encryption key with no rotation mechanism. Key compromise meant all historical data was permanently exposed with no migration path.
* **The Fix**: Added multi-key decryption with fallback chain:
    ```java
    // Constructor accepts current key + ordered list of previous keys
    public EncryptionService(String currentKey, List<String> previousKeys) {
        this.currentKeyVersion = previousKeys.size() + 1;
        this.secretKey = deriveKey(currentKey);
        this.previousKeys = previousKeys.stream().map(this::deriveKey).toList();
    }
    ```
* **Decrypt Strategy**: Try current key first → on failure, iterate previous keys → log which version succeeded → throw if none match.
* **Re-encryption Helper**: `reEncrypt(ciphertext)` decrypts with any known key, re-encrypts with current key. Use in batch migration jobs.
* **Backward Compatibility**: Original single-key constructor delegates to `this(key, Collections.emptyList())` — zero breaking changes.
* **Key Lesson**: Don't embed key version bytes in ciphertext format. AES-GCM authentication tag naturally rejects wrong keys, so trial decryption is both simpler and backward-compatible with existing encrypted data.
* **Testing**: 6 dedicated rotation tests (multi-key decrypt, re-encrypt, database round-trip, unknown key failure).

### 2. PBKDF2 Key Derivation — Already Fixed, Document the Why (Feb 9, 2026)

* **Context**: P1 fix upgraded `EncryptionService` from SHA-256 to PBKDF2WithHmacSHA256 (600k iterations per OWASP 2024).
* **Key Lesson**: When changing key derivation, ALL existing encrypted data becomes unreadable unless you maintain backward compatibility. The key rotation pattern (Lesson 1 above) solves this — put the old SHA-256-derived key as a `previousKey` during migration.
* **Migration Path**:
    1. Deploy with `new EncryptionService(newPBKDF2Key, List.of(oldSHA256Key))`
    2. Run batch `reEncrypt()` on all encrypted columns
    3. Remove old key from config after migration complete

---

## 🧪 Testing Infrastructure (Feb 2026 - P2/P3 Audit)

### 4. Spring Boot AutoConfiguration Testing with ApplicationContextRunner (Feb 9, 2026)

* **The Problem**: Testing `@Configuration` and `@ConditionalOnProperty` classes requires a Spring context but full `@SpringBootTest` is slow and brittle.
* **The Fix**: Use `ApplicationContextRunner` for isolated, fast auto-configuration tests:
    ```java
    @Test
    void autoConfigurationEnabledByDefault() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventsAutoConfiguration.class))
            .run(context -> {
                assertThat(context).hasSingleBean(CloudEventBuilder.class);
                assertThat(context).hasSingleBean(EventsProperties.class);
            });
    }

    @Test
    void autoConfigurationDisabledWhenPropertyFalse() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventsAutoConfiguration.class))
            .withPropertyValues("payu.events.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(CloudEventBuilder.class);
            });
    }
    ```
* **Key Lesson**: `ApplicationContextRunner` boots in ~200ms vs ~5s for `@SpringBootTest`. Use it for ALL starter auto-configuration tests. No need for `@SpringBootApplication` test class.
* **Applies To**: events-starter, outbox-starter, saga-starter, cache-starter, resilience-starter, security-starter.

### 5. Spring Cloud Contract — Groovy DSL for Provider-Driven Contracts (Feb 9, 2026)

* **The Problem**: 22 microservices communicating via REST with no contract tests. API changes (renamed fields, changed status codes) break consumers silently.
* **The Pattern**: Provider-driven contracts using Groovy DSL:
    ```groovy
    Contract.make {
        description "Transfer funds between wallets"
        request {
            method POST()
            url '/api/v1/transactions/transfer'
            headers {
                contentType applicationJson()
                header 'Authorization': $(consumer(regex('Bearer .+')))
                header 'X-Idempotency-Key': $(consumer(regex('[a-f0-9-]+')))
            }
            body([
                sourceWalletId: $(consumer(regex('[a-f0-9-]+')), producer('wallet-src-uuid')),
                amount: $(consumer(regex('[0-9]+')), producer(100000))
            ])
        }
        response {
            status 201
            body([
                transactionId: $(producer(regex('[a-f0-9-]+')))
            ])
        }
    }
    ```
* **Directory Convention**: `tests/contract/<provider-service>/<contractName>.groovy`
* **Key Lesson**: Start with the 4 critical financial pairs (transaction↔wallet, transaction↔account, lending↔wallet, billing↔wallet). Groovy DSL is more readable than Pact JSON for banking contracts.

### 6. OWASP ZAP DAST — Podman-Based Security Scanning (Feb 9, 2026)

* **The Problem**: Security tests were static-only (config file checks, report existence). No actual HTTP-based vulnerability scanning.
* **The Fix**: OWASP ZAP via podman with Automation Framework:
    ```bash
    podman run --rm -v $(pwd)/reports:/zap/wrk:rw \
      --network host \
      ghcr.io/zaproxy/zaproxy:stable \
      zap.sh -cmd -autorun /zap/wrk/zap-automation.yaml
    ```
* **Three Scan Modes**:
    1. `baseline` — passive scan only (5 min, safe for CI)
    2. `api` — OpenAPI spec import + passive (10 min)
    3. `full` — active scan with injection tests (30+ min, staging only)
* **CI Gate**: Parse JSON report, exit 1 if any High/Critical alerts found:
    ```bash
    HIGH_ALERTS=$(jq '[.site[].alerts[] | select(.riskcode >= "3")] | length' report.json)
    [[ "$HIGH_ALERTS" -gt 0 ]] && exit 1
    ```
* **Key Lesson**: Never run active scan against production. Use `--network host` to reach services on localhost. Suppress known false positives (CSP on API endpoints, anti-CSRF on stateless APIs).

### 7. PITest Mutation Testing Configuration (Feb 9, 2026)

* **The Problem**: Tests pass but may have weak assertions (e.g., `assertNotNull` instead of value checks). No way to measure test quality beyond line coverage.
* **The Fix**: Added PITest 1.15.0 to parent POM `pluginManagement`:
    ```xml
    <plugin>
        <groupId>org.pitest</groupId>
        <artifactId>pitest-maven</artifactId>
        <version>1.15.0</version>
        <dependencies>
            <dependency>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-junit5-plugin</artifactId>
                <version>1.2.1</version>
            </dependency>
        </dependencies>
        <configuration>
            <mutationThreshold>60</mutationThreshold>
            <coverageThreshold>70</coverageThreshold>
            <threads>4</threads>
            <excludedClasses>
                <param>id.payu.*.config.*</param>
                <param>id.payu.*Application</param>
            </excludedClasses>
        </configuration>
    </plugin>
    ```
* **Usage**: `mvn org.pitest:pitest-maven:mutationCoverage -pl :service-name`
* **Key Lesson**: Exclude `*config.*` and `*Application` classes — they're infrastructure, not business logic. Set initial thresholds low (60% mutation, 70% coverage) and ratchet up over sprints. 4 threads balances speed vs memory for monorepo builds.

### 8. Load Test Consolidation — Symlink Strategy (Feb 9, 2026)

* **The Problem**: `tests/load-tests/` had an empty scaffold (pom.xml, config), while real Gatling simulations lived in `tests/performance/`. Contributors didn't know which to use.
* **The Fix**: Symlinks instead of moving files (preserves git history):
    ```bash
    ln -s ../../performance/src/test/scala tests/load-tests/src/gatling/scala
    ln -s ../../performance/src/test/resources/data tests/load-tests/src/gatling/resources/data
    ```
* **Key Lesson**: When consolidating duplicated test directories, symlinks are better than file moves. Git tracks the link, both paths work, and no git history is lost. Document the consolidation in a README at the symlink root.

### 9. Verifying Issues Before Fixing — False Positive Detection (Feb 9, 2026)

* **The Problem**: TODOS.md listed "P3-ARCH-001: No GlobalExceptionHandler in api-commons" as an issue. Investigation revealed it already existed with 12 `@ExceptionHandler` methods.
* **The Lesson**: Always `grep` or search the codebase before implementing a fix for a reported issue. Audit findings can become stale as the codebase evolves. In this session, 3 of 19 P2/P3 items were false positives or already-deprecated:
    1. P3-ARCH-001 — GlobalExceptionHandler already existed
    2. P2-INFRA-002 — Traefik insecure only in deprecated archive
    3. P2-INFRA-003 — Kafka Zookeeper only in deprecated archive
* **Best Practice**: Before marking an issue on a roadmap, include the exact file path and line number. Stale issues without references waste engineering time.

---

## 🎨 Frontend Engineering (Feb 2026 - P2/P3 Audit)

### 5. Zustand Store Design for Banking Apps (Feb 9, 2026)

* **The Problem**: Only 2 Zustand stores (`authStore`, `uiStore`) for a 22-route banking app. Server state was handled by TanStack Query hooks, but client-only state (UI filters, optimistic updates, notification drawer) had no home.
* **The Pattern**: Separate concerns between TanStack Query (server state) and Zustand (client state):
    ```
    TanStack Query: useWallet(), useAuth(), useTransactions()  → server data + cache
    Zustand:        walletStore, transactionStore               → UI state + optimistic updates
    ```
* **Stores Added**:
    1. `notificationStore` — unread count, drawer open/close, mark-as-read (client-only UI state)
    2. `walletStore` — cached balance for instant display, optimistic debit on transfer initiation
    3. `transactionStore` — filter state (type, status, date range, search), selected transaction for detail panel
* **Key Lesson**: Don't duplicate server state in Zustand. Use Zustand for: UI state (drawers, filters, selections), optimistic updates (show balance change before API confirms), and derived state (unread notification count).

### 6. Jest to Vitest Migration — Deprecation Strategy (Feb 9, 2026)

* **The Problem**: Both `vitest.config.ts` and `jest.config.js` existed. New contributors ran the wrong runner. CI used Vitest but Jest config confused people.
* **The Fix**: Rename, don't delete:
    ```bash
    mv jest.config.js jest.config.js.deprecated
    mv jest.setup.js jest.setup.js.deprecated
    ```
* **Why Rename Instead of Delete**:
    1. Git history preserved — `git log --follow jest.config.js.deprecated` still works
    2. Any CI scripts referencing old config fail loudly (file not found) instead of silently running stale tests
    3. `.deprecated` suffix is self-documenting — no need for a migration guide
* **Key Lesson**: Verify the new runner already covers everything before deprecating. In our case, `vitest.setup.ts` already had `IntersectionObserver` mock and `jest-dom` matchers that were in `jest.setup.js`. No functionality lost.

### 7. Next.js Loading Skeletons for LCP Optimization (Feb 9, 2026)

* **The Problem**: Lighthouse LCP at 9.3s on key routes. Users see blank screen while page data loads.
* **The Fix**: Add `loading.tsx` to each route directory. Next.js automatically shows it during navigation:
    ```tsx
    // app/[locale]/dashboard/loading.tsx
    export default function DashboardLoading() {
      return (
        <div className="animate-pulse">
          {/* Balance card skeleton */}
          <div className="p-6 rounded-3xl border bg-card mb-8">
            <div className="h-10 w-56 bg-muted rounded-xl mb-4" />
            <div className="flex gap-3">
              <div className="h-10 w-28 bg-muted rounded-full" />
            </div>
          </div>
          {/* Transaction list skeleton */}
          {[...Array(5)].map((_, i) => (
            <div key={i} className="flex items-center gap-4">
              <div className="w-10 h-10 bg-muted rounded-full" />
              <div className="h-4 w-32 bg-muted rounded" />
            </div>
          ))}
        </div>
      );
    }
    ```
* **Routes Covered**: dashboard, transfer, investments, lending, bills (the 5 highest-traffic routes).
* **Key Lesson**: Skeleton shapes should match the actual UI layout — users perceive faster load when the skeleton resembles the final page. Use `animate-pulse` (Tailwind) for subtle shimmer. Don't skeleton everything — focus on above-the-fold content.

### 8. Client-Side Search Component for Documentation (Feb 9, 2026)

* **The Problem**: Developer docs portal had no search. Developers had to manually navigate through guides.
* **The Fix**: Client-side search with static index (no external service needed):
    ```tsx
    const SEARCH_INDEX: SearchResult[] = [
      { title: 'Authentication', description: 'OAuth2, JWT...', path: '/getting-started/auth', category: 'Getting Started' },
      // ... all pages
    ];
    ```
* **UX Pattern**: Cmd/Ctrl+K modal (matches VS Code, GitHub, Stripe docs convention):
    - Arrow key navigation with visual highlight
    - Category badges for result grouping
    - Escape to close, Enter to navigate
* **Key Lesson**: For small doc sites (<50 pages), a static search index is simpler and faster than Algolia/Meilisearch. Maintain the index alongside page creation. Consider moving to a dynamic index (crawl pages at build time) when docs exceed 100 pages.

---

## 🏗️ Infrastructure & CI/CD (Feb 2026 - P2/P3 Audit)

### 8. OpenShift Image Pinning — Registry + Semver (Feb 9, 2026)

* **The Problem**: All 25 OpenShift manifests used `image: <service>:latest`. In production, `:latest` means:
    - No rollback capability (which version was "latest"?)
    - No audit trail for deployments
    - Pods may pull different versions during rolling updates
* **The Fix**: Pin to internal registry with semver:
    ```yaml
    # Before
    image: account-service:latest

    # After
    image: image-registry.openshift-image-registry.svc:5000/payu/account-service:1.0.0
    ```
* **Bulk Update Command**:
    ```bash
    for f in infrastructure/openshift/base/*.yaml; do
      service=$(basename "$f" .yaml)
      sed -i "s|image: ${service}:latest|image: image-registry.openshift-image-registry.svc:5000/payu/${service}:1.0.0|" "$f"
    done
    ```
* **Key Lesson**: In CI/CD pipelines, the build step should update the image tag in the manifest (via Kustomize overlay or sed) before ArgoCD syncs. Never rely on `:latest` in any environment beyond local development.

### 9. Tekton Task Definitions — Complete CI/CD Task Set (Feb 9, 2026)

* **The Problem**: Tekton pipelines referenced 5 tasks (maven, buildah, deploy, trivy, pytest) but only `security-scan-task.yaml` existed. Pipelines were non-functional.
* **The Fix**: Created all 5 task definitions:
    1. `maven-task.yaml` — UBI9 OpenJDK 21, extracts version from POM, produces JAR
    2. `buildah-task.yaml` — Rootless build with VFS storage driver (required for OpenShift), outputs image digest
    3. `deploy-task.yaml` — 3-step: patch manifest → rollout wait → health check (10 retries, 10s interval)
    4. `trivy-task.yaml` — Severity gate (CRITICAL,HIGH by default), configurable via params
    5. `pytest-task.yaml` — Python 3.12, markers support, JUnit XML output
* **Key Pattern for Tekton Tasks**:
    ```yaml
    # Always include resource limits for OpenShift quota compliance
    stepTemplate:
      resources:
        requests: { cpu: 500m, memory: 1Gi }
        limits: { cpu: 2, memory: 2Gi }

    # Use workspaces for sharing between tasks in a pipeline
    workspaces:
      - name: source
        description: Source code workspace
      - name: dockerconfig
        description: Docker registry credentials
        optional: true
    ```
* **Key Lesson**: Tekton tasks should be reusable across pipelines. Use `params` for service-specific values (service name, image tag) and `results` to pass outputs between tasks (image digest, version string). Always set resource limits — OpenShift rejects pods without them.

---

## ☁️ OpenShift Deployment (Feb 18, 2026)

### 1. DataGrid (Infinispan) 8.5.x RESP Connector

* **No Custom Port Attribute**: DataGrid 8.5.14 does NOT support `port:` attribute on `respConnector`. The RESP protocol shares the default `socketBinding` port (11222). Adding `port: 6379` causes CrashLoopBackOff with `ISPN000encoding` errors.
* **Authentication Required**: RESP connector requires `endpointAuthentication: true` with a credential secret in `identities.yaml` format. Without it, DataGrid crashes because RESP needs a security realm with password-capable identities.
* **TLS Mismatch**: If `endpointEncryption.type: Service` (default on OpenShift), clients must use `rediss://` (TLS). Using plain `redis://` results in `CONNECTION_CLOSED` immediately. For dev, set `endpointEncryption.type: None` and use `redis://`.
* **Auth URL Format**: Quarkus Redis client supports `redis://username:password@host:port` format for AUTH with DataGrid RESP.

### 2. Keycloak (RHSSO) ExternalName DNS

* **FQDN Required**: ExternalName services MUST use fully qualified domain names (FQDN) ending in `.svc.cluster.local`. Short names like `payu-postgres-primary.payu-dev.svc` cause DNS NXDOMAIN from within pods because the DNS search path doesn't always resolve them.
* **The Symptom**: Keycloak CrashLoopBackOff with `Connection refused` or `NXDOMAIN` to the PostgreSQL ExternalName service.
* **The Fix**: Always use `payu-postgres-primary.payu-dev.svc.cluster.local` in ExternalName services and secrets.

### 3. NetworkPolicy & Pod Labels (Critical)

* **The Problem**: If a NetworkPolicy selects a pod (via `podSelector`), only traffic explicitly allowed by matching policies is permitted. The `allow-from-router` policy selected gateway/web-app pods (by `app` label) but only allowed router ingress — blocking all intra-namespace pod-to-pod traffic.
* **The Symptom**: Web-app → gateway-service:8080 connection timeout. External routes work fine (router → pod), but internal service-to-service calls fail.
* **The Fix**: Ensure all service pods have the `app.kubernetes.io/part-of: payu-banking` label so the `allow-intra-namespace` NetworkPolicy also applies. Use `commonLabels` in Kustomize base to guarantee this.
* **Key Insight**: NetworkPolicies are additive per pod. If Pod X is selected by Policy A (allows router) and Policy B (allows intra-namespace), BOTH sets of rules apply. But if only Policy A selects it, only router traffic is allowed.

### 4. Vault Dev Mode on OpenShift

* **No StatefulSet Needed**: Vault in `-dev` mode doesn't persist data. Use a simple `Deployment` + `Service` instead of StatefulSet.
* **K8s Auth**: Vault Secrets Operator (VSO) needs `VaultConnection` (address) + `VaultAuth` (kubernetes method, role, mount) resources. The Vault init job should configure `vault auth enable kubernetes`, write policies, and create roles.
* **VAULT_ADDR**: Always explicitly set `VAULT_ADDR=http://127.0.0.1:8200` in healthchecks — Vault defaults to HTTPS which fails in dev mode.

### 5. Kustomize Image Transformers for OCP Internal Registry

* **Pattern**: Use `images:` in Kustomize overlay to remap image names from external to internal registry:
    ```yaml
    images:
      - name: external-registry/payu-dev/account-service
        newName: image-registry.openshift-image-registry.svc:5000/payu-dev/account-service
        newTag: "1.2.0"
    ```
* **Gotcha**: The `name` must match the image reference in the base manifest exactly, including any registry prefix.

### 6. cert-manager on OpenShift

* **Operator Namespace**: The `openshift-cert-manager-operator` subscription goes in `cert-manager-operator` namespace (not `openshift-operators`).
* **AWS Credentials Secret**: Must be in `cert-manager` namespace (where the cert-manager controller runs), not in the application namespace.
* **ClusterIssuer**: Cluster-scoped resource — no namespace needed. Certificates are namespace-scoped and reference the ClusterIssuer by name.

---

## 🧪 E2E Testing with Playwright (Feb 18, 2026)

### 1. Test Selectors Must Match Actual UI

* **The Problem**: E2E tests fail when they reference UI elements that don't exist (e.g., looking for "Domisili Saat Ini" field that was removed from settings page).
* **The Symptom**: Tests timeout with `locator.fill: Timeout 15000ms exceeded` or similar errors.
* **The Fix**: Always verify test selectors against actual rendered UI. When UI changes, update tests immediately.
* **Best Practice**: Use stable selectors like `data-testid` instead of text content when possible.

### 2. Form Placeholder Mismatches

* **The Problem**: Tests use hardcoded placeholders that don't match the actual input placeholders in the UI.
* **Example**: Test expects `PENGGUNA PAYU` but UI has `Nama lengkap`.
* **The Fix**: Keep test placeholders in sync with UI. Use page object pattern to centralize selector definitions.

### 3. Button State Assertions

* **The Problem**: Testing `toBeEnabled()` on buttons that may be disabled during loading states causes flaky tests.
* **The Fix**: Use `toBeAttached()` or `toBeVisible()` for buttons that may have dynamic enable/disable states based on data loading.
* **Alternative**: Wait for specific conditions before asserting button state.

### 4. Type Compatibility Between Services and UI

* **The Problem**: TypeScript interfaces for Transaction types differ between `services/TransactionService.ts` and page components, causing type errors.
* **The Fix**: Import types from a single source of truth (e.g., `types/index.ts`) rather than defining duplicate interfaces.
* **Pattern**: Use helper functions like `isCreditType(type: string)` instead of hardcoded string comparisons.

### 5. Dependency Management for E2E

* **The Problem**: Missing runtime dependencies (like `sonner` for toasts or `@radix-ui/react-select` for Select component) cause build failures.
* **The Fix**: Always verify `package.json` dependencies include all UI libraries used in components under test.
* **Check**: Run `npm run type-check` before committing E2E tests to catch missing dependencies early.

---

_See also: [REMEDIATION_PLAYBOOK.md](REMEDIATION_PLAYBOOK.md) for prioritized step-by-step action plans._

