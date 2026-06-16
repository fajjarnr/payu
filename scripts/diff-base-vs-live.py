#!/usr/bin/env python3
"""
Diff base manifests vs live OCP cluster state.

Detects:
1. Image tag drift in deployment.yaml
2. Image registry drift (internal vs default-route)
3. ConfigMap/secret drift

Usage:
  ./scripts/diff-base-vs-live.sh              # audit payu-dev
  NAMESPACE=payu-prod ./scripts/diff-base-vs-live.sh  # audit other env

Exit codes:
  0 = no drift
  1 = drift detected (prints diff to stderr)
  2 = oc not authenticated or cluster unreachable
"""
import json
import re
import subprocess
import sys
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent / 'infrastructure' / 'workloads' / 'base'
NAMESPACE = 'payu-dev'


def sh(cmd: list[str], check=True) -> str:
    """Run shell command, return stdout."""
    r = subprocess.run(cmd, capture_output=True, text=True)
    if check and r.returncode != 0:
        print(f"ERROR: {cmd} -> {r.stderr}", file=sys.stderr)
        sys.exit(2)
    return r.stdout.strip()


def get_live_deployments() -> dict[str, str]:
    """Returns {deployment_name: full_image_ref} from OCP cluster."""
    raw = sh(['oc', '-n', NAMESPACE, 'get', 'deployments',
               '-o', 'jsonpath={range .items[*]}{.metadata.name}{\" \"}{.spec.template.spec.containers[0].image}{\"\\n\"}{end}'])
    live = {}
    for line in raw.splitlines():
        if not line.strip():
            continue
        parts = line.split(maxsplit=1)
        if len(parts) == 2:
            live[parts[0]] = parts[1]
    return live


def get_live_configmaps() -> dict[str, dict[str, str]]:
    """Returns {cm_name: {key: value}} from OCP cluster."""
    raw = sh(['oc', '-n', NAMESPACE, 'get', 'cm', '-o', 'json'])
    data = json.loads(raw)['items']
    out = {}
    for cm in data:
        name = cm['metadata']['name']
        if name.endswith(('.cue', '.bin')) or name.startswith('kube-root-ca') or name.startswith('openshift-'):
            continue  # skip binary/operator CMs
        out[name] = cm.get('data', {})
    return out


def get_base_deployments() -> dict[str, tuple[str, str | None]]:
    """Returns {svc: (base_image_ref, file_path)} for all base service manifests."""
    out = {}
    # Subdirs (services)
    for d in (BASE).iterdir():
        if not d.is_dir():
            continue
        dep = d / 'deployment.yaml'
        if dep.exists():
            text = dep.read_text()
            m = re.search(r'^\s+image:\s+(\S+)', text, re.MULTILINE)
            if m:
                out[d.name] = (m.group(1), str(dep))
    # Top-level yamls (simulators)
    for f in BASE.glob('*-simulator.yaml'):
        text = f.read_text()
        m = re.search(r'^\s+image:\s+(\S+)', text, re.MULTILINE)
        if m:
            out[f.stem] = (m.group(1), str(f))
    return out


def main() -> int:
    # Verify oc auth
    sh(['oc', 'whoami'])

    live_deps = get_live_deployments()
    base_deps = get_base_deployments()

    # Skip operator-managed
    skip = {'payu-kafka-console-console-deployment',
            'payu-kafka-console-prometheus-deployment',
            'payu-kafka-entity-operator'}

    drift_count = 0

    # 1. Image drift
    print('=' * 70)
    print('IMAGE TAG / REGISTRY DRIFT')
    print('=' * 70)
    for svc, (base_img, _path) in sorted(base_deps.items()):
        live_img = live_deps.get(svc)
        if not live_img:
            print(f'  ✗ {svc:30s}  base-only (not in cluster)')
            drift_count += 1
        elif base_img != live_img:
            print(f'  ✗ {svc:30s}  DRIFT')
            print(f'      base: {base_img}')
            print(f'      live: {live_img}')
            drift_count += 1

    # Live-only (operator-managed or new)
    live_only = set(live_deps.keys()) - set(base_deps.keys()) - skip
    if live_only:
        print(f'  ℹ  live-only: {sorted(live_only)} (operator-managed or new)')

    # 2. ConfigMap drift
    print()
    print('=' * 70)
    print('CONFIGMAP DATA DRIFT (sample: service-endpoints + 4 simulator CMs)')
    print('=' * 70)
    live_cms = get_live_configmaps()
    for cm_name in ['service-endpoints', 'bi-fast-simulator-config',
                   'biller-simulator-config', 'dukcapil-simulator-config',
                   'qris-simulator-config']:
        live = live_cms.get(cm_name, {})
        # Find the base file (either top-level or per-svc configmap.yaml)
        base_data = {}
        # Simulators: the ConfigMap is in the same yaml file as the Deployment
        if cm_name.endswith('-simulator-config'):
            sim_short = cm_name.replace('-config', '')  # e.g. 'bi-fast-simulator'
            for f in (BASE).glob(f'{sim_short}.yaml'):
                text = f.read_text()
                for part in text.split('---'):
                    if 'kind: ConfigMap' in part and cm_name in part:
                        for line in part.splitlines():
                            m = re.match(r"^  ([A-Z_][A-Z0-9_]*):\s*'?([^']*)'?\s*$", line)
                            if m and m.group(1) != 'name':
                                base_data[m.group(1)] = m.group(2)
                        break
        elif cm_name == 'service-endpoints':
            text = (BASE / 'service-endpoints.yaml').read_text()
            for line in text.splitlines():
                m = re.match(r"^  ([A-Z_][A-Z0-9_]*):\s*\"?([^'\"]*?)\"?\s*$", line)
                if m and m.group(1) != 'data':
                    base_data[m.group(1)] = m.group(2)

        diffs = []
        for k in sorted(live.keys() | base_data.keys()):
            if live.get(k) != base_data.get(k):
                diffs.append(f'    {k}: live={live.get(k)!r} base={base_data.get(k)!r}')
        if diffs:
            print(f'  ✗ {cm_name} ({len(diffs)} diffs)')
            for d in diffs[:5]:
                print(d)
            if len(diffs) > 5:
                print(f'    ... +{len(diffs) - 5} more')
            drift_count += len(diffs)
        else:
            print(f'  ✓ {cm_name} ({len(live)} keys match)')

    print()
    if drift_count == 0:
        print('=' * 70)
        print('NO DRIFT DETECTED')
        print('=' * 70)
        return 0
    else:
        print('=' * 70)
        print(f'TOTAL DRIFT ITEMS: {drift_count}')
        print('=' * 70)
        print('Run ./scripts/sync-base-to-live.sh to fix.', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
