# Git Hooks Documentation - PayU Mobile

This document describes the Git hooks configured for the PayU Mobile application to ensure code quality, consistency, and security.

---

## Overview

Git hooks are scripts that run automatically at certain points in the Git workflow. PayU Mobile uses [Husky](https://typicode.github.io/husky/) to manage Git hooks and enforce code quality standards.

### Installed Hooks

| Hook | Trigger | Purpose |
|------|---------|---------|
| `pre-commit` | Before each commit | Fast checks on staged files |
| `pre-push` | Before pushing to remote | Full test suite and security checks |
| `commit-msg` | After writing commit message | Validate commit message format |

---

## Pre-commit Hook

Runs automatically when you run `git commit`.

### What It Does

1. **Lint-staged** - Runs ESLint and Prettier on staged files only
2. **TypeScript Check** - Validates TypeScript types without emitting files
3. **Related Tests** - Runs Jest tests only for files changed in the commit

### Execution Flow

```
git commit -m "feat: add new feature"
    ↓
[pre-commit hook triggered]
    ↓
┌─────────────────────────────────────┐
│ 1. npx lint-staged                  │
│    - ESLint --fix on *.ts, *.tsx    │
│    - Prettier --write on all files  │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 2. npx tsc --noEmit                 │
│    - TypeScript type checking       │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 3. npx jest --findRelatedTests      │
│    - Tests for changed files only   │
└─────────────────────────────────────┘
    ↓
[Commit proceeds if all pass]
```

### Performance Optimizations

- Only staged files are linted (not the entire codebase)
- Only tests related to changed files are run
- TypeScript check is fast with `--noEmit` flag

---

## Pre-push Hook

Runs automatically when you run `git push`.

### What It Does

1. **Full Test Suite** - Runs all tests with coverage report
2. **TypeScript Check** - Validates all TypeScript types
3. **Secret Detection** - Scans for potential secrets/credentials
4. **Console.log Check** - Prevents accidental console.log statements in production code

### Execution Flow

```
git push origin main
    ↓
[pre-push hook triggered]
    ↓
┌─────────────────────────────────────┐
│ 1. npm test -- --coverage          │
│    - Full Jest test suite           │
│    - Coverage report generated      │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 2. npx tsc --noEmit                 │
│    - Full TypeScript type check     │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 3. Secret/Credential Detection      │
│    - Scans for API keys, passwords  │
│    - Checks for hardcoded secrets   │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 4. console.log Detection            │
│    - Prevents debug statements      │
└─────────────────────────────────────┘
    ↓
[Push proceeds if all pass]
```

---

## Commit Message Hook

Validates commit messages to ensure they follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Commit Message Format

```
<type>(<scope>): <subject>

[optional body]

[optional footer(s)]
```

### Types

| Type | Description |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only changes |
| `style` | Changes that don't affect code meaning (formatting, semicolons, etc) |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `perf` | Performance improvement |
| `test` | Adding or correcting tests |
| `build` | Changes to build system or dependencies |
| `ci` | Changes to CI/CD configuration |
| `chore` | Other changes that don't modify src or test files |
| `revert` | Reverts a previous commit |

### Examples

```bash
# Feature addition
feat(auth): add biometric authentication support

# Bug fix
fix(wallet): resolve balance calculation precision issue

# Documentation
docs(readme): update installation instructions for iOS

# Code refactoring
refactor(api): extract common error handling logic

# Test addition
test(hooks): add unit tests for useAuth hook
```

### Scope Guidelines

The scope is optional but recommended. Common scopes in PayU Mobile:

- `auth` - Authentication related
- `wallet` - Wallet/Balance features
- `transfer` - Money transfer features
- `cards` - Card management
- `profile` - User profile
- `api` - API integration
- `ui` - UI components
- `hooks` - Custom React hooks
- `store` - State management
- `utils` - Utility functions
- `config` - Configuration files
- `deps` - Dependencies

---

## Lint-staged Configuration

Lint-staged runs linters on git staged files only.

### Configuration Files

- **`.lintstagedrc.json`** - JSON configuration
- **`lint-staged.config.js`** - JavaScript configuration (alternative)

### File Patterns

| Pattern | Actions |
|---------|---------|
| `*.{ts,tsx}` | ESLint --fix, Prettier --write |
| `*.{json,md}` | Prettier --write |
| `*.{css,scss}` | Prettier --write |
| `*.{yml,yaml}` | Prettier --write |

---

## Troubleshooting

### Common Issues

#### 1. Hook Not Running

**Problem**: Hooks don't execute on commit/push.

**Solution**:
```bash
# Ensure Husky is installed
npm install

# Verify hooks are executable
chmod +x .husky/pre-commit
chmod +x .husky/pre-push
chmod +x .husky/commit-msg

# Reinstall Husky
npx husky install
```

#### 2. TypeScript Check Too Slow

**Problem**: `tsc --noEmit` takes too long.

**Solution**:
```bash
# Incremental type checking (already configured in tsconfig.json)
# Ensure "incremental": true is set in compilerOptions

# Skip type checking in pre-commit (not recommended)
# Edit .husky/pre-commit and comment out the tsc line
```

#### 3. Tests Failing in Pre-commit

**Problem**: Tests fail but you need to commit anyway.

**Solution**:
```bash
# Skip pre-commit hooks (not recommended for regular use)
git commit -m "feat: wip" --no-verify

# Better: Fix the failing tests
npm test -- --watch
```

#### 4. Commit Message Rejected

**Problem**: Commit message doesn't match Conventional Commits format.

**Solution**:
```bash
# Use correct format
git commit -m "feat(scope): description"

# Examples:
git commit -m "feat(auth): add login form validation"
git commit -m "fix(wallet): correct balance display"
git commit -m "docs: update API documentation"
```

#### 5. ESLint/Prettier Conflicts

**Problem**: ESLint and Prettier rules conflict.

**Solution**:
```bash
# Ensure eslint-config-prettier is installed
npm install --save-dev eslint-config-prettier

# Update .eslintrc.js to extend prettier last
module.exports = {
  extends: [
    'expo',
    'plugin:react/recommended',
    'prettier'  // Add this last
  ],
  // ...
};
```

---

## Bypassing Hooks (Emergency Only)

In emergency situations, you can bypass Git hooks using the `--no-verify` flag.

### ⚠️ Warning

Bypassing hooks should be **extremely rare** and only used in true emergencies. Bypassed commits may:
- Contain broken code
- Have incorrect formatting
- Include security vulnerabilities
- Have non-standard commit messages

### Commands

```bash
# Bypass pre-commit hook
git commit -m "fix: urgent hotfix" --no-verify

# Bypass pre-push hook
git push origin main --no-verify

# Bypass both (emergency only)
git commit -m "fix: critical bug" --no-verify && git push --no-verify
```

### When to Bypass

Acceptable scenarios:
- Critical production hotfix at 3 AM
- Reverting a commit that broke production
- Fixing a broken CI/CD pipeline

Unacceptable scenarios:
- "I'm in a hurry"
- "The hooks are annoying"
- "My code works on my machine"

---

## Configuration Files

### package.json Scripts

```json
{
  "scripts": {
    "prepare": "husky",
    "lint-staged": "lint-staged"
  }
}
```

### Required Dependencies

```json
{
  "devDependencies": {
    "husky": "^9.0.0",
    "lint-staged": "^15.0.0",
    "prettier": "^3.0.0"
  }
}
```

---

## Best Practices

1. **Don't Bypass Hooks** - Fix issues instead of bypassing
2. **Keep Commits Small** - Smaller commits = faster hooks
3. **Use Meaningful Scopes** - Helps with changelog generation
4. **Write Good Messages** - Be descriptive but concise
5. **Run Tests Locally** - Don't rely solely on CI/CD

---

## CI/CD Integration

The same checks run in CI/CD pipeline. If hooks pass locally, CI/CD should pass too.

### GitHub Actions Example

```yaml
name: Quality Checks
on: [push, pull_request]
jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci
      - run: npm run lint
      - run: npx tsc --noEmit
      - run: npm test -- --coverage
```

---

## Additional Resources

- [Husky Documentation](https://typicode.github.io/husky/)
- [Lint-staged Documentation](https://github.com/okonet/lint-staged)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [ESLint Documentation](https://eslint.org/docs/latest/)
- [Prettier Documentation](https://prettier.io/docs/en/)

---

## Support

For issues with Git hooks:
1. Check this troubleshooting guide
2. Review hook logs in terminal output
3. Contact the PayU Mobile team

---

*Last Updated: January 2026*
