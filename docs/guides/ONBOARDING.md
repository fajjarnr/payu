# Developer Onboarding Guide

> **First-day setup and environment configuration for new PayU developers**

Welcome to the PayU Digital Banking Platform! This guide will help you get up and running quickly.

---

## 📋 Pre-Onboarding Checklist

Before your first day, ensure you have:

- [ ] **Company laptop** with admin privileges
- [ ] **GitHub account** added to PayU organization
- [ ] **OpenShift cluster access** credentials
- [ ] **Slack account** joined to `#payu-dev` workspace
- [ ] **Email access** configured

---

## 🚀 Day 1: Environment Setup

### 1. Clone Repository

```bash
# Clone with SSH (recommended)
git clone git@github.com:payu-id/payu.git
cd payu

# Or with HTTPS
git clone https://github.com/payu-id/payu.git
cd payu

# Verify upstream remote
git remote -v
```

### 2. Install Prerequisites

#### Java 21 LTS

```bash
# Verify Java installation
java -version  # Should show 21.x.x

# If not installed:
# macOS: brew install openjdk@21
# Ubuntu: sudo apt install openjdk-21-jdk
# Windows: Download from adoptium.net
```

#### Maven 3.9+

```bash
# Verify Maven
mvn -version  # Should show 3.9.x or higher

# Set environment variables
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export MAVEN_HOME=/usr/local/opt/maven
```

#### Node.js 22+

```bash
# Verify Node.js
node -v  # Should show v22.x or higher
npm -v   # Should show 10.x or higher

# If not installed:
# macOS: brew install node
# Ubuntu: sudo apt install nodejs npm
```

#### Python 3.12

```bash
# Verify Python
python3 --version  # Should show 3.12.x

# If not installed:
# macOS: brew install python@3.12
# Ubuntu: sudo apt install python3.12 python3.12-venv
```

#### Podman (Preferred) / Docker (Fallback)

```bash
# Verify Podman
podman --version
podman compose version || podman-compose --version

# If using Docker instead
docker --version
docker compose version

# Start Docker Desktop if not running
open -a Docker
```

### 3. Configure Git

```bash
# Set your Git credentials
git config --global user.name "Your Name"
git config --global user.email "your.email@payu.id"

# Set default branch
git config --global init.defaultBranch main

# Enable GPG signing for commits (optional)
git config --global commit.gpgsign true
```

### 4. Install IDE Extensions

**For VS Code:**

```bash
# Install recommended extensions
code --install-extension ms-vscode.vscode-typescript-next
code --install-extension golang.go
code --install-extension ms-python.python
code --install-extension redhat.java
code --install-extension vscjava.vscode-java-pack
code --install-extension dbaeumer.vscode-eslint
code --install-extension esbenp.prettier-vscode
```

**For IntelliJ IDEA:**

- Install Spring Boot Plugin
- Install Lombok Plugin
- Enable annotation processing

---

## 🔧 Development Environment Setup

### 1. Local Infrastructure (Compose)

```bash
# Start all infrastructure services (preferred)
podman compose up -d

# Or with Docker
docker compose up -d

# Verify services are running
podman compose ps

# Check logs
podman compose logs -f

# Docker fallback
docker compose ps
docker compose logs -f
```

Services started:
- PostgreSQL 16 (port 5432)
- Redis/Data Grid (port 6379)
- Kafka (port 9092)
- Keycloak (port 8080)

### 2. Backend Services

```bash
# Build all services (skip tests for speed)
cd backend
mvn clean package -DskipTests -T 1C

# Or build specific service
cd account-service
mvn clean package -DskipTests

# Run a service
cd account-service
mvn spring-boot:run
```

### 3. Frontend Applications

```bash
# Web App
cd frontend/web-app
npm install
npm run dev

# Developer Docs
cd frontend/developer-docs
npm install
npm run dev

# Mobile App
cd frontend/mobile
npm install
npm run start  # Expo Go app
```

---

## 🔑 Access & Credentials

### OpenShift Cluster Access

```bash
# Login to OpenShift
oc login https://api.payu-openshift.id:6443 \
  --username=<your-username> \
  --password=<your-password>

# Set current project
oc project payu-dev

# Verify access
oc get pods
```

### Database Access

| Environment | Host | Port | Username | Password |
|-------------|------|------|----------|----------|
| **Local** | localhost | 5432 | payu | payu123 |
| **Dev** | postgres-dev.payu.svc | 5432 | See Vault | See Vault |

### Service Accounts

