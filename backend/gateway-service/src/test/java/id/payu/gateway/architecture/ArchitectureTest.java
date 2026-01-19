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
 * Architecture Tests for Gateway Service (Quarkus).
 * 
 * Enforces:
 * - Layered architecture boundaries (Filter -> Resource)
 * - Naming conventions
 * - No Spring dependencies (Quarkus only)
 * - Jakarta imports only (no javax)
 */
@DisplayName("Architecture Rules - Gateway Service")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.gateway");
    }

    @Nested
    @DisplayName("Layered Architecture")
    class LayeredArchitectureRules {

        @Test
        @DisplayName("should follow API gateway layered architecture")
        void shouldFollowLayeredArchitecture() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Filter").definedBy("..filter..")
                    .layer("Resource").definedBy("..resource..")
                    .layer("Config").definedBy("..config..")
                    .layer("DTO").definedBy("..dto..")
                    
                    // Filters are request interceptors
                    .whereLayer("Filter").mayNotBeAccessedByAnyLayer()
                    // Resources are API endpoints
                    .whereLayer("Resource").mayNotBeAccessedByAnyLayer()
                    // Config can be accessed by all layers
                    .whereLayer("Config").mayOnlyBeAccessedByLayers("Filter", "Resource")
                    
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
                    .that().resideInAPackage("..filter..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Filter")
                    .because("Request filter classes should follow naming convention")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("resources should have Resource or Handler suffix")
        void resourcesShouldHaveResourceSuffix() {
            classes()
                    .that().resideInAPackage("..resource..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Resource")
                        .orShould().haveSimpleNameEndingWith("Handler")
                    .because("JAX-RS resource classes should follow naming convention")
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
                    .check(importedClasses);
        }

        @Test
        @DisplayName("should not use Spring Autowired")
        void shouldNotUseSpringAutowired() {
            noFields()
                    .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .because("This is a Quarkus service - use @Inject instead of @Autowired")
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
                    .check(importedClasses);
        }

        @Test
        @DisplayName("should use jakarta.inject instead of javax.inject")
        void shouldUseJakartaInject() {
            noClasses()
                    .should().dependOnClassesThat()
                    .resideInAPackage("javax.inject..")
                    .because("Use jakarta.inject instead of javax.inject for Quarkus 3.x")
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
                    .that().resideInAPackage("..filter..")
                    .and().areNotInterfaces()
                    .should().beAnnotatedWith(jakarta.enterprise.context.ApplicationScoped.class)
                        .orShould().beAnnotatedWith(jakarta.enterprise.context.RequestScoped.class)
                    .because("Filters should have proper CDI scope")
                    .check(importedClasses);
        }
    }
}
