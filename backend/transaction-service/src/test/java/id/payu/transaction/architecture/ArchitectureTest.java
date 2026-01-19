package id.payu.transaction.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "id.payu.transaction", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_layer_should_be_free_of_dependencies =
            classes().that().resideInAPackage("..domain..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("java..", "org.springframework.data..", "..domain..", "id.payu.transaction.dto..", "lombok..");

    @ArchTest
    static final ArchRule application_layer_should_only_depend_on_domain =
            classes().that().resideInAPackage("..application..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("id.payu.transaction.domain..", "id.payu.transaction.dto..", "java..", "lombok..", "org.springframework..", "org.slf4j..");

    @ArchTest
    static final ArchRule adapter_layer_should_only_depend_on_domain_and_application =
            classes().that().resideInAPackage("..adapter..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("id.payu.transaction.domain..", "id.payu.transaction.application..", "id.payu.transaction.dto..", "java..", "org.springframework..", "lombok..", "org.slf4j..", "com.fasterxml.jackson..", "jakarta..", "..adapter..", "io.github.resilience4j..");

    @ArchTest
    static final ArchRule controllers_should_only_depend_on_usecases =
            classes().that().resideInAPackage("..adapter.web..")
                    .should().onlyDependOnClassesThat()
                    // Allowed domain.model because controller returns Transaction object currently
                    .resideInAnyPackage("id.payu.transaction.domain.port.in..", "id.payu.transaction.domain.model..", "id.payu.transaction.dto..", "java..", "org.springframework..", "jakarta..", "lombok..");

    @ArchTest
    static final ArchRule adapters_should_have_suffixed_names =
            classes().that().resideInAPackage("..adapter..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Adapter")
                    .orShould().haveSimpleNameEndingWith("Controller"); // Controller is also an adapter

    @ArchTest
    static final ArchRule services_should_have_suffixed_names =
            classes().that().resideInAPackage("..application.service..")
                    .and().areNotInterfaces()
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Service");

    @ArchTest
    static final ArchRule controllers_should_have_suffixed_names =
            classes().that().resideInAPackage("..adapter.web..")
                    .and().areNotInterfaces()
                    .should().haveSimpleNameEndingWith("Controller");
}
