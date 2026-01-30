# Example Usage of ArchUnit Starter

## Example 1: Basic Usage (Extend Base Class)

File: `account-service/src/test/java/id/payu/account/archunit/AccountArchitectureTest.java`

```java
package id.payu.account.archunit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import id.payu.archunit.HexagonalArchitectureTest;

/**
 * Architecture tests for Account Service.
 *
 * Extends HexagonalArchitectureTest to automatically enforce all
 * hexagonal architecture rules.
 */
@AnalyzeClasses(
    packages = "id.payu.account",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class AccountArchitectureTest extends HexagonalArchitectureTest {
    // All rules from HexagonalArchitectureTest are automatically applied
}
```

## Example 2: Custom Rules + Base Rules

File: `wallet-service/src/test/java/id/payu/wallet/archunit/WalletArchitectureTest.java`

```java
package id.payu.wallet.archunit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import id.payu.archunit.HexagonalArchitectureTest;
import id.payu.archunit.HexagonalArchitectureRules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "id.payu.wallet")
class WalletArchitectureTest extends HexagonalArchitectureTest {

    // Base rules are inherited automatically

    // Add service-specific rules
    @ArchTest
    static final ArchRule walletDomainShouldHaveRichBehavior =
        classes()
            .that()
            .resideInAPackage("..domain.model..")
            .and()
            .haveSimpleNameContaining("Wallet")
            .should()
            .haveMethods("credit", "debit", "freeze")
            .because("Wallet domain entity should encapsulate business logic");

    @ArchTest
    static final ArchRule noDirectBigDecimalComparison =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .callMethod(BigDecimal.class, "equals", Object.class)
            .because("BigDecimal.equals() considers scale, use compareTo() instead");
}
```

## Example 3: Standalone Usage (Without Base Class)

File: `transaction-service/src/test/java/id/payu/transaction/archunit/TransactionArchitectureTest.java`

```java
package id.payu.transaction.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static id.payu.archunit.HexagonalArchitectureRules.*;

class TransactionArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("id.payu.transaction");
    }

    @Test
    void domainShouldBeIndependent() {
        domainShouldNotDependOnInfrastructure().check(importedClasses);
    }

    @Test
    void useCasesShouldBeTransactional() {
        stateModifyingUseCasesShouldBeTransactional().check(importedClasses);
    }

    @Test
    void repositoriesShouldReturnDomainObjects() {
        repositoriesShouldNotReturnEntities().check(importedClasses);
    }
}
```

## Example 4: Using All Rules

```java
package id.payu.payment.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;

import id.payu.archunit.HexagonalArchitectureRules;

class PaymentArchitectureTest {

    @Test
    void shouldFollowHexagonalArchitecture() {
        JavaClasses classes = new ClassFileImporter()
            .importPackages("id.payu.payment");

        // Apply all rules at once
        HexagonalArchitectureRules.allRules()
            .forEach(rule -> rule.check(classes));
    }
}
```

## Example 5: Custom Package Patterns

If your service uses different package naming:

```java
package id.payu.billing.archunit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(packages = "id.payu.billing")
class BillingArchitectureTest {

    // Custom rule with different package patterns
    @ArchTest
    static final ArchRule coreShouldNotDependOnInfra =
        ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("..core..")  // Custom domain package
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..infra..",
                "..adapter..",
                "org.springframework.."
            )
            .because("Core domain should be framework independent");
}
```

## Example 6: Freezing Violations

For gradual adoption, freeze existing violations:

```java
package id.payu.legacy.archunit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import static id.payu.archunit.HexagonalArchitectureRules.*;

@AnalyzeClasses(packages = "id.payu.legacy")
class LegacyArchitectureTest {

    // Freeze existing violations to prevent new ones
    @ArchTest
    static final ArchRule domainIndependence =
        FreezingArchRule.freeze(domainShouldNotDependOnInfrastructure());
}
```

## Running Tests

### Maven
```bash
# Run only architecture tests
mvn test -Dtest="*ArchitectureTest"

# Run all tests including architecture
mvn test
```

### Gradle
```bash
# Run only architecture tests
./gradlew test --tests "*ArchitectureTest"
```

### IDE
Right-click on the test class and run as JUnit test.

## CI/CD Integration

Add to your pipeline:

```yaml
# .github/workflows/architecture.yml
name: Architecture Check

on: [push, pull_request]

jobs:
  archunit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run ArchUnit Tests
        run: mvn test -Dtest="*ArchitectureTest"
```

## Common Issues

### Issue: "No classes found to check"
**Solution**: Ensure the package name in `@AnalyzeClasses` matches your base package.

### Issue: "False positives on @Transactional"
**Solution**: Use `stateModifyingUseCasesShouldBeTransactional()` instead of `useCasesShouldBeTransactional()`.

### Issue: "Domain depends on Lombok"
**Solution**: Either exclude Lombok from checks or accept it as a compile-only dependency.

```java
// Exclude Lombok from domain independence check
.domainShouldNotDependOnInfrastructure()
    .withPriority(Priority.LOW)  // Lower priority for this rule
    .check(classes);
```
