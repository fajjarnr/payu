#!/bin/bash
set -e

# Configuration
DB_CONTAINER="payu-postgres-verification"
DB_USER="postgres"
DB_PASS="postgres"

echo "=== Starting Migration Verification for All Services ==="

# 1. Cleanup old container if exists
podman rm -f $DB_CONTAINER 2>/dev/null || true

# 2. Start Postgres
echo "Starting PostgreSQL container..."
podman run --name $DB_CONTAINER \
  -e POSTGRES_PASSWORD=$DB_PASS \
  -e POSTGRES_USER=$DB_USER \
  -p 5434:5432 \
  -d docker.io/library/postgres:16

# Wait for DB to be ready
echo "Waiting for DB to accept connections..."
sleep 5
until podman exec $DB_CONTAINER pg_isready -U $DB_USER; do
  echo "Waiting..."
  sleep 2
done

SERVICES_ROOT="backend"
SERVICES=$(ls $SERVICES_ROOT)

FAILED_SERVICES=""

for SERVICE in $SERVICES; do
    MIGRATION_DIR="$SERVICES_ROOT/$SERVICE/src/main/resources/db/migration"
    
    # Clean service name for DB name (replace - with _)
    DB_NAME="payu_$(echo $SERVICE | tr '-' '_' | sed 's/_service//')"
    
    if [ -d "$MIGRATION_DIR" ] && [ "$(ls -A $MIGRATION_DIR)" ]; then
        echo "------------------------------------------------"
        echo "Testing service: $SERVICE (DB: $DB_NAME)"
        
        # Create Database
        podman exec $DB_CONTAINER psql -U $DB_USER -c "CREATE DATABASE $DB_NAME;" || true
        
        # Find and sort SQL files
        FILES=$(find $MIGRATION_DIR -name "V*.sql" | sort -V)
        
        SERVICE_FAILED=false
        
        for SQL_FILE in $FILES; do
            FILENAME=$(basename $SQL_FILE)
            echo "  Applying: $FILENAME"
            
            # Copy file to container
            podman cp "$SQL_FILE" "$DB_CONTAINER:/tmp/$FILENAME"
            
            # Execute
            if ! podman exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -f "/tmp/$FILENAME" > /dev/null 2>&1; then
                echo "  ❌ FAILED: $FILENAME"
                # Print error details
                podman exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -f "/tmp/$FILENAME"
                SERVICE_FAILED=true
                break
            else
                 echo "  ✅ Success: $FILENAME"
            fi
        done
        
        if [ "$SERVICE_FAILED" = true ]; then
            FAILED_SERVICES="$FAILED_SERVICES $SERVICE"
        else
            echo "🏆 $SERVICE Migrations Verified"
        fi
        
    else
        echo "Skipping $SERVICE (No migrations found at $MIGRATION_DIR)"
    fi
done

echo "================================================"
echo "Cleanup..."
podman stop $DB_CONTAINER && podman rm $DB_CONTAINER

if [ -n "$FAILED_SERVICES" ]; then
    echo "❌ Migration verification FAILED for: $FAILED_SERVICES"
    exit 1
else
    echo "✅ ALL SERVICE MIGRATIONS VERIFIED SUCCESSFULLY"
    exit 0
fi
