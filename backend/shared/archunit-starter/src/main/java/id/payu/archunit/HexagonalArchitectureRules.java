package id.payu.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.Arrays;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Predefined ArchUnit rules for enforcing Hexagonal Architecture (Ports and Adapters)
 * in PayU microservices.
 *
 * <p>These rules ensure:
 * <ul>
 *   <li>Domain layer independence from frameworks</li>
 *   <li>Proper dependency direction (Domain ← Application ← Infrastructure)</li>
 *   <li>Port interfaces define boundaries</li>
 *   <li>Adapters implement ports, not accessed directly</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * @ArchTest
 * static final ArchRule domainShouldNotDependOnInfrastructure =
 *     HexagonalArchitectureRules.domainShouldNotDependOnInfrastructure();
 * }
 * </pre>
 *
 * @author PayU Architecture Team
 * @version 1.0.0
 */
public final class HexagonalArchitectureRules {

    private HexagonalArchitectureRules() {
        // Utility class - prevent instantiation
    }

    // ============================================================================
    // Layer Package Patterns
    // ============================================================================

    /**
     * Standard package patterns for hexagonal architecture layers.
     * These can be customized per service.
     */
    public static class PackagePatterns {
        public static final String DOMAIN = "..domain..";
        public static final String APPLICATION = "..application..";
        public static final String INFRASTRUCTURE = "..infrastructure..";
        public static final String ADAPTER = "..adapter..";
        public static final String CONFIG = "..config..";
        public static final String PORT = "..port..";
        public static final String REPOSITORY = "..repository..";
        public static final String ENTITY = "..entity..";
        public static final String USECASE = "..usecase..";
        public static final String SERVICE = "..service..";

        private PackagePatterns() {}
    }

    /**
     * Framework packages that domain layer should not depend on.
     */
    public static final String[] FRAMEWORK_PACKAGES = {
        "org.springframework..",
        "org.springframework.data..",
        "org.springframework.web..",
        "org.springframework.boot..",
        "org.springframework.cloud..",
        "jakarta.persistence..",
        "jakarta.validation..",
        "jakarta.inject..",
        "jakarta.ws.rs..",
        "org.hibernate..",
        "org.apache.kafka..",
        "io.micrometer..",
        "org.slf4j..",
        "org.apache.logging.."
    };

    /**
     * Infrastructure-related packages that domain should not depend on.
     */
    public static final String[] INFRASTRUCTURE_PACKAGES = {
        "..infrastructure..",
        "..adapter..",
        "..config..",
        "..repository..",
        "..entity..",
        "..persistence..",
        "..web..",
        "..api..",
        "..client..",
        "..messaging..",
        "..external.."
    };

    // ============================================================================
    // Rule 1: Domain Independence
    // ============================================================================

    /**
     * Rule: Domain layer should not depend on infrastructure, adapters, or Spring framework.
     *
     * <p>This ensures the domain layer remains pure Java with no framework dependencies,
     * making it highly testable and portable.
     *
     * @return ArchRule enforcing domain independence
     */
    public static ArchRule domainShouldNotDependOnInfrastructure() {
        return noClasses()
                .that()
                .resideInAPackage(PackagePatterns.DOMAIN)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(INFRASTRUCTURE_PACKAGES)
                .orShould()
                .dependOnClassesThat()
                .resideInAnyPackage(FRAMEWORK_PACKAGES)
                .because("Domain layer must be framework-independent for hexagonal architecture. " +
                        "Domain should only contain pure business logic without dependencies on " +
                        "Spring, JPA, or infrastructure concerns");
    }

    /**
     * Rule: Domain layer should not depend on application layer.
     *
     * <p>The domain is the inner core and should not know about use cases or application services.
     *
     * @return ArchRule enforcing domain-application boundary
     */
    public static ArchRule domainShouldNotDependOnApplication() {
        return noClasses()
                .that()
                .resideInAPackage(PackagePatterns.DOMAIN)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(PackagePatterns.APPLICATION)
                .because("Domain layer is the inner core and should not depend on the application layer. " +
                        "Application layer depends on domain, not vice versa");
    }

