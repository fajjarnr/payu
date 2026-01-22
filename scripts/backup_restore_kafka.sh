#!/bin/bash
#
# PayU Kafka Backup and Restore Script
# Supports topic data export/import and topic configuration backup
#

set -euo pipefail

# Configuration
CONTAINER_NAME="payu-kafka"
BOOTSTRAP_SERVER="localhost:9092"
BACKUP_DIR="/backups/kafka"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="/var/log/payu/kafka_backup_restore_${TIMESTAMP}.log"

# Default topics to backup (empty = backup all topics)
DEFAULT_TOPICS=""

# Create backup directories if they don't exist
mkdir -p "${BACKUP_DIR}/topics"
mkdir -p "${BACKUP_DIR}/config"
mkdir -p "$(dirname ${LOG_FILE})"

# Logging function
log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[${timestamp}] [${level}] ${message}" | tee -a "${LOG_FILE}"
}

# Check if container is running
check_container() {
    log "INFO" "Checking if Kafka container is running..."
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        log "ERROR" "Container ${CONTAINER_NAME} is not running"
        return 1
    fi
    log "INFO" "Container ${CONTAINER_NAME} is running"
    return 0
}

# Test Kafka connectivity
test_connection() {
    log "INFO" "Testing Kafka connectivity..."

    if ! docker exec "${CONTAINER_NAME}" kafka-broker-api-versions --bootstrap-server "${BOOTSTRAP_SERVER}" > /dev/null 2>&1; then
        log "ERROR" "Cannot connect to Kafka"
        return 1
    fi

    log "INFO" "Kafka connectivity verified"
    return 0
}

# List all topics
list_topics() {
    log "INFO" "Listing all topics..."

    local topics=$(docker exec "${CONTAINER_NAME}" kafka-topics \
        --bootstrap-server "${BOOTSTRAP_SERVER}" \
        --list 2>/dev/null | grep -v "^__")

    if [[ -z "${topics}" ]]; then
        log "INFO" "No topics found"
        return 0
    fi

    echo "${topics}"
}

# Get topic metadata
get_topic_metadata() {
    local topic="$1"

    docker exec "${CONTAINER_NAME}" kafka-topics \
        --bootstrap-server "${BOOTSTRAP_SERVER}" \
        --describe \
        --topic "${topic}" 2>/dev/null
}

# Backup topic data to file
backup_topic_data() {
    local topic="$1"
    local backup_file="${BACKUP_DIR}/topics/${topic}_${TIMESTAMP}.json"
    local offset="earliest"

    log "INFO" "Backing up topic '${topic}' to: ${backup_file}"

    # Create temp file for output
    local temp_file=$(mktemp)

    # Export messages from topic
    docker exec "${CONTAINER_NAME}" kafka-console-consumer \
        --bootstrap-server "${BOOTSTRAP_SERVER}" \
        --topic "${topic}" \
        --from-beginning \
        --property print.key=true \
        --property key.separator=, \
        --property print.value=true \
        > "${temp_file}" 2>/dev/null

    # Check if any messages were exported
    if [[ ! -s "${temp_file}" ]]; then
        log "INFO" "Topic '${topic}' is empty, creating empty backup"
        echo "[]" > "${backup_file}"
    else
        # Convert to JSON format
        local message_count=0
        echo "[" > "${backup_file}"

        while IFS= read -r line; do
            [[ -z "${line}" ]] && continue

            if [[ ${message_count} -gt 0 ]]; then
                echo "," >> "${backup_file}"
            fi

            local key=$(echo "${line}" | cut -d',' -f1 | sed 's/"/\\"/g')
            local value=$(echo "${line}" | cut -d',' -f2- | sed 's/"/\\"/g')

            echo "  {\"key\": \"${key}\", \"value\": \"${value}\"}" >> "${backup_file}"
            message_count=$((message_count + 1))
        done < "${temp_file}"

        echo "]" >> "${backup_file}"
        log "INFO" "Backed up ${message_count} messages from topic '${topic}'"
    fi

    # Clean up
    rm -f "${temp_file}"

    # Compress backup
    gzip "${backup_file}"
    local file_size=$(du -h "${backup_file}.gz" | cut -f1)
    log "INFO" "Successfully backed up topic '${topic}' to ${backup_file}.gz (${file_size})"

    return 0
}

