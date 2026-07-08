# Method of Procedure (MOP) - PayU Infrastructure Deployment

> Current operational runbook for deploying PayU infrastructure on Red Hat OpenShift.
> This replaces the obsolete Crunchy/Postgres example guide and uses the current
> Kustomize entrypoints in `infrastructure/`.

## Document Control

| Field | Value |
|:---|:---|
| Scope | Infrastructure bootstrap, platform services, workload deployment, verification, rollback |
| Target platform | Red Hat OpenShift 4.20+ |
| Last verified cluster | OCP 4.20.26, Kubernetes v1.33.12, 7 Ready nodes |
| Last verified date | 2026-07-08 |
| Primary namespace | `payu-dev` for current data, messaging, cache, and application workloads |
| Change mode | Manual bootstrap first; GitOps only after ApplicationSet reconciliation is complete |

## Critical Drift Notes

Do not use the old deployment path `infrastructure/openshift/examples/`; it is no longer the source of truth.

Current state differs from older docs:

| Area | Current source of truth | Notes |
|:---|:---|:---|
| Namespaces | `infrastructure/foundation/namespaces/` | Creates `payu-dev`, `payu-sit`, `payu-uat`, `payu-preprod`, `payu`, and `payu-cicd` |
| Operators | `infrastructure/foundation/cluster-operators/` | Installs CNPG, Data Grid, AMQ Streams, AMQ Broker, RHBK, 3scale, GitOps, Pipelines, Vault Secrets, Tempo, Compliance |
| Database | `infrastructure/platform/data/base/current/cnpg-*.yaml` | CloudNativePG `payu-database` in `payu-dev`; not Crunchy |
| Kafka | `infrastructure/platform/data/base/kafka-amqstreams.yaml` | AMQ Streams `payu-kafka` in `payu-dev` |
| Cache | `infrastructure/platform/data/base/current/datagrid.yaml` | Infinispan `payu-cache`; RESP clients use `payu-cache-resp:11222`, not direct pod port `6379` |
| AMQ Broker | `infrastructure/platform/amq-broker/base/` | `payu-broker` with CORE, AMQP, and STOMP on `61616` |
| Identity | `infrastructure/platform/identity/keycloak/` | RHBK `payu-keycloak` in `payu-sso`; verify CR conditions before declaring healthy |
| API management | `infrastructure/platform/api-management/` | Operator/policy only by default; `APIManager` is gated until external backing-store and Vault secrets exist |
| Workloads | `infrastructure/workloads/overlays/<env>/` | Use environment overlays, not workload base directly |

## Execution Rules

1. Run every command through `rtk`.
2. Never commit or apply real secrets from Git. Create runtime secrets from Vault, External Secrets, or one-time `oc create secret` commands.
3. Render before apply. Every touched Kustomize root must pass `oc kustomize`.
4. Apply in dependency order: namespaces, operators, data/messaging, identity, API management shell, images, workloads.
5. Stop the deployment if any infrastructure CR reports `Ready=False`, `HasErrors=True`, CrashLoopBackOff, or recent Warning events.
6. Do not apply `3scale/apimanager.yaml` until external backing-store, storage, and Vault-managed secrets are available.
7. Do not enable automated GitOps sync until `INFRA-020` is closed and the live `payu-dev` image tags match the overlay.

## Environment Map

| Environment | Namespace | Overlay |
|:---|:---|:---|
| Development | `payu-dev` | `infrastructure/workloads/overlays/payu-dev` |
| SIT | `payu-sit` | `infrastructure/workloads/overlays/payu-sit` |
| UAT | `payu-uat` | `infrastructure/workloads/overlays/payu-uat` |
| Preprod | `payu-preprod` | `infrastructure/workloads/overlays/payu-preprod` |
| Production | `payu` | `infrastructure/workloads/overlays/payu-prod` |

Platform namespaces:

| Namespace | Purpose |
|:---|:---|
| `openshift-operators` | Cluster-wide operators |
| `openshift-gitops` / `openshift-gitops-operator` | Argo CD and GitOps operator |
| `payu-cicd` | Tekton pipelines and image build permissions |
| `payu-sso` | RHBK/Keycloak |
| `payu-api-management` | 3scale operator and API management |

## Preflight Checklist

Examples in this MOP target `payu-dev`. For another environment, replace the namespace and overlay path using the environment map above.

Verify cluster and permissions:

```bash
rtk oc whoami
rtk oc version
rtk oc auth can-i '*' '*' --all-namespaces
rtk oc get nodes
rtk oc get clusterversion
rtk oc get storageclass
rtk oc get route default-route -n openshift-image-registry
```

