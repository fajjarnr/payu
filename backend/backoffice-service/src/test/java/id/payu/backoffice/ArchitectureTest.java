package id.payu.backoffice;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "id.payu.backoffice",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_is_framework_and_adapter_free = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..", "org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule application_does_not_depend_on_persistence_adapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.persistence..");

    @ArchTest
    static final ArchRule customer_case_slice_does_not_expose_persistence = noClasses()
            .that().haveSimpleName("CustomerCaseService")
            .or().haveSimpleName("CustomerCaseResponse")
            .or().haveSimpleName("BackofficeController")
            .should().dependOnClassesThat().haveSimpleName("CustomerCaseEntity");

    @ArchTest
    static final ArchRule fraud_case_slice_does_not_expose_persistence = noClasses()
            .that().haveSimpleName("FraudCaseService")
            .or().haveSimpleName("FraudCaseResponse")
            .or().haveSimpleName("BackofficeController")
            .should().dependOnClassesThat().haveSimpleName("FraudCaseEntity");

    @ArchTest
    static final ArchRule kyc_slice_does_not_expose_persistence = noClasses()
            .that().haveSimpleName("KycReviewService")
            .or().haveSimpleName("KycReviewResponse")
            .or().haveSimpleName("BackofficeController")
            .should().dependOnClassesThat().haveSimpleName("KycReviewEntity");

    @ArchTest
    static final ArchRule hexagonal_architecture = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("id.payu.backoffice..")
            .withOptionalLayers(true)
            .layer("Adapter.Web").definedBy("..adapter.web..")
            .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Config").definedBy("..config..")
            .layer("Dto").definedBy("..dto..")
            .layer("OpenApi").definedBy("..openapi..")

            .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapter.Persistence").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter.Web")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter.Web", "Adapter.Persistence", "Application", "Dto")
            .whereLayer("Dto").mayOnlyBeAccessedByLayers("Adapter.Web", "Application");
}
