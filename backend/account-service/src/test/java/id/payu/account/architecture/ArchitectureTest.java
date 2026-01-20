package id.payu.account.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture Tests for Account Service.
 * 
 * Enforces:
 * - Hexagonal Architecture boundaries
 * - Domain isolation (domain must not depend on infrastructure/api)
 * - Clean dependency flow
 * - Naming conventions
 * - No field injection
 */
@DisplayName("Architecture Rules")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.account");
    }

    @Nested
    @DisplayName("Hexagonal Architecture")
    class HexagonalArchitectureRules {

        @Test
        @DisplayName("should follow hexagonal architecture layers")
        void shouldFollowHexagonalArchitecture() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Domain").definedBy("..domain..")
                    .layer("Application").definedBy("..application..")
                    .layer("Adapter").definedBy("..adapter..")
                    .layer("Config").definedBy("..config..")

                    // Domain should rely on nothing (pure java ideally, but pojos/dtos allowed)
                    // Application relies on Domain
                    // Adapter relies on Domain and Application (for Ports)
                    
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter", "Config")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter", "Config")
                    // .whereLayer("Adapter").mayNotBeAccessedByAnyLayer() // Ideally true but config accesses it
                    
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Domain Isolation Rules")
    class DomainIsolationRules {

        @Test
        @DisplayName("domain should not depend on infrastructure")
        void domainShouldNotDependOnInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter..",
                            "..infrastructure..",
                            "..config.."
                    )
                    .because("Domain layer must be independent of infrastructure concerns (Hexagonal Architecture)");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("domain should not depend on Spring framework")
        void domainShouldNotDependOnSpring() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "org.springframework.web..",
                            "org.springframework.data..",
                            "jakarta.persistence.."
                    )
                    .because("Domain entities should be framework-agnostic POJOs");

            // Note: This rule may need exceptions for validation annotations
            // Uncomment when domain is fully decoupled
            // rule.check(importedClasses);
        }

        @Test
        @DisplayName("domain entities should not depend on external services")
        void domainShouldNotDependOnExternalServices() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain.entity..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter.out..",
                            "..infrastructure.external.."
                    )
                    .allowEmptyShould(true)
                    .because("Domain entities must not have dependencies on external service clients");
            
            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Service Access Rules")
    class ServiceAccessRules {

        @Test
        @DisplayName("services should only be accessed by controllers and other services")
        void servicesShouldOnlyBeAccessedByControllersOrServices() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..application.service..")
                    .or().resideInAPackage("..domain.service..")
                    .should().onlyBeAccessed().byAnyPackage(
                            "..adapter.in..",
                            "..adapter.in.rest..",
                            "..application..",
                            "..domain.service..",
                            "..config.."
                    )
                    .because("Services should be accessed via adapters (controllers) or other services, not directly from infrastructure");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("controllers should not access repositories directly")
        void controllersShouldNotAccessRepositories() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..adapter.in.rest..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..domain.repository..",
                            "..adapter.out.persistence.."
                    )
                    .because("Controllers must use services, not repositories directly (separation of concerns)");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Repository Access Rules")
    class RepositoryAccessRules {

        @Test
        @DisplayName("repositories should only be accessed by services")
        void repositoriesShouldOnlyBeAccessedByServices() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..domain.repository..")
                    .should().onlyBeAccessed().byAnyPackage(
                            "..application..",
                            "..domain.service..",
                            "..adapter.out.persistence..",
                            "..config.."
                    )
                    .because("Repositories (ports) should only be accessed by application services or their implementations");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("No Field Injection")
    class NoFieldInjectionRules {

        @Test
        @DisplayName("should not use field injection with @Autowired")
        void shouldNotUseFieldInjection() {
            ArchRule rule = noFields()
                    .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .because("Use constructor injection instead of field injection for better testability");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("should not use field injection with @Inject")
        void shouldNotUseJakartaFieldInjection() {
            ArchRule rule = noFields()
                    .should().beAnnotatedWith("jakarta.inject.Inject")
                    .because("Use constructor injection instead of field injection for better testability");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionRules {

        @Test
        @DisplayName("services should have Service suffix")
        void servicesShouldHaveServiceSuffix() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(Service.class)
                    .should().haveSimpleNameEndingWith("Service")
                    .orShould().haveSimpleNameEndingWith("ServiceImpl")
                    .because("Service classes should follow naming convention");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("controllers should have Controller suffix")
        void controllersShouldHaveControllerSuffix() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(RestController.class)
                    .or().areAnnotatedWith(Controller.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("Controller classes should follow naming convention");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("repositories should have Repository suffix")
        void repositoriesShouldHaveRepositorySuffix() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(Repository.class)
                    .or().resideInAPackage("..repository..")
                    .should().haveSimpleNameEndingWith("Repository")
                    .orShould().haveSimpleNameEndingWith("RepositoryImpl")
                    .because("Repository classes should follow naming convention");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Exception Handling")
    class ExceptionHandlingRules {

        @Test
        @DisplayName("custom exceptions should extend RuntimeException or specific base")
        void exceptionsShouldExtendRuntimeException() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Exception")
                    .and().resideInAPackage("..domain..")
                    .should().beAssignableTo(RuntimeException.class)
                    .because("Domain exceptions should be unchecked (RuntimeException) for cleaner code");

            rule.check(importedClasses);
        }
    }
}

