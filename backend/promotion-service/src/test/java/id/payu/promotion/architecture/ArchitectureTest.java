package id.payu.promotion.architecture;

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
 * Architecture Tests for Promotion Service (Spring Boot).
 *
 * Enforces:
 * - Layered architecture boundaries
 * - Naming conventions
 * - Spring dependency injection best practices
 * - Domain isolation
 */
@DisplayName("Architecture Rules - Promotion Service")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.promotion");
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
                    .layer("Repository").definedBy("..repository..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("DTO").definedBy("..dto..")

                    // Resource layer is entry point
                    .whereLayer("Resource").mayNotBeAccessedByAnyLayer()
                    // Service layer accessed by Resource
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Resource")
                    // Repository layer accessed by Service
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                    // Domain layer can be accessed by all business layers
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Service", "Resource", "DTO", "Repository")
                    // DTOs can be accessed by Resource and Service
                    .whereLayer("DTO").mayOnlyBeAccessedByLayers("Resource", "Service")

                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Domain Isolation Rules")
    class DomainIsolationRules {

        @Test
        @DisplayName("domain should not depend on external frameworks except JPA")
        void domainShouldNotDependOnExternalFrameworks() {
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..resource..",
                            "org.springframework.web..",
                            "jakarta.ws.rs.."
                    )
                    .because("Domain layer must be independent of infrastructure concerns")
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

        @Test
        @DisplayName("domain should not depend on DTOs")
        void domainShouldNotDependOnDTOs() {
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..dto..")
                    .because("Domain entities should be independent of DTOs")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionRules {

        @Test
        @DisplayName("resources should have Resource suffix")
        void resourcesShouldHaveResourceSuffix() {
            classes()
                    .that().resideInAPackage("..resource..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Resource")
                    .because("Spring controller resource classes should follow naming convention")
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
    }

    @Nested
    @DisplayName("Controller Rules")
    class ControllerRules {

        @Test
        @DisplayName("controllers should only be in resource package")
        void controllersShouldOnlyBeInResourcePackage() {
            classes()
                    .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should().resideInAPackage("..resource..")
                    .because("Spring controllers should be in the resource package")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("resources should not access database directly")
        void resourcesShouldNotAccessDatabaseDirectly() {
            noClasses()
                    .that().resideInAPackage("..resource..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("jakarta.persistence..")
                    .orShould().dependOnClassesThat()
                    .resideInAPackage("..repository..")
                    .because("Resources should use services for data access, not direct database or repository access")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependency Injection Rules")
    class DependencyInjectionRules {

        @Test
        @DisplayName("should not use Inject annotations")
        void shouldNotUseInjectAnnotations() {
            noFields()
                    .should().beAnnotatedWith("jakarta.inject.Inject")
                    .because("This is a Spring Boot service - use @Autowired instead of @Inject")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Repository Rules")
    class RepositoryRules {

        @Test
        @DisplayName("Repositories should be in repository package")
        void repositoriesShouldBeInRepositoryPackage() {
            classes()
                    .that().areAssignableTo(org.springframework.data.repository.Repository.class)
                    .should().resideInAPackage("..repository..")
                    .because("Repositories belong in the repository layer")
                    .check(importedClasses);
        }
    }
}
