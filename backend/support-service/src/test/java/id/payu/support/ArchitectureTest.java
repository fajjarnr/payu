package id.payu.support;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    @Test
    void domainModelShouldNotDependOnServiceLayer() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("id.payu.support");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..service..");

        rule.check(importedClasses);
    }

    @Test
    void domainModelShouldNotDependOnResourceLayer() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("id.payu.support");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..resource..");

        rule.check(importedClasses);
    }

    @Test
    void resourceLayerShouldOnlyUseServiceLayer() {
        JavaClasses importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.payu.support");

        ArchRule rule = classes()
                .that().resideInAPackage("..resource..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        "jakarta..",
                        "id.payu.support.dto..",
                        "id.payu.support.service..",
                        "id.payu.support.domain..",
                        "org.jboss.logging..",
                        "org.eclipse.microprofile.openapi.annotations..",
                        "org.hamcrest.."
                );

        rule.check(importedClasses);
    }

    @Test
    void servicesShouldBeApplicationScoped() {
        JavaClasses importedClasses = new ClassFileImporter()
                .importPackages("id.payu.support.service");

        ArchRule rule = classes()
                .that().resideInAPackage("..service..")
                .and().doNotHaveSimpleName("AgentServiceTest")
                .should().beAnnotatedWith("jakarta.enterprise.context.ApplicationScoped");

        rule.check(importedClasses);
    }
}
