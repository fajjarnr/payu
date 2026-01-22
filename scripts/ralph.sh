#!/bin/bash

# =============================================================================
# PayU Development, Testing, and Debugging Utilities (The "Ralph" Script)
# "Me fail English? That's unpossible." - Ralph Wiggum
# =============================================================================

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_ROOT="/home/jay/payu"
BACKEND_DIR="${PROJECT_ROOT}/backend"

show_help() {
    echo -e "${BLUE}PayU Ralph Utility Script${NC}"
    echo "Usage: ./scripts/ralph.sh [command]"
    echo ""
    echo "Commands:"
    echo "  status         Show status of all microservices"
    echo "  up             Start development infrastructure (Docker)"
    echo "  down           Stop development infrastructure"
    echo "  test           Run tests for all services"
    echo "  test [svc]     Run tests for a specific service (e.g., account-service)"
    echo "  build          Build all Java services (Maven)"
    echo "  build [svc]    Build a specific Java service"
    echo "  logs [svc]     Follow logs for a specific service"
    echo "  doctor         Check if all required tools are installed"
    echo "  quotes         Get a random Ralph Wiggum quote (Original feature!)"
    echo ""
}

show_quotes() {
    QUOTES=(
        "I'm in danger."
        "My cat's breath smells like cat food."
        "I'm helping!"
        "Me fail English? That's unpossible."
        "I bent my wookiee."
        "It tastes like burning!"
        "Hi, Super Nintendo Chalmers!"
        "Oh boy, sleep! That's where I'm a Viking!"
    )
    RANDOM_QUOTE=${QUOTES[$RANDOM % ${#QUOTES[@]}]}
    echo -e "${YELLOW}Ralph says: \"$RANDOM_QUOTE\"${NC}"
}

check_doctor() {
    echo -e "${BLUE}Checking system requirements...${NC}"
    TOOLS=("java" "mvn" "python3" "docker" "oc" "psql")
    for tool in "${TOOLS[@]}"; do
        if command -v $tool >/dev/null 2>&1; then
            echo -e "${GREEN}✓ $tool${NC} $(eval $tool --version | head -n 1)"
        else
            echo -e "${RED}✗ $tool is missing${NC}"
        fi
    done
}

run_status() {
    if [ -f "${BACKEND_DIR}/SERVICES_STATUS.md" ]; then
        cat "${BACKEND_DIR}/SERVICES_STATUS.md"
    else
        echo -e "${RED}SERVICES_STATUS.md not found.${NC}"
    fi
}

run_build() {
    SERVICE=$1
    if [ -z "$SERVICE" ]; then
        echo -e "${BLUE}Building all Java services...${NC}"
        # Core Banking (Spring Boot)
        for svc in account-service auth-service transaction-service wallet-service; do
            echo -e "${YELLOW}Building $svc...${NC}"
            cd "${BACKEND_DIR}/$svc" && mvn clean package -DskipTests
        done
        # Supporting (Quarkus)
        for svc in billing-service notification-service gateway-service; do
            echo -e "${YELLOW}Building $svc...${NC}"
            cd "${BACKEND_DIR}/$svc" && mvn clean package -DskipTests
        done
    else
        echo -e "${BLUE}Building $SERVICE...${NC}"
        if [ -d "${BACKEND_DIR}/$SERVICE" ]; then
            cd "${BACKEND_DIR}/$SERVICE" && mvn clean package -DskipTests
        else
            echo -e "${RED}Service $SERVICE not found.${NC}"
        fi
    fi
}

run_test() {
    SERVICE=$1
    if [ -z "$SERVICE" ]; then
        echo -e "${BLUE}Running all tests...${NC}"
        # Python tests
        bash "${PROJECT_ROOT}/run_tests.sh"
        # Java tests (simplified call)
        echo -e "${YELLOW}Running Java service tests...${NC}"
        for svc in account-service auth-service transaction-service wallet-service billing-service notification-service gateway-service; do
            echo -e "${YELLOW}Testing $svc...${NC}"
            cd "${BACKEND_DIR}/$svc" && mvn test || echo -e "${RED}$svc tests failed${NC}"
        done
    else
        echo -e "${BLUE}Testing $SERVICE...${NC}"
        if [ -d "${BACKEND_DIR}/$SERVICE" ]; then
            cd "${BACKEND_DIR}/$SERVICE"
            if [ -f "pom.xml" ]; then
                mvn test
            elif [ -f "requirements.txt" ]; then
                python3 -m pytest
            fi
        else
            echo -e "${RED}Service $SERVICE not found.${NC}"
        fi
    fi
}

case "$1" in
    status) run_status ;;
    up) docker compose up -d ;;
    down) docker compose down ;;
    build) run_build "$2" ;;
    test) run_test "$2" ;;
    logs) docker compose logs -f "$2" ;;
    doctor) check_doctor ;;
    quotes) show_quotes ;;
    *) show_help ;;
esac
