# Method of Procedure (MOP) - PayU Shared Infrastructure Deployment

> Current operational runbook for deploying PayU infrastructure on Red Hat OpenShift.
> This replaces the obsolete Crunchy/Postgres example guide and uses the current
> Kustomize entrypoints in `infrastructure/`.

Environment-specific execution is split into dedicated runbooks. Use this file
for shared bootstrap, dependency order, common rollback rules, and troubleshooting;
use the matching environment runbook for promotion gates and acceptance criteria.

| Environment | Runbook |
|:---|:---|
| Development | [INFRASTRUCTURE_DEPLOYMENT_DEV.md](INFRASTRUCTURE_DEPLOYMENT_DEV.md) |
| SIT | [INFRASTRUCTURE_DEPLOYMENT_SIT.md](INFRASTRUCTURE_DEPLOYMENT_SIT.md) |
| UAT | [INFRASTRUCTURE_DEPLOYMENT_UAT.md](INFRASTRUCTURE_DEPLOYMENT_UAT.md) |
| Preprod | [INFRASTRUCTURE_DEPLOYMENT_PREPROD.md](INFRASTRUCTURE_DEPLOYMENT_PREPROD.md) |
| Production | [INFRASTRUCTURE_DEPLOYMENT_PROD.md](INFRASTRUCTURE_DEPLOYMENT_PROD.md) |

The environment runbooks are focused, not placeholders: each owns its
environment contract, preflight, gate evidence, abort criteria, and rollback.
Shared commands remain here; an environment gate must never be inferred from a
different environment.

## Document Control

| Field | Value |
|:---|:---|
| Scope | Infrastructure bootstrap, platform services, workload deployment, verification, rollback |
| Target platform | Red Hat OpenShift 4.20+ |
| Last verified cluster | OCP 4.22.7, Kubernetes v1.35.6, 8 Ready nodes (3 control-plane + 5 workers, 3 AZs) |
| Last verified date | 2026-08-06 |
| Primary namespaces | `payu-dev`, `payu-sit`, `payu-uat`, `payu-preprod`, `payu`, and `payu-cicd` |
| Change mode | GitOps ArgoCD ApplicationSets with automated sync, prune, and self-heal after bootstrap; manual `oc apply` only for approved operator/bootstrap resources |

### Document ownership and source precedence

1. `architecture/DEVSECOPS_ARCHITECTURE.md` is the architecture and gate
   contract.
2. This file is the shared bootstrap/dependency/rollback MOP.
3. The five environment files are the execution and evidence procedures.
4. Kustomize/Tekton/Argo manifests are the executable source of truth. If docs,
   rendered YAML, and live state disagree, stop and reconcile Git before a
   release. A green pod is never authority over Git.

## Architecture-to-runbook traceability

| Architecture contract | Operational owner |
|:---|:---|
| Namespace matrix and promotion order | Matching `INFRASTRUCTURE_DEPLOYMENT_<ENV>.md` |
| Source/build/image security | Dev runbook + Tekton PipelineRun/TaskRun evidence |
| ZAP, Schemathesis, k6 | SIT/UAT runbooks + deploy pipeline results |
| LitmusChaos | SIT runbook + `chaosengine`/`chaosresult` |
| Kraken/Cerberus | Preprod runbook + `kraken-run`/Cerberus evidence |
| Vault/VSO, mTLS, NetworkPolicy | Shared MOP + each environment preflight |
| Argo prune/self-heal and digest write-back | Shared MOP + SIT/UAT/preprod promotion |
| Rollouts, CAB/CISO, DR and production rollback | Production runbook; currently a release blocker |

## Critical Drift Notes

Do not use the old deployment path `infrastructure/openshift/examples/`; it is no longer the source of truth.

Current state differs from older docs:

