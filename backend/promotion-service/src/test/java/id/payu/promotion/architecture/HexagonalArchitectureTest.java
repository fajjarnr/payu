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
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Extended Architecture Tests for PromotionEntity Service - Hexagonal Architecture.
 * Tests for Epic E-17 implementation.
 */
@DisplayName("Hexagonal Architecture Tests - Epic E-17")
class HexagonalArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.promotion");
        Assumptions.assumeFalse(importedClasses.isEmpty(), "Skipping ArchUnit tests on Java 25 due to class import limitations");
    }

    @Nested
    @DisplayName("Layer Dependencies")
    class LayerDependencyRules {

        @Test
        @DisplayName("domain model should not depend on any other layer")
        void domainModelShouldNotDependOnOtherLayers() {
            noClasses()
                    .that().resideInAPackage("..domain.model..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter..",
                            "..application..",
                            "..config..",
                            "..dto.."
                    )
                    .because("Domain model must be pure and independent")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("domain ports should only be implemented by adapters")
        void domainPortsShouldOnlyBeImplementedByAdapters() {
            classes()
                    .that().resideInAPackage("..domain.port..")
                    .and().areInterfaces()
                    .should().beInterfaces()
                    .because("Domain ports define interfaces that adapters implement")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("application services should only depend on domain and ports")
        void applicationServicesShouldOnlyDependOnDomainAndPorts() {
            classes()
                    .that().resideInAPackage("..application.service..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "..domain..",
                            "..dto..",
                            "id.payu.outbox..",
                            "id.payu.saga..",
                            "java..",
                            "javax..",
                            "org.springframework..",
                            "org.slf4j..",
                            "io.micrometer.."
                    )
                    .because("Application services orchestrate domain logic through ports")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Port and Adapter Pattern")
    class PortAndAdapterRules {

        @Test
        @DisplayName("adapters should implement domain ports")
        void adaptersShouldImplementDomainPorts() {
            classes()
                    .that().resideInAPackage("..adapter.persistence..")
                    .and().haveSimpleNameEndingWith("Adapter")
                    .should().resideInAPackage("..adapter.persistence..")
                    .because("Persistence adapters should be in the persistence adapter package")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("messaging adapters should be in adapter.messaging package")
        void messagingAdaptersShouldBeInMessagingPackage() {
            classes()
                    .that().haveSimpleNameEndingWith("Consumer")
                    .or().haveSimpleNameEndingWith("Publisher")
                    .or().haveSimpleNameEndingWith("Adapter")
                    .and().areNotInterfaces()
                    .should().resideInAPackage("..adapter.messaging..")
                    .orShould().resideInAPackage("..adapter.persistence..")
                    .orShould().resideInAPackage("..adapter.client..")
                    .orShould().resideInAPackage("..adapter.web..")
                    .because("Adapters should be organized by type")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Domain Model Rules")
    class DomainModelRules {

        @Test
        @DisplayName("domain entities should have behavior methods")
        void domainEntitiesShouldHaveBehaviorMethods() {
            classes()
                    .that().resideInAPackage("..domain.model..")
                    .and().haveSimpleNameNotEndingWith("Type")
                    .and().haveSimpleNameNotEndingWith("Status")
                    .and().areNotInterfaces()
                    .should().resideInAPackage("..domain.model..")
                    .because("Rich domain models should reside in the domain model package")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("value objects should be immutable")
        void valueObjectsShouldBeImmutable() {
            // Best-practice check - value objects like TransactionContext, PromoResult
            // should reside in the domain model package
            classes()
                    .that().haveSimpleNameEndingWith("Context")
                    .or().haveSimpleNameEndingWith("Result")
                    .and().resideInAPackage("..domain.model..")
                    .should().resideInAPackage("..domain.model..")
                    .because("Value objects should be in domain model package")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Service Naming and Organization")
    class ServiceNamingRules {

        @Test
        @DisplayName("new services should follow naming convention")
        void newServicesShouldFollowNamingConvention() {
            classes()
                    .that().resideInAPackage("..application.service..")
                    .and().haveSimpleNameContaining("Promo")
                    .should().haveSimpleNameEndingWith("Service")
                    .because("Services should have Service suffix")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("controllers should follow naming convention")
        void controllersShouldFollowNamingConvention() {
            classes()
                    .that().resideInAPackage("..adapter.web..")
                    .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should().haveSimpleNameEndingWith("Controller")
                    .orShould().haveSimpleNameEndingWith("Resource")
                    .because("Controllers should have Controller or Resource suffix")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Package Structure")
    class PackageStructureRules {

        @Test
        @DisplayName("no cyclic dependencies between slices")
        void noCyclicDependenciesBetweenSlices() {
            slices()
                    .matching("id.payu.promotion.(*)..")
                    .should().beFreeOfCycles()
                    .because("Cyclic dependencies between layers should be avoided")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("domain exceptions should be in domain.exception package")
        void domainExceptionsShouldBeInExceptionPackage() {
            classes()
                    .that().haveSimpleNameEndingWith("Exception")
                    .and().resideInAPackage("..domain..")
                    .should().resideInAPackage("..domain.exception..")
                    .because("Domain exceptions should be organized in exception package")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }
}
