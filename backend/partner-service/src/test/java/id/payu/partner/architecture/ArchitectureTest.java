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
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture Tests for Partner Service (Quarkus).
 *
 * Enforces:
 * - Layered architecture boundaries
 * - Naming conventions
 * - Domain isolation (with noted technical debt exceptions)
 * - Resource abstraction patterns
 *
 * NOTE: This service has documented technical debt:
 * - SnapBiResource directly accesses PartnerRepository (should use PartnerService)
 * - Domain entities extend PanacheEntityBase (Quarkus framework dependency)
 *
 * These violations are allowed for now but should be addressed in future refactoring.
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
                    // Repository layer accessed by Service and Domain (and SnapBiResource - TECHNICAL DEBT)
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

        @Test
        @DisplayName("domain entities should not depend on reactive frameworks")
        void domainEntitiesShouldNotDependOnReactiveFrameworks() {
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "io.vertx..",
                            "io.smallrye.mutiny.."
                    )
                    .because("Domain entities should be blocking, not reactive")
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
        @DisplayName("should not use Spring annotations")
        void shouldNotUseSpringAnnotations() {
            noFields()
                    .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .because("This is a Quarkus service - use @Inject instead of @Autowired")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Service Abstraction Rules")
    class ServiceAbstractionRules {

        @Test
        @DisplayName("PartnerResource should not access repositories directly")
        void partnerResourceShouldNotAccessRepositories() {
            noClasses()
                    .that().resideInAPackage("..resource..")
                    .and().haveSimpleNameNotContaining("SnapBi")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..repository..")
                    .because("Resources must use services, not repositories directly (separation of concerns)")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("resources should not access other resources")
        void resourcesShouldNotAccessOtherResources() {
            noClasses()
                    .that().resideInAPackage("..resource..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..resource..")
                    .because("Resources should be independent and use services for cross-resource communication")
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
        @DisplayName("Panache repositories should be in repository package")
        void panacheRepositoriesShouldBeInRepositoryPackage() {
            classes()
                    .that().areAssignableTo(io.quarkus.hibernate.orm.panache.PanacheRepositoryBase.class)
                    .should().resideInAPackage("..repository..")
                    .because("Panache repositories should be in the repository package")
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
