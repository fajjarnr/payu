# Deprecated Docker Compose Files

These files were archived on **February 9, 2026** as part of the Podman standardization effort.

PayU has standardized on **Podman** as the container runtime. All compose files are now in:
- `infrastructure/local-podman/podman-compose.yml` — Main development environment
- `infrastructure/local-podman/podman-compose.test.yml` — Isolated test environment

## Archived Files

| File | Original Location | Reason |
|:-----|:-----------------|:-------|
| `docker-compose.yml` | `/docker-compose.yml` | Replaced by `podman-compose.yml`. Had port conflict (P0-INFRA-001). |
| `docker-compose.test.yml` | `/docker-compose.test.yml` | Replaced by `podman-compose.test.yml`. |
| `performance-docker-compose.yml` | `/tests/performance/docker-compose.yml` | Gatling tests now use podman compose. |
| `verify_docker_compose.sh` | `/scripts/verify_docker_compose.sh` | Docker-specific verification. Replaced by Podman health checks. |
| `run_e2e_docker.sh` | `/scripts/run_e2e_docker.sh` | Replaced by `/scripts/run-e2e-container.sh` (Podman). |
| `test_docker_compose_verification.py` | `/tests/infrastructure/` | Tests docker-compose.yml which is no longer active. |

## If You Need These

If you still need Docker compatibility for any reason, these files are preserved here.
However, DO NOT use them in production or CI/CD — always use the Podman compose files.
