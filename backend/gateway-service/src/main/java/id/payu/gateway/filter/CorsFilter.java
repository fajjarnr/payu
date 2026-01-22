package id.payu.gateway.filter;

import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
@ApplicationScoped
public class CorsFilter implements ContainerResponseFilter {

    private static final String ORIGIN_HEADER = "Origin";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    @Inject
    GatewayConfig config;

    private Set<String> allowedOrigins;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
    private List<String> exposedHeaders;

    @PostConstruct
    void init() {
        GatewayConfig.CorsConfig corsConfig = config.cors();
        
        this.allowedOrigins = Arrays.stream(corsConfig.allowedOrigins().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
        
        this.allowedMethods = Arrays.stream(corsConfig.allowedMethods().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        
        this.allowedHeaders = Arrays.stream(corsConfig.allowedHeaders().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        
        this.exposedHeaders = Arrays.stream(corsConfig.exposedHeaders().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (!config.cors().enabled()) {
            return;
        }

        String origin = requestContext.getHeaderString(ORIGIN_HEADER);
        
        if (origin == null || origin.isBlank()) {
            return;
        }

        if (!isOriginAllowed(origin)) {
            Log.warnf("CORS: Origin not allowed: %s", origin);
            responseContext.getHeaders().remove(ACCESS_CONTROL_ALLOW_ORIGIN);
            return;
        }

        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        headers.putSingle(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        
        if (!allowedMethods.isEmpty()) {
            headers.putSingle(ACCESS_CONTROL_ALLOW_METHODS, String.join(", ", allowedMethods));
        }
        
        if (!allowedHeaders.isEmpty()) {
            headers.putSingle(ACCESS_CONTROL_ALLOW_HEADERS, String.join(", ", allowedHeaders));
        }
        
        if (!exposedHeaders.isEmpty()) {
            headers.putSingle(ACCESS_CONTROL_EXPOSE_HEADERS, String.join(", ", exposedHeaders));
        }
        
        if (config.cors().allowCredentials()) {
            headers.putSingle(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
        
        headers.putSingle(ACCESS_CONTROL_MAX_AGE, String.valueOf(config.cors().maxAge()));
    }

    private boolean isOriginAllowed(String origin) {
        if (allowedOrigins.isEmpty()) {
            return false;
        }
        
        for (String allowedOrigin : allowedOrigins) {
            if (allowedOrigin.equals("*")) {
                return true;
            }
            
            if (allowedOrigin.equals(origin)) {
                return true;
            }
            
            if (allowedOrigin.endsWith("*")) {
                String prefix = allowedOrigin.substring(0, allowedOrigin.length() - 1);
                if (origin.startsWith(prefix)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
