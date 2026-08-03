package id.payu.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-30 + GAP-28 fix: {@link SecurityAutoConfiguration#encryptionService()} must fail
 * fast in production profiles (container/prod/staging) when
 * {@code payu.security.encryption.password} is not configured.
 *
 * <p>Previously the method silently fell back to a hardcoded default key, breaking
 * multi-pod scaling (each pod derived the same key only by coincidence in dev) and
 * corrupting data after pod restart in production (random key per restart).</p>
 *
 * <p>Uses {@link ApplicationContextRunner} to assert bean creation failure on missing
 * password (real bean wiring, no mocks).</p>
 */
class SecurityAutoConfigurationFailFastTest {

    private final ApplicationContextRunner containerRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class))
        .withPropertyValues(
            "payu.security.enabled=true",
            "payu.security.encryption-enabled=true",
            "spring.profiles.active=container"
            // NOTE: deliberately no payu.security.encryption.password
        );

    @Test
    void shouldFailFastWhenPasswordMissingInContainerProfile() {
        containerRunner.run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure())
                .hasStackTraceContaining("encryption.password");
        });
    }

    @Test
    void shouldStartWhenPasswordProvidedInContainerProfile() {
        containerRunner
            .withPropertyValues(
                "payu.security.encryption.password=test-password-not-empty-12345",
                "payu.security.encryption.salt=test-salt-not-empty-12345")
            .run(ctx -> {
                assertThat(ctx).hasNotFailed();
                assertThat(ctx).hasBean("encryptionService");
            });
    }

    @Test
    void shouldNotFailInDevProfileEvenWithoutPassword() {
        // Dev profile keeps the dev-fallback behaviour for local development.
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class))
            .withPropertyValues(
                "payu.security.enabled=true",
                "payu.security.encryption-enabled=true",
                "spring.profiles.active=dev"
            )
            .run(ctx -> assertThat(ctx).hasNotFailed());
    }
}
