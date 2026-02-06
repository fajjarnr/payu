# PayU Developer Onboarding Guide

> **Welcome to PayU Digital Banking Platform!**
>
> This guide will help you set up your development environment and get familiar with the platform.

---

## 📋 Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Environment Setup](#2-environment-setup)
3. [Project Structure](#3-project-structure)
4. [Running the Application](#4-running-the-application)
5. [Development Workflow](#5-development-workflow)
6. [Testing](#6-testing)
7. [Troubleshooting](#7-troubleshooting)
8. [Resources](#8-resources)

---

## 1. Prerequisites

### Required Tools

| Tool | Version | Description |
|:-----|:--------|:------------|
| **Java** | 21+ | Core banking services runtime |
| **Maven** | 3.9+ | Java build tool |
| **Node.js** | 22+ LTS | Frontend runtime |
| **Python** | 3.12+ | AI/ML services runtime |
| **Podman** | Latest | Container runtime (rootless) |
| **Git** | Latest | Version control |

### Verify Your Installation

```bash
# Run verification script
./scripts/verify-env.sh --deps
```

### Tool Installation

If you're missing any tools, run the full setup:

```bash
./scripts/setup.sh
```

This will install all required tools on:
- Ubuntu/Debian Linux
- Fedora/RHEL/CentOS
- macOS

---

## 2. Environment Setup

### Quick Start (Recommended)

For developers who already have tools installed:

```bash
# Clone the repository
git clone https://github.com/fajjarnr/payu.git
cd payu

# Quick start (build + start services)
./scripts/setup-dev.sh
```

This will:
1. Verify all prerequisites
2. Build all backend services (Java)
3. Build all frontend applications
4. Start all infrastructure services
5. Run health checks

### Manual Setup

If you prefer manual setup:

```bash
# 1. Build shared starters (required for backend)
cd backend/shared
mvn clean install -DskipTests

# 2. Build individual services
cd ../account-service && mvn clean package -DskipTests
cd ../auth-service && mvn clean package -DskipTests
# ... repeat for other services

# 3. Install frontend dependencies
cd ../../frontend/web-app
npm install --legacy-peer-deps

# 4. Start infrastructure services
cd ../..
podman-compose up -d postgres redis kafka keycloak

# 5. Start backend services (wait for infra to be ready)
podman-compose up -d account-service auth-service transaction-service wallet-service

# 6. Start frontend (in a new terminal)
cd frontend/web-app
npm run dev
```

### Environment Configuration

Copy the environment template:

```bash
cp .env.example .env
# Edit .env with your local configuration if needed
```

For local development with Podman Compose, the defaults usually work fine.

---

## 3. Project Structure

```
payu/
├── backend/                    # Backend microservices
│   ├── shared/                 # Shared libraries (starters)
│   │   ├── api-commons/        # Common API models
│   │   ├── cache-starter/     # Redis caching
│   │   ├── resilience-starter/ # Circuit breakers
│   │   └── security-starter/   # Security utilities
│   ├── account-service/        # User accounts & profiles
│   ├── auth-service/           # Authentication & MFA
│   ├── transaction-service/    # Transfers & payments
│   ├── wallet-service/         # Double-entry ledger
│   ├── kyc-service/            # OCR & liveness (Python)
│   ├── analytics-service/      # Fraud scoring (Python)
│   └── simulators/             # External service mocks
├── frontend/                   # Frontend applications
│   ├── web-app/                # Next.js 15 web application
│   ├── developer-docs/         # Partner portal docs
│   └── mobile/                 # React Native mobile app
├── docs/                       # Documentation
│   ├── architecture/           # Architecture diagrams
│   ├── guides/                 # Technical guides
│   └── roadmap/                # Project roadmap
├── infrastructure/             # OpenShift/K8s manifests
├── scripts/                    # Automation scripts
│   ├── setup.sh                # Full environment setup
│   ├── setup-dev.sh            # Quick start
│   └── verify-env.sh           # Health verification
├── docker-compose.yml          # Local development compose
└── pom.xml                     # Parent POM
```

---

## 4. Running the Application

### Start All Services

```bash
# Using Podman Compose
podman-compose up -d

# Or use the quick start script
./scripts/setup-dev.sh
```

### Service URLs

| Service | URL | Description |
|:--------|:-----|:------------|
| **Web App** | http://localhost:3001 | Main application UI |
| **API Gateway** | http://localhost:8080 | Backend API gateway |
| **API Portal** | http://localhost:8080/api-docs | API documentation |
| **Keycloak Admin** | http://localhost:8099 | User management |
| **Jaeger UI** | http://localhost:16686 | Distributed tracing |
| **Grafana** | http://localhost:3000 | Monitoring dashboard |

### Default Credentials

| Account | Username | Password | Realm |
|:--------|:---------|:---------|:-------|
| **Keycloak Admin** | `admin` | `P@ssw0rd123` | `master` |
| **Customer User** | `customer1` | `P@ssw0rd123` | `payu` |

### Check Service Health

```bash
# Verify all services
./scripts/verify-env.sh

# Quick health check
./scripts/verify-env.sh --quick
```

### Stop Services

```bash
podman-compose down

# Or use the script
./scripts/setup-dev.sh --stop
```

---

## 5. Development Workflow

### Backend Development (Java/Spring Boot)

```bash
# 1. Navigate to service
cd backend/account-service

# 2. Make changes
# ... edit source files ...

# 3. Build
mvn clean package -DskipTests

# 4. Restart service
podman restart payu-account-service

# Or rebuild container
podman-compose up -d --build account-service
```

### Frontend Development (Next.js)

```bash
# 1. Navigate to web app
cd frontend/web-app

# 2. Start dev server (with hot reload)
npm run dev

# 3. Open browser
# http://localhost:3001
```

### Python Services (KYC, Analytics)

```bash
# 1. Navigate to service
cd backend/kyc-service

# 2. Create virtual environment
python3 -m venv .venv
source .venv/bin/activate

# 3. Install dependencies
pip install -r requirements.txt

# 4. Run locally (for development)
uvicorn app.main:app --reload --port 8010

# Or use container
podman-compose up -d --build kyc-service
```

### Running Tests

```bash
# Backend tests (all services)
mvn -f backend/pom.xml test

# Single service tests
cd backend/account-service
mvn test

# Frontend tests
cd frontend/web-app
npm test
npm run test:e2e

# Verify test coverage
./scripts/verify-env.sh
```

### Code Quality

```bash
# Pre-commit hooks (auto-run on git commit)
pre-commit run --all-files

# Backend: Spotless formatting
cd backend/account-service
mvn spotless:apply

# Frontend: ESLint + Prettier
cd frontend/web-app
npm run lint
npm run format
```

---

## 6. Testing

### Unit Tests

```bash
# Backend
cd backend/account-service
mvn test

# Frontend
cd frontend/web-app
npm test
```

### Integration Tests

```bash
# Backend (requires services running)
cd backend/account-service
mvn verify -P integration

# With Testcontainers
mvn test -Dtest.groups=integration
```

### E2E Tests (Playwright)

```bash
cd frontend/web-app

# Install browsers
npx playwright install --with-deps

# Run E2E tests
npx playwright test

# With UI
npx playwright test --ui
```

### Performance Tests (k6)

```bash
# Install k6 first (via setup.sh)
k6 run tests/performance/load-test.js
```

---

## 7. Troubleshooting

### Services Won't Start

```bash
# Check service logs
podman logs payu-account-service -f

# Check all services
podman ps -a

# Restart specific service
podman restart payu-account-service
```

### Port Already in Use

```bash
# Find process using port
lsof -i :8001

# Or
netstat -tulpn | grep 8001

# Stop conflicting service
sudo systemctl stop postgresql  # if you have local PostgreSQL
```

### Database Connection Issues

```bash
# Check PostgreSQL is ready
podman exec payu-postgres pg_isready -U payu

# Check database exists
podman exec payu-postgres psql -U payu -d payu_account -c "SELECT 1;"

# View logs
podman logs payu-postgres -f
```

### Out of Memory Errors

```bash
# Check container memory limits
podman inspect payu-account-service | jq '.[0].HostConfig.Memory'

# Increase limit in docker-compose.yml
# Then restart
podman-compose up -d --force-recreate
```

### Clean Restart

```bash
# Stop everything
podman-compose down

# Remove volumes (WARNING: deletes data!)
podman volume rm payu_postgres_data payu_redis_data

# Rebuild and start
./scripts/setup-dev.sh --clean
./scripts/setup-dev.sh
```

### Get Help

1. Check [docs/TROUBLESHOOTING.md](TROUBLESHOOTING.md)
2. Check [docs/guides/LESSONS.md](../guides/LESSONS.md)
3. Search existing issues: `docs/roadmap/TODOS.md`
4. Create new issue with:
   - Error messages
   - Steps to reproduce
   - Environment details

---

## 8. Resources

### Documentation

| Document | Path | Description |
|:---------|:-----|:------------|
| **Usage Guide** | `/docs/USAGE.md` | API credentials & quick start |
| **Architecture** | `/docs/architecture/` | System design & patterns |
| **API Docs** | http://localhost:8080/api-docs | Interactive API documentation |
| **Roadmap** | `/docs/roadmap/TODOS.md` | Active tasks & progress |
| **Lessons** | `/docs/guides/LESSONS.md` | Learned lessons & patterns |

### Key Concepts

- **Hexagonal Architecture**: Core logic isolated from infrastructure
- **Event-First**: Kafka for cross-service communication
- **Zero Trust**: All requests authenticated
- **API-First**: OpenAPI contracts before implementation

### Development Commands Quick Reference

```bash
# Quick start
./scripts/setup-dev.sh              # Build + start all
./scripts/verify-env.sh            # Check health

# Service management
podman-compose up -d               # Start all
podman-compose up -d <service>      # Start one service
podman-compose logs -f <service>    # View logs
podman-compose down                # Stop all

# Backend
mvn clean package -DskipTests      # Build service
mvn test                            # Run tests
mvn spotless:apply                 # Format code

# Frontend
npm install                         # Install deps
npm run dev                         # Start dev server
npm run build                       # Production build
npm test                            # Run tests
npm run lint                        # Check code quality
```

### Best Practices

1. **Always run tests** before committing
2. **Format code** with spotless (backend) or prettier (frontend)
3. **Use meaningful commit messages** (Conventional Commits)
4. **Update documentation** when changing APIs
5. **Check health endpoints** after changes
6. **Never commit secrets** (passwords, keys, tokens)

### Getting Help

- **Documentation**: Check `/docs/` first
- **Issues**: GitHub Issues
- **Architecture**: See `/docs/architecture/ARCHITECTURE.md`
- **Troubleshooting**: See `/docs/TROUBLESHOOTING.md`

---

**Happy Coding! 🚀**

For questions or issues, refer to the documentation or create a GitHub issue.
