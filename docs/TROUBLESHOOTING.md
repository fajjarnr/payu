# PayU Troubleshooting Guide

> **Common issues and solutions for PayU Digital Banking Platform development**

---

## 📋 Table of Contents

1. [Installation Issues](#1-installation-issues)
2. [Container Issues](#2-container-issues)
3. [Database Issues](#3-database-issues)
4. [Service Health Issues](#4-service-health-issues)
5. [Build Issues](#5-build-issues)
6. [Frontend Issues](#6-frontend-issues)
7. [Performance Issues](#7-performance-issues)
8. [Networking Issues](#8-networking-issues)

---

## 1. Installation Issues

### Setup Script Fails

**Problem**: `./setup.sh` fails with errors

**Solutions**:
```bash
# Check OS compatibility
cat /etc/os-release

# Run with verbose output
bash -x ./scripts/setup.sh 2>&1 | tee setup.log

# Try installing components individually
./scripts/setup.sh --backend
./scripts/setup.sh --frontend
./scripts/setup.sh --podman
```

### Java Version Mismatch

**Problem**: Service fails with "Unsupported class file major version"

**Solutions**:
```bash
# Check Java version
java -version

# Install Java 21
./scripts/setup.sh --backend

# Set JAVA_HOME manually
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
```

### Node.js Version Issues

**Problem**: Frontend build fails with module errors

**Solutions**:
```bash
# Check Node version
node --version  # Should be v22+

# Install correct version via nvm
nvm install 22
nvm use 22

# Or via setup script
./scripts/setup.sh --frontend
```

### Permission Denied Errors

**Problem**: Scripts fail with "Permission denied"

**Solutions**:
```bash
# Make scripts executable
chmod +x scripts/*.sh

# Or run with bash
bash ./scripts/setup.sh
```

---

## 2. Container Issues

### Podman Compose Not Found

**Problem**: `podman-compose: command not found`

**Solutions**:
```bash
# Install podman-compose
pip3 install --user podman-compose
# Or
sudo apt install podman-compose

# Add to PATH if needed
export PATH=$HOME/.local/bin:$PATH
```

### Container Won't Start

**Problem**: Service exits immediately after starting

**Solutions**:
```bash
# Check container logs
podman logs payu-account-service

# Check exit code
podman ps -a --filter "name=payu-account-service"

# Common causes:
# 1. Port conflict - See Networking Issues
# 2. Missing environment variables
# 3. Database not ready

# Try restarting with delay
podman-compose stop account-service
podman-compose up -d account-service
```

### Container in Restart Loop

**Problem**: Container keeps restarting

**Solutions**:
```bash
# Check logs for error
podman logs payu-account-service --tail 100

# Check health status
podman inspect payu-account-service | jq '.[0].State.Health'

# Common causes:
# 1. Database connection failed
# 2. Configuration error
# 3. Out of memory

# Try increasing memory limit in docker-compose.yml
# Then rebuild
podman-compose up -d --force-recreate <service>
```

### Out of Memory (OOM Killed)

**Problem**: Container exits with code 137

**Solutions**:
```bash
# Check container limits
podman inspect payu-account-service | jq '.[0].HostConfig.Memory'

# Increase in docker-compose.yml:
# services:
#   account-service:
#     mem_limit: 2g  # Increase from 1g

# Rebuild and restart
podman-compose up -d --force-recreate account-service
```

---

## 3. Database Issues

### PostgreSQL Connection Refused

**Problem**: Service can't connect to PostgreSQL

**Solutions**:
```bash
# Check PostgreSQL is running
podman ps | grep postgres

# Check PostgreSQL is ready
podman exec payu-postgres pg_isready -U payu

# Check database exists
podman exec payu-postgres psql -U payu -l | grep payu

# Restart PostgreSQL
podman restart payu-postgres

# Check connection string in application.yml
# Should be: jdbc:postgresql://postgres:5432/payu_account
```

### Database Schema Issues

**Problem**: "Relation does not exist" errors

**Solutions**:
```bash
# Check Flyway migrations
cd backend/account-service
mvn flyway:info
mvn flyway:migrate

# Check if tables exist
podman exec payu-postgres psql -U payu -d payu_account -c "\dt"

# Reset database (WARNING: deletes data!)
podman exec payu-postgres psql -U payu -d payu_account -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
mvn flyway:migrate
```

### Keycloak Database Issues

**Problem**: Keycloak won't start or can't connect

**Solutions**:
```bash
# Check Keycloak database
podman exec payu-postgres psql -U payu -d keycloak -c "SELECT 1;"

# Reset Keycloak (WARNING: deletes users!)
podman volume rm payu_keycloak_data
podman-compose up -d keycloak

# Access Keycloak CLI
podman exec payu-keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master \
  --user admin --password P@ssw0rd123
```

---

## 4. Service Health Issues

### Health Check Returns DOWN

**Problem**: `/actuator/health` returns `{"status":"DOWN"}`

**Solutions**:
```bash
# Get detailed health info
curl http://localhost:8001/actuator/health | jq '.'

# Check individual components:
# - Database connectivity
# - Redis connectivity
# - Kafka connectivity

# Common causes:
# 1. Redis: Missing REDIS_HOST env var
# 2. Database: Connection pool exhausted
# 3. Kafka: Consumer group not ready

# Check service logs
podman logs payu-account-service -f | grep -i error
```

### Redis Connection Failure

**Problem**: "Unable to connect to Redis" in health check

**Solutions**:
```bash
# Check Redis is running
podman ps | grep redis

# Test Redis connection
podman exec payu-redis redis-cli ping

# Check REDIS_HOST env var
podman exec payu-account-service printenv | grep REDIS

# Fix: Add to docker-compose.yml
# environment:
#   REDIS_HOST: redis
#   REDIS_PORT: 6379
#   PAYU_CACHE_REDIS_HOST: redis

# Then restart service
podman-compose up -d account-service
```

### Kafka Consumer Errors

**Problem**: "LEADER_NOT_AVAILABLE" in logs

**Solutions**:
```bash
# Check Kafka is running
podman ps | grep kafka

# Check topic exists
podman exec payu-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Create topic if missing
podman exec payu-kafka kafka-topics --bootstrap-server localhost:9092 \
  --create --topic cache-invalidation --partitions 3 --replication-factor 1

# Reset consumer group (WARNING: may lose data)
podman exec payu-kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --delete --group account-service-container
```

---

## 5. Build Issues

### Maven Build Fails

**Problem**: `mvn clean package` fails

**Solutions**:
```bash
# Clean Maven cache
rm -rf ~/.m2/repository/id/payu

# Rebuild shared starters first
cd backend/shared
mvn clean install -DskipTests

# Then rebuild service
cd ../account-service
mvn clean package -DskipTests

# Check for dependency conflicts
mvn dependency:tree

# Update snapshots
mvn clean install -U
```

### Lombok Annotation Issues

**Problem**: "Cannot find symbol" for Lombok annotations

**Solutions**:
```bash
# Check Lombok is configured
grep lombok pom.xml

# Try without Lombok (replace with explicit code)
# Or use annotation processor
mvn clean compile -Dmaven.compiler.proc=only

# Regenerate IDE config
mvn idea:idea  # or mvn eclipse:eclipse
```

### Docker Build Fails

**Problem**: Container build fails

**Solutions**:
```bash
# Check build context
# In docker-compose.yml:
# build:
#   context: ./backend  # Should be parent of all services
#   dockerfile: account-service/Dockerfile

# Try building manually
cd backend/account-service
podman build -t payu-account-service .

# Check for syntax errors in Dockerfile
podman build --no-cache -t payu-account-service .
```

### Frontend Build Fails

**Problem**: `npm install` or `npm run build` fails

**Solutions**:
```bash
# Clean install
rm -rf node_modules package-lock.json
npm install --legacy-peer-deps

# Check Node version
node --version  # Should be v22+

# Clear npm cache
npm cache clean --force

# Try with verbose output
npm install --verbose
```

---

## 6. Frontend Issues

### Web App Won't Load

**Problem**: Blank page or errors at http://localhost:3001

**Solutions**:
```bash
# Check dev server is running
cd frontend/web-app
npm run dev

# Check browser console for errors
# Common issues:
# 1. API Gateway not running
# 2. CORS errors
# 3. Missing environment variables

# Check NEXT_PUBLIC_ env vars
cat .env.local
```

### API Calls Failing

**Problem**: Frontend can't connect to backend

**Solutions**:
```bash
# Check gateway is running
curl http://localhost:8080/actuator/health

# Check CORS configuration
# In gateway-service application.yml

# Check browser console for errors
# Look for: CORS, network error, 401 Unauthorized

# Verify credentials
curl -X POST http://localhost:8002/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"customer1","password":"P@ssw0rd123"}'
```

### Hot Reload Not Working

**Problem**: Changes don't appear in browser

**Solutions**:
```bash
# Restart dev server
npm run dev

# Clear Next.js cache
rm -rf .next

# Check file watcher limits
# Linux:
echo fs.inotify.max_user_watches=524288 | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

---

## 7. Performance Issues

### Slow Service Startup

**Problem**: Services take too long to start

**Solutions**:
```bash
# Check health checks are not blocking
# In application.yml:
# management:
#   health:
#     readinessstate:
#       enabled: false  # Disable if causing delays

# Increase startup timeout in docker-compose.yml
# healthcheck:
#   start_period: 60s  # Increase from 30s

# Check for blocking operations in logs
podman logs payu-account-service -f
```

### High Memory Usage

**Problem**: Services using too much memory

**Solutions**:
```bash
# Check container memory
podman stats --no-stream

# Tune JVM settings
# In docker-compose.yml:
# JAVA_OPTS: "-XX:MaxRAMPercentage:60.0"

# Check for memory leaks
# Take heap dump
podman exec payu-account-service jcmd 1 GC.heap_dump /tmp/heap.hprof
```

### Database Slow Queries

**Problem**: Slow API responses from database

**Solutions**:
```bash
# Check PostgreSQL connections
podman exec payu-postgres psql -U payu -d payu_account \
  -c "SELECT count(*) FROM pg_stat_activity;"

# Check long-running queries
podman exec payu-postgres psql -U payu -d payu_account \
  -c "SELECT pid, now() - query_start as duration, query FROM pg_stat_activity WHERE state = 'active';"

# Add indexes if needed
# Check with EXPLAIN ANALYZE
```

---

## 8. Networking Issues

### Port Already in Use

**Problem**: "Port is already allocated"

**Solutions**:
```bash
# Find process using port
sudo lsof -i :8001
sudo netstat -tulpn | grep 8001

# Stop conflicting service
sudo systemctl stop postgresql  # If local PostgreSQL conflicts

# Or change port in docker-compose.yml
# ports:
#   - "8002:8080"  # Use different host port
```

### Services Can't Communicate

**Problem**: Service A can't reach Service B

**Solutions**:
```bash
# Check services are on same network
podman network inspect payu_payu-network

# Check DNS resolution
podman exec payu-account-service getent hosts postgres

# Test connectivity
podman exec payu-account-service curl -s http://postgres:5432

# Common issues:
# 1. Using localhost instead of service name
# 2. Services on different networks
# 3. Firewall blocking
```

### CORS Errors

**Problem**: Browser blocks API calls due to CORS

**Solutions**:
```bash
# Check gateway CORS configuration
# In gateway-service application.yml:

# Or add CORS headers manually
curl -H "Origin: http://localhost:3001" \
  -H "Access-Control-Request-Method: GET" \
  -X OPTIONS http://localhost:8080/api/v1/accounts
```

---

## 🆘 Getting More Help

### Diagnostic Commands

```bash
# Full environment check
./scripts/verify-env.sh

# Check service logs
podman logs payu-account-service --tail 100

# Check service health
curl -s http://localhost:8001/actuator/health | jq '.'

# Database diagnostics
podman exec payu-postgres psql -U payu -d payu_account -c "SELECT version();"
```

### Creating Bug Reports

When reporting issues, include:

1. **Environment**: OS, tool versions
2. **Error messages**: Full stack traces
3. **Steps to reproduce**: What you did
4. **Expected vs actual**: What should happen
5. **Logs**: Relevant service logs

```bash
# Collect diagnostic info
./scripts/verify-env.sh > diagnostic.txt
podman logs payu-account-service --tail 200 > account-service.log
```

### Useful Resources

- [docs/DEVELOPER_ONBOARDING.md](DEVELOPER_ONBOARDING.md) - Getting started
- [docs/guides/LESSONS.md](../guides/LESSONS.md) - Learned lessons
- [docs/roadmap/TODOS.md](../roadmap/TODOS.md) - Known issues
- [docs/USAGE.md](USAGE.md) - API documentation

---

**Last Updated**: February 6, 2026
