#!/bin/bash
#
# PayU Redis Backup and Restore Script
# Supports RDB snapshot backups and restoration
#

set -euo pipefail

# Configuration
CONTAINER_NAME="payu-redis"
BACKUP_ROOT="${BACKUP_ROOT:-/backups}"
BACKUP_DIR="${BACKUP_ROOT}/redis/snapshots"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${BACKUP_ROOT}/logs/redis_backup_restore_${TIMESTAMP}.log"

# Create backup directory if it doesn't exist
mkdir -p "${BACKUP_DIR}"
mkdir -p "$(dirname ${LOG_FILE})" 2>/dev/null || true

# Logging function
log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[${timestamp}] [${level}] ${message}" | tee -a "${LOG_FILE}" >&2
}

# Check if container is running
check_container() {
    log "INFO" "Checking if Redis container is running..."
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        log "ERROR" "Container ${CONTAINER_NAME} is not running"
        return 1
    fi
    log "INFO" "Container ${CONTAINER_NAME} is running"
    return 0
}

# Test Redis connectivity
test_connection() {
    log "INFO" "Testing Redis connectivity..."
    if ! docker exec "${CONTAINER_NAME}" redis-cli ping > /dev/null 2>&1; then
        log "ERROR" "Cannot connect to Redis"
        return 1
    fi
    log "INFO" "Redis connectivity verified"
    return 0
}

# Trigger Redis background save
trigger_save() {
    log "INFO" "Triggering background save..."

    local result=$(docker exec "${CONTAINER_NAME}" redis-cli BGSAVE)

    if [[ "${result}" == *"Background saving started"* ]]; then
        log "INFO" "Background save triggered successfully"
        return 0
    else
        log "ERROR" "Failed to trigger background save: ${result}"
        return 1
    fi
}

# Wait for save to complete
wait_for_save() {
    log "INFO" "Waiting for save to complete..."

    local max_wait=60
    local elapsed=0

    while [[ ${elapsed} -lt ${max_wait} ]]; do
        local status=$(docker exec "${CONTAINER_NAME}" redis-cli LASTSAVE)
        sleep 1
        local new_status=$(docker exec "${CONTAINER_NAME}" redis-cli LASTSAVE)

        if [[ "${status}" != "${new_status}" ]]; then
            log "INFO" "Save completed"
            return 0
        fi

        elapsed=$((elapsed + 1))
    done

    log "ERROR" "Save did not complete within ${max_wait} seconds"
    return 1
}

# Backup RDB file
backup_snapshot() {
    local backup_file="${BACKUP_DIR}/dump_${TIMESTAMP}.rdb"

    log "INFO" "Creating backup to: ${backup_file}"

    # Trigger background save
    if ! trigger_save; then
        return 1
    fi

    # Wait for save to complete
    if ! wait_for_save; then
        return 1
    fi

    # Copy RDB file from container
    if docker cp "${CONTAINER_NAME}:/data/dump.rdb" "${backup_file}"; then
        local file_size=$(du -h "${backup_file}" | cut -f1)
        log "INFO" "Successfully backed up Redis snapshot to ${backup_file} (${file_size})"
        return 0
    else
        log "ERROR" "Failed to copy RDB file from container"
        return 1
    fi
}

# Restore RDB file
restore_snapshot() {
    local backup_file="$1"
    local force="${2:-false}"

    log "INFO" "Restoring from: ${backup_file}"

    if [[ ! -f "${backup_file}" ]]; then
        log "ERROR" "Backup file not found: ${backup_file}"
        return 1
    fi

    if [[ "${force}" != "true" ]]; then
        echo ""
        read -p "WARNING: This will STOP Redis and LOAD the backup, losing all current data. Continue? (yes/no): " confirm
        if [[ "${confirm}" != "yes" ]]; then
            log "INFO" "Restore cancelled by user"
            return 1
        fi
    fi

    # Stop Redis container
    log "INFO" "Stopping Redis container..."
    docker stop "${CONTAINER_NAME}" || true

    # Get the Docker volume name for Redis data
    local volume_name=$(docker inspect "${CONTAINER_NAME}" --format '{{range .Mounts}}{{if .Destination == "/data"}}{{.Name}}{{end}}{{end}}' 2>/dev/null)

    if [[ -z "${volume_name}" ]]; then
        # Fallback: use Docker cp to container's data directory
        log "INFO" "Copying backup to container data directory..."
        docker start "${CONTAINER_NAME}"
        sleep 2
        docker cp "${backup_file}" "${CONTAINER_NAME}:/data/dump_temp.rdb"
        docker exec "${CONTAINER_NAME}" mv /data/dump_temp.rdb /data/dump.rdb
        docker restart "${CONTAINER_NAME}"
    else
        # Use Docker volume directly
        log "INFO" "Copying backup to Docker volume..."
        docker run --rm -v "${volume_name}:/data" -v "$(dirname ${backup_file}):/backup" alpine:latest cp "/backup/$(basename ${backup_file})" /data/dump.rdb
        docker start "${CONTAINER_NAME}"
    fi

    # Wait for Redis to start
    log "INFO" "Waiting for Redis to start..."
    sleep 5

    # Verify Redis is running
    if test_connection; then
        log "INFO" "Redis is running after restore"
        return 0
    else
        log "ERROR" "Redis failed to start after restore"
        return 1
    fi
}

