package id.payu.promotion.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture Tests for PromotionEntity Service (Spring Boot) — Hexagonal Architecture.
 */
@DisplayName("Architecture Rules - PromotionEntity Service (Hexagonal)")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.promotion");
        Assumptions.assumeFalse(importedClasses.isEmpty(), "Skipping ArchUnit tests on Java 25 due to class import limitations");
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
                    .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
                    .layer("Application").definedBy("..application..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("Config").definedBy("..config..")
                    .layer("DTO").definedBy("..dto..")

                    .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter.Persistence").mayOnlyBeAccessedByLayers("Application")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter.Web")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                            "Application", "Adapter.Web", "Adapter.Persistence", "DTO")
                    .whereLayer("DTO").mayOnlyBeAccessedByLayers("Adapter.Web", "Application")

                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Domain Isolation Rules")
    class DomainIsolationRules {

        @Test
        @DisplayName("domain should not depend on adapter or application layers")
        void domainShouldNotDependOnExternalFrameworks() {
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter..",
                            "..application..",
                            "org.springframework.web.."
                    )
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
    @DisplayName("Controller Rules")
    class ControllerRules {

        @Test
        @DisplayName("controllers should only be in adapter.web package")
        void controllersShouldOnlyBeInWebPackage() {
            classes()
                    .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should().resideInAPackage("..adapter.web..")
                    .because("Spring controllers should be in the adapter.web package")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Repository Rules")
    class RepositoryRules {

        @Test
        @DisplayName("Repositories should be in adapter.persistence package")
        void repositoriesShouldBeInPersistencePackage() {
            classes()
                    .that().areAssignableTo(org.springframework.data.repository.Repository.class)
                    .should().resideInAPackage("..adapter.persistence..")
                    .because("Repositories belong in the adapter.persistence layer")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }
}
