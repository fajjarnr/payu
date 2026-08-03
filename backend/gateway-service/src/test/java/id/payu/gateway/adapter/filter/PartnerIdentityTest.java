package id.payu.gateway.adapter.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class PartnerIdentityTest {

    @Test
    void demoApiKeysAreNotMappedToARealPartner() {
        assertNull(PartnerRateLimitFilter.derivePartnerFromApiKey("demo_token"));
        assertNull(ApiAnalyticsFilter.derivePartnerFromApiKey("demo_token"));
    }
}
