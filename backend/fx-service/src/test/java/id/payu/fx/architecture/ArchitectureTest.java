package id.payu.fx.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@DisplayName("Architecture Tests - FX Service Hexagonal Architecture")
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.fx");
    }

    @Test
    @DisplayName("Domain layer should not depend on adapters or application layer")
    void domainShouldNotDependOnOuterLayers() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .because("Domain layer must be independent of infrastructure")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Domain model should not depend on Spring Framework")
    void domainModelShouldNotDependOnSpring() {
        noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("Domain model must be framework-agnostic")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Ports should be interfaces")
    void portsShouldBeInterfaces() {
        classes()
                .that().resideInAPackage("..domain.port..")
                .should().beInterfaces()
                .because("Ports define contracts and should be interfaces")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Controllers should be in adapter.web package")
    void controllersShouldBeInWebPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..adapter.web..")
                .because("Controllers are driving adapters and belong in adapter.web")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Services should be in application.service package")
    void servicesShouldBeInApplicationPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().resideInAPackage("..application.service..")
                .because("Service implementations belong in application layer")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("JPA entities should only be in adapter.persistence.entity package")
    void jpaEntitiesShouldBeInEntityPackage() {
        classes()
                .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInAPackage("..adapter.persistence.entity..")
                .because("JPA entities are infrastructure concerns")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Layered architecture should be respected")
    void layeredArchitectureShouldBeRespected() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Adapter").definedBy("..adapter..")
                .layer("Config").definedBy("..config..")
                .layer("DTO").definedBy("..dto..")
                .layer("SharedStarters").definedBy("id.payu.grpc..")
                .whereLayer("Domain").mayNotAccessAnyLayer()
                .whereLayer("Application").mayOnlyAccessLayers("Domain", "DTO", "SharedStarters")
                .whereLayer("Adapter").mayOnlyAccessLayers("Domain", "Application", "DTO", "SharedStarters")
                .because("Hexagonal architecture dependencies must flow inward")
                .check(classes);
    }
}