Take a pre-change snapshot:

```bash
rtk mkdir -p /tmp/payu-mop
rtk oc get nodes -o wide > /tmp/payu-mop/nodes.before.txt
rtk oc get pods -A > /tmp/payu-mop/pods.before.txt
rtk oc get events -A --sort-by=.lastTimestamp > /tmp/payu-mop/events.before.txt
rtk oc get csv -A > /tmp/payu-mop/csv.before.txt
```

Render all mandatory roots:

```bash
rtk oc kustomize infrastructure/foundation/namespaces >/tmp/payu-mop/namespaces.yaml
rtk oc kustomize infrastructure/foundation/cluster-operators >/tmp/payu-mop/operators.yaml
rtk oc kustomize infrastructure/platform/data/base >/tmp/payu-mop/data.yaml
rtk oc kustomize infrastructure/platform/amq-broker/base >/tmp/payu-mop/amq-broker.yaml
rtk oc kustomize infrastructure/platform/api-management >/tmp/payu-mop/api-management.yaml
rtk oc kustomize infrastructure/workloads/overlays/payu-dev >/tmp/payu-mop/workloads.yaml
```

Run server-side dry-run where CRDs already exist:

```bash
rtk oc apply --server-side --dry-run=server -k infrastructure/foundation/namespaces
rtk oc apply --server-side --dry-run=server -k infrastructure/foundation/cluster-operators
rtk oc apply --server-side --dry-run=server -k infrastructure/platform/api-management
```

## Procedure

### 1. Apply Foundation Namespaces

```bash
rtk oc apply -k infrastructure/foundation/namespaces
rtk oc get ns | rtk rg 'payu|gitops|operators'
```

Expected result:

- `payu-dev`, `payu-sit`, `payu-uat`, `payu-preprod`, `payu`, and `payu-cicd` exist.
- Platform namespaces for SSO, API management, and GitOps exist.
- Default-deny NetworkPolicies, LimitRanges, ResourceQuotas, and image-push RoleBindings exist.

### 2. Install Operators

```bash
rtk oc apply -k infrastructure/foundation/cluster-operators
rtk oc get subscriptions -A
rtk oc get csv -A
```

Wait for required operators:

```bash
rtk oc get csv -A | rtk rg 'cloudnative|datagrid|amq|3scale|keycloak|gitops|pipelines|vault|tempo|compliance'
```

Do not proceed until each required CSV is `Succeeded`.

### 3. Deploy Data, Kafka, and Cache

```bash
rtk oc apply -k infrastructure/platform/data/base -n payu-dev
```

Verify:

```bash
rtk oc get cluster.postgresql.cnpg.io -n payu-dev
rtk oc get kafka -n payu-dev
rtk oc get infinispan -n payu-dev
rtk oc get pods -n payu-dev | rtk rg 'payu-database|payu-kafka|payu-cache'
rtk oc get events -n payu-dev --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -40
```

Expected result:

- CNPG `payu-database` has 3 instances Ready.
- Kafka `payu-kafka` reports `READY=True`.
- Infinispan `payu-cache` pods are Running and using the custom XML configuration ConfigMap (`payu-cache-custom-config`) to enable the RESP connector.
- `payu-cache-resp` service points to the Data Grid RESP connector and exposes service port `11222`.

### 4. Deploy AMQ Broker

```bash
rtk oc apply -k infrastructure/platform/amq-broker/base -n payu-dev
```

Verify:

```bash
rtk oc get activemqartemis -n payu-dev
rtk oc get svc -n payu-dev | rtk rg 'payu-broker|artemis'
rtk oc get pods -n payu-dev | rtk rg 'payu-broker'
rtk oc get events -n payu-dev --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -40
```

Expected result:

- `payu-broker` reports `READY=True`.
- `payu-broker-hdls-svc` and `artemis` service exist.
- STOMP, CORE, and AMQP are available on `61616`.

### 5. Create Runtime Secrets

Secrets are not part of the normal GitOps apply path. Create them from Vault or External Secrets in real environments.

Minimum workload secrets expected by current deployments:

| Secret | Namespace | Purpose |
|:---|:---|:---|
| `db-secrets` | workload namespace | DB username/password and Python async DB URLs |
| `dev-env-secrets` or env-specific equivalent | workload namespace | encryption salt, webhook secret, JWT secret material |
| `payu-cache-credentials` | `payu-dev` | Data Grid endpoint user/password |
| `payu-broker-credentials-secret` | `payu-dev` | AMQ broker credentials |
| `payu-keycloak-admin` | `payu-sso` | RHBK bootstrap admin |
| `payu-keycloak-db` | `payu-sso` | RHBK database username/password |

