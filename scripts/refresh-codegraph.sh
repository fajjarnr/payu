#!/bin/bash
# Refresh / validate the CodeGraph knowledge-graph index (DX-CODEGRAPH-001)
#
# The index lags source writes by ~1s via the file watcher, but after a large
# refactor the graph can be stale. This script re-syncs it (incremental by
# default) or rebuilds it from scratch, then prints the index status.
#
# Usage:
#   ./scripts/refresh-codegraph.sh            # incremental sync + status
#   ./scripts/refresh-codegraph.sh --full     # full rebuild from scratch
#   ./scripts/refresh-codegraph.sh --status   # just print index status
# ==============================================================================

set -euo pipefail

REPO_ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
MODE="${1:-sync}"

if ! command -v codegraph &>/dev/null; then
  echo "ERROR: 'codegraph' CLI not found on PATH. Install CodeGraph and retry." >&2
  exit 1
fi

cd "${REPO_ROOT}"

case "${MODE}" in
  --full)
    echo "=== Rebuilding CodeGraph index from scratch (this may take a while) ==="
    codegraph index
    ;;
  --status)
    echo "=== CodeGraph index status ==="
    codegraph status
    ;;
  sync|--sync|"")
    echo "=== Syncing CodeGraph index (incremental) ==="
    codegraph sync
    echo ""
    codegraph status
    ;;
  *)
    echo "unknown mode: ${MODE}" >&2
    echo "usage: $0 [sync|--full|--status]" >&2
    exit 2
    ;;
esac