| Area | Current source of truth | Notes |
|:---|:---|:---|
| Namespaces | `infrastructure/foundation/namespaces/` | Creates `payu-dev`, `payu-sit`, `payu-uat`, `payu-preprod`, `payu`, and `payu-cicd` |
| Operators | `infrastructure/foundation/cluster-operators/` | Installs CNPG, Data Grid, AMQ Streams, AMQ Broker, RHBK, 3scale, GitOps, Pipelines, Vault Secrets, Tempo, Compliance |
| Database | `infrastructure/platform/data/base/cnpg-*.yaml` | Base manifest location; always apply via `data/overlays/<env>` |
| Kafka | `infrastructure/platform/data/base/kafka-amqstreams.yaml` | Base manifest location; always apply via `data/overlays/<env>` |
| Cache | `infrastructure/platform/data/base/datagrid.yaml` + environment overlay | Infinispan `payu-cache` (operator-managed); dev uses plain Hot Rod/no endpoint auth, production uses mTLS; cache `payu` (RESP removed — ARCH-007) |
| AMQ Broker | `infrastructure/platform/amq-broker/base/` | `payu-broker` with CORE, AMQP, and STOMP on `61616` |
| Identity | `infrastructure/platform/identity/overlays/<env>/` | RHBK `payu-keycloak` in `payu-sso`; verify CR conditions before declaring healthy |
| API management | `infrastructure/platform/api-management/` | Operator/policy only by default; `APIManager` is gated until external backing-store and Vault secrets exist |
| Workloads | `infrastructure/workloads/overlays/<env>/` | Use environment overlays, not workload base directly |

### Current platform constraints

- The lab uses a single-cluster/single-zone operating profile for the gates
  that are actually run. Production requires explicit multi-zone scheduling,
  storage, backup, and failure evidence.
- Ceph storage classes are available in the lab, but every workload must use
  the class rendered by its overlay. Do not document “CephFS” as a universal
  replacement: the current CNPG/Kafka/AMQ manifests still contain explicit
  `gp3-csi` references while shared-file workloads may use CephFS. Production
  approval requires a dedicated storage-class review.
- `payu-dev` cache is plain Hot Rod. `payu-sit`/UAT/preprod/prod must use the
  security contract in their overlays; a dev cache exception must not leak.

## Execution Rules

1. Run every command through `rtk`.
2. Never commit or apply real secrets from Git. Create runtime secrets from Vault, External Secrets, or one-time `oc create secret` commands.
3. Render before apply. Every touched Kustomize root must pass `oc kustomize`.
   For an Argo-managed root, server dry-run may use
   `--field-manager=argocd-controller --force-conflicts` to validate the
   rendered schema without mutating live state; never use `--force-conflicts`
   on a real production apply.
4. Apply in dependency order: namespaces, operators, data/messaging, identity, API management shell, images, workloads.
5. Stop the deployment if any infrastructure CR reports `Ready=False`, `HasErrors=True`, CrashLoopBackOff, or recent Warning events.
6. Do not apply `3scale/apimanager.yaml` until external backing-store, storage, and Vault-managed secrets are available.
7. Automated GitOps sync (`syncPolicy.automated.prune: true` and `selfHeal: true`) is enabled on the environment/platform/identity ApplicationSets. Production still requires its Argo sync window and approval gates; do not bypass them with direct `oc apply`.
8. ArgoCD Application objects are resolved with the explicit group `applications.argoproj.io` — `oc get application` resolves to the `app.k8s.io` shadow CRD (L-171).
9. LitmusChaos execution plane: `infrastructure/platform/security/litmus/` (operator bundle) + `infrastructure/platform/security/chaos/litmus/` (SIT overlay: RBAC, experiments, ChaosEngine, NetworkPolicy). Images are digest-pinned via `mirror.gcr.io`; the registry must stay in `image.config.openshift.io/cluster` allowedRegistries.
10. SIT exposes a gateway Route (`gateway-sit.apps.fajjjar.my.id`, edge TLS) for DAST/fuzzing/E2E; OpenShift Route port uses `spec.port.targetPort` at top level (not nested under `to`, and no `name` field — L-172/173 lessons).
11. HPA is production-only. The `payu-dev` overlay removes `HorizontalPodAutoscaler` resources; verify with `rtk oc get hpa -n payu-dev`. Production keeps the base HPA manifests and must be capacity-tested before promotion.

## Promotion Pipeline (SIT → UAT → preprod → prod)

`payu-deploy-gitops-pipeline` (Tekton, `payu-cicd`) is the sole promotion path:

```bash
tkn pipeline start payu-deploy-gitops-pipeline -n payu-cicd \
  -w name=source,claimName=tekton-workspace-pvc \
  -p service-name=<service> -p environment=<sit|uat|preprod|prod> \
  -p image-digest=<sha256-digest> -p push-changes=true \
  -p environment-base-url=<env-url> -p schema-url=<gateway>/q/openapi \
  --use-param-defaults
```

Gate sequence per environment (snapshot 2026-08-06):

