package id.payu.gateway.security;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GAP-23: Verify that the removed {@code quarkus.oidc.tls.verification}
 * setting is not retained in {@code gateway-service/src/main/resources/application.yaml}.
 *
 * <p>Quarkus now manages OIDC TLS through the TLS registry. Retaining the legacy
 * setting emits a startup warning and does not configure an HTTPS trust store.</p>
 *
 * <p>This test asserts YAML-resource wiring only (no Quarkus boot, no AssertJ dep —
 * plain JUnit to avoid pulling transitive shared-starter deps). The actual TLS
 * enforcement is verified by Quarkus at runtime in dev/prod.</p>
 */
class OidcTlsVerificationTest {

    @Test
    void deprecatedOidcTlsVerificationMustBeAbsent() throws Exception {
        String yaml = loadApplicationYaml();
        assertFalse(yaml.contains("verification:"),
            "Use Quarkus TLS registry for HTTPS OIDC instead of deprecated "
                + "quarkus.oidc.tls.verification");
    }

    @Test
    void oidcTlsVerificationMustNotBeNone() throws Exception {
        String yaml = loadApplicationYaml();
        assertFalse(yaml.contains("verification: none"),
            "GAP-23 guard: regression check — must NOT be 'none'");
    }

    private String loadApplicationYaml() throws Exception {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("application.yaml")) {
            assertNotNull(in, "gateway-service application.yaml must be on classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
