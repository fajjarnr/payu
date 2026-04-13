# Container Security Guide

## Overview

This document outlines container security best practices for the PayU Digital Banking Platform. Following these guidelines ensures compliance with PCI-DSS, OJK regulations, and industry security standards.

## Important Note: Podman Compatibility

This guide has been updated to use Podman instead of Docker. Podman provides enhanced security features:

- Rootless container execution by default
- Pod management for multi-container applications
- No daemon dependency
- Native SELinux support
- Compatible with Docker command interface for most operations

Key changes:

- `podman` commands replaced with `podman`
- Rootless execution is default behavior in Podman
- Podman supports all container security features mentioned in this document

**Document Version:** 1.0.0
**Last Updated:** 2026-01-30
**Maintained By:** Platform Engineering Team

---

## Table of Contents

1. [Security Best Practices Checklist](#security-best-practices-checklist)
2. [OCI Label Requirements](#oci-label-requirements)
3. [Build Commands with BUILDKIT=1](#build-commands-with-buildkit1)
4. [Environment Variable Requirements](#environment-variable-requirements)
5. [.env File Usage Instructions](#env-file-usage-instructions)
6. [Security Scanning Commands](#security-scanning-commands)
7. [Container Signing Guidelines](#container-signing-guidelines)
8. [Runtime Security](#runtime-security)
9. [CI/CD Integration](#cicd-integration)

---

## Security Best Practices Checklist

### Image Build Security

- [ ] **Use Red Hat UBI (Universal Base Image) base images** for all services
- [ ] **Always use multi-stage builds** to separate build and runtime dependencies
- [ ] **Use minimal runtime images** (e.g., `ubi9/openjdk-21-runtime` instead of `ubi9/openjdk-21`)
- [ ] **Never run as root user** - use UID 185 for Java services, 1001 for Node.js
- [ ] **Set filesystem to read-only** where possible (add `--read-only` flag)
- [ ] **Remove package manager caches** after installation (`microdnf clean all`, `dnf clean all`)
- [ ] **Use specific image versions** (e.g., `ubi9/openjdk-21:1.20`, not `latest`)
- [ ] **Include HEALTHCHECK instructions** in all Dockerfiles
- [ ] **Set appropriate memory limits** using container-aware JVM flags

### Vulnerability Management

- [ ] **Scan images before deployment** using Trivy or Grype
- [ ] **Fix CRITICAL vulnerabilities within 7 days**
- [ ] **Fix HIGH vulnerabilities within 30 days**
- [ ] **Maintain vulnerability scan reports** for audit purposes
- [ ] **Subscribe to UBI security updates** via Red Hat Security Advisory
- [ ] **Automate dependency updates** using Dependabot or Renovate

### Secrets Management

- [ ] **NEVER commit .env files** to version control
- [ ] **NEVER hardcode credentials** in Dockerfiles
- [ ] **Use build-time secrets** for private dependencies (`--secret` flag)
- [ ] **Mount secrets from HashiCorp Vault** or OpenShift Secrets at runtime
- [ ] **Use environment variables** for runtime configuration only
- [ ] **Rotate secrets regularly** (minimum every 90 days)

### Network Security

- [ ] **Expose only necessary ports** using EXPOSE instruction
- [ ] **Use service mesh** (Istio/OpenShift Service Mesh) for mTLS
- [ ] **Configure network policies** to restrict pod-to-pod communication
- [ ] **Disable unnecessary services** in the base image

### Compliance

- [ ] **Maintain OCI metadata labels** for all images
- [ ] **Sign all production images** using cosign or Notary
- [ ] **Document SBOM (Software Bill of Materials)** for each image
- [ ] **Keep image scan records** for 7 years (OJK requirement)
- [ ] **Use approved base images only** from registry.access.redhat.com

---

## OCI Label Requirements

All PayU container images MUST include the following OCI (Open Container Initiative) labels:

### Required Labels (Mandatory)

```podmanfile
# Standard OCI Labels (REQUIRED for all images)
LABEL org.opencontainers.image.title="PayU [Service Name]"
LABEL org.opencontainers.image.description="[Brief service description]"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="PayU Indonesia"
LABEL org.opencontainers.image.authors="PayU Platform Team"
LABEL org.opencontainers.image.licenses="Proprietary"
```

### Build Information Labels

```podmanfile
# Build-time information (set via build arguments)
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.revision="${GIT_COMMIT}"
LABEL org.opencontainers.image.source="https://github.com/payu/[service-path]"
```

### PayU-Specific Labels

```podmanfile
# Service classification labels
LABEL id.payu.service.tier="1"                    # 1=customer-facing, 2=internal, 3=data
LABEL id.payu.service.domain="[domain]"           # backend, frontend, analytics, etc.
LABEL id.payu.compliance.pci-dss="true"           # true if handles payment data
LABEL id.payu.compliance.ojk="true"               # true if regulated by OJK
LABEL id.payu.security.scan-level="high"          # low, medium, high
LABEL id.payu.data.classification="confidential"  # public, internal, confidential, restricted
```

### Example: Complete Label Set

```podmanfile
####
# Metadata Labels - Gateway Service
####
LABEL org.opencontainers.image.title="PayU Gateway Service"
LABEL org.opencontainers.image.description="API Gateway with rate limiting, circuit breaker, and routing"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="PayU Indonesia"
LABEL org.opencontainers.image.url="https://payu.fajjjar.my.id"
LABEL org.opencontainers.image.source="https://github.com/payu/backend/gateway-service"
LABEL org.opencontainers.image.authors="PayU Backend Team"
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.revision="${GIT_COMMIT}"
LABEL id.payu.service.tier="1"
LABEL id.payu.service.domain="backend"
LABEL id.payu.compliance.pci-dss="true"
LABEL id.payu.compliance.ojk="true"
LABEL id.payu.security.scan-level="high"
LABEL id.payu.data.classification="confidential"
```

### Label Enforcement

Labels are enforced via CI/CD pipeline. Images without required labels will be rejected:

```bash
# Check labels on existing image
podman inspect payu/gateway-service:1.0.0 --format='{{json .Config.Labels}}' | jq
```

---

## Build Commands with BUILDKIT=1

### Enable BuildKit

BuildKit MUST be enabled for all builds to ensure security, caching, and performance:

```bash
# Method 1: Environment variable (Recommended)
export DOCKER_BUILDKIT=1

# Method 2: Docker daemon config
# Add to /etc/podman/daemon.json:
# { "features": { "buildkit": true } }

# Method 3: Buildx (for multi-platform builds)
podman buildx build --build-arg BUILDKIT_INLINE_CACHE=1
```

### Standard Build Command Template

```bash
DOCKER_BUILDKIT=1 podman build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    --build-arg VERSION=1.0.0 \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    -t payu/[service-name]:1.0.0 \
    -t payu/[service-name]:latest \
    [context-path]
```

### Secure Build with Secrets

```bash
# For private Maven repositories
DOCKER_BUILDKIT=1 podman build \
    --secret=id=settings.xml,src=~/.m2/settings.xml \
    -t payu/[service-name]:latest .

# For private npm packages
DOCKER_BUILDKIT=1 podman build \
    --secret=id=npmrc,src=~/.npmrc \
    -t payu/web-app:latest .
```

### Build with Cache Export

```bash
# Export cache to registry for CI/CD
DOCKER_BUILDKIT=1 podman build \
    --cache-from=type=registry,ref=payu.azurecr.io/[service]:cache \
    --cache-to=type=registry,ref=payu.azurecr.io/[service]:cache,mode=max \
    -t payu/[service]:latest \
    [context-path]
```

---

## Environment Variable Requirements

### Required Build Arguments

All PayU images MUST support these build arguments:

```podmanfile
# Build-time metadata arguments
ARG BUILD_DATE
ARG GIT_COMMIT
ARG VERSION=1.0.0

# Use them in labels
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.revision="${GIT_COMMIT}"
LABEL org.opencontainers.image.version="${VERSION}"
```

### Runtime Environment Variables

#### Common Variables (All Services)

```bash
# Application environment
APP_ENV=production              # development, staging, production
APP_DEBUG=false                 # Must be false in production
LOG_LEVEL=info                  # debug, info, warn, error
TZ=Asia/Jakarta                 # Timezone

# Observability
OTEL_SERVICE_NAME=[service-name]
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=otlp
```

#### Java Services (Spring Boot/Quarkus)

```bash
# JVM configuration
JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:MinRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseStringDeduplication"

# Application properties
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/payu
SPRING_DATASOURCE_USERNAME=${POSTGRES_USER}
SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}
```

#### Python Services (FastAPI)

```bash
# Python configuration
PYTHONUNBUFFERED=1
PYTHONDONTWRITEBYTECODE=1
LOG_LEVEL=INFO

# Service-specific
WORKERS=4
MAX_REQUESTS=1000
MAX_REQUESTS_JITTER=100
```

#### Node.js Services (Next.js)

```bash
# Node.js configuration
NODE_ENV=production
NEXT_TELEMETRY_DISABLED=1
PORT=3000
HOSTNAME=0.0.0.0
```

### Environment Variable Security

1. **Never pass secrets via build arguments** - they're visible in image history
2. **Use runtime environment variables** for secrets
3. **Use .env files locally only** - never commit them
4. **Validate required variables** at application startup

---

## .env File Usage Instructions

### Purpose

`.env` files are for **local development only**. They must NEVER be committed to version control.

### Setup Instructions

```bash
# 1. Copy the template
cp .env.example .env

# 2. Edit with your local values
nano .env

# 3. Set restrictive permissions
chmod 600 .env

# 4. Load environment variables
source .env  # or export $(cat .env | xargs)
```

### .gitignore Configuration

Ensure `.env` is in `.gitignore`:

```gitignore
# Environment variables
.env
.env.local
.env.*.local
!.env.example
```

### Environment Variable Validation

All services should validate required environment variables at startup:

```java
// Spring Boot example
@Component
public class EnvValidator implements ApplicationRunner {
    @Value("${required.property}")
    private String requiredProp;

    @Override
    public void run(ApplicationArguments args) {
        if (requiredProp == null) {
            throw new RuntimeException("Missing required property");
        }
    }
}
```

### Production Environment Variables

For production, use:

1. **OpenShift Secrets:** `oc create secret generic payu-secrets --from-literal=DB_PASSWORD=xxx`
2. **HashiCorp Vault:** Store secrets in Vault, inject via annotations
3. **External Secrets Operator:** Sync external secrets to Kubernetes

---

## Security Scanning Commands

### Trivy (Recommended)

Trivy is a comprehensive security scanner for containers.

#### Installation

```bash
# Linux/macOS
brew install trivy

# Docker
podman run --rm -v /var/run/podman.sock:/var/run/podman.sock \
    aquasec/trivy:latest image [image-name]
```

#### Scan Commands

```bash
# Scan local image
trivy image --severity CRITICAL,HIGH payu/account-service:1.0.0

# Scan with output format (for CI/CD)
trivy image --format json --output trivy-report.json payu/account-service:1.0.0

# Scan with exit code on vulnerabilities
trivy image --severity CRITICAL --exit-code 1 payu/account-service:1.0.0

# Scan and generate SARIF report
trivy image --format sarif --output trivy.sarif payu/account-service:1.0.0

# Scan for misconfigurations
trivy config --severity CRITICAL,HIGH .

# Scan filesystem
trivy fs --security-checks vuln,config /path/to/project
```

#### Trivy Configuration

Create `.trivyignore` for false positives:

```text
# Acceptable risks
CVE-2020-28476  # False positive - not used in our codebase
```

### Grype (Alternative)

```bash
# Install
brew install grype

# Scan image
grype payu/account-service:1.0.0

# Scan with severity filter
grype payu/account-service:1.0.0 --only-fixed

# Generate report
grype payu/account-service:1.0.0 -o json > grype-report.json
```

### Docker Scout (Native)

```bash
# Scan image
podman scout quickview payu/account-service:1.0.0

# View vulnerabilities
podman scout cves payu/account-service:1.0.0

# Compare images
podman scout compare payu/account-service:1.0.0 payu/account-service:1.0.1
```

### Integrated Scanning Pipeline

```bash
#!/bin/bash
# scan-image.sh - Comprehensive container scanning

IMAGE=$1
OUTPUT_DIR="./scan-results"
mkdir -p $OUTPUT_DIR

echo "Scanning $IMAGE with Trivy..."
trivy image --severity CRITICAL,HIGH \
    --format json \
    --output $OUTPUT_DIR/trivy-report.json \
    $IMAGE

echo "Scanning $IMAGE with Grype..."
grype $IMAGE \
    --output json \
    --file $OUTPUT_DIR/grype-report.json

echo "Generating SBOM..."
syft $IMAGE -o json > $OUTPUT_DIR/sbom.json

echo "Checking for critical vulnerabilities..."
if trivy image --severity CRITICAL --exit-code 1 $IMAGE; then
    echo "CRITICAL vulnerabilities found! Build failed."
    exit 1
fi

echo "Scan complete. Results in $OUTPUT_DIR"
```

---

## Container Signing Guidelines

### Why Sign Containers?

Container signing ensures:

- **Image authenticity** - verify the image source
- **Image integrity** - detect tampering
- **Supply chain security** - meet compliance requirements

### Sigstore/cosign (Recommended)

#### Installation

```bash
# Linux/macOS
brew install cosign

# Docker
podman pull ghcr.io/sigstore/cosign/cosign:latest
```

#### Generate Signing Key

```bash
# Generate key pair (one-time setup)
cosign generate-key-pair

# This creates:
# - cosign.key (private key - KEEP SECRET)
# - cosign.pub (public key - distribute)
```

#### Sign an Image

```bash
# Sign image
cosign sign --key cosign.key payu/account-service:1.0.0

# Sign with annotations
cosign sign --key cosign.key \
    -a "commit=$(git rev-parse HEAD)" \
    -a "build_url=https://ci.payu.fajjjar.my.id/build/123" \
    payu/account-service:1.0.0
```

#### Verify an Image

```bash
# Verify image
cosign verify --key cosign.pub payu/account-service:1.0.0

# Verify with annotations
cosign verify --key cosign.pub \
    -a "commit=$(git rev-parse HEAD)" \
    payu/account-service:1.0.0
```

### Notary v2 (Alternative)

```bash
# Sign with Notary
notary sign payu/account-service:1.0.0

# Verify
notary verify payu/account-service:1.0.0
```

### Keyless Signing (Fulcio)

For CI/CD pipelines without key management:

```bash
# Enable OIDC token
export COSIGN_EXPERIMENTAL=1

# Sign with keyless authentication
cosign sign payu/account-service:1.0.0

# Verify
cosign verify payu/account-service:1.0.0
```

### SBOM Generation

Generate Software Bill of Materials (SBOM) for compliance:

```bash
# Using Syft
syft payu/account-service:1.0.0 -o json > sbom.json

# Attach SBOM to image
cosign attach sbom payu/account-service:1.0.0 --sbom sbom.json

# Sign SBOM
cosign sign --key cosign.key payu/account-service:1.0.0

# Verify SBOM
cosign verify --key cosign.pub payu/account-service:1.0.0
```

---

## Runtime Security

### Container Runtime Security

```bash
# Run with read-only root filesystem
podman run --read-only --tmpfs /tmp payu/service:latest

# Drop all capabilities
podman run --cap-drop=ALL payu/service:latest

# Run as non-root user
podman run -u 185 payu/service:latest

# Set resource limits
podman run --memory="512m" --cpus="1.0" payu/service:latest

# Set security options
podman run --security-opt=no-new-privileges payu/service:latest
```

### OpenShift Security Context

```yaml
# deployment.yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 185
  fsGroup: 185
  seccompProfile:
    type: RuntimeDefault
  capabilities:
    drop:
      - ALL
  readOnlyRootFilesystem: true
```

### Pod Security Standards

Apply OpenShift Pod Security Standards:

```bash
# Apply restricted profile
oc label ns payu \
    pod-security.kubernetes.io/enforce=restricted \
    pod-security.kubernetes.io/audit=restricted \
    pod-security.kubernetes.io/warn=restricted
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build and Scan

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Docker Buildx
        uses: podman/setup-buildx-action@v2

      - name: Build image
        run: |
          DOCKER_BUILDKIT=1 podman build \
            --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
            --build-arg GIT_COMMIT=${{ github.sha }} \
            -t payu/service:${{ github.sha }} .

      - name: Scan with Trivy
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: payu/service:${{ github.sha }}
          format: "sarif"
          output: "trivy-results.sarif"

      - name: Sign with cosign
        run: |
          echo "${{ secrets.COSIGN_KEY }}" > cosign.key
          cosign sign --key cosign.key payu/service:${{ github.sha }}

      - name: Generate SBOM
        run: |
          syft payu/service:${{ github.sha }} -o json > sbom.json
```

### Tekton Pipeline Example

```yaml
apiVersion: tekton.dev/v1
kind: Task
metadata:
  name: container-security-scan
spec:
  params:
    - name: IMAGE_URL
      type: string
  steps:
    - name: trivy-scan
      image: aquasec/trivy:latest
      command:
        - trivy
      args:
        - image
        - --severity
        - CRITICAL,HIGH
        - --exit-code
        - "1"
        - $(params.IMAGE_URL)
```

## Podman Security Advantages

Podman provides several security advantages over traditional container runtimes:

### Rootless Execution

All containers run as non-root by default, reducing attack surface:

```bash
# Check container user (shows non-root by default)
podman run --rm payu/account-service:latest id

# Force root user (not recommended)
podman run --user root payu/account-service:latest
```

### Enhanced Security Features

```bash
# Run with read-only root filesystem
podman run --read-only --tmpfs /tmp payu/service:latest

# Drop all capabilities (more restrictive than Docker)
podman run --cap-drop=ALL payu/service:latest

# No new privileges flag (default in Podman)
podman run payu/service:latest

# SELinux enabled by default
podman run --security-opt label=disable payu/service:latest  # Only if needed
```

### Pod Isolation

Pods provide better isolation for multi-container applications:

```bash
# Create security-enhanced pod
podman pod create --name secure-pod \
    --share net \
    --share cgroup \
    --infra-conmon-pidfile=/run/podman/pods/$PODNAME/infra.pid

# Run containers in pod with shared namespace
podman run --pod secure-pod --name account-service -d payu/account-service:latest
podman run --pod secure-pod --name auth-service -d payu/auth-service:latest
```

### Containerfile Security

Using Containerfile instead of Dockerfile for future-proofing:

```dockerfile
# Containerfile with security best practices
FROM registry.access.redhat.com/ubi9/openjdk-21:1.20

# Non-root user
USER 185

# Read-only filesystem where possible
RUN mkdir /app && chown 185:0 /app
WORKDIR /app

# Copy and set permissions
COPY --chown=185:0 *.jar /app/
```

---

## Appendix

### Useful Commands

```bash
# View image labels
podman inspect payu/service:latest --format='{{json .Config.Labels}}' | jq

# View image history
podman history payu/service:latest --no-trunc

# Check image layers
podman inspect payu/service:latest --format='{{json .RootFS.Layers}}' | jq

# Find large layers
podman history payu/service:latest --format "table {{.Size}}\t{{.CreatedBy}}" | sort -hr

# Remove build cache
podman builder prune

# Clean everything
podman system prune -a --volumes
```

### Resources

- [OCI Image Specification](https://github.com/opencontainers/image-spec)
- [Red Hat UBI Documentation](https://www.redhat.com/en/topics/linux/what-is-ubi)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
- [cosign Documentation](https://sigstore.github.io/cosign/)
- [Docker Security Best Practices](https://docs.podman.com/engine/security/)

---

**Document Version:** 1.0.1
**Last Updated:** 2026-01-30
**Maintained By:** Platform Engineering Team
**Next Review:** 2026-07-30
