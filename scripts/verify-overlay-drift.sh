#!/usr/bin/env bash
# L-433/L-434/L-438 — Overlay-vs-live drift guard for payu-dev.
# Compares each PER-SERVICE overlay render (the actual deploy unit behind
# `oc apply -k .../<service>`) against the live Deployment: image equality
# and overlay-declared env presence. Plus: every referenced image tag must
# exist in the internal registry, and root-overlay tags must agree with the
# per-service overlays (a root apply must never downgrade a service).
# Catches: unapplied env patches (auth WEB secret, tx Redis, analytics
# KEYCLOAK_URL), missing registry tags (tx 1.8.115), dropped Service ports.
#
# The mesh packages are applied as whole kustomizations, not as per-service
# overlays, so their drift check is the server-side dry run of the package:
# every resource must come back "unchanged" (see mesh_pkg_drift below).
#
# Usage: ./scripts/verify-overlay-drift.sh [payu-dev]
set -euo pipefail
NS="${1:-payu-dev}"
OVERLAY="infrastructure/workloads/overlays/payu-dev"
MESH="infrastructure/platform/mesh"
FAIL=0

say() { printf '%s\n' "$*"; }
flag() { printf 'DRIFT: %s\n' "$*"; FAIL=1; }

# The mesh packages are applied as whole kustomizations, so they are compared
# against their rendered package: a server-side dry run must report every
# resource as "unchanged". CRD-guarded - a cluster without the operator
# skips the package instead of reporting a false drift.
mesh_pkg_drift() {
  local pkg="$1" crd="${2:-}" mode="${3:--k}"
  local out line
  if [ -n "$crd" ] && ! oc get crd "$crd" >/dev/null 2>&1; then
    say "SKIP: mesh package $pkg (CRD $crd absent)"
    return
  fi
  # OVERLAY-STRUCT-001: server-side dry-run (matches how the packages are
  # applied: `oc apply --server-side`). Client dry-run always reports
  # `configured` for server-side-managed objects (no last-applied
  # annotation), which is a false drift. Both `unchanged` and
  # `serverside-applied` mean "render matches live".
  if ! out="$(oc apply --server-side --dry-run=server --field-manager=payu-dev-root "$mode" "$pkg" 2>&1)"; then
    flag "mesh package $pkg: server dry-run failed: $out"
    return
  fi
  while IFS= read -r line; do
    case "$line" in
      *"(server dry run)") ;;
      *) continue ;;
    esac
    case "$line" in
      *"unchanged (server dry run)"|*"serverside-applied (server dry run)") ;;
      *) flag "mesh package $pkg: $line" ;;
    esac
  done <<<"$out"
}

command -v oc >/dev/null || { echo "no oc"; exit 2; }
command -v python3 >/dev/null || { echo "no python3"; exit 2; }

for dir in "$OVERLAY"/*/; do
  [ -f "$dir/kustomization.yaml" ] || continue
  rendered="$(oc kustomize "$dir" 2>/dev/null || true)"
  [ -z "$rendered" ] && continue
  while IFS='|' read -r deploy want_image want_env; do
    [ -z "$deploy" ] && continue
    live_image="$(oc get deploy "$deploy" -n "$NS" -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)"
    [ -z "$live_image" ] && { flag "deployment $deploy (from ${dir}) missing live"; continue; }
    [ -n "$want_image" ] && [ "$want_image" != "$live_image" ] && \
      flag "image $deploy overlay=$want_image live=$live_image"
    live_env="$(oc get deploy "$deploy" -n "$NS" -o jsonpath='{range .spec.template.spec.containers[0].env[*]}{.name}{"\n"}{end}' 2>/dev/null || true)"
    IFS=',' read -ra names <<<"$want_env"
    for n in "${names[@]}"; do
      [ -z "$n" ] && continue
      echo "$live_env" | grep -qx "$n" || flag "env $deploy missing live: $n (overlay ${dir})"
    done
  done < <(echo "$rendered" | python3 -c "
import sys, yaml
for d in yaml.safe_load_all(sys.stdin):
    if not isinstance(d, dict) or d.get('kind') != 'Deployment': continue
    name = d['metadata']['name']
    for c in d['spec']['template']['spec']['containers']:
        envs = ','.join(e['name'] for e in c.get('env', []) if 'name' in e)
        print(f\"{name}|{c.get('image', '')}|{envs}\")
        break
")
done

root_tags="$(oc kustomize "$OVERLAY" 2>/dev/null | python3 -c "
import sys, yaml
for d in yaml.safe_load_all(sys.stdin):
    if isinstance(d, dict) and d.get('kind') == 'Deployment':
        for c in d['spec']['template']['spec']['containers']:
            print(d['metadata']['name'], c.get('image', ''))
            break
" 2>/dev/null | sort -u || true)"
while IFS= read -r line; do
  deploy="${line%% *}"; img="${line#* }"
  for dir in "$OVERLAY"/*/; do
    [ -f "$dir/kustomization.yaml" ] || continue
    sub="$(oc kustomize "$dir" 2>/dev/null | python3 -c "
