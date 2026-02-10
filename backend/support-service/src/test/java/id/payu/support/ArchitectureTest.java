package id.payu.support;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "id.payu.support")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal_architecture = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("id.payu.support..")
            .layer("Adapter.Web").definedBy("..adapter.web..")
            .layer("Adapter.Persistence").definedBy("..adapter.persistence..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Config").definedBy("..config..")
            .layer("Dto").definedBy("..dto..")

            .whereLayer("Adapter.Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapter.Persistence").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter.Web")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter.Web", "Adapter.Persistence", "Application", "Dto")
            .whereLayer("Dto").mayOnlyBeAccessedByLayers("Adapter.Web", "Application");
}
