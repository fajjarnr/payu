# BuildKit Optimization Guide

## Overview

BuildKit is the next-generation Podman build engine with advanced caching, parallelization, and dependency management capabilities. This guide documents BuildKit best practices for the PayU platform across Java (Spring Boot), Python (FastAPI), and JavaScript/TypeScript (Next.js) services.

## Important Note: Podman Compatibility

This guide has been updated to use Podman instead of Docker. Podman provides similar functionality to Docker but with additional security features like rootless containers and pod management. Key changes:
- `podman` commands replaced with `podman`
- Dockerfile naming is still supported but consider using Containerfile for future-proofing
- Podman supports all BuildKit features natively

## Table of Contents

1. [Enabling BuildKit](#enabling-buildkit)
2. [Cache Mount Patterns](#cache-mount-patterns)
3. [Multi-Stage Build Best Practices](#multi-stage-build-best-practices)
4. [Build Command Examples](#build-command-examples)
5. [Service-Specific Patterns](#service-specific-patterns)

---

## Enabling BuildKit

### Method 1: Environment Variable (Recommended)

```bash
export DOCKER_BUILDKIT=1
podman build -t payu/account-service:latest .
```

### Method 2: Docker Daemon Configuration

Add to `/etc/podman/daemon.json`:
```json
{
  "features": {
    "buildkit": true
  }
}
```

### Method 3: Buildx (Newer syntax)

```bash
podman buildx build --build-arg BUILDKIT_INLINE_CACHE=1 -t payu/account-service:latest .
```

---

## Cache Mount Patterns

Cache mounts allow BuildKit to cache dependencies between builds without adding layers to the image. This significantly reduces build times for dependency-heavy operations.

### Maven (Java/Spring Boot)

**Standard cache mount for Maven local repository:**

```podmanfile
# --mount=type=cache,target=/root/.m2
RUN mvn dependency:go-offline -B

# Combined with source mount for incremental builds
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=bind,from=rust,source=/usr/local/cargo/bin,target=/usr/local/bin \
    mvn package -DskipTests -B
```

**Cache key customization:**

```podmanfile
RUN --mount=type=cache,target=/root/.m2,id=maven-$(md5sum pom.xml | cut -d' ' -f1) \
    mvn dependency:go-offline -B
```

### npm (Node.js/Next.js)

**Standard cache mount for npm:**

```podmanfile
# Cache ~/.npm for faster dependency installation
RUN --mount=type=cache,target=/root/.npm \
    npm ci --ignore-scripts

# Alternative: Mount specific cache directories
RUN --mount=type=cache,target=/app/node_modules/.cache \
    npm run build
```

**For Next.js builds:**

```podmanfile
# Next.js build cache
RUN --mount=type=cache,target=/app/.next/cache \
    npm run build
```

### pip (Python/FastAPI)

**Standard cache mount for pip:**

```podmanfile
# Cache pip downloads
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install --no-cache-dir -r requirements.txt

# For wheel cache (faster builds)
RUN --mount=type=cache,target=/root/.cache/pip,id=pip-$(md5sum requirements.txt | cut -d' ' -f1) \
    pip install --no-cache-dir -r requirements.txt
```

**With editable installs:**

```podmanfile
RUN --mount=type=cache,target=/root/.cache/pip \
    --mount=type=bind,target=/src \
    pip install -e /src
```

---

## Multi-Stage Build Best Practices

### Principle: Separate Build and Runtime Dependencies

Multi-stage builds allow you to separate build-time tools from runtime dependencies, resulting in smaller, more secure images.

#### Pattern 1: Maven Spring Boot (Java 21)

```podmanfile
####
# Build stage - Using Red Hat UBI9 OpenJDK 21 with Maven
####
FROM registry.access.redhat.com/ubi9/openjdk-21:1.20 AS build

USER root
WORKDIR /build

# Install Maven
RUN microdnf install -y maven && microdnf clean all

# Copy pom.xml first for dependency caching
COPY pom.xml ./

# Download dependencies with BuildKit cache mount
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B && \
    mv target/*.jar target/app.jar

####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20

USER 185
WORKDIR /deployments

# Copy the built artifact from build stage
COPY --from=build --chown=185 /build/target/app.jar /deployments/app.jar

EXPOSE 8001

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC"

CMD ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]
```

#### Pattern 2: Next.js (Node.js 20)

```podmanfile
####
# Base Image - Red Hat UBI9 Node.js 20
####
FROM registry.access.redhat.com/ubi9/nodejs-20:1-20 AS base

####
# Stage 1: Dependencies
####
FROM base AS deps
USER root
WORKDIR /app

# Install build tools for native modules
RUN microdnf install -y python3 make gcc-c++ && microdnf clean all

# Copy package files
COPY package.json package-lock.json* ./

# Install dependencies with BuildKit cache mount
RUN --mount=type=cache,target=/root/.npm \
    npm ci --ignore-scripts

####
# Stage 2: Builder
####
FROM base AS builder
WORKDIR /app

# Copy dependencies from deps stage
COPY --from=deps /app/node_modules ./node_modules

# Copy source code
COPY . .

# Disable Next.js telemetry
ENV NEXT_TELEMETRY_DISABLED=1

# Build Next.js with BuildKit cache mount
RUN --mount=type=cache,target=/app/.next/cache \
    npm run build

####
# Stage 3: Runner (Production)
####
FROM registry.access.redhat.com/ubi9/nodejs-20-minimal:1-20 AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

USER root
RUN microdnf install -y curl && microdnf clean all

# Copy public assets
COPY --from=builder /app/public ./public

# Create .next directory with proper permissions
RUN mkdir -p .next && chown -R 1001:0 .next

# Copy standalone output
COPY --from=builder --chown=1001:0 /app/.next/standalone ./
COPY --from=builder --chown=1001:0 /app/.next/static ./.next/static

USER 1001

EXPOSE 3000

CMD ["node", "server.js"]
```

#### Pattern 3: FastAPI (Python 3.11)

```podmanfile
####
# Build stage - Using UBI9 Python 3.11
####
FROM registry.access.redhat.com/ubi9/python-311:latest AS build

USER root
WORKDIR /app

# Install system dependencies
RUN dnf install -y gcc g++ glibc-devel zlib-devel && \
    dnf clean all

# Copy requirements and install dependencies with cache mount
COPY requirements.txt ./
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install --no-cache-dir -r requirements.txt

####
# Runtime stage
####
FROM registry.access.redhat.com/ubi9/python-311:latest AS runtime

USER root
WORKDIR /app

# Install only runtime dependencies
RUN dnf install -y libjpeg libpng && dnf clean all

# Copy virtualenv from build stage
COPY --from=build /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=build /usr/local/bin /usr/local/bin

# Copy application code
COPY src/ /app/src/

# Create directories with proper permissions
RUN mkdir -p /app/models /app/temp /app/uploads && \
    chown -R 185:0 /app

USER 185

EXPOSE 8007

ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8007"]
```

---

## Build Command Examples

### Basic BuildKit Build

```bash
# Enable BuildKit and build
DOCKER_BUILDKIT=1 podman build -t payu/account-service:latest backend/account-service/

# With buildx
podman buildx build --build-arg BUILDKIT_INLINE_CACHE=1 \
    -t payu/account-service:latest \
    backend/account-service/
```

### Parallel Multi-Service Build

```bash
# Build multiple services in parallel
DOCKER_BUILDKIT=1 podman build -t payu/account-service:latest backend/account-service/ & \
DOCKER_BUILDKIT=1 podman build -t payu/auth-service:latest backend/auth-service/ & \
DOCKER_BUILDKIT=1 podman build -t payu/transaction-service:latest backend/transaction-service/ & \
wait
```

### With Build Arguments

```bash
DOCKER_BUILDKIT=1 podman build \
    --build-arg GIT_COMMIT=$(git rev-parse --short HEAD) \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg VERSION=1.0.0 \
    -t payu/account-service:1.0.0 \
    backend/account-service/
```

### With Cache Export (for CI/CD)

```bash
# Export cache to registry
DOCKER_BUILDKIT=1 podman build \
    --cache-from=type=registry,ref=payu.azurecr.io/account-service:cache \
    --cache-to=type=registry,ref=payu.azurecr.io/account-service:cache,mode=max \
    -t payu/account-service:latest \
    backend/account-service/

# Local cache export
DOCKER_BUILDKIT=1 podman build \
    --cache-from=type=local,src=/tmp/buildkit-cache \
    --cache-to=type=local,dest=/tmp/buildkit-cache \
    -t payu/account-service:latest \
    backend/account-service/
```

### With Secrets Mount (for private dependencies)

```bash
# Mount SSH keys for private git dependencies
DOCKER_BUILDKIT=1 podman build \
    --secret=id=ssh,src=~/.ssh/id_rsa \
    -t payu/account-service:latest \
    backend/account-service/

# Mount npm token for private packages
DOCKER_BUILDKIT=1 podman build \
    --secret=id=npm,src=~/.npmrc \
    -t payu/web-app:latest \
    frontend/web-app/
```

---

## Service-Specific Patterns

### Java Spring Boot Services

**Directory Structure:**
```
backend/account-service/
├── src/
├── pom.xml
├── Dockerfile
└── .podmanignore
```

**Optimized Dockerfile:**
```podmanfile
FROM registry.access.redhat.com/ubi9/openjdk-21:1.20 AS build

USER root
WORKDIR /build

RUN microdnf install -y maven && microdnf clean all

COPY pom.xml ./

RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B && \
    mv target/*.jar target/app.jar

FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20

USER 185
WORKDIR /deployments

COPY --from=build --chown=185 /build/target/app.jar /deployments/app.jar

EXPOSE 8001

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

CMD ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]
```

**Build Command:**
```bash
DOCKER_BUILDKIT=1 podman build -t payu/account-service:latest backend/account-service/
```

---

### Python FastAPI Services (KYC)

**Directory Structure:**
```
backend/kyc-service/
├── src/
├── requirements.txt
├── Dockerfile
└── .podmanignore
```

**Optimized Dockerfile:**
```podmanfile
FROM registry.access.redhat.com/ubi9/python-311:latest AS build

USER root
WORKDIR /app

RUN dnf install -y gcc g++ glibc-devel zlib-devel libjpeg-devel libpng-devel && \
    dnf clean all

COPY requirements.txt ./

RUN --mount=type=cache,target=/root/.cache/pip,id=pip-$(md5sum requirements.txt | cut -d' ' -f1) \
    pip install --no-cache-dir -r requirements.txt

FROM registry.access.redhat.com/ubi9/python-311:latest

USER root
WORKDIR /app

RUN dnf install -y libjpeg libpng && dnf clean all

COPY --from=build /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=build /usr/local/bin /usr/local/bin

COPY src/ /app/src/

RUN mkdir -p /app/models /app/temp /app/uploads && \
    chown -R 185:0 /app

USER 185

EXPOSE 8007

ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8007"]
```

**Build Command:**
```bash
DOCKER_BUILDKIT=1 podman build -t payu/kyc-service:latest backend/kyc-service/
```

---

### Next.js Web App

**Directory Structure:**
```
frontend/web-app/
├── src/
├── public/
├── package.json
├── next.config.js
├── Dockerfile
└── .podmanignore
```

**Optimized Dockerfile:**
```podmanfile
FROM registry.access.redhat.com/ubi9/nodejs-20:1-20 AS base

FROM base AS deps
USER root
WORKDIR /app

RUN microdnf install -y python3 make gcc-c++ && microdnf clean all

COPY package.json package-lock.json* ./

RUN --mount=type=cache,target=/root/.npm \
    npm ci --ignore-scripts

FROM base AS builder
WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

ENV NEXT_TELEMETRY_DISABLED=1

RUN --mount=type=cache,target=/app/.next/cache \
    npm run build

FROM registry.access.redhat.com/ubi9/nodejs-20-minimal:1-20 AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

USER root
RUN microdnf install -y curl && microdnf clean all

COPY --from=builder /app/public ./public

RUN mkdir -p .next && chown -R 1001:0 .next

COPY --from=builder --chown=1001:0 /app/.next/standalone ./
COPY --from=builder --chown=1001:0 /app/.next/static ./.next/static

USER 1001

EXPOSE 3000

CMD ["node", "server.js"]
```

**Build Command:**
```bash
DOCKER_BUILDKIT=1 podman build -t payu/web-app:latest frontend/web-app/
```

---

## Performance Tips

### 1. Layer Caching

Order Dockerfile instructions from least to most frequently changed:
1. Base image
2. System dependencies
3. Dependency installation (with cache mounts)
4. Application code
5. Entrypoint

### 2. .podmanignore Optimization

Ensure `.podmanignore` files exclude unnecessary files to reduce build context:
- `node_modules/`
- `__pycache__/`
- `target/`
- `.git/`
- `*.log`
- Test files (in production builds)

### 3. Parallel Builds

Use BuildKit's parallel execution for multi-stage builds:
```podmanfile
FROM base AS deps1
# ... stage 1 dependencies

FROM base AS deps2
# ... stage 2 dependencies

FROM base AS builder
COPY --from=deps1 ...
COPY --from=deps2 ...
```

### 4. Build Cache Persistence

For CI/CD pipelines, persist cache between builds:
```bash
# GitHub Actions example
- name: Build with BuildKit
  run: |
    DOCKER_BUILDKIT=1 podman build \
      --cache-from=type=gha \
      --cache-to=type=gha,mode=max \
      -t payu/service:latest .
```

### 5. Inline Cache for Push

Enable inline caching when pushing to registries:
```bash
podman buildx build \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    --cache-from=payu.azurecr.io/service:latest \
    -t payu.azurecr.io/service:latest \
    --push \
    .
```

---

## Monitoring & Debugging

### View BuildKit Build Progress

```bash
# Enable progress output
DOCKER_BUILDKIT=1 podman build --progress=plain -t payu/service:latest .

# With buildx
podman buildx build --progress=plain -t payu/service:latest .
```

### Inspect Build Cache

```bash
# List build cache
podman buildx du

# Prune build cache
podman buildx prune

# Prune older than 24 hours
podman buildx prune --filter until=24h
```

### Debug Cache Misses

```bash
# Enable verbose mode
BUILDKIT_DEBUG=1 DOCKER_BUILDKIT=1 podman build -t payu/service:latest .
```

## Podman-Specific Features

### Rootless Containers

Podman runs containers as rootless by default, providing enhanced security:

```bash
# Run container as non-root user (default in Podman)
podman run -d payu/account-service:latest

# Specify user if needed
podman run -u 185 payu/account-service:latest

# Run with pod isolation
podman pod create --name payu-pod
podman run --pod payu-pod -d payu/account-service:latest
```

### Pod Management

Pods allow running multiple containers together with shared resources:

```bash
# Create a pod for related services
podman pod create --name payu-backend --share net --share cgroup

# Run services in the pod
podman run --pod payu-backend -d payu/account-service:latest
podman run --pod payu-backend -d payu/auth-service:latest
podman run --pod payu-backend -d payu/transaction-service:latest

# List pods
podman pod ls

# Stop all containers in pod
podman pod stop payu-backend
```

### Containerfile vs Dockerfile

While Dockerfile is still supported, consider using Containerfile for future-proofing:

```bash
# Build with Containerfile
podman build -f Containerfile -t payu/service:latest .

# Podman automatically detects Dockerfile or Containerfile
podman build -t payu/service:latest .
```

---

## References

- [Docker BuildKit Documentation](https://docs.podman.com/build/buildkit/)
- [BuildKit Cache Mounts](https://github.com/moby/buildkit#cache-mounts)
- [Multi-Stage Builds](https://docs.podman.com/build/building/multi-stage/)
- [Red Hat UBI Images](https://catalog.redhat.com/software/containers/search)

---

**Document Version:** 1.0.1
**Last Updated:** 2026-01-30
**Maintained By:** Platform Engineering Team
