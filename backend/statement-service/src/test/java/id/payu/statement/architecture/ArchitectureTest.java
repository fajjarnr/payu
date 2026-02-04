package id.payu.statement.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

@DisplayName("Architecture Tests - Statement Service")
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.statement");
    }

    @Test
    @DisplayName("Domain layer should not depend on service layer")
    void domainShouldNotDependOnServiceLayer() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..service..")
                .because("Domain layer must be independent of service layer")
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
    @DisplayName("Controllers should be in api package")
    void controllersShouldBeInApiPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..api..")
                .because("Controllers belong in api layer")
                .check(classes);
    }

    @Test
    @DisplayName("Services should be in service package")
    void servicesShouldBeInServicePackage() {
        classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().resideInAPackage("..service..")
                .because("Service implementations belong in service layer")
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
}
