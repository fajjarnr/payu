#!/bin/bash

# PayU Platform - Scan Images with Trivy using Podman
# =================================================
# This script scans PayU container images for vulnerabilities using Trivy
# Requirements: podman, trivy, jq
#
# Usage: ./scan-images-podman.sh [OPTIONS]
#   -a, --all             Scan all local images
#   -t, --tag TAG         Scan specific tag (e.g., payu:latest)
#   -s, --severity SEV    Minimum severity level (CRITICAL,HIGH,MEDIUM,LOW,UNKNOWN)
#   -f, --format FORMAT   Output format (table, json, sarif, junit)
#   -o, --output FILE     Output file for results
#   -j, --json            JSON output format
#   -v, --verbose         Verbose output
#   -h, --help            Show this help message

set -euo pipefail

# Configuration
DEFAULT_SEVERITY="CRITICAL,HIGH,MEDIUM"
DEFAULT_FORMAT="table"
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
PayU Platform - Scan Images with Trivy using Podman

This script scans PayU container images for vulnerabilities using Trivy.

Usage: $0 [OPTIONS]

Options:
  -a, --all             Scan all local images
  -t, --tag TAG         Scan specific tag (e.g., payu:latest)
  -s, --severity SEV    Minimum severity level (default: $DEFAULT_SEVERITY)
  -f, --format FORMAT   Output format: table, json, sarif, junit (default: $DEFAULT_FORMAT)
  -o, --output FILE     Output file for results
  -j, --json            JSON output format (alias for -f json)
  -v, --verbose         Verbose output
  -h, --help            Show this help message

Environment Variables:
  TRIVY_CACHE_DIR       Trivy cache directory
  TRIVY_TIMEOUT        Scan timeout in seconds
  TRIVY_SKIP_UPDATE     Skip vulnerability database update

Examples:
  $0 -a                                    # Scan all images
  $0 -t account-service:latest            # Scan specific image
  $0 -a -s HIGH -f json -o report.json    # Scan with high severity and JSON output
  $0 -t gateway-service -v                # Scan with verbose output
EOF
}

# Parse arguments
SCAN_ALL=false
TAG=""
SEVERITY="$DEFAULT_SEVERITY"
FORMAT="$DEFAULT_FORMAT"
OUTPUT_FILE=""
VERBOSE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -a|--all)
            SCAN_ALL=true
            shift
            ;;
        -t|--tag)
            TAG="$2"
            shift 2
            ;;
        -s|--severity)
            SEVERITY="$2"
            shift 2
            ;;
        -f|--format)
            FORMAT="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -j|--json)
            FORMAT="json"
            shift
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

    for cmd in podman trivy jq; do
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

# Get list of images to scan
get_images_to_scan() {
    if [[ "$SCAN_ALL" == true ]]; then
        # Get all PayU images
        podman images --format "table {{.Repository}}:{{.Tag}}" | \
            grep -E "(payu|localhost)" | \
            grep -v "<none>" || true
    elif [[ -n "$TAG" ]]; then
        # Check if specific tag exists
        if podman image exists "$TAG"; then
            echo "$TAG"
        else
            log_error "Image not found: $TAG"
            log_info "Available images:"
            podman images --format "table {{.Repository}}:{{.Tag}}" | \
                grep -E "(payu|localhost)" | \
                grep -v "<none>" || true
            exit 1
        fi
    else
        # Get PayU service images
        find "$PROJECT_ROOT" -name "Dockerfile" -type f | \
            while read -r dockerfile; do
                local service_dir
                service_dir=$(dirname "$dockerfile")
                local service_name
                service_name=$(basename "$service_dir")

                # Check if image exists
                local local_image="localhost/${service_name}:latest"
                if podman image exists "$local_image"; then
                    echo "$local_image"
                fi
            done
    fi
}

# Scan single image
scan_image() {
    local image="$1"
    local output_file="${2:-}"

    log_info "Scanning image: $image"

    # Trivy command
    local trivy_cmd=(
        trivy image
        --format "$FORMAT"
        --severity "$SEVERITY"
        --template "{{.Target}}: {{.VulnerabilitiesCount}} vulnerabilities ({{.CriticalCount}} critical, {{.HighCount}} high, {{.MediumCount}} medium, {{.LowCount}} low)"
    )

    # Add output file if specified
    if [[ -n "$output_file" ]]; then
        trivy_cmd+=("--output=$output_file")
    fi

    # Add security options
    trivy_cmd+=(
        --scanners vuln,secret,misconfig
        --skip-dirs "/proc"
        --skip-files "/.dockerenv"
    )

    # Add timeout if specified
    if [[ -n "${TRIVY_TIMEOUT:-}" ]]; then
        trivy_cmd+=("--timeout=${TRIVY_TIMEOUT}s")
    fi

    # Add cache options
    if [[ -z "${TRIVY_SKIP_UPDATE:-}" ]]; then
        trivy_cmd+=("--update")
    fi

    if [[ -n "${TRIVY_CACHE_DIR:-}" ]]; then
        trivy_cmd+=("--cache-dir=$TRIVY_CACHE_DIR")
    else
        trivy_cmd+=("--cache-dir=/tmp/trivy-cache")
    fi

    # Execute scan
    if [[ "$VERBOSE" == true ]]; then
        log_info "Trivy command: ${trivy_cmd[*]}"
    fi

    if "${trivy_cmd[@]}" "$image"; then
        # Parse results for summary
        if [[ "$FORMAT" == "table" && -z "$output_file" ]]; then
            local vuln_count
            vuln_count=$(trivy image --format json --severity "$SEVERITY" "$image" | \
                jq '.Results[0].Vulnerabilities | length // 0' 2>/dev/null || echo "unknown")

            if [[ "$vuln_count" -gt 0 ]]; then
                log_warning "Found $vuln_count vulnerabilities in $image"
            else
                log_success "No vulnerabilities found in $image"
            fi
        fi
    else
        log_error "Scan failed for image: $image"
        return 1
    fi
}

