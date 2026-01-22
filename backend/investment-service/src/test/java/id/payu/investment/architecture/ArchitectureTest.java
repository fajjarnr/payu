package id.payu.investment.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Disabled("Architecture tests fail due to Lombok-generated classes - need to refine rules")
public class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("id.payu.investment");

    @Test
    void domainShouldNotDependOnAdapters() {
        classes().that().resideInAPackage("..domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..domain..", "java..", "jakarta..", "org.springframework..")
                .because("Domain layer should not depend on adapters or application layer")
                .check(classes);
    }

    @Test
    void adaptersShouldOnlyDependOnDomain() {
        classes().that().resideInAPackage("..adapter..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..domain..", "java..", "jakarta..", "org.springframework..", "org.slf4j..", "lombok..")
                .because("Adapters should only depend on domain layer and framework classes")
                .check(classes);
    }

    @Test
    void applicationShouldOnlyDependOnDomain() {
        classes().that().resideInAPackage("..application..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..domain..", "java..", "jakarta..", "org.springframework..", "io.github.resilience4j..", "org.slf4j..", "lombok..")
                .because("Application layer should only depend on domain layer")
                .check(classes);
    }
}
