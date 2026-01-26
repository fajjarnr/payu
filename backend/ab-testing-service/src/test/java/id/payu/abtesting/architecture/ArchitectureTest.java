package id.payu.abtesting.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture tests ensuring hexagonal/clean architecture principles
 */
@DisplayName("Architecture Tests")
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("id.payu.abtesting");

    @Test
    @DisplayName("Domain entities should not depend on other layers")
    void domainEntitiesShouldBeIndependent() {
        noClasses()
                .that().resideInAPackage("..domain.entity..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..", "..interfaces..")
                .because("Domain entities should be independent of application, infrastructure, and interface layers")
                .check(classes);
    }

    @Test
    @DisplayName("Controllers should reside in interfaces.rest package")
    void controllersShouldResideInInterfacesPackage() {
        classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().resideInAPackage("..interfaces.rest..")
                .because("All REST controllers should be in interfaces.rest package")
                .check(classes);
    }

    @Test
    @DisplayName("Repositories should reside in domain.repository package")
    void repositoriesShouldResideInDomainPackage() {
        classes()
                .that().areAssignableTo(org.springframework.data.jpa.repository.JpaRepository.class)
                .should().resideInAPackage("..domain.repository..")
                .because("All repositories should be in domain.repository package")
                .check(classes);
    }

    @Test
    @DisplayName("Entities should reside in domain.entity package")
    void entitiesShouldResideInDomainPackage() {
        classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..domain.entity..")
                .because("All entities should be in domain.entity package")
                .check(classes);
    }

    @Test
    @DisplayName("Should enforce layered architecture")
    void shouldEnforceLayeredArchitecture() {
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Interface").definedBy("..interfaces..")
                .layer("Application").definedBy("..application..")
                .layer("Domain").definedBy("..domain..")
                .layer("Infrastructure").definedBy("..infrastructure..")
                // Interface layer is the entry point - can access Domain (for ports, DTOs)
                .whereLayer("Interface").mayOnlyBeAccessedByLayers("Interface", "Domain")
                // Application layer can be accessed by Interface, and can access Domain and Infrastructure
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Interface", "Application")
                // Domain layer can be accessed by Application and Infrastructure
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Interface", "Domain")
                // Infrastructure layer can be accessed by Application layer
                .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Application", "Domain", "Infrastructure")
                .check(classes);
    }
}
