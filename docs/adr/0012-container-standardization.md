# ADR-0012: Container Standardization (Podman)

**Status**: Accepted
**Date**: 2026-01-30
**Deciders**: Platform Engineering

## Context

We need a container runtime for local development and CI/CD. Docker Desktop Licensing changes and the need for daemonless, rootless containers drive us to look for alternatives.

## Decision

Standardize on **Podman** (Pod Manager) for all container operations.

### Standards

1.  **Runtime**: Podman 4.x/5.x.
2.  **Definition**: Use `Containerfile` preferred over `Dockerfile`.
3.  **Orchestration**:
    - Local: `podman-compose` or `docker-compose` (compatible).
    - System: Quadlets (`.container` files) for systemd integration.
4.  **Rootless**: Default to rootless containers for security.

## Implementation

- Project contains `infrastructure/containers/` with Quadlet definitions.
- `scripts/podman-aliases.sh` provided for Docker CLI compatibility.
- CI pipelines use Podman for image building (Buildah).

## Consequences

- **Positive**: Improved security (rootless), open-source (no licensing fees), daemonless.
- **Negative**: Minor tooling differences vs Docker, some learning curve.
