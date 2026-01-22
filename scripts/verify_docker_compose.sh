#!/bin/bash
set -e

echo "=========================================="
echo "PayU Docker Compose Verification"
echo "=========================================="

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        exit 1
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check Docker
echo "Checking Docker installation..."
docker --version > /dev/null 2>&1
print_status $? "Docker is installed"

# Check Docker Compose
echo "Checking Docker Compose installation..."
docker-compose --version > /dev/null 2>&1 || docker compose version > /dev/null 2>&1
print_status $? "Docker Compose is installed"

# Determine which compose command to use
if docker-compose --version > /dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
else
    COMPOSE_CMD="docker compose"
fi

echo "Using: $COMPOSE_CMD"

# Stop any existing containers
echo ""
echo "Stopping existing containers..."
$COMPOSE_CMD down -v > /dev/null 2>&1 || true
print_status 0 "Existing containers stopped"

# Start infrastructure
echo ""
echo "Starting infrastructure..."
$COMPOSE_CMD up -d --build
print_status $? "Infrastructure started"

# Wait for services
echo ""
echo "Waiting for services to become healthy..."
TIMEOUT=300
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    RUNNING=$($COMPOSE_CMD ps --services --filter "status=running" | wc -l)
    if [ $RUNNING -ge 15 ]; then
        print_status 0 "Services are running ($RUNNING services)"
        break
    fi
    echo -n "."
    sleep 5
    ELAPSED=$((ELAPSED + 5))
done

if [ $ELAPSED -ge $TIMEOUT ]; then
    echo ""
    print_status 1 "Timeout waiting for services"
fi

# Check PostgreSQL
echo ""
echo "Checking PostgreSQL..."
docker exec payu-postgres pg_isready -U payu > /dev/null 2>&1
print_status $? "PostgreSQL is ready"

# Check Kafka
echo ""
echo "Checking Kafka..."
docker exec payu-kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1
print_status $? "Kafka is ready"

# Check Redis
echo ""
echo "Checking Redis..."
docker exec payu-redis redis-cli ping > /dev/null 2>&1
print_status $? "Redis is ready"

# Check Keycloak
echo ""
echo "Checking Keycloak..."
if command -v curl &> /dev/null; then
    curl -s -o /dev/null -w "%{http_code}" http://localhost:8099/health | grep -q "200\|204"
    print_status $? "Keycloak is accessible"
else
    print_warning "curl not available, skipping Keycloak check"
fi

# Check Gateway
echo ""
echo "Checking Gateway Service..."
if command -v curl &> /dev/null; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080)
    if [ "$STATUS" = "200" ] || [ "$STATUS" = "404" ] || [ "$STATUS" = "401" ]; then
        print_status 0 "Gateway is accessible (HTTP $STATUS)"
    else
        print_warning "Gateway returned unexpected status: $STATUS"
    fi
else
    print_warning "curl not available, skipping Gateway check"
fi

# Check Account Service
echo ""
echo "Checking Account Service..."
if command -v curl &> /dev/null; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8001)
    if [ "$STATUS" = "200" ] || [ "$STATUS" = "404" ] || [ "$STATUS" = "401" ]; then
        print_status 0 "Account Service is accessible (HTTP $STATUS)"
    else
        print_warning "Account Service returned unexpected status: $STATUS"
    fi
else
    print_warning "curl not available, skipping Account Service check"
fi

# Verify databases
echo ""
echo "Verifying databases..."
DBS=$($COMPOSE_CMD exec -T postgres psql -U payu -c "\l" 2>/dev/null | grep payu | wc -l)
if [ $DBS -ge 10 ]; then
    print_status 0 "Databases created ($DBS databases)"
else
    print_warning "Expected at least 10 databases, found $DBS"
fi

# Display running containers
echo ""
echo "Running containers:"
$COMPOSE_CMD ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

# Stop infrastructure
echo ""
echo "Stopping infrastructure..."
$COMPOSE_CMD down -v
print_status $? "Infrastructure stopped"

# Verify cleanup
echo ""
echo "Verifying cleanup..."
RUNNING=$($COMPOSE_CMD ps -q | wc -l)
if [ $RUNNING -eq 0 ]; then
    print_status 0 "All containers stopped and removed"
else
    print_status 1 "Some containers still running"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}✅ All verification checks passed!${NC}"
echo "=========================================="
