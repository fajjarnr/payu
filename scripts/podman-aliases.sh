#!/bin/bash
# =============================================================================
# PayU Podman Developer Aliases
# =============================================================================
# Collection of useful Podman aliases to improve developer productivity
# Usage: Source this file in your shell: source /home/ubuntu/payu/scripts/podman-aliases.sh
# =============================================================================

# Color codes for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Basic Podman Commands
alias dcp='podman-compose'                    # Podman Compose
alias db='podman build'                        # Build container images
alias dp='podman push'                         # Push images to registry
alias dps='podman ps'                          # List running containers
alias da='podman ps -a'                        # List all containers
alias dr='podman run'                          # Run a container
alias dexec='podman exec'                      # Execute command in container
alias drm='podman rm'                          # Remove containers
alias drmi='podman rmi'                        # Remove images
alias dstop='podman stop'                      # Stop containers
alias dstart='podman start'                    # Start stopped containers
alias drestart='podman restart'                # Restart containers

# Docker Compose compatibility
alias up='dcp up -d'                           # Start services
alias down='dcp down'                          # Stop and remove containers
alias logs='dcp logs -f'                       # Follow logs
alias ps='dps'                                 # List running services
alias stop='dcp stop'                          # Stop services

# Build Commands
alias dbuild='db'                              # Build image
alias dbuild-nc='db --no-cache'               # Build without cache
alias dbuild-t='db --tag'                     # Build with tag
alias dbuild-tf='db --tag --force'            # Force rebuild with tag
alias dbuild-l='db --layers'                  # Build with layer information

# Image Management
alias dpsa='podman images'                    # List all images
alias dpsn='podman images --no-trunc'         # List images without truncation
alias dpsq='podman images --filter dangling=true'  # List dangling images
alias dpsr='podman images --digests'           # List images with digests
alias dpsa='podman images -a'                 # List all images (all)

# Container Management
alias dlogs='podman logs'                     # Show container logs
alias dlogsf='dlogs -f'                       # Follow container logs
alias dlogst='dlogs --tail'                   # Show last N lines of logs
alias dlogsf-t='dlogs -f --tail'              # Follow last N lines
alias dinspect='podman inspect'                # Inspect container/image
alias dinspect-c='dinspect --format "{{.Config}}"'  # Inspect container config
alias dinspect-n='dinspect --format "{{.NetworkSettings.Networks}}"'  # Inspect networks
alias dtop='podman top'                       # Show running processes
alias dport='podman port'                     # Show container ports
alias dkill='podman kill'                     # Kill containers
alias dprune='podman container prune'         # Remove unused containers

# Network Management
alias dnetwork='podman network ls'            # List networks
alias dnetwork-inspect='podman network inspect'  # Inspect network
alias dnetwork-create='podman network create'  # Create network
alias dnetwork-rm='podman network rm'          # Remove network

# Volume Management
alias dvolume='podman volume ls'              # List volumes
alias dvolume-inspect='podman volume inspect'  # Inspect volume
alias dvolume-create='podman volume create'    # Create volume
alias dvolume-rm='podman volume rm'           # Remove volume
alias dvolume-prune='podman volume prune'      # Remove unused volumes

# System Commands
alias dinfo='podman info'                     # Show system information
alias dversion='podman version'               # Show version
alias dstats='podman stats'                   # Show container stats
alias dstats-c='podman stats --no-stream'     # Show stats without stream
alias dstats-s='podman stats --format "{{.Container}} {{.CPUPerc}}"'  # Custom stats format
alias dsearch='podman search'                 # Search for images
alias dpull='podman pull'                     # Pull images
alias dtag='podman tag'                       # Tag images
alias dcommit='podman commit'                 # Create image from container
alias ddiff='podman diff'                     # Show container changes

# Registry Commands
alias dlogin='podman login'                   # Login to registry
alias dlogout='podman logout'                 # Logout from registry
alias dsearch-r='dsearch --registry'          # Search in specific registry
alias dpull-r='dpull --registry'             # Pull from specific registry
alias dpush-r='dpush --registry'             # Push to specific registry

# Advanced Commands
alias dexec-t='dexec -it'                    # Execute with terminal
alias drun-d='drun -d'                       # Run in background
alias drun-it='drun -it'                     # Run in interactive mode
alias drun-name='drun --name'                # Run with name
alias drun-net='drun --network'              # Run with network
alias drun-vol='drun -v'                     # Run with volume
alias drun-p='drun -p'                       # Run with port mapping
alias drun-e='drun -e'                       # Run with environment variable
alias drun-env='drun --env'                  # Run with environment file

# Development Commands
alias ddev='dcp up -d --build'               # Build and start development
alias ddev-log='dcp logs -f --tail 100'      # Follow development logs
alias ddev-restart='dcp restart'              # Restart all services
alias ddev-stop='dcp stop'                   # Stop all services
alias ddev-rm='dcp down -v'                  # Remove with volumes
alias ddev-logs-all='dcp logs -f --all'      # Show all logs
alias ddev-logs-service='dcp logs'           # Logs for specific service
alias ddev-logs-follow='dcp logs -f'         # Follow logs for service

# Testing Commands
alias dtest='dcp run --rm test'              # Run tests
alias dtest-unit='dtest pytest tests/unit'    # Run unit tests
alias dtest-integration='dtest pytest tests/integration'  # Run integration tests
alias dtest-e2e='dtest pytest tests/e2e'      # Run e2e tests
alias dtest-coverage='dtest pytest --cov'     # Run tests with coverage

# Security Commands
alias dscan='podman image scan'              # Scan images for vulnerabilities
alias dscan-vuln='dscan vuln'                # Scan for vulnerabilities
alias dscan-all='dscan vuln'                 # Scan all layers
alias dscan-high='dscan --severity high'     # Scan for high severity
alias dscan-cve='dscan --cve'               # Scan for CVEs
alias dsign='podman image sign'               # Sign images
alias dverify='podman image verify'           # Verify image signatures

# System Cleanup
alias dclean-all='podman system prune -a -f'  # Clean all unused
alias dclean-containers='dprune -f'          # Clean containers
alias dclean-images='podman image prune -a -f' # Clean images
alias dclean-volumes='podman volume prune -f' # Clean volumes
alias dclean-networks='podman network prune -f' # Clean networks
alias dclean-system='podman system df'       # Show disk usage

# Helper Functions
dshow-aliases() {
    echo -e "${BLUE}PayU Podman Aliases:${NC}"
    echo ""
    echo -e "${YELLOW}Basic Commands:${NC}"
    echo "  dcp          → podman-compose"
    echo "  db           → podman build"
    echo "  dp           → podman push"
    echo "  dps          → podman ps"
    echo "  da           → podman ps -a"
    echo ""
    echo -e "${YELLOW}Docker Compose Compatibility:${NC}"
    echo "  up           → dcp up -d"
    echo "  down         → dcp down"
    echo "  logs         → dcp logs -f"
    echo "  stop         → dcp stop"
    echo ""
    echo -e "${YELLOW}Build Commands:${NC}"
    echo "  dbuild       → db"
    echo "  dbuild-nc    → db --no-cache"
    echo "  dbuild-t     → db --tag"
    echo ""
    echo -e "${YELLOW}Image Management:${NC}"
    echo "  dpsa         → podman images"
    echo "  dpsq         → podman images --filter dangling=true"
    echo ""
    echo -e "${YELLOW}Container Management:${NC}"
    echo "  dlogs        → podman logs"
    echo "  dlogsf       → dlogs -f"
    echo "  dlogst       → dlogs --tail"
    echo "  dexec        → podman exec"
    echo "  dinspect     → podman inspect"
    echo ""
    echo -e "${YELLOW}Development Commands:${NC}"
    echo "  ddev         → dcp up -d --build"
    echo "  ddev-log     → dcp logs -f --tail 100"
    echo "  ddev-restart  → dcp restart"
    echo ""
    echo -e "${YELLOW}Testing Commands:${NC}"
    echo "  dtest        → dcp run --rm test"
    echo "  dtest-unit   → dtest pytest tests/unit"
    echo "  dtest-e2e    → dtest pytest tests/e2e"
    echo ""
    echo -e "${YELLOW}Security Commands:${NC}"
    echo "  dscan        → podman image scan"
    echo "  dsign        → podman image sign"
    echo ""
    echo -e "${YELLOW}System Cleanup:${NC}"
    echo "  dclean-all   → podman system prune -a -f"
    echo "  dclean-images → podman image prune -a -f"
    echo "  dclean-volumes → podman volume prune -f"
    echo ""
    echo -e "${GREEN}Source this file to use aliases: source /home/ubuntu/payu/scripts/podman-aliases.sh${NC}"
}

# Function to show container resource usage
dtop-cpu() {
    if [ $# -eq 0 ]; then
        echo -e "${YELLOW}Top CPU-consuming containers:${NC}"
        podman stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"
    else
        podman stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" | grep "$1"
    fi
}

# Function to show container network I/O
dtop-net() {
    if [ $# -eq 0 ]; then
        echo -e "${YELLOW}Top Network I/O containers:${NC}"
        podman stats --no-stream --format "table {{.Container}}\t{{.NetIO}}\t{{.BlockIO}}"
    else
        podman stats --no-stream --format "table {{.Container}}\t{{.NetIO}}\t{{.BlockIO}}" | grep "$1"
    fi
}

# Function to export/import containers
dexport() {
    if [ $# -eq 0 ]; then
        echo "Usage: dexport <container_name> [output_file.tar]"
        return 1
    fi
    local container_name=$1
    local output_file=${2:-${container_name}_export.tar}
    echo -e "${BLUE}Exporting container ${container_name} to ${output_file}...${NC}"
    podman export -o "${output_file}" "${container_name}"
    echo -e "${GREEN}Export completed!${NC}"
}

dimport() {
    if [ $# -eq 0 ]; then
        echo "Usage: dimport <image_file.tar> [image_name:tag]"
        return 1
    fi
    local input_file=$1
    local image_name=${2:-imported_image}
    echo -e "${BLUE}Importing container image from ${input_file}...${NC}"
    podman import "${input_file}" "${image_name}"
    echo -e "${GREEN}Import completed! Image tag: ${image_name}${NC}"
}

# Function to create podman-compose.yml from existing docker-compose.yml
convert-docker-compose() {
    if [ ! -f "docker-compose.yml" ]; then
        echo -e "${RED}Error: docker-compose.yml not found in current directory${NC}"
        return 1
    fi
    echo -e "${BLUE}Converting docker-compose.yml to podman-compose.yml...${NC}"
    podman-compose convert
    echo -e "${GREEN}Conversion completed!${NC}"
}

# Function to build images with specific registry prefix
build-for-payu() {
    if [ $# -eq 0 ]; then
        echo "Usage: build-for-payu <image_name> [tag]"
        return 1
    fi
    local image_name=$1
    local tag=${2:-1.4.0}
    local full_name="${PODMAN_REGISTRY:-registry.payu.internal}/${image_name}:${tag}"
    echo -e "${BLUE}Building image ${image_name} as ${full_name}...${NC}"
    podman build -t "${full_name}" .
    echo -e "${GREEN}Build completed! Image: ${full_name}${NC}"
}

# Function to push images to PayU registry
push-to-payu() {
    if [ $# -eq 0 ]; then
        echo "Usage: push-to-payu <image_name> [tag]"
        return 1
    fi
    local image_name=$1
    local tag=${2:-1.4.0}
    local full_name="${PODMAN_REGISTRY:-registry.payu.internal}/${image_name}:${tag}"
    echo -e "${BLUE}Pushing image ${full_name} to registry...${NC}"
    podman push "${full_name}"
    echo -e "${GREEN}Push completed!${NC}"
}

# Function to pull images from PayU registry
pull-from-payu() {
    if [ $# -eq 0 ]; then
        echo "Usage: pull-from-payu <image_name> [tag]"
        return 1
    fi
    local image_name=$1
    local tag=${2:-1.4.0}
    local full_name="${PODMAN_REGISTRY:-registry.payu.internal}/${image_name}:${tag}"
    echo -e "${BLUE}Pulling image ${full_name} from registry...${NC}"
    podman pull "${full_name}"
    echo -e "${GREEN}Pull completed!${NC}"
}

# Show podman setup status
podman-status() {
    echo -e "${BLUE}=== Podman Status Check ===${NC}"
    echo ""
    echo -e "${YELLOW}Podman Version:${NC}"
    podman version --format "{{.Client.Version}}" 2>/dev/null || echo "Podman not installed"
    echo ""
    echo -e "${YELLOW}Podman Info:${NC}"
    podman info --format "Podman running: {{.Host.OCIRuntime}}" 2>/dev/null || echo "Podman info not available"
    echo ""
    echo -e "${YELLOW}Registry Settings:${NC}"
    if [ -n "${PODMAN_REGISTRY}" ]; then
        echo "  Registry: ${PODMAN_REGISTRY}"
    else
        echo "  Registry: Not set (defaulting to registry.payu.internal)"
    fi
    echo ""
    echo -e "${YELLOW}Build Settings:${NC}"
    if [ -n "${BUILDAH_LAYERS}" ] && [ "${BUILDAH_LAYERS}" = "1" ]; then
        echo "  BuildKit: Enabled (BUILDAH_LAYERS=1)"
    else
        echo "  BuildKit: Disabled"
    fi
    echo ""
    echo -e "${YELLOW}Containers Running:${NC}"
    local running_count=$(podman ps --format "{{.ID}}" | wc -l)
    if [ "${running_count}" -gt 0 ]; then
        echo "  ${running_count} container(s) running:"
        podman ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    else
        echo "  No containers running"
    fi
    echo ""
    echo -e "${YELLOW}Images Available:${NC}"
    local image_count=$(podman images --format "{{.ID}}" | wc -l)
    if [ "${image_count}" -gt 0 ]; then
        echo "  ${image_count} image(s) available:"
        podman images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
    else
        echo "  No images available"
    fi
}

# Export helper functions to shell
export -f dshow-aliases
export -f dtop-cpu
export -f dtop-net
export -f dexport
export -f dimport
export -f convert-docker-compose
export -f build-for-payu
export -f push-to-payu
export -f pull-from-payu
export -f podman-status

echo -e "${GREEN}PayU Podman aliases loaded!${NC}"
echo -e "${BLUE}Type 'dshow-aliases' to see all available aliases${NC}"
echo -e "${BLUE}Type 'podman-status' to check your podman setup${NC}"
