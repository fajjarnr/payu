package id.payu.account.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtDecoderConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtDecoderConfig.class)
            .withPropertyValues(
                    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak/realms/payu",
                    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://keycloak/realms/payu/protocol/openid-connect/certs"
            );

    @Test
    void shouldReadStandardSpringResourceServerJwtProperties() {
        contextRunner.run(context -> {
            JwtDecoderConfig config = context.getBean(JwtDecoderConfig.class);

            assertThat(ReflectionTestUtils.getField(config, "oidcIssuerUri"))
                    .isEqualTo("http://keycloak/realms/payu");
            assertThat(ReflectionTestUtils.getField(config, "oidcJwkSetUri"))
                    .isEqualTo("http://keycloak/realms/payu/protocol/openid-connect/certs");
        });
    }
}
