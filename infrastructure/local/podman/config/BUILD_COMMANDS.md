# Container Build Commands Reference

## Overview

This document provides comprehensive build commands for all PayU Digital Banking Platform services. It includes Java (Spring Boot/Quarkus), Python (FastAPI), Next.js, multi-platform builds, security scanning, and tagging strategies.

## Important Note: Podman Compatibility

This guide has been updated to use Podman instead of Docker. Podman provides:
- Docker-compatible command interface
- Rootless container execution by default
- Enhanced security with SELinux integration
- Pod management for multi-container applications
- No daemon dependency

Key changes in this document:
- `podman` commands replaced with `podman`
- All build and scan commands work identically
- Podman is preferred for security and compliance
- Consider using `Containerfile` instead of `Dockerfile` for future-proofing

**Document Version:** 1.0.0
**Last Updated:** 2026-01-30
**Maintained By:** Platform Engineering Team

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Java Build Commands](#java-build-commands)
3. [Python Build Commands](#python-build-commands)
4. [Next.js Build Commands](#nextjs-build-commands)
5. [Multi-Platform Builds](#multi-platform-builds)
6. [BuildKit Cache Strategies](#buildkit-cache-strategies)
7. [Security Scanning Commands](#security-scanning-commands)
8. [Tagging and Versioning](#tagging-and-versioning)
9. [CI/CD Examples](#cicd-examples)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Enable BuildKit

```bash
# BuildKit is integrated with podman buildx
# Buildx is automatically enabled with podman
podman buildx version
```

### Required Build Arguments

All builds MUST include these arguments:

```bash
BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')
GIT_COMMIT=$(git rev-parse --short HEAD)
VERSION=1.0.0
```

---

## Java Build Commands

### Spring Boot Services

#### Standard Build (Development)

```bash
# Account Service
podman build \
    -t payu/account-service:latest \
    backend/account-service/

# Auth Service
podman build \
    -t payu/auth-service:latest \
    backend/auth-service/

# Transaction Service
podman build \
    -t payu/transaction-service:latest \
    backend/transaction-service/
```

#### Production Build with Build Arguments

```bash
podman build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    --build-arg VERSION=1.0.0 \
    -t payu/account-service:1.0.0 \
    -t payu/account-service:latest \
    backend/account-service/
```

#### Build with Maven Cache Mount

```bash
podman build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    --build-arg VERSION=1.0.0 \
    --cache-from=type=local,src=/tmp/maven-cache \
    --cache-to=type=local,dest=/tmp/maven-cache \
    -t payu/account-service:1.0.0 \
    backend/account-service/
```

#### Build with Private Maven Repository

```bash
podman build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    --secret=id=settings.xml,src=~/.m2/settings.xml \
    -t payu/account-service:1.0.0 \
    backend/account-service/
```

### Quarkus Services

#### Standard Native Build (Long build time, fast startup)

```bash
# Gateway Service (Native)
podman build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    -f backend/gateway-service/Dockerfile.native \
    -t payu/gateway-service:1.0.0-native \
    backend/gateway-service/

# Note: Native builds require significant memory and time
# Recommended: Use fast-jar for development, native for production
```

#### Fast-JAR Build (Quick build, good startup)

```bash
podman build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    -t payu/gateway-service:1.0.0 \
    backend/gateway-service/

# Default Dockerfile uses fast-jar
```

### Java Service Batch Build (All Services)

```bash
#!/bin/bash
# build-all-java.sh - Build all Java services in parallel

SERVICES=(
    "account-service"
    "auth-service"
    "transaction-service"
    "wallet-service"
    "gateway-service"
)

for SERVICE in "${SERVICES[@]}"; do
    echo "Building $SERVICE..."
    podman build \
        -t payu/$SERVICE:latest \
        backend/$SERVICE/ &
done

wait
echo "All Java services built successfully"
```

---

## Python Build Commands

### FastAPI Services

#### Standard Build (Development)

```bash
# KYC Service
podman build \
    -t payu/kyc-service:latest \
    backend/kyc-service/

# Analytics Service
podman build \
    -t payu/analytics-service:latest \
    backend/analytics-service/
```

#### Production Build with Build Arguments

```bash
podman build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    --build-arg VERSION=1.0.0 \
    --build-arg PYTHONDONTWRITEBYTECODE=1 \
    --build-arg PYTHONUNBUFFERED=1 \
    -t payu/kyc-service:1.0.0 \
    -t payu/kyc-service:latest \
    backend/kyc-service/
```

#### Build with pip Cache Mount

```bash
podman build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    --cache-from=type=local,src=/tmp/pip-cache \
    --cache-to=type=local,dest=/tmp/pip-cache \
    -t payu/kyc-service:1.0.0 \
    backend/kyc-service/
```

#### Build with Private PyPI Repository

```bash
podman build \
    --secret=id=pip.conf,src=~/.pip/pip.conf \
    -t payu/kyc-service:1.0.0 \
    backend/kyc-service/
```

### Python Service Batch Build

```bash
#!/bin/bash
# build-all-python.sh - Build all Python services

PYTHON_SERVICES=(
    "kyc-service"
    "analytics-service"
)

for SERVICE in "${PYTHON_SERVICES[@]}"; do
    echo "Building $SERVICE..."
    podman build \
        -t payu/$SERVICE:latest \
        backend/$SERVICE/ &
done

wait
echo "All Python services built successfully"
```

---

## Next.js Build Commands

### Standard Build (Development)

```bash
# Web App
podman build \
    -t payu/web-app:latest \
    frontend/web-app/

# Developer Docs
podman build \
    -t payu/developer-docs:latest \
    frontend/developer-docs/
```

### Production Build with Build Arguments

```bash
podman build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    --build-arg VERSION=1.0.0 \
    --build-arg NODE_ENV=production \
    --build-arg NEXT_TELEMETRY_DISABLED=1 \
    -t payu/web-app:1.0.0 \
    -t payu/web-app:latest \
    frontend/web-app/
```

### Build with npm Cache Mount

```bash
podman build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    --cache-from=type=local,src=/tmp/npm-cache \
    --cache-to=type=local,dest=/tmp/npm-cache,mode=max \
    -t payu/web-app:1.0.0 \
    frontend/web-app/
```

### Build with Private npm Registry

```bash
podman build \
    --secret=id=npmrc,src=~/.npmrc \
    -t payu/web-app:1.0.0 \
    frontend/web-app/
```

### Next.js Standalone Build (Optimized)

```bash
# Ensure next.config.js has output: 'standalone'
podman build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    -t payu/web-app:1.0.0-standalone \
    frontend/web-app/
```

---

## Multi-Platform Builds

### Buildx Setup

```bash
# Create a new builder instance
podman buildx create --name payu-builder --use

# Start the builder
podman buildx inspect --bootstrap

# Verify platforms
podman buildx ls
```

### Multi-Platform Build (Linux amd64 + arm64)

```bash
# Java Service
podman buildx build \
    --platform linux/amd64,linux/arm64 \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    -t payu/account-service:1.0.0 \
    --push \
    backend/account-service/

# Python Service
podman buildx build \
    --platform linux/amd64,linux/arm64 \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    -t payu/kyc-service:1.0.0 \
    --push \
    backend/kyc-service/

# Next.js Service
podman buildx build \
    --platform linux/amd64,linux/arm64 \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    -t payu/web-app:1.0.0 \
    --push \
    frontend/web-app/
```

### Platform-Specific Builds

```bash
# Build for specific platform
podman buildx build \
    --platform linux/arm64 \
    -t payu/account-service:1.0.0-arm64 \
    --load \
    backend/account-service/
```

---

## BuildKit Cache Strategies

### Local Cache

```bash
podman build \
    --cache-from=type=local,src=/tmp/buildkit-cache \
    --cache-to=type=local,dest=/tmp/buildkit-cache,mode=max \
    -t payu/account-service:1.0.0 \
    backend/account-service/
```

### Registry Cache

```bash
podman buildx build \
    --cache-from=type=registry,ref=payu.azurecr.io/account-service:cache \
    --cache-to=type=registry,ref=payu.azurecr.io/account-service:cache,mode=max \
    -t payu/account-service:1.0.0 \
    --push \
    backend/account-service/
```

### Inline Cache

```bash
podman buildx build \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    --cache-from=payu.azurecr.io/account-service:latest \
    -t payu.azurecr.io/account-service:1.0.0 \
    --push \
    backend/account-service/
```

### GitHub Actions Cache

```bash
podman buildx build \
    --cache-from=type=gha \
    --cache-to=type=gha,mode=max \
    -t payu/account-service:1.0.0 \
    backend/account-service/
```

### Cache Mount in Dockerfile

```podmanfile
# Maven cache mount
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# npm cache mount
RUN --mount=type=cache,target=/root/.npm \
    npm ci --ignore-scripts

# pip cache mount
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install --no-cache-dir -r requirements.txt
```

---

## Security Scanning Commands

### Trivy Scanning

#### Scan After Build

```bash
# Scan local image
trivy image payu/account-service:1.0.0

# Scan with severity filter
trivy image --severity CRITICAL,HIGH payu/account-service:1.0.0

# Scan and exit on vulnerabilities
trivy image --severity CRITICAL --exit-code 1 payu/account-service:1.0.0

# Generate SARIF report (for GitHub Security)
trivy image --format sarif --output trivy-report.sarif payu/account-service:1.0.0

# Generate JSON report
trivy image --format json --output trivy-report.json payu/account-service:1.0.0
```

#### Scan During Build (Inline)

```bash
#!/bin/bash
# build-and-scan.sh

IMAGE_NAME=$1
VERSION=$2

# Build the image
podman build \
    -t $IMAGE_NAME:$VERSION \
    .

# Scan the image
trivy image --severity CRITICAL,HIGH $IMAGE_NAME:$VERSION

# Exit if vulnerabilities found
if [ $? -ne 0 ]; then
    echo "Security scan failed! Vulnerabilities found."
    exit 1
fi

echo "Build and scan successful"
```

### Grype Scanning

```bash
# Scan image
grype payu/account-service:1.0.0

# Scan with severity filter
grype payu/account-service:1.0.0 --only-fixed

# Generate JSON report
grype payu/account-service:1.0.0 -o json > grype-report.json

# Scan and fail on vulnerabilities
grype payu/account-service:1.0.0 --fail-on severe
```

### Docker Scout (Native)

```bash
# Quick view of vulnerabilities
podman scout quickview payu/account-service:1.0.0

# Detailed CVEs
podman scout cves payu/account-service:1.0.0

# Compare two images
podman scout compare \
    payu/account-service:1.0.0 \
    payu/account-service:1.0.1
```

### SBOM Generation

```bash
# Using Syft
syft payu/account-service:1.0.0 -o json > sbom.json

# Using Syft with SPDX format
syft payu/account-service:1.0.0 -o spdx-json > sbom.spdx.json

# Using Syft with CycloneDX format
syft payu/account-service:1.0.0 -o cyclonedx-json > sbom.cdx.json
```

### Comprehensive Security Scan

```bash
#!/bin/bash
# comprehensive-scan.sh

IMAGE=$1
OUTPUT_DIR="./security-scans"
mkdir -p $OUTPUT_DIR

echo "=== Comprehensive Security Scan for $IMAGE ==="

# Trivy scan
echo "Running Trivy scan..."
trivy image --severity CRITICAL,HIGH \
    --format json \
    --output $OUTPUT_DIR/trivy-report.json \
    $IMAGE

# Grype scan
echo "Running Grype scan..."
grype $IMAGE \
    --output json \
    --file $OUTPUT_DIR/grype-report.json

# Generate SBOM
echo "Generating SBOM..."
syft $IMAGE -o json > $OUTPUT_DIR/sbom.json
syft $IMAGE -o spdx-json > $OUTPUT_DIR/sbom.spdx.json

# Check for critical vulnerabilities
echo "Checking for critical vulnerabilities..."
if trivy image --severity CRITICAL --exit-code 1 $IMAGE; then
    echo "No critical vulnerabilities found"
else
    echo "CRITICAL vulnerabilities detected!"
    exit 1
fi

echo "Security scan complete. Results in $OUTPUT_DIR"
```

---

## Tagging and Versioning

### Semantic Versioning

```bash
# Format: MAJOR.MINOR.PATCH
# MAJOR: Breaking changes
# MINOR: New features (backward compatible)
# PATCH: Bug fixes (backward compatible)

VERSION=1.0.0

podman build \
    -t payu/account-service:${VERSION} \
    -t payu/account-service:1 \
    -t payu/account-service:latest \
    backend/account-service/
```

### Git-Based Tagging

```bash
# Use git commit hash as tag
GIT_TAG=$(git rev-parse --short HEAD)

podman build \
    --build-arg GIT_COMMIT=${GIT_TAG} \
    -t payu/account-service:${GIT_TAG} \
    backend/account-service/

# Use git branch name
BRANCH=$(git rev-parse --abbrev-ref HEAD | sed 's/\//-/g')

podman build \
    -t payu/account-service:${BRANCH} \
    backend/account-service/
```

### Environment-Based Tagging

```bash
ENVIRONMENT=production
BUILD_NUMBER=123

podman build \
    -t payu/account-service:${ENVIRONMENT}-${BUILD_NUMBER} \
    -t payu/account-service:${ENVIRONMENT} \
    backend/account-service/
```

### Registry Tagging

```bash
REGISTRY=payu.azurecr.io
SERVICE=account-service
VERSION=1.0.0

# Full registry path
podman tag payu/${SERVICE}:${VERSION} ${REGISTRY}/${SERVICE}:${VERSION}
podman tag payu/${SERVICE}:${VERSION} ${REGISTRY}/${SERVICE}:latest

# Push to registry
podman push ${REGISTRY}/${SERVICE}:${VERSION}
podman push ${REGISTRY}/${SERVICE}:latest
```

### Multi-Tag Build Script

```bash
#!/bin/bash
# build-with-tags.sh

SERVICE=$1
VERSION=$2
BUILD_NUMBER=${BUILD_NUMBER:-local}

# Tags to apply
TAGS=(
    "${VERSION}"
    "latest"
    "${BUILD_NUMBER}"
    "git-$(git rev-parse --short HEAD)"
)

# Build image
echo "Building ${SERVICE}:${VERSION}..."
podman build \
    -t payu/${SERVICE}:${VERSION} \
    backend/${SERVICE}/

# Apply additional tags
for TAG in "${TAGS[@]}"; do
    if [ "${TAG}" != "${VERSION}" ]; then
        podman tag payu/${SERVICE}:${VERSION} payu/${SERVICE}:${TAG}
        echo "Tagged: payu/${SERVICE}:${TAG}"
    fi
done

echo "Build complete. Applied tags: ${TAGS[@]}"
```

---

## Podman vs Docker in CI/CD

### Key Differences in CI/CD Context

| Aspect | Podman | Docker |
|--------|--------|--------|
| **Architecture** | Daemonless, direct process execution | Client-server model with daemon |
| **Rootless** | Default behavior | Requires special setup |
| **Build Tool** | Usually `buildah` | Docker BuildKit |
| **Registry Auth** | Uses `~/.config/containers/registries.conf` | Uses `~/.docker/config.json` |
| **Volume Mounts** | Requires `--security-opt label=disable` | Works with default settings |
| **Network** | Rootless pods require extra setup | Standard network behavior |
| **Cache** | Buildah-specific cache location | Docker BuildKit cache |

### Advantages for CI/CD

1. **No Docker Daemon**: More reliable in CI environments
2. **Root by Default**: Better security posture
3. **Pod Support**: Run multiple containers together
4. **Docker Compat**: Most commands work unchanged
5. **Buildah Integration**: Better build tooling

---

## CI/CD Examples

### GitHub Actions with Podman

```yaml
name: Build and Push with Podman

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up Podman
        run: |
          sudo apt-get update -y
          sudo apt-get install -y podman buildah skopeo

      - name: Log in to registry
        run: |
          podman login payu.azurecr.io \
            --username ${{ secrets.REGISTRY_USERNAME }} \
            --password ${{ secrets.REGISTRY_PASSWORD }}

      - name: Build image with Podman
        run: |
          podman build \
            --build-arg BUILD_DATE=${{ github.event.head_commit.timestamp }} \
            --build-arg GIT_COMMIT=${{ github.sha }} \
            --build-arg VERSION=1.0.0 \
            -t payu.azurecr.io/account-service:${{ github.sha }} \
            backend/account-service/

      - name: Push image
        run: |
          podman push payu.azurecr.io/account-service:${{ github.sha }}

      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: payu.azurecr.io/account-service:${{ github.sha }}
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload Trivy results to GitHub Security
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'
```

### GitHub Actions with Buildah (Alternative)

```yaml
name: Build with Buildah

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up Buildah
        uses: redhat-actions/buildah-action@v1
        with:
          image: quay.io/buildah/stable
          containerfiles: |
            Dockerfile

      - name: Build image
        id: build
        uses: redhat-actions/buildah-build@v1
        with:
          tags: payu.azurecr.io/account-service:${{ github.sha }}
          containerfiles: |
            Dockerfile
          build-args: |
            BUILD_DATE=${{ github.event.head_commit.timestamp }}
            GIT_COMMIT=${{ github.sha }}
            VERSION=1.0.0

      - name: Push image
        uses: redhat-actions/push-to-registry@v1
        with:
              image: ${{ steps.build.outputs.image }}
              tags: ${{ steps.build.outputs.tags }}
              registry: payu.azurecr.io
              username: ${{ secrets.REGISTRY_USERNAME }}
              password: ${{ secrets.REGISTRY_PASSWORD }}
```

### Tekton Pipeline with Buildah

```yaml
apiVersion: tekton.dev/v1
kind: Pipeline
metadata:
  name: payu-build-pipeline
spec:
  params:
    - name: SERVICE_NAME
      type: string
    - name: SERVICE_PATH
      type: string
    - name: IMAGE_URL
      type: string
  workspaces:
    - name: source
  tasks:
    - name: build
      taskRef:
        name: buildah-build
      params:
        - name: IMAGE
          value: $(params.IMAGE_URL)
        - name: DOCKERFILE
          value: $(params.SERVICE_PATH)/Containerfile
        - name: CONTEXT
          value: $(params.SERVICE_PATH)
        - name: BUILDAH_ARGS
          value: |
            --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')
            --build-arg GIT_COMMIT=$(git rev-parse --short HEAD)
            --build-arg VERSION=1.0.0
            --format=docker
            --loglevel=debug
      workspaces:
        - name: source
          workspace: source

    - name: scan
      taskRef:
        name: trivy-scan
      params:
        - name: IMAGE
          value: $(params.IMAGE_URL)
      runAfter:
        - build

    - name: push
      taskRef:
        name: image-pusher
      params:
        - name: IMAGE
          value: $(params.IMAGE_URL)
      runAfter:
        - scan
```

### GitLab CI with Podman

```yaml
# .gitlab-ci.yml
stages:
  - build
  - scan
  - push

variables:
  IMAGE_NAME: payu.azurecr.io/account-service
  VERSION: $CI_COMMIT_SHA

before_script:
  - apt-get update -y
  - apt-get install -y podman buildah skopeo

build-image:
  stage: build
  image: ubuntu:22.04
  script:
    - podman build \
        --build-arg BUILD_DATE=$CI_PIPELINE_CREATED_AT \
        --build-arg GIT_COMMIT=$CI_COMMIT_SHA \
        --build-arg VERSION=1.0.0 \
        -t $IMAGE_NAME:$VERSION \
        backend/account-service/

scan-image:
  stage: scan
  image: aquasec/trivy:latest
  script:
    - trivy image --severity CRITICAL,HIGH $IMAGE_NAME:$VERSION
    - trivy image --format json --output trivy-report.json $IMAGE_NAME:$VERSION
  dependencies:
    - build-image
  artifacts:
    reports:
      container_scanning: trivy-report.json
    paths:
      - trivy-report.json
    expire_in: 1 week

push-image:
  stage: push
  script:
    - podman login payu.azurecr.io \
        --username $CI_REGISTRY_USER \
        --password $CI_REGISTRY_PASSWORD
    - podman push $IMAGE_NAME:$VERSION
  dependencies:
    - scan-image
  only:
    - main
    - develop
```

### Jenkins with Podman

```groovy
pipeline {
    agent any

    environment {
        REGISTRY = 'payu.azurecr.io'
        SERVICE = 'account-service'
        VERSION = '1.0.0'
    }

    stages {
        stage('Install Podman') {
            steps {
                script {
                    sh '''
                        sudo apt-get update -y
                        sudo apt-get install -y podman buildah skopeo
                        sudo systemctl enable --now podman.socket
                    '''
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    sh """
                        podman build \
                            --build-arg BUILD_DATE=\$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
                            --build-arg GIT_COMMIT=\$(git rev-parse --short HEAD) \
                            --build-arg VERSION=${VERSION} \
                            -t ${REGISTRY}/${SERVICE}:${VERSION} \
                            backend/${SERVICE}/
                    """
                }
            }
        }

        stage('Scan') {
            steps {
                script {
                    sh 'trivy image --severity CRITICAL,HIGH ${REGISTRY}/${SERVICE}:${VERSION}'
                }
            }
        }

        stage('Push') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'registry-credentials',
                        usernameVariable: 'REGISTRY_USER',
                        passwordVariable: 'REGISTRY_PASS'
                    )]) {
                        sh """
                            podman login ${REGISTRY} -u \$REGISTRY_USER -p \$REGISTRY_PASS
                            podman push ${REGISTRY}/${SERVICE}:${VERSION}
                        """
                    }
                }
            }
        }
    }
}
```

### GitLab CI with Buildah

```yaml
# .gitlab-ci.yml
variables:
  BUILDAH_FORMAT: "docker"
  BUILDAH_NAMESPACE: "payu.azurecr.io"

build_with_buildah:
  image: quay.io/buildah/stable
  stage: build
  script:
    - buildah build-using-dockerfile \
        --build-arg BUILD_DATE=$CI_PIPELINE_CREATED_AT \
        --build-arg GIT_COMMIT=$CI_COMMIT_SHA \
        --build-arg VERSION=1.0.0 \
        --format=$BUILDAH_FORMAT \
        --loglevel=debug \
        --tag=$BUILDAH_NAMESPACE/account-service:$CI_COMMIT_SHA \
        --tls-verify=false \
        backend/account-service/
  only:
    - main
```

---

## Skopeo for Image Operations

Skopeo is a command-line utility for examining and working with container images and image repositories.

### Skopeo Basic Commands

#### Inspect Image

```bash
# Inspect local image
skopeo inspect docker-daemon:payu/account-service:1.0.0

# Inspect remote image
skopeo inspect docker://payu.azurecr.io/account-service:1.0.0 --creds username:password

# Inspect with output format
skopeo inspect docker://payu.azurecr.io/account-service:1.0.0 --format "{{ .Manifest.MediaType }}"
```

#### Copy Image Between Registries

```bash
# Copy with authentication
skopeo copy \
    docker-daemon:payu/account-service:1.0.0 \
    docker://payu.azurecr.io/account-service:1.0.0 \
    --dest-creds username:password

# Copy with TLS verification
skopeo copy \
    docker://reg1.example.com/ns/image:tag \
    docker://reg2.example.com/ns/image:tag \
    --src-creds user1:pass1 \
    --dest-creds user2:pass2 \
    --dest-tls-verify=true
```

#### Sync Repository

```bash
# Sync entire repository
skopeo sync \
    docker://source-registry.com/namespace/image \
    docker://destination-registry.com/namespace/image \
    --src-creds user:pass \
    --dest-creds user:pass \
    --remove-signatures

# Sync with tags filter
skopeo sync \
    docker://source-registry.com/namespace/image \
    dir:/tmp/local-copy \
    --src-creds user:pass \
    --dest-creds user:pass
```

#### Delete Image

```bash
# Delete from registry
skopeo delete \
    docker://payu.azurecr.io/account-service:1.0.0 \
    --creds username:password
```

#### Image Manifest Manipulation

```bash
# Get manifest digest
skopeo inspect docker://payu.azurecr.io/account-service:1.0.0 --format "{{ .Manifest.Digest }}"

# Copy with specific digest
skopeo copy \
    docker://payu.azurecr.io/account-service:1.0.0 \
    docker://payu.azurecr.io/account-service@sha256:abc123 \
    --dest-creds username:password
```

### Skopeo in CI/CD Pipelines

#### Authentication Setup

```bash
# Create auth file for Podman
mkdir -p ~/.config/containers
cat > ~/.config/containers/registries.conf <<EOF
[[registry]]
location = "payu.azurecr.io"
insecure = false
tls = true
[registry.auth]
username = "$REGISTRY_USERNAME"
password = "$REGISTRY_PASSWORD"
EOF
```

#### GitHub Action with Skopeo

```yaml
- name: Sync image with Skopeo
  run: |
    skopeo copy \
        docker-daemon:payu.azurecr.io/account-service:${{ github.sha }} \
        docker://ghcr.io/payu/account-service:${{ github.sha }} \
        --dest-creds ${{ secrets.GITHUB_TOKEN }}:
```

#### GitLab CI with Skopeo

```yaml
sync_to_github:
  stage: sync
  image: quay.io/skopeo/stable
  script:
    - skopeo copy \
        docker://payu.azurecr.io/account-service:$CI_COMMIT_SHA \
        docker://ghcr.io/payu/account-service:$CI_COMMIT_SHA \
        --dest-creds $CI_REGISTRY_USER:$CI_REGISTRY_PASSWORD
  only:
    - main
```

---

### Tekton Pipeline with Buildah (already uses Buildah!)

The Tekton example above shows how to use Buildah, which is the recommended build tool for Podman in CI/CD pipelines. Key features:

- No Docker daemon required
- Rootless build support
- Better integration with OpenShift/Kubernetes
- Direct OCI image creation
- Build cache management
- Multi-architecture support

---

## CI/CD with Buildah (Advanced)

### Buildah Caching Strategy

```yaml
- name: build-with-cache
  taskRef:
    name: buildah
  params:
    - name: image
      value: payu/account-service:latest
    - name: buildContext
      value: .
    - name: containerfiles
      value: |
        Containerfile
    - name: buildArgs
      value: |
        BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')
        GIT_COMMIT=$(git rev-parse --short HEAD)
        VERSION=1.0.0
  workspaces:
    - name: cache
      workspace: shared
```

### Podman Compose in CI/CD

```bash
# podman-compose.yml alternative for Podman
version: '3.8'

services:
  app:
    image: payu/account-service:latest
    build:
      context: .
      dockerfile: Containerfile
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=postgresql://postgres:password@db:5432/payu
    depends_on:
      - db

  db:
    image: postgres:14
    environment:
      - POSTGRES_DB=payu
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=password
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  postgres-data:

# Build and run with podman-compose
podman-compose up --build -d
```

---

## Troubleshooting

### Build Failures

#### Issue: Maven out of memory

```bash
# Increase Maven memory
podman build \
    --build-arg MAVEN_OPTS="-Xmx2048m" \
    -t payu/account-service:latest \
    backend/account-service/
```

#### Issue: npm install fails

```bash
# Clear npm cache
podman buildx prune

# Rebuild with cache disabled
podman build \
    --no-cache \
    -t payu/web-app:latest \
    frontend/web-app/
```

#### Issue: pip install fails

```bash
# Use trusted hosts
podman build \
    --build-arg PIP_TRUSTED_HOST=pypi.org \
    --build-arg PIP_INDEX_URL=https://pypi.org/simple \
    -t payu/kyc-service:latest \
    backend/kyc-service/
```

### Cache Issues

#### Clear BuildKit Cache

```bash
# Clear all cache
podman builder prune -a

# Clear cache older than 24 hours
podman builder prune --filter until=24h

# Clear specific image cache
podman builder prune --filter "image=payu/account-service"
```

### Scan Failures

#### Issue: Trivy timeout

```bash
# Increase timeout
trivy image --timeout 10m payu/account-service:1.0.0

# Skip DB update (for offline builds)
trivy image --skip-db-update payu/account-service:1.0.0
```

#### Issue: False positives

```bash
# Create .trivyignore file
cat > .trivyignore << EOF
CVE-2020-28476  # False positive - not used in our codebase
EOF

# Scan with ignore file
trivy image --skip-db-update payu/account-service:1.0.0
```

### Performance Optimization

#### Parallel Builds

```bash
# Build multiple services in parallel
for service in account auth transaction; do
    podman build \
        -t payu/${service}-service:latest \
        backend/${service}-service/ &
done
wait
```

#### Layer Caching

```podmanfile
# Good: Copy package files first
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .

# Bad: Copy everything at once
COPY . .
RUN npm ci
```

---

## Quick Reference

### Java Services

```bash
# Spring Boot
podman build -t payu/account-service:latest backend/account-service/

# Quarkus (fast-jar)
podman build -t payu/gateway-service:latest backend/gateway-service/

# Quarkus (native)
podman build -f backend/gateway-service/Dockerfile.native -t payu/gateway-service:native backend/gateway-service/
```

### Python Services

```bash
podman build -t payu/kyc-service:latest backend/kyc-service/
podman build -t payu/analytics-service:latest backend/analytics-service/
```

### Next.js Services

```bash
podman build -t payu/web-app:latest frontend/web-app/
podman build -t payu/developer-docs:latest frontend/developer-docs/
```

### Security Scan

```bash
# Trivy
trivy image --severity CRITICAL,HIGH payu/account-service:latest

# Grype
grype payu/account-service:latest --only-fixed

# SBOM
syft payu/account-service:latest -o spdx-json > sbom.json
```

### Container Signing

```bash
# Sign
cosign sign --key cosign.key payu/account-service:1.0.0

# Verify
cosign verify --key cosign.pub payu/account-service:1.0.0
```

### Buildah Build Commands

```bash
# Build with Buildah
buildah build-using-dockerfile \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    --build-arg VERSION=1.0.0 \
    --format=docker \
    -t payu/account-service:1.0.0 \
    backend/account-service/

# Inspect built image
buildah inspect payu/account-service:1.0.0

# Push to registry
buildah push payu/account-service:1.0.0 docker://payu.azurecr.io/account-service:1.0.0
```

### Skopeo Image Operations

```bash
# Copy images
skopeo copy docker-daemon:payu/account-service:1.0.0 docker://payu.azurecr.io/account-service:1.0.0

# Inspect images
skopeo inspect docker://payu.azurecr.io/account-service:1.0.0

# Sync repositories
skopeo sync docker://source-registry.com/namespace/image docker://destination-registry.com/namespace/image
```

---

## Podman Build Advantages

### Rootless Builds
Podman builds can run as non-root user:

```bash
# Build as current user (recommended for security)
podman build -t payu/service:latest .

# Build with specific user
podman build --user $(id -u):$(id -g) -t payu/service:latest .
```

### Pod Build Context
Use pods for building related services together:

```bash
# Create build pod
podman pod create --name payu-build-pod

# Build in pod context
podman build --pod payu-build-pod \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    -t payu/account-service:latest \
    backend/account-service/

# Clean up build pod
podman pod stop payu-build-pod
podman pod rm payu-build-pod
```

### Build with Security Profiles
```bash
# Build with SELinux context
podman build --security-opt label=type:svirt_sandbox_file_t \
    -t payu/service:latest .

# Build with AppArmor profile
podman build --security-opt apparmor=unconfined \
    -t payu/service:latest .
```

### Containerfile Best Practices
```bash
# Build with Containerfile
podman build -f Containerfile -t payu/service:latest .

# Multi-platform builds with Podman
podman buildx build --platform linux/amd64,linux/arm64 \
    -t payu/service:latest \
    --push \
    .
```

---

**Document Version:** 2.0.0
**Last Updated:** 2026-01-30
**Maintained By:** Platform Engineering Team
**Next Review:** 2026-04-30
