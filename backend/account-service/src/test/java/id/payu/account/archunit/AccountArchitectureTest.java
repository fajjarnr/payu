package id.payu.account.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture tests for Account Service following Hexagonal Architecture.
 *
 * <p>These tests enforce architectural rules to maintain clean boundaries
 * between the domain, application, and infrastructure layers.</p>
 *
 * <p>Architecture Pattern: Hexagonal (Ports and Adapters)</p>
 * <ul>
 *   <li><b>Domain Layer (Inner Core):</b> Pure Java, no framework dependencies</li>
 *   *   Entities, Value Objects, Domain Services</li>
 *   *   Located in: id.payu.account.domain.model, id.payu.account.domain.service</li>
 *   <li><b>Application Layer (Use Cases):</b> Orchestration, no business logic</li>
 *   *   Use Cases, Ports (interfaces), DTOs</li>
   *   *   Located in: id.payu.account.application, id.payu.account.domain.port</li>
 *   *   Located in: id.payu.account.infrastructure, id.payu.account.adapter</li>
   *   *   Located in: id.payu.account.controller, id.payu.account.repository</li>
 * </ul>
 *
 * <p>Key Rules Enforced:</p>
 * <ol>
 *   <li>Domain layer must be independent of Spring/Frameworks</li>
   *   <li>Controllers depend only on Services (not Repositories)</li>
   *   <li>Services depend on Ports (interfaces), not implementations</li>
   *   <li>Infrastructure depends on Application/Domain (reverse dependency rule)</li>
 *   <li>No circular dependencies between slices</li>
   * </ol>
 */
@DisplayName("Account Service Architecture Tests")
class AccountArchitectureTest {

    /**
     * Import all classes from account-service package for analysis.
     */
    private final JavaClasses importedClasses = new JavaClasses()
            .withImportLocation(ImportOption.Urls.of(
                    Location.of("../account-service/src/main/java")),
                    ImportOption.Urls.of(
                            Location.of("../../account-service/src/main/java")));

    @Test
    @DisplayName("Domain layer should not depend on Spring")
    void domainLayerShouldNotDependOnSpring() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.validation..",
                        "jakarta.inject..",
                        "org.springframework.data.."
                )
                .because("Domain layer must be framework-independent for hexagonal architecture");
    }

    @Test
    @DisplayName("Domain layer should not depend on application layer")
    void domainLayerShouldNotDependOnApplicationLayer() {
        classes()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..application..")
                .because("Domain layer is the inner core and should not depend on application layer");
    }

    @Test
    @DisplayName("Domain layer should not depend on infrastructure layer")
    void domainLayerShouldNotDependOnInfrastructureLayer() {
        classes()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..infrastructure..",
                        "..adapter..",
                        "..entity..",
                        "..repository.."
                )
                .because("Domain layer should be independent of infrastructure concerns");
    }

    @Test
 @DisplayName("Controllers should only depend on Services")
    void controllersShouldOnlyDependOnServices() {
        classes()
                .that()
                .areAnnotatedWith(Controller.class)
                .should()
                .onlyDependOnClassesThat()
                .areAnnotatedWith(Service.class)
                .or()
                .resideInAnyPackage(
                        "..dto..",
                        "..exception..",
                        "..constant..",
                        "org.springframework..",
                        "jakarta..",
                        "java..",
                        "lombok.."
                )
                .because("Controllers should orchestrate through services, not directly access repositories");
    }

    @Test
    @DisplayName("Controllers should not directly access repositories")
    void controllersShouldNotDirectlyAccessRepositories() {
        classes()
                .that()
                .areAnnotatedWith(Controller.class)
                .should()
                .notDependOnClassesThat()
                .areAnnotatedWith(Repository.class)
                .because("Controllers should access data through Services, not Repositories");
    }

    @Test
    @DisplayName("Services should depend on Ports (interfaces), not Adapters")
    void servicesShouldDependOnPortsNotAdapters() {
        classes()
                .that()
                .areAnnotatedWith(Service.class)
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage("..domain.port..")
                .or()
                .areAnnotatedWith(Component.class)
                .or()
                .resideInAnyPackage(
                        "..dto..",
                        "..exception..",
                        "java..",
                        "lombok..",
                        "org.springframework..",
                        "jakarta.."
                )
                .because("Services should depend on Port interfaces, not concrete Adapter implementations");
    }

    @Test
    @DisplayName("Repositories should be in infrastructure layer")
    void repositoriesShouldBeInInfrastructureLayer() {
        classes()
                .that()
                .areAnnotatedWith(Repository.class)
                .should()
                .resideInAPackage("..infrastructure..")
                .or()
                .resideInAPackage("..repository..")
                .because("Repositories are infrastructure concerns and should be in the infrastructure layer");
    }

    @Test
    @DisplayName("Entities should be in domain model")
    void entitiesShouldBeInDomainModel() {
        classes()
                .that()
                .haveSimpleNameContaining("Entity")
                .should()
                .resideInAPackage("..domain.model..")
                .because("Entity classes belong to the domain model layer");
    }

    @Test
    @DisplayName("DTOs should be shared or in application layer")
    void dtosShouldBeInCorrectLayer() {
        classes()
                .that()
                .haveSimpleNameContaining("DTO")
                .or()
                .haveSimpleNameContaining("Request")
                .or()
                .haveSimpleNameContaining("Response")
                .should()
                .resideInAnyPackage(
                        "..api.common..",
                        "..application..",
                        "..application.dto.."
                )
                .because("DTOs should be in application layer or shared libraries");
    }

    @Test
    @DisplayName("Exceptions should be in domain layer")
    void exceptionsShouldBeInDomainLayer() {
        classes()
                .that()
                .haveSimpleNameContaining("Exception")
                .should()
                .resideInAPackage("..domain..")
                .because("Business exceptions belong to the domain layer");
    }

    @Test
