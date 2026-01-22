package id.payu.backoffice;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "id.payu.backoffice")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule layered_architecture = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("id.payu.backoffice..")
            .layer("Resource").definedBy("..resource..")
            .layer("Service").definedBy("..service..")
            .layer("Domain").definedBy("..domain..")
            .layer("Dto").definedBy("..dto..")

            .whereLayer("Resource").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Resource")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Resource", "Service", "Dto") // Resource accesses Domain for DTO mapping sometimes? Or should strictly use DTOs?
            // In current code, Resource uses Domain objects (e.g. KycReviewService returns KycReview, Resource maps it to Response).
            // So Resource accesses Domain.
            .whereLayer("Dto").mayOnlyBeAccessedByLayers("Resource", "Service");
}
