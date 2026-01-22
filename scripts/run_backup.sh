#!/bin/bash
#
# PayU Automated Backup Orchestration Script
# Orchestrates backups for all PayU components
#

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_ROOT="/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${LOG_FILE:-/var/log/payu/run_backup_${TIMESTAMP}.log}"
NOTIFICATION_ENABLED=false

# Create log directory if it doesn't exist (silently fail if no permission)
mkdir -p "$(dirname ${LOG_FILE})" 2>/dev/null || true

# Use /tmp fallback if log directory creation failed
if [ ! -w "$(dirname ${LOG_FILE})" ]; then
    LOG_FILE="/tmp/payu_backup_${TIMESTAMP}.log"
    mkdir -p "$(dirname ${LOG_FILE})" 2>/dev/null || true
fi

# Logging function
log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[${timestamp}] [${level}] ${message}" | tee -a "${LOG_FILE}"
}

# Send notification (placeholder for email/Slack integration)
send_notification() {
    local status="$1"
    local message="$2"

    if [[ "${NOTIFICATION_ENABLED}" == "true" ]]; then
        # Placeholder: integrate with notification service (email, Slack, PagerDuty, etc.)
        log "INFO" "Notification would be sent: [${status}] ${message}"
    fi
}

# Backup PostgreSQL
backup_postgres() {
    local backup_type="${1:-daily}"
    local retention_days="${2:-7}"

    log "INFO" "=========================================="
    log "INFO" "Starting PostgreSQL Backup (${backup_type})"
    log "INFO" "=========================================="

    if "${SCRIPT_DIR}/backup_postgres.sh" "${backup_type}" "${retention_days}"; then
        log "INFO" "✓ PostgreSQL backup completed successfully"
        return 0
    else
        log "ERROR" "✗ PostgreSQL backup failed"
        return 1
    fi
}

# Backup Redis
backup_redis() {
    local retention_days="${1:-7}"

    log "INFO" "=========================================="
    log "INFO" "Starting Redis Backup"
    log "INFO" "=========================================="

    if "${SCRIPT_DIR}/backup_restore_redis.sh" backup; then
        log "INFO" "✓ Redis backup completed successfully"
        return 0
    else
        log "ERROR" "✗ Redis backup failed"
        return 1
    fi
}

# Backup Kafka
backup_kafka() {
    local retention_days="${1:-7}"

    log "INFO" "=========================================="
    log "INFO" "Starting Kafka Backup"
    log "INFO" "=========================================="

    if "${SCRIPT_DIR}/backup_restore_kafka.sh" backup; then
        log "INFO" "✓ Kafka backup completed successfully"
        return 0
    else
        log "ERROR" "✗ Kafka backup failed"
        return 1
    fi
}

# Backup Docker Compose configuration
backup_config() {
    local backup_file="${BACKUP_ROOT}/config/docker-compose_${TIMESTAMP}.yml"

    log "INFO" "=========================================="
    log "INFO" "Starting Configuration Backup"
    log "INFO" "=========================================="

    mkdir -p "${BACKUP_ROOT}/config"

    # Backup docker-compose files
    cp "${SCRIPT_DIR}/../docker-compose.yml" "${backup_file}"
    cp "${SCRIPT_DIR}/../docker-compose.test.yml" "${BACKUP_ROOT}/config/docker-compose.test_${TIMESTAMP}.yml"

    # Backup .env files if they exist
    if [[ -f "${SCRIPT_DIR}/../.env" ]]; then
        cp "${SCRIPT_DIR}/../.env" "${BACKUP_ROOT}/config/.env_${TIMESTAMP}"
    fi

    log "INFO" "✓ Configuration backup completed successfully"
    return 0
}

