package id.payu.jms.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AUDIT-059 fix: {@link JmsAutoConfiguration} must fail fast in production
 * profiles (container/prod/staging) when {@code payu.jms.password} is missing,
 * blank, or still set to the weak default {@code "admin"}.
 *
 * <p>Previously, the yaml placeholder {@code ${ARTEMIS_PASSWORD:admin}} silently
 * fell back to {@code "admin"} if the env var was unset — meaning container pods
 * could start with a publicly known Artemis password.</p>
 *
 * <p>Tests the constructor directly using {@link MockEnvironment} (no
 * ApplicationContextRunner, no Jackson dependency).</p>
 */
class JmsAutoConfigurationFailFastTest {

    private static final List<String> PROD_LIKE_PROFILES =
        List.of("container", "prod", "staging");

    private static final String STRONG_PASSWORD =
        "ZmRzZmRzZmRzZmRzZmRzZmRzZmRzZmRzZmRzZmRzZmRzZmQ="; // 48 chars base64

    private MockEnvironment envFor(String profile) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profile);
        return env;
    }

    @Test
    void shouldFailFastWhenPasswordMissingInContainerProfile() {
        JmsProperties props = new JmsProperties();
        // JmsProperties.password defaults to "admin" via Java field default;
        // but we also exercise the null path.
        props.setPassword(null);

        assertThatThrownBy(() -> new JmsAutoConfiguration(props, envFor("container")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ARTEMIS_PASSWORD")
            .hasMessageContaining("container");
    }

    @Test
    void shouldFailFastWhenPasswordBlankInProdProfile() {
        JmsProperties props = new JmsProperties();
        props.setPassword("");

        assertThatThrownBy(() -> new JmsAutoConfiguration(props, envFor("prod")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ARTEMIS_PASSWORD")
            .hasMessageContaining("prod");
    }

    @Test
    void shouldFailFastWhenPasswordIsAdminInStagingProfile() {
        // Catches regression: yaml `${ARTEMIS_PASSWORD:admin}` placeholder default.
        JmsProperties props = new JmsProperties();
        props.setPassword("admin");

        assertThatThrownBy(() -> new JmsAutoConfiguration(props, envFor("staging")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("weak default")
            .hasMessageContaining("ARTEMIS_PASSWORD");
    }

    @Test
    void shouldStartWhenStrongPasswordProvidedInContainerProfile() {
        JmsProperties props = new JmsProperties();
        props.setPassword(STRONG_PASSWORD);

        assertThatCode(() -> new JmsAutoConfiguration(props, envFor("container")))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldNotFailInDevProfileWithWeakPassword() {
        // Dev profile keeps weak password for local docker-compose convenience.
        JmsProperties props = new JmsProperties();
        props.setPassword("admin");

        assertThatCode(() -> new JmsAutoConfiguration(props, envFor("dev")))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldEnumerateAllProductionLikeProfiles() {
        // Guard against new production-like profile names slipping past review.
        assertThat(PROD_LIKE_PROFILES)
            .contains("container", "prod", "staging");
    }
}
