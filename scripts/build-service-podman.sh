#!/bin/bash

# PayU Platform - Build Single Service with Podman
# ===============================================
# This script builds a single PayU microservice using Podman
# Requirements: podman, buildah
#
# Usage: ./build-service-podman.sh <SERVICE_NAME> [OPTIONS]
#   SERVICE_NAME           Name of the service to build
#   -t, --tag TAG          Tag for the image (default: payu:latest)
#   -p, --push             Push image to registry after build
#   -r, --registry REG     Registry to push to (default: localhost)
#   -v, --verbose          Verbose output
#   -h, --help             Show this help message

set -euo pipefail

# Configuration
DEFAULT_TAG="payu:latest"
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
PayU Platform - Build Single Service with Podman

This script builds a single PayU microservice using Podman.

Usage: $0 <SERVICE_NAME> [OPTIONS]

Arguments:
  SERVICE_NAME           Name of the service to build (e.g., account-service)

Options:
  -t, --tag TAG          Tag for the image (default: $DEFAULT_TAG)
  -p, --push             Push image to registry after build
  -r, --registry REG     Registry to push to (default: $DEFAULT_REGISTRY)
  -v, --verbose          Verbose output
  -h, --help             Show this help message

Environment Variables:
  PODMAN_PLATFORM       Platform to build for (e.g., linux/amd64)
  PODMAN_NETWORK        Network to use (default: podman)
  BUILD_ARGS            Additional build arguments (space-separated)

Examples:
  $0 account-service                # Build account-service
  $0 gateway-service -t v1.0.0     # Build with specific tag
  $0 wallet-service -p -r docker.io/myorg  # Build and push to Docker Hub
EOF
}

# Parse arguments
SERVICE_NAME=""
TAG="$DEFAULT_TAG"
PUSH=false
REGISTRY="$DEFAULT_REGISTRY"
VERBOSE=false

