package id.payu.loanorigination;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(
        packages = "id.payu.loanorigination",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal_architecture = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("id.payu.loanorigination..")
            .layer("Adapter.Web").definedBy("..adapter.web..")
            .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
            .layer("Service").definedBy("..service..")
            .layer("Domain").definedBy("..domain..")

            .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapter.Persistence").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Adapter.Web")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter.Web", "Adapter.Persistence", "Service")
            .withOptionalLayers(true);
}