    // ============================================================================
    // Rule 2: Application Layer Port Access
    // ============================================================================

    /**
     * Rule: Application layer should only access repositories through port interfaces.
     *
     * <p>Application services should depend on Repository Port interfaces (output ports),
     * not on concrete repository implementations.
     *
     * @return ArchRule enforcing port-based repository access
     */
    public static ArchRule applicationShouldOnlyAccessRepositoriesThroughPorts() {
        return classes()
                .that()
                .resideInAPackage(PackagePatterns.APPLICATION)
                .and()
                .haveSimpleNameContaining("Service")
                .or()
                .haveSimpleNameContaining("UseCase")
                .should()
                .onlyDependOnClassesThat()
                .resideOutsideOfPackages("..repository..", "..adapter.persistence..")
                .orShould()
                .onlyDependOnClassesThat()
                .resideInAPackage("..port.out..")
                .orShould()
                .onlyDependOnClassesThat()
                .resideInAPackage("..port.output..")
                .orShould()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..domain..",
                        "..dto..",
                        "..exception..",
                        "..mapper..",
                        "java..",
                        "lombok..",
                        "org.springframework.transaction..",
                        "org.springframework.stereotype.."
                )
                .because("Application layer should only access repositories through Port interfaces " +
                        "(Dependency Inversion Principle). Concrete repository implementations " +
                        "should be in infrastructure layer");
    }

    // ============================================================================
    // Rule 3: Adapter Isolation
    // ============================================================================

    /**
     * Rule: Adapters should not leak into domain layer.
     *
     * <p>Domain code should never directly call adapter classes. All communication
     * should go through ports.
     *
     * @return ArchRule enforcing adapter isolation
     */
    public static ArchRule adaptersShouldNotLeakIntoDomain() {
        return noClasses()
                .that()
                .resideInAPackage(PackagePatterns.DOMAIN)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(PackagePatterns.ADAPTER)
                .because("Adapters should not leak into domain layer. Domain should only depend on " +
                        "Port interfaces, not concrete Adapter implementations");
    }

    /**
     * Rule: Adapters should implement ports and be in infrastructure layer.
     *
     * @return ArchRule enforcing adapter location and implementation
     */
    public static ArchRule adaptersShouldImplementPorts() {
        return classes()
                .that()
                .haveSimpleNameEndingWith("Adapter")
                .should()
                .resideInAPackage(PackagePatterns.INFRASTRUCTURE)
                .andShould()
                .implement(DescribedPredicate.describe("an interface in port package",
                        javaClass -> javaClass.getPackageName().contains("port")))
                .because("Adapters should be in infrastructure layer and implement Port interfaces");
    }

    // ============================================================================
    // Rule 4: Transactional Use Cases
    // ============================================================================

    /**
     * Rule: All use case methods should be annotated with @Transactional.
     *
     * <p>Ensures that business operations are atomic and properly managed.
     *
     * @return ArchRule enforcing @Transactional on use cases
     */
    public static ArchRule useCasesShouldBeTransactional() {
        return methods()
                .that()
                .arePublic()
                .and()
                .areDeclaredInClassesThat()
                .haveSimpleNameEndingWith("UseCase")
                .or()
                .areDeclaredInClassesThat()
                .haveSimpleNameEndingWith("Service")
                .and()
                .areDeclaredInClassesThat()
                .resideInAPackage(PackagePatterns.APPLICATION)
                .should()
                .beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .because("All use case methods should be @Transactional to ensure atomicity " +
                        "and proper transaction management for business operations");
    }

    /**
     * Alternative rule: Use cases that modify state should be transactional.
     * Excludes query methods (starting with 'get', 'find', 'search').
     *
     * @return ArchRule enforcing @Transactional on state-modifying use cases
     */
    public static ArchRule stateModifyingUseCasesShouldBeTransactional() {
        return methods()
                .that()
                .arePublic()
                .and()
                .areDeclaredInClassesThat()
                .resideInAPackage(PackagePatterns.APPLICATION)
                .and()
                .areDeclaredInClassesThat()
                .resideOutsideOfPackages("..metrics..")
                .and()
                .areDeclaredInClassesThat()
                .areNotInterfaces()
                .and()
                .areDeclaredInClassesThat(DescribedPredicate.describe("have Service or UseCase suffix",
                        javaClass -> javaClass.getSimpleName().endsWith("Service") || javaClass.getSimpleName().endsWith("UseCase")))
                .and(new DescribedPredicate<>("modify state (not query)") {
                    @Override
                    public boolean test(JavaMethod method) {
                        String name = method.getName();
                        return !name.startsWith("get")
                                && !name.startsWith("find")
                                && !name.startsWith("search")
                                && !name.startsWith("query")
                                && !name.startsWith("list")
                                && !name.startsWith("count")
                                && !name.startsWith("exists")
                                && !name.startsWith("is")
                                && !name.startsWith("has")
                                && !name.startsWith("record");
                    }
                })
                .should()
                .beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .because("State-modifying use case methods should be @Transactional to ensure " +
                        "data consistency. Query methods are excluded");
    }

    // ============================================================================
    // Rule 5: Repository Port Contracts
    // ============================================================================

    /**
     * Rule: Repository port methods should return domain objects, not entities.
     *
     * <p>Repository ports (interfaces in domain layer) should work with domain aggregates
     * and value objects, not JPA entities or infrastructure types.
     *
     * @return ArchRule enforcing domain object returns from repository ports
     */
    public static ArchRule repositoriesShouldNotReturnEntities() {
        return methods()
                .that()
                .areDeclaredInClassesThat()
                .resideInAPackage("..port.out..")
                .or()
                .areDeclaredInClassesThat()
                .resideInAPackage("..port.output..")
                .or()
                .areDeclaredInClassesThat()
                .resideInAPackage("..port.outbound..")
                .or()
                .areDeclaredInClassesThat()
                .haveSimpleNameEndingWith("Repository")
                .or()
                .areDeclaredInClassesThat()
                .haveSimpleNameEndingWith("RepositoryPort")
                .and()
                .areDeclaredInClassesThat()
                .resideInAPackage(PackagePatterns.DOMAIN)
                .should(new ArchCondition<>("return domain objects, not JPA entities") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        JavaClass returnType = method.getRawReturnType();

                        // Check if return type contains "Entity" in name
                        if (returnType.getSimpleName().endsWith("Entity")) {
                            events.add(SimpleConditionEvent.violated(method,
                                    String.format("Method %s in %s returns JPA entity %s. " +
                                                    "Repository ports should return domain aggregates, not entities",
                                            method.getName(),
                                            method.getOwner().getName(),
                                            returnType.getName())));
                        }

                        // Check for JPA annotations on return type
                        if (returnType.isAnnotatedWith("jakarta.persistence.Entity")) {
                            events.add(SimpleConditionEvent.violated(method,
                                    String.format("Method %s in %s returns type annotated with @Entity. " +
                                                    "Repository ports should return domain aggregates",
                                            method.getName(),
                                            method.getOwner().getName())));
                        }
                    }
                })
                .because("Repository ports should return domain aggregates/entities, not JPA entities. " +
                        "This keeps the domain layer independent of persistence technology");
    }

    // ============================================================================
    // Additional Helper Rules
    // ============================================================================

    /**
     * Rule: Domain entities should be in domain.model package.
     *
     * @return ArchRule enforcing domain entity location
     */
    public static ArchRule domainEntitiesShouldBeInModelPackage() {
        return classes()
                .that()
                .haveSimpleNameEndingWith("Entity")
                .and()
                .areNotAnnotatedWith("jakarta.persistence.Entity")
                .should()
                .resideInAPackage("..domain.model..")
                .because("Domain entities should be in domain.model package");
    }

    /**
     * Rule: JPA entities should be in infrastructure/persistence layer.
     *
     * @return ArchRule enforcing JPA entity location
     */
    public static ArchRule jpaEntitiesShouldBeInInfrastructure() {
        return classes()
                .that()
                .areAnnotatedWith("jakarta.persistence.Entity")
                .should()
                .resideInAnyPackage(
                        "..infrastructure.persistence..",
                        "..infrastructure.entity..",
                        "..adapter.persistence..",
                        "..repository.entity.."
                )
                .because("JPA entities are infrastructure concerns and should not be in domain layer");
    }

    /**
     * Rule: Ports should be interfaces.
     *
     * @return ArchRule enforcing port interface type
     */
    public static ArchRule portsShouldBeInterfaces() {
        return classes()
                .that()
                .resideInAPackage("..port..")
                .and()
                .areTopLevelClasses()
                .should()
                .beInterfaces()
                .because("Ports in hexagonal architecture should be interfaces defining contracts");
    }

    /**
     * Rule: Dependency direction should follow: Infrastructure → Application → Domain.
     *
     * @return ArchRule enforcing correct dependency direction
     */
    public static ArchRule dependencyDirectionShouldFollowHexagonal() {
        return noClasses()
                .that()
                .resideInAPackage(PackagePatterns.INFRASTRUCTURE)
                .or()
                .resideInAPackage(PackagePatterns.ADAPTER)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(PackagePatterns.INFRASTRUCTURE)
                .andShould()
                .dependOnClassesThat()
                .resideInAPackage(PackagePatterns.APPLICATION)
                .andShould()
                .dependOnClassesThat()
                .resideInAPackage(PackagePatterns.DOMAIN)
                .because("Dependencies should point inward: Infrastructure depends on Application, " +
                        "Application depends on Domain. Domain has no outgoing dependencies.");
    }

    /**
     * GRPC-021(a): inter-service HTTP clients (RestTemplate/WebClient) must only
     * live in the client adapter package. Application/scheduler code that talks
     * to another service directly over HTTP bypasses the hexagonal boundary and
     * the resilience/correlation handling of the adapter layer.
     *
     * @return ArchRule forbidding raw HTTP clients outside adapter.client
     */
    public static ArchRule httpClientsOnlyInClientAdapters() {
        return noClasses()
                .that()
                .resideOutsideOfPackage("..adapter.client..")
                .and()
                .resideOutsideOfPackage("..config..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("RestTemplate")
                .orShould()
                .dependOnClassesThat()
                .haveSimpleName("WebClient")
                .orShould()
                .dependOnClassesThat()
                .haveSimpleName("RestClient")
                .orShould()
                .dependOnClassesThat()
                .haveSimpleName("OkHttpClient")
                .because("GRPC-021: inter-service calls must go through adapter client ports, "
                        + "not raw RestTemplate/WebClient from application or domain code "
                        + "(config classes may wire client beans)");
    }

    /**
     * Combined rule set for comprehensive hexagonal architecture validation.
     *
     * @return List of all core hexagonal architecture rules
     */
    public static List<ArchRule> allRules() {
        return Arrays.asList(
                domainShouldNotDependOnInfrastructure(),
                domainShouldNotDependOnApplication(),
                applicationShouldOnlyAccessRepositoriesThroughPorts(),
                adaptersShouldNotLeakIntoDomain(),
                useCasesShouldBeTransactional(),
                repositoriesShouldNotReturnEntities(),
                portsShouldBeInterfaces(),
                jpaEntitiesShouldBeInInfrastructure()
        );
    }
}
