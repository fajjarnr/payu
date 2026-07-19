package id.payu.api.common.controller;

import id.payu.cache.service.DistributedAtomicCache;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitInterceptorTest {

    @Test
    void shouldRejectRequestOverTheLimitWithAtomicCacheTtl() throws Exception {
        DistributedAtomicCache distributedCache = mock(DistributedAtomicCache.class);
        when(distributedCache.increment(anyString(), eq(Duration.ofSeconds(60)))).thenReturn(3L);
        when(distributedCache.getRemainingTtlSeconds(anyString())).thenReturn(40L);
        RateLimitInterceptor interceptor = new RateLimitInterceptor(distributedCache, 2, 60);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(new MockHttpServletRequest(), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("40");
    }
}
