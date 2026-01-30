#!/bin/bash

# PayU Podman Compose Management Script
# Provides commands to manage Podman services, networks, and volumes

set -e

PROJECT_ROOT="/home/ubuntu/payu"
CONTAINERS_DIR="$PROJECT_ROOT/containers"
COMPOSE_FILE="$PROJECT_ROOT/podman-compose.yml"
TEST_COMPOSE_FILE="$PROJECT_ROOT/podman-compose.test.yml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if podman is installed
check_podman() {
    if ! command -v podman &> /dev/null; then
        print_error "Podman is not installed. Please install Podman first."
        exit 1
    fi
}

# Create environment file if it doesn't exist
create_env_file() {
    if [ ! -f "$PROJECT_ROOT/.env" ]; then
        print_warning ".env file not found. Creating template..."
        cat > "$PROJECT_ROOT/.env" << EOF
# PostgreSQL
POSTGRES_USER=payu
POSTGRES_PASSWORD=payu_secret
POSTGRES_DB=payu_account

# Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_DB_PASSWORD=payu_secret

# Test Environment
TEST_POSTGRES_USER=payu_test
TEST_POSTGRES_PASSWORD=test_secret
TEST_POSTGRES_DB=payu_test_account
TEST_KEYCLOAK_DB_PASSWORD=test_secret

# Monitoring
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin

# Vault
VAULT_DEV_ROOT_TOKEN_ID=dev-only-token
EOF
        print_info "Created .env template. Please review and update with your values."
    fi
}

# Check services status
check_status() {
    print_info "Checking Podman services status..."
    echo "==============================================="
    podman play ps | grep payu || print_warning "No PayU services running"
    echo "==============================================="
    echo ""
    print_info "Systemd services status:"
    systemctl list-units --type=target | grep -E "(podman|payu)" || echo "No podman targets found"
}

# Start development environment
start_dev() {
    print_info "Starting PayU development environment..."
    check_podman
    create_env_file

    # Create networks if they don't exist
    if ! podman network exists payu-network; then
        podman network create payu-network
        print_info "Created payu-network"
    fi

    if ! podman network exists payu-test-network; then
        podman network create payu-test-network
        print_info "Created payu-test-network"
    fi

    # Start services
    if [ -f "$COMPOSE_FILE" ]; then
        podman play "$COMPOSE_FILE"
        print_info "Started development environment"
    else
        print_error "Compose file not found: $COMPOSE_FILE"
        exit 1
    fi
}

# Start test environment
start_test() {
    print_info "Starting PayU test environment..."
    check_podman

    if [ -f "$TEST_COMPOSE_FILE" ]; then
        podman play "$TEST_COMPOSE_FILE"
        print_info "Started test environment"
    else
        print_error "Test compose file not found: $TEST_COMPOSE_FILE"
        exit 1
    fi
}

# Stop all services
stop_all() {
    print_info "Stopping all PayU services..."
    podman down --all
    print_info "All services stopped"
}

# Stop specific service
stop_service() {
    local service=$1
    if [ -z "$service" ]; then
        print_error "Please specify a service name to stop"
        exit 1
    fi

    print_info "Stopping service: $service"
    podman stop "$service" || print_warning "Service $service not found"
    podman rm "$service" || true
}

# View logs
logs() {
    local service=$1
    if [ -z "$service" ]; then
        print_error "Please specify a service name to view logs"
        print_info "Usage: $0 logs <service>"
        exit 1
    fi

    print_info "Logs for service: $service"
    podman logs -f "$service"
}

# Build services
build() {
    print_info "Building PayU services..."
    podman compose --file "$COMPOSE_FILE" build
    podman compose --file "$TEST_COMPOSE_FILE" build
    print_info "Services built"
}

# Clean up volumes
clean_volumes() {
    print_warning "This will remove all PayU volumes and data!"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "Cleaning up PayU volumes..."
        podman volume rm $(podman volume ls | grep payu | awk '{print $2}') || true
        print_info "Volumes cleaned"
    else
        print_info "Cleanup cancelled"
    fi
}

# Deploy quadlet files
deploy_quadlet() {
    print_info "Deploying quadlet files to systemd..."

    # Check if we have root privileges
    if [ "$EUID" -ne 0 ]; then
        print_error "This command requires root privileges. Use sudo."
        exit 1
    fi

    # Copy quadlet files
    cp "$CONTAINERS_DIR"/*.container /etc/containers/systemd/
    cp "$CONTAINERS_DIR"/*.network /etc/containers/systemd/
    cp "$CONTAINERS_DIR"/*.volume /etc/containers/systemd/
    cp "$CONTAINERS_DIR"/*.service /etc/systemd/system/
    cp "$CONTAINERS_DIR"/*.target /etc/systemd/system/

    # Reload systemd
    systemctl daemon-reload

    # Enable and start the service
    systemctl enable podman-payu.service
    systemctl start podman-payu.service

    print_info "Quadlet files deployed successfully"
}

# Show help
show_help() {
    cat << EOF
PayU Podman Compose Management Script

Usage: $0 <command> [options]

Commands:
    start-dev      Start development environment
    start-test     Start test environment
    stop           Stop all services
    stop <service>  Stop specific service
    status         Check services status
    logs <service> View logs for service
    build          Build all services
    clean-volumes  Remove all PayU volumes
    deploy         Deploy quadlet files to systemd
    help           Show this help message

Examples:
    $0 start-dev          # Start development environment
    $0 start-test         # Start test environment
    $0 stop postgres      # Stop PostgreSQL only
    $0 logs keycloak      # View Keycloak logs
    $0 deploy             # Deploy quadlet files (requires sudo)
EOF
}

# Main script logic
case "${1:-help}" in
    start-dev)
        start_dev
        ;;
    start-test)
        start_test
        ;;
    stop)
        if [ -n "$2" ]; then
            stop_service "$2"
        else
            stop_all
        fi
        ;;
    status)
        check_status
        ;;
    logs)
        logs "$2"
        ;;
    build)
        build
        ;;
    clean-volumes)
        clean_volumes
        ;;
    deploy)
        deploy_quadlet
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac
