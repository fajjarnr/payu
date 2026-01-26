#!/bin/bash
#
# PayU Development Environment Setup Script
# ==========================================
# Script ini menginstall semua dependencies yang dibutuhkan untuk development PayU.
# Supports: Ubuntu/Debian, macOS, dan RHEL/Fedora
#
# Usage:
#   chmod +x setup.sh
#   ./setup.sh              # Full install
#   ./setup.sh --backend    # Backend only (Java, Maven, Python)
#   ./setup.sh --frontend   # Frontend only (Node.js, npm)
#   ./setup.sh --docker     # Docker only
#   ./setup.sh --check      # Check installed versions
#
# Requirements:
#   - Java 21 (GraalVM CE or Temurin)
#   - Maven 3.9+
#   - Python 3.12+
#   - Node.js 20+ LTS
#   - Docker + Docker Compose
#   - Git
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Versions
JAVA_VERSION="21"
NODE_VERSION="22"
PYTHON_VERSION="3.12"
MAVEN_VERSION="3.9.9"
OC_VERSION="stable"

# Detect OS
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if [ -f /etc/os-release ]; then
            . /etc/os-release
            OS=$ID
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="macos"
    else
        echo -e "${RED}Unsupported OS: $OSTYPE${NC}"
        exit 1
    fi
    echo -e "${BLUE}Detected OS: $OS${NC}"
}

# Print section header
print_section() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

# Print success message
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Print warning message
print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Print error message
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# ============================================================================
# INSTALL FUNCTIONS
# ============================================================================

install_base_packages() {
    print_section "Installing Base Packages"
    
    case $OS in
        ubuntu|debian|pop)
            sudo apt update && sudo apt upgrade -y
            sudo apt install -y \
                git \
                curl \
                wget \
                unzip \
                zip \
                gnupg \
                ca-certificates \
                build-essential \
                software-properties-common \
                apt-transport-https \
                lsb-release
            ;;
        fedora|rhel|centos|rocky|almalinux)
            sudo dnf update -y
            sudo dnf install -y \
                git \
                curl \
                wget \
                unzip \
                zip \
                gnupg2 \
                ca-certificates \
                gcc \
                gcc-c++ \
                make
            ;;
        macos)
            if ! command_exists brew; then
                echo "Installing Homebrew..."
                /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
            fi
            brew update
            brew install git curl wget
            ;;
    esac
    
    print_success "Base packages installed"
}

install_docker() {
    print_section "Installing Docker"
    
    if command_exists docker; then
        print_warning "Docker already installed: $(docker --version)"
        return 0
    fi
    
    case $OS in
        ubuntu|debian|pop)
            # Remove old versions
            sudo apt remove -y docker.io docker-compose docker-compose-v2 docker-doc podman-docker containerd runc 2>/dev/null || true
            
            # Add Docker's official GPG key
            sudo install -m 0755 -d /etc/apt/keyrings
            sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
            sudo chmod a+r /etc/apt/keyrings/docker.asc
            
            # Add the repository to Apt sources
            echo \
              "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
              $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
              sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
            
            sudo apt update -y
            sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
            ;;
        fedora|rhel|centos|rocky|almalinux)
            sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo
            sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
            ;;
        macos)
            brew install --cask docker
            echo -e "${YELLOW}Please open Docker Desktop to complete installation${NC}"
            ;;
    esac
    
    # Add user to docker group (Linux only)
    if [[ "$OS" != "macos" ]]; then
        sudo usermod -aG docker $USER
        sudo systemctl enable docker
        sudo systemctl start docker
        print_warning "Log out and back in for docker group changes to take effect"
    fi
    
    print_success "Docker installed"
}

