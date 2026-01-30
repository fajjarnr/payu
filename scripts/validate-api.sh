#!/bin/bash

# =============================================================================
# PayU API Validation Script
# =============================================================================
# This script validates OpenAPI specifications using Spectral
# Usage: ./scripts/validate-api.sh [options] [files...]
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
RULESET=".spectral.yaml"
FORMAT="stylish"
FAIL_SEVERITY="error"
OUTPUT_DIR=""
VERBOSE=false
WATCH_MODE=false
FIX_MODE=false
PARALLEL=false
MAX_JOBS=4

# Spectral version
SPECTRAL_VERSION="6.14.2"

# =============================================================================
# Helper Functions
# =============================================================================

print_usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS] [FILES...]

Validate OpenAPI specifications using Spectral linter.

OPTIONS:
    -h, --help              Show this help message
    -r, --ruleset FILE      Use custom ruleset (default: .spectral.yaml)
    -f, --format FORMAT     Output format: stylish, json, html, junit, github-actions
                            (default: stylish)
    -s, --severity LEVEL    Fail on severity: error, warn, info, hint
                            (default: error)
    -o, --output DIR        Save reports to directory
    -v, --verbose           Enable verbose output
    -w, --watch             Watch mode - revalidate on file changes
    --fix                   Attempt to auto-fix violations where possible
    -p, --parallel          Run validations in parallel
    -j, --jobs N            Maximum parallel jobs (default: 4)
    --install               Install/Update Spectral CLI
    --version               Show script version

FILES:
    OpenAPI specification files to validate. If not provided, the script
    will search for common OpenAPI file patterns.