# Generate security report
generate_report() {
    local scan_results="$1"
    local timestamp=$(date +%Y%m%d_%H%M%S)

    # Create reports directory
    local reports_dir="$PROJECT_ROOT/reports/security"
    mkdir -p "$reports_dir"

    # Generate different report formats
    if [[ -f "$scan_results" ]]; then
        # JSON summary
        local json_report="${reports_dir}/trivy_summary_${timestamp}.json"
        if command -v jq >/dev/null 2>&1; then
            jq -c '.' "$scan_results" > "$json_report" 2>/dev/null || true
        fi

        # HTML report
        local html_report="${reports_dir}/trivy_report_${timestamp}.html"
        cat > "$html_report" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Trivy Security Scan Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 10px; margin-bottom: 20px; }
        .vulnerability { margin: 10px 0; padding: 10px; border: 1px solid #ddd; }
        .critical { background-color: #ffebee; }
        .high { background-color: #fff3e0; }
        .medium { background-color: #fffde7; }
        .low { background-color: #e8f5e9; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Trivy Security Scan Report</h1>
        <p>Generated: $(date)</p>
    </div>
    <div id="content">
        <!-- Report content would be embedded here -->
    </div>
</body>
</html>
EOF

        log_success "Generated reports:"
        log_info "  JSON: $json_report"
        log_info "  HTML: $html_report"
    fi
}

# Main execution
main() {
    log_info "PayU Platform - Scan Images with Trivy using Podman"
    log_info "===================================================="
    log_info "Scan all: $SCAN_ALL"
    log_info "Tag: ${TAG:-all}"
    log_info "Severity: $SEVERITY"
    log_info "Format: $FORMAT"
    log_info "Output: ${OUTPUT_FILE:-stdout}"

    # Check dependencies
    check_dependencies

    # Update Trivy database if not skipped
    if [[ -z "${TRIVY_SKIP_UPDATE:-}" ]]; then
        log_info "Updating vulnerability database..."
        trivy image --update >/dev/null 2>&1 || true
    fi

    # Get images to scan
    log_info "Getting images to scan..."
    local images
    images=$(get_images_to_scan)

    if [[ -z "$images" ]]; then
        log_warning "No images found to scan"
        exit 0
    fi

    local image_count=$(echo "$images" | wc -l)
    log_info "Found $image_count image(s) to scan:"
    echo "$images" | sed 's/^/  - /'

    # Create temporary output file if format is not table
    local temp_output=""
    if [[ "$FORMAT" != "table" && -z "$OUTPUT_FILE" ]]; then
        temp_output=$(mktemp)
        OUTPUT_FILE="$temp_output"
    fi

    # Scan images
    local total_vulnerabilities=0
    local critical_vulnerabilities=0
    local high_vulnerabilities=0
    local failed_scans=0

    while read -r image; do
        if [[ -n "$image" ]]; then
            if scan_image "$image" "$OUTPUT_FILE"; then
                # Count vulnerabilities
                if [[ "$FORMAT" == "json" && -f "$OUTPUT_FILE" ]]; then
                    local vulns
                    vulns=$(jq '.Results[0].Vulnerabilities | length // 0' "$OUTPUT_FILE" 2>/dev/null || echo "0")
                    total_vulnerabilities=$((total_vulnerabilities + vulns))

                    local crit
                    crit=$(jq '.Results[0].Vulnerabilities[]? | select(.Severity == "CRITICAL") | length // 0' "$OUTPUT_FILE" 2>/dev/null || echo "0")
                    critical_vulnerabilities=$((critical_vulnerabilities + crit))

                    local high
                    high=$(jq '.Results[0].Vulnerabilities[]? | select(.Severity == "HIGH") | length // 0' "$OUTPUT_FILE" 2>/dev/null || echo "0")
                    high_vulnerabilities=$((high_vulnerabilities + high))
                fi
            else
                ((failed_scans++))
            fi
        fi
    done <<< "$images"

    # Clean up temporary file
    if [[ -n "$temp_output" ]]; then
        rm -f "$temp_output"
    fi

    # Summary
    log_info "Scan Summary:"
    log_info "  Total images scanned: $image_count"
    log_info "  Total vulnerabilities: ${total_vulnerabilities:-0}"
    log_info "  Critical vulnerabilities: ${critical_vulnerabilities:-0}"
    log_info "  High vulnerabilities: ${high_vulnerabilities:-0}"
    log_info "  Failed scans: $failed_scans"

    if [[ $critical_vulnerabilities -gt 0 ]]; then
        log_error "Found $critical_vulnerabilities critical vulnerabilities!"
        exit 1
    elif [[ $high_vulnerabilities -gt 0 ]]; then
        log_warning "Found $high_vulnerabilities high vulnerabilities"
        exit 1
    elif [[ $failed_scans -gt 0 ]]; then
        log_error "$failed_scans scans failed"
        exit 1
    else
        log_success "All scans completed successfully with no critical vulnerabilities!"
    fi
}

# Execute main function
main "$@"
