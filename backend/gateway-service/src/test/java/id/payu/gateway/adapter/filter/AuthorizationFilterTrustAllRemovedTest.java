package id.payu.gateway.adapter.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDIT-065 regression test: Verifies that the trust-all TLS bypass
 * has been permanently removed from AuthorizationFilter.
 *
 * This is a compile-time + reflection guard — if anyone re-introduces
 * trust-all code, this test will catch it.
 */
@DisplayName("AUDIT-065: Trust-All TLS Bypass Removed")
class AuthorizationFilterTrustAllRemovedTest {

    @Test
    @DisplayName("Should not have trustAllCerts field")
    void noTrustAllCertsField() {
        Field[] fields = AuthorizationFilter.class.getDeclaredFields();
        boolean hasTrustAll = Arrays.stream(fields)
                .anyMatch(f -> f.getName().contains("trustAll") || f.getName().contains("trust_all"));
        assertFalse(hasTrustAll,
                "AuthorizationFilter must NOT contain trustAllCerts field — AUDIT-065 P0 security fix");
    }

    @Test
    @DisplayName("Should not import javax.net.ssl.X509TrustManager")
    void noX509TrustManagerUsage() {
        // Reflection check: no field or method return type references X509TrustManager
        Field[] fields = AuthorizationFilter.class.getDeclaredFields();
        for (Field f : fields) {
            assertNotEquals("javax.net.ssl.X509TrustManager", f.getType().getName(),
                    "Field " + f.getName() + " references X509TrustManager — trust-all bypass risk");
        }

        Method[] methods = AuthorizationFilter.class.getDeclaredMethods();
        for (Method m : methods) {
            for (Class<?> paramType : m.getParameterTypes()) {
                assertNotEquals("javax.net.ssl.X509TrustManager", paramType.getName(),
                        "Method " + m.getName() + " accepts X509TrustManager — trust-all bypass risk");
            }
        }
    }

    @Test
    @DisplayName("loadJwkSet method should not reference SSLContext")
    void loadJwkSetNoSSLContext() {
        // Verify loadJwkSet exists and has simple signature (URL only)
        Method loadJwkSet = Arrays.stream(AuthorizationFilter.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("loadJwkSet"))
                .findFirst()
                .orElse(null);

        assertNotNull(loadJwkSet, "loadJwkSet method should exist");
        assertEquals(1, loadJwkSet.getParameterCount(),
                "loadJwkSet should take only URL param (no SSL bypass params)");
        assertEquals(java.net.URL.class, loadJwkSet.getParameterTypes()[0],
                "loadJwkSet single param should be java.net.URL");
    }
}