EXAMPLES:
    # Validate all OpenAPI files in the project
    ./scripts/validate-api.sh

    # Validate specific files
    ./scripts/validate-api.sh docs/openapi/account-api.yaml

    # Validate with custom ruleset and JSON output
    ./scripts/validate-api.sh -r custom-rules.yaml -f json docs/openapi/*.yaml

    # Validate and save reports
    ./scripts/validate-api.sh -o reports/ docs/openapi/*.yaml

    # Watch mode for development
    ./scripts/validate-api.sh -w docs/openapi/account-api.yaml

    # Parallel validation with verbose output
    ./scripts/validate-api.sh -p -v -j 8 docs/openapi/*.yaml

EOF
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

log_verbose() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${BLUE}[VERB]${NC} $1"
    fi
}

check_spectral() {
    if ! command -v spectral &> /dev/null; then
        log_error "Spectral CLI is not installed"
        echo ""
        echo "To install Spectral CLI, run one of the following:"
        echo "  npm install -g @stoplight/spectral-cli"
        echo "  yarn global add @stoplight/spectral-cli"
        echo ""
        echo "Or run this script with --install flag:"
        echo "  ./scripts/validate-api.sh --install"
        exit 1
    fi

    local version
    version=$(spectral --version 2>/dev/null | head -1 || echo "unknown")
    log_verbose "Spectral version: $version"
}

install_spectral() {
    log_info "Installing Spectral CLI v${SPECTRAL_VERSION}..."

    if command -v npm &> /dev/null; then
        npm install -g @stoplight/spectral-cli@${SPECTRAL_VERSION}
    elif command -v yarn &> /dev/null; then
        yarn global add @stoplight/spectral-cli@${SPECTRAL_VERSION}
    else
        log_error "Neither npm nor yarn is installed"
        exit 1
    fi

    log_success "Spectral CLI installed successfully"
}

find_openapi_files() {
    local files=""

    log_verbose "Searching for OpenAPI files..."

    # Common patterns for OpenAPI files
    local patterns=(
        "openapi*.yaml"
        "openapi*.yml"
        "openapi*.json"
        "*api*.yaml"
        "*api*.yml"
        "swagger*.yaml"
        "swagger*.yml"
        "*spec*.yaml"
        "*spec*.yml"
    )

    # Search in common directories
    local dirs=(
        "."
        "docs/openapi"
        "docs/api"
        "backend"
        "api"
    )

    for dir in "${dirs[@]}"; do
        if [ -d "$dir" ]; then
            for pattern in "${patterns[@]}"; do
                local found
                found=$(find "$dir" -type f -name "$pattern" ! -path "*/node_modules/*" ! -path "*/.git/*" ! -path "*/target/*" ! -path "*/build/*" 2>/dev/null || true)
                if [ -n "$found" ]; then
                    files="$files $found"
                fi
            done
        fi
    done

    # Remove duplicates and sort
    echo "$files" | tr ' ' '\n' | sort -u | grep -v '^$' || true
}

validate_file() {
    local file="$1"
    local exit_code=0

    log_verbose "Validating: $file"

    # Check if file exists
    if [ ! -f "$file" ]; then
        log_error "File not found: $file"
        return 1
    fi

    # Determine output file if output directory is set
    local output_file=""
    if [ -n "$OUTPUT_DIR" ]; then
        local basename
        basename=$(basename "$file" | sed 's/[^a-zA-Z0-9._-]/_/g')
        output_file="${OUTPUT_DIR}/spectral-report-${basename}.${FORMAT}"
    fi

    # Build spectral command
    local cmd="spectral lint \"$file\" --ruleset \"$RULESET\" --fail-severity=$FAIL_SEVERITY --format=$FORMAT"

    if [ -n "$output_file" ]; then
        cmd="$cmd --output=\"$output_file\""
    fi

    if [ "$VERBOSE" = true ]; then
        echo "Command: $cmd"
    fi

    # Run validation
    if eval "$cmd"; then
        log_success "$file"
        return 0
    else
        log_error "$file"
        return 1
    fi
}

validate_parallel() {
    local files=("$@")
    local pids=()
    local results=()
    local total=${#files[@]}
    local completed=0
    local failed=0

    log_info "Running validations in parallel (max $MAX_JOBS jobs)..."

    # Function to run validation in background
    run_validation() {
        local file="$1"
        local idx="$2"
        local result_file="/tmp/spectral_result_${idx}"

        if validate_file "$file" > /dev/null 2>&1; then
            echo "0" > "$result_file"
        else
            echo "1" > "$result_file"
        fi
    }

    # Run validations in parallel
    local idx=0
    for file in "${files[@]}"; do
        # Wait if we've reached max jobs
        while [ $(jobs -r | wc -l) -ge "$MAX_JOBS" ]; do
            sleep 0.1
        done

        run_validation "$file" "$idx" &
        pids+=($!)
        ((idx++))
    done

    # Wait for all jobs to complete
    wait

    # Collect results
    idx=0
    for file in "${files[@]}"; do
        local result_file="/tmp/spectral_result_${idx}"
        local result
        result=$(cat "$result_file" 2>/dev/null || echo "1")

        if [ "$result" == "0" ]; then
            log_success "$file"
        else
            log_error "$file"
            ((failed++))
            # Re-run to show errors
            validate_file "$file" || true
        fi

        rm -f "$result_file"
        ((completed++))
        ((idx++))
    done

    return $failed
}

watch_mode() {
    local files=("$@")

    if ! command -v fswatch &> /dev/null; then
        log_error "fswatch is required for watch mode"
        echo "Install it with: brew install fswatch (macOS) or apt-get install fswatch (Linux)"
        exit 1
    fi

    log_info "Watch mode enabled. Press Ctrl+C to stop."
    echo ""

    # Initial validation
    log_info "Running initial validation..."
    for file in "${files[@]}"; do
        validate_file "$file" || true
    done
    echo ""

    # Watch for changes
    fswatch -o "${files[@]}" | while read -r; do
        echo ""
        log_info "File changed, revalidating..."
        echo ""
        for file in "${files[@]}"; do
            validate_file "$file" || true
        done
        echo ""
        log_info "Waiting for changes..."
    done
}

generate_summary() {
    local total="$1"
    local passed="$2"
    local failed="$3"

    echo ""
    echo "========================================"
    echo "       API Validation Summary"
    echo "========================================"
    echo "Total files:  $total"
    echo -e "Passed:       ${GREEN}$passed${NC}"
    echo -e "Failed:       ${RED}$failed${NC}"
    echo "========================================"

    if [ "$OUTPUT_DIR" != "" ] && [ -d "$OUTPUT_DIR" ]; then
        echo ""
        log_info "Reports saved to: $OUTPUT_DIR"
        ls -la "$OUTPUT_DIR"
    fi
}

# =============================================================================
# Main Script
# =============================================================================

main() {
    local files=()

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                print_usage
                exit 0
                ;;
            -r|--ruleset)
                RULESET="$2"
                shift 2
                ;;
            -f|--format)
                FORMAT="$2"
                shift 2
                ;;
            -s|--severity)
                FAIL_SEVERITY="$2"
                shift 2
                ;;
            -o|--output)
                OUTPUT_DIR="$2"
                shift 2
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -w|--watch)
                WATCH_MODE=true
                shift
                ;;
            --fix)
                FIX_MODE=true
                shift
                ;;
            -p|--parallel)
                PARALLEL=true
                shift
                ;;
            -j|--jobs)
                MAX_JOBS="$2"
                shift 2
                ;;
            --install)
                install_spectral
                exit 0
                ;;
            --version)
                echo "PayU API Validation Script v1.0.0"
                echo "Recommended Spectral version: $SPECTRAL_VERSION"
                exit 0
                ;;
            -*)
                log_error "Unknown option: $1"
                print_usage
                exit 1
                ;;
            *)
                files+=("$1")
                shift
                ;;
        esac
    done

    # Check if Spectral is installed
    check_spectral

    # Check if ruleset exists
    if [ ! -f "$RULESET" ]; then
        log_error "Ruleset not found: $RULESET"
        exit 1
    fi

    log_verbose "Using ruleset: $RULESET"
    log_verbose "Fail severity: $FAIL_SEVERITY"
    log_verbose "Output format: $FORMAT"

    # Create output directory if specified
    if [ -n "$OUTPUT_DIR" ]; then
        mkdir -p "$OUTPUT_DIR"
        log_verbose "Output directory: $OUTPUT_DIR"
    fi

    # Find files if none specified
    if [ ${#files[@]} -eq 0 ]; then
        log_info "No files specified, searching for OpenAPI files..."
        mapfile -t files < <(find_openapi_files)

        if [ ${#files[@]} -eq 0 ]; then
            log_warn "No OpenAPI files found"
            exit 0
        fi

        log_info "Found ${#files[@]} OpenAPI file(s)"
    fi

    # Watch mode
    if [ "$WATCH_MODE" = true ]; then
        watch_mode "${files[@]}"
        exit 0
    fi

    # Run validations
    log_info "Validating ${#files[@]} file(s)..."
    echo ""

    local total=${#files[@]}
    local passed=0
    local failed=0

    if [ "$PARALLEL" = true ]; then
        # Parallel validation
        if validate_parallel "${files[@]}"; then
            passed=$total
            failed=0
        else
            passed=$((total - $?))
            failed=$?
        fi
    else
        # Sequential validation
        for file in "${files[@]}"; do
            if validate_file "$file"; then
                ((passed++))
            else
                ((failed++))
            fi
        done
    fi

    # Generate summary
    generate_summary "$total" "$passed" "$failed"

    # Exit with appropriate code
    if [ $failed -gt 0 ]; then
        echo ""
        log_error "Validation failed for $failed file(s)"
        exit 1
    else
        echo ""
        log_success "All validations passed!"
        exit 0
    fi
}

# Run main function
main "$@"
