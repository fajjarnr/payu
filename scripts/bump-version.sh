#!/usr/bin/env bash
set -euo pipefail
# bump-version.sh — single source VERSION → propagate to 387 infra refs
# ponytail: one command replaces 30× podman-compose + 160× kustomize + 31× pipeline manual sed
# Usage: ./scripts/bump-version.sh 1.18.75  (or no arg = read VERSION file)
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NEW="${1:-$(cat "$ROOT/VERSION" | tr -d ' \n')}"
OLD="$(cat "$ROOT/VERSION" 2>/dev/null | tr -d ' \n' || echo "")"
if [[ -z "$NEW" ]]; then echo "Usage: $0 <new-version>  e.g. 1.18.75"; exit 1; fi
if [[ ! "$NEW" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-z0-9.-]+)?$ ]]; then echo "invalid version $NEW"; exit 1; fi
echo "Bump $OLD → $NEW"
# helper: replace only version in image tags / PAYU_VERSION defaults / pipeline image-tag / kustomize newTag
# scope to specific patterns to avoid touching axios 1.18.1 etc
find "$ROOT/infrastructure" -type f \( -name "*.yaml" -o -name "*.yml" \) -exec sed -i -E \
  -e "s|(image: localhost/payu-[^:]+:).+|\1$NEW|g" \
  -e "s|(image: image-registry\.openshift-image-registry\.svc:5000/payu[^:]*:).+|\1$NEW|g" \
  -e "s|(newTag: \")[0-9]+\.[0-9]+\.[0-9]+[^\"]*\"|\1$NEW\"|g" \
  -e "s|(app\.kubernetes\.io/version: )[0-9]+\.[0-9]+\.[0-9]+|\1$NEW|g" \
  -e "s|(PAYU_VERSION:-\")[0-9]+\.[0-9]+\.[0-9]+[^\"]*\"|\1$NEW\"|g" \
  -e "s|(PAYU_VERSION:-)[0-9]+\.[0-9]+\.[0-9]+[^\}]*\}|\1$NEW\}|g" \
  -e "s|(value: \")[0-9]+\.[0-9]+\.[0-9]+\"|\1$NEW\"|g" \
  -e "s|(image-tag\"? *: *\")[0-9]+\.[0-9]+\.[0-9]+[^\"]*\"|\1$NEW\"|g" \
  -e "s|(default: \")[0-9]+\.[0-9]+\.[0-9]+\"|\1$NEW\"|g" \
  {} +
# podman-compose uses ${PAYU_VERSION:-1.18.XX}  — handle without quotes
sed -i -E "s|\\$\\{PAYU_VERSION:-1\.18\.[0-9]+\}|\\\${PAYU_VERSION:-$NEW}|g" "$ROOT/infrastructure/local/podman/podman-compose.yml"
# update VERSION file itself
echo "$NEW" > "$ROOT/VERSION"
# also update CHANGELOG header if needed? keep manual
echo "Done. Grep new version count:"
grep -r "$NEW" "$ROOT/infrastructure" --include="*.yaml" --include="*.yml" | wc -l
