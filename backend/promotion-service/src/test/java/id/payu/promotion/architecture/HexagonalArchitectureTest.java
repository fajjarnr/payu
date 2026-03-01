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
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Extended Architecture Tests for Promotion Service - Hexagonal Architecture.
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
                    .check(importedClasses);
        }

        @Test
        @DisplayName("domain ports should only be implemented by adapters")
        void domainPortsShouldOnlyBeImplementedByAdapters() {
            classes()
                    .that().resideInAPackage("..domain.port..")
                    .and().areInterfaces()
                    .should().onlyBeImplemented()
                    .byClassesThat()
                    .resideInAPackage("..adapter..")
                    .because("Domain ports define interfaces that adapters implement")
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
                            "java..",
                            "javax..",
                            "org.springframework..",
                            "org.slf4j.."
                    )
                    .because("Application services orchestrate domain logic through ports")
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
                    .should().implement(interfaceWithPackage("..domain.port.out.."))
                    .because("Persistence adapters implement output ports")
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
                    .should().containNumberOfMethods(greaterThanOrEqualTo(3))
                    .because("Rich domain models should have behavior methods, not just getters/setters")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("value objects should be immutable")
        void valueObjectsShouldBeImmutable() {
            // This is a best-practice check - value objects like TransactionContext, PromoResult
            // should have final fields and no setters
            classes()
                    .that().haveSimpleNameEndingWith("Context")
                    .or().haveSimpleNameEndingWith("Result")
                    .and().resideInAPackage("..domain.model..")
                    .should().haveOnlyFinalFields()
                    .because("Value objects should be immutable")
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
                    .or().haveSimpleNameContaining("Cashback")
                    .should().haveSimpleNameEndingWith("Service")
                    .because("Services should have Service suffix")
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
                    .check(importedClasses);
        }
    }
}
