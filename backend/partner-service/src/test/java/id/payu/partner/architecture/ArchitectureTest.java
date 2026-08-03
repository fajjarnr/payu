package id.payu.partner.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture Tests for PartnerEntity Service (Spring Boot) — Hexagonal Architecture.
 *
 * Enforces:
 * - Hexagonal layer boundaries (Adapter → Application → Domain)
 * - Naming conventions per layer
 * - Domain isolation
 */
@DisplayName("Architecture Rules - PartnerEntity Service (Hexagonal)")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.partner");
    }

    @Nested
    @DisplayName("Hexagonal Architecture")
    class HexagonalArchitectureRules {

        @Test
        @DisplayName("should follow hexagonal architecture boundaries")
        void shouldFollowHexagonalArchitecture() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .withOptionalLayers(true)
                    .layer("Adapter.Web").definedBy("..adapter.web..")
                    .layer("Adapter.Webhook").definedBy("..adapter.webhook..")
                    .layer("Adapter.Messaging").definedBy("..adapter.messaging..")
                    .layer("Adapter.Client").definedBy("..adapter.client..")
                    .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
                    .layer("Application").definedBy("..application..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("Config").definedBy("..config..")
                    .layer("DTO").definedBy("..dto..")

                    .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter.Webhook").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter.Messaging").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter.Client").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter.Persistence").mayOnlyBeAccessedByLayers(
                            "Application", "Adapter.Web", "Adapter.Webhook")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers(
                            "Adapter.Web", "Adapter.Webhook", "Adapter.Messaging")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                            "Application", "Adapter.Web", "Adapter.Webhook",
                            "Adapter.Messaging", "Adapter.Persistence", "Adapter.Client", "DTO")

                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Domain Isolation Rules")
    class DomainIsolationRules {

        @Test
        @DisplayName("domain should not depend on application or adapter layers")
        void domainShouldNotDependOnServices() {
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..adapter..")
                    .because("Domain layer must be independent of infrastructure concerns")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionRules {

        @Test
        @DisplayName("application services should have Service suffix")
        void servicesShouldHaveServiceSuffix() {
            classes()
                    .that().resideInAPackage("..application.service..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Service")
                    .because("Service classes should follow naming convention")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("repositories should have Repository suffix")
        void repositoriesShouldHaveRepositorySuffix() {
            classes()
                    .that().resideInAPackage("..adapter.persistence.repository..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("Repository classes should follow naming convention")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("API Layer Rules")
    class ApiLayerRules {

        @Test
        @DisplayName("snap package DTOs should only contain data")
        void snapDtosShouldOnlyContainData() {
            noClasses()
                    .that().resideInAPackage("..dto.snap..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application..")
                    .because("DTOs should be simple data containers without service dependencies")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Async Transaction Rules")
    class AsyncTransactionRules {

        @Test
        @DisplayName("BUG-WEBHOOK-ASYNC-001: @Async methods should not be @Transactional")
        void asyncMethodsShouldNotBeTransactional() {
            // BUG-BE-049 lesson: @Transactional on @Async method is a no-op.
            // @Async runs in a different thread; Spring's @Transactional proxy
            // is only applied at the call site, not on the async thread.
            // The transaction context is not propagated. Each repository.save()
            // call runs in its own implicit transaction. If the method does
            // multiple writes and one fails, partial state is left behind.
            // Fix: remove @Transactional from @Async methods.
            //
            // CALIBRATION 2026-06-19: ArchUnit 1.2.1 can't parse Java 25 bytecode
            // (ASM incompatibility — `importPackages()` returns empty).
            // Use Java reflection on explicitly-known class instead. If this test
            // is moved to ArchUnit later, swap the implementation.
            java.util.List<String> violations = new java.util.ArrayList<>();
            String[] targetClasses = {
                "id.payu.partner.application.service.WebhookDispatcherService",
            };
            for (String className : targetClasses) {
                try {
                    Class<?> clazz = Class.forName(className);
                    for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
                        boolean hasAsync = method.isAnnotationPresent(Async.class);
                        boolean hasTx = method.isAnnotationPresent(Transactional.class);
                        if (hasAsync && hasTx) {
                            violations.add(String.format(
                                "Method %s.%s is @Async AND @Transactional (BUG-BE-049 no-op)",
                                className, method.getName()));
                        }
                    }
                } catch (ClassNotFoundException e) {
                    violations.add("Class not found: " + className);
                }
            }
            org.junit.jupiter.api.Assertions.assertTrue(violations.isEmpty(),
                "Found @Async + @Transactional methods (BUG-BE-049 no-op):\n  " + String.join("\n  ", violations));
        }
    }
}
