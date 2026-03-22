#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REGISTRY_FILE="$ROOT_DIR/.agent/skills/REGISTRY.yaml"

if [[ ! -f "$REGISTRY_FILE" ]]; then
  echo "Missing registry: $REGISTRY_FILE"
  exit 1
fi

if command -v rg >/dev/null 2>&1; then
  skill_names=$(rg -o "^[[:space:]]*- name: [A-Za-z0-9-]+" -N "$REGISTRY_FILE" | sed 's/^[[:space:]]*- name: //')
else
  skill_names=$(grep -E "^[[:space:]]*- name: " "$REGISTRY_FILE" | sed 's/^[[:space:]]*- name: //')
fi

if [[ -z "$skill_names" ]]; then
  echo "No skills found in $REGISTRY_FILE"
  exit 1
fi

missing=0
declare -A registry_map

for skill in $skill_names; do
  registry_map["$skill"]=1
  skill_dir="$ROOT_DIR/.agent/skills/$skill"
  skill_file="$skill_dir/SKILL.md"

  if [[ ! -d "$skill_dir" ]]; then
    echo "Missing skill directory: $skill_dir"
    missing=1
    continue
  fi

  if [[ ! -f "$skill_file" ]]; then
    echo "Missing SKILL.md: $skill_file"
    missing=1
    continue
  fi

  front_name=$(
    awk '
      /^---[[:space:]]*$/ { in_front = !in_front; next }
      in_front && /^name:[[:space:]]*/ {
        sub(/^name:[[:space:]]*/, "", $0);
        print $0;
        exit
      }
    ' "$skill_file"
  )

  if [[ -z "$front_name" ]]; then
    echo "Missing frontmatter name in: $skill_file"
    missing=1
  elif [[ "$front_name" != "$skill" ]]; then
    echo "Name mismatch in $skill_file: expected '$skill', got '$front_name'"
    missing=1
  fi
done

for dir in "$ROOT_DIR/.agent/skills"/*; do
  [[ -d "$dir" ]] || continue
  base="$(basename "$dir")"
  if [[ -z "${registry_map[$base]+x}" ]]; then
    echo "Directory not in registry: $base"
    missing=1
  fi
done

if [[ $missing -ne 0 ]]; then
  echo "Skill validation failed."
  exit 1
fi

echo "Skill validation OK."
