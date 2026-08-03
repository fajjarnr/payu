package id.payu.commons.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdempotencyInterceptorTest {

    private static final String KEY = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    void shouldBindFingerprintToBodyAndPrincipal() throws Exception {
        IdempotencyService service = mock(IdempotencyService.class);
        IdempotencyInterceptor interceptor = new IdempotencyInterceptor(service, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
        request.addHeader("Idempotency-Key", KEY);
        request.setContent("{ \"amount\": 100, \"account\": \"ACC-1\" }".getBytes());
        request.setUserPrincipal((Principal) () -> "user-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Method method = TestController.class.getMethod("create", Map.class);
        HandlerMethod handler = new HandlerMethod(new TestController(), method);
        AtomicBoolean handled = new AtomicBoolean();

        when(service.get(eq(KEY), any())).thenAnswer(invocation -> {
            Object requestFingerprint = invocation.getArgument(1);
            assertThat(requestFingerprint).isInstanceOf(Map.class);
            Map<?, ?> fingerprint = (Map<?, ?>) requestFingerprint;
            assertThat(fingerprint.get("body")).isEqualTo("{\"account\":\"ACC-1\",\"amount\":100}");
            assertThat(((Map<?, ?>) fingerprint.get("identity")).get("principal")).isEqualTo("user-1");
            return Optional.of(IdempotencyEntry.completed(KEY, "unused", 200, "{}"));
        });

        new IdempotencyRequestBodyFilter().doFilter(request, response,
                (wrapped, ignored) -> {
                    try {
                        handled.set(!interceptor.preHandle((HttpServletRequest) wrapped, response, handler));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        assertThat(handled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @RestController
    static class TestController {
        @PostMapping
        @Idempotent
        public void create(@RequestBody Map<String, Object> request) {
        }
    }
}
