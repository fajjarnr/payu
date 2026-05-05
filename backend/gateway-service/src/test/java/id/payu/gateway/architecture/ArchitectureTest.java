package id.payu.gateway.architecture;

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
 * Architecture Tests for Gateway Service (Quarkus) — Hexagonal Architecture.
 *
 * Enforces:
 * - Hexagonal layer boundaries (Adapter → Application → Domain)
 * - Naming conventions per layer
 * - No Spring dependencies (Quarkus only)
 * - Jakarta imports only (no javax)
 */
@DisplayName("Architecture Rules - Gateway Service (Hexagonal)")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.gateway");
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
                    .layer("Adapter.Filter").definedBy("..adapter.filter..")
                    .layer("Application").definedBy("..application..")
                    .layer("Config").definedBy("..config..")
                    .layer("DTO").definedBy("..dto..")

                    // Adapter layers are outermost — nothing accesses them
                    .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter.Filter").mayNotBeAccessedByAnyLayer()
                    // Application layer is accessed by adapters only
                    .whereLayer("Application").mayOnlyBeAccessedByLayers(
                            "Adapter.Web", "Adapter.Filter")
                    // Config is infrastructure — used by all layers
                    .whereLayer("Config").mayOnlyBeAccessedByLayers(
                            "Adapter.Web", "Adapter.Filter", "Application")

                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionRules {

        @Test
        @DisplayName("filters should have Filter suffix")
        void filtersShouldHaveFilterSuffix() {
            classes()
                    .that().resideInAPackage("..adapter.filter..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Filter")
                    .because("Request filter classes should follow naming convention")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("web resources should have Resource or Handler suffix")
        void resourcesShouldHaveResourceSuffix() {
            classes()
                    .that().resideInAPackage("..adapter.web..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Resource")
                        .orShould().haveSimpleNameEndingWith("Handler")
                    .because("JAX-RS resource classes should follow naming convention")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("application services should have Service or Registry suffix")
        void servicesShouldHaveServiceSuffix() {
            classes()
                    .that().resideInAPackage("..application.service..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Service")
                        .orShould().haveSimpleNameEndingWith("Registry")
                    .because("Application service classes should follow naming convention")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Quarkus-Only Rules")
    class QuarkusOnlyRules {

        @Test
        @DisplayName("should not use Spring annotations")
        void shouldNotUseSpringAnnotations() {
            noClasses()
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..")
                    .because("This is a Quarkus service - do not use Spring dependencies")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("should not use Spring Autowired")
        void shouldNotUseSpringAutowired() {
            noFields()
                    .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .because("This is a Quarkus service - use @Inject instead of @Autowired")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Jakarta Migration Rules")
    class JakartaMigrationRules {

        @Test
        @DisplayName("should use jakarta.ws.rs instead of javax.ws.rs")
        void shouldUseJakartaWsRs() {
            noClasses()
                    .should().dependOnClassesThat()
                    .resideInAPackage("javax.ws.rs..")
                    .because("Use jakarta.ws.rs instead of javax.ws.rs for Quarkus 3.x")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("should use jakarta.inject instead of javax.inject")
        void shouldUseJakartaInject() {
            noClasses()
                    .should().dependOnClassesThat()
                    .resideInAPackage("javax.inject..")
                    .because("Use jakarta.inject instead of javax.inject for Quarkus 3.x")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Filter Implementation Rules")
    class FilterImplementationRules {

        @Test
        @DisplayName("filters should be ApplicationScoped or RequestScoped")
        void filtersShouldHaveProperScope() {
            classes()
                    .that().resideInAPackage("..adapter.filter..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().beAnnotatedWith(jakarta.enterprise.context.ApplicationScoped.class)
                        .orShould().beAnnotatedWith(jakarta.enterprise.context.RequestScoped.class)
                    .because("Filters should have proper CDI scope")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }
}
