#!/bin/bash
set -e

# ============================================
# PayU Test Environment Health Check Script
# Validates all test services are healthy before running tests
# ============================================

echo "=========================================="
echo "PayU Test Environment Health Check"
echo "=========================================="

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        return 1
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Determine which compose command to use
if docker-compose --version > /dev/null 2>&1; then
    COMPOSE_CMD="docker-compose -f docker-compose.test.yml"
else
    COMPOSE_CMD="docker compose -f docker-compose.test.yml"
fi

# Expected services (in dependency order)
EXPECTED_SERVICES=(
    "postgres-test"
    "redis-test"
    "timescaledb-test"
    "zookeeper-test"
    "kafka-test"
    "keycloak-test"
    "bi-fast-simulator-test"
    "dukcapil-simulator-test"
    "qris-simulator-test"
    "account-service-test"
    "auth-service-test"
    "transaction-service-test"
    "wallet-service-test"
    "billing-service-test"
    "notification-service-test"
    "gateway-service-test"
    "kyc-service-test"
    "analytics-service-test"
    "web-app-test"
)

FAILED_SERVICES=()
UNHEALTHY_SERVICES=()

echo ""
echo "Step 1: Checking if test environment is running..."
RUNNING_COUNT=$($COMPOSE_CMD ps --services --filter "status=running" | wc -l)
if [ "$RUNNING_COUNT" -lt 5 ]; then
    print_warning "Test environment not running. Starting it now..."
    $COMPOSE_CMD up -d
    echo "Waiting 30 seconds for services to start..."
    sleep 30
fi

echo ""
echo "Step 2: Verifying all containers are running..."
for service in "${EXPECTED_SERVICES[@]}"; do
    if $COMPOSE_CMD ps -q "$service" > /dev/null 2>&1; then
        STATUS=$($COMPOSE_CMD ps "$service" --format "{{.State}}")
        if [[ "$STATUS" == *"running"* ]]; then
            print_status 0 "$service is running"
        else
            print_status 1 "$service is not running (State: $STATUS)"
            FAILED_SERVICES+=("$service")
        fi
    else
        print_status 1 "$service container not found"
        FAILED_SERVICES+=("$service")
    fi
done

echo ""
echo "Step 3: Checking service health endpoints..."

# Check PostgreSQL
print_info "Checking PostgreSQL..."
docker exec payu-postgres-test pg_isready -U payu_test > /dev/null 2>&1
print_status $? "PostgreSQL is ready"

# Check Redis
print_info "Checking Redis..."
docker exec payu-redis-test redis-cli ping > /dev/null 2>&1
print_status $? "Redis is ready"

# Check TimescaleDB
print_info "Checking TimescaleDB..."
docker exec payu-timescaledb-test pg_isready -U payu_test > /dev/null 2>&1
print_status $? "TimescaleDB is ready"

# Check Kafka
print_info "Checking Kafka..."
docker exec payu-kafka-test kafka-broker-api-versions --bootstrap-server localhost:9093 > /dev/null 2>&1
print_status $? "Kafka is ready"

# Check Keycloak
print_info "Checking Keycloak..."
if command -v curl &> /dev/null; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8100/health 2>/dev/null || echo "000")
    if [[ "$STATUS" == "200" ]] || [[ "$STATUS" == "204" ]]; then
        print_status 0 "Keycloak is accessible"
    else
        print_status 1 "Keycloak not accessible (HTTP $STATUS)"
        UNHEALTHY_SERVICES+=("keycloak-test")
    fi
else
    print_warning "curl not available, skipping Keycloak HTTP check"
fi

echo ""
echo "Step 4: Checking core banking service health..."

# Helper function to check Spring Boot service health
check_spring_service() {
    local service_name=$1
    local port=$2
    local container=$3

    if command -v curl &> /dev/null; then
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health/liveness 2>/dev/null || echo "000")
        if [[ "$STATUS" == "200" ]] || [[ "$STATUS" == "204" ]]; then
            print_status 0 "$service_name is healthy"
        else
            print_status 1 "$service_name not healthy (HTTP $STATUS)"
            UNHEALTHY_SERVICES+=("$container")
        fi
    else
        print_warning "curl not available, skipping $service_name HTTP check"
    fi
}

