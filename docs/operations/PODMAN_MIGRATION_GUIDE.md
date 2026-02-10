# PayU Docker to Podman Migration Guide

## Overview

This guide documents the migration from Docker to Podman for the PayU Digital Banking Platform. Podman provides a daemonless, rootless container runtime that is more secure and aligned with Red Hat OpenShift standards.

## Migration Status

| Component | Status | Notes |
|-----------|--------|-------|
| Container Images | ✅ Complete | All services use UBI9-based Containerfiles |
| Build Scripts | ✅ Complete | Updated to support Containerfile/Dockerfile |
| Compose Files | ✅ Complete | podman-compose.yml created |
| Quadlet Files | ✅ Complete | Systemd integration files created |
| Documentation | ✅ Complete | Migration guide and READMEs updated |
| CI/CD Pipeline | ✅ Complete | Tekton pipelines configured (5 tasks: maven, buildah, deploy, trivy, pytest) |

## Quick Start

### 1. Install Podman

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y podman buildah skopeo

# RHEL/CentOS/Fedora
sudo dnf install -y podman buildah skopeo

# macOS
brew install podman
podman machine init
podman machine start
```

### 2. Install podman-compose

```bash
# Using pip
pip3 install podman-compose

# Or using the wrapper script
sudo ln -s /usr/bin/podman /usr/bin/docker-compose
```

### 3. Configure Environment

```bash
# Copy environment file
cp .env.example .env

# Source Podman aliases
source scripts/podman-aliases.sh

# Optional: Add to your shell profile
echo 'source /home/ubuntu/payu/scripts/podman-aliases.sh' >> ~/.bashrc
```

### 4. Start Services

```bash
# Using podman-compose
dcp up -d

# Or using individual services
podman-compose up -d postgres redis kafka
```

## Command Mapping

| Docker Command | Podman Command | Alias |
|----------------|----------------|-------|
| `docker` | `podman` | - |
| `docker-compose` | `podman-compose` | `dcp` |
| `docker build` | `podman build` | `db` |
| `docker push` | `podman push` | `dp` |
| `docker ps` | `podman ps` | `dps` |
| `docker ps -a` | `podman ps -a` | `da` |
| `docker exec` | `podman exec` | `dexec` |
| `docker logs` | `podman logs` | `dlogs` |
| `docker run` | `podman run` | `drun` |
| `docker pull` | `podman pull` | `dpull` |
| `docker rm` | `podman rm` | `drm` |
| `docker rmi` | `podman rmi` | `drmi` |
| `docker network` | `podman network` | `dnetwork` |
| `docker volume` | `podman volume` | `dvolume` |
| `docker inspect` | `podman inspect` | `dinspect` |
| `docker stats` | `podman stats` | `dstat` |
| `docker system prune` | `podman system prune` | `dprune` |

## Key Differences

### 1. Daemonless Architecture

**Docker:** Requires a daemon running as root
```bash
sudo systemctl status docker
```

**Podman:** Daemonless, runs containers directly
```bash
# No daemon to check - containers run as child processes
podman ps  # Lists containers directly
```

### 2. Rootless by Default

**Docker:** Runs as root by default
```bash
docker run -v /host/path:/container/path nginx
# May have permission issues
```

**Podman:** Runs rootless by default
```bash
podman run -v /home/user/path:/container/path:Z nginx
# Uses user namespace mapping
# :Z flag for SELinux labeling
```

### 3. Pod Concept

Podman introduces the concept of pods (similar to Kubernetes pods):

```bash
# Create a pod
podman pod create --name payu-pod --publish 8080:8080

# Add containers to pod
podman run -d --pod payu-pod --name gateway-service payu/gateway-service
podman run -d --pod payu-pod --name account-service payu/account-service

# Manage pod
podman pod stop payu-pod
podman pod start payu-pod
```

### 4. Systemd Integration

Podman works natively with systemd through quadlet:

```bash
# Generate systemd unit file
podman generate systemd --new --name payu-gateway-service > ~/.config/systemd/user/payu-gateway.service

