package id.payu.api.common.controller;

import id.payu.api.common.exception.RateLimitExceededException;
import id.payu.api.common.exception.ServiceUnavailableException;
import id.payu.cache.service.DistributedAtomicCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * LOGIN-004: the aspect must honor {@code requests} (not {@code value}), key by
 * account when authenticated (per-IP otherwise), and fail CLOSED when the
 * counting cache is unavailable.
 */
@DisplayName("RateLimitAspect")
class RateLimitAspectTest {

    private DistributedAtomicCache cache;
    private RateLimitAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() throws Throwable {
        cache = mock(DistributedAtomicCache.class);
        aspect = new RateLimitAspect(cache);
        joinPoint = mock(ProceedingJoinPoint.class);
        given(joinPoint.proceed()).willReturn("ok");
        request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("enforces the requests alias, not the value attribute")
    void usesRequestsAlias() throws Throwable {
        given(cache.increment(any(String.class), any(Duration.class))).willReturn(1L, 2L, 3L);

        aspect.applyRateLimit(joinPoint, probe(2, 999, 60));
        aspect.applyRateLimit(joinPoint, probe(2, 999, 60));

        assertThatThrownBy(() -> aspect.applyRateLimit(joinPoint, probe(2, 999, 60)))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("fails closed with 503 when the counting cache is unavailable")
    void failsClosedWhenCacheDown() throws Throwable {
        given(cache.increment(any(String.class), any(Duration.class)))
                .willThrow(new IllegalStateException("cache down"));

        assertThatThrownBy(() -> aspect.applyRateLimit(joinPoint, probe(10, 10, 60)))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("keys by account subject when authenticated")
    void keysByAccountWhenAuthenticated() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("sub-123", null, java.util.List.of()));
        given(cache.increment(any(String.class), any(Duration.class))).willReturn(1L);

        aspect.applyRateLimit(joinPoint, probe(10, 10, 60));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cache).increment(keyCaptor.capture(), any(Duration.class));
        assertThat(keyCaptor.getValue()).contains("account:sub-123");
    }

    @Test
    @DisplayName("keys by client IP when unauthenticated")
    void keysByIpWhenAnonymous() throws Throwable {
        given(cache.increment(any(String.class), any(Duration.class))).willReturn(1L);

        aspect.applyRateLimit(joinPoint, probe(10, 10, 60));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cache).increment(keyCaptor.capture(), any(Duration.class));
        assertThat(keyCaptor.getValue()).contains("10.0.0.7");
    }

    private RateLimit probe(int requests, int value, int windowSeconds) {
        return new RateLimit() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RateLimit.class;
            }

            @Override
            public int value() {
                return value;
            }

            @Override
            public int requests() {
                return requests;
            }

            @Override
            public int windowSeconds() {
                return windowSeconds;
            }

            @Override
            public String keyPrefix() {
                return "login";
            }
        };
    }
}
