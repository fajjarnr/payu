package id.payu.account.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture tests using ArchUnit
 * Enforces architectural rules to maintain clean code structure
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
    @DisplayName("Layered Architecture")
    class LayeredArchitectureRules {

        @Test
        @DisplayName("should follow layered architecture pattern")
        void shouldFollowLayeredArchitecture() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Controller").definedBy("..controller..")
                    .layer("Service").definedBy("..service..")
                    .layer("Repository").definedBy("..repository..")
                    .layer("Entity").definedBy("..entity..")
                    .layer("DTO").definedBy("..dto..")
                    .layer("Config").definedBy("..config..")
                    
                    .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Config")
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                    
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventions {

        @Test
        @DisplayName("controllers should be suffixed with Controller")
        void controllersShouldBeSuffixedWithController() {
            classes()
                    .that().resideInAPackage("..controller..")
                    .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("services should be suffixed with Service")
        void servicesShouldBeSuffixedWithService() {
            classes()
                    .that().resideInAPackage("..service..")
                    .and().areAnnotatedWith(org.springframework.stereotype.Service.class)
                    .should().haveSimpleNameEndingWith("Service")
                    .orShould().haveSimpleNameEndingWith("Client")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("repositories should be suffixed with Repository")
        void repositoriesShouldBeSuffixedWithRepository() {
            classes()
                    .that().resideInAPackage("..repository..")
                    .should().haveSimpleNameEndingWith("Repository")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependency Rules")
    class DependencyRules {

        @Test
        @DisplayName("entities should not depend on services")
        void entitiesShouldNotDependOnServices() {
            noClasses()
                    .that().resideInAPackage("..entity..")
                    .should().dependOnClassesThat().resideInAPackage("..service..")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("entities should not depend on controllers")
        void entitiesShouldNotDependOnControllers() {
            noClasses()
                    .that().resideInAPackage("..entity..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("DTOs should not depend on entities")
        void dtosShouldNotDependOnEntities() {
            noClasses()
                    .that().resideInAPackage("..dto..")
                    .should().dependOnClassesThat().resideInAPackage("..entity..")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Spring Annotations")
    class SpringAnnotations {

        @Test
        @DisplayName("controllers should be annotated with RestController")
        void controllersShouldBeAnnotatedWithRestController() {
            classes()
                    .that().resideInAPackage("..controller..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("services should be annotated with Service")
        void servicesShouldBeAnnotatedWithService() {
            classes()
                    .that().resideInAPackage("..service..")
                    .and().haveSimpleNameEndingWith("Service")
                    .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
                    .check(importedClasses);
        }
    }
}
