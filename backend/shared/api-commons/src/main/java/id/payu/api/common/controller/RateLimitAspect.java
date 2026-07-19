package id.payu.api.common.controller;

import id.payu.api.common.exception.RateLimitExceededException;
import id.payu.api.common.constant.ApiConstants;
import id.payu.cache.service.DistributedAtomicCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

/**
 * Aspect for implementing rate limiting on API endpoints.
 * Uses the shared atomic cache port to track request counts per client.
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final DistributedAtomicCache distributedCache;

    public RateLimitAspect(DistributedAtomicCache distributedCache) {
        this.distributedCache = distributedCache;
    }

    /**
     * Applies rate limiting to methods annotated with @RateLimit.
     * Fails open if the distributed cache is unavailable.
     */
    @Around("@annotation(rateLimit)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        String key = buildRateLimitKey(request, rateLimit.keyPrefix(), rateLimit.windowSeconds());
        int limit = rateLimit.value();
        long windowSeconds = rateLimit.windowSeconds();

        try {
            long currentCount = distributedCache.increment(key, Duration.ofSeconds(windowSeconds));

            if (currentCount > limit) {
                long retryAfter = distributedCache.getRemainingTtlSeconds(key);
                throw new RateLimitExceededException(retryAfter);
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Distributed cache unavailable for rate limiting, allowing request: {}", e.getMessage());
        }

        return joinPoint.proceed();
    }

    /**
     * Builds the rate limit key for Redis.
     */
    private String buildRateLimitKey(HttpServletRequest request, String prefix, long windowSeconds) {
        // Use IP address or user ID for rate limiting
        String identifier = getClientIdentifier(request);
        return String.format("rate_limit:%s:%s:%s",
                prefix,
                identifier,
                System.currentTimeMillis() / 1000 / windowSeconds // Window based on annotation parameter
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
