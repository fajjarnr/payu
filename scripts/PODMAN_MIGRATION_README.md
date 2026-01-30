# PayU Podman Migration Developer Tools

This directory contains productivity tools to help developers migrate from Docker to Podman and work efficiently with the PayU Digital Banking Platform.

## Files Overview

### 1. `/home/ubuntu/payu/scripts/podman-aliases.sh`
Comprehensive shell aliases and functions for Podman operations.

### 2. `/home/ubuntu/payu/.env.example`
Updated environment variables template with Podman-specific settings.

### 3. `/home/ubuntu/payu/.containers.conf.toml.example`
Podman configuration template optimized for PayU platform.

### 4. `/home/ubuntu/payu/scripts/PODMAN_MIGRATION_README.md` (this file)

---

## Quick Start

### 1. Load Podman Aliases

```bash
# Source the aliases in your current shell session
source /home/ubuntu/payu/scripts/podman-aliases.sh

# Or add to your ~/.bashrc or ~/.zshrc for permanent loading
echo 'source /home/ubuntu/payu/scripts/podman-aliases.sh' >> ~/.bashrc
```

### 2. Configure Environment

```bash
# Copy the updated environment file
cp /home/ubuntu/payu/.env.example .env

# Edit the environment file with your values
nano .env
```

### 3. Set up Podman Configuration

```bash
# For system-wide configuration (requires sudo)
sudo cp /home/ubuntu/payu/.containers.conf.toml.example /etc/containers/containers.conf

# Or for user-specific configuration
mkdir -p ~/.config/containers
cp /home/ubuntu/payu/.containers.conf.toml.example ~/.config/containers/containers.conf
```

---

## Podman Aliases Reference

### Basic Commands
- `dcp` → `podman-compose`
- `db` → `podman build`
- `dp` → `podman push`
- `dps` → `podman ps`
- `da` → `podman ps -a`
- `dexec` → `podman exec`

### Docker Compose Compatibility
- `up` → `dcp up -d`
- `down` → `dcp down`
- `logs` → `dcp logs -f`
- `stop` → `dcp stop`

### Development Commands
- `ddev` → `dcp up -d --build`
- `ddev-log` → `dcp logs -f --tail 100`
- `ddev-restart` → `dcp restart`
- `ddev-stop` → `dcp stop`

### Testing Commands
- `dtest` → `dcp run --rm test`
- `dtest-unit` → `dtest pytest tests/unit`
- `dtest-integration` → `dtest pytest tests/integration`
- `dtest-e2e` → `dtest pytest tests/e2e`

### Special Functions
- `dshow-aliases` - Display all available aliases
- `podman-status` - Check podman setup status
- `dtop-cpu` - Show CPU usage by container
- `dtop-net` - Show network I/O by container
- `build-for-payu <image> [tag]` - Build with PayU registry prefix
- `push-to-payu <image> [tag]` - Push to PayU registry
- `pull-from-payu <image> [tag]` - Pull from PayU registry

---

## Environment Variables

### Podman Specific Variables
- `PODMAN_REGISTRY` - Internal registry URL (default: registry.payu.internal)
- `PODMAN_BUILDKIT` - Enable BuildKit (1/0)
- `BUILDAH_LAYERS` - Enable layer optimization (1/0)
- `CONTAINERS_CONF` - Containers config path

### Build Configuration
- `BUILDAH_FORMAT` - Image format (docker/native)
- `BUILDAH_ISOLATION` - Security isolation mode
- `PODMAN_DRIVER` - Storage driver (overlay/vfs)

### Network Configuration
- `PODMAN_NETWORK_PREFIX` - Network prefix
- `PODMAN_CNI_PLUGIN` - CNI plugin
- `PODMAN_CNI_SUBNET` - Network subnet

### Security Settings
- `PODMAN_SECURITY_LEVEL` - Security level (standard/restricted)
- `PODMAN_SIGNATURE_POLICY` - Image signing policy
- `PODMAN_TLS_VERIFY` - TLS verification

---

## Podman Configuration Settings

### Registry Mirrors
The configuration includes registry mirrors for Red Hat products:
- registry.access.redhat.com → registry.payu.internal/redhat
- registry.redhat.io → registry.payu.internal/redhat