Validation:

```bash
rtk oc get secret db-secrets dev-env-secrets payu-cache-credentials payu-broker-credentials-secret -n payu-dev
rtk oc get secret payu-keycloak-admin payu-keycloak-db -n payu-sso
```

For production, do not apply `infrastructure/workloads/base/db-secrets.yaml` or `dev-env-secrets.yaml` as-is. Those files are development-only shapes and contain non-production values.

### 6. Deploy Identity

Apply the RHBK stack after CNPG and `payu-keycloak-db` are available:

```bash
rtk oc apply -k infrastructure/platform/identity/keycloak
```

Verify:

```bash
rtk oc get keycloak -n payu-sso
rtk oc get pods -n payu-sso
rtk oc get route -n payu-sso
rtk oc get keycloak -n payu-sso payu-keycloak -o jsonpath='{range .status.conditions[*]}{.type}={.status}{"\n"}{end}'
```

Expected result:

- `payu-keycloak-0` is `1/1 Running`.
- `Ready=True`.
- `HasErrors` is absent or `False`.
- The route resolves and returns a Keycloak response.

Current known gate as of 2026-07-08:

- `payu-keycloak-0` is Running, but the `Keycloak` CR reports `Ready=Unknown` and `HasErrors=True` after a service patch conflict. Treat identity as not production-healthy until that CR condition is cleared.

### 7. Deploy API Management Shell

Install only the safe default API management resources:

```bash
rtk oc apply -k infrastructure/platform/api-management
rtk oc get subscriptions -n payu-api-management
rtk oc get pods -n payu-api-management
```

This root intentionally excludes:

- `3scale/apimanager.yaml`
- `3scale/payu-capabilities.yaml`

Apply the full APIManager only after external 3scale backing-store, storage, and Vault-managed secrets exist:

```bash
rtk oc get secret system-seed system-database zync apicast-payu-env system-events-hook -n payu-api-management
rtk oc apply -f infrastructure/platform/api-management/3scale/system-storage-pvc.yaml
rtk oc apply -f infrastructure/platform/api-management/3scale/apimanager.yaml
```

### 8. Build and Push Images

For `payu-dev`, build the exact images referenced by the rendered overlay:

```bash
rtk env OVERLAY=infrastructure/workloads/overlays/payu-dev NAMESPACE=payu-dev scripts/build-push-ocp.sh
rtk oc get is -n payu-dev
rtk oc get istag -n payu-dev | rtk wc -l
```

Production rule:

- Image tags must match the SemVer release tag.
- Do not use `latest`.
- Promote immutable images between namespaces; do not rebuild different bytes with the same tag.

### 9. Deploy Workloads

Apply the environment overlay:

```bash
rtk oc apply -k infrastructure/workloads/overlays/payu-dev
rtk proxy bash -lc 'set -euo pipefail; for deploy in $(oc get deployment -n payu-dev -o name); do oc rollout status "$deploy" -n payu-dev --timeout=10m; done'
```

If the rollout command needs per-deployment handling:

```bash
rtk oc rollout status deployment/<service> -n payu-dev --timeout=10m
```

Verify:

```bash
rtk oc get deployments -n payu-dev
rtk oc get pods -n payu-dev
rtk oc get events -n payu-dev --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -80
rtk env NAMESPACE=payu-dev scripts/deployment/verify-deployment.sh
```

Expected result for current `payu-dev`:

- 32/32 deployments Ready.
- 46/46 pods Running.
- No new Warning events after rollout.
- Logs for changed services contain no `error`, `warn`, `exception`, `traceback`, `failed`, or `unavailable` matches after startup stabilization.

### 10. GitOps Handoff

Current GitOps state:

- OpenShift GitOps operator is installed.
- No `Application` resources are currently present in `openshift-gitops`.
- `INFRA-020` remains open for reconciling ApplicationSet with manually recovered `payu-dev` workloads.

Do not enable automated sync until the overlay and live cluster are reconciled:

```bash
rtk oc get application -n openshift-gitops
rtk oc get applicationset -n openshift-gitops
rtk oc diff -k infrastructure/workloads/overlays/payu-dev
```

When ready, apply the GitOps app-of-apps:

```bash
rtk oc apply -k infrastructure/platform/cicd/argocd
rtk oc get application -n openshift-gitops
rtk oc get applicationset -n openshift-gitops
```

Enable automated sync only after manual `oc diff` shows no destructive drift.

