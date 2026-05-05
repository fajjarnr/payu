package id.payu.notification.architecture;

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
 * Architecture Tests for Notification Service (Quarkus) — Hexagonal Architecture.
 *
 * Enforces:
 * - Hexagonal layer boundaries (Adapter → Application → Domain)
 * - Naming conventions per layer
 * - Domain isolation
 * - Sender abstraction patterns
 */
@DisplayName("Architecture Rules - Notification Service (Hexagonal)")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.notification");
    }

    @Nested
    @DisplayName("Hexagonal Architecture")
    class HexagonalArchitectureRules {

        @Test
        @DisplayName("should follow hexagonal architecture boundaries")
        void shouldFollowHexagonalArchitecture() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .withOptionalLayers(true)
                    .layer("Adapter.Web").definedBy("..adapter.web..")
                    .layer("Adapter.Messaging").definedBy("..adapter.messaging..")
                    .layer("Adapter.Sender").definedBy("..adapter.sender..")
                    .layer("Application").definedBy("..application..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("Config").definedBy("..config..")
                    .layer("DTO").definedBy("..dto..")

                    .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter.Messaging").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter.Sender").mayOnlyBeAccessedByLayers("Application")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers(
                            "Adapter.Web", "Adapter.Messaging")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                            "Application", "Adapter.Web", "Adapter.Messaging",
                            "Adapter.Sender", "DTO")

                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Domain Isolation Rules")
    class DomainIsolationRules {

        @Test
        @DisplayName("domain should not depend on infrastructure")
        void domainShouldNotDependOnInfrastructure() {
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter..",
                            "..application..",
                            "org.eclipse.microprofile.."
                    )
                    .because("Domain layer must be independent of infrastructure concerns")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionRules {

        @Test
        @DisplayName("web resources should have Resource suffix")
        void resourcesShouldHaveResourceSuffix() {
            classes()
                    .that().resideInAPackage("..adapter.web..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Resource")
                    .because("JAX-RS resource classes should follow naming convention")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("application services should have Service suffix")
        void servicesShouldHaveServiceSuffix() {
            classes()
                    .that().resideInAPackage("..application.service..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Service")
                    .because("Service classes should follow naming convention")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("senders should have Sender suffix")
        void sendersShouldHaveSenderSuffix() {
            classes()
                    .that().resideInAPackage("..adapter.sender..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Sender")
                    .because("Sender classes should follow naming convention")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("consumers should have Consumer suffix")
        void consumersShouldHaveConsumerSuffix() {
            classes()
                    .that().resideInAPackage("..adapter.messaging..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Consumer")
                    .because("Kafka consumer classes should follow naming convention")
                    .allowEmptyShould(true)
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
                    .allowEmptyShould(true)
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
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }
}