# Backup topic configuration
backup_topic_config() {
    local topic="$1"
    local config_file="${BACKUP_DIR}/config/${topic}_${TIMESTAMP}.conf"

    log "INFO" "Backing up configuration for topic '${topic}'..."

    get_topic_metadata "${topic}" > "${config_file}"

    local file_size=$(du -h "${config_file}" | cut -f1)
    log "INFO" "Successfully backed up topic configuration to ${config_file} (${file_size})"

    return 0
}

# Backup all topics
backup_all_topics() {
    local topics=("$@")

    if [[ ${#topics[@]} -eq 0 ]]; then
        # Get all topics except internal ones
        topics=($(list_topics))
    fi

    if [[ ${#topics[@]} -eq 0 ]]; then
        log "INFO" "No topics to backup"
        return 0
    fi

    log "INFO" "Backing up ${#topics[@]} topic(s)..."

    local success_count=0
    local total_count=${#topics[@]}

    for topic in "${topics[@]}"; do
        if backup_topic_data "${topic}" && backup_topic_config "${topic}"; then
            success_count=$((success_count + 1))
        else
            log "ERROR" "Failed to backup topic '${topic}'"
        fi
    done

    log "INFO" "Backup complete: ${success_count}/${total_count} topics backed up"

    if [[ ${success_count} -eq ${total_count} ]]; then
        return 0
    else
        return 1
    fi
}

# Restore topic data from file
restore_topic_data() {
    local topic="$1"
    local backup_file="$2"
    local force="${3:-false}"

    log "INFO" "Restoring topic '${topic}' from: ${backup_file}"

    if [[ ! -f "${backup_file}" ]]; then
        # Try with .gz extension
        if [[ -f "${backup_file}.gz" ]]; then
            backup_file="${backup_file}.gz"
        else
            log "ERROR" "Backup file not found: ${backup_file}"
            return 1
        fi
    fi

    if [[ "${force}" != "true" ]]; then
        echo ""
        read -p "WARNING: This will APPEND messages to topic '${topic}'. Continue? (yes/no): " confirm
        if [[ "${confirm}" != "yes" ]]; then
            log "INFO" "Restore cancelled by user"
            return 1
        fi
    fi

    # Extract data from backup file
    local temp_file=$(mktemp)

    if [[ "${backup_file}" == *.gz ]]; then
        gunzip -c "${backup_file}" > "${temp_file}"
    else
        cp "${backup_file}" "${temp_file}"
    fi

    # Parse JSON and produce messages to topic
    local message_count=0

    # Check if backup is empty
    local data=$(cat "${temp_file}" | jq -r '.[]' 2>/dev/null)

    if [[ -z "${data}" || "${data}" == "[]" ]]; then
        log "INFO" "Backup is empty, no messages to restore"
        rm -f "${temp_file}"
        return 0
    fi

    # Restore messages
    docker exec -i "${CONTAINER_NAME}" kafka-console-producer \
        --bootstrap-server "${BOOTSTRAP_SERVER}" \
        --topic "${topic}" \
        --property "parse.key=true" \
        --property "key.separator=," \
        < "${temp_file}" 2>/dev/null

    message_count=$(docker exec "${CONTAINER_NAME}" kafka-run-class kafka.tools.GetOffsetShell \
        --broker-list "${BOOTSTRAP_SERVER}" --topic "${topic}" --time -1 2>/dev/null | awk -F: '{sum+=$3} END {print sum}')

    rm -f "${temp_file}"

    log "INFO" "Restored messages to topic '${topic}'"
    return 0
}

# Restore topic configuration
restore_topic_config() {
    local config_file="$1"

    log "INFO" "Restoring topic configuration from: ${config_file}"

    if [[ ! -f "${config_file}" ]]; then
        log "ERROR" "Configuration file not found: ${config_file}"
        return 1
    fi

    # Extract topic name and recreate with config
    local topic=$(grep "^Topic:" "${config_file}" | awk '{print $2}')
    local partitions=$(grep "PartitionCount:" "${config_file}" | awk '{print $2}')
    local replication=$(grep "ReplicationFactor:" "${config_file}" | awk '{print $2}')

    if [[ -z "${topic}" ]]; then
        log "ERROR" "Could not determine topic name from config file"
        return 1
    fi

    # Delete existing topic if it exists
    docker exec "${CONTAINER_NAME}" kafka-topics \
        --bootstrap-server "${BOOTSTRAP_SERVER}" \
        --delete \
        --topic "${topic}" 2>/dev/null || true

    sleep 2

    # Recreate topic with original configuration
    docker exec "${CONTAINER_NAME}" kafka-topics \
        --bootstrap-server "${BOOTSTRAP_SERVER}" \
        --create \
        --topic "${topic}" \
        --partitions "${partitions}" \
        --replication-factor "${replication}" 2>/dev/null

    log "INFO" "Restored topic '${topic}' configuration"
    return 0
}

# Restore all topics
restore_all_topics() {
    local backup_date="${1:-}"

    log "INFO" "Restoring all topics..."

    # Find backup files for the specified date or latest
    local backup_files=()

    if [[ -n "${backup_date}" ]]; then
        backup_files=($(find "${BACKUP_DIR}/topics" -name "*_${backup_date}*.json.gz"))
    else
        # Use latest backup
        local latest=$(ls -t ${BACKUP_DIR}/topics/*.json.gz 2>/dev/null | head -1)
        if [[ -n "${latest}" ]]; then
            # Extract date pattern from latest file
            backup_date=$(echo "${latest}" | grep -oP '_\d{8}_\d{6}' | head -1 | tr -d '_')
            backup_files=($(find "${BACKUP_DIR}/topics" -name "*_${backup_date}*.json.gz"))
        fi
    fi

    if [[ ${#backup_files[@]} -eq 0 ]]; then
        log "ERROR" "No backup files found for date: ${backup_date}"
        return 1
    fi

    log "INFO" "Found ${#backup_files[@]} backup file(s)"

    # First restore configurations
    for backup_file in "${backup_files[@]}"; do
        local topic=$(basename "${backup_file}" | sed 's/_[0-9]*_[0-9]*\.json\.gz//')
        local config_file="${BACKUP_DIR}/config/${topic}_${backup_date}.conf"

        if [[ -f "${config_file}" ]]; then
            restore_topic_config "${config_file}"
        fi
    done

    sleep 5

    # Then restore data
    local success_count=0
    local total_count=${#backup_files[@]}

    for backup_file in "${backup_files[@]}"; do
        local topic=$(basename "${backup_file}" | sed 's/_[0-9]*_[0-9]*\.json\.gz//')

        if restore_topic_data "${topic}" "${backup_file}" "true"; then
            success_count=$((success_count + 1))
        else
            log "ERROR" "Failed to restore topic '${topic}'"
        fi
    done

    log "INFO" "Restore complete: ${success_count}/${total_count} topics restored"

    if [[ ${success_count} -eq ${total_count} ]]; then
        return 0
    else
        return 1
    fi
}

# List available backups
list_backups() {
    log "INFO" "Available Kafka backups:"

    local backups=$(ls -lht ${BACKUP_DIR}/topics/*.json.gz 2>/dev/null)
    if [[ -z "${backups}" ]]; then
        log "INFO" "No backups found"
        return 0
    fi

    echo ""
    echo "Topic Data Backups:"
    ls -lht ${BACKUP_DIR}/topics/*.json.gz | awk 'NR>1 {print "  " $9 " (" $5 ", " $6 " " $7 " " $8 ")"}' | sed 's|.*/topics/||'

    echo ""
    echo "Topic Configuration Backups:"
    ls -lht ${BACKUP_DIR}/config/*.conf 2>/dev/null | awk 'NR>1 {print "  " $9 " (" $5 ", " $6 " " $7 " " $8 ")"}' | sed 's|.*/config/||'
}

# Get Kafka cluster statistics
get_stats() {
    log "INFO" "Kafka Cluster Statistics:"

    local topic_count=$(docker exec "${CONTAINER_NAME}" kafka-topics \
        --bootstrap-server "${BOOTSTRAP_SERVER}" \
        --list 2>/dev/null | grep -v "^__" | wc -l)

    echo ""
    echo "Total Topics: ${topic_count}"
    echo ""

    log "INFO" "Topic Details:"
    echo ""

    docker exec "${CONTAINER_NAME}" kafka-topics \
        --bootstrap-server "${BOOTSTRAP_SERVER}" \
        --describe 2>/dev/null | grep -v "^__" || true
}

# Clean up old backups
cleanup_old_backups() {
    local retention_days="${1:-7}"

    log "INFO" "Cleaning up backups older than ${retention_days} days..."

    local deleted=$(find "${BACKUP_DIR}" -type f -mtime +${retention_days} -delete -print | wc -l)

    local remaining_data=$(find "${BACKUP_DIR}/topics" -name "*.json.gz" | wc -l)
    local remaining_config=$(find "${BACKUP_DIR}/config" -name "*.conf" | wc -l)

    log "INFO" "Deleted ${deleted} old backup file(s). ${remaining_data} data backups, ${remaining_config} config backups remaining"
}

# Main routine
main() {
    local action="$1"
    shift || true

    log "INFO" "=========================================="
    log "INFO" "PayU Kafka Backup/Restore"
    log "INFO" "Action: ${action}"
    log "INFO" "=========================================="

    case "${action}" in
        backup)
            if ! check_container; then
                exit 1
            fi
            if ! test_connection; then
                exit 1
            fi
            if backup_all_topics "$@"; then
                log "INFO" "✓ Backup completed successfully"
                exit 0
            else
                log "ERROR" "✗ Backup failed"
                exit 1
            fi
            ;;
        restore)
            if ! check_container; then
                exit 1
            fi
            if ! test_connection; then
                exit 1
            fi
            local backup_date="${1:-}"
            if restore_all_topics "${backup_date}"; then
                log "INFO" "✓ Restore completed successfully"
                exit 0
            else
                log "ERROR" "✗ Restore failed"
                exit 1
            fi
            ;;
        list)
            list_backups
            ;;
        stats)
            if ! check_container; then
                exit 1
            fi
            if ! test_connection; then
                exit 1
            fi
            get_stats
            ;;
        topics)
            if ! check_container; then
                exit 1
            fi
            if ! test_connection; then
                exit 1
            fi
            list_topics
            ;;
        cleanup)
            cleanup_old_backups "${1:-7}"
            ;;
        *)
            echo "Usage: $0 {backup|restore|list|stats|topics|cleanup} [options]"
            echo ""
            echo "Commands:"
            echo "  backup [topic1 topic2...]  - Backup all topics or specific topics"
            echo "  restore [date]             - Restore all topics from backup date (format: YYYYMMDD_HHMMSS)"
            echo "  list                       - List available backups"
            echo "  stats                      - Show Kafka cluster statistics"
            echo "  topics                     - List all topics"
            echo "  cleanup [days]             - Clean up old backups (default: 7 days)"
            echo ""
            echo "Examples:"
            echo "  $0 backup"
            echo "  $0 backup transactions accounts"
            echo "  $0 restore 20250122_020000"
            echo "  $0 list"
            echo "  $0 stats"
            echo "  $0 topics"
            echo "  $0 cleanup 30"
            exit 1
            ;;
    esac
}

main "$@"
