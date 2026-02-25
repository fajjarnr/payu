#!/bin/bash

# PayU Platform - Build All Services with Podman
# ============================================
# This script builds all PayU microservices in parallel using Podman
# Requirements: podman, buildah, jq
#
# Usage: ./build-all-podman.sh [OPTIONS]
#   -t, --tag TAG          Tag for all images (default: payu:latest)
#   -j, --jobs JOBS        Number of parallel build jobs (default: 4)
#   -p, --push             Push images to registry after build
#   -r, --registry REG     Registry to push to (default: localhost)
#   -v, --verbose          Verbose output
#   -h, --help             Show this help message

set -euo pipefail

# Configuration
DEFAULT_TAG="payu:latest"
DEFAULT_JOBS=4
DEFAULT_REGISTRY="localhost"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_help() {
    cat << EOF
PayU Platform - Build All Services with Podman

This script builds all PayU microservices in parallel using Podman.

Usage: $0 [OPTIONS]

Options:
  -t, --tag TAG          Tag for all images (default: $DEFAULT_TAG)
  -j, --jobs JOBS        Number of parallel build jobs (default: $DEFAULT_JOBS)
  -p, --push             Push images to registry after build
  -r, --registry REG     Registry to push to (default: $DEFAULT_REGISTRY)
  -v, --verbose          Verbose output
  -h, --help             Show this help message

Environment Variables:
  PODMAN_USERNS         User namespace setting (default: keep-id)
  PODMAN_NETWORK        Network to use (default: podman)
  BUILDKIT_BUILD       BuildKit build context (default: 1)

Examples:
  $0                          # Build all with default settings
  $0 -t payu:v1.0.0 -j 8     # Build with custom tag and 8 jobs
  $0 -p -r docker.io/myorg    # Build and push to Docker Hub
EOF
}

# Parse arguments
TAG="$DEFAULT_TAG"
JOBS="$DEFAULT_JOBS"
PUSH=false
REGISTRY="$DEFAULT_REGISTRY"
VERBOSE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--tag)
            TAG="$2"
            shift 2
            ;;
        -j|--jobs)
            JOBS="$2"
            shift 2
            ;;
        -p|--push)
            PUSH=true
            shift
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Check dependencies
check_dependencies() {
    local missing_deps=()

    for cmd in podman buildah jq; do
        if ! command -v "$cmd" >/dev/null 2>&1; then
            missing_deps+=("$cmd")
        fi
    done

    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        log_error "Missing dependencies: ${missing_deps[*]}"
        log_error "Please install: ${missing_deps[*]}"
        exit 1
    fi
}

# Get service list with Containerfiles
get_services() {
    find "$PROJECT_ROOT" -name "Containerfile" -type f | \
        grep -E "(backend/|frontend/|tests/)" | \
        sort | \
        while read -r containerfile; do
            local service_name
            service_name=$(echo "$containerfile" | sed 's|/Containerfile||' | xargs basename)
            echo "$service_name:$containerfile"
        done
}

# Build single service
build_service() {
    local service_name="$1"
    local containerfile="$2"
    local context_dir="$(dirname "$containerfile")"
    local full_tag="${REGISTRY}/${service_name}:${TAG}"

    log_info "Building ${service_name}..."
    log_info "Containerfile: ${containerfile}"

    # Buildah build command with advanced features
    local buildah_cmd=(
        buildah build-using-dockerfile
        --tag "${full_tag}"
        --file "${containerfile}"
        --contextdir "${context_dir}"
        --no-cache
        --format docker
        --log-level info
        --jobs "${JOBS}"
    )

    # Add platform-specific build options
    if [[ -n "${PODMAN_PLATFORM:-}" ]]; then
        buildah_cmd+=("--platform=${PODMAN_PLATFORM}")
    fi

    # Add security options
    buildah_cmd+=(
        --security-opt label=disable
        --security-opt no-new-privileges
        --cap-drop ALL
        --cap-add CAP_NET_BIND_SERVICE
    )

    # Add network options
    if [[ -n "${PODMAN_NETWORK:-}" ]]; then
        buildah_cmd+=("--network=${PODMAN_NETWORK}")
    fi

    # Execute build
    if [[ "$VERBOSE" == true ]]; then
        log_info "Build command: ${buildah_cmd[*]}"
    fi

    if "${buildah_cmd[@]}"; then
        log_success "✓ Built ${service_name} (${full_tag})"
        echo "${full_tag}"
    else
        log_error "✗ Failed to build ${service_name}"
        return 1
    fi
}

# Main execution
main() {
    log_info "PayU Platform - Build All Services with Podman"
    log_info "============================================="
    log_info "Tag: ${TAG}"
    log_info "Parallel jobs: ${JOBS}"
    log_info "Registry: ${REGISTRY}"
    log_info "Push after build: ${PUSH}"

    # Check dependencies
    check_dependencies

    # Get services
    log_info "Discovering services..."
    local services
    services=$(get_services)

    if [[ -z "$services" ]]; then
        log_error "No services found with Containerfiles"
        exit 1
    fi

    local service_count=$(echo "$services" | wc -l)
    log_info "Found ${service_count} services to build:"
    echo "$services" | sed 's/^/  - /'

    # Build in parallel
    log_info "Starting parallel build with ${JOBS} jobs..."

    # Create pod for builds if using Podman
    local pod_name="payu-build-$$"
    if [[ "${PODMAN_USE_POD:-false}" == "true" ]]; then
        log_info "Creating build pod: ${pod_name}"
        podman pod create --name "${pod_name}" --share net
    fi

    # Build services in parallel
    local pids=()
    local build_results=()

    while read -r service_line; do
        local service_name=$(echo "$service_line" | cut -d: -f1)
        local containerfile=$(echo "$service_line" | cut -d: -f2-)

        # Build in subshell
        (
            local result
            result=$(build_service "$service_name" "$containerfile")
            echo "$result"
        ) &

        pids+=($!)
        build_results+=("${service_name}:$!")
    done <<< "$services"

    # Wait for all builds
    local success_count=0
    local fail_count=0
    local built_images=()

    for i in "${!pids[@]}"; do
        local pid=${pids[$i]}
        local service_name=${build_results[$i]%%:*}

        if wait "$pid"; then
            ((success_count++))
        else
            ((fail_count++))
        fi
    done

    # Clean up pod
    if [[ "${PODMAN_USE_POD:-false}" == "true" ]]; then
        log_info "Cleaning up build pod..."
        podman pod stop "${pod_name}" >/dev/null 2>&1 || true
        podman pod rm "${pod_name}" >/dev/null 2>&1 || true
    fi

    # Summary
    log_info "Build Summary:"
    log_info "  Total services: ${service_count}"
    log_info "  ✓ Success: ${success_count}"
    log_info "  ✗ Failed: ${fail_count}"

    if [[ $fail_count -gt 0 ]]; then
        log_error "Build failed for ${fail_count} services"
        exit 1
    fi

    # Push images if requested
    if [[ "$PUSH" == true ]]; then
        log_info "Pushing images to ${REGISTRY}..."

        for image in "${built_images[@]}"; do
            log_info "Pushing ${image}..."
            if podman push "${image}"; then
                log_success "✓ Pushed ${image}"
            else
                log_error "✗ Failed to push ${image}"
            fi
        done
    fi

    log_success "All services built successfully!"
}

# Execute main function
main "$@"
