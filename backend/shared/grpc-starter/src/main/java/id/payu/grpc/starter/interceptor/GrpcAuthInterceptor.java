package id.payu.grpc.starter.interceptor;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC interceptor for authentication and authorization.
 * Validates JWT tokens from incoming requests and propagates auth context.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Slf4j
@Component
public class GrpcAuthInterceptor {

    public static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> USER_ID_KEY =
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> TENANT_ID_KEY =
            Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> ROLES_KEY =
            Metadata.Key.of("x-roles", Metadata.ASCII_STRING_MARSHALLER);

    private static final String BEARER_PREFIX = "Bearer ";

    // gRPC Context key for propagating authentication across threads
    public static final Context.Key<Authentication> AUTH_CONTEXT_KEY = Context.key("grpc-authentication");

    /**
     * Server interceptor that validates JWT tokens from incoming gRPC calls.
     */
    public static class ServerInterceptor implements io.grpc.ServerInterceptor {

        private final JwtDecoder jwtDecoder;
        private final boolean requireToken;

        public ServerInterceptor(JwtDecoder jwtDecoder) {
            this(jwtDecoder, false);
        }

        public ServerInterceptor(JwtDecoder jwtDecoder, boolean requireToken) {
            this.jwtDecoder = jwtDecoder;
            this.requireToken = requireToken;
        }

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call,
                Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {

            String authHeader = headers.get(AUTHORIZATION_KEY);
            String userId = headers.get(USER_ID_KEY);
            String tenantId = headers.get(TENANT_ID_KEY);
            String rolesHeader = headers.get(ROLES_KEY);

            try {
                Authentication authentication = null;

                if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                    String token = authHeader.substring(BEARER_PREFIX.length());
                    Jwt jwt = jwtDecoder.decode(token);

                    // Extract claims
                    String subject = jwt.getSubject();
                    if (userId == null) {
                        userId = subject;
                    }

                    // Extract roles from JWT or header
                    List<String> roles = extractRoles(jwt, rolesHeader);

                    Collection<? extends GrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // Create authentication token
                    authentication = new UsernamePasswordAuthenticationToken(
                            subject,
                            token,
                            authorities
                    );

                    // Set tenant context if available
                    if (tenantId != null) {
                        log.debug("Setting tenant context: {}", tenantId);
                    }

                    log.debug("Authenticated gRPC call - user: {}, tenant: {}, method: {}",
                            userId, tenantId, call.getMethodDescriptor().getFullMethodName());

                } else if (requireToken) {
                    // GRPC-014: enforcement mode — reject calls without a token
                    log.warn("Rejected anonymous gRPC call - method: {}",
                            call.getMethodDescriptor().getFullMethodName());
                    call.close(Status.UNAUTHENTICATED
                            .withDescription("Authentication required"), headers);
                    return new ServerCall.Listener<ReqT>() {};
                } else {
                    // Allow anonymous access (some endpoints may be public)
                    log.debug("Anonymous gRPC call - method: {}",
                            call.getMethodDescriptor().getFullMethodName());
                }

                // Propagate authentication via gRPC Context (thread-safe)
                io.grpc.Context ctx = io.grpc.Context.current();
                if (authentication != null) {
                    ctx = ctx.withValue(AUTH_CONTEXT_KEY, authentication);
                }

                final Authentication auth = authentication;

                ServerCall.Listener<ReqT> listener = Contexts.interceptCall(ctx, call, headers, next);

                return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {

                    private void setSecurityContext() {
                        Authentication ctxAuth = AUTH_CONTEXT_KEY.get();
                        if (ctxAuth != null) {
                            SecurityContextHolder.getContext().setAuthentication(ctxAuth);
                        }
                    }

                    private void clearSecurityContext() {
                        SecurityContextHolder.clearContext();
                    }

                    @Override
                    public void onMessage(ReqT message) {
                        setSecurityContext();
                        try {
                            super.onMessage(message);
                        } finally {
                            clearSecurityContext();
                        }
                    }

                    @Override
                    public void onHalfClose() {
                        setSecurityContext();
                        try {
                            super.onHalfClose();
                        } finally {
                            clearSecurityContext();
                        }
                    }

                    @Override
                    public void onReady() {
                        setSecurityContext();
                        try {
                            super.onReady();
                        } finally {
                            clearSecurityContext();
                        }
                    }

                    @Override
                    public void onComplete() {
                        setSecurityContext();
                        try {
                            super.onComplete();
                        } finally {
                            clearSecurityContext();
                        }
                    }

                    @Override
                    public void onCancel() {
                        setSecurityContext();
                        try {
                            super.onCancel();
                        } finally {
                            clearSecurityContext();
                        }
                    }
                };

            } catch (JwtException e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                call.close(Status.UNAUTHENTICATED.withDescription("Invalid token"), headers);
                return new ServerCall.Listener<ReqT>() {};
            } catch (Exception e) {
                log.error("Authentication error: {}", e.getMessage(), e);
                call.close(Status.INTERNAL.withDescription("Authentication error"), headers);
                return new ServerCall.Listener<ReqT>() {};
            }
        }

        private List<String> extractRoles(Jwt jwt, String rolesHeader) {
            if (rolesHeader != null && !rolesHeader.isEmpty()) {
                return Arrays.asList(rolesHeader.split(","));
            }

            // Try to extract from JWT claims
            Object rolesClaim = jwt.getClaim("roles");
            if (rolesClaim instanceof List) {
                return ((List<?>) rolesClaim).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
            }

            Object scopeClaim = jwt.getClaim("scope");
            if (scopeClaim instanceof String) {
                return Arrays.asList(((String) scopeClaim).split(" "));
            }

            return Collections.emptyList();
        }
    }

    /**
     * Client interceptor that adds JWT token to outgoing gRPC calls.
     */
    public static class ClientInterceptor implements io.grpc.ClientInterceptor {

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions,
                Channel next) {

            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {

                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    // Get current authentication from SecurityContext
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                    if (authentication != null && authentication.getCredentials() instanceof String) {
                        String token = (String) authentication.getCredentials();
                        headers.put(AUTHORIZATION_KEY, BEARER_PREFIX + token);

                        // Add user ID if available
                        if (authentication.getName() != null) {
                            headers.put(USER_ID_KEY, authentication.getName());
                        }

                        // Add roles
                        String roles = authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.joining(","));
                        if (!roles.isEmpty()) {
                            headers.put(ROLES_KEY, roles);
                        }
                    }

                    super.start(responseListener, headers);
                }
            };
        }
    }
}
