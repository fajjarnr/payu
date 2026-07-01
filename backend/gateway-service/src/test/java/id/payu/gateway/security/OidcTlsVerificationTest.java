package id.payu.gateway.security;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GAP-23: Verify that {@code quarkus.oidc.tls.verification} is set to {@code required}
 * in {@code gateway-service/src/main/resources/application.yaml}.
 *
 * <p>Previously {@code none} which exposed the gateway to MITM attacks inside the
 * OpenShift cluster when validating tokens with Keycloak.</p>
 *
 * <p>This test asserts YAML-resource wiring only (no Quarkus boot, no AssertJ dep —
 * plain JUnit to avoid pulling transitive shared-starter deps). The actual TLS
 * enforcement is verified by Quarkus at runtime in dev/prod.</p>
 */
class OidcTlsVerificationTest {

    @Test
    void oidcTlsVerificationMustBeRequired() throws Exception {
        String yaml = loadApplicationYaml();
        assertTrue(yaml.contains("verification: required"),
            "GAP-23 fix: quarkus.oidc.tls.verification must be 'required' "
                + "to prevent MITM during Keycloak token validation");
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