install_java() {
    print_section "Installing Java $JAVA_VERSION (Temurin)"
    
    if command_exists java; then
        CURRENT_JAVA=$(java -version 2>&1 | head -n 1)
        if echo "$CURRENT_JAVA" | grep -q "21"; then
            print_warning "Java 21 already installed: $CURRENT_JAVA"
            return 0
        fi
    fi
    
    case $OS in
        ubuntu|debian|pop)
            # Add Adoptium repository
            wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
            echo "deb https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo $VERSION_CODENAME) main" | \
                sudo tee /etc/apt/sources.list.d/adoptium.list
            sudo apt update
            sudo apt install -y temurin-21-jdk
            ;;
        fedora|rhel|centos|rocky|almalinux)
            sudo dnf install -y java-21-openjdk java-21-openjdk-devel
            ;;
        macos)
            brew install --cask temurin@21
            ;;
    esac
    
    # Set JAVA_HOME
    if [[ "$OS" != "macos" ]]; then
        JAVA_HOME_PATH=$(dirname $(dirname $(readlink -f $(which java))))
        if ! grep -q "JAVA_HOME" ~/.bashrc; then
            echo "export JAVA_HOME=$JAVA_HOME_PATH" >> ~/.bashrc
            echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
        fi
    fi
    
    print_success "Java $JAVA_VERSION installed"
}

install_maven() {
    print_section "Installing Maven $MAVEN_VERSION"
    
    if command_exists mvn; then
        print_warning "Maven already installed: $(mvn -version | head -n 1)"
        return 0
    fi
    
    case $OS in
        ubuntu|debian|pop)
            sudo apt install -y maven
            ;;
        fedora|rhel|centos|rocky|almalinux)
            sudo dnf install -y maven
            ;;
        macos)
            brew install maven
            ;;
    esac
    
    print_success "Maven installed"
}

install_python() {
    print_section "Installing Python $PYTHON_VERSION"
    
    if command_exists python3; then
        CURRENT_PY=$(python3 --version)
        if echo "$CURRENT_PY" | grep -q "3.12\|3.13"; then
            print_warning "Python 3.12+ already installed: $CURRENT_PY"
            return 0
        fi
    fi
    
    case $OS in
        ubuntu|debian|pop)
            sudo add-apt-repository -y ppa:deadsnakes/ppa
            sudo apt update
            sudo apt install -y python3.12 python3.12-venv python3.12-dev python3-pip
            sudo update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.12 1
            ;;
        fedora|rhel|centos|rocky|almalinux)
            sudo dnf install -y python3.12 python3.12-devel python3-pip
            ;;
        macos)
            brew install python@3.12
            ;;
    esac
    
    # Install pipx for global tools
    python3 -m pip install --user pipx
    python3 -m pipx ensurepath
    
    print_success "Python $PYTHON_VERSION installed"
}

install_nodejs() {
    print_section "Installing Node.js $NODE_VERSION LTS"
    
    if command_exists node; then
        CURRENT_NODE=$(node --version)
        if echo "$CURRENT_NODE" | grep -q "v2[0-9]"; then
            print_warning "Node.js 20+ already installed: $CURRENT_NODE"
            return 0
        fi
    fi
    
    case $OS in
        ubuntu|debian|pop)
            # Install via NodeSource
            curl -fsSL https://deb.nodesource.com/setup_${NODE_VERSION}.x | sudo -E bash -
            sudo apt install -y nodejs
            ;;
        fedora|rhel|centos|rocky|almalinux)
            curl -fsSL https://rpm.nodesource.com/setup_${NODE_VERSION}.x | sudo bash -
            sudo dnf install -y nodejs
            ;;
        macos)
            brew install node@${NODE_VERSION}
            ;;
    esac
    
    # Install global npm packages
    sudo npm install -g pnpm yarn
    
    print_success "Node.js $NODE_VERSION installed"
}

install_cloud_clis() {
    print_section "Installing Cloud CLIs (OpenShift & Kubernetes)"
    
    if command_exists oc; then
        print_warning "OpenShift CLI already installed: $(oc version --client | head -n 1)"
        return 0
    fi
    
    case $OS in
        ubuntu|debian|pop|fedora|rhel|centos|rocky|almalinux)
            echo "Downloading OpenShift Client..."
            mkdir -p /tmp/oc-client
            wget -q https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/openshift-client-linux.tar.gz -O /tmp/oc-client/oc.tar.gz
            tar -xzf /tmp/oc-client/oc.tar.gz -C /tmp/oc-client
            sudo mv /tmp/oc-client/oc /tmp/oc-client/kubectl /usr/local/bin/
            rm -rf /tmp/oc-client
            ;;
        macos)
            brew install openshift-cli
            ;;
    esac
    
    print_success "OpenShift and Kubernetes CLIs installed"
}