# Helper function to check Quarkus service health
check_quarkus_service() {
    local service_name=$1
    local port=$2
    local container=$3

    if command -v curl &> /dev/null; then
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/q/health 2>/dev/null || echo "000")
        if [[ "$STATUS" == "200" ]] || [[ "$STATUS" == "204" ]]; then
            print_status 0 "$service_name is healthy"
        else
            print_status 1 "$service_name not healthy (HTTP $STATUS)"
            UNHEALTHY_SERVICES+=("$container")
        fi
    else
        print_warning "curl not available, skipping $service_name HTTP check"
    fi
}

# Helper function to check Python service health
check_python_service() {
    local service_name=$1
    local port=$2
    local container=$3

    if command -v curl &> /dev/null; then
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/health 2>/dev/null || echo "000")
        if [[ "$STATUS" == "200" ]] || [[ "$STATUS" == "204" ]]; then
            print_status 0 "$service_name is healthy"
        else
            print_status 1 "$service_name not healthy (HTTP $STATUS)"
            UNHEALTHY_SERVICES+=("$container")
        fi
    else
        print_warning "curl not available, skipping $service_name HTTP check"
    fi
}

check_spring_service "Account Service" 8101 "account-service-test"
check_spring_service "Auth Service" 8102 "auth-service-test"
check_spring_service "Transaction Service" 8103 "transaction-service-test"
check_spring_service "Wallet Service" 8104 "wallet-service-test"
check_quarkus_service "Billing Service" 8105 "billing-service-test"
check_quarkus_service "Notification Service" 8106 "notification-service-test"
check_quarkus_service "Gateway Service" 8180 "gateway-service-test"
check_python_service "KYC Service" 8107 "kyc-service-test"
check_python_service "Analytics Service" 8108 "analytics-service-test"

echo ""
echo "Step 5: Verifying test databases..."
DBS=$(docker exec payu-postgres-test psql -U payu_test -c "\l" 2>/dev/null | grep payu_test | wc -l)
if [ $DBS -ge 15 ]; then
    print_status 0 "Test databases created ($DBS databases)"
else
    print_status 1 "Expected at least 15 test databases, found $DBS"
fi

echo ""
echo "Step 6: Checking for test data..."
TEST_USER_COUNT=$(docker exec payu-postgres-test psql -U payu_test -d payu_test_account -c "SELECT COUNT(*) FROM test_users;" -t 2>/dev/null || echo "0")
if [ "$TEST_USER_COUNT" -gt 0 ]; then
    print_status 0 "Test data found ($TEST_USER_COUNT test users)"
else
    print_warning "No test data found. Run ./scripts/seed-test-data.sh to populate test data."
fi

# Final Summary
echo ""
echo "=========================================="
echo "Health Check Summary"
echo "=========================================="

if [ ${#FAILED_SERVICES[@]} -eq 0 ] && [ ${#UNHEALTHY_SERVICES[@]} -eq 0 ]; then
    echo -e "${GREEN}✅ All test services are healthy!${NC}"
    echo ""
    echo "Test environment is ready for testing."
    echo ""
    echo "Available endpoints:"
    echo "  - Gateway:        http://localhost:8180"
    echo "  - Account:        http://localhost:8101"
    echo "  - Auth:           http://localhost:8102"
    echo "  - Transaction:    http://localhost:8103"
    echo "  - Wallet:         http://localhost:8104"
    echo "  - Billing:        http://localhost:8105"
    echo "  - Notification:   http://localhost:8106"
    echo "  - KYC:            http://localhost:8107"
    echo "  - Analytics:      http://localhost:8108"
    echo "  - Web App:        http://localhost:3101"
    echo "  - Keycloak:       http://localhost:8100"
    echo "  - Jaeger:         http://localhost:16687"
    echo "  - Prometheus:     http://localhost:9190"
    exit 0
else
    echo -e "${RED}❌ Some services are not healthy${NC}"
    if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
        echo ""
        echo -e "${RED}Failed services:${NC}"
        for svc in "${FAILED_SERVICES[@]}"; do
            echo "  - $svc"
        done
    fi
    if [ ${#UNHEALTHY_SERVICES[@]} -gt 0 ]; then
        echo ""
        echo -e "${YELLOW}Unhealthy services:${NC}"
        for svc in "${UNHEALTHY_SERVICES[@]}"; do
            echo "  - $svc"
        done
    fi
    echo ""
    echo "Check logs with: $COMPOSE_CMD logs <service-name>"
    exit 1
fi
