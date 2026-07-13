#!/bin/bash
set -e

# ============================================
# PayU Test Environment Health Check Script
# Validates all test services are healthy before running tests.
# Works with docker or podman; auto-detects running containers.
# ============================================

echo "=========================================="
echo "PayU Test Environment Health Check"
echo "=========================================="

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    if [ "$1" -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
    fi
}

print_warning() { echo -e "${YELLOW}⚠${NC} $1"; }
print_info() { echo -e "${BLUE}ℹ${NC} $1"; }

# ── Detect container runtime ──
CONTAINER_CLI=""
COMPOSE_CMD=""
COMPOSE_FILE="infrastructure/local/podman/podman-compose.yml"

if command -v docker >/dev/null 2>&1; then
    CONTAINER_CLI="docker"
elif command -v podman >/dev/null 2>&1; then
    CONTAINER_CLI="podman"
else
    print_warning "Neither docker nor podman found — container checks skipped."
fi

# Detect running containers via native CLI (not compose — podman-compose v1.x
# lacks --filter / --services flags and may not be installed).
if [ -n "$CONTAINER_CLI" ]; then
    RUNNING_CONTAINERS=$($CONTAINER_CLI ps --format '{{.Names}}' 2>/dev/null | sort)
    RUNNING_COUNT=$(echo "$RUNNING_CONTAINERS" | wc -l | tr -d ' ')
else
    RUNNING_COUNT=0
    RUNNING_CONTAINERS=""
fi

FAILED_CHECKS=0

echo ""
echo "Step 1: Container runtime"
echo "  Runtime  : ${CONTAINER_CLI:-none}"
echo "  Running  : ${RUNNING_COUNT} container(s)"

if [ "$RUNNING_COUNT" -eq 0 ]; then
    print_warning "No running containers. Start environment with:"
    echo "  podman compose -f $COMPOSE_FILE up -d"
    echo ""
    echo "Health checks requiring containers will be skipped."
fi

echo ""

