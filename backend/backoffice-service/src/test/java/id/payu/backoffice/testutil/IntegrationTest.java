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
 * Tests only run when the {@code docker.enabled} system property is {@code true}.
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
