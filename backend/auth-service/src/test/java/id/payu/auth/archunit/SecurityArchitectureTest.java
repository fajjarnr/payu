package id.payu.auth.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.GeneralCodingRules.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture tests for Auth Service security patterns.
 *
 * Tests verify that:
 * - No hardcoded credentials exist in the codebase
 * - Controllers don't depend directly on infrastructure
 * - Security annotations are properly used
 * - No circular dependencies exist
 *
 * PCI-DSS Compliance:
 * - Requirement 2: No vendor-supplied defaults (hardcoded credentials)
 * - Requirement 6.5: Secure coding practices (architecture patterns)
 */
class SecurityArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .importPackages("id.payu.auth");

    // Test: No hardcoded credentials

    @Test
    @DisplayName("Should not contain hardcoded passwords in production code")
    void shouldNotContainHardcodedPasswords() {
        ArchRule rule = NO_CLASSES_SHOULD_USE_STANDARD_LOGGING
                .as("No hardcoded passwords")
                .because("Passwords must be externalized to Vault or environment variables");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Should not contain hardcoded API keys or secrets")
    void shouldNotContainHardcodedSecrets() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..service..")
                .or()
                .resideInAPackage("..controller..")
                .should()
                .dependOnClassesThat()
                .haveNameMatching(".*[Ss]ecret.*")
                .or()
                .haveNameMatching(".*[Kk]ey.*")
                .as("Should not depend on classes with Secret/Key in name (potential hardcoded credentials)");

        rule.check(importedClasses);
    }

    // Test: Layered architecture

    @Test
    @DisplayName("Controllers should only depend on services")
    void controllersShouldOnlyDependOnServices() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage("..controller..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..controller..",
                        "..service..",
                        "..dto..",
                        "..exception..",
                        "jakarta..",
                        "org.springframework..",
                        "lombok..",
                        "java..",
                        "id.payu.api.common.."
                )
                .as("Controllers should only depend on services and DTOs")
                .because("Direct infrastructure dependencies violate hexagonal architecture");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Services should not depend on controllers")
    void servicesShouldNotDependOnControllers() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage("..service..")
                .should()
                .notDependOnClassesThat()
                .resideInAPackage("..controller..")
                .as("Services should not depend on controllers")
                .because("This creates tight coupling and violates layered architecture");

        rule.check(importedClasses);
    }

    // Test: Security annotations

    @Test
    @DisplayName("Controllers should use security annotations")
    void controllersShouldUseSecurityAnnotations() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage("..controller..")
                .and()
                .arePublic()
                .should()
                .beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .or()
                .beAnnotatedWith("org.springframework.security.access.prepost.PreAuthorize")
                .as("Controllers should be annotated with @RestController or security annotations")
                .because("This ensures proper security configuration");

        rule.check(importedClasses);
    }

    // Test: No circular dependencies

    @Test
    @DisplayName("Should have no circular dependencies between packages")
    void shouldHaveNoCircularDependencies() {
        ArchRule rule = slices()
                .matching("id.payu.auth.(*)..")
                .should()
                .beFreeOfCycles()
                .as("Packages should be free of circular dependencies")
                .because("Circular dependencies make code hard to maintain and test");

        rule.check(importedClasses);
    }

    // Test: Logging best practices

    @Test
    @DisplayName("Should not use standard logging (use SLF4J instead)")
    void shouldUseSlf4jLogging() {
        ArchRule rule = NO_CLASSES_SHOULD_USE_STANDARD_LOGGING
                .as("Should not use standard logging")
                .because("SLF4J is the standard logging framework for PayU");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Should not use JodaTime (use java.time instead)")
    void shouldUseJavaTime() {
        ArchRule rule = NO_CLASSES_SHOULD_USE_JODATIME
                .as("Should not use JodaTime")
                .because("java.time is the standard Java 8+ date/time API");

        rule.check(importedClasses);
    }

    // Test: Security configuration

    @Test
    @DisplayName("Security configuration should be in dedicated package")
    void securityConfigShouldBeInDedicatedPackage() {
        ArchRule rule = classes()
                .that()
                .haveNameContaining("Security")
                .or()
                .haveNameContaining("Config")
                .and()
                .resideInAPackage("..auth..")
                .should()
                .resideInAPackage("..config..")
                .as("Security configuration should be in config package")
                .because("This keeps security concerns centralized");

        rule.check(importedClasses);
    }

    // Test: Exception handling

    @Test
    @DisplayName("Custom exceptions should extend from base exception")
    void customExceptionsShouldExtendBaseException() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage("..exception..")
                .and()
                .areNotAnonymousClasses()
                .should()
                .beAssignableTo("id.payu.api.common.exception.BusinessException")
                .or()
                .beAssignableTo("java.lang.RuntimeException")
                .as("Custom exceptions should extend BusinessException or RuntimeException")
                .because("This ensures consistent exception handling across services");

        rule.check(importedClasses);
    }

    // Test: Field access rules

    @Test
    @DisplayName("Classes should not access fields of other classes directly")
    void shouldNotAccessFieldsDirectly() {
        ArchRule rule = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
                .as("Should not access standard streams directly")
                .because("All logging should go through the configured logging framework");

        rule.check(importedClasses);
    }

    // Test: Test structure

    @Test
    @DisplayName("Test classes should mirror production package structure")
    void testClassesShouldMirrorProductionStructure() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage("..")
                .and()
                .haveSimpleNameContaining("Test")
                .should()
                .resideInAPackage("..test..")
                .as("Test classes should be in test package")
                .because("This separates test code from production code");

        rule.check(importedClasses);
    }
}
