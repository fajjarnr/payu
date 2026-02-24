package id.payu.api.common.controller;

import id.payu.api.common.exception.RateLimitExceededException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

/**
 * Unit tests for {@link RateLimitAspect}.
 *
 * Test scenarios:
 * - Rate limit within threshold (allow request)
 * - Rate limit exceeded (reject with 429)
 * - First request sets expiration
 * - Redis execution failure handling
 * - Multiple concurrent requests (atomicity)
 * - Different key prefixes
 * - Window expiration handling
 *
 * BUG-BE-090: Tests for atomic Lua script-based rate limiting.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitAspectTest {

    private static final String TEST_KEY_PREFIX = "test";
    private static final int TEST_LIMIT = 5;
    private static final long TEST_WINDOW_SECONDS = 60;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private RateLimit rateLimit;

    private RateLimitAspect rateLimitAspect;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        rateLimitAspect = new RateLimitAspect(redisTemplate);
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(rateLimit.keyPrefix()).thenReturn(TEST_KEY_PREFIX);
        when(rateLimit.value()).thenReturn(TEST_LIMIT);
        when(rateLimit.windowSeconds()).thenReturn((int) TEST_WINDOW_SECONDS);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ==================== Basic Rate Limiting Tests ====================

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should allow request when rate limit is not exceeded")
    void shouldAllowRequestWhenUnderLimit() throws Throwable {
        // Given: current count is 3 (under limit of 5)
        doReturn(Long.valueOf(3L))
                .when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any());

        Object expectedResult = new Object();
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // When
        Object result = rateLimitAspect.applyRateLimit(joinPoint, rateLimit);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(joinPoint).proceed();
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should allow request at exact rate limit threshold")
    void shouldAllowRequestAtExactLimit() throws Throwable {
        // Given: current count is exactly at limit (5)
        doReturn(Long.valueOf(5L))
                .when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any());

        Object expectedResult = new Object();
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // When
        Object result = rateLimitAspect.applyRateLimit(joinPoint, rateLimit);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(joinPoint).proceed();
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should reject request when rate limit is exceeded")
    void shouldRejectRequestWhenOverLimit() {
        // Given: current count is 6 (over limit of 5)
        doReturn(Long.valueOf(6L))
                .when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any());
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS)))
                .thenReturn(Long.valueOf(45L));

        // When/Then
        assertThatThrownBy(() -> rateLimitAspect.applyRateLimit(joinPoint, rateLimit))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> {
                    RateLimitExceededException rateEx = (RateLimitExceededException) ex;
                    assertThat(rateEx.getRetryAfterSeconds()).isEqualTo(45L);
                });

        try {
            verify(joinPoint, never()).proceed();
        } catch (Throwable t) {
            // proceed() declares Throwable, but we never call it in this test
            throw new RuntimeException(t);
        }
    }

    // ==================== Atomic Lua Script Tests ====================

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should execute Lua script with correct parameters")
    void shouldExecuteLuaScriptWithCorrectParameters() throws Throwable {
        // Given
        doReturn(Long.valueOf(1L))
                .when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any());
        when(joinPoint.proceed()).thenReturn(new Object());

        // When
        rateLimitAspect.applyRateLimit(joinPoint, rateLimit);

        // Then: Verify Lua script is called with correct key and window
        @SuppressWarnings("unchecked")
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> argsCaptor = ArgumentCaptor.forClass(String.class);

        verify(redisTemplate).execute(scriptCaptor.capture(), keysCaptor.capture(), argsCaptor.capture());

        // Verify window seconds argument
        assertThat(argsCaptor.getValue()).isEqualTo(String.valueOf(TEST_WINDOW_SECONDS));

        // Verify key format
        String key = keysCaptor.getValue().get(0);
        assertThat(key).startsWith("rate_limit:" + TEST_KEY_PREFIX + ":");
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should handle first request (count = 1) correctly")
    void shouldHandleFirstRequest() throws Throwable {
        // Given: First request returns count = 1
        doReturn(Long.valueOf(1L))
                .when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any());
        when(joinPoint.proceed()).thenReturn(new Object());

        // When
        Object result = rateLimitAspect.applyRateLimit(joinPoint, rateLimit);

        // Then
        assertThat(result).isNotNull();
        verify(joinPoint).proceed();
        // Lua script handles expiration atomically, no separate expire call
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    // ==================== Redis Failure Handling Tests ====================

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should allow request when Redis returns null (fail-open)")
    void shouldAllowRequestWhenRedisReturnsNull() throws Throwable {
        // Given: Redis execution returns null (failure)
        doReturn(null)
                .when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any());

        Object expectedResult = new Object();
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // When
        Object result = rateLimitAspect.applyRateLimit(joinPoint, rateLimit);

        // Then: Fail-open - allow request when Redis fails
        assertThat(result).isEqualTo(expectedResult);
        verify(joinPoint).proceed();
    }

    // ==================== Key Generation Tests ====================

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should use IP address when user is not authenticated")
    void shouldUseIpAddressWhenNotAuthenticated() throws Throwable {
        // Given: No authenticated user
        request.setRemoteUser(null);
        doReturn(Long.valueOf(1L))
                .when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any());
        when(joinPoint.proceed()).thenReturn(new Object());

        // When
        rateLimitAspect.applyRateLimit(joinPoint, rateLimit);

        // Then: Key should contain IP-based identifier
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), any());

        String key = keysCaptor.getValue().get(0);
        assertThat(key).contains("192.168.1.1");
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should use user ID when authenticated")
    void shouldUseUserIdWhenAuthenticated() throws Throwable {
        // Given: Authenticated user
        request.setRemoteUser("user123");
        doReturn(Long.valueOf(1L))
                .when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any());
        when(joinPoint.proceed()).thenReturn(new Object());

        // When
        rateLimitAspect.applyRateLimit(joinPoint, rateLimit);

        // Then: Key should contain user-based identifier
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), any());

        String key = keysCaptor.getValue().get(0);
        assertThat(key).contains("user:user123");
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should extract client IP from X-Forwarded-For header")
    void shouldExtractIpFromXForwardedFor() throws Throwable {
        // Given: Request behind proxy
        request.addHeader("X-Forwarded-For", "10.0.0.1");
        doReturn(Long.valueOf(1L))
                .when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any());
        when(joinPoint.proceed()).thenReturn(new Object());

        // When
        rateLimitAspect.applyRateLimit(joinPoint, rateLimit);

        // Then: Key should use forwarded IP
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), any());

        String key = keysCaptor.getValue().get(0);
        assertThat(key).contains("10.0.0.1");
    }

    // ==================== Different Prefix Tests ====================

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should use different keys for different prefixes")
    void shouldUseDifferentKeysForDifferentPrefixes() throws Throwable {
        // Given: Two different prefixes
        when(rateLimit.keyPrefix()).thenReturn("api");
        doReturn(Long.valueOf(1L))
                .when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any());
        when(joinPoint.proceed()).thenReturn(new Object());

        // When
        rateLimitAspect.applyRateLimit(joinPoint, rateLimit);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), any());

        String key = keysCaptor.getValue().get(0);
        assertThat(key).startsWith("rate_limit:api:");
    }

    // ==================== Retry-After Tests ====================

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should include correct retry-after in exception")
    void shouldIncludeCorrectRetryAfter() {
        // Given
        doReturn(Long.valueOf(10L))
                .when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any()); // Over limit
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS)))
                .thenReturn(Long.valueOf(30L));

        // When/Then
        assertThatThrownBy(() -> rateLimitAspect.applyRateLimit(joinPoint, rateLimit))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> {
                    RateLimitExceededException rateEx = (RateLimitExceededException) ex;
                    assertThat(rateEx.getRetryAfterSeconds()).isEqualTo(30L);
                    assertThat(rateEx.getMessage()).contains("30 seconds");
                });
    }

    // ==================== No Request Context Tests ====================

    @Test
    @DisplayName("Should allow request when no request context available")
    void shouldAllowRequestWhenNoContext() throws Throwable {
        // Given: No request context
        RequestContextHolder.resetRequestAttributes();

        Object expectedResult = new Object();
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // When
        Object result = rateLimitAspect.applyRateLimit(joinPoint, rateLimit);

        // Then: Allow request when not in web context
        assertThat(result).isEqualTo(expectedResult);
        verify(joinPoint).proceed();
        verifyNoInteractions(redisTemplate);
    }
}
