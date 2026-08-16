package id.payu.sdk;

import id.payu.sdk.config.PayUConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayUConfigTest {

    @Test
    void builderAppliesDefaults() {
        PayUConfig config = PayUConfig.builder()
                .apiKey("k")
                .apiSecret("s")
                .build();

        assertEquals(PayUEnvironment.SANDBOX, config.getEnvironment());
        assertEquals(30000, config.getTimeout());
        assertTrue(config.isEnableRetries());
        assertEquals(3, config.getMaxRetries());
        assertEquals(PayUEnvironment.SANDBOX.getBaseUrl(), config.getBaseUrl());
    }

    @Test
    void customBaseUrlOverridesEnvironment() {
        PayUConfig config = PayUConfig.builder()
                .apiKey("k")
                .apiSecret("s")
                .baseUrl("https://localhost:8080")
                .build();

        assertEquals("https://localhost:8080", config.getBaseUrl());
    }

    @Test
    void missingApiKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> PayUConfig.builder().apiSecret("s").build());
    }

    @Test
    void missingApiSecretThrows() {
        assertThrows(IllegalArgumentException.class, () -> PayUConfig.builder().apiKey("k").build());
    }
}
