# PayU Quadlet Systemd Container Files

This directory contains quadlet `.container` files for managing PayU services with systemd.

## What is Quadlet?

Quadlet allows Podman containers to be managed by systemd. With quadlet, you can:
- Start/stop containers with `systemctl start/stop <service>`
- Enable auto-start on boot with `systemctl enable <service>`
- View logs with `journalctl -u <service>`
- Manage container dependencies automatically

## Installation

### System-wide (recommended for servers)
```bash
sudo cp *.container /etc/containers/systemd/
sudo systemctl daemon-reload
```

### User-specific (for development)
```bash
mkdir -p ~/.config/containers/systemd
cp *.container ~/.config/containers/systemd/
systemctl --user daemon-reload
```

## Usage

### Start all PayU services
```bash
# Start infrastructure services first
sudo systemctl start payu-postgres payu-redis payu-kafka

# Then start application services
sudo systemctl start payu-account-service payu-auth-service payu-gateway-service
sudo systemctl start payu-transaction-service payu-wallet-service
```

### Enable auto-start on boot
```bash
sudo systemctl enable payu-postgres payu-redis payu-kafka
sudo systemctl enable payu-account-service payu-auth-service payu-gateway-service
```

### Check service status
```bash
sudo systemctl status payu-gateway-service
```

### View logs
```bash
# Follow logs
sudo journalctl -u payu-gateway-service -f

# Last 100 lines
sudo journalctl -u payu-gateway-service -n 100
```

### Stop services
```bash
sudo systemctl stop payu-gateway-service
```

## Service Dependencies

The quadlet files define dependencies automatically:
- Application services depend on postgres, redis, kafka
- Kafka depends on zookeeper
- Gateway depends on most backend services

## Available Services

### Infrastructure
- `postgres.container` - PostgreSQL database
- `redis.container` - Redis cache
- `zookeeper.container` - Zookeeper for Kafka
- `kafka.container` - Kafka message broker
- `jaeger.container` - Distributed tracing

### Core Services
- `account-service.container` - User accounts, eKYC, multi-pocket
- `auth-service.container` - Authentication, MFA, biometrics
- `gateway-service.container` - API Gateway, rate limiting
- `transaction-service.container` - Transfers, BI-FAST, QRIS
- `wallet-service.container` - Double-entry ledger

### Business Services
- `billing-service.container` - Bill payments
- `notification-service.container` - Push, SMS, email
- `compliance-service.container` - Regulatory compliance
- `investment-service.container` - Mutual funds, gold
- `lending-service.container` - Loans, PayLater

### ML Services
- `kyc-service.container` - OCR, liveness detection
- `analytics-service.container` - Fraud scoring, insights

## Customization

### Environment Variables
Edit the `.container` files to customize environment variables:
```ini
[Container]
Environment=MY_VAR=my_value
```

### Secrets
Create secrets with podman:
```bash
podman secret create postgres-password <password-file>
```

Then reference in the container file:
```ini
[Container]
Secret=postgres-password,type=env,target=POSTGRES_PASSWORD
```

### Resource Limits
Adjust resource limits in the `[Container]` section:
```ini
[Container]
Memory=2G
MemorySwap=4G
CPUShares=2048
```

Or in the `[Service]` section:
```ini
[Service]
MemoryMax=3G
CPUQuota=200%
```

## Troubleshooting

### Container fails to start
```bash
# Check systemd status
sudo systemctl status payu-<service>

# Check podman logs
sudo podman logs payu-<service>

# Check journal
sudo journalctl -u payu-<service> --no-pager
```

### Network issues
```bash
# Check network
sudo podman network ls
sudo podman network inspect payu-network

# Recreate network
sudo podman network rm payu-network
sudo podman network create payu-network
```

### Permission issues
```bash
# For rootless mode, ensure user has subordinate UIDs
grep $USER /etc/subuid
grep $USER /etc/subgid

# If missing, add with:
sudo usermod --add-subuids 100000-165536 $USER
sudo usermod --add-subgids 100000-165536 $USER
```

## References

- [Podman Quadlet Documentation](https://docs.podman.io/en/latest/markdown/podman-systemd.unit.5.html)
- [Systemd Service Documentation](https://www.freedesktop.org/software/systemd/man/systemd.service.html)
