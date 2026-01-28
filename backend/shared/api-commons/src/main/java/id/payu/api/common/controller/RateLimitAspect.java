package id.payu.api.common.controller;

import id.payu.api.common.exception.RateLimitExceededException;
import id.payu.api.common.constant.ApiConstants;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Aspect for implementing rate limiting on API endpoints.
 * Uses Redis to track request counts per client.
 */
@Aspect
@Component
public class RateLimitAspect {

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimitAspect(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Applies rate limiting to methods annotated with @RateLimit.
     */
    @Around("@annotation(rateLimit)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        String key = buildRateLimitKey(request, rateLimit.keyPrefix());
        int limit = rateLimit.value();
        long windowSeconds = rateLimit.windowSeconds();

        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount == null || currentCount == 1) {
            // First request in window, set expiration
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        if (currentCount > limit) {
            long retryAfter = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            throw new RateLimitExceededException(retryAfter);
        }

        return joinPoint.proceed();
    }

    /**
     * Builds the rate limit key for Redis.
     */
    private String buildRateLimitKey(HttpServletRequest request, String prefix) {
        // Use IP address or user ID for rate limiting
        String identifier = getClientIdentifier(request);
        return String.format("rate_limit:%s:%s:%s",
                prefix,
                identifier,
                System.currentTimeMillis() / 1000 / 60 // Minute-based window
        );
    }

    /**
     * Gets the client identifier for rate limiting.
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get user ID from authentication
        String userId = request.getRemoteUser();
        if (userId != null) {
            return "user:" + userId;
        }

        // Fall back to IP address
        return getClientIpAddress(request);
    }

    /**
     * Gets the client IP address.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Gets the current HTTP request.
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