install_db_clients() {
    print_section "Installing Database & Messaging Clients"
    
    case $OS in
        ubuntu|debian|pop)
            sudo apt install -y postgresql-client redis-tools kcat
            ;;
        fedora|rhel|centos|rocky|almalinux)
            sudo dnf install -y postgresql redis kcat
            ;;
        macos)
            brew install postgresql@16 redis kcat
            brew link --force postgresql@16
            ;;
    esac
    
    print_success "Database & Messaging clients installed"
}

install_additional_tools() {
    print_section "Installing Additional Development Tools"
    
    # Install jq (JSON processor)
    case $OS in
        ubuntu|debian|pop)
            sudo apt install -y jq httpie pre-commit
            ;;
        fedora|rhel|centos|rocky|almalinux)
            sudo dnf install -y jq httpie pre-commit
            ;;
        macos)
            brew install jq httpie pre-commit
            ;;
    esac
    
    # Install k9s (Kubernetes TUI) - optional
    if ! command_exists k9s; then
        echo "Installing k9s..."
        case $OS in
            ubuntu|debian|pop|fedora|rhel|centos|rocky|almalinux)
                curl -sS https://webinstall.dev/k9s | bash 2>/dev/null || print_warning "k9s installation skipped"
                ;;
            macos)
                brew install k9s
                ;;
        esac
    fi
    
    print_success "Additional tools installed"
}

# ============================================================================
# PROJECT SETUP
# ============================================================================

setup_project() {
    print_section "Setting Up PayU Project"
    
    # Get project root directory
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    cd "$SCRIPT_DIR"
    
    # Copy environment file if not exists
    if [ ! -f .env ] && [ -f .env.example ]; then
        cp .env.example .env
        print_success "Created .env from .env.example"
    fi

    # Build Shared Starters (Mandatory for backend)
    print_section "Building Shared Backend Starters"
    if [ -d "backend/shared" ]; then
        make build-test-deps || {
            echo "Fallback: building starters manually..."
            cd backend/shared/cache-starter && mvn clean install -DskipTests -q
            cd ../resilience-starter && mvn clean install -DskipTests -q
            cd ../security-starter && mvn clean install -DskipTests -q
            cd "$SCRIPT_DIR"
        }
    fi
    
    # Install frontend dependencies
    if [ -d "frontend/web-app" ]; then
        echo "Installing web-app dependencies..."
        cd frontend/web-app
        npm install --legacy-peer-deps
        cd "$SCRIPT_DIR"
        print_success "web-app dependencies installed"
    fi
    
    if [ -d "frontend/developer-docs" ]; then
        echo "Installing developer-docs dependencies..."
        cd frontend/developer-docs
        npm install --legacy-peer-deps
        cd "$SCRIPT_DIR"
        print_success "developer-docs dependencies installed"
    fi
    
    if [ -d "frontend/mobile" ]; then
        echo "Installing mobile dependencies..."
        cd frontend/mobile
        npm install --legacy-peer-deps
        cd "$SCRIPT_DIR"
        print_success "mobile dependencies installed"
    fi
    
    # Install Python service dependencies
    for service in kyc-service analytics-service; do
        if [ -d "backend/$service" ]; then
            echo "Installing $service Python dependencies..."
            cd "backend/$service"
            python3 -m venv .venv
            if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
                source .venv/Scripts/activate
            else
                source .venv/bin/activate
            fi
            pip install -r requirements.txt || print_warning "Failed to install requirements for $service"
            deactivate
            cd "$SCRIPT_DIR"
            print_success "$service dependencies installed"
        fi
    done

    # Setup Pre-commit hooks
    if command_exists pre-commit; then
        echo "Setting up pre-commit hooks..."
        pre-commit install
        print_success "Pre-commit hooks installed"
    fi
    
    print_success "Project setup complete"
}

# ============================================================================
# CHECK VERSIONS
# ============================================================================