# Or use quadlet files (recommended)
cp infrastructure/quadlet/*.container ~/.config/containers/systemd/
systemctl --user daemon-reload
systemctl --user start payu-gateway-service
```

## Building Images

### Build Single Service

```bash
# Using the build script
./scripts/build-service-podman.sh account-service

# With custom tag
./scripts/build-service-podman.sh account-service -t v1.2.3

# Build and push
./scripts/build-service-podman.sh account-service -p -r registry.payu.internal
```

### Build All Services

```bash
# Build all services
./scripts/build-all-podman.sh

# With parallel jobs
./scripts/build-all-podman.sh -j 8

# With custom registry
./scripts/build-all-podman.sh -r registry.payu.internal -t v1.0.0
```

### Manual Build

```bash
# Using podman build
cd backend/account-service
podman build -t payu/account-service:latest -f Containerfile .

# Using buildah (more advanced)
buildah build-using-dockerfile -t payu/account-service:latest -f Containerfile .
```

## Security Considerations

### Rootless Mode

```bash
# Run container as non-root user
podman run --user 1001:1001 payu/account-service

# Check user namespace
podman unshare cat /proc/self/uid_map
```

### SELinux

```bash
# Label volumes correctly (:Z for private, :z for shared)
podman run -v /data:/data:Z payu/postgres

# Check SELinux context
ls -Z /data
```

### Capabilities

```bash
# Drop all capabilities
podman run --cap-drop=ALL payu/account-service

# Add specific capabilities
podman run --cap-drop=ALL --cap-add=NET_BIND_SERVICE payu/gateway-service
```

### Seccomp

```bash
# Use default seccomp profile
podman run --security-opt seccomp=default payu/account-service

# Use custom profile
podman run --security-opt seccomp=/path/to/profile.json payu/account-service
```

## Networking

### Create Network

```bash
# Create custom network
podman network create payu-network

# Inspect network
podman network inspect payu-network
```

### DNS Resolution

Podman provides automatic DNS resolution within networks:

```bash
# Containers can resolve each other by name
podman run --name postgres --network payu-network postgres:16
podman run --name account-service --network payu-network payu/account-service
# account-service can connect to postgres:5432
```

### Port Mapping

```bash
# Map ports (same as Docker)
podman run -p 8080:8080 payu/gateway-service

# Map to specific interface
podman run -p 127.0.0.1:8080:8080 payu/gateway-service
```

## Storage

### Volume Management

```bash
# Create volume
podman volume create postgres_data

# Use volume
podman run -v postgres_data:/var/lib/postgresql/data postgres:16

# List volumes
podman volume ls

# Remove volume
podman volume rm postgres_data
```

### Storage Locations

```bash
# Rootless storage location
~/.local/share/containers/storage/

# Root storage location
/var/lib/containers/storage/
```

## Troubleshooting

### Permission Denied

**Issue:** Permission denied when accessing mounted volumes

**Solution:**
```bash
# Check SELinux context
ls -Z /path/to/volume

# Fix with restorecon
restorecon -Rv /path/to/volume

# Or use :Z flag
podman run -v /path:/path:Z image
```

### Port Already in Use

**Issue:** Port already in use

**Solution:**
```bash
# Find process using port
sudo ss -tlnp | grep 8080

# Or use different port
podman run -p 8081:8080 payu/gateway-service
```

### Container Won't Start

**Issue:** Container fails to start

**Solution:**
```bash
# Check logs
podman logs container-name

# Run interactively for debugging
podman run -it --rm payu/account-service bash

# Check events
podman events --filter container=container-name
```

### Network Issues

**Issue:** Containers can't communicate

**Solution:**
```bash
# Check network
podman network ls
podman network inspect payu-network

# Test connectivity
podman exec container-name ping other-container
```

### Image Pull Failures

**Issue:** Can't pull images

**Solution:**
```bash
# Check registry configuration
cat /etc/containers/registries.conf

# Add insecure registry (for development)
echo "[[registry]]
location = "localhost:5000"
insecure = true" | sudo tee -a /etc/containers/registries.conf

# Pull with verbose output
podman pull -v image:tag
```

## Performance Optimization

### Build Cache

```bash
# Use layer caching
podman build --layers -t image:tag .

# Clear build cache
podman builder prune
```

### Storage Driver

```bash
# Check storage driver
podman info | grep "graphDriverName"

# Recommended: overlay
# For rootless, ensure fuse-overlayfs is installed
```

### Resource Limits

```bash
# Set memory limit
podman run --memory=1g --memory-swap=2g payu/account-service

# Set CPU limit
podman run --cpus=2 payu/account-service

# Set OOM score
podman run --oom-score-adj=100 payu/account-service
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Build with Podman

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Install Podman
        run: |
          sudo apt update
          sudo apt install -y podman buildah

      - name: Build image
        run: |
          podman build -t payu/account-service:latest -f Containerfile backend/account-service

      - name: Test container
        run: |
          podman run --rm payu/account-service:latest java -version
```

### Tekton Pipeline

See `infrastructure/tekton/` for Tekton pipeline definitions using Podman/Buildah.

## Best Practices

1. **Use Containerfile naming** - Prefer `Containerfile` over `Dockerfile` for Podman projects
2. **Run rootless** - Use rootless Podman for better security
3. **Use quadlet** - Manage containers with systemd for production
4. **Label volumes** - Always use `:Z` or `:z` for SELinux compatibility
5. **Set resource limits** - Define memory and CPU limits for all containers
6. **Use health checks** - Define health checks in Containerfiles
7. **Scan images** - Use Trivy or similar tools to scan for vulnerabilities
8. **Version tags** - Use semantic versioning for image tags

## Migration Checklist

- [ ] Install Podman and dependencies
- [ ] Update build scripts to support Containerfile
- [ ] Create podman-compose.yml
- [ ] Test all services with Podman
- [ ] Update CI/CD pipelines
- [ ] Create quadlet files for production
- [ ] Document new commands and workflows
- [ ] Train team on Podman usage
- [ ] Decommission Docker (after validation)

## References

- [Podman Documentation](https://docs.podman.io/)
- [Podman Compose](https://github.com/containers/podman-compose)
- [Buildah Documentation](https://buildah.io/)
- [Red Hat Container Tools](https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/9/html/building_running_and_managing_containers/)
- [Podman Systemd Integration](https://docs.podman.io/en/latest/markdown/podman-systemd.unit.5.html)

## Support

For issues or questions:
- Check the troubleshooting section above
- Review the [Podman Troubleshooting Guide](https://github.com/containers/podman/blob/main/troubleshooting.md)
- Contact the PayU DevOps team