# ── Step 2: Infrastructure container health ──
if [ "$RUNNING_COUNT" -gt 0 ]; then
    echo "Step 2: Infrastructure health"

    # PostgreSQL (payu-database-rw or payu-database-*)
    DB_CONTAINER=$(echo "$RUNNING_CONTAINERS" | grep -m1 'payu-database' || true)
    if [ -n "$DB_CONTAINER" ]; then
        $CONTAINER_CLI exec "$DB_CONTAINER" pg_isready -U postgres >/dev/null 2>&1
        print_status $? "PostgreSQL ($DB_CONTAINER)"
    fi

    # Kafka (payu-kafka-broker-* or payu-kafka)
    KAFKA_CONTAINER=$(echo "$RUNNING_CONTAINERS" | grep -m1 'payu-kafka' | grep -v entity | grep -v controller || true)
    if [ -n "$KAFKA_CONTAINER" ]; then
        # Try kafka-broker-api-versions.sh (AMQ Streams), then kafka-broker-api-versions (Strimzi)
        $CONTAINER_CLI exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 >/dev/null 2>&1 \
            || $CONTAINER_CLI exec "$KAFKA_CONTAINER" kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1
        print_status $? "Kafka ($KAFKA_CONTAINER)"
    fi

    # Cache (payu-cache / Data Grid, or redis-native)
    CACHE_CONTAINER=$(echo "$RUNNING_CONTAINERS" | grep -E 'payu-cache|redis' | head -1 || true)
    if [ -n "$CACHE_CONTAINER" ]; then
        # Data Grid listens on RESP 11222, /rest/v2/cache-managers/default/health needs auth.
        # Accept any TCP response on the expected port as "reachable".
        if command -v curl >/dev/null 2>&1; then
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 \
                "http://localhost:11222/rest/v2/cache-managers/default/health" 2>/dev/null || echo "000")
            # 401 (auth required) or 200 = server alive
            if [ "$STATUS" = "200" ] || [ "$STATUS" = "401" ]; then
                print_status 0 "Cache/DataGrid ($CACHE_CONTAINER)"
            else
                print_status 1 "Cache ($CACHE_CONTAINER — HTTP $STATUS)"
                FAILED_CHECKS=$((FAILED_CHECKS + 1))
            fi
        else
            print_warning "curl not available, skipping cache health check"
        fi
    fi

    # Keycloak
    KC_CONTAINER=$(echo "$RUNNING_CONTAINERS" | grep -m1 'payu-keycloak' || true)
    if [ -n "$KC_CONTAINER" ]; then
        if command -v curl >/dev/null 2>&1; then
            # RHBK 26: /realms/master returns 200, /health/ready for newer, /auth/health/ready for older
            for endpoint in /health/ready /auth/health/ready /realms/master; do
                STATUS=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 \
                    "http://localhost:8099$endpoint" 2>/dev/null || echo "000")
                [ "$STATUS" = "200" ] || [ "$STATUS" = "204" ] && break
            done
            if [ "$STATUS" = "200" ] || [ "$STATUS" = "204" ]; then
                print_status 0 "Keycloak ($KC_CONTAINER)"
            else
                print_status 1 "Keycloak ($KC_CONTAINER — HTTP $STATUS)"
                FAILED_CHECKS=$((FAILED_CHECKS + 1))
            fi
        fi
    fi

    echo ""
    echo "Step 3: Application service health"

    # Auto-discover application containers and probe their health endpoints
    APP_CONTAINERS=$(echo "$RUNNING_CONTAINERS" | grep -vE 'payu-database|payu-kafka|payu-cache|payu-keycloak|payu-broker|payu-apicast|payu-rustfs|payu-artemis|entity-operator' | sort || true)
    if [ -z "$APP_CONTAINERS" ]; then
        print_info "No application containers running. Start with:"
        echo "  podman compose --profile app -f $COMPOSE_FILE up -d"
    fi

    # Known port mapping — add new services here as they get local ports
    declare -A SERVICE_PORTS
    SERVICE_PORTS["account-service"]=8001
    SERVICE_PORTS["auth-service"]=8002
    SERVICE_PORTS["transaction-service"]=8003
    SERVICE_PORTS["wallet-service"]=8004
    SERVICE_PORTS["billing-service"]=8005
    SERVICE_PORTS["notification-service"]=8006
    SERVICE_PORTS["kyc-service"]=8007
    SERVICE_PORTS["analytics-service"]=8008
    SERVICE_PORTS["investment-service"]=8009
    SERVICE_PORTS["lending-service"]=8010
    SERVICE_PORTS["backoffice-service"]=8011
    SERVICE_PORTS["partner-service"]=8012
    SERVICE_PORTS["promotion-service"]=8013
    SERVICE_PORTS["support-service"]=8014
    SERVICE_PORTS["statement-service"]=8015
    SERVICE_PORTS["api-portal-service"]=8021
    SERVICE_PORTS["gateway-service"]=8080
    SERVICE_PORTS["compliance-service"]=8087
    SERVICE_PORTS["cms-service"]=8095
    SERVICE_PORTS["fx-service"]=8096
    SERVICE_PORTS["dispute-service"]=8098
    SERVICE_PORTS["product-catalog-service"]=8100
    SERVICE_PORTS["integration-service"]=8101
    SERVICE_PORTS["web-app"]=3000

    for container in $APP_CONTAINERS; do
        service_name=$(echo "$container" | sed 's/^payu-//')
        port="${SERVICE_PORTS[$service_name]}"
        if [ -n "$port" ] && command -v curl >/dev/null 2>&1; then
            # Try /actuator/health/liveness (Spring) then /q/health (Quarkus) then /health (generic)
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 \
                "http://localhost:$port/actuator/health/liveness" 2>/dev/null || echo "000")
            if [ "$STATUS" = "000" ]; then
                STATUS=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 \
                    "http://localhost:$port/q/health" 2>/dev/null || echo "000")
            fi
            if [ "$STATUS" = "000" ]; then
                STATUS=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 \
                    "http://localhost:$port/health" 2>/dev/null || echo "000")
            fi
            if [ "$STATUS" = "200" ] || [ "$STATUS" = "204" ]; then
                print_status 0 "$service_name (:$port)"
            else
                print_status 1 "$service_name (:$port — HTTP $STATUS)"
                FAILED_CHECKS=$((FAILED_CHECKS + 1))
            fi
        elif [ -n "$port" ]; then
            print_warning "curl not available, skipping $service_name HTTP check"
        fi
    done

    echo ""
    echo "Step 4: Database health"
    if [ -n "$DB_CONTAINER" ]; then
        DB_COUNT=$($CONTAINER_CLI exec "$DB_CONTAINER" psql -U postgres -tAc "SELECT count(*) FROM pg_database WHERE datname LIKE '%payu%'" 2>/dev/null | tr -d '[:space:]')
        print_status 0 "PayU databases found: ${DB_COUNT:-0}"
    fi
fi

# ── Final summary ──
echo ""
echo "=========================================="
echo "Health Check Summary"
echo "=========================================="

if [ "$FAILED_CHECKS" -eq 0 ]; then
    echo -e "${GREEN}✅ All health checks passed.${NC}"
    exit 0
else
    echo -e "${RED}❌ $FAILED_CHECKS health check(s) failed.${NC}"
    if [ -n "$CONTAINER_CLI" ]; then
        echo "Check logs: $CONTAINER_CLI logs <container>"
    fi
    exit 1
fi
