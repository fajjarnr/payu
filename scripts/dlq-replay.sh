#!/bin/bash
# DLQ replay per ADR-0041 (SKIP LOCKED + *.dlq) — replays DLQ to original topic best-effort
# Usage: ./scripts/dlq-replay.sh <original-topic> [dlq-topic] [max-records]
# Example: ./scripts/dlq-replay.sh payu.wallet.balance-changed.v1
set -euo pipefail

TOPIC="${1:-}"
DLQ_TOPIC="${2:-}"
MAX="${3:-100}"

if [[ -z "$TOPIC" ]]; then
  echo "Usage: $0 <original-topic> [dlq-topic] [max-records]" >&2
  echo "Example: $0 payu.wallet.balance-changed.v1" >&2
  exit 1
fi

if [[ -z "$DLQ_TOPIC" ]]; then
  DLQ_TOPIC="${TOPIC}.dlq"
fi

KAFKA_CONTAINER="${KAFKA_CONTAINER:-payu-kafka-kafka-bootstrap}"
BOOTSTRAP="${KAFKA_BOOTSTRAP:-localhost:9092}"

echo "Replaying DLQ $DLQ_TOPIC -> $TOPIC (max $MAX, container $KAFKA_CONTAINER)"

if ! podman ps --format "{{.Names}}" 2>/dev/null | grep -q "$KAFKA_CONTAINER"; then
  echo "Kafka container $KAFKA_CONTAINER not running — start podman-compose infra first" >&2
  exit 0
fi

# ponytail: one-shot consumer->producer via kafka-console, no extra deps
podman exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server "$BOOTSTRAP" \
  --topic "$DLQ_TOPIC" \
  --max-messages "$MAX" \
  --timeout-ms 5000 2>/dev/null | \
podman exec -i "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server "$BOOTSTRAP" \
  --topic "$TOPIC" 2>/dev/null || true

echo "Replay done (best-effort, check $TOPIC consumer lag)"
