package id.payu.investment.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class ArchitectureTest {

    // Exclude Lombok-generated classes and test classes from analysis
    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("id.payu.investment");

    @Test
    void domainShouldNotDependOnAdapters() {
        classes().that().resideInAPackage("..domain..")
                .and().areNotAssignableTo(Object.class) // include all
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..domain..", "..dto..", "java..", "jakarta..", "org.springframework..", "lombok..")
                .because("Domain layer should not depend on adapters or application layer (DTOs allowed for event payloads + value objects shared with adapters)")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void adaptersShouldOnlyDependOnDomain() {
        classes().that().resideInAPackage("..adapter..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..domain..", "..dto..", "..adapter..", "..application..",
                        "java..", "javax..", "jakarta..", "org.springframework..",
                        "org.slf4j..", "lombok..", "io.github.resilience4j..",
                        "com.fasterxml..", "org.mapstruct..",
                        "org.apache.kafka..", "io.grpc..",
                        "id.payu..", "io.swagger.."
                )
                .because("Adapters depend on domain + framework + shared PayU modules (api-commons, outbox, security, gRPC stubs) + swagger annotations")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void applicationShouldOnlyDependOnDomain() {
        classes().that().resideInAPackage("..application..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..domain..", "..dto..", "..application..",
                        "java..", "jakarta..", "org.springframework..",
                        "io.github.resilience4j..", "org.slf4j..", "lombok..",
                        "id.payu..", "com.fasterxml.."
                )
                .because("Application layer depends on domain + framework + shared PayU modules (outbox, events, etc)")
                .allowEmptyShould(true)
                .check(classes);
    }
}
