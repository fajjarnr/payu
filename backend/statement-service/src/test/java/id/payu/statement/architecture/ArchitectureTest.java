package id.payu.statement.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "id.payu.statement")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal_architecture = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("id.payu.statement..")
            .layer("Adapter.Web").definedBy("..adapter.web..")
            .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Config").definedBy("..config..")

            .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapter.Persistence").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter.Web")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter.Web", "Adapter.Persistence", "Application")
            .allowEmptyShould(true);

    // Epic E-19: Receipt Domain Architecture Tests
    @ArchTest
    static final ArchRule receipt_domain_classes_should_be_in_domain_model_package =
            ArchRuleDefinition.classes()
                    .that().haveSimpleNameContaining("Receipt")
                    .and().haveSimpleNameNotContaining("Test")
                    .and().haveSimpleNameNotContaining("Entity")
                    .and().haveSimpleNameNotContaining("Repository")
                    .and().haveSimpleNameNotContaining("Service")
                    .and().haveSimpleNameNotContaining("Controller")
                    .and().haveSimpleNameNotContaining("Response")
                    .and().haveSimpleNameNotContaining("Request")
                    .should().resideInAPackage("..domain.model..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule receipt_repository_port_should_be_in_application_port_package =
            ArchRuleDefinition.classes()
                    .that().haveSimpleNameContaining("ReceiptRepositoryPort")
                    .should().resideInAPackage("..application.port.output..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule receipt_service_should_be_in_application_service_package =
            ArchRuleDefinition.classes()
                    .that().haveSimpleName("ReceiptService")
                    .should().resideInAPackage("..application.service..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule receipt_entity_should_be_in_adapter_persistence_package =
            ArchRuleDefinition.classes()
                    .that().haveSimpleName("ReceiptEntity")
                    .should().resideInAPackage("..adapter.persistence.entity..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule receipt_repository_adapter_should_be_in_adapter_persistence_package =
            ArchRuleDefinition.classes()
                    .that().haveSimpleName("ReceiptRepositoryAdapter")
                    .should().resideInAPackage("..adapter.persistence..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring_framework =
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage("..domain.model..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule value_objects_should_be_immutable =
            ArchRuleDefinition.classes()
                    .that().haveSimpleNameContaining("Info")
                    .and().resideInAPackage("..domain.model..")
                    .should().haveOnlyFinalFields()
                    .allowEmptyShould(true);
}
