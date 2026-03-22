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
COMPOSE_FILE="infrastructure/local-podman/podman-compose.yml"
if docker-compose --version > /dev/null 2>&1; then
    COMPOSE_CMD="docker-compose -f $COMPOSE_FILE"
else
    COMPOSE_CMD="docker compose -f $COMPOSE_FILE"
fi

# Expected services (in dependency order)
EXPECTED_SERVICES=(
    "postgres"
    "redis"
    "kafka"
    "keycloak"
    "bi-fast-simulator"
    "dukcapil-simulator"
    "qris-simulator"
    "account-service"
    "auth-service"
    "transaction-service"
    "wallet-service"
    "billing-service"
    "notification-service"
    "gateway-service"
    "kyc-service"
    "analytics-service"
    "investment-service"
    "lending-service"
    "backoffice-service"
    "partner-service"
    "promotion-service"
    "support-service"
    "statement-service"
    "api-portal-service"
    "compliance-service"
    "cms-service"
    "fx-service"
    "dispute-service"
    "integration-service"
    "product-catalog-service"
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
docker exec payu-postgres pg_isready -U postgres > /dev/null 2>&1
print_status $? "PostgreSQL is ready"

# Check Redis
print_info "Checking Redis..."
docker exec payu-redis redis-cli ping > /dev/null 2>&1
print_status $? "Redis is ready"

# Check Kafka
print_info "Checking Kafka..."
docker exec payu-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1
print_status $? "Kafka is ready"

# Check Keycloak
print_info "Checking Keycloak..."
if command -v curl &> /dev/null; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8099/health 2>/dev/null || echo "000")
    if [[ "$STATUS" == "200" ]] || [[ "$STATUS" == "204" ]]; then
        print_status 0 "Keycloak is accessible"
    else
        print_status 1 "Keycloak not accessible (HTTP $STATUS)"
        UNHEALTHY_SERVICES+=("keycloak")
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

check_spring_service "Account Service" 8001 "account-service"
check_spring_service "Auth Service" 8002 "auth-service"
check_spring_service "Transaction Service" 8003 "transaction-service"
check_spring_service "Wallet Service" 8004 "wallet-service"
check_quarkus_service "Billing Service" 8005 "billing-service"
check_quarkus_service "Notification Service" 8006 "notification-service"
check_python_service "KYC Service" 8007 "kyc-service"
check_python_service "Analytics Service" 8008 "analytics-service"
check_spring_service "Investment Service" 8009 "investment-service"
check_spring_service "Lending Service" 8010 "lending-service"
check_quarkus_service "Backoffice Service" 8011 "backoffice-service"
check_quarkus_service "Partner Service" 8012 "partner-service"
check_quarkus_service "Promotion Service" 8013 "promotion-service"
check_quarkus_service "Support Service" 8014 "support-service"
check_spring_service "Statement Service" 8015 "statement-service"
check_quarkus_service "API Portal Service" 8021 "api-portal-service"
check_quarkus_service "Gateway Service" 8080 "gateway-service"
check_spring_service "Compliance Service" 8087 "compliance-service"
check_spring_service "CMS Service" 8095 "cms-service"
check_spring_service "FX Service" 8096 "fx-service"
check_spring_service "Dispute Service" 8098 "dispute-service"
check_spring_service "Product Catalog Service" 8100 "product-catalog-service"
check_spring_service "Integration Service" 8101 "integration-service"

echo ""
echo "Step 5: Verifying test databases..."
DBS=$(docker exec payu-postgres psql -U postgres -c "\l" 2>/dev/null | grep payu | wc -l)
if [ $DBS -ge 10 ]; then
    print_status 0 "Databases found ($DBS databases)"
else
    print_status 1 "Expected at least 10 databases, found $DBS"
fi

echo ""
echo "Step 6: Checking for test data..."
TEST_USER_COUNT=$(docker exec payu-postgres psql -U postgres -d accountdb -c "SELECT COUNT(*) FROM users;" -t 2>/dev/null || echo "0")
if [ "$TEST_USER_COUNT" -gt 0 ]; then
    print_status 0 "Data found ($TEST_USER_COUNT users)"
else
    print_warning "No data found. Run ./scripts/seed-test-data.sh to populate."
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
    echo "  - Account:        http://localhost:8001"
    echo "  - Auth:           http://localhost:8002"
    echo "  - Transaction:    http://localhost:8003"
    echo "  - Wallet:         http://localhost:8004"
    echo "  - Billing:        http://localhost:8005"
    echo "  - Notification:   http://localhost:8006"
    echo "  - KYC:            http://localhost:8007"
    echo "  - Analytics:      http://localhost:8008"
    echo "  - Investment:     http://localhost:8009"
    echo "  - Lending:        http://localhost:8010"
    echo "  - Backoffice:     http://localhost:8011"
    echo "  - Partner:        http://localhost:8012"
    echo "  - Promotion:      http://localhost:8013"
    echo "  - Support:        http://localhost:8014"
    echo "  - Statement:      http://localhost:8015"
    echo "  - API Portal:     http://localhost:8021"
    echo "  - Gateway:        http://localhost:8080"
    echo "  - Compliance:     http://localhost:8087"
    echo "  - CMS:            http://localhost:8095"
    echo "  - FX:             http://localhost:8096"
    echo "  - Dispute:        http://localhost:8098"
    echo "  - Product Catalog:http://localhost:8100"
    echo "  - Integration:    http://localhost:8101"
    echo "  - Keycloak:       http://localhost:8099"
    echo "  - Jaeger:         http://localhost:16686"
    echo "  - Prometheus:     http://localhost:9090"
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
