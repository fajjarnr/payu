package id.payu.backoffice.testutil;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for integration tests that require Docker.
 * <p>
 * Integration tests annotated with {@code @IntegrationTest} will only run
 * when the {@code docker.enabled} system property is set to {@code true}.
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @IntegrationTest
 * @QuarkusTest
 * @QuarkusTestResource(PostgreSQLResourceTestLifecycleManager.class)
 * class MyRepositoryIntegrationTest {
 *     // Integration test methods
 * }
 * }
 * </pre>
 * <p>
 * Running integration tests:
 * <pre>
 * ./mvnw test -Ddocker.enabled=true
 * </pre>
 * <p>
 * Skipping integration tests (default):
 * <pre>
 * ./mvnw test
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@EnabledIfSystemProperty(named = "docker.enabled", matches = "true", disabledReason = """
        Integration tests require Docker to run PostgreSQL container.
        Enable Docker tests by running: ./mvnw test -Ddocker.enabled=true
        """)
public @interface IntegrationTest {
}
