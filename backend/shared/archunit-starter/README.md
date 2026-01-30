# ArchUnit Starter for PayU

Shared ArchUnit module for enforcing Hexagonal Architecture (Ports and Adapters) across PayU microservices.

## Features

- **Pre-defined Architecture Rules**: Ready-to-use rules for hexagonal architecture enforcement
- **Base Test Class**: Extendable base class for easy integration
- **Custom Conditions & Predicates**: Reusable components for advanced rule definitions
- **Spring Boot Integration**: Seamless integration with Spring Boot 3.4+

## Installation

Add dependency to your service's `pom.xml`:

```xml
<dependency>
    <groupId>id.payu</groupId>
    <artifactId>archunit-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

## Quick Start

### Option 1: Extend Base Test Class (Recommended)

```java
package id.payu.account.archunit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import id.payu.archunit.HexagonalArchitectureTest;

@AnalyzeClasses(packages = "id.payu.account")
class AccountArchitectureTest extends HexagonalArchitectureTest {
    // All hexagonal rules are automatically applied
}
```

### Option 2: Use Individual Rules

```java
package id.payu.account.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;

import static id.payu.archunit.HexagonalArchitectureRules.*;

class CustomArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("id.payu.account");

    @Test
    void domainShouldBeIndependent() {
        domainShouldNotDependOnInfrastructure().check(classes);
    }

    @Test
    void useCasesShouldBeTransactional() {
        useCasesShouldBeTransactional().check(classes);
    }
}
```

### Option 3: Use @ArchTest with JUnit 5

```java
package id.payu.account.archunit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import id.payu.archunit.HexagonalArchitectureRules;

@AnalyzeClasses(packages = "id.payu.account")
class ArchUnitTest {

    @ArchTest
    static final ArchRule domainIndependence =
        HexagonalArchitectureRules.domainShouldNotDependOnInfrastructure();

    @ArchTest
    static final ArchRule transactionalUseCases =
        HexagonalArchitectureRules.useCasesShouldBeTransactional();
}
```

## Available Rules

### Core Hexagonal Rules

| Rule | Description |
|------|-------------|
| `domainShouldNotDependOnInfrastructure()` | Domain layer must not depend on Spring, JPA, or infrastructure |
| `domainShouldNotDependOnApplication()` | Domain must not depend on application layer |
| `applicationShouldOnlyAccessRepositoriesThroughPorts()` | Application services must use port interfaces |
| `adaptersShouldNotLeakIntoDomain()` | Domain must not reference adapter classes |
| `useCasesShouldBeTransactional()` | All use case methods must be @Transactional |
| `stateModifyingUseCasesShouldBeTransactional()` | Only state-modifying methods need @Transactional |
| `repositoriesShouldNotReturnEntities()` | Repository ports return domain objects, not JPA entities |
| `portsShouldBeInterfaces()` | Ports must be interfaces |
| `jpaEntitiesShouldBeInInfrastructure()` | JPA entities belong in infrastructure layer |

### Usage Example

```java
// Get all rules as a list
List<ArchRule> allRules = HexagonalArchitectureRules.allRules();
allRules.forEach(rule -> rule.check(importedClasses));
```

## Package Structure Convention

The rules assume the following package structure:

```
id.payu.{service}
├── domain
│   ├── model          # Domain entities, value objects
│   ├── service        # Domain services
│   └── port
│       ├── in         # Input ports (use case interfaces)
│       └── out        # Output ports (repository interfaces)
├── application
│   ├── service        # Application services
│   ├── dto            # DTOs
│   └── usecase        # Use case implementations
├── infrastructure
│   ├── persistence    # JPA entities, repository implementations
│   ├── web            # REST controllers
│   └── external       # External service adapters
└── adapter
    └── ...            # Additional adapters
```

## Customization

### Custom Package Patterns

```java
// Override default package patterns
HexagonalArchitectureRules.PackagePatterns.DOMAIN = "..core..";
HexagonalArchitectureRules.PackagePatterns.APPLICATION = "..app..";
```

### Custom Rules

```java
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

ArchRule customRule = classes()
    .that()
    .resideInAPackage("..domain..")
    .should()
    .notBeAnnotatedWith("lombok.Data")
    .because("Domain entities should use constructors, not Lombok @Data");
```

## Configuration

Create `archunit.properties` in `src/test/resources`:

```properties
# Resolve missing dependencies from classpath
archunit.resolveMissingDependenciesFromClassPath=true

# Fail on empty should
archunit.failOnEmptyShould=true

# Enable parallel analysis
archunit.enableParallelAnalysis=true
```

## Best Practices

1. **Run ArchUnit tests in CI/CD**: Add to your Maven/Gradle build pipeline
2. **Fail build on violation**: Set `archunit.failOnEmptyShould=true`
3. **Document exceptions**: Use `@ArchIgnore` with reason for intentional violations
4. **Regular review**: Update rules as architecture evolves

## References

- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [PayU Architecture Guidelines](../../docs/architecture/ARCHITECTURE.md)
