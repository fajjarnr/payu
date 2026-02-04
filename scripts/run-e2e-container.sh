#!/bin/bash
# E2E Test Runner for PayU Platform
# Uses Podman Compose to run full-stack E2E tests

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
WEB_APP_DIR="$PROJECT_ROOT/frontend/web-app"

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Parse arguments
MODE="${1:-quick}"  # quick, full, or smoke
SKIP_BUILD="${2:-false}"

log_info "PayU E2E Test Runner"
log_info "Mode: $MODE"
log_info "Skip Build: $SKIP_BUILD"
echo ""

# Step 1: Check if services are running
log_info "Step 1: Checking service status..."
RUNNING_SERVICES=$(podman ps --format "{{.Names}}" | grep "payu-" | wc -l)
log_info "Currently running services: $RUNNING_SERVICES"

if [ "$RUNNING_SERVICES" -lt 5 ]; then
    log_warning "Less than 5 services running. Starting core services..."
    cd "$PROJECT_ROOT"
    podman compose up -d postgres redis kafka zookeeper jaeger 2>&1 | tail -10
    log_info "Waiting for infrastructure to be ready..."
    sleep 20
fi

# Step 2: Build/Start the web-app
log_info "Step 2: Starting web-app..."
cd "$PROJECT_ROOT"

# Check if web-app image exists, build if needed
if ! podman images | grep -q "payu_web-app"; then
    log_warning "Web-app image not found. Building..."
    podman compose build web-app 2>&1 | tail -20
fi

# Tag the image
podman tag localhost/payu_web-app:latest payu_web-app:latest 2>/dev/null || true

# Start web-app (without gateway dependency for now)
log_info "Starting web-app container..."
podman run -d \
    --name payu-web-app \
    --network payu_payu-network \
    --network-alias web-app \
    -p 3001:3000 \
    -e NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1 \
    -e NODE_ENV=production \
    payu_web-app:latest \
    npm run start 2>&1 | tail -5 &

WEB_APP_PID=$!
log_info "Web-app started with PID: $WEB_APP_PID"

# Wait for web-app to be ready
log_info "Waiting for web-app to start (this may take 30-60 seconds)..."
MAX_WAIT=60
WAIT_COUNT=0
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if curl -s http://localhost:3001 > /dev/null 2>&1; then
        log_success "Web-app is ready!"
        break
    fi
    sleep 5
    WAIT_COUNT=$((WAIT_COUNT + 5))
    echo -n "."
done
echo ""

if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
    log_error "Web-app failed to start within $MAX_WAIT seconds"
    log_info "Checking web-app logs..."
    podman logs payu-web-app 2>&1 | tail -30
    exit 1
fi

# Step 3: Run E2E tests based on mode
log_info "Step 3: Running E2E tests..."
cd "$WEB_APP_DIR"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    log_info "Installing dependencies..."
    npm ci 2>&1 | tail -10
fi

# Install Playwright browsers if needed
if ! npx playwright --version > /dev/null 2>&1; then
    log_info "Installing Playwright browsers..."
    npx playwright install --with-deps 2>&1 | tail -10
fi

case "$MODE" in
    "smoke")
        log_info "Running smoke tests only..."
        npx playwright test e2e/check_ui.spec.ts --reporter=line
        ;;
    "quick")
        log_info "Running quick subset of tests..."
        npx playwright test e2e/login-flow.spec.ts --reporter=line
        ;;
    "full")
        log_info "Running full E2E test suite..."
        npx playwright test --reporter=html --reporter=line
        ;;
    *)
        log_error "Unknown mode: $MODE (use: smoke, quick, or full)"
        exit 1
        ;;
esac

# Capture test results
TEST_RESULT=$?

# Step 4: Cleanup
log_info "Step 4: Cleaning up..."
log_info "Stopping web-app container..."
podman stop payu-web-app 2>/dev/null || true
podman rm payu-web-app 2>/dev/null || true

# Report results
echo ""
log_info "E2E Test Results:"
if [ $TEST_RESULT -eq 0 ]; then
    log_success "All tests passed!"
else
    log_error "Some tests failed. Check the HTML report for details."
    log_info "Report: $WEB_APP_DIR/playwright-report/index.html"
fi

exit $TEST_RESULT