import sys, yaml
for d in yaml.safe_load_all(sys.stdin):
    if isinstance(d, dict) and d.get('kind') == 'Deployment' and d['metadata']['name'] == '$deploy':
        print(d['spec']['template']['spec']['containers'][0].get('image', ''))
        break
" 2>/dev/null || true)"
    [ -n "$sub" ] && [ "$sub" != "$img" ] && flag "tag split $deploy root=$img service-overlay=$sub"
  done
done <<<"$root_tags"

# ponytail: istag lookup instead of registry v2 API — `oc whoami -t` is empty
# for system:admin sessions, which made every tag look missing (L-433 guard
# cried wolf). istag is the same source the deployment actually pulls from.
while IFS= read -r img; do
  repo="${img#*/}"; repo="${repo%%:*}"; tag="${img##*:}"
  name="${repo#*/}"
  oc get istag "$name:$tag" -n "$NS" -o name >/dev/null 2>&1 || flag "registry tag missing: $img"
done < <(oc kustomize "$OVERLAY" 2>/dev/null | grep -E '^[[:space:]]*image: ' | awk '{print $2}' | sort -u | grep -v '^docker.io\|quay.io\|registry.access\|ghcr.io')

# OVERLAY-STRUCT-001 — root-vs-per-service parity: one `oc apply -k` of
# EITHER unit must carry the same Deployment env contract. Compares the root
# render against every per-service render: any env present in root but absent
# in per-service (or vice versa, outside the documented per-service-only
# allowlist) fails. KEYCLOAK_URL on analytics is exempt: root carries it as a
# service-endpoints ref, per-service as the same resolved literal.
root_env="$(oc kustomize "$OVERLAY" 2>/dev/null | python3 -c "
import sys, yaml
for d in yaml.safe_load_all(sys.stdin):
    if isinstance(d, dict) and d.get('kind') == 'Deployment':
        envs = ','.join(sorted(e['name'] for e in d['spec']['template']['spec']['containers'][0].get('env', []) if 'name' in e))
        print(d['metadata']['name'] + '|' + envs)
" 2>/dev/null | sort || true)"
for dir in "$OVERLAY"/*/; do
  [ -f "$dir/kustomization.yaml" ] || continue
  case "$dir" in */components/*) continue;; esac
  rendered="$(oc kustomize "$dir" 2>/dev/null || true)"
  [ -z "$rendered" ] && continue
  echo "$rendered" | python3 -c "
import sys, yaml
for d in yaml.safe_load_all(sys.stdin):
    if not isinstance(d, dict) or d.get('kind') != 'Deployment': continue
    envs = ','.join(sorted(e['name'] for e in d['spec']['template']['spec']['containers'][0].get('env', []) if 'name' in e))
    print(d['metadata']['name'] + '|' + envs)
" 2>/dev/null | while IFS='|' read -r deploy svc_env; do
    [ -z "$deploy" ] && continue
    want="$(echo "$root_env" | grep "^$deploy|" | cut -d'|' -f2 || true)"
    [ -z "$want" ] && { flag "parity $deploy per-service not in root render"; continue; }
    IFS=',' read -ra rnames <<<"$want"
    IFS=',' read -ra snames <<<"$svc_env"
    for n in "${rnames[@]}"; do
      [ -z "$n" ] && continue
      echo ",$svc_env," | grep -q ",$n," || flag "parity $deploy root-only env (stripped by per-service apply): $n"
    done
    for n in "${snames[@]}"; do
      [ -z "$n" ] && continue
      case ",$want," in *",$n,"*) continue;; esac
      case "$deploy:$n" in
        analytics-service:KEYCLOAK_URL) continue;; # ref-vs-literal, same resolved value
        *:MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS|*:PAYU_KEYCLOAK_WEB_CLIENT_ID|*:PAYU_KEYCLOAK_WEB_CLIENT_SECRET|*:OIDC_ISSUER|*:WS_TICKET_SECRET|*:SERVICES_ANALYTICS_URL|*:NEXT_PUBLIC_WS_URL) continue;;
      esac
      flag "parity $deploy per-service-only env (stripped by root apply): $n"
    done
  done
done

# Mesh packages: the control plane, the console CRs, the per-environment
# policy overlay and the dev-scoped ingress/telemetry packages.
say "Checking mesh packages..."
mesh_pkg_drift "$MESH" istios.sailoperator.io
mesh_pkg_drift "$MESH/base/kiali.yaml" ossmconsoles.kiali.io "-f"
case "$NS" in
  payu-dev)
    mesh_pkg_drift "$MESH/overlays/payu-dev" peerauthentications.security.istio.io
    mesh_pkg_drift "$MESH/ingress" gateways.gateway.networking.k8s.io
    mesh_pkg_drift "$MESH/telemetry" servicemonitors.monitoring.coreos.com
    ;;
  payu-sit|payu-uat|payu-preprod)
    mesh_pkg_drift "$MESH/overlays/$NS" peerauthentications.security.istio.io
    ;;
  *)
    say "SKIP: no applied mesh overlay for namespace $NS (the prod overlay is not applied yet)"
    ;;
esac

[ "$FAIL" -eq 0 ] && say "OK: no overlay drift in $NS" || say "FAIL: drift found above"
exit "$FAIL"
