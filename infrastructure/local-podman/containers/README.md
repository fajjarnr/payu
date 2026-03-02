# Podman Compose Setup for PayU Digital Banking Platform

This directory contains Podman quadlet configurations for local development and testing of the PayU digital banking platform.

## Files Structure

### Podman Compose Files
- `podman-compose.yml` - Main development environment with all services
- `podman-compose.test.yml` - Test environment with isolated databases and services

### Quadlet Files
- `*.container` - Individual container configurations
- `*.network` - Network definitions
- `*.volume` - Volume definitions
- `*.target` - systemd service targets
- `*.service` - systemd service files

## Core Services Quadlet Files

### Data Layer
- `postgres.container` - PostgreSQL database with persistent storage
- `redis.container` - Redis cache with memory limits

### Messaging Layer
- `kafka.container` - Apache Kafka broker (KRaft mode, no Zookeeper — aligned with AMQ Streams on OpenShift)

### Identity Management
- `keycloak.container` - Keycloak identity server

## Network Configuration
- `payu-network.network` - Main development network (172.20.0.0/24)
- `payu-test-network.network` - Test environment network (172.21.0.0/24)

## Usage

### Starting Services
```bash
# Start all services using Podman Compose
podman play podman-compose.yml

# Start test environment services
podman play podman-compose.test.yml

# Start specific service (using quadlet)
systemctl start postgres.target
systemctl start keycloak.target
```

### Stopping Services
```bash
# Stop all services
podman down --all

# Stop specific service
systemctl stop postgres.target
```

### Checking Status
```bash
# Check podman-compose status
podman play ps

# Check systemd services
systemctl status podman-payu.service

# Check individual services
podman ps | grep payu
```

### Managing Networks and Volumes
```bash
# Create networks
podman network create payu-network
podman network create payu-test-network

# Clean up volumes
podman volume rm postgres_data redis_data
```

## Systemd Integration

The quadlet files can be deployed to systemd to manage services persistently:

```bash
# Install quadlet files to systemd
sudo cp *.service *.target /etc/systemd/system/
sudo cp *.container *.network *.volume /etc/containers/systemd/

# Reload systemd
sudo systemctl daemon-reload

# Enable and start services
sudo systemctl enable podman-payu.service
sudo systemctl start podman-payu.service
```

## Environment Variables

Make sure to create a `.env` file in the project root with the required variables:

```env
# PostgreSQL
POSTGRES_USER=payu
POSTGRES_PASSWORD=payu_secret
POSTGRES_DB=payu_account

# Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_DB_PASSWORD=payu_secret

# Test Environment
TEST_POSTGRES_USER=payu_test
TEST_POSTGRES_PASSWORD=test_secret
TEST_POSTGRES_DB=payu_test_account
TEST_KEYCLOAK_DB_PASSWORD=test_secret
```

## Port Mapping

### Main Development Environment
- **Database**: PostgreSQL (5432), Redis (6379)
- **Messaging**: Kafka (9092, 29092), Zookeeper (2181)
- **Identity**: Keycloak (8099)
- **Gateway**: API Gateway (8080)
- **Frontend**: Web App (3001)

### Test Environment
- **Database**: Test PostgreSQL (5433), Test Redis (6380)
- **Messaging**: Test Kafka (9093), Test Zookeeper (2182)
- **Identity**: Test Keycloak (8100)
- **Gateway**: Test API Gateway (8180)
- **Frontend**: Test Web App (3101)

## Health Checks

All core services include health checks with appropriate timeouts and retry intervals:
- PostgreSQL: 10s intervals, 5 retries
- Redis: 10s intervals, 3 retries
- Kafka: 30s intervals, 5 retries
- Keycloak: 15s intervals, 5 retries

## Pod Management

Services can be grouped into pods for better resource management:

```bash
# Create data pod
podman pod create --name payu-data-pod --network payu-network

# Create messaging pod
podman pod create --name payu-messaging-pod --network payu-network

# Create identity pod
podman pod create --name payu-identity-pod --network payu-network
```

Note: The quadlet files already include pod configuration in the `PodmanArgs` field.
