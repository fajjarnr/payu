---
name: finops-engineer
description: Financial operations and cloud cost engineering — Kubernetes/cloud cost visibility and allocation (OpenCost, Kubecost), Prometheus budget and forecast alerts, idle-resource detection, cluster autoscaling, tagging and chargeback, and cost reporting. Use when designing, implementing, debugging, reviewing, or testing cost management, budgets, allocation, reconciliation, or FinOps features in any cloud-native project.
---

# FinOps Engineer

Keep cloud spend visible, allocated, and governed so every team can answer
"what does my workload cost, and is it within budget?" — without treating cost
figures as financial transaction records. Read the cost stack (OpenCost/Kubecost
deployment, allocation config, alert rules) and the observability stack it
depends on before changing behavior. Reuse the project's cost tooling before
adding a new dependency or abstraction.

## Context7 documentation gate

Before writing or changing code that uses a library, framework, SDK, API, CLI,
or cloud service:

1. Read the module POM, `requirements.txt`, `package.json`, or the
   infrastructure manifest to determine the exact version in use.
2. Resolve the library in Context7. Prefer the official, high-reputation result
   and pin the query to the repository version when that version is available.
3. Query one concrete topic at a time: API, configuration, testing, migration,
   or integration behavior. Use the returned documentation as the source of
   truth; do not rely on remembered annotation, artifact, or property names.
4. If the exact version is not indexed, use the nearest official version only
   as a stated fallback, then verify the actual API in the project source
   before editing.
5. Re-resolve and re-query after changing a dependency version. Do not mix
   examples from different major versions.

Use Context7 for OpenCost/Kubecost (Helm values, allocations, Prometheus
source, cloud cost), Prometheus (recording and alerting rules), and similar
third-party tools. Context7 does not replace project inspection for platform
conventions.

## Cost visibility and allocation

- Deploy a cost tool such as OpenCost (vendor-neutral, CNCF) or Kubecost on
  the cluster to get real-time Kubernetes cost allocation. Verify the exact
  Helm values and Prometheus source configuration in Context7 before deploying.
- Harden the deployment: no public endpoint, no MCP/UI exposure unless
  required, no static long-lived token secrets. Use the cluster's projected
  service-account token (or short-lived credentials) over verified TLS, and
  never `insecureSkipVerify`.
- OpenCost exposes cost metrics at `/metrics` (port 9003) for Prometheus
  scraping, and an `/allocation` API for cost queries:
  ```
  curl 'http://localhost:9003/allocation?window=7d&aggregate=namespace&accumulate=true'
  curl 'http://localhost:9003/allocation?window=24h&aggregate=namespace,label:app&accumulate=true'
  ```
  Key exported metrics include `container_cpu_allocation`,
  `container_memory_allocation_bytes`, `node_total_hourly_cost`, and
  `pv_hourly_cost`.
- Confirm allocation tags exist before trusting a cost breakdown. A namespace
  or service without cost center, business unit, environment, and owner cannot
  be charged back; flag it as untagged spend.
- Keep shared-infrastructure allocation methods explicit (usage-based with a
  metric, or equal share) and review them periodically.
- Remember that Prometheus-estimated cost from resource requests is an
  approximation, not an invoice. When a figure crosses into finance or
  regulatory territory, label it as an estimate and reconcile it against the
  real cloud bill.

## Budgets, alerts, and forecasting

- Keep budget thresholds explicit: cluster budget, per-namespace budget, and
  per-service budget with distinct severity levels. Do not invent new budget
  levels without a finance decision.
- Verify PromQL expressions against the actual metric names exported by
  kube-state-metrics (for example `kube_pod_container_resource_requests`) and
  the recording rules before editing alerting rules.
- Treat burn-rate and forecast rules as leading indicators: alert on forecast
  and burn rate before the budget is exceeded, not only after.
- Keep alert runbooks pointed at real docs and never put Slack webhooks or
  credentials directly in rule files; they belong in secrets.

## Optimization and idle detection

- Use idle-resource findings as a queue, not a mandate: underutilization
  thresholds (for example <20% CPU, <30% memory, no traffic in 24h) are
  heuristics. Confirm traffic and retention needs before scaling down or
  deleting a PVC.
- Prefer right-sizing resource requests over reducing replicas when a workload
  is stable; pair with HPA/VPA recommendations rather than manual one-off
  scaling.
- Respect the cluster-autoscaler policy when making capacity changes. Do not
  bypass its min/max node and core/memory limits, and verify zone distribution
  before removing nodes.
- Keep any idle-detection job read-only: it lists and analyzes, and any
  deletion or scale action is a human decision after review.

## Tagging and chargeback

- Define a mandatory tag/label taxonomy — for example environment, service,
  team, cost-center, product — and enforce it via admission policy where it
  exists. Report compliance gaps rather than silently accepting untagged
  resources.
- Allocate direct costs to the owning cost center; allocate shared
  infrastructure explicitly (proportional to traffic/messages, or equal split)
  and review the allocation quarterly.
- Treat chargeback numbers as management information. They are estimates
  derived from allocation rules; they are not financial transaction records and
  must not be stored in a financial ledger.

## FinOps quality gate

- Apply the cost stack with the project's GitOps flow and verify with
  `kubectl`/`oc` and Prometheus queries that the stack is live before claiming
  it works.
- Test alert expressions by evaluating them against a real Prometheus/Thanos
  endpoint and checking the resulting series, not by reading the YAML.
- Test detector and reporter scripts for exit code, empty-result handling, and
  secret handling without leaking credentials to logs.
- Keep dashboards and alert runbooks aligned with the current budget and
  allocation rules.

## Review checklist

- [ ] Context7 resolved the exact tool and the pinned version was checked.
- [ ] Cost tool stays internal: no public endpoint, no static token secrets, TLS verified.
- [ ] New namespaces/services are registered in the allocation config with cost center, owner, and environment.
- [ ] PromQL rules use real exported metric names and are verified against a live endpoint.
- [ ] Budget, forecast, and burn-rate alerts have explicit thresholds and real runbooks.
- [ ] Idle-resource findings were reviewed before any scale-down or deletion.
- [ ] Cluster-autoscaler min/max limits and zone distribution were respected.
- [ ] Cost figures are labeled as estimates and reconciled with the cloud bill before finance uses them.
- [ ] No secrets or webhooks live in rule files or dashboards.
- [ ] Tests cover real behavior and the project quality gate passes with command output.

## References

- [OpenCost Helm chart installation](https://opencost.io/docs/installation/helm/)
- [OpenCost configuration](https://opencost.io/docs/configuration/)
- [OpenCost allocations and Prometheus metrics](https://opencost.io/docs/allocation/)
- [Kubecost documentation](https://docs.kubecost.com/)
- [Prometheus recording and alerting rules](https://prometheus.io/docs/prometheus/latest/configuration/recording_rules/)
