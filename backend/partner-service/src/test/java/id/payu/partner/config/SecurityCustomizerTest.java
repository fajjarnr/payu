package id.payu.partner.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityCustomizerTest {

    @Test
    void snapBiBearerTokenIsHandledByControllerNotPlatformJwtFilter() {
        BearerTokenResolver resolver = new SecurityCustomizer().partnerBearerTokenResolver();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/partner/payments");
        request.addHeader("Authorization", "Bearer snap-bi-token");

        assertNull(resolver.resolve(request));
    }
}
