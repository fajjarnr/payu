package id.payu.lendingrules;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(
        packages = "id.payu.lendingrules",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal_architecture = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("id.payu.lendingrules..")
            .layer("Adapter.Web").definedBy("..adapter.web..")
            .layer("Domain").definedBy("..domain..")
            .layer("Config").definedBy("..config..")

            .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter.Web", "Config")
            .withOptionalLayers(true);
}
