package id.payu.billing.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Architecture Tests for Billing Service (Spring Boot) — Hexagonal Architecture.
 *
 * Enforces:
 * - Ports & Adapters (hexagonal) layer boundaries
 * - Domain isolation from frameworks
 * - Naming conventions for each layer
 */
@DisplayName("Architecture Rules - Billing Service (Hexagonal)")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.billing");
        assertFalse(importedClasses.isEmpty(), "ArchUnit must import billing classes");
    }

    @Nested
    @DisplayName("Hexagonal Architecture")
    class HexagonalArchitectureRules {

        @Test
        @DisplayName("should follow hexagonal (ports & adapters) architecture pattern")
        void shouldFollowHexagonalArchitecture() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .withOptionalLayers(true)
                    // Adapter layers (driving / driven)
                    .layer("Adapter.Web").definedBy("..adapter.web..")
                    .layer("Adapter.Client").definedBy("..adapter.client..")
                    .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
                    .layer("Infrastructure.Persistence").definedBy("..infrastructure.persistence..")
                    .layer("Adapter.Messaging").definedBy("..adapter.messaging..")
                    // Application layer (use-case orchestration)
                    .layer("Application").definedBy("..application..")
                    // Domain layer (model + ports)
                    .layer("Domain").definedBy("..domain..")
                    // Cross-cutting
                    .layer("Config").definedBy("..config..")
                    .layer("DTO").definedBy("..dto..")
                    .layer("Exception").definedBy("..exception..")

                    // Driving adapters (Web) are entry points — not accessed by other layers
                    .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
                    // Application layer accessed only by driving adapters
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter.Web")
                    // Domain layer is the core — accessed by Application + Adapters
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                            "Application",
                            "Adapter.Web", "Adapter.Client", "Adapter.Persistence", "Adapter.Messaging",
                            "DTO", "Exception"
                    )
                    // Driven adapters implement domain ports
                    .whereLayer("Adapter.Client").mayOnlyBeAccessedByLayers("Application")
                    .whereLayer("Adapter.Persistence").mayOnlyBeAccessedByLayers("Application")
                    .whereLayer("Infrastructure.Persistence").mayOnlyBeAccessedByLayers("Adapter.Persistence")
                    .whereLayer("Adapter.Messaging").mayOnlyBeAccessedByLayers("Application")

                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Domain Isolation Rules")
    class DomainIsolationRules {

        @Test
        @DisplayName("domain should not depend on adapters, config, or Spring")
        void domainShouldNotDependOnInfrastructure() {
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter..",
                            "..infrastructure..",
                            "..config..",
                            "org.springframework.."
                    )
                    .because("Domain layer must be independent of infrastructure concerns (Hexagonal rule)")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("DTOs should not depend on application services")
        void dtosShouldNotDependOnServices() {
            noClasses()
                    .that().resideInAPackage("..dto..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application..")
                    .because("DTOs should be data transfer objects without business logic dependencies")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionRules {

        @Test
        @DisplayName("web adapter classes should have Controller suffix")
        void webAdaptersShouldHaveControllerSuffix() {
            classes()
                    .that().resideInAPackage("..adapter.web..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("REST controller classes should follow naming convention")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

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
        @DisplayName("persistence adapters should have Adapter or Repository suffix")
        void persistenceAdaptersShouldFollowNaming() {
            classes()
                    .that().resideInAPackage("..adapter.persistence..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Adapter")
                    .orShould().haveSimpleNameEndingWith("Repository")
                    .because("Persistence adapter classes should follow naming convention")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("ports should be interfaces")
        void portsShouldBeInterfaces() {
            classes()
                    .that().resideInAPackage("..domain.port..")
                    .and().areTopLevelClasses()
                    .should().beInterfaces()
                    .because("Ports define contracts and must be interfaces")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependency Injection Rules")
    class DependencyInjectionRules {

        @Test
        @DisplayName("should use constructor injection (Spring best practice)")
        void shouldPreferConstructorInjection() {
            noFields()
                    .should().beAnnotatedWith("jakarta.inject.Inject")
                    .because("This is a Spring Boot service — avoid using Quarkus @Inject")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Persistence Rules")
    class PersistenceRules {

        @Test
        @DisplayName("Repositories should be in adapter.persistence package")
        void repositoriesShouldBeInPersistencePackage() {
            classes()
                    .that().areAssignableTo(org.springframework.data.repository.Repository.class)
                    .should().resideInAPackage("..adapter.persistence..")
                    .because("Repositories are driven adapter infrastructure concerns")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }
}
