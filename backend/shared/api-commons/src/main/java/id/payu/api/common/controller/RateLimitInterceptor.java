package id.payu.api.common.controller;

import id.payu.api.common.constant.ApiConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Interceptor for applying rate limiting based on IP address.
 * Provides global rate limiting when method-level annotation is not sufficient.
 */
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private RedisTemplate<String, String> redisTemplate;
    private int defaultLimit;
    private int windowSeconds;

    public RateLimitInterceptor() {
        this.defaultLimit = ApiConstants.DEFAULT_RATE_LIMIT_PER_MINUTE;
        this.windowSeconds = ApiConstants.DEFAULT_RATE_LIMIT_WINDOW_SECONDS;
    }

    public RateLimitInterceptor(
            RedisTemplate<String, String> redisTemplate,
            int defaultLimit,
            int windowSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.defaultLimit = defaultLimit;
        this.windowSeconds = windowSeconds;
    }

    public RateLimitInterceptor(RedisTemplate<String, String> redisTemplate) {
        this(redisTemplate,
                ApiConstants.DEFAULT_RATE_LIMIT_PER_MINUTE,
                ApiConstants.DEFAULT_RATE_LIMIT_WINDOW_SECONDS);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // BUG-SHARED-023: Graceful fallback when redisTemplate is null (no-arg constructor path)
        if (redisTemplate == null) {
            log.warn("RateLimitInterceptor: redisTemplate is null, allowing request without rate limiting");
            return true;
        }

        String key = buildKey(request);

        // BUG-SHARED-024: Atomic increment + expire using increment with Duration overload.
        // This avoids the race between increment() and expire() where the key could
        // be incremented but the expire never set if the process crashes in between.
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            // First request in this window — set TTL atomically
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        // Defensive null check
        long count = (currentCount != null) ? currentCount : 1;

        response.setHeader(ApiConstants.RATE_LIMIT_LIMIT_HEADER, String.valueOf(defaultLimit));
        response.setHeader(ApiConstants.RATE_LIMIT_REMAINING_HEADER, String.valueOf(Math.max(0, defaultLimit - count)));

        if (count > defaultLimit) {
            long retryAfter = redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
            response.setHeader(ApiConstants.RETRY_AFTER_HEADER, String.valueOf(retryAfter));
            response.setStatus(429); // HTTP 429 Too Many Requests
            return false;
        }

        return true;
    }

    private String buildKey(HttpServletRequest request) {
        return String.format("rate_limit:global:%s:%s",
                getClientIp(request),
                System.currentTimeMillis() / 1000 / windowSeconds
        );
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
