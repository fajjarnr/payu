package id.payu.partner.config;

import id.payu.security.config.SecurityConfigurerCustomizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

@Configuration
public class SecurityCustomizer {

    @Bean
    public BearerTokenResolver partnerBearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return request -> isSnapBiRequest(request) ? null : delegate.resolve(request);
    }

    @Bean
    public SecurityConfigurerCustomizer partnerSecurityCustomizer() {
        return auth -> auth
                // SNAP-BI validates client-key HMAC and SNAP access tokens in SnapBiController.
                // Both the legacy /v1/partner and the standard /v1.0 taxonomy are public
                // contract paths (SNAP-PATH-001).
                .requestMatchers("/v1/partner/**").permitAll()
                .requestMatchers("/v1.0/**").permitAll()
                .requestMatchers("/api/v1/partners/callback/**").permitAll()
                // PARTNER-004: public health probe is intentionally unauthenticated.
                .requestMatchers("/partners/public/health").permitAll()
                .requestMatchers("/pay/**").permitAll();
    }

    private boolean isSnapBiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/v1/partner")
                || uri.startsWith("/v1/partner/")
                || uri.equals("/v1.0")
                || uri.startsWith("/v1.0/")
                || uri.equals("/api/v1/v1/partner")
                || uri.startsWith("/api/v1/v1/partner/");
    }
}
