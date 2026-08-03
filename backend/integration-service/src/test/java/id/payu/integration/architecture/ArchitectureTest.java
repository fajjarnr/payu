package id.payu.integration.architecture;

import id.payu.integration.application.port.out.MessagePublisherPort;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * ArchUnit tests for hexagonal architecture compliance.
 */
public class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("id.payu.integration");

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
        // BUG-INT-HEX-001 Fix (iter 46): MessageProcessingService moved from
        // domain.service to application.service. Domain no longer has
        // @Service / @Transactional. Rule re-enabled.
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
        // BUG-INT-HEX-001 Fix (iter 46): ProducerTemplate moved from
        // IntegrationService to MessagePublisherAdapter (via MessagePublisherPort).
        // Application no longer depends on Camel API directly.
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.apache.camel..",
                        "..adapter.camel..",
                        "..adapter.messaging..",
                        "..adapter.web..",
                        "..adapter.persistence.entity.."
                )
                .because("Application layer should depend only on domain layer (ports) — adapter types belong in adapter layer")
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

    @Test
    void messagePublisherPortDoesNotExposeUnsupportedGrpcPlaceholder() {
        assertFalse(Arrays.stream(MessagePublisherPort.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("publishToGrpc")));
    }
}
