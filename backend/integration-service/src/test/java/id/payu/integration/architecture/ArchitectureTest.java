package id.payu.integration.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit tests for hexagonal architecture compliance.
 */
public class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("id.payu.integration");

    @BeforeEach
    void skipIfNoClasses() {
        Assumptions.assumeFalse(classes.isEmpty(),
                "Skipping architecture tests: no classes imported (likely Java 25 / ASM incompatibility)");
    }

    @Test
    void domainShouldNotDependOnCamel() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.apache.camel..")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    void domainShouldNotDependOnSpring() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    void domainShouldNotDependOnJpa() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("jakarta.persistence..")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    void applicationShouldOnlyDependOnDomain() {
        ArchRule rule = ArchRuleDefinition.classes()
                .that().resideInAPackage("..application..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..domain..",
                        "..application..",
                        "java..",
                        "lombok..",
                        "org.slf4j.."
                )
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    void adaptersShouldNotDependOnEachOther() {
        ArchRule rule = SlicesRuleDefinition.slices()
                .matching("..adapter.(*)..")
                .should().notDependOnEachOther()
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    void controllersShouldResideInWebAdapter() {
        ArchRule rule = ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..adapter.web..")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    void repositoriesShouldResideInPersistenceAdapter() {
        ArchRule rule = ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("Repository")
                .and().haveSimpleNameNotContaining("JpaRepository")
                .should().resideInAPackage("..adapter.persistence..")
                .orShould().resideInAPackage("..domain.repository..")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    void routeBuildersShouldResideInCamelAdapter() {
        ArchRule rule = ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("RouteBuilder")
                .should().resideInAPackage("..adapter.camel..")
                .allowEmptyShould(true);

        rule.check(classes);
    }
}