### Network Configuration
- Default network: payu-net (10.88.0.0/16)
- External network: payu-external (10.89.0.0/16)
- DNS servers: 8.8.8.8, 8.8.4.4, 1.1.1.1

### Security Features
- Seccomp profiles enabled
- AppArmor profiles active
- SELinux integration
- Secure default capabilities

### Storage Optimization
- Overlay storage driver
- Size limits (10G for overlay, 100G for volumes)
- Compressed layers enabled

---

## Migration from Docker to Podman

### 1. Basic Commands Mapping
| Docker | Podman | Alias |
|--------|--------|-------|
| `docker-compose` | `podman-compose` | `dcp` |
| `docker build` | `podman build` | `db` |
| `docker push` | `podman push` | `dp` |
| `docker ps` | `podman ps` | `dps` |
| `docker exec` | `podman exec` | `dexec` |
| `docker logs` | `podman logs` | `dlogs` |

### 2. Docker Compose Migration
```bash
# Convert existing docker-compose.yml to podman-compose.yml
convert-docker-compose

# Use converted files with dcp commands
dcp up -d
dcp logs -f
```

### 3. Image Migration
```bash
# Export Docker images
docker save my-image:latest > my-image.tar
podman import my-image.tar my-image:latest

# Build with Podman
build-for-payu my-service v1.0
push-to-payu my-service v1.0
```

---

## PayU Platform Integration

### 1. Registry Integration
```bash
# Login to PayU registry
dlogin -u payu -p $PODMAN_REGISTRY_PASSWORD registry.payu.internal

# Build and push images
build-for-payu account-service latest
push-to-payu account-service latest

# Pull images
pull-from-payu account-service latest
```

### 2. Development Workflow
```bash
# Start development environment
ddev

# Monitor logs
ddev-log

# Check resource usage
dtop-cpu
dtop-net

# Run tests
dtest-unit
```

### 3. Production Deployment
```bash
# Build optimized images
dbuild --layers --tag registry.payu.internal/account-service:v1.0

# Push to registry
dp registry.payu.internal/account-service:v1.0

# Deploy with podman-compose
dcp up -d
```

---

## Best Practices

### 1. Security
- Use non-root containers where possible
- Enable read-only filesystems for stateless services
- Implement proper RBAC for registry access
- Use image scanning for vulnerabilities
- Enable digital signatures for images

### 2. Performance
- Enable BuildKit with `BUILDAH_LAYERS=1`
- Use overlay storage driver
- Set appropriate CPU/memory limits
- Configure proper network MTU
- Use CNI plugins for networking

### 3. Monitoring
- Use `podman stats` for resource monitoring
- Configure logging to journald
- Set up proper log rotation
- Monitor registry access patterns

### 4. Development
- Use `ddev` for development environment
- Use `dtest-*` commands for testing
- Use `dshow-aliases` for quick reference
- Use `podman-status` to verify setup

---

## Troubleshooting

### Common Issues

1. **Container not starting**
```bash
# Check container status
dps -a

# View logs
dlogsf container-name

# Inspect container
dinspect container-name
```

2. **Image build errors**
```bash
# Build with verbose output
db --verbose

# Check build cache
db --layers

# Clear build cache
podman builder prune
```

3. **Network issues**
```bash
# List networks
dnetwork

# Inspect network
dnetwork-inspect payu-net

# Test connectivity
dexec container-name ping registry.payu.internal
```

4. **Permission issues**
```bash
# Check user namespace
id

# Enable user namespace if needed
sudo sysctl kernel.unprivileged_userns_clone=1
```

### Support Resources
- Podman documentation: https://docs.podman.io/
- PayU developer documentation: /home/ubuntu/payu/docs/
- Podman GitHub: https://github.com/containers/podman

---

## Next Steps

1. Review and customize `/home/ubuntu/payu/.env.example`
2. Set up the Podman configuration
3. Source the aliases in your development environment
4. Test the migration on non-production services
5. Document any custom workflows specific to your team

For additional help or questions, contact the DevOps team or refer to the PayU architecture documentation.