check_versions() {
    print_section "Checking Installed Versions"
    
    echo ""
    echo "Required Tools:"
    echo "---------------"
    
    # Git
    if command_exists git; then
        print_success "Git: $(git --version)"
    else
        print_error "Git: Not installed"
    fi
    
    # Docker
    if command_exists docker; then
        print_success "Docker: $(docker --version)"
        if command_exists docker-compose || docker compose version >/dev/null 2>&1; then
            print_success "Docker Compose: $(docker compose version 2>/dev/null || docker-compose --version)"
        fi
    else
        print_error "Docker: Not installed"
    fi
    
    # Java
    if command_exists java; then
        JAVA_VER=$(java -version 2>&1 | head -n 1)
        if echo "$JAVA_VER" | grep -q "21"; then
            print_success "Java: $JAVA_VER"
        else
            print_warning "Java: $JAVA_VER (Recommended: 21)"
        fi
    else
        print_error "Java: Not installed"
    fi
    
    # Maven
    if command_exists mvn; then
        print_success "Maven: $(mvn -version 2>&1 | head -n 1)"
    else
        print_error "Maven: Not installed"
    fi
    
    # Python
    if command_exists python3; then
        PY_VER=$(python3 --version)
        if echo "$PY_VER" | grep -q "3.12\|3.13"; then
            print_success "Python: $PY_VER"
        else
            print_warning "Python: $PY_VER (Recommended: 3.12+)"
        fi
    else
        print_error "Python: Not installed"
    fi
    
    # Node.js
    if command_exists node; then
        NODE_VER=$(node --version)
        if echo "$NODE_VER" | grep -q "v2[0-9]"; then
            print_success "Node.js: $NODE_VER"
        else
            print_warning "Node.js: $NODE_VER (Recommended: 20+)"
        fi
        print_success "npm: $(npm --version)"
    else
        print_error "Node.js: Not installed"
    fi
    
    # Optional tools
    echo ""
    echo "Optional Tools:"
    echo "---------------"
    
    if command_exists jq; then
        print_success "jq: $(jq --version)"
    else
        print_warning "jq: Not installed"
    fi
    
    if command_exists k9s; then
        print_success "k9s: $(k9s version --short 2>/dev/null || echo 'installed')"
    else
        print_warning "k9s: Not installed"
    fi
    
    if command_exists oc; then
        print_success "OpenShift CLI: $(oc version --client 2>/dev/null | head -n 1)"
    else
        print_error "OpenShift CLI: Not installed"
    fi

    if command_exists psql; then
        print_success "psql: $(psql --version)"
    else
        print_error "psql: Not installed"
    fi

    if command_exists redis-cli; then
        print_success "redis-cli: $(redis-cli --version)"
    else
        print_error "redis-cli: Not installed"
    fi

    if command_exists kcat; then
        print_success "kcat: $(kcat -V | head -n 1)"
    else
        print_error "kcat: Not installed"
    fi
    
    echo ""
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║         PayU Development Environment Setup Script             ║${NC}"
    echo -e "${GREEN}║                    Version 1.0.0                               ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    detect_os
    
    case "${1:-full}" in
        --check|-c)
            check_versions
            ;;
        --backend|-b)
            install_base_packages
            install_java
            install_maven
            install_python
            check_versions
            ;;
        --frontend|-f)
            install_base_packages
            install_nodejs
            check_versions
            ;;
        --docker|-d)
            install_base_packages
            install_docker
            check_versions
            ;;
        --project|-p)
            setup_project
            ;;
        --help|-h)
            echo "Usage: ./setup.sh [OPTION]"
            echo ""
            echo "Options:"
            echo "  (none)       Full installation (all components)"
            echo "  --backend    Install backend tools (Java, Maven, Python)"
            echo "  --frontend   Install frontend tools (Node.js, npm)"
            echo "  --docker     Install Docker only"
            echo "  --project    Setup project dependencies (npm install, etc)"
            echo "  --check      Check installed versions"
            echo "  --help       Show this help message"
            echo ""
            ;;
        *)
            # Full installation
            install_base_packages
            install_docker
            install_java
            install_maven
            install_python
            install_nodejs
            install_db_clients
            install_cloud_clis
            install_additional_tools
            setup_project
            check_versions
            
            print_section "Installation Complete!"
            echo ""
            echo "Next steps:"
            echo "  1. Log out and back in (for docker group)"
            echo "  2. Run: docker-compose up -d"
            echo "  3. Run: cd frontend/web-app && npm run dev"
            echo ""
            echo "For help: ./setup.sh --help"
            echo ""
            ;;
    esac
}

main "$@"