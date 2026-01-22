#!/bin/bash
#
# PayU PostgreSQL Backup Script
# Supports both daily logical backups and weekly physical backups
#

set -euo pipefail

# Configuration
CONTAINER_NAME="payu-postgres"
POSTGRES_USER="payu"
BACKUP_DIR="/backups/postgres"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="/var/log/payu/backup_postgres_${TIMESTAMP}.log"

# Create backup directories if they don't exist
mkdir -p "${BACKUP_DIR}/daily"
mkdir -p "${BACKUP_DIR}/weekly"
mkdir -p "$(dirname ${LOG_FILE})"

# List of databases to backup
DATABASES=(
    "payu_account"
    "payu_auth"
    "payu_transaction"
    "payu_wallet"
    "payu_notification"
    "payu_billing"
    "payu_kyc"
    "payu_analytics"
    "payu_bifast"
    "payu_dukcapil"
    "payu_qris"
)

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
    log "INFO" "Checking if PostgreSQL container is running..."
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        log "ERROR" "Container ${CONTAINER_NAME} is not running"
        return 1
    fi
    log "INFO" "Container ${CONTAINER_NAME} is running"
    return 0
}

# Test database connectivity
test_connection() {
    log "INFO" "Testing database connectivity..."
    if ! docker exec "${CONTAINER_NAME}" pg_isready -U "${POSTGRES_USER}" > /dev/null 2>&1; then
        log "ERROR" "Cannot connect to PostgreSQL database"
        return 1
    fi
    log "INFO" "Database connectivity verified"
    return 0
}

# Backup a single database using pg_dump (custom format)
backup_database() {
    local db_name="$1"
    local backup_file="${BACKUP_DIR}/daily/${db_name}_${TIMESTAMP}.dump"
    local backup_file_gz="${backup_file}.gz"

    log "INFO" "Starting backup of database: ${db_name}"

    if docker exec "${CONTAINER_NAME}" pg_dump -U "${POSTGRES_USER}" -Fc "${db_name}" | gzip > "${backup_file_gz}"; then
        local file_size=$(du -h "${backup_file_gz}" | cut -f1)
        log "INFO" "Successfully backed up ${db_name} to ${backup_file_gz} (${file_size})"
        return 0
    else
        log "ERROR" "Failed to backup database: ${db_name}"
        return 1
    fi
}

# Backup all databases at once using pg_dumpall
backup_all_databases() {
    local backup_file="${BACKUP_DIR}/daily/all_databases_${TIMESTAMP}.sql"
    local backup_file_gz="${backup_file}.gz"

    log "INFO" "Starting full backup of all databases..."

    if docker exec "${CONTAINER_NAME}" pg_dumpall -U "${POSTGRES_USER}" | gzip > "${backup_file_gz}"; then
        local file_size=$(du -h "${backup_file_gz}" | cut -f1)
        log "INFO" "Successfully backed up all databases to ${backup_file_gz} (${file_size})"
        return 0
    else
        log "ERROR" "Failed to backup all databases"
        return 1
    fi
}

# Perform weekly physical backup (base backup for PITR)
weekly_backup() {
    local backup_dir="${BACKUP_DIR}/weekly/base_backup_${TIMESTAMP}"

    log "INFO" "Starting weekly physical backup to ${backup_dir}"

    if docker exec "${CONTAINER_NAME}" pg_basebackup -U "${POSTGRES_USER}" -D "/tmp/base_backup" -Ft -z -P; then
        # Copy from container to host
        docker cp "${CONTAINER_NAME}:/tmp/base_backup.tar.gz" "${BACKUP_DIR}/weekly/base_backup_${TIMESTAMP}.tar.gz"
        docker exec "${CONTAINER_NAME}" rm -rf /tmp/base_backup

        local file_size=$(du -h "${BACKUP_DIR}/weekly/base_backup_${TIMESTAMP}.tar.gz" | cut -f1)
        log "INFO" "Successfully completed weekly backup (${file_size})"
        return 0
    else
        log "ERROR" "Failed to complete weekly backup"
        return 1
    fi
}

