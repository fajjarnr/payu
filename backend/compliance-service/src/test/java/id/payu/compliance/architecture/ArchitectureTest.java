package id.payu.compliance.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("id.payu.compliance");

    @Test
    void domainLayerShouldNotDependOnAdapterLayer() {
        ArchRule rule = classes()
                .that().resideInAPackage("id.payu.compliance.domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "id.payu.compliance.domain..",
                        "java..",
                        "jakarta..",
                        "org.springframework.format..",
                        "org.springframework.validation..",
                        "org.springframework.data.domain..",
                        "lombok.."
                );

        rule.check(classes);
    }

    @Test
    void serviceLayerShouldOnlyDependOnDomainAndPorts() {
        ArchRule rule = classes()
                .that().resideInAPackage("id.payu.compliance.application..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "id.payu.compliance.domain..",
                        "id.payu.compliance.application..",
                        "id.payu.compliance.domain.port..",
                        "java..",
                        "org.springframework..",
                        "org.slf4j..",
                        "lombok..",
                        "jakarta.."
                );

        rule.check(classes);
    }

    @Test
    void adapterLayerShouldOnlyDependOnDomainAndPorts() {
        ArchRule rule = classes()
                .that().resideInAPackage("id.payu.compliance.adapter..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "id.payu.compliance.domain..",
                        "id.payu.compliance.domain.port..",
                        "id.payu.compliance.application..",
                        "id.payu.compliance.adapter..",
                        "id.payu.compliance.dto..",
                        "id.payu.compliance.exception..",
                        "java..",
                        "org.springframework..",
                        "org.slf4j..",
                        "lombok..",
                        "jakarta..",
                        "io.swagger.."
                );

        rule.check(classes);
    }

    @Test
    void controllersShouldResideInWebPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().resideInAPackage("id.payu.compliance.adapter.web");

        rule.check(classes);
    }

    @Test
    void domainModelsShouldNotBeAnnotatedWithSpringAnnotations() {
        ArchRule rule = classes()
                .that().resideInAPackage("id.payu.compliance.domain.model..")
                .should().notBeAnnotatedWith("org.springframework.stereotype.Component")
                .andShould().notBeAnnotatedWith("org.springframework.stereotype.Service");

        rule.check(classes);
    }
}