# Verify backup integrity
verify_backups() {
    log "INFO" "=========================================="
    log "INFO" "Verifying Backup Integrity"
    log "INFO" "=========================================="

    local verification_failed=false

    # Verify PostgreSQL backups
    local pg_backups=$(ls -1 ${BACKUP_ROOT}/postgres/daily/*.gz 2>/dev/null | tail -1)
    if [[ -n "${pg_backups}" ]]; then
        log "INFO" "Verifying PostgreSQL backup: ${pg_backups}"
        if docker exec payu-postgres pg_restore -l /dev/stdin <(gunzip -c "${pg_backups}") > /dev/null 2>&1; then
            log "INFO" "✓ PostgreSQL backup verified"
        else
            log "ERROR" "✗ PostgreSQL backup verification failed"
            verification_failed=true
        fi
    fi

    # Verify Redis backup
    local redis_backup=$(ls -1 ${BACKUP_ROOT}/redis/snapshots/dump_*.rdb 2>/dev/null | tail -1)
    if [[ -n "${redis_backup}" ]]; then
        log "INFO" "Verifying Redis backup: ${redis_backup}"
        if [[ -f "${redis_backup}" && -s "${redis_backup}" ]]; then
            log "INFO" "✓ Redis backup verified"
        else
            log "ERROR" "✗ Redis backup verification failed"
            verification_failed=true
        fi
    fi

    # Verify Kafka backup
    local kafka_backups=$(ls -1 ${BACKUP_ROOT}/kafka/topics/*.json.gz 2>/dev/null | tail -1)
    if [[ -n "${kafka_backups}" ]]; then
        log "INFO" "Verifying Kafka backup: ${kafka_backups}"
        if [[ -f "${kafka_backups}" && -s "${kafka_backups}" ]]; then
            log "INFO" "✓ Kafka backup verified"
        else
            log "ERROR" "✗ Kafka backup verification failed"
            verification_failed=true
        fi
    fi

    if [[ "${verification_failed}" == "true" ]]; then
        return 1
    fi

    return 0
}

# Generate backup report
generate_report() {
    local success_count="$1"
    local total_count="$2"
    local start_time="$3"

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    log "INFO" ""
    log "INFO" "=========================================="
    log "INFO" "Backup Summary Report"
    log "INFO" "=========================================="
    log "INFO" "Start Time: $(date -d @${start_time} '+%Y-%m-%d %H:%M:%S')"
    log "INFO" "End Time: $(date -d @${end_time} '+%Y-%m-%d %H:%M:%S')"
    log "INFO" "Duration: ${duration} seconds"
    log "INFO" "Success Rate: ${success_count}/${total_count} (${success_count}/${total_count})"

    # List backup sizes
    log "INFO" ""
    log "INFO" "Backup Sizes:"

    local total_size=0

    # PostgreSQL
    local pg_size=$(du -sh ${BACKUP_ROOT}/postgres/daily 2>/dev/null | awk '{print $1}')
    log "INFO" "  PostgreSQL: ${pg_size}"
    total_size=$((${total_size} + $(du -sk ${BACKUP_ROOT}/postgres/daily 2>/dev/null | awk '{print $1}')))

    # Redis
    local redis_size=$(du -sh ${BACKUP_ROOT}/redis/snapshots 2>/dev/null | awk '{print $1}')
    log "INFO" "  Redis: ${redis_size}"
    total_size=$((${total_size} + $(du -sk ${BACKUP_ROOT}/redis/snapshots 2>/dev/null | awk '{print $1}')))

    # Kafka
    local kafka_size=$(du -sh ${BACKUP_ROOT}/kafka/topics 2>/dev/null | awk '{print $1}')
    log "INFO" "  Kafka: ${kafka_size}"
    total_size=$((${total_size} + $(du -sk ${BACKUP_ROOT}/kafka/topics 2>/dev/null | awk '{print $1}')))

    log "INFO" "  Total: $(echo ${total_size} | awk '{printf "%.2f GB", $1/1024/1024}')"
    log "INFO" "=========================================="
}

# Main backup routine
main() {
    local components=("$@")
    local backup_type="daily"
    local retention_days=7

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --weekly)
                backup_type="weekly"
                retention_days=365
                shift
                ;;
            --notify)
                NOTIFICATION_ENABLED=true
                shift
                ;;
            --help)
                echo "Usage: $0 [components...] [options]"
                echo ""
                echo "Components:"
                echo "  postgres    Backup PostgreSQL databases"
                echo "  redis       Backup Redis data"
                echo "  kafka       Backup Kafka topics"
                echo "  config      Backup configuration files"
                echo "  all         Backup all components"
                echo ""
                echo "Options:"
                echo "  --weekly    Run weekly backup [365-day retention]"
                echo "  --notify    Send notification on completion"
                echo "  --help      Show this help message"
                echo ""
                echo "Examples:"
                echo "  $0"
                echo "  $0 postgres redis"
                echo "  $0 --weekly"
                echo "  $0 postgres --notify"
                exit 0
                ;;
            *)
                components+=("$1")
                shift
                ;;
        esac
    done

    # Default: backup all components
    if [[ ${#components[@]} -eq 0 ]]; then
        components=("postgres" "redis" "kafka" "config")
    fi

    local start_time=$(date +%s)
    local success_count=0
    local total_count=${#components[@]}

    log "INFO" "=========================================="
    log "INFO" "PayU Automated Backup"
    log "INFO" "Backup Type: ${backup_type}"
    log "INFO" "Retention: ${retention_days} days"
    log "INFO" "Components: ${components[*]}"
    log "INFO" "=========================================="

    # Run backups for each component
    for component in "${components[@]}"; do
        case "${component}" in
            postgres)
                if backup_postgres "${backup_type}" "${retention_days}"; then
                    success_count=$((success_count + 1))
                fi
                ;;
            redis)
                if backup_redis "${retention_days}"; then
                    success_count=$((success_count + 1))
                fi
                ;;
            kafka)
                if backup_kafka "${retention_days}"; then
                    success_count=$((success_count + 1))
                fi
                ;;
            config)
                if backup_config; then
                    success_count=$((success_count + 1))
                fi
                ;;
            *)
                log "ERROR" "Unknown component: ${component}"
                ;;
        esac
    done

    # Verify backups
    if verify_backups; then
        log "INFO" "✓ All backups verified"
    else
        log "WARNING" "Some backups failed verification"
    fi

    # Generate report
    generate_report "${success_count}" "${total_count}" "${start_time}"

    # Send notification
    if [[ ${success_count} -eq ${total_count} ]]; then
        send_notification "SUCCESS" "Backup completed successfully: ${success_count}/${total_count} components"
        log "INFO" "✓ All backups completed successfully"
        exit 0
    else
        send_notification "FAILURE" "Backup partially failed: ${success_count}/${total_count} components"
        log "ERROR" "✗ Some backups failed"
        exit 1
    fi
}

main "$@"
