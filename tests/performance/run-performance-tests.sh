#!/bin/bash

# PayU Performance Load Testing Runner
# This script provides an easy interface to run Gatling performance tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Default values
BASE_URL="${BASE_URL:-http://localhost:8080}"
SIMULATION="AllServicesSimulation"
PROFILE=""

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    cat << EOF
PayU Performance Load Testing Runner

Usage: $0 [OPTIONS]

Options:
    -s, --simulation SIMULATION    Simulation to run
                                   Available: login, transfer, qris, balance, all
                                   Default: all
    -u, --url URL                  Base URL for the services
                                   Default: http://localhost:8080
    -h, --help                     Show this help message

Examples:
    # Run all simulations
    $0

    # Run login simulation only
    $0 -s login

    # Run against staging environment
    $0 -u https://staging-api.payu.id

    # Run transfer simulation against staging
    $0 -s transfer -u https://staging-api.payu.id

Available Simulations:
    login       - Authentication service performance test
    transfer    - BI-FAST transfer performance test
    qris        - QRIS payment performance test
    balance     - Balance query performance test
    all         - Comprehensive test with all services (default)

EOF
}

# Function to validate services are running
check_services() {
    print_info "Checking if services are accessible at $BASE_URL..."

    if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" | grep -q "200\|404"; then
        print_success "Services are accessible"
        return 0
    else
        print_warning "Services may not be accessible at $BASE_URL"
        print_warning "Continuing anyway..."
        return 0
    fi
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--simulation)
            SIMULATION="$2"
            shift 2
            ;;
        -u|--url)
            BASE_URL="$2"
            shift 2
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Map simulation name to Maven profile or class
case $SIMULATION in
    login)
        PROFILE="-Plogin"
        SIMULATION_CLASS="id.payu.simulations.LoginSimulation"
        ;;
    transfer)
        PROFILE="-Ptransfer"
        SIMULATION_CLASS="id.payu.simulations.TransferSimulation"
        ;;
    qris)
        PROFILE="-Pqris"
        SIMULATION_CLASS="id.payu.simulations.QRISPaymentSimulation"
        ;;
    balance)
        PROFILE="-Pbalance"
        SIMULATION_CLASS="id.payu.simulations.BalanceQuerySimulation"
        ;;
    all)
        PROFILE=""
        SIMULATION_CLASS="id.payu.simulations.AllServicesSimulation"
        ;;
    *)
        print_error "Unknown simulation: $SIMULATION"
        show_usage
        exit 1
        ;;
esac

# Print configuration
print_info "=========================================="
print_info "PayU Performance Load Testing"
print_info "=========================================="
print_info "Simulation: $SIMULATION ($SIMULATION_CLASS)"
print_info "Base URL: $BASE_URL"
print_info "=========================================="

# Check if services are running
check_services

# Build Maven command
MAVEN_CMD="mvn clean gatling:test $PROFILE -DbaseUrl=$BASE_URL"

print_info "Starting performance test..."
print_info "Maven command: $MAVEN_CMD"
echo ""

# Run the test
if eval $MAVEN_CMD; then
    echo ""
    print_success "Performance test completed successfully!"
    echo ""

    # Find the latest report
    LATEST_REPORT=$(ls -td target/gatling/results/*/ 2>/dev/null | head -1)

    if [ -n "$LATEST_REPORT" ]; then
        print_success "Report location: $SCRIPT_DIR/$LATEST_REPORT"
        print_info "Opening report in browser..."

        # Try to open the report based on OS
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            open "$SCRIPT_DIR/$LATEST_REPORT/index.html"
        elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
            # Linux
            if command -v xdg-open &> /dev/null; then
                xdg-open "$SCRIPT_DIR/$LATEST_REPORT/index.html"
            else
                print_info "Please open the report manually: $SCRIPT_DIR/$LATEST_REPORT/index.html"
            fi
        else
            print_info "Please open the report manually: $SCRIPT_DIR/$LATEST_REPORT/index.html"
        fi
    else
        print_warning "No report found. Check if the test ran successfully."
    fi

    exit 0
else
    echo ""
    print_error "Performance test failed!"
    print_info "Check the logs above for details."
    exit 1
fi