| Environment | Gates | Status |
|:---|:---|:---|
| SIT | Argo sync-wait → ZAP baseline → Schemathesis → LitmusChaos → k6 smoke | ✅ green (pilot SUCCEEDED) |
| UAT | Argo sync-wait → Schemathesis → k6 smoke/load | validation pipeline completed; acceptance blocked by Vault KV/VSO + cache mTLS |
| PREPROD | Argo sync-wait → Kraken/Cerberus chaos | pending evidence run |
| PROD | Argo sync-wait → Argo Rollouts analysis | blocked: Rollouts/approval/storage/DR gates |

Prereqs for UAT/preprod/prod runs: healthy VaultStaticSecret per env, registry
`newName` entries, digest pinning (`image-digest`), and a real gate evidence
bundle. Presence of a VSS object is not enough; its Ready/Sync condition and
consumer pod behavior must be verified.

UAT evidence snapshot (`account-service-deploy-uat-rjj9s`, 2026-08-06): the
non-mutating validation run completed with Argo sync, ZAP, k6 smoke, and
Schemathesis (`44 passed`, `3809/3809` checks). It is not an acceptance result:
the required UAT `VaultStaticSecret` objects still report `SecretSynced=False`,
Vault KV paths under `secret/payu/uat/...` are absent, and gateway logs show
Hot Rod `certificate_unknown` against `payu-cache`. The cache client Secret must
be reseeded through the approved Vault procedure, then affected workloads must
be restarted and the UAT runtime gate rerun. Do not copy live credential values
into Git or create placeholder Secrets.

## Environment Map

| Environment | Namespace | Overlay |
|:---|:---|:---|
| Development | `payu-dev` | `infrastructure/workloads/overlays/payu-dev` |
| SIT | `payu-sit` | `infrastructure/workloads/overlays/payu-sit` |
| UAT | `payu-uat` | `infrastructure/workloads/overlays/payu-uat` |
| Preprod | `payu-preprod` | `infrastructure/workloads/overlays/payu-preprod` |
| Production | `payu` | `infrastructure/workloads/overlays/payu-prod` |

## Local Podman Development

`infrastructure/local/podman/podman-compose.yml` mirrors the `payu-dev` runtime contract for local work: single-node infrastructure, Kafka's local `29092` listener, and plain Infinispan Hot Rod/REST on `payu-cache:11222`. It intentionally does not copy production mTLS files or credentials.

```bash
rtk podman compose -f infrastructure/local/podman/podman-compose.yml up -d payu-database-rw payu-cache payu-kafka-kafka-bootstrap artemis payu-keycloak-service
rtk podman compose -f infrastructure/local/podman/podman-compose.yml --profile apps up -d
```

For day-to-day development, use
`infrastructure/local/podman/containers/manage-podman.sh core|apps|all|smoke`.
The canonical local guide and host endpoint map are in
[`infrastructure/local/podman/README.md`](../../infrastructure/local/podman/README.md).

The cache config uses the distributed `payu` cache with non-XA pessimistic transactions. If the host has no Compose provider, install `podman-compose` or Docker Compose before running these commands; YAML/XML parsing alone does not start services.

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
rtk oc kustomize infrastructure/platform/data/overlays/dev >/tmp/payu-mop/data.yaml
rtk oc kustomize infrastructure/platform/messaging/overlays/payu-dev >/tmp/payu-mop/messaging.yaml
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
rtk oc apply -k infrastructure/platform/data/overlays/dev -n payu-dev
rtk oc apply -k infrastructure/platform/messaging/overlays/payu-dev -n payu-dev
```

Verify:

```bash
rtk oc get cluster.postgresql.cnpg.io -n payu-dev
rtk oc get kafka -n payu-dev
rtk oc get kafkatopic -n payu-dev
rtk oc get infinispan -n payu-dev
rtk oc get pods -n payu-dev | rtk rg 'payu-database|payu-kafka|payu-cache'
rtk oc get events -n payu-dev --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -40
```

Expected result:

- CNPG `payu-database` has 3 instances Ready.
- Kafka `payu-kafka` reports `READY=True`.
- Declared application topics, including each required `.dlq`, report `Ready=True`; do not rely on Kafka auto-create for outbox destinations.
- Infinispan `payu-cache` pods are Running (`WellFormed=True`) using the custom XML configuration ConfigMap (`payu-cache-custom-config`) with the `payu` cache (text/plain) over the native Hot Rod endpoint.
- `payu-cache` service exposes Hot Rod port `11222`; dev backend workloads use plain `payu-cache:11222` with endpoint authentication disabled, while production workloads use the mTLS contract (RESP/RESP-compat services are removed — ARCH-007).
- Validate the installed CRD schema before changing Data Grid fields: `rtk oc explain infinispan.spec.security --recursive`, `rtk oc explain infinispan.spec.logging --recursive`, and `rtk oc explain infinispan.spec.container --recursive`.

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
rtk oc apply -k infrastructure/platform/identity/overlays/dev
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

Current dev gate as of 2026-08-04: `payu-keycloak` reports `Ready=True`, `HasErrors=False`; keep the condition check in every environment.

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

For the development cluster, deploy the runtime after ODF CephFS, shared
ingress, external PostgreSQL/Redis, and their connection secrets are ready.
External system PostgreSQL, system Redis, and backend Redis are mandatory
since 3scale 2.16:

```bash
rtk oc get storageclass ocs-storagecluster-cephfs
rtk oc wait --for=condition=Ready clustersecretstore/payu-vault --timeout=2m
rtk oc apply -k infrastructure/platform/api-management/3scale
rtk oc wait --for=condition=Ready externalsecret --all \
  -n payu-api-management --timeout=3m
rtk oc wait --for=condition=Available apimanager/payu-apimanager \
  -n payu-api-management --timeout=30m
rtk oc get pods,pvc,route -n payu-api-management
```

`system-seed.MASTER_DOMAIN` must be the `master` prefix, not an FQDN; 3scale
appends `spec.wildcardDomain`.

Production remains gated on dedicated external PostgreSQL/Redis and
Vault-managed secrets. Never reuse application databases or commit provider
tokens.

### 8. Deploy Shared Ingress & Cert-Manager TLS (`*.apps.fajjjar.my.id`)

All application workloads and SSO routes use base domain `*.apps.fajjjar.my.id` (e.g. `web-app.apps.fajjjar.my.id`, `sso.apps.fajjjar.my.id`).

1. **Shared IngressController**:
   - Location: `infrastructure/foundation/cluster-config/ingress/shared.yaml`
   - Target domain: `apps.fajjjar.my.id`
   - Default certificate secret: `shared-ingress-cert`

2. **AWS Route 53 Alias A-Record Configuration**:
   - Hosted Zone: `apps.fajjjar.my.id.` (`hostedZoneID: Z04089013J3OEZ617CSS4`)
   - Target NLB: `router-shared-ingress` LoadBalancer DNS (`HostedZoneId: Z26RNL4JYFTOTI`)
   - Records: Apex `apps.fajjjar.my.id` and Wildcard `*.apps.fajjjar.my.id` Alias A records mapped to `router-shared-ingress` NLB.

Apply shared IngressController and Cert-Manager TLS configuration:

```bash
# Apply shared IngressController
rtk oc apply -f infrastructure/foundation/cluster-config/ingress/shared.yaml

# Apply Cert-Manager production ClusterIssuer and Wildcard Certificate
rtk oc apply -k infrastructure/platform/security/cert-manager/