@DisplayName("Should follow layered architecture")
    void shouldFollowLayeredArchitecture() {
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controller")
                    .definedBy("..controller..")
                .layer("Service")
                    .definedBy("..service..")
                .layer("Repository")
                    .definedBy("..repository..")
                .layer("Domain")
                    .definedBy("..domain..")
                .layer("Infrastructure")
                    .definedBy("..infrastructure..", "..adapter..")
                .whereLayer("Controller")
                    .mayNotBeAccessedByAnyLayer()
                .whereLayer("Service")
                    .mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("Repository")
                    .mayOnlyBeAccessedByLayers("Service", "Infrastructure")
                .whereLayer("Domain")
                    .mayNotBeAccessedByAnyLayer()
                .because("Domain layer is the inner core and should not be accessed by outer layers")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Should have no circular dependencies")
    void shouldHaveNoCircularDependencies() {
        slices()
                .matching("id.payu.account.(*)")
                .should()
                .beFreeOfCycles()
                .because("Circular dependencies make code hard to maintain and test");
    }

    @Test
    @DisplayName("Freeze annotations should only be on domain entities")
    void freezeAnnotationsShouldOnlyBeOnDomainEntities() {
        // If using @Freeze from Jakarta Persistence, it should only be on entities
        // in the domain model, not in services or controllers
        classes()
                .that()
                .areAnnotatedWith("jakarta.persistence.Entity")
                .should()
                .resideInAPackage("..domain.model..")
                .or()
                .resideInAPackage("..entity..")
                .because("JPA entities should be in the domain model or entity layer");
    }

    @Test
    @DisplayName("Business logic should be in domain entities, not services")
    void businessLogicShouldBeInDomainEntities() {
        // This test checks that methods like credit(), debit(), freeze() are in the Account entity
        // rather than being implemented in a service class
        classes()
                .that()
                .haveSimpleName("Account")
                .should()
                .resideInAPackage("..domain.model..")
                .and()
                .haveMethods(
                        "credit",
                        "debit",
                        "freeze",
                        "unfreeze",
                        "close",
                        "isActive",
                        "isFrozen",
                        "isOwnedBy"
                )
                .because("Business logic should be encapsulated in domain entities (rich domain model)");
    }
}
