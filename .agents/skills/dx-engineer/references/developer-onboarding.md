# PayU Developer Onboarding Guide

## Welcome to PayU! 🎉

Panduan quick-start untuk developer baru di PayU Digital Banking Platform.

---

## 🚀 Day 1: Environment Setup

### Prerequisites

```bash
# Required tools
- Java 21 (Temurin/Corretto)
- Node.js 20 LTS
- Docker Desktop / Podman
- kubectl / oc CLI
- Git

# Recommended IDE
- IntelliJ IDEA Ultimate (Java services)
- VS Code (Frontend, Python)
```

### Clone & Setup

```bash
# 1. Clone repository
git clone git@github.com:payu-id/payu-platform.git
cd payu-platform

# 2. Setup environment
cp .env.example .env

# 3. Start infrastructure
docker-compose up -d postgres redis kafka

# 4. Build all services
mvn clean package -DskipTests -T 1C

# 5. Run a service locally
cd backend/wallet-service
mvn spring-boot:run
```

### Verify Setup

```bash
# Check service health
curl http://localhost:8080/actuator/health

# Expected response
{"status":"UP"}
```

---

## 📁 Project Structure

```
payu-platform/
├── backend/                 # Microservices (Java/Python)
│   ├── account-service/
│   ├── wallet-service/
│   ├── transaction-service/
│   └── shared/              # Shared starters
├── frontend/
│   ├── web-app/            # Next.js web app
│   ├── mobile/             # Expo React Native
│   └── developer-docs/     # API documentation portal
├── infrastructure/          # K8s/OpenShift configs
├── docs/                    # Architecture documentation
└── .agents/                  # AI skill definitions
    └── skills/             # 18 specialized AI skills
```

---

## 🔑 Access Setup

### Day 1 Access Requests

| System | Request Via | Approval |
|--------|-------------|----------|
| GitHub (payu-id org) | IT Helpdesk | Auto |
| OpenShift Console | Platform Team | 1 day |
| Vault (dev secrets) | Security Team | 1 day |
| Grafana/Kibana | Platform Team | Auto |
| Jira/Confluence | IT Helpdesk | Auto |

### VPN Setup

```bash
# Download OpenVPN config
# Connect to: vpn.payu.fajjjar.my.id

# Verify internal access
curl https://api-internal.payu.fajjjar.my.id/health
```

---

## 🏗️ Architecture Quick Reference

### Service Communication

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Gateway   │────▶│   Service   │────▶│  PostgreSQL │
│   (Quarkus) │     │  (Spring)   │     │             │
└─────────────┘     └──────┬──────┘     └─────────────┘
                           │
                    ┌──────▼──────┐
                    │    Kafka    │
                    │ (AMQ Streams)│
                    └─────────────┘
```

### Key Patterns

| Pattern | Where Used | Reference |
|---------|------------|-----------|
| Hexagonal Architecture | All Java services | `core-banking-engineer` skill |
| Event Sourcing | Wallet, Transaction | `integration-architect` skill |
| CQRS | Read-heavy services | `data-architect` skill |
| Circuit Breaker | External APIs | `core-banking-engineer` skill |

---

## 🛠️ Daily Development Workflow

### 1. Pick Up a Task

```bash
# Create feature branch
git checkout -b feature/PAYU-1234-add-new-endpoint

# Link to Jira
# Branch name must contain ticket ID
```

### 2. Develop

```bash
# Run tests continuously
mvn test -Dtest=WalletServiceTest

# Check code style
mvn spotless:check

# Run locally with hot reload
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"
```

### 3. Submit PR

```bash
# Push and create PR
git push -u origin feature/PAYU-1234-add-new-endpoint

# PR will automatically:
# - Run unit tests
# - Run integration tests
# - Check code coverage (min 80%)
# - Run security scan
# - Deploy to preview environment
```

### 4. Code Review Checklist

- [ ] Tests cover happy path and edge cases
- [ ] No hardcoded secrets
- [ ] Proper error handling
- [ ] API documented in OpenAPI spec
- [ ] Database migrations are reversible
- [ ] Logging follows structured format

---

## 🤖 Using AI Skills

PayU memiliki **18 AI skills** untuk membantu development:

```bash
# View available skills
ls .agents/skills/

# Key skills for developers:
# - core-banking-engineer  → Java/Spring patterns
# - frontend-architect     → Next.js/React patterns
# - debugging-methodology  → Systematic debugging
# - quality-engineer       → Testing patterns
```

### Example: Ask AI for Help

```
"Using the core-banking-engineer skill, help me implement 
a new endpoint for account balance inquiry with proper 
Hexagonal Architecture patterns."
```

---

## 📚 Learning Path

### Week 1: Foundation

- [ ] Complete environment setup
- [ ] Read `docs/architecture/ARCHITECTURE.md`
- [ ] Understand Hexagonal Architecture
- [ ] Shadow a senior developer on a PR review

### Week 2: First Contribution

- [ ] Fix a "good-first-issue" bug
- [ ] Write unit tests for an existing feature
- [ ] Understand CI/CD pipeline

### Week 3: Feature Development

- [ ] Implement a small feature end-to-end
- [ ] Learn Kafka event publishing
- [ ] Understand database migrations

### Month 1: Independence

- [ ] Own a medium-sized feature
- [ ] Participate in on-call rotation (shadow)
- [ ] Present a tech topic to the team

---

## 🆘 Getting Help

### Slack Channels

| Channel | Purpose |
|---------|---------|
| `#dev-general` | General development questions |
| `#platform-support` | Infrastructure issues |
| `#code-review` | Request PR reviews |
| `#incidents` | Production issues |

### Documentation

- **Architecture**: `docs/architecture/`
- **API Reference**: https://docs.payu.fajjjar.my.id/api
- **Runbooks**: `docs/operations/`
- **AI Skills Guide**: `.agents/skills/REGISTRY.yaml`

### People

| Role | Contact |
|------|---------|
| Tech Lead | @tech-lead in Slack |
| Platform Team | @platform-team in Slack |
| Security Questions | @security-team in Slack |

---

## ✅ Onboarding Checklist

### Day 1
- [ ] Laptop setup complete
- [ ] GitHub access granted
- [ ] Repository cloned
- [ ] Local environment running

### Week 1
- [ ] All tool access obtained
- [ ] Completed architecture overview
- [ ] First PR submitted (even if small)
- [ ] Met with buddy/mentor

### Month 1
- [ ] Shipped first feature to production
- [ ] Comfortable with codebase
- [ ] Participated in code reviews
- [ ] Joined on-call shadow rotation

---

**Welcome to the team!** 🚀

Questions? Reach out to your buddy or post in `#dev-general`.
