#!/usr/bin/env bash
# L-433/L-434/L-438 — Overlay-vs-live drift guard for payu-dev.
# Compares each PER-SERVICE overlay render (the actual deploy unit behind
# `oc apply -k .../<service>`) against the live Deployment: image equality
# and overlay-declared env presence. Plus: every referenced image tag must
# exist in the internal registry, and root-overlay tags must agree with the
# per-service overlays (a root apply must never downgrade a service).
# Catches: unapplied env patches (auth WEB secret, tx Redis, analytics
# KEYCLOAK_URL), missing registry tags (tx 1.8.115), dropped Service ports.
# Usage: ./scripts/verify-overlay-drift.sh [payu-dev]
set -euo pipefail
NS="${1:-payu-dev}"
OVERLAY="infrastructure/workloads/overlays/payu-dev"
FAIL=0

say() { printf '%s\n' "$*"; }
flag() { printf 'DRIFT: %s\n' "$*"; FAIL=1; }

command -v oc >/dev/null || { echo "no oc"; exit 2; }
command -v python3 >/dev/null || { echo "no python3"; exit 2; }

# [1] Per-service overlay render vs live Deployment (image + env names).
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

# [2] Root overlay tags must agree with per-service overlays (no downgrade on root apply).
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

# [3] Every referenced image tag exists in the internal registry.
REGISTRY="$(oc get route default-route -n openshift-image-registry -o jsonpath='{.spec.host}' 2>/dev/null || true)"
if [ -n "$REGISTRY" ]; then
  while IFS= read -r img; do
    repo="${img#*/}"; repo="${repo%%:*}"; tag="${img##*:}"
    ns="${repo%%/*}"; name="${repo#*/}"
    out="$(curl -sk -u "$(oc whoami | tr -d ':'):$(oc whoami -t)" \
      "https://$REGISTRY/v2/$ns/$name/tags/list" 2>/dev/null || true)"
    echo "$out" | grep -q "\"$tag\"" || flag "registry tag missing: $img"
  done < <(oc kustomize "$OVERLAY" 2>/dev/null | grep -E '^[[:space:]]*image: ' | awk '{print $2}' | sort -u | grep -v '^docker.io\|quay.io\|registry.access\|ghcr.io')
else
  say "registry route missing — skipping tag check"
fi

[ "$FAIL" -eq 0 ] && say "OK: no overlay drift in $NS" || say "FAIL: drift found above"
exit "$FAIL"
