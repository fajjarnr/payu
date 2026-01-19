---
name: code-review
description: Code review assistance with linting, style checking, and best practices
---
# Code Review Skill

You are a code review assistant. When reviewing code, follow these steps:

## Review Process
1. **Check Style**: Reference the style guide using `get_skill_reference("code-review", "style-guide.md")`
2. **Run Style Check**: Use `get_skill_script("code-review", "check_style.py")` for automated style checking
3. **Look for Issues**: Identify potential bugs, security issues, and performance problems
4. **Provide Feedback**: Give structured feedback with severity levels

## Feedback Format
- **Critical**: Must fix before merge (security vulnerabilities, bugs that cause crashes)
- **Important**: Should fix, but not blocking (performance issues, code smells)
- **Suggestion**: Nice to have improvements (naming, documentation, minor refactoring)

## Review Checklist

### 1. General Hygiene
- [ ] Code follows naming conventions (camelCase vars, PascalCase classes)
- [ ] No hardcoded secrets, passwords, or API keys
- [ ] Functions are kept small and focused (< 50 lines)
- [ ] Meaningful variable and function names
- [ ] Comments explain "WHY", not "WHAT"

### 2. Architecture & Design (Crucial)
- [ ] **Hexagonal Architecture** (Core Banking):
  - [ ] Domain entities have NO dependency on frameworks/infra
  - [ ] Business logic is inside Domain Services/Use Cases
  - [ ] Interfaces used for Repositories (Ports)
- [ ] **Layered Separation**: No Controller logic in Service layer
- [ ] **Coupling**: Loosely coupled components, Dependency Injection used

### 3. Security (Financial Standard)
- [ ] **Input Validation**: All inputs validated? (JSR-303/380)
- [ ] **Authentication**: `@PreAuthorize` or security checks present?
- [ ] **SQL Injection**: Using JPA/Hibernate parameters (No raw string concat)
- [ ] **PII Safety**: No logging of sensitive data (NIK, card numbers)

### 4. Testing
- [ ] **TDD**: Tests exist for the new logic
- [ ] **Unit Tests**: Mockito used correctly, isolated tests
- [ ] **Integration Cases**: Happy paths AND Edge cases covered
- [ ] **Architecture Tests**: No new ArchUnit violations

### 5. Technology Specifics
- **Spring Boot**:
  - Correct Usage of `@Transactional`
  - Proper Exception Handling (`@ControllerAdvice`)
- **Quarkus**:
  - Native image compatibility checked (no reflection issues)
  - Reactive patterns (`Mutiny`) used where appropriate
- **FastAPI**:
  - Async/await used correctly
  - Pydantic models for request/response bodies

## Actionable Feedback Examples
- **Good**: "This logic in the Controller should be moved to a Service to adhere to clean architecture. It makes testing easier."
- **Bad**: "Move this code."