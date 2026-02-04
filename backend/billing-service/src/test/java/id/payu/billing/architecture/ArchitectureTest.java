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

/**
 * Architecture Tests for Billing Service (Spring Boot).
 * 
 * Enforces:
 * - Layered architecture boundaries
 * - Naming conventions
 * - Domain isolation
 */
@DisplayName("Architecture Rules - Billing Service")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.billing");
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
                    .layer("Client").definedBy("..client..")
                    .layer("DTO").definedBy("..dto..")
                    
                    // Resource layer is entry point
                    .whereLayer("Resource").mayNotBeAccessedByAnyLayer()
                    // Service layer accessed by Resource
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Resource")
                    // Domain layer can be accessed by all business layers
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Service", "Resource", "DTO")
                    // Client layer for external calls (wallet-service)
                    .whereLayer("Client").mayOnlyBeAccessedByLayers("Service")
                    
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
                            "..client..",
                            "org.springframework.."
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
        @DisplayName("clients should have Client suffix")
        void clientsShouldHaveClientSuffix() {
            classes()
                    .that().resideInAPackage("..client..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Client")
                    .because("REST client interfaces should follow naming convention")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependency Injection Rules")
    class DependencyInjectionRules {

        @Test
        @DisplayName("should use constructor injection (Spring best practice)")
        void shouldPreferConstructorInjection() {
            // This is a relaxed check, but encourages constructor injection
            // We'll just check that @jakarta.inject.Inject is NOT used (Quarkus/CDI)
            noFields()
                    .should().beAnnotatedWith("jakarta.inject.Inject")
                    .because("This is a Spring Boot service - avoid using Quarkus @Inject")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Persistence Rules")
    class PersistenceRules {
 
        @Test
        @DisplayName("Repositories should be in repository package")
        void repositoriesShouldBeInRepositoryPackage() {
            classes()
                    .that().areAssignableTo(org.springframework.data.repository.Repository.class)
                    .should().resideInAPackage("..repository..")
                    .because("Repositories are infrastructure concerns for data access")
                    .check(importedClasses);
        }
    }
}
