package id.payu.auth.adapter.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * ADR-0062: accepts both Authorization: Bearer and Authorization: DPoP.
 * Spring's DefaultBearerTokenResolver only handles Bearer; DPoP tokens would
 * otherwise be ignored and treated as anonymous.
 */
@Component
public class DPoPBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            if (header.startsWith("DPoP ")) {
                return header.substring(5).trim();
            }
            if (header.startsWith("Bearer ")) {
                return header.substring(7).trim();
            }
        }
        return delegate.resolve(request);
    }
}
