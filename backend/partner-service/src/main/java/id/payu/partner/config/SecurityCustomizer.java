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
                .requestMatchers("/v1/partner/**").permitAll()
                .requestMatchers("/api/v1/partners/callback/**").permitAll()
                .requestMatchers("/pay/**").permitAll();
    }

    private boolean isSnapBiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/v1/partner")
                || uri.startsWith("/v1/partner/")
                || uri.equals("/api/v1/v1/partner")
                || uri.startsWith("/api/v1/v1/partner/");
    }
}
