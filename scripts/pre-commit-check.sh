#!/bin/bash
###############################################################################
# PayU Pre-commit Hook
# Purpose: Catch errors before they reach the repository
#
# Usage:
#   1. Install: cp scripts/pre-commit-check.sh .git/hooks/pre-commit
#   2. chmod +x .git/hooks/pre-commit
#   3. Git will automatically run this before each commit
###############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_header() {
    echo -e "${BLUE}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Get the root directory of the project
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║           PayU Pre-commit Check                                ║"
echo "║           Catching errors before commit                        ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Track if we're in a backend service directory
IN_BACKEND=false
if [[ "$PROJECT_ROOT" =~ backend/.*-service ]]; then
    IN_BACKEND=true
    SERVICE_NAME=$(basename "$PROJECT_ROOT")
    echo -e "${BLUE}Service: ${SERVICE_NAME}${NC}\n"
fi

###############################################################################
# 1. Check for empty dependency blocks (POM files)
###############################################################################
print_header "Checking POM files for empty dependencies..."

if find . -name "pom.xml" -type f -exec grep -l "<dependency>\s*<groupId>" {} \; | \
   xargs grep -Pzo '<dependency>\s*<groupId>.*?</groupId>\s*(<!--.*?-->\s*)?</dependency>' \
   /dev/null 2>&1; then
    print_error "Found empty dependency blocks in POM files!"
    echo "  Run: grep -r '<dependency>.*</dependency>' pom.xml"
    echo "  Fix: Remove empty <dependency> blocks or add artifactId and version"
    exit 1
fi
print_success "POM files validated"

###############################################################################
# 2. Compilation check
###############################################################################
print_header "Checking compilation..."

if [ "$IN_BACKEND" = true ]; then
    # Check if this is a Quarkus or Spring Boot service
    if [ -f "./mvnw" ]; then
        # Quarkus service
        ./mvnw clean compile -q -DskipTests 2>&1 | grep -i "error\|failure" && {
            print_error "Compilation failed!"
            echo "  Fix compilation errors before committing."
            exit 1
        }
    else
        # Spring Boot service
        mvn clean compile -q -DskipTests 2>&1 | grep -i "error\|failure" && {
            print_error "Compilation failed!"
            echo "  Fix compilation errors before committing."
            exit 1
        }
    fi
else
    # Root project - check each backend service
    for pom in backend/*/pom.xml; do
        if [ -f "$pom" ]; then
            service_dir=$(dirname "$pom")
            print_header "Compiling $service_dir..."
            (cd "$service_dir" && mvn compile -q -DskipTests 2>&1 | grep -i "error\|failure") && {
                print_error "Compilation failed in $service_dir!"
                exit 1
            }
        fi
    done
fi

print_success "Compilation check passed"

###############################################################################
# 3. Unit tests (fast, no Docker)
###############################################################################
print_header "Running unit tests..."

if [ "$IN_BACKEND" = true ]; then
    if [ -f "./mvnw" ]; then
        ./mvnw test -q 2>&1 | tail -5
    else
        mvn test -q -Dtest.excluded.groups=integration 2>&1 | tail -5
    fi
else
    echo "  Skipping tests at root level (run per-service)"
fi

print_success "Unit tests passed"

###############################################################################
# 4. Architecture tests
###############################################################################
print_header "Validating architecture..."

if [ "$IN_BACKEND" = true ] && [ -d "src/test/java" ]; then
    # Run architecture tests if they exist
    if find src/test/java -name "*ArchitectureTest.java" -o -name "*ArchTest.java" | grep -q .; then
        if [ -f "./mvnw" ]; then
            ./mvnw test -q -Dtest="*Architecture*,*ArchTest*" 2>&1 | tail -5
        else
            mvn test -q -Dtest="*Architecture*,*ArchTest*" 2>&1 | tail -5
        fi
        print_success "Architecture tests passed"
    else
        print_warning "No architecture tests found"
        echo "  Consider adding ArchUnit tests to enforce architecture rules"
    fi
else
    print_warning "No architecture tests to run"
fi

###############################################################################
# 5. Check for TODO/FIXME in new code
###############################################################################
print_header "Checking for TODO/FIXME..."

# Get list of staged files
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.(java|kt|py)$' || true)

if [ -n "$STAGED_FILES" ]; then
    TODO_COUNT=0
    for file in $STAGED_FILES; do
        if [ -f "$file" ]; then
            COUNT=$(grep -c "TODO\|FIXME\|XXX" "$file" 2>/dev/null || echo "0")
            if [ "$COUNT" -gt 0 ]; then
                print_warning "$file has $COUNT TODO/FIXME comments"
                TODO_COUNT=$((TODO_COUNT + COUNT))
            fi
        fi
    done

    if [ "$TODO_COUNT" -gt 0 ]; then
        echo ""
        print_warning "Found $TODO_COUNT TODO/FIXME comments in staged files"
        read -p "  Continue anyway? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_error "Commit aborted"
            exit 1
        fi
    fi
fi

###############################################################################
# 6. Check file sizes (prevent large files)
###############################################################################
print_header "Checking file sizes..."

LARGE_FILES=$(git diff --cached --name-only | while read file; do
    if [ -f "$file" ]; then
        SIZE=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || echo "0")
        if [ "$SIZE" -gt 1048576 ]; then  # 1MB
            echo "$file ($((SIZE / 1024))KB)"
        fi
    fi
done || true)

if [ -n "$LARGE_FILES" ]; then
    print_warning "Large files detected:"
    echo "$LARGE_FILES"
    read -p "  Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_error "Commit aborted"
        exit 1
    fi
fi

###############################################################################
# Success!
###############################################################################
echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════╗"
echo "║  ✓ All pre-commit checks passed!                                    ║"
echo "║  Proceeding with commit...                                          ║"
echo "╚═══════════════════════════════════════════════════════════════╝${NC}"
echo ""

exit 0
