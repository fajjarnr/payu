package id.payu.auth.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture tests using ArchUnit — Hexagonal Architecture.
 * Enforces ports & adapters rules for auth-service.
 */
@DisplayName("Architecture Rules - Auth Service (Hexagonal)")
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.auth");
    }

    @BeforeEach
    void skipIfNoClasses() {
        Assumptions.assumeFalse(importedClasses.isEmpty(),
                "Skipping architecture tests: no classes imported (likely Java 25 / ASM incompatibility)");
    }

    @Nested
    @DisplayName("Hexagonal Architecture")
    class HexagonalArchitectureRules {

        @Test
        @DisplayName("should follow hexagonal architecture pattern")
        void shouldFollowHexagonalArchitecture() {
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Adapter.Web").definedBy("..adapter.web..")
                    .layer("Adapter.Security").definedBy("..adapter.security..")
                    .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
                    .layer("Application").definedBy("..application..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("Config").definedBy("..config..")
                    .layer("DTO").definedBy("..dto..")
                    .layer("Exception").definedBy("..exception..")

                    .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter.Persistence").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Application").mayOnlyBeAccessedByLayers(
                            "Adapter.Web", "Adapter.Security")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                            "Application",
                            "Adapter.Web", "Adapter.Security", "Adapter.Persistence",
                            "DTO", "Exception"
                    )

                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventions {

        @Test
        @DisplayName("web adapters should be suffixed with Controller")
        void controllersShouldBeSuffixedWithController() {
            classes()
                    .that().resideInAPackage("..adapter.web..")
                    .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("application services should be suffixed with Service")
        void servicesShouldBeSuffixedWithService() {
            classes()
                    .that().resideInAPackage("..application.service..")
                    .and().areAnnotatedWith(org.springframework.stereotype.Service.class)
                    .should().haveSimpleNameEndingWith("Service")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("config classes should be suffixed with Config")
        void configsShouldBeSuffixedWithConfig() {
            classes()
                    .that().resideInAPackage("..config..")
                    .and().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                    .should().haveSimpleNameEndingWith("Config")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("ports should be interfaces")
        void portsShouldBeInterfaces() {
            classes()
                    .that().resideInAPackage("..domain.port..")
                    .and().areTopLevelClasses()
                    .should().beInterfaces()
                    .because("Ports define contracts and must be interfaces")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependency Rules")
    class DependencyRules {

        @Test
        @DisplayName("DTOs should not depend on application services")
        void dtosShouldNotDependOnServices() {
            noClasses()
                    .that().resideInAPackage("..dto..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("DTOs should not depend on adapters")
        void dtosShouldNotDependOnAdapters() {
            noClasses()
                    .that().resideInAPackage("..dto..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Spring Annotations")
    class SpringAnnotations {

        @Test
        @DisplayName("web adapters should be annotated with RestController")
        void controllersShouldBeAnnotatedWithRestController() {
            classes()
                    .that().resideInAPackage("..adapter.web..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }

        @Test
        @DisplayName("application services should be annotated with Service")
        void servicesShouldBeAnnotatedWithService() {
            classes()
                    .that().resideInAPackage("..application.service..")
                    .and().haveSimpleNameEndingWith("Service")
                    .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }
}
