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
 * Architecture Tests for Notification Service (Quarkus).
 * 
 * Enforces:
 * - Layered architecture boundaries
 * - Naming conventions
 * - Domain isolation
 * - Sender abstraction patterns
 */
@DisplayName("Architecture Rules - Notification Service")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.notification");
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
                    .layer("Sender").definedBy("..sender..")
                    .layer("Consumer").definedBy("..consumer..")
                    .layer("DTO").definedBy("..dto..")
                    
                    // Resource layer is entry point (REST API)
                    .whereLayer("Resource").mayNotBeAccessedByAnyLayer()
                    // Consumer layer is entry point (Kafka consumers)
                    .whereLayer("Consumer").mayNotBeAccessedByAnyLayer()
                    // Service layer accessed by Resource and Consumer
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Resource", "Consumer")
                    // Domain layer can be accessed by all business layers
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Service", "Resource", "DTO", "Consumer", "Sender")
                    // Sender layer for outbound notifications
                    .whereLayer("Sender").mayOnlyBeAccessedByLayers("Service")
                    
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
                            "..resource..",
                            "..sender..",
                            "..consumer..",
                            "org.eclipse.microprofile.."
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
        @DisplayName("senders should have Sender suffix")
        void sendersShouldHaveSenderSuffix() {
            classes()
                    .that().resideInAPackage("..sender..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Sender")
                    .because("Sender classes should follow naming convention")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("consumers should have Consumer suffix")
        void consumersShouldHaveConsumerSuffix() {
            classes()
                    .that().resideInAPackage("..consumer..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Consumer")
                    .because("Kafka consumer classes should follow naming convention")
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
    @DisplayName("Sender Abstraction Rules")
    class SenderAbstractionRules {

        @Test
        @DisplayName("senders should not depend on each other")
        void sendersShouldNotDependOnEachOther() {
            // Each sender should be independent
            noClasses()
                    .that().resideInAPackage("..sender..")
                    .and().haveSimpleNameEndingWith("Sender")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..sender..")
                    .andShould().haveSimpleNameEndingWith("Sender")
                    .because("Senders should be independent implementations")
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
    }
}