# List available backups
list_backups() {
    log "INFO" "Available Redis snapshots:"

    local backups=$(ls -lht ${BACKUP_DIR}/dump_*.rdb 2>/dev/null)
    if [[ -z "${backups}" ]]; then
        log "INFO" "No backups found"
        return 0
    fi

    echo ""
    ls -lht ${BACKUP_DIR}/dump_*.rdb | awk 'NR>1 {print "  " $9 " (" $5 ", " $6 " " $7 " " $8 ")"}'
}

# Clean up old backups
cleanup_old_backups() {
    local retention_days="${1:-7}"

    log "INFO" "Cleaning up backups older than ${retention_days} days..."

    local deleted=$(find "${BACKUP_DIR}" -name "dump_*.rdb" -mtime +${retention_days} -delete -print | wc -l)
    local remaining=$(find "${BACKUP_DIR}" -name "dump_*.rdb" | wc -l)

    log "INFO" "Deleted ${deleted} old backup(s). ${remaining} backup(s) remaining"
}

# Get Redis statistics
get_stats() {
    log "INFO" "Redis Statistics:"

    local info=$(docker exec "${CONTAINER_NAME}" redis-cli INFO)

    echo ""
    echo "Connected Clients: $(echo "${info}" | grep "^connected_clients:" | cut -d: -f2 | tr -d '\r')"
    echo "Used Memory: $(echo "${info}" | grep "^used_memory_human:" | cut -d: -f2 | tr -d '\r')"
    echo "Total Keys: $(echo "${info}" | grep "^db0:" | cut -d: -f2 | cut -d= -f2 | tr -d '\r')"
    echo "Uptime: $(echo "${info}" | grep "^uptime_in_days:" | cut -d: -f2 | tr -d '\r') days"
    echo "Last Save: $(echo "${info}" | grep "^rdb_last_save_time:" | cut -d: -f2 | tr -d '\r')"
}

# Find the latest backup
find_latest_backup() {
    local backup_file=$(ls -t ${BACKUP_DIR}/dump_*.rdb 2>/dev/null | head -1)

    if [[ -z "${backup_file}" ]]; then
        log "ERROR" "No backup found"
        return 1
    fi

    echo "${backup_file}"
    return 0
}

# Verify backup integrity
verify_backup() {
    local backup_file="$1"

    log "INFO" "Verifying backup: ${backup_file}"

    if [[ ! -f "${backup_file}" ]]; then
        log "ERROR" "Backup file does not exist: ${backup_file}"
        return 1
    fi

    # Check file size
    local file_size=$(du -k "${backup_file}" | cut -f1)

    if [[ ${file_size} -eq 0 ]]; then
        log "ERROR" "Backup file is empty: ${backup_file}"
        return 1
    fi

    # Check RDB file header
    local header=$(xxd -l 9 "${backup_file}" 2>/dev/null | awk '{print $2 $3 $4}')

    if [[ "${header}" != "52454449535602006" ]]; then
        log "WARNING" "RDB header not recognized (expected: REDIS, got: ${header})"
    else
        log "INFO" "RDB header verified"
    fi

    log "INFO" "Backup integrity verified (${file_size} KB)"
    return 0
}

# Main routine
main() {
    local action="$1"
    shift || true

    log "INFO" "=========================================="
    log "INFO" "PayU Redis Backup/Restore"
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
            if backup_snapshot; then
                verify_backup "${BACKUP_DIR}/dump_${TIMESTAMP}.rdb"
                log "INFO" "✓ Backup completed successfully"
                exit 0
            else
                log "ERROR" "✗ Backup failed"
                exit 1
            fi
            ;;
        restore)
            if [[ -z "${1:-}" ]]; then
                log "ERROR" "Backup file required"
                exit 1
            fi
            local backup_file="$1"
            local force="${2:-false}"
            if restore_snapshot "${backup_file}" "${force}"; then
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
        cleanup)
            cleanup_old_backups "${1:-7}"
            ;;
        verify)
            if [[ -z "${1:-}" ]]; then
                backup_file=$(find_latest_backup)
            else
                backup_file="$1"
            fi
            if verify_backup "${backup_file}"; then
                log "INFO" "✓ Verification passed"
                exit 0
            else
                log "ERROR" "✗ Verification failed"
                exit 1
            fi
            ;;
        *)
            echo "Usage: $0 {backup|restore|list|stats|cleanup|verify} [options]"
            echo ""
            echo "Commands:"
            echo "  backup                      - Create a new snapshot backup"
            echo "  restore <file> [force]     - Restore from backup file"
            echo "  list                        - List available backups"
            echo "  stats                       - Show Redis statistics"
            echo "  cleanup [days]              - Clean up old backups (default: 7 days)"
            echo "  verify [file]               - Verify backup integrity"
            echo ""
            echo "Examples:"
            echo "  $0 backup"
            echo "  $0 restore /backups/redis/snapshots/dump_20250122_100000.rdb"
            echo "  $0 restore /backups/redis/snapshots/dump_20250122_100000.rdb force"
            echo "  $0 list"
            echo "  $0 stats"
            echo "  $0 cleanup 30"
            echo "  $0 verify"
            exit 1
            ;;
    esac
}

main "$@"
