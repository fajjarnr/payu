package id.payu.api.common.controller;

import id.payu.api.common.exception.RateLimitExceededException;
import id.payu.cache.service.DistributedAtomicCache;
import java.time.Duration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitAspectTest {

    @Mock
    private DistributedAtomicCache distributedCache;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private RateLimit rateLimit;

    private RateLimitAspect aspect;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect(distributedCache);
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(rateLimit.keyPrefix()).thenReturn("test");
        when(rateLimit.value()).thenReturn(5);
        when(rateLimit.windowSeconds()).thenReturn(60);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldAllowRequestWithinLimitUsingAtomicIncrement() throws Throwable {
        when(distributedCache.increment(anyString(), eq(Duration.ofSeconds(60)))).thenReturn(3L);
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.applyRateLimit(joinPoint, rateLimit)).isEqualTo("ok");
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(distributedCache).increment(key.capture(), eq(Duration.ofSeconds(60)));
        assertThat(key.getValue()).startsWith("rate_limit:test:192.168.1.1:");
    }

    @Test
    void shouldRejectRequestWithHotRodTtlWhenLimitExceeded() {
        when(distributedCache.increment(anyString(), eq(Duration.ofSeconds(60)))).thenReturn(6L);
        when(distributedCache.getRemainingTtlSeconds(anyString())).thenReturn(45L);

        assertThatThrownBy(() -> aspect.applyRateLimit(joinPoint, rateLimit))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(error -> assertThat(((RateLimitExceededException) error).getRetryAfterSeconds())
                        .isEqualTo(45L));
    }

    @Test
    void shouldFailOpenWhenAtomicCacheIsUnavailable() throws Throwable {
        when(distributedCache.increment(anyString(), eq(Duration.ofSeconds(60))))
                .thenThrow(new IllegalStateException("cache unavailable"));
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.applyRateLimit(joinPoint, rateLimit)).isEqualTo("ok");
    }

    @Test
    void shouldUseAuthenticatedUserInRateLimitKey() throws Throwable {
        request.setRemoteUser("user123");
        when(distributedCache.increment(anyString(), eq(Duration.ofSeconds(60)))).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.applyRateLimit(joinPoint, rateLimit);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(distributedCache).increment(key.capture(), eq(Duration.ofSeconds(60)));
        assertThat(key.getValue()).contains("user:user123");
    }

    @Test
    void shouldSkipCacheWithoutRequestContext() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.applyRateLimit(joinPoint, rateLimit)).isEqualTo("ok");
        verifyNoInteractions(distributedCache);
    }
}