# Create Route 53 Wildcard and Apex Alias A-Records targeting shared Ingress NLB
aws route53 change-resource-record-sets --hosted-zone-id Z04089013J3OEZ617CSS4 --change-batch '{
  "Comment": "Add wildcard and apex alias A records for apps.fajjjar.my.id to shared ingress NLB",
  "Changes": [
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "*.apps.fajjjar.my.id.",
        "Type": "A",
        "AliasTarget": {
          "HostedZoneId": "Z26RNL4JYFTOTI",
          "DNSName": "a538a4695ab88463cb8376bb9891fea5-d3ccc74e421a7cc5.elb.us-east-1.amazonaws.com.",
          "EvaluateTargetHealth": false
        }
      }
    },
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "apps.fajjjar.my.id.",
        "Type": "A",
        "AliasTarget": {
          "HostedZoneId": "Z26RNL4JYFTOTI",
          "DNSName": "a538a4695ab88463cb8376bb9891fea5-d3ccc74e421a7cc5.elb.us-east-1.amazonaws.com.",
          "EvaluateTargetHealth": false
        }
      }
    }
  ]
}'
```

Verify IngressController, Cert-Manager, and Route 53 status:

```bash
rtk oc get crd ingresscontrollers.operator.openshift.io
rtk oc get ingresscontroller shared-ingress -n openshift-ingress-operator
rtk oc get clusterissuer letsencrypt-prod-issuer
rtk oc get certificate shared-ingress-cert -n openshift-ingress
aws route53 list-resource-record-sets --hosted-zone-id Z04089013J3OEZ617CSS4
```

Expected result:

- `ingresscontroller/shared-ingress` created in `openshift-ingress-operator` with 3 router replicas.
- `clusterissuer/letsencrypt-prod-issuer` reports `READY=True`.
- `certificate/shared-ingress-cert` issued in `openshift-ingress` for `apps.fajjjar.my.id` and `*.apps.fajjjar.my.id`.
- Route 53 `*.apps.fajjjar.my.id` and `apps.fajjjar.my.id` Alias A records target `router-shared-ingress` NLB.

### 9. Build and Push Images

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

### 10. Deploy Workloads

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

- 33/33 deployments Ready.
- 47/47 pods Running/Ready.
- No Warning events introduced after the preflight baseline; historical Warning events alone are not a rollout failure.
- Logs for changed services contain no `error`, `warn`, `exception`, `traceback`, `failed`, or `unavailable` matches after startup stabilization.

### 11. GitOps Handoff

Current GitOps state:

- OpenShift GitOps operator is installed.
- The live ApplicationSets generate `payu-dev`, `payu-sit`, `payu-uat`,
  `payu-preprod`, `payu-prod` plus `data-*`, `messaging-*`, and `identity-*`.
- Query the explicit `applications.argoproj.io` group; the unqualified
  `application` shortcut can resolve to a shadow CRD.
- Current evidence is dev/SIT green; UAT/preprod/prod remain release gates,
  not completion claims.

Inspect generated Applications and drift before any approved bootstrap/change:

```bash
rtk oc get applications.argoproj.io -n openshift-gitops
rtk oc get applicationsets.argoproj.io -n openshift-gitops
rtk oc diff -k infrastructure/workloads/overlays/payu-dev
```

When ready, apply the GitOps app-of-apps:

```bash
rtk oc apply -k infrastructure/platform/cicd/argocd
rtk oc get applications.argoproj.io -n openshift-gitops
rtk oc get applicationsets.argoproj.io -n openshift-gitops
```

Do not disable automated `prune`/`selfHeal` to hide drift. Production sync also
depends on the AppProject window and approval policy.

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

For workload deployment failure, prefer the promotion/rollback pipeline or a
Git revert:

```bash
rtk git revert <bad_commit_sha>
rtk oc get applications.argoproj.io payu-dev -n openshift-gitops
rtk oc rollout status deployment/<service> -n payu-dev --timeout=10m
rtk oc logs -n payu-dev deployment/<service> --since=10m --all-containers=true
```

For an immutable image rollback, use `payu-rollback-pipeline` with the
previous digest. Direct `oc rollout undo` may be used only as an emergency
diagnostic action and must be followed by the authoritative Git/pipeline
rollback.

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

## Redeploy-Safe Hardening (2026-08-01, L-188..L-192)

Destroy + redeploy ulang cluster **tidak boleh** mengulang error berikut (root cause sudah di-fix di manifest):

| Error lama | Root cause | Fix di manifest |
|:---|:---|:---|
| VSO CrashLoopBackOff `dial tcp 172.30.0.1:443: i/o timeout` (OPS-2026-08-01-03) | kyverno `generate-default-deny-networkpolicy` membuat `default-deny-all` di ns `vault-secrets-operator`; OVN-K tidak enforce rule egress rinci | `networkpolicy-vso-egress.yaml` egress `- {}` (allow-all), ingress tetap deny |
| vector→Loki DNS lookup timeout (OPS-2026-08-01-04) | sama, di `openshift-logging` | `cluster-logging.yaml` `allow-logging-platform-egress` egress `- {}` |
| `kraken.report` Read-only (OPS-2026-08-01-05) | CRI-O mount rootfs image `ro` walau `readOnlyRootFilesystem: false` | `chaos/kraken/runtime.yaml`: emptyDir `/home/krkn/kraken` + `/tmp` (init `fixperms` + container `kraken`) |
| ArgoCD sync `field is immutable` pada Job | Job spec immutable antar deploy | anotasi `argocd.argoproj.io/sync-options: Replace=true` di `outbox-bootstrap-job.yaml` + `post-deploy-db-grants.yaml` |
| bootstrap Job "Running 0/1" >3h | `psql` tanpa timeout + DB tak terjangkau | `PGCONNECT_TIMEOUT=10` + `-w` + per-DB skip (L-191) |

**Pola egress**: namespace platform/operator (`openshift-logging`, `vault-secrets-operator`, `payu-drill`) pakai NP dengan egress `- {}` + ingress zero-trust; namespace aplikasi PayU pakai pola `payu-dev` (allow-all egress). Jangan kembalikan ke rule egress rinci tanpa bukti `getent`/`curl` nyata (L-188).

### Vault DR drill (INFRA-026)

MOP ringkas setelah manifest `infrastructure/platform/security/vault/promotion/dr-drill.yaml` di-apply:

1. `vault operator init -recovery-shares=1 -recovery-threshold=1` — **jangan** `-key-shares` (awskms auto-unseal → 400, L-189). Simpan output (`> /tmp/init.out`) utk root token sementara.
2. `aws s3 cp s3://payu-vault-snapshots-390403884108-cluster-9xtfg/raft/<snapshot>.snap /tmp/drill.snap` (host) → `oc cp` ke pod.
3. `vault operator raft snapshot restore -force /tmp/drill.snap` (pakai token dari init.out).
4. Verifikasi: `vault kv list secret/payu` via kubernetes login (`role=vault-admin`) — pod drill harus punya `system:auth-delegator` utk TokenReview (L-192). Catatan: snapshot yang diambil sebelum perubahan kubernetes auth config akan menolak JWT saat ini → ambil snapshot FRESH pasca-migrasi sebelum drill.

## Troubleshooting Quick Checks

| Symptom | First checks |
|:---|:---|
| CNPG not Ready | `rtk oc describe cluster.postgresql.cnpg.io payu-database -n payu-dev`; check PVC and storage class |
| Kafka not Ready | `rtk oc describe kafka payu-kafka -n payu-dev`; check AMQ Streams CSV and broker pods |
| Cache connection fails | Check the environment contract: dev uses plain `payu-cache.payu-dev.svc.cluster.local:11222` with no endpoint auth; production uses mTLS and `payu-cache-credentials` (L-148). |
| AMQ STOMP disconnects | Confirm broker acceptor includes `STOMP` on `61616` and clients send heartbeats |
| Keycloak route works but CR unhealthy | Check `Keycloak` conditions and RHBK operator logs in `payu-sso` |
| 3scale pods missing | Confirm only operator shell was applied; APIManager is gated by external secrets |
| Workload ImagePullBackOff | Confirm internal registry route, ImageStreamTags, and image pull permissions |
| GitOps does nothing | Check `oc get applications.argoproj.io -n openshift-gitops` and `oc get applicationsets.argoproj.io -n openshift-gitops`; then inspect controller logs |
| VSO CrashLoopBackOff / egress timeout | Cek NP `allow-vso-platform-egress` egress `- {}` (jangan rule rinci) + `oc get pods -n vault-secrets-operator` |
| Collector DNS lookup timeout | Cek `allow-logging-platform-egress` egress `- {}`; verifikasi `oc exec -n openshift-logging <collector> -- getent hosts loki-gateway-http.openshift-logging.svc` |
| Vector `403 Forbidden` ke loki gateway | Cek `oc get cm loki-gateway -n openshift-logging -o jsonpath='{.binaryData}'` — `lokistack-gateway.rego`/`rbac.yaml` kosong = bug loki-operator 6.5.1 (L-193); SAR `logcollector` harus `allowed`; butuh RH support/upgrade |
| Kraken job `kraken.report` error | Verifikasi emptyDir `/home/krkn/kraken` + `/tmp` ter-mount di pod (L-188) |

## Current Known Gates

| Key | Gate | Status |
|:---|:---|:---|
| `DEPLOY-010` | Deploy 3scale APIManager after external backing-store/Vault secrets exist | Open |
| `INFRA-021` | Clear RHBK `payu-keycloak` CR `HasErrors=True` service patch conflict | ✅ Closed 2026-08-04; live `Ready=True`, `HasErrors=False` |
| `SEC-020` | Remediate CIS platform failures | Open |
| `PROMO-2026-08` | Complete UAT final rerun and preprod Kraken evidence | Open |
| `PROD-READINESS` | Argo Rollouts, production storage, Vault HA/DR, approvals, and signed-image admission evidence | Open |
