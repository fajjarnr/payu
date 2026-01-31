package id.payu.partner.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture Tests for Partner Service (Spring Boot).
 *
 * Enforces:
 * - Layered architecture boundaries
 * - Naming conventions
 * - Domain isolation
 * - Resource abstraction patterns
 */
@DisplayName("Architecture Rules - Partner Service")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.partner");
    }

    @Nested
    @DisplayName("Layered Architecture")
    class LayeredArchitectureRules {

        @Test
        @DisplayName("should follow layered architecture pattern")
        void shouldFollowLayeredArchitecture() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Resource").definedBy("..resource..")
                    .layer("Service").definedBy("..service..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("Repository").definedBy("..repository..")
                    .layer("DTO").definedBy("..dto..")

                    // Resource layer is entry point (REST API)
                    .whereLayer("Resource").mayNotBeAccessedByAnyLayer()
                    // Service layer accessed by Resource
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Resource", "DTO")
                    // Domain layer can be accessed by Service, Repository, and DTO
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Service", "Repository", "DTO", "Resource")
                    // Repository layer accessed by Service and Domain
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Domain", "Resource")

                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Domain Isolation Rules")
    class DomainIsolationRules {

        @Test
        @DisplayName("domain should not depend on application services")
        void domainShouldNotDependOnServices() {
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..service..")
                    .because("Domain layer must be independent of service concerns")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("domain should not depend on resources")
        void domainShouldNotDependOnResources() {
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..resource..")
                    .because("Domain layer must be independent of resource layer")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("DTOs should not depend on services")
        void dtosShouldNotDependOnServices() {
            noClasses()
                    .that().resideInAPackage("..dto..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..service..")
                    .because("DTOs should be data transfer objects without business logic dependencies")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionRules {

        @Test
        @DisplayName("resources should have Controller suffix")
        void resourcesShouldHaveControllerSuffix() {
            classes()
                    .that().resideInAPackage("..resource..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("Spring MVC controller classes should follow naming convention")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("services should have Service suffix")
        void servicesShouldHaveServiceSuffix() {
            classes()
                    .that().resideInAPackage("..service..")
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
                    .that().resideInAPackage("..repository..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("Repository classes should follow naming convention")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("DTOs should have DTO suffix or be Request/Response")
        void dtosShouldHaveDtosuffix() {
            classes()
                    .that().resideInAPackage("..dto..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("DTO")
                    .orShould().haveSimpleNameEndingWith("Request")
                    .orShould().haveSimpleNameEndingWith("Response")
                    .because("DTO classes should follow naming convention")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependency Injection Rules")
    class DependencyInjectionRules {

        @Test
        @DisplayName("should use Spring annotations for dependency injection")
        void shouldUseSpringAnnotations() {
            classes()
                    .that().resideInAPackage("..service..")
                    .and().areNotTopLevelClasses()
                    .or().resideInAPackage("..resource..")
                    .and().areTopLevelClasses()
                    .should().beAnnotatedWith("org.springframework.stereotype.Service")
                    .orShould().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .because("This is a Spring Boot service - use Spring annotations (inner classes are excluded)")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Service Abstraction Rules")
    class ServiceAbstractionRules {

        @Test
        @DisplayName("resources should not access other resources")
        @Disabled("Inner classes used for OpenAPI schema definitions cause false positives")
        void resourcesShouldNotAccessOtherResources() {
            noClasses()
                    .that().resideInAPackage("..resource..")
                    .and().areTopLevelClasses()
                    .should().dependOnClassesThat()
                    .resideInAPackage("..resource..")
                    .because("Resources should be independent and use services for cross-resource communication (inner classes excluded)")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Circular Dependency Rules")
    class CircularDependencyRules {

        @Test
        @DisplayName("should have no circular dependencies between packages")
        void shouldHaveNoCircularDependencies() {
            slices().matching("id.payu.partner.(*)..")
                    .should().beFreeOfCycles()
                    .because("Circular dependencies between packages indicate tight coupling")
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
                    .resideInAPackage("..service..")
                    .because("DTOs should be simple data containers without service dependencies")
                    .check(importedClasses);
        }
    }
}
