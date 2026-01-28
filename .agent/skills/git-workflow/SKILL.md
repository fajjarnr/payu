---
name: git-workflow
description: Expert in Git branching strategies, Conventional Commits, and Pull Request (PR) standards for the PayU Digital Banking Platform.
---

# PayU Git Workflow & PR Skill

You are an expert in Git best practices and Pull Request management for the **PayU Digital Banking Platform**. You ensure that every piece of code merged into `main` follows strict enterprise standards for traceability, clarity, and quality.

## 🚀 Branching Strategy

- **Feature**: `feat/<short-description>` or `feat/<issue-id>-<description>`
- **Bug Fix**: `fix/<short-description>`
- **Hotfix**: `hotfix/<short-description>` (Directly from `main`)
- **Refactor**: `refactor/<short-description>`

---

## 📝 Conventional Commits & PR Titles

PayU uses the Conventional Commits standard for clear history and automatic changelog generation.

### PR Title Format: `<type>(<scope>): <summary>`

#### 1. Types
| Type | Description |
| :--- | :--- |
| `feat` | New feature (Adds business value) |
| `fix` | Bug fix |
| `perf` | Performance improvement |
| `test` | Adding or fixing tests |
| `docs` | Documentation changes only |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `chore` | Maintenance, dependencies, build tasks |
| `ci` | CI/CD configuration changes |

#### 2. Scopes (Examples)
- `shared`: Changes in `backend/shared/`
- `auth`: `auth-service`
- `txn`: `transaction-service`
- `ui`: `frontend/web-app`
- `mobile`: `frontend/mobile`
- `infra`: Infrastructure or Helm charts
- `api-docs`: `frontend/developer-docs`

#### 3. Summary Rules
- Use imperative present tense ("Add", not "Added").
- Capitalize the first letter.
- No period at the end.

---

## 🏗️ Pull Request Standards

### PR Creation Workflow
1. **Analyze**: Verify changes with `git status` and `git diff --stat`.
2. **Push**: `git push -u origin HEAD`.
3. **Create**: Use `gh pr create` with a descriptive body.

### PR Body Guidelines
Every PR must include:
1. **Summary**: A clear description of *what* was changed and *why*.
2. **Testing**: Steps to verify the change (Testing logs or screenshots).
3. **Related Tasks**: Link to Jira/Issue tracker (e.g., `Closes #123`).
4. **Checklist**:
    - [ ] PR title follows Conventional Commits.
    - [ ] Unit/Integration tests included.
    - [ ] Documentation updated (via `@docs-specialist`).
    - [ ] Security review performed (if PII or Auth is involved).

---

## 🛠️ Git Commands Cheat Sheet

```bash
# Preview changes before PR
git log origin/main..HEAD --oneline

# Squashing commits before merge
git rebase -i main

# Creating PR via CLI (PayU Template)
gh pr create --title "feat(txn): add QRIS payment support" --body "## Summary\nImplemented QRIS payment flow...\n\n## Testing\n- Passed integration test QRISPaymentFlowTest"
```

---
*Last Updated: January 2026*
