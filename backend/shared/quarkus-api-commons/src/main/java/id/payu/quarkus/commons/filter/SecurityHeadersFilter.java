package id.payu.quarkus.commons.filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        responseContext.getHeaders().putSingle("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        responseContext.getHeaders().putSingle("X-Frame-Options", "DENY");
        responseContext.getHeaders().putSingle("X-Content-Type-Options", "nosniff");
        responseContext.getHeaders().putSingle("Content-Security-Policy",
                "default-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
        responseContext.getHeaders().putSingle("X-XSS-Protection", "1; mode=block");
        responseContext.getHeaders().putSingle("Referrer-Policy", "no-referrer");
        responseContext.getHeaders().putSingle("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=(), usb=()");

        if (requestContext.getHeaderString("Authorization") != null) {
            responseContext.getHeaders().putSingle("Cache-Control", "no-store, no-cache, must-revalidate, private");
            responseContext.getHeaders().putSingle("Pragma", "no-cache");
            responseContext.getHeaders().putSingle("Expires", "0");
        }
    }
}
