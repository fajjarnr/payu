# PayU Podman Build Scripts

## Overview

This directory contains Podman-based build and security scanning scripts for the PayU platform. All scripts are designed to work with rootless Podman (no sudo required) and include support for quadlet systemd integration.

## Scripts

### 1. `build-all-podman.sh`
Build all PayU microservices in parallel using Podman.

**Features:**
- Parallel builds with configurable jobs
- Buildah integration for advanced builds
- Pod structure for related containers
- Quadlet service file generation
- Rootless execution

**Usage:**
```bash
# Build all services with defaults
./build-all-podman.sh

# Build with custom tag and 8 parallel jobs
./build-all-podman.sh -t payu:v1.0.0 -j 8

# Build and push to Docker Hub
./build-all-podman.sh -p -r docker.io/myorg

# Build with verbose output
./build-all-podman.sh -v
```

### 2. `build-service-podman.sh`
Build a single PayU microservice using Podman.

**Features:**
- Single service builds
- Automatic Containerfile discovery
- Quadlet systemd service file generation
- Build context detection
- Multi-stage build support

**Usage:**
```bash
# Build account-service
./build-service-podman.sh account-service

# Build with specific tag
./build-service-podman.sh gateway-service -t v1.0.0

# Build and push to Docker Hub
./build-service-podman.sh wallet-service -p -r docker.io/myorg

# Build with verbose output
./build-service-podman.sh auth-service -v
```

### 3. `scan-images-podman.sh`
Scan PayU container images for vulnerabilities using Trivy.

**Features:**
- Multiple output formats (table, json, sarif, junit)
- Severity level filtering
- HTML report generation
- Parallel scanning support
- Comprehensive vulnerability detection

**Usage:**
```bash
# Scan all local images
./scan-images-podman.sh -a

# Scan specific image
./scan-images-podman.sh -t account-service:1.4.0

# Scan with high severity only
./scan-images-podman.sh -a -s HIGH

# Generate JSON report
./scan-images-podman.sh -a -j -o report.json

# Scan with verbose output
./scan-images-podman.sh -t gateway-service -v
```

## Installation

### Prerequisites
```bash
# Install Podman (Ubuntu 22.04+)
sudo apt update
sudo apt install -y podman buildah jq

# Install Trivy
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
```

### Configuration
Copy the environment file and customize:
```bash
cp infrastructure/quadlet/.env.example .env
# Edit .env as needed
```

## Quadlet Integration

The build scripts automatically generate quadlet files in `infrastructure/quadlet/` for systemd integration.

### Example systemd Service
After building a service, you can manage it with systemd:
```bash
# Enable and start the service
sudo systemctl enable --now account-service

# Check status
sudo systemctl status account-service

# View logs
sudo journalctl -u account-service -f
```

### Environment Variables
- `PODMAN_PLATFORM`: Target platform (e.g., `linux/amd64`)
- `PODMAN_NETWORK`: Network to use
- `BUILD_ARGS`: Additional build arguments
- `TRIVY_CACHE_DIR`: Trivy cache directory
- `TRIVY_TIMEOUT`: Scan timeout

## Examples

### Building Services
```bash
# Quick build of all services
./build-all-podman.sh

# Production build with specific tags
./build-all-podman.sh -t payu-prod:$(date +%Y%m%d) -j 8 -p -r docker.io/payu

# Development build with fast builds
export BUILDKIT_BUILD=1
./build-all-podman.sh -j 2
```

### Scanning Images
```bash
# Quick security scan
./scan-images-podman.sh -a -s CRITICAL,HIGH

# Detailed scan with JSON report
./scan-images-podman.sh -a -s HIGH -f json -o security-report.json

# CI/CD pipeline scan
./scan-images-podman.sh -t myapp:1.4.0 -s HIGH -f junit -o security-report.xml
```

### Running Services
```bash
# Build and run a service
./build-service-podman.sh account-service
podman run -d --name account-service -p 8080:8080 localhost/account-service:1.4.0

# Or use quadlet service
sudo systemctl enable --now account-service
```

## Troubleshooting

### Common Issues

1. **Permission Denied**
   - Ensure Podman is installed correctly
   - Check user is in the `podman` group: `podman ps`

2. **Build Failures**
   - Check Containerfile syntax
   - Verify build context paths
   - Review build logs with `-v` flag

3. **Scan Failures**
   - Ensure Trivy is installed and updated
   - Check image exists: `podman images`
   - Verify Trivy cache permissions

### Debug Mode
All scripts support verbose output for debugging:
```bash
# Enable verbose mode
./build-all-podman.sh -v
./build-service-podman.sh account-service -v
./scan-images-podman.sh -a -v
```

## Best Practices

1. **Use Parallel Builds**
   - Adjust `-j` flag based on CPU cores
   - Start with 4 jobs and adjust as needed

2. **Tag Management**
   - Use semantic versioning for production
   - Include build date for CI/CD
   - Use separate tags for environments

3. **Security Scanning**
   - Scan at multiple stages
   - Use severity filters as needed
   - Generate reports for compliance

4. **Resource Management**
   - Monitor build memory usage
   - Clean up unused images: `podman image prune`
   - Use build cache efficiently
