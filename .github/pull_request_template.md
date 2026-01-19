## Pull Request

### 📋 Description

<!-- Describe what this PR does and why -->

**Ticket**: [PAYU-XXX](https://jira.payu.id/browse/PAYU-XXX)

### 🔄 Type of Change

<!-- Mark with [x] -->

- [ ] 🆕 Feature - New functionality
- [ ] 🐛 Bug Fix - Non-breaking fix
- [ ] 🔧 Refactor - Code improvement without behavior change
- [ ] 📚 Documentation - Docs only
- [ ] 🧪 Test - Adding or updating tests
- [ ] 🚨 Hotfix - Critical production fix
- [ ] ⬆️ Dependency - Dependency updates

### 📁 Affected Services

<!-- Mark all that apply -->

- [ ] account-service
- [ ] auth-service
- [ ] transaction-service
- [ ] wallet-service
- [ ] billing-service
- [ ] notification-service
- [ ] gateway-service
- [ ] infrastructure
- [ ] simulators

---

## ✅ Checklist

### Code Quality

- [ ] Code compiles without errors
- [ ] Code follows project style guidelines
- [ ] No new lint warnings/errors
- [ ] Self-reviewed my own code

### Testing

- [ ] Unit tests added/updated
- [ ] All tests pass locally (`mvn test`)
- [ ] Integration tests pass (if applicable)
- [ ] Coverage meets threshold (80% line, 70% branch)

### Documentation

- [ ] CHANGELOG.md updated (if user-facing change)
- [ ] API docs updated (if applicable)
- [ ] README updated (if setup changes)

### Security

- [ ] No hardcoded secrets/credentials
- [ ] No PII logged
- [ ] Input validation implemented
- [ ] Auth/authz properly enforced

### Database (if applicable)

- [ ] Migration script added
- [ ] Migration tested locally
- [ ] Rollback script provided

---

## 🧪 Testing Instructions

<!-- How can reviewers test this change? -->

```bash
# Example commands
cd backend/account-service
mvn test -Dtest=NewFeatureTest
```

### Manual Testing Steps

1.
2.
3.

---

## 📸 Screenshots (if applicable)

<!-- Add screenshots for UI changes -->

---

## 🔗 Related PRs

<!-- Link to related PRs if any -->

-

---

## 📝 Additional Notes

<!-- Any additional context, dependencies, or deployment notes -->

---

<!--
Review Reminder for Reviewers:
1. Check code correctness and design
2. Verify test coverage
3. Look for security issues
4. Ensure documentation is updated
-->
