package id.payu.gateway.adapter.filter;

import id.payu.gateway.adapter.cache.HotRodCacheClient;
import id.payu.gateway.config.GatewayConfig;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdempotencyFilterUnitTest {

    private static final String KEY = "550e8400-e29b-41d4-a716-446655440000";

    private IdempotencyFilter filter;
    private GatewayConfig config;
    private GatewayConfig.IdempotencyConfig idempotencyConfig;
    private HotRodCacheClient cache;

    @BeforeEach
    void setUp() {
        config = mock(GatewayConfig.class);
        idempotencyConfig = mock(GatewayConfig.IdempotencyConfig.class);
        cache = mock(HotRodCacheClient.class);
        when(config.idempotency()).thenReturn(idempotencyConfig);
        when(idempotencyConfig.enabled()).thenReturn(true);
        when(idempotencyConfig.headerName()).thenReturn("X-Idempotency-Key");
        when(idempotencyConfig.legacyHeaderName()).thenReturn("Idempotency-Key");
        when(idempotencyConfig.ttl()).thenReturn(Duration.ofHours(24));
        filter = new IdempotencyFilter();
        filter.config = config;
        filter.cache = cache;
        filter.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    }

    @Test
    void shouldRejectSameKeyWhenRequestBodyDiffers() {
        ContainerRequestContext request = request("/api/v1/payments", "{\"amount\":200}", "user-1");
        when(cache.get("idempotency:" + KEY)).thenReturn(Uni.createFrom().item(
                "{\"status\":200,\"fingerprint\":\"body-from-original-request\",\"body\":\"cached\"}"));
        AtomicReference<Response> aborted = abortResponse(request);

        filter.filter(request);

        assertNotNull(aborted.get());
        assertEquals(409, aborted.get().getStatus());
    }

    @Test
    void shouldReplaySameKeyForCanonicalEquivalentBody() {
        ContainerRequestContext original = request(
                "/api/v1/payments", "{\"amount\":200,\"currency\":\"IDR\"}", "user-1");
        AtomicReference<String> fingerprint = new AtomicReference<>();
        doAnswer(invocation -> {
            if ("idempotency-fingerprint".equals(invocation.getArgument(0))) {
                fingerprint.set((String) invocation.getArgument(1));
            }
            return null;
        }).when(original).setProperty(anyString(), any());
        when(cache.get("idempotency:" + KEY)).thenReturn(Uni.<String>createFrom().nullItem());

        filter.filter(original);

        assertNotNull(fingerprint.get());

        ContainerRequestContext replay = request(
                "/api/v1/payments", "{\"currency\":\"IDR\",\"amount\":200}", "user-1");
        when(cache.get("idempotency:" + KEY)).thenReturn(Uni.createFrom().item(
                "{\"status\":201,\"fingerprint\":\"" + fingerprint.get()
                        + "\",\"body\":\"cached\"}"));
        AtomicReference<Response> aborted = abortResponse(replay);

        filter.filter(replay);

        assertNotNull(aborted.get());
        assertEquals(201, aborted.get().getStatus());
        assertEquals("true", aborted.get().getHeaderString("Idempotency-Replayed"));
    }

    @Test
    void shouldRejectSameKeyWhenPrincipalDiffers() {
        ContainerRequestContext request = request("/api/v1/payments", "{\"amount\":200}", "user-2");
        when(cache.get("idempotency:" + KEY)).thenReturn(Uni.createFrom().item(
                "{\"status\":200,\"fingerprint\":\"user-1-request\",\"body\":\"cached\"}"));
        AtomicReference<Response> aborted = abortResponse(request);

        filter.filter(request);

        assertNotNull(aborted.get());
        assertEquals(409, aborted.get().getStatus());
    }

    @Test
    void shouldRejectSameKeyWhenAccountDiffers() {
        ContainerRequestContext request = request("/api/v1/payments", "{\"amount\":200}", "user-1", "account-2");
        when(cache.get("idempotency:" + KEY)).thenReturn(Uni.createFrom().item(
                "{\"status\":200,\"fingerprint\":\"account-1-request\",\"body\":\"cached\"}"));
        AtomicReference<Response> aborted = abortResponse(request);

        filter.filter(request);

        assertNotNull(aborted.get());
        assertEquals(409, aborted.get().getStatus());
    }

    @Test
    void shouldFailClosedForFinancialRequestWhenCacheIsUnavailable() {
        ContainerRequestContext request = request("/api/v1/payments", "{\"amount\":200}", "user-1");
        when(cache.get(anyString())).thenReturn(Uni.createFrom().failure(new IllegalStateException("redis down")));
        AtomicReference<Response> aborted = abortResponse(request);

        filter.filter(request);

        assertNotNull(aborted.get());
        assertEquals(503, aborted.get().getStatus());
    }

    private ContainerRequestContext request(String path, String body, String principalName) {
        return request(path, body, principalName, null);
    }

    private ContainerRequestContext request(String path, String body, String principalName, String account) {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn(path);
        when(uriInfo.getRequestUri()).thenReturn(URI.create(path));
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeaderString("X-Idempotency-Key")).thenReturn(KEY);
        when(request.getHeaderString("Idempotency-Key")).thenReturn(null);
        when(request.hasEntity()).thenReturn(true);
        when(request.getEntityStream()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        when(request.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn((Principal) () -> principalName);
        when(request.getProperty("tenant-id")).thenReturn("tenant-1");
        when(request.getHeaderString("X-Account-Id")).thenReturn(account);
        return request;
    }

    private AtomicReference<Response> abortResponse(ContainerRequestContext request) {
        AtomicReference<Response> aborted = new AtomicReference<>();
        doAnswer(invocation -> {
            aborted.set(invocation.getArgument(0));
            return null;
        }).when(request).abortWith(org.mockito.ArgumentMatchers.any(Response.class));
        return aborted;
    }
}