## Post-Deployment Acceptance Criteria

Infrastructure:

```bash
rtk oc get nodes
rtk oc get csv -A
rtk oc get cluster.postgresql.cnpg.io -n payu-dev
rtk oc get kafka -n payu-dev
rtk oc get infinispan -n payu-dev
rtk oc get activemqartemis -n payu-dev
rtk oc get keycloak -n payu-sso
```

Workloads:

```bash
rtk oc get deployments -n payu-dev
rtk oc get pods -n payu-dev
rtk oc get events -n payu-dev --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -80
```

Logs:

```bash
rtk proxy bash -lc 'set -euo pipefail; for deploy in $(oc get deploy -n payu-dev -o jsonpath="{range .items[*]}{.metadata.name}{\"\\n\"}{end}"); do echo "== $deploy =="; oc logs -n payu-dev deploy/$deploy --since=10m --all-containers=true | rg -i "error|warn|exception|traceback|failed|unavailable" || true; done'
```

Release evidence to save in the deployment ticket:

- Render commands and output file paths.
- `oc version`, `oc get nodes`, and `oc get csv -A`.
- Data/messaging/cache CR readiness.
- Workload deployment readiness.
- Recent warning events.
- Log scan result for changed services.
- Commit SHA and image tags deployed.

## Rollback Procedure

For workload deployment failure:

```bash
rtk oc rollout undo deployment/<service> -n payu-dev
rtk oc rollout status deployment/<service> -n payu-dev --timeout=10m
rtk oc logs -n payu-dev deployment/<service> --since=10m --all-containers=true
```

For manifest drift:

```bash
rtk git revert <bad_commit_sha>
rtk oc apply -k infrastructure/workloads/overlays/payu-dev
```

For infrastructure CR failure:

1. Stop applying dependent layers.
2. Capture the CR, pod logs, and events.
3. Revert only the last applied manifest set.
4. Do not delete PVCs, CNPG clusters, Kafka clusters, or Keycloak CRs without an approved data-loss plan.

Evidence commands:

```bash
rtk oc get <kind> <name> -n <namespace> -o yaml > /tmp/payu-mop/<name>.failed.yaml
rtk oc describe <kind> <name> -n <namespace> > /tmp/payu-mop/<name>.describe.txt
rtk oc get events -n <namespace> --sort-by=.lastTimestamp > /tmp/payu-mop/<namespace>.events.failed.txt
```

## Abort Criteria

Abort and roll back if any of these conditions occur:

| Condition | Threshold |
|:---|:---|
| Infrastructure CR readiness | `Ready=False`, `Ready=Unknown`, or `HasErrors=True` after retry window |
| Workload rollout | Any deployment unavailable after 10 minutes |
| Pod restarts | More than 2 restarts on a changed deployment during rollout |
| Error rate | More than 1 percent on gateway/APIcast after release |
| P95 latency | More than 500 ms sustained for core payment routes |
| Data integrity | Any ledger, outbox, or migration error |
| Events | New persistent Warning events tied to changed resources |

## Troubleshooting Quick Checks

| Symptom | First checks |
|:---|:---|
| CNPG not Ready | `rtk oc describe cluster.postgresql.cnpg.io payu-database -n payu-dev`; check PVC and storage class |
| Kafka not Ready | `rtk oc describe kafka payu-kafka -n payu-dev`; check AMQ Streams CSV and broker pods |
| Cache connection fails | Confirm clients use `payu-cache-resp.payu-dev.svc.cluster.local:11222` and `payu-cache-credentials` |
| AMQ STOMP disconnects | Confirm broker acceptor includes `STOMP` on `61616` and clients send heartbeats |
| Keycloak route works but CR unhealthy | Check `Keycloak` conditions and RHBK operator logs in `payu-sso` |
| 3scale pods missing | Confirm only operator shell was applied; APIManager is gated by external secrets |
| Workload ImagePullBackOff | Confirm internal registry route, ImageStreamTags, and image pull permissions |
| GitOps does nothing | Check `oc get application -n openshift-gitops`; ApplicationSet may not be installed yet |

## Current Known Gates

| Key | Gate | Status |
|:---|:---|:---|
| `INFRA-020` | Reconcile GitOps ApplicationSet with manually recovered `payu-dev` workloads | Open |
| `DEPLOY-010` | Deploy 3scale APIManager after external backing-store/Vault secrets exist | Open |
| `INFRA-021` | Clear RHBK `payu-keycloak` CR `HasErrors=True` service patch conflict | Open |
| `SEC-020` | Remediate CIS platform failures | Open |
