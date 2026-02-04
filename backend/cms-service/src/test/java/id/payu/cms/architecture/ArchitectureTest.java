package id.payu.cms.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@DisplayName("Architecture Tests - CMS Service Hexagonal Architecture")
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.cms");
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
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("Domain model must be framework-agnostic")
                .check(classes);
    }

    @Test
    @DisplayName("Repositories should be interfaces")
    void repositoriesShouldBeInterfaces() {
        classes()
                .that().resideInAPackage("..domain.repository..")
                .should().beInterfaces()
                .because("Repositories define contracts and should be interfaces")
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
    @DisplayName("Services should be in application.service package")
    void servicesShouldBeInApplicationPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().resideInAPackage("..application.service..")
                .because("Service implementations belong in application layer")
                .check(classes);
    }

    @Test
    @DisplayName("JPA entities should only be in domain.entity package")
    void jpaEntitiesShouldBeInEntityPackage() {
        classes()
                .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInAPackage("..domain.entity..")
                .because("JPA entities are infrastructure concerns")
                .check(classes);
    }

    @Test
    @DisplayName("Layered architecture should be respected")
    void layeredArchitectureShouldBeRespected() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Adapter").definedBy("..adapter..")
                .layer("Config").definedBy("..config..")
                .whereLayer("Domain").mayNotAccessAnyLayer()
                .whereLayer("Application").mayOnlyAccessLayers("Domain")
                .whereLayer("Adapter").mayOnlyAccessLayers("Domain", "Application")
                .because("Hexagonal architecture dependencies must flow inward")
                .check(classes);
    }
}
