# Contributing to PayU

Panduan untuk kontributor PayU Digital Banking Platform.

---

## Table of Contents

- [Git Workflow](#git-workflow)
- [Branching Strategy](#branching-strategy)
- [Commit Convention](#commit-convention)
- [Pull Request Process](#pull-request-process)
- [Definition of Done](#definition-of-done)
- [CI/CD Pipeline](#cicd-pipeline)
- [Code Review Guidelines](#code-review-guidelines)

---

## Git Workflow

PayU menggunakan **Trunk-Based Development** dengan short-lived feature branches.

```
main (trunk)
  │
  ├── feature/PAYU-123-add-transfer-api     (max 2-3 days)
  ├── feature/PAYU-456-fix-balance-calc     (max 2-3 days)
  ├── bugfix/PAYU-789-login-timeout
  └── hotfix/PAYU-999-critical-fix          (direct to main)
```

### Why Trunk-Based?

- ✅ Faster integration dan feedback
- ✅ Smaller, reviewable PRs
- ✅ Reduced merge conflicts
- ✅ Continuous deployment ready
- ✅ Feature flags untuk incomplete features

---

## Branching Strategy

### Branch Types

| Type        | Pattern                    | Description               | Lifetime  |
| ----------- | -------------------------- | ------------------------- | --------- |
| **main**    | `main`                     | Production-ready code     | Permanent |
| **feature** | `feature/PAYU-{id}-{desc}` | New features              | 1-3 days  |
| **bugfix**  | `bugfix/PAYU-{id}-{desc}`  | Bug fixes                 | 1-2 days  |
| **hotfix**  | `hotfix/PAYU-{id}-{desc}`  | Critical production fixes | < 1 day   |
| **release** | `release/v{version}`       | Release preparation       | 1-2 days  |

### Branch Naming Examples

```bash
# Good
feature/PAYU-123-add-bifast-transfer
bugfix/PAYU-456-fix-balance-calculation
hotfix/PAYU-789-fix-auth-bypass

# Bad
feature/new-feature          # No ticket ID
john-working-branch          # Not descriptive
fix                          # Too vague
```

### Creating a Branch

```bash
# Always start from latest main
git checkout main
git pull origin main

# Create feature branch
git checkout -b feature/PAYU-123-add-transfer-api

# Work on your changes...
git add .
git commit -m "feat(transaction): add BI-FAST transfer endpoint"

# Push and create PR
git push -u origin feature/PAYU-123-add-transfer-api
```

---

## Commit Convention

Menggunakan [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Commit Types

| Type       | Description      | Example                                       |
| ---------- | ---------------- | --------------------------------------------- |
| `feat`     | New feature      | `feat(wallet): add virtual card support`      |
| `fix`      | Bug fix          | `fix(auth): resolve token expiry issue`       |
| `docs`     | Documentation    | `docs(api): update transfer endpoint docs`    |
| `refactor` | Code refactoring | `refactor(account): extract validation logic` |
| `test`     | Adding tests     | `test(wallet): add balance reservation tests` |
| `chore`    | Maintenance      | `chore(deps): update Spring Boot to 3.4.2`    |
| `perf`     | Performance      | `perf(db): optimize account query with index` |
| `ci`       | CI/CD changes    | `ci(pipeline): add security scanning stage`   |

### Scopes

| Scope          | Service/Area             |
| -------------- | ------------------------ |
| `account`      | Account Service          |
| `auth`         | Auth Service             |
| `transaction`  | Transaction Service      |
| `wallet`       | Wallet Service           |
| `billing`      | Billing Service          |
| `notification` | Notification Service     |
| `gateway`      | Gateway Service          |
| `infra`        | Infrastructure/OpenShift |
| `deps`         | Dependencies             |

### Examples

```bash
# Feature
feat(transaction): add QRIS payment support

Implements QR code generation and payment processing
for QRIS merchant payments.

Closes PAYU-123

# Bug fix
fix(wallet): correct balance calculation for reserved funds

The balance was not properly deducting reserved amounts
when displaying available balance.

Fixes PAYU-456

# Breaking change
feat(api)!: change response format for transfers

BREAKING CHANGE: Transfer response now includes
`transactionId` instead of `txnId`.
```

---

## Pull Request Process

### Before Creating PR

1. ✅ All tests pass locally (`mvn test`)
2. ✅ Code follows style guidelines
3. ✅ No new lint warnings
4. ✅ CHANGELOG.md updated (if applicable)
5. ✅ Documentation updated (if applicable)

### PR Title Format

```
[PAYU-{id}] <type>: <description>
```

Examples:

- `[PAYU-123] feat: add BI-FAST transfer endpoint`
- `[PAYU-456] fix: resolve balance calculation error`

### PR Size Guidelines

| Size      | Lines Changed | Review Time       |
| --------- | ------------- | ----------------- |
| 🟢 Small  | < 100         | < 30 min          |
| 🟡 Medium | 100-400       | 30-60 min         |
| 🔴 Large  | > 400         | Split if possible |

> **Tip**: Smaller PRs = faster reviews = faster merges

### Required Approvals

| Branch Target    | Approvals | Required Reviewers           |
| ---------------- | --------- | ---------------------------- |
| `main` (feature) | 2         | 1 code owner + 1 team member |
| `main` (hotfix)  | 1         | 1 tech lead                  |
| `release/*`      | 2         | 1 tech lead + 1 QA           |

---

## Definition of Done

A task is considered **DONE** when:

### Code Quality ✅

- [ ] Code compiles without errors
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Code coverage meets threshold (80% line, 70% branch)
- [ ] No new SonarQube/lint issues
- [ ] ArchUnit rules pass

### Documentation ✅

- [ ] Code is self-documenting (clear naming, comments where needed)
- [ ] API documentation updated (if applicable)
- [ ] CHANGELOG.md updated for user-facing changes
- [ ] README updated if setup/config changes

### Review ✅

- [ ] PR reviewed and approved by required reviewers
- [ ] All review comments addressed
- [ ] No unresolved conversations

### Testing ✅

- [ ] Unit tests written for new code
- [ ] Integration tests for API changes
- [ ] Manual testing completed (if applicable)
- [ ] Security testing for sensitive changes

### Deployment ✅

- [ ] Feature flag configured (if applicable)
- [ ] Database migration tested
- [ ] Monitoring/alerts configured
- [ ] Rollback plan documented

---

## CI/CD Pipeline

### Pipeline Stages

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   BUILD     │───▶│    TEST     │───▶│   SCAN      │───▶│   DEPLOY    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
     │                   │                  │                  │
     ▼                   ▼                  ▼                  ▼
 • Compile           • Unit Tests      • SAST (Sonar)     • DEV (auto)
 • Dependencies      • Integration     • DAST             • SIT (auto)
 • Docker Build      • ArchUnit        • Container Scan   • UAT (manual)
                     • Coverage        • License Check    • PROD (manual)
```

### Stage Details

#### 1. Build Stage

```yaml
# OpenShift Pipelines (Tekton)
- name: build
  tasks:
    - maven-build # mvn clean package -DskipTests
    - docker-build # Build container image
    - push-registry # Push to Quay.io
```

#### 2. Test Stage

```yaml
- name: test
  tasks:
    - unit-tests # mvn test
    - integration-tests # mvn verify -Pintegration
    - architecture-tests # mvn test -Dtest=ArchitectureTest
    - coverage-check # mvn jacoco:check (80% threshold)
```

#### 3. Security Scan Stage

```yaml
- name: security
  tasks:
    - sonarqube # Static code analysis
    - trivy # Container vulnerability scan
    - dependency-check # OWASP dependency check
    - license-check # License compliance
```

#### 4. Deploy Stage

| Environment | Trigger          | Approval        | Duration |
| ----------- | ---------------- | --------------- | -------- |
| DEV         | Auto on PR merge | None            | ~5 min   |
| SIT         | Auto after DEV   | None            | ~5 min   |
| UAT         | Manual           | QA Lead         | ~10 min  |
| PREPROD     | Manual           | Tech Lead       | ~10 min  |
| PROD        | Manual           | Release Manager | ~15 min  |

### Quality Gates

Pipeline **fails** if:

| Gate                     | Threshold |
| ------------------------ | --------- |
| Unit Test Coverage       | < 80%     |
| Branch Coverage          | < 70%     |
| SonarQube Quality Gate   | Failed    |
| Critical Vulnerabilities | > 0       |
| High Vulnerabilities     | > 5       |

---

## Code Review Guidelines

### What to Look For

1. **Correctness**: Does the code do what it's supposed to?
2. **Design**: Is the code well-structured? Follows patterns?
3. **Readability**: Can you understand it easily?
4. **Performance**: Any obvious performance issues?
5. **Security**: SQL injection, auth bypasses, data leaks?
6. **Tests**: Adequate test coverage?

### Review Etiquette

**For Authors:**

- Keep PRs small and focused
- Provide context in PR description
- Respond to feedback promptly
- Don't take feedback personally

**For Reviewers:**

- Be constructive and respectful
- Explain the "why" behind suggestions
- Approve when ready, don't block unnecessarily
- Use suggestions for minor fixes

### Review Comments

```
# Good
"Consider extracting this into a separate method for reusability.
Similar logic exists in TransferService.validateAmount()"

# Bad
"This is wrong"
"Fix this"
```

---

## Quick Reference

```bash
# Start new feature
git checkout main && git pull
git checkout -b feature/PAYU-123-description

# Commit changes
git add .
git commit -m "feat(scope): description"

# Push and create PR
git push -u origin feature/PAYU-123-description

# After PR merged, cleanup
git checkout main && git pull
git branch -d feature/PAYU-123-description
```

---

## Questions?

- **Backend Team**: backend-team@payu.id
- **DevOps Team**: devops-team@payu.id
- **Architecture**: architect@payu.id