# Clean up old backups
cleanup_old_backups() {
    local backup_type="$1"
    local retention_days="$2"
    local backup_path="${BACKUP_DIR}/${backup_type}"

    log "INFO" "Cleaning up backups older than ${retention_days} days in ${backup_path}..."

    find "${backup_path}" -type f -mtime +${retention_days} -delete

    local remaining=$(find "${backup_path}" -type f | wc -l)
    log "INFO" "Cleanup complete. ${remaining} backup files remaining"
}

# Verify backup integrity
verify_backup() {
    local backup_file="$1"

    log "INFO" "Verifying backup: ${backup_file}"

    if [[ ! -f "${backup_file}" ]]; then
        log "ERROR" "Backup file does not exist: ${backup_file}"
        return 1
    fi

    # For pg_dump custom format, list contents
    if [[ "${backup_file}" == *.dump.gz ]]; then
        local temp_file=$(mktemp)
        gunzip -c "${backup_file}" > "${temp_file}"
        if docker exec -i "${CONTAINER_NAME}" pg_restore -l "${temp_file}" > /dev/null 2>&1; then
            log "INFO" "Backup integrity verified: ${backup_file}"
            rm -f "${temp_file}"
            return 0
        else
            log "ERROR" "Backup integrity check failed: ${backup_file}"
            rm -f "${temp_file}"
            return 1
        fi
    fi

    log "INFO" "Backup file exists: ${backup_file}"
    return 0
}

# Main backup routine
main() {
    local backup_type="${1:-daily}"
    local retention_days="${2:-7}"

    log "INFO" "=========================================="
    log "INFO" "PayU PostgreSQL Backup"
    log "INFO" "Backup Type: ${backup_type}"
    log "INFO" "Retention: ${retention_days} days"
    log "INFO" "=========================================="

    if ! check_container; then
        exit 1
    fi

    if ! test_connection; then
        exit 1
    fi

    local success_count=0
    local total_count=0

    if [[ "${backup_type}" == "weekly" ]]; then
        # Weekly physical backup
        total_count=1
        if weekly_backup; then
            success_count=1
        fi
    else
        # Daily logical backup
        total_count=$((${#DATABASES[@]} + 1))

        # Backup all databases
        if backup_all_databases; then
            success_count=$((success_count + 1))
            verify_backup "${BACKUP_DIR}/daily/all_databases_${TIMESTAMP}.sql.gz"
        fi

        # Backup individual databases
        for db in "${DATABASES[@]}"; do
            if backup_database "${db}"; then
                success_count=$((success_count + 1))
                verify_backup "${BACKUP_DIR}/daily/${db}_${TIMESTAMP}.dump.gz"
            fi
        done
    fi

    # Cleanup old backups
    cleanup_old_backups "${backup_type}" "${retention_days}"

    log "INFO" "=========================================="
    log "INFO" "Backup Summary: ${success_count}/${total_count} successful"
    log "INFO" "=========================================="

    if [[ ${success_count} -eq ${total_count} ]]; then
        log "INFO" "✓ All backups completed successfully"
        exit 0
    else
        log "ERROR" "✗ Some backups failed. Check log: ${LOG_FILE}"
        exit 1
    fi
}

# Parse command line arguments
case "${1:-}" in
    daily)
        main "daily" "${2:-7}"
        ;;
    weekly)
        main "weekly" "${2:-365}"
        ;;
    all)
        main "daily" "${2:-7}"
        main "weekly" "${2:-365}"
        ;;
    cleanup)
        cleanup_old_backups "daily" "${2:-7}"
        cleanup_old_backups "weekly" "${2:-365}"
        ;;
    *)
        echo "Usage: $0 {daily|weekly|all|cleanup} [retention_days]"
        echo ""
        echo "Examples:"
        echo "  $0 daily 7        # Daily backup with 7-day retention"
        echo "  $0 weekly 365     # Weekly backup with 365-day retention"
        echo "  $0 all 7          # Both daily and weekly backups"
        echo "  $0 cleanup 30     # Clean up backups older than 30 days"
        exit 1
        ;;
esac
