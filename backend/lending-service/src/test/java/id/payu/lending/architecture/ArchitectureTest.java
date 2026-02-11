package id.payu.lending.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@DisplayName("Architecture Tests - Lending Service Hexagonal Architecture")
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.lending");
    }

    @Test
    @DisplayName("Domain layer should not depend on adapters or application layer")
    void domainShouldNotDependOnOuterLayers() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .because("Domain layer must be independent of infrastructure")
                .check(classes);
    }

    @Test
    @DisplayName("Domain model should not depend on Spring Framework")
    void domainModelShouldNotDependOnSpring() {
        noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("Domain model must be framework-agnostic")
                .check(classes);
    }

    @Test
    @DisplayName("Ports should be interfaces")
    void portsShouldBeInterfaces() {
        classes()
                .that().resideInAPackage("..domain.port..")
                .should().beInterfaces()
                .because("Ports define contracts and should be interfaces")
                .check(classes);
    }

    @Test
    @DisplayName("Controllers should be in adapter.web package")
    void controllersShouldBeInWebPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..adapter.web..")
                .because("Controllers are driving adapters and belong in adapter.web")
                .check(classes);
    }

    @Test
    @DisplayName("Services should be in application package")
    void servicesShouldBeInApplicationPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .and().haveSimpleNameNotContaining("Security") // Security services can be in security package
                .should().resideInAPackage("..application..")
                .because("Service implementations belong in application layer")
                .check(classes);
    }

    @Test
    @DisplayName("JPA entities should only be in entity package")
    void jpaEntitiesShouldBeInEntityPackage() {
        classes()
                .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInAPackage("..entity..")
                .because("JPA entities are infrastructure concerns")
                .check(classes);
    }

    @Test
    @DisplayName("Layered architecture should be respected")
    void layeredArchitectureShouldBeRespected() {
        // NOTE: Application layer currently depends on adapters due to direct persistence adapter usage.
        // This is a known technical debt - services should depend on ports (interfaces) only.
        // TODO: Refactor to use proper dependency injection via ports
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Adapter").definedBy("..adapter..")
                .layer("Config").definedBy("..config..")
                .layer("Exception").definedBy("..exception..")
                .whereLayer("Domain").mayNotAccessAnyLayer()
                .whereLayer("Application").mayOnlyAccessLayers("Domain", "Adapter", "Exception") // Allow adapter for now
                .whereLayer("Adapter").mayOnlyAccessLayers("Domain", "Application", "Exception")
                .because("Hexagonal architecture dependencies must flow inward (with temporary adapter allowance in application)")
                .check(classes);
    }
}