| Service | Purpose | How to Get |
|---------|---------|-----------|
| **GitHub** | Code access | Request in #platform-ops |
| **OpenShift** | Cluster access | Request in #platform-ops |
| **Vault** | Secrets access | Request in #security |

---

## 📚 Essential Documentation

Read these in order:

1. **[INDEX.md](./INDEX.md)** - Documentation catalog
2. **[ARCHITECTURE.md](./architecture/ARCHITECTURE.md)** - System architecture overview
3. **[CONTRIBUTING.md](./guides/CONTRIBUTING.md)** - Git workflow & conventions
4. **[QA_STRATEGY.md](./qa/QA_STRATEGY.md)** - Testing standards

---

## 🏃 Quick Start Tasks

Complete these tasks in your first week:

### Day 1-2: Setup & Hello World

- [ ] Complete environment setup
- [ ] Run all infrastructure services locally
- [ ] Build and run `account-service` locally
- [ ] Run `web-app` locally
- [ ] Make a small test change and verify it works

### Day 3-4: Understand the Codebase

- [ ] Read ARCHITECTURE.md (focus on microservices)
- [ ] Explore `backend/account-service` code
- [ ] Explore `frontend/web-app` code
- [ ] Run unit tests for a service
- [ ] Review the C4 diagrams

### Day 5: First Contribution

- [ ] Pick a small task from TODOS.md
- [ ] Create a feature branch
- [ ] Make the change
- [ ] Write/update tests
- [ ] Submit a PR

---

## 🧪 Verification Commands

Verify your setup by running these commands:

```bash
# 1. Verify Java
java -version  # Expected: openjdk 21.x.x

# 2. Verify Maven
mvn -version   # Expected: Apache Maven 3.9.x

# 3. Verify Node.js
node -v        # Expected: v22.x.x
npm -v         # Expected: 10.x.x

# 4. Verify Python
python3 --version  # Expected: Python 3.12.x

# 5. Verify Podman
podman ps       # Should list running containers

# 6. Verify Infrastructure
curl -s http://localhost:5432 > /dev/null && echo "PostgreSQL OK"
curl -s http://localhost:6379 > /dev/null && echo "Redis OK"

# 7. Verify Git
git remote -v  # Should show payu origin

# 8. Verify OpenShift (if configured)
oc whoami       # Should show your username
```

---

## 🆘 Troubleshooting

### Issue: "Port already in use"

```bash
# Find process using port
lsof -i :5432  # PostgreSQL
lsof -i :8080  # Backend service

# Kill process
kill -9 <PID>
```

### Issue: "Maven build fails"

```bash
# Clean Maven cache
rm -rf ~/.m2/repository

# Rebuild
mvn clean install -DskipTests -T 1C
```

### Issue: "npm install fails"

```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Issue: "Docker containers won't start"

```bash
# Check Docker Desktop is running
docker info

# Restart Docker Desktop
# Or restart Docker daemon (Linux)
sudo systemctl restart docker
```

---

## 📖 Learning Resources

### Internal Resources

- **[Agent Skills Guide](./guides/AGENT_SKILLS_GUIDE.md)** - AI-assisted development
- **[TDD Quick Reference](./guides/TDD_QUICK_REFERENCE.md)** - Test-driven development
- **[Database Optimization](./guides/DATABASE_CACHE_OPTIMIZATION.md)** - DB best practices

### External Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Quarkus Documentation](https://quarkus.io/guides/)
- [Next.js Documentation](https://nextjs.org/docs)
- [React Native Docs](https://reactnative.dev/docs/getting-started)
- [OpenShift Documentation](https://docs.openshift.com/)

---

## 👥 Who to Ask

| Question Type | Channel/Person |
|---------------|----------------|
| **Setup issues** | #platform-ops Slack channel |
| **Code review** | Create PR, request review |
| **Architecture** | @architect Slack mention |
| **Security** | #security Slack channel |
| **Product** | #product Slack channel |
| **General help** | #payu-dev Slack channel |

---

## ✅ Onboarding Completion Checklist

Complete all items to finish onboarding:

### Week 1
- [ ] Environment fully configured
- [ ] Can build and run backend services
- [ ] Can build and run frontend apps
- [ ] First PR submitted and merged

### Month 1
- [ ] Completed 3-5 tasks from TODOS.md
- [ ] Written tests for new code
- [ ] Presented a topic in team standup
- [ ] Reviewed at least 2 PRs

---

**Welcome aboard!** We're excited to have you on the team. If you have any questions, don't hesitate to reach out.

---

_Last Updated: January 30, 2026_