# Parse service name first (skip help option)
if [[ $# -eq 0 || "$1" == "-h" || "$1" == "--help" ]]; then
    show_help
    exit 0
fi

SERVICE_NAME="$1"
shift

# Parse options
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--tag)
            TAG="$2"
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

    for cmd in podman buildah; do
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

# Find Containerfile/Dockerfile for service (prefers Containerfile)
find_containerfile() {
    local service_name="$1"
    local containerfile

    # Try Containerfile first (Podman standard)
    containerfile=$(find "$PROJECT_ROOT" -name "Containerfile" -path "*/${service_name}/Containerfile" -type f | head -1)

    # If not found, try Dockerfile (backward compatibility)
    if [[ -z "$containerfile" ]]; then
        containerfile=$(find "$PROJECT_ROOT" -name "Dockerfile" -path "*/${service_name}/Dockerfile" -type f | head -1)
    fi

    # If not found, try service pattern with Containerfile
    if [[ -z "$containerfile" ]]; then
        containerfile=$(find "$PROJECT_ROOT" -name "Containerfile" -type f | grep -i "${service_name}" | head -1)
    fi

    # If not found, try service pattern with Dockerfile
    if [[ -z "$containerfile" ]]; then
        containerfile=$(find "$PROJECT_ROOT" -name "Dockerfile" -type f | grep -i "${service_name}" | head -1)
    fi

    # If still not found, look for partial matches
    if [[ -z "$containerfile" ]]; then
        local services
        services=$(find "$PROJECT_ROOT" -type f \( -name "Containerfile" -o -name "Dockerfile" \) | sort)

        for cf in $services; do
            if [[ "$(basename "$(dirname "$cf")" | grep -i "$service_name")" ]]; then
                containerfile="$cf"
                break
            fi
        done
    fi

    if [[ -z "$containerfile" ]]; then
        log_error "Containerfile/Dockerfile not found for service: $service_name"
        log_error "Available services:"
        find "$PROJECT_ROOT" -type f \( -name "Containerfile" -o -name "Dockerfile" \) | \
            while read -r cf; do
                echo "  - $(basename "$(dirname "$cf")")"
            done
        exit 1
    fi

    echo "$containerfile"
}

# Get build context
get_build_context() {
    local containerfile="$1"
    local context_dir

    # Check for build context file (check both .containerfile.context and .dockerfile.context)
    if [[ -f "${containerfile}.context" ]]; then
        context_dir=$(cat "${containerfile}.context")
        if [[ ! -d "$context_dir" ]]; then
            log_warning "Build context from file does not exist: $context_dir"
            context_dir="$(dirname "$containerfile")"
        fi
    else
        context_dir="$(dirname "$containerfile")"
    fi

    echo "$context_dir"
}

# Generate quadlet file
generate_quadlet() {
    local service_name="$1"
    local image_tag="$2"
    local output_dir="$3"

    local quadlet_file="${output_dir}/${service_name}.container"

    cat > "$quadlet_file" << EOF
# Generated quadlet file for ${service_name}
# This file can be used with systemd to manage the container

[Container]
Image=${image_tag}
ContainerName=${service_name}
AutoUpdate=registry
Network=podman
PodmanArgs=--network=host --label=io.payu.service=${service_name}
# Add your custom arguments below
# EnvironmentFile=/${service_name}/env
# Volume=/${service_name}/config:/etc/${service_name}:ro
# Volume=/${service_name}/data:/var/lib/${service_name}

[Service]
# Restart on failure, after 5 seconds
Restart=on-failure
RestartSec=5s

# Health check
ExecStartPre=/usr/bin/podman check-network podman
ExecStartPre=/usr/bin/podman pull ${image_tag}

# Set resource limits
# LimitMEM=512M
# LimitCPU=1.0

# Security settings
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true

[Install]
WantedBy=default.target
EOF

    log_success "Generated quadlet file: $quadlet_file"
}

# Build service
build_service() {
    local service_name="$1"
    local containerfile="$2"
    local context_dir="$3"
    local full_tag="${REGISTRY}/${service_name}:${TAG}"

    log_info "Building ${service_name}..."
    log_info "Containerfile: ${containerfile}"
    log_info "Context: ${context_dir}"
    log_info "Tag: ${full_tag}"

    # Create quadlet directory
    local quadlet_dir="$PROJECT_ROOT/infrastructure/quadlet"
    mkdir -p "$quadlet_dir"

    # Build command
    local build_cmd=(
        podman build
        --tag "${full_tag}"
        --file "${containerfile}"
        --context "${context_dir}"
    )

    # Add platform if specified
    if [[ -n "${PODMAN_PLATFORM:-}" ]]; then
        build_cmd+=("--platform=${PODMAN_PLATFORM}")
    fi

    # Add build arguments from environment
    if [[ -n "${BUILD_ARGS:-}" ]]; then
        for arg in $BUILD_ARGS; do
            build_cmd+=("--build-arg=$arg")
        done
    fi

    # Add multi-stage build support
    if [[ -n "${BUILD_STAGE:-}" ]]; then
        build_cmd+=("--target=${BUILD_STAGE}")
    fi

    # Add security options
    build_cmd+=(
        --security-opt label=disable
        --security-opt no-new-privileges
        --cap-drop ALL
        --cap-add CAP_NET_BIND_SERVICE
    )

    # Add network if specified
    if [[ -n "${PODMAN_NETWORK:-}" ]]; then
        build_cmd+=("--network=${PODMAN_NETWORK}")
    fi

    # Add parallel build if available
    if podman build --help | grep -q -- --jobs; then
        build_cmd+=("--jobs=${JOBS:-4}")
    fi

    # Execute build
    if [[ "$VERBOSE" == true ]]; then
        log_info "Build command: ${build_cmd[*]}"
    fi

    if "${build_cmd[@]}"; then
        log_success "✓ Built ${service_name} (${full_tag})"

        # Generate quadlet file
        generate_quadlet "$service_name" "$full_tag" "$quadlet_dir"

        echo "${full_tag}"
    else
        log_error "✗ Failed to build ${service_name}"
        return 1
    fi
}

# Show service information
show_service_info() {
    local service_name="$1"
    local containerfile="$2"
    local context_dir="$3"

    log_info "Service Information:"
    log_info "  Name: ${service_name}"
    log_info "  Containerfile: ${containerfile}"
    log_info "  Context: ${context_dir}"

    # Show Containerfile contents summary
    if [[ -f "$containerfile" ]]; then
        local base_image
        base_image=$(grep "^FROM" "$containerfile" | head -1 | sed 's/FROM[[:space:]]*//')
        log_info "  Base image: ${base_image}"

        local build_contexts
        build_contexts=$(grep "COPY\|ADD" "$containerfile" | wc -l)
        log_info "  Copy/ADD statements: ${build_contexts}"
    fi
}

# Main execution
main() {
    log_info "PayU Platform - Build Single Service with Podman"
    log_info "================================================"
    log_info "Service: ${SERVICE_NAME}"
    log_info "Tag: ${TAG}"
    log_info "Registry: ${REGISTRY}"
    log_info "Push after build: ${PUSH}"

    # Check dependencies
    check_dependencies

    # Find Containerfile
    log_info "Finding Containerfile for service: ${SERVICE_NAME}"
    local containerfile
    containerfile=$(find_containerfile "$SERVICE_NAME")

    # Get build context
    local context_dir
    context_dir=$(get_build_context "$containerfile")

    # Show service information
    show_service_info "$SERVICE_NAME" "$containerfile" "$context_dir"

    # Build service
    local result
    result=$(build_service "$SERVICE_NAME" "$containerfile" "$context_dir")

    if [[ $? -eq 0 ]]; then
        # Push image if requested
        if [[ "$PUSH" == true ]]; then
            log_info "Pushing image to ${REGISTRY}..."
            if podman push "$result"; then
                log_success "✓ Pushed ${result}"
            else
                log_error "✗ Failed to push ${result}"
                exit 1
            fi
        fi

        log_success "Build completed successfully!"
    else
        exit 1
    fi
}

# Execute main function
main "$@"
