package id.payu.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;

/**
 * Base test class for Hexagonal Architecture enforcement.
 *
 * <p>Services should extend this class and provide the {@link AnalyzeClasses} annotation
 * to specify the packages to analyze. All hexagonal architecture rules will be automatically
 * applied.</p>
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * @AnalyzeClasses(
 *     packages = "id.payu.account",
 *     importOptions = ImportOption.DoNotIncludeTests.class
 * )
 * class AccountArchitectureTest extends HexagonalArchitectureTest {
 *     // Additional service-specific rules can be added here
 * }
 * }
 * </pre>
 *
 * <p>Architecture Layers:
 * <ul>
 *   <li><b>Domain Layer:</b> Pure business logic, no framework dependencies</li>
 *   <li><b>Application Layer:</b> Use cases, orchestration, ports</li>
 *   <li><b>Infrastructure Layer:</b> Adapters, persistence, external services</li>
 * </ul>
 *
 * @author PayU Architecture Team
 * @version 1.0.0
 * @see HexagonalArchitectureRules
 */
@DisplayName("Hexagonal Architecture Enforcement")
public abstract class HexagonalArchitectureTest {

    /**
     * Rule: Domain layer should not depend on infrastructure, adapters, or frameworks.
     *
     * <p>Ensures the domain layer remains pure and framework-independent.
     */
    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
            HexagonalArchitectureRules.domainShouldNotDependOnInfrastructure();

    /**
     * Rule: Domain layer should not depend on application layer.
     *
     * <p>Domain is the inner core and should not know about use cases.
     */
    @ArchTest
    static final ArchRule domainShouldNotDependOnApplication =
            HexagonalArchitectureRules.domainShouldNotDependOnApplication();

    /**
     * Rule: Application layer should only access repositories through port interfaces.
     *
     * <p>Enforces Dependency Inversion Principle for repository access.
     */
    @ArchTest
    static final ArchRule applicationShouldOnlyAccessRepositoriesThroughPorts =
            HexagonalArchitectureRules.applicationShouldOnlyAccessRepositoriesThroughPorts();

    /**
     * Rule: Adapters should not leak into domain layer.
     *
     * <p>Ensures domain only depends on port interfaces, not concrete implementations.
     */
    @ArchTest
    static final ArchRule adaptersShouldNotLeakIntoDomain =
            HexagonalArchitectureRules.adaptersShouldNotLeakIntoDomain();

    /**
     * Rule: State-modifying use case methods should be @Transactional.
     *
     * <p>Ensures atomicity for business operations that modify state.
     * Query methods (get*, find*, search*) are excluded.
     */
    @ArchTest
    static final ArchRule useCasesShouldBeTransactional =
            HexagonalArchitectureRules.stateModifyingUseCasesShouldBeTransactional();

    /**
     * Rule: Repository port methods should return domain objects, not JPA entities.
     *
     * <p>Keeps domain layer independent of persistence technology.
     */
    @ArchTest
    static final ArchRule repositoriesShouldNotReturnEntities =
            HexagonalArchitectureRules.repositoriesShouldNotReturnEntities();

    /**
     * Rule: Ports should be interfaces.
     *
     * <p>Enforces that ports define contracts as interfaces.
     */
    @ArchTest
    static final ArchRule portsShouldBeInterfaces =
            HexagonalArchitectureRules.portsShouldBeInterfaces();

    /**
     * Rule: JPA entities should be in infrastructure layer.
     *
     * <p>Ensures persistence concerns don't leak into domain.
     */
    @ArchTest
    static final ArchRule jpaEntitiesShouldBeInInfrastructure =
            HexagonalArchitectureRules.jpaEntitiesShouldBeInInfrastructure();

    /**
     * Rule: raw HTTP clients (RestTemplate/WebClient) only in adapter.client.
     *
     * <p>GRPC-021: inter-service calls must cross the hexagonal boundary through
     * adapter client ports.
     */
    @ArchTest
    static final ArchRule httpClientsOnlyInClientAdapters =
            HexagonalArchitectureRules.httpClientsOnlyInClientAdapters();

    /**
     * Utility method to import classes for a specific package.
     *
     * <p>Useful for programmatic rule checking in custom tests.
     *
     * @param packageName the base package to import
     * @return imported JavaClasses for analysis
     */
    protected static JavaClasses importClasses(String packageName) {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages(packageName);
    }

    /**
     * Utility method to import classes with custom import options.
     *
     * @param packageName the base package to import
     * @param importOptions additional import options
     * @return imported JavaClasses for analysis
     */
    protected static JavaClasses importClasses(String packageName, ImportOption... importOptions) {
        ClassFileImporter importer = new ClassFileImporter();
        for (ImportOption option : importOptions) {
            importer = importer.withImportOption(option);
        }
        return importer.importPackages(packageName);
    }
}
