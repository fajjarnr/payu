#!/usr/bin/env python3
"""
Sync base manifests to live OCP cluster state.

Per L-058: ALWAYS run this after `oc set image` to keep git in sync.
CI should also run this nightly and fail if drift detected.

Usage:
  ./scripts/sync-base-to-live.sh              # sync payu-dev
  NAMESPACE=payu-prod ./scripts/sync-base-to-live.sh
  ./scripts/sync-base-to-live.sh --dry-run    # show what would change

Exits 0 if no changes needed, 1 if changes were applied.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent / 'infrastructure' / 'workloads' / 'base'
OVERLAY = Path(__file__).resolve().parent.parent / 'infrastructure' / 'workloads' / 'overlays' / 'payu-dev'
NAMESPACE = 'payu-dev'
DRY_RUN = '--dry-run' in sys.argv


def sh(cmd: list[str], check=True) -> str:
    r = subprocess.run(cmd, capture_output=True, text=True)
    if check and r.returncode != 0:
        print(f"ERROR: {cmd} -> {r.stderr}", file=sys.stderr)
        sys.exit(2)
    return r.stdout.strip()


def get_live_deployments() -> dict[str, str]:
    raw = sh(['oc', '-n', NAMESPACE, 'get', 'deployments',
               '-o', 'jsonpath={range .items[*]}{.metadata.name}{\" \"}{.spec.template.spec.containers[0].image}{\"\\n\"}{end}'])
    out = {}
    for line in raw.splitlines():
        if not line.strip():
            continue
        parts = line.split(maxsplit=1)
        if len(parts) == 2:
            out[parts[0]] = parts[1]
    return out


def update_base_image(svc: str, dep_file: Path, new_image: str) -> bool:
    text = dep_file.read_text()
    new_text = re.sub(
        r'^( {8}image: ).*$',
        f'\\g<1>{new_image}',
        text,
        count=1,
        flags=re.MULTILINE,
    )
    if new_text == text:
        return False
    if DRY_RUN:
        print(f'  [DRY-RUN] {svc}: would update {dep_file.name} -> {new_image}')
    else:
        dep_file.write_text(new_text)
        print(f'  [BASE] {svc:30s} -> {new_image}')
    return True


def update_overlay_image(svc: str, old_image_full: str, alt_image_full: str,
                          new_image_full: str, registry: str, tag: str) -> bool:
    overlay_file = OVERLAY / 'kustomization.yaml'
    if not overlay_file.exists():
        return False
    text = overlay_file.read_text()
    name_pattern = rf'(^- name: )({re.escape(old_image_full)}|{re.escape(alt_image_full)})\n  newName: [^\n]+\n  newTag: "[^"]+"'
    repl = f'\\g<1>{new_image_full}\n  newName: {registry}\n  newTag: "{tag}"'
    new_text, n = re.subn(name_pattern, repl, text, flags=re.MULTILINE)
    if n == 0:
        return False
    if DRY_RUN:
        print(f'  [DRY-RUN] {svc:30s} overlay: would update {n} block')
    else:
        overlay_file.write_text(new_text)
        print(f'  [OVL]  {svc:30s} overlay: {n} block updated')
    return True


def main() -> int:
    sh(['oc', 'whoami'])
    live = get_live_deployments()

    # Skip operator-managed
    skip = {'payu-kafka-console-console-deployment',
            'payu-kafka-console-prometheus-deployment',
            'payu-kafka-entity-operator'}

    changes = 0
    for svc, image in sorted(live.items()):
        if svc in skip:
            continue
        # Skip non-payu services (e.g. operator deployments)
        if not any(svc.endswith(suf) for suf in ('-service', '-simulator')):
            continue
        # Find base file
        dep_file = BASE / svc / 'deployment.yaml'
        if not dep_file.exists():
            sim_file = BASE / f'{svc}.yaml'
            if sim_file.exists():
                dep_file = sim_file
        if dep_file.exists():
            if update_base_image(svc, dep_file, image):
                changes += 1
        # Also update overlay
        registry, _, tag_full = image.rpartition(':')
        # rpartition on the LAST ':' may include port. Split more carefully:
        last_colon = image.rfind(':')
        registry_part = image[:last_colon]
        tag_part = image[last_colon + 1:]
        # Extract just the registry/repo part (strip /payu-dev from registry if present)
        repo_part = registry_part.split('/payu-dev/')[-1] if '/payu-dev/' in registry_part else registry_part.split('/')[-1]
        svc_in_image = image.split('/')[-1].rsplit(':', 1)[0]
        old_internal = f'image-registry.openshift-image-registry.svc:5000/payu-dev/{svc_in_image}'
        alt_external = f'default-route-openshift-image-registry.apps.payu.ocp.fajjjar.my.id/payu-dev/{svc_in_image}'
        if update_overlay_image(svc, old_internal, alt_external, image, registry_part, tag_part):
            changes += 1

    print()
    if changes == 0:
        print('NO CHANGES NEEDED — base is in sync with live cluster')
        return 0
    elif DRY_RUN:
        print(f'{changes} changes would be made (use without --dry-run to apply)')
        return 1
    else:
        print(f'APPLIED {changes} updates. Commit + push with:')
        print('  git add infrastructure/workloads/base infrastructure/workloads/overlays')
        print('  git commit -m "fix(deploy): sync base manifests to live cluster"')
        return 0


if __name__ == '__main__':
    sys.exit(main())
