# Quick Usage Guide

## Prerequisites
- Podman installed and working
- Docker build tools for building custom images

## 1. Quick Start (Development Environment)

```bash
# Navigate to project root
cd /home/ubuntu/payu

# Make the management script executable (if not already)
chmod +x containers/manage-podman.sh

# Start development environment
./containers/manage-podman.sh start-dev

# Check status
./containers/manage-podman.sh status
```

## 2. Quick Start (Test Environment)

```bash
# Start test environment
./containers/manage-podman.sh start-test

# View test logs
./containers/manage-podman.sh logs payu-postgres-test
```

## 3. Stop Services

```bash
# Stop all services
./containers/manage-podman.sh stop

# Stop specific service
./containers/manage-podman.sh stop keycloak
```

## 4. View Logs

```bash
# View logs for a specific service
./containers/manage-podman.sh logs postgres

# View real-time logs
./containers/manage-podman.sh logs keycloak -f
```

## 5. Access Services

Once services are running, you can access them at:

### Development Environment
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Keycloak: `http://localhost:8099`
- API Gateway: `http://localhost:8080`
- Web App: `http://localhost:3001`
- Grafana: `http://localhost:3000` (admin/admin)
- Jaeger: `http://localhost:16686`

### Test Environment
- Test PostgreSQL: `localhost:5433`
- Test Redis: `localhost:6380`
- Test Kafka: `localhost:9093`
- Test Keycloak: `http://localhost:8100`
- Test API Gateway: `http://localhost:8180`
- Test Web App: `http://localhost:3101`

## 6. System Integration

### Deploy as systemd Service (requires sudo)
```bash
# Deploy quadlet files
sudo ./containers/manage-podman.sh deploy

# Enable to start on boot
sudo systemctl enable podman-payu.service

# Check service status
sudo systemctl status podman-payu.service
```

### Manual Podman Commands
```bash
# Check running containers
podman ps | grep payu

# View all PayU containers
podman ps --filter name=payu

# Stop PayU services
podman down --file /home/ubuntu/payu/containers/podman-compose.yml

# Build services
podman compose --file /home/ubuntu/payu/containers/podman-compose.yml build

# View network status
podman network ls | grep payu

# View volumes
podman volume ls | grep payu
```

## 7. Environment Variables

Edit `.env` file to customize:
```env
# Database credentials
POSTGRES_USER=payu
POSTGRES_PASSWORD=your_secure_password
POSTGRES_DB=payu_account

# Keycloak credentials
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=your_secure_password
KEYCLOAK_DB_PASSWORD=your_secure_password

# Test environment
TEST_POSTGRES_USER=payu_test
TEST_POSTGRES_PASSWORD=your_test_password
```

## 8. Troubleshooting

### Common Issues

1. **Port already in use**
   ```bash
   # Find process using port
   sudo lsof -i :5432
   # Kill process if needed
   sudo kill -9 <PID>
   ```

2. **Network conflicts**
   ```bash
   # Remove existing networks
   podman network rm payu-network payu-test-network
   ```

3. **Volume issues**
   ```bash
   # Clean up all PayU volumes
   ./containers/manage-podman.sh clean-volumes
   ```

4. **Container not starting**
   ```bash
   # Check container status
   podman ps -a | grep payu

   # View container logs
   podman logs <container_name>
   ```

### Health Checks

All services have health checks. You can verify service health:

```bash
# Check if a service is healthy
podman inspect <container_name> --format '{{json .State.Health.Status}}'

# Wait for a service to be healthy
podman wait <container_name> --condition=healthy
```
