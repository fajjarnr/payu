# Local Podman Development

This stack mirrors the `payu-dev` service identities with single-node local
infrastructure. It supports either host-run services or containerized workloads.

## Prerequisites

```bash
podman --version
podman compose version
podman login registry.redhat.io
```

`podman compose` requires an external provider such as the Ubuntu
`podman-compose` package.

## Commands

Run from the repository root:

```bash
# PostgreSQL, Data Grid, Kafka, Artemis, and Keycloak
infrastructure/local/podman/containers/manage-podman.sh core

# Verify the running core protocols without changing container state
infrastructure/local/podman/containers/manage-podman.sh smoke

# Build and run backend services plus web-app in containers
infrastructure/local/podman/containers/manage-podman.sh apps

# Run apps and APIcast together
infrastructure/local/podman/containers/manage-podman.sh all

infrastructure/local/podman/containers/manage-podman.sh status
infrastructure/local/podman/containers/manage-podman.sh stop
```

`stop` preserves named volumes. Remove volumes manually only when an intentional
database reset is required.

## Host-run development

After `core`, start a backend service with its `local` profile from the host.
The local profiles use PostgreSQL `5432`, Kafka `9092`, Data Grid `11222`, and
Keycloak `8099`.

```bash
mvn -f backend/account-service/pom.xml spring-boot:run -Dspring-boot.run.profiles=local

# Quarkus gateway (install its shared module once after a clean Maven cache)
mvn -f backend/pom.xml -pl shared/quarkus-api-commons -am install -DskipTests
mvn -f backend/gateway-service/pom.xml quarkus:dev \
  -Dquarkus.profile=local -Dquarkus.test.continuous-testing=disabled
```

Host endpoints:

| Component | Endpoint |
|:---|:---|
| PostgreSQL | `localhost:5432` |
| Data Grid Hot Rod/REST | `localhost:11222` |
| Kafka | `localhost:9092` |
| Artemis | `localhost:61616` / console `localhost:8161` |
| Keycloak | `http://localhost:8099` |
| Gateway | `http://localhost:8080` |
| Web app container | `http://localhost:3001` |
| APIcast | `http://localhost:8095` |
| CMS | `http://localhost:8097` |

Application containers use the same service identities as `payu-dev`, but use
plain local transport and development-only credentials by design.
