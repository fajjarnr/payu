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
 * Architecture Tests for Promotion Service (Quarkus).
 *
 * Enforces:
 * - Layered architecture boundaries
 * - Naming conventions
 * - No field injection (follows Quarkus CDI best practices)
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
                    .layer("Domain").definedBy("..domain..")
                    .layer("DTO").definedBy("..dto..")

                    // Resource layer is entry point
                    .whereLayer("Resource").mayNotBeAccessedByAnyLayer()
                    // Service layer accessed by Resource
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Resource")
                    // Domain layer can be accessed by all business layers
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Service", "Resource", "DTO")
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
                            "org.eclipse.microprofile..",
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
                    .because("JAX-RS resource classes should follow naming convention")
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
                    .that().areAnnotatedWith("jakarta.ws.rs.Path")
                    .should().resideInAPackage("..resource..")
                    .because("JAX-RS resources should be in the resource package")
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
                    .areAssignableTo(io.quarkus.hibernate.orm.panache.PanacheEntityBase.class)
                    .because("Resources should use services for data access, not direct database access")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependency Injection Rules")
    class DependencyInjectionRules {

        @Test
        @DisplayName("should not use Spring annotations")
        void shouldNotUseSpringAnnotations() {
            noFields()
                    .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .because("This is a Quarkus service - use @Inject instead of @Autowired")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Panache Entity Rules")
    class PanacheEntityRules {

        @Test
        @DisplayName("Panache entities should be in domain package")
        void panacheEntitiesShouldBeInDomainPackage() {
            classes()
                    .that().areAssignableTo(io.quarkus.hibernate.orm.panache.PanacheEntityBase.class)
                    .should().resideInAPackage("..domain..")
                    .because("Panache entities are domain objects in this architecture")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("Panache entities should not be in resource or service packages")
        void panacheEntitiesShouldNotBeInResourceOrService() {
            noClasses()
                    .that().areAssignableTo(io.quarkus.hibernate.orm.panache.PanacheEntityBase.class)
                    .should().resideInAnyPackage("..resource..", "..service..")
                    .because("Panache entities belong in the domain layer")
                    .check(importedClasses);
        }
    }
}
