package id.payu.partner.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture Tests for Partner Service (Spring Boot) — Hexagonal Architecture.
 *
 * Enforces:
 * - Hexagonal layer boundaries (Adapter → Application → Domain)
 * - Naming conventions per layer
 * - Domain isolation
 */
@DisplayName("Architecture Rules - Partner Service (Hexagonal)")
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
                    .layer("Adapter.Web").definedBy("..adapter.web..")
                    .layer("Adapter.Webhook").definedBy("..adapter.webhook..")
                    .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
                    .layer("Application").definedBy("..application..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("Config").definedBy("..config..")
                    .layer("DTO").definedBy("..dto..")

                    .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter.Webhook").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter.Persistence").mayOnlyBeAccessedByLayers(
                            "Application", "Adapter.Web", "Adapter.Webhook")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers(
                            "Adapter.Web", "Adapter.Webhook")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                            "Application", "Adapter.Web", "Adapter.Webhook",
                            "Adapter.Persistence", "DTO")

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
                    .check(importedClasses);
        }
    }
}
