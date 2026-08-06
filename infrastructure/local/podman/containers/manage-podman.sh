#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd -- "$SCRIPT_DIR/../../../.." && pwd)
COMPOSE_FILE="$REPO_ROOT/infrastructure/local/podman/podman-compose.yml"
COMPOSE=(podman compose -f "$COMPOSE_FILE")
CORE=(payu-database-rw payu-cache payu-kafka-kafka-bootstrap artemis payu-keycloak-service)

case "${1:-help}" in
  core)
    "${COMPOSE[@]}" up -d "${CORE[@]}"
    ;;
  apps)
    "${COMPOSE[@]}" --profile apps up -d
    ;;
  api-management)
    "${COMPOSE[@]}" --profile api-management up -d apicast
    ;;
  all)
    "${COMPOSE[@]}" --profile apps --profile api-management up -d
    ;;
  build)
    "${COMPOSE[@]}" --profile apps build
    ;;
  status)
    "${COMPOSE[@]}" --profile apps --profile api-management ps
    ;;
  logs)
    "${COMPOSE[@]}" logs -f "${2:?usage: $0 logs SERVICE}"
    ;;
  stop)
    "${COMPOSE[@]}" --profile apps --profile api-management down
    ;;
  smoke)
    PAYU_RUN_PODMAN_INTEGRATION=1 \
      python3 -m unittest tests.infrastructure.test_docker_infrastructure -v
    ;;
  help|--help|-h)
    printf '%s\n' \
      "usage: $0 {core|apps|api-management|all|build|status|logs SERVICE|stop|smoke}" \
      "  core            PostgreSQL, Data Grid, Kafka, Artemis, Keycloak" \
      "  apps            core infrastructure plus all backend/web workloads" \
      "  api-management  local APIcast on http://localhost:8095" \
      "  all             apps plus APIcast"
    ;;
  *)
    printf 'unknown command: %s\n' "$1" >&2
    exit 2
    ;;
esac
