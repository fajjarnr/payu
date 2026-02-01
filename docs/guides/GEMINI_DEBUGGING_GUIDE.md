# 🧠 GEMINI Debugging Knowledge Base

> **Systematic Debugging Patterns for PayU Platform**
> Last Updated: February 1, 2026
> **Core Principle**: NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST

---

## 📋 Table of Contents

1. [The Iron Law](#the-iron-law)
2. [Four-Phase Debugging Process](#four-phase-debugging-process)
3. [Platform-Specific Patterns](#platform-specific-patterns)
4. [Common Debugging Anti-Patterns](#common-debugging-anti-patterns)
5. [Case Studies from PayU](#case-studies-from-payu)

---

## 🔥 The Iron Law

```
NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST
```

**Violating this law means:**
- Wasting time on symptom fixes that don't work
- Creating new bugs while "fixing" old ones
- Building up technical debt through "quick patches"
- Missing architectural problems that need refactoring

---

## 🔄 Four-Phase Debugging Process

### Phase 1: Root Cause Investigation (DO THIS FIRST)

**Before ANY code changes:**

1. **Read Error Messages Completely**
   - Don't skip stack traces
   - Note error codes, line numbers, file paths
   - Copy the FULL error message

2. **Reproduce Consistently**
   - Can you trigger it every time?
   - What are the exact steps?
   - If not reproducible → gather more data, DON'T guess

3. **Check Recent Changes**
   ```bash
   git log --oneline -10
   git diff HEAD~1
   ```

4. **Gather Evidence (Multi-Component Systems)**

   For systems with multiple layers (CI → build → signing, API → service → database):

   ```bash
   # Log data at EACH component boundary
   echo "=== Layer 1: Secrets ==="
   echo "VAR: ${VAR:+SET}${VAR:-UNSET}"

   echo "=== Layer 2: Build ==="
   env | grep VAR

   echo "=== Layer 3: Runtime ==="
   # Add logging at component boundaries
   ```

5. **Trace Data Flow Backward**
   - Where does the bad value originate?
   - What called this with bad data?
   - Keep tracing UP to the source
   - Fix at source, not symptom

### Phase 2: Pattern Analysis

**Find the pattern before fixing:**

1. **Find Working Examples**
   - Locate similar working code
   - What works that's like what's broken?

2. **Compare Against References**
   - Read reference implementation COMPLETELY
   - Don't skim - read every line
   - Understand before applying

3. **Identify Differences**
   - List EVERY difference between working/broken
   - Don't assume "that can't matter"

4. **Understand Dependencies**
   - What config/environment is needed?
   - What assumptions does code make?

### Phase 3: Hypothesis and Testing

**Scientific method:**

1. **Form Single Hypothesis**
   - Write: "I think X causes Y because Z"
   - Be specific, not vague

2. **Test Minimally**
   - SMALLEST possible change
   - ONE variable at a time
   - Don't fix multiple things

3. **Verify Before Continuing**
   - Did it work? → Phase 4
   - Didn't work? → NEW hypothesis
   - DON'T add more fixes on top

### Phase 4: Implementation

**Fix root cause, not symptom:**

1. **Create Failing Test**
   - Simplest reproduction
   - Automated if possible
   - MUST have before fixing

2. **Implement Single Fix**
   - Address root cause only
   - ONE change at a time
   - No "while I'm here" improvements

3. **Verify Fix**
   - Test passes now?
   - No other tests broken?
   - Issue actually resolved?

4. **If 3+ Fixes Failed → Question Architecture**
   - Each fix reveals new problem?
   - Fixes require "massive refactoring"?
   - STOP - discuss fundamentals first

---

## 🎯 Platform-Specific Patterns

### ☕ Lombok Annotation Processing (Spring Boot 3.4)

**Symptom:** `cannot find symbol` for `get*`, `set*`, `builder()`, `log`

**Root Cause Checklist:**
1. Is service using `id.payu:payu-backend-parent`?
2. Check `maven-compiler-plugin` configuration
3. Ensure Lombok version `${lombok.version}` is defined
4. If works in IDE but fails in `mvn` → Maven config issue

**Fix Pattern:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**Break Glass Strategy (after 2 failed attempts):**
```java
// Abandon Lombok, implement manually
public class Entity {
    private String field;

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
}
```
*Rationale: Build Stability > Boilerplate Reduction*

### ☕ Enum Placement Best Practice

**Symptom:** JPA mapping errors, "cannot find symbol" for enum values

**Root Cause:** Inner class enums confuse annotation processors

**Fix:**
```java
// ❌ WRONG - Inner class
@Entity
public class Loan {
    public static enum Type { PERSONAL, MULTIGUNA }
}

// ✅ CORRECT - Top-level file
// domain/model/LoanType.java
public enum LoanType { PERSONAL, MULTIGUNA }
```

### 🦆 Quarkus → Spring Boot Migration

**Common Patterns:**

| Quarkus (Panache) | Spring Boot (JPA) | Fix |
|:---|:---|:---|
| `entity.field` (public) | `entity.setField()` | Add `@Data`, refactor usage |
| `Response` | `ResponseEntity<T>` | Update return types |
| `Uni<T>` | `T` or `Mono<T>` | Remove Mutiny, use blocking |
| `@QuarkusTest` | `@SpringBootTest` | Update test annotations |
| `@InjectMock` | `@Mock` | Update mock annotations |

### 🐳 Container/Podman Build Failures

**Pattern 1: Parent POM Resolution**
```dockerfile
# ❌ WRONG
COPY pom.xml ./

# ✅ CORRECT
COPY . .
```

**Pattern 2: Maven Build Hanging**
```dockerfile
# Use pre-built JAR strategy
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2
COPY target/*.jar /app/app.jar
```

**Pattern 3: UBI9 Conflicts**
```dockerfile
# ❌ WRONG
RUN microdnf install -y curl  # conflicts with curl-minimal

# ✅ CORRECT
# curl-minimal already available, use it directly
```

### 🎭 Playwright E2E Test Failures

**Common Patterns:**

| Symptom | Root Cause | Fix |
|:---|:---|:---|
| `strict mode violation` | Selector matches multiple elements | Use `.first()` or more specific selectors |
| `Timeout 5000ms exceeded` | Element not visible/clickable | Add explicit wait or `waitForSelector` |
| Text content mismatch | Translations vs hardcoded | Match actual translation content |
| Currency format mismatch | `Rp 50.000` vs `Rp50.000` | Use regex `\s*` for optional space |

**Fix Example:**
```typescript
// ❌ WRONG
await expect(page.getByText('Rp50.000.000')).toBeVisible();

// ✅ CORRECT
await expect(page.getByText(/Rp\s*50\.000\.000/).first()).toBeVisible();
```

### 🔐 Vault Configuration Issues

**Symptom:** `Could not resolve placeholder`

**Root Cause:** Wrong Vault import syntax

```yaml
# ❌ WRONG
spring:
  config:
    import: optional:vault://  # Invalid syntax

# ✅ CORRECT
spring:
  config:
    import: optional:vault  # Correct syntax
```

---

## 🚫 Common Debugging Anti-Patterns

| Anti-Pattern | Why It Fails | Correct Approach |
|:---|:---|:---|
| "Quick fix, investigate later" | Never investigated → accumulates debt | Always investigate first |
| "Just try changing X" | No understanding → random thrashing | Form hypothesis, test |
| "Multiple fixes at once" | Can't isolate what worked | One change at a time |
| "Skip test, manually verify" | Untested fixes don't stick | Create failing test first |
| "Reference too long, adapt pattern" | Partial understanding → bugs | Read reference completely |
| "One more fix attempt" (3+) | 3+ failures = architectural problem | Question fundamentals |
| "It's probably X, fix it" | Assuming vs knowing | Investigate to confirm |

---

## 📚 Case Studies from PayU

### Case 1: promotion-service Compilation Failures

**Symptom:** 100+ compilation errors after Quarkus → Spring Boot migration

**Root Cause Analysis:**
1. Quarkus annotations still present (`@ApplicationScoped`, `@Inject`)
2. Direct field access (Panache pattern) vs getter/setter calls
3. Missing Lombok configuration

**Solution:**
1. Replaced all Quarkus annotations with Spring equivalents
2. Refactored `entity.field` → `entity.setField()` throughout
3. Added proper `maven-compiler-plugin` configuration
4. Created 13 Spring Data JPA repositories

**Time with systematic approach:** ~2 hours
**Estimated time with random fixes:** 6-8 hours

### Case 2: lending-service Test Failures

**Symptom:** Tests failing with "cannot find symbol: RepaymentStatus"

**Root Cause Analysis:**
1. `RepaymentStatus` was inner class in `RepaymentSchedule`
2. Test imported `RepaymentSchedule.RepaymentStatus`
3. Inner enum confused annotation processor

**Solution:**
1. Extracted `RepaymentStatus` to top-level file
2. Updated all imports
3. All 27 tests passing

**Time with systematic approach:** ~15 minutes
**Estimated time with random fixes:** 1-2 hours

### Case 3: E2E Registration Flow Failures

**Symptom:** 25/27 tests failing (7% pass rate)

**Root Cause Analysis:**
1. Tests expected hardcoded Indonesian text
2. Actual implementation used `next-intl` translations
3. Mismatch: "Mulai Proses Verifikasi" vs "Lanjut ke Profil Data"
4. Currency format: "Rp50.000.000" vs "Rp 50.000.000"
5. Strict mode violations on common text

**Solution:**
1. Read actual translation file (`messages/id.json`)
2. Updated all test expectations to match translations
3. Fixed currency regex patterns: `/Rp\s*50\.000\.000/`
4. Added `.first()` for strict mode violations

**Result:** 23/23 passing (100% pass rate)
**Time with systematic approach:** ~1 hour

### Case 4: Container Build Hangs

**Symptom:** `mvn package` in container hanging 4+ hours

**Root Cause Analysis:**
1. Parallel builds (`-T 1C`) causing resource deadlock
2. Building from source in slow container environment

**Solution:**
Switched to pre-built JAR strategy:
```dockerfile
FROM ubi9/openjdk-21-runtime
COPY target/*.jar /app/app.jar
```

**Build Time:** 4+ hours → ~5 minutes

---

## 🎓 Learning Patterns

### Pattern Recognition Checklist

When encountering an issue, ask:

1. **Have I seen this error before?**
   - Check historical issues, similar code

2. **What changed recently?**
   - Git diff, recent commits, new dependencies

3. **Is this a known pattern?**
   - Lombok annotation processing
   - Quarkus → Spring migration
   - Container build issues

4. **What do working examples do?**
   - Find similar code that works
   - Compare line-by-line

### Debugging Commands Reference

```bash
# Maven/Build
mvn clean compile -pl <module> -am
mvn help:effective-pom
mvn dependency:tree

# Container
podman logs <container>
podman exec -it <container> bash

# E2E Tests
npx playwright test --project=chromium --reporter=list
npx playwright test --debug

# Git/Diff
git log --oneline -10
git diff HEAD~1
git blame <file> <line>
```

---

## 📖 Related Documentation

- **`.agent/skills/debugging-methodology/`** - Complete debugging skill
- **`REGISTRY.yaml`** - Available AI skills
- **`docs/adr/`** - Architecture Decision Records

---

*"The best debugger is still a clear mind and systematic approach."*
