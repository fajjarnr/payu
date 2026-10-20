package id.payu.grpc.starter.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GRPC-AUTH-001: the client interceptor must forward the caller's bearer token
 * on every outgoing gRPC call. Spring Security's resource-server filter
 * authenticates with {@link JwtAuthenticationToken}, whose credentials are the
 * {@link Jwt} object — not a String — so the token was silently dropped and
 * every authenticated service-to-service call arrived as UNAUTHENTICATED.
 */
class GrpcAuthInterceptorClientTokenTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static Jwt jwt(String tokenValue, String subject) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    /** Runs the interceptor against a capturing channel and returns the outbound metadata. */
    @SuppressWarnings("unchecked")
    private Metadata outboundHeaders() {
        AtomicReference<Metadata> captured = new AtomicReference<>(new Metadata());
        ClientCall<Object, Object> delegate = mock(ClientCall.class);
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(1));
            return null;
        }).when(delegate).start(any(), any());

        Channel channel = mock(Channel.class);
        when(channel.newCall(any(MethodDescriptor.class), any(CallOptions.class))).thenReturn(delegate);

        MethodDescriptor<Object, Object> method = mock(MethodDescriptor.class);
        new GrpcAuthInterceptor.ClientInterceptor()
                .interceptCall(method, CallOptions.DEFAULT, channel)
                .start(mock(ClientCall.Listener.class), new Metadata());

        return captured.get();
    }

    @Test
    void attachesBearerTokenForJwtAuthenticationToken() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt("jwt-token-value", "user-1"), List.of()));

        Metadata headers = outboundHeaders();

        assertThat(headers.get(GrpcAuthInterceptor.AUTHORIZATION_KEY)).isEqualTo("Bearer jwt-token-value");
    }

    @Test
    void attachesBearerTokenForStringCredentials() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", "plain-token", List.of()));

        Metadata headers = outboundHeaders();

        assertThat(headers.get(GrpcAuthInterceptor.AUTHORIZATION_KEY)).isEqualTo("Bearer plain-token");
    }

    @Test
    void attachesUserIdAndRolesAlongsideTheToken() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt("jwt-token-value", "user-1"),
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("read:transaction"))));

        Metadata headers = outboundHeaders();

        assertThat(headers.get(GrpcAuthInterceptor.USER_ID_KEY)).isEqualTo("user-1");
        assertThat(headers.get(GrpcAuthInterceptor.ROLES_KEY)).isEqualTo("read:transaction");
    }

    @Test
    void sendsNoTokenWhenNobodyIsAuthenticated() {
        SecurityContextHolder.clearContext();

        Metadata headers = outboundHeaders();

        assertThat(headers.get(GrpcAuthInterceptor.AUTHORIZATION_KEY)).isNull();
    }

    @Test
    void sendsNoTokenForAnonymousAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.AnonymousAuthenticationToken(
                        "key", "anonymousUser",
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        Metadata headers = outboundHeaders();

        assertThat(headers.get(GrpcAuthInterceptor.AUTHORIZATION_KEY)).isNull();
    }
}
