package id.payu.gateway.domain.vo;

import java.time.Duration;

/**
 * Value Object representing rate limit configuration.
 * Immutable and thread-safe.
 */
public record RateLimit(int requestsPerMinute, int requestsPerHour, int requestsPerDay) {

    public RateLimit {
        if (requestsPerMinute < 0) {
            throw new IllegalArgumentException("Requests per minute cannot be negative");
        }
        if (requestsPerHour < 0) {
            throw new IllegalArgumentException("Requests per hour cannot be negative");
        }
        if (requestsPerDay < 0) {
            throw new IllegalArgumentException("Requests per day cannot be negative");
        }
    }

    public static RateLimit of(int requestsPerMinute, int requestsPerHour, int requestsPerDay) {
        return new RateLimit(requestsPerMinute, requestsPerHour, requestsPerDay);
    }

    public static RateLimit defaultLimits() {
        return new RateLimit(60, 1000, 10000);
    }

    public static RateLimit unlimited() {
        return new RateLimit(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public static RateLimit strict() {
        return new RateLimit(10, 100, 500);
    }

    public boolean isExceeded(long requestsInWindow, Duration window) {
        long windowSeconds = window.getSeconds();
        if (windowSeconds <= 60) {
            return requestsInWindow > requestsPerMinute;
        } else if (windowSeconds <= 3600) {
            return requestsInWindow > requestsPerHour;
        } else {
            return requestsInWindow > requestsPerDay;
        }
    }

    public int getLimitForWindow(Duration window) {
        long windowSeconds = window.getSeconds();
        if (windowSeconds <= 60) {
            return requestsPerMinute;
        } else if (windowSeconds <= 3600) {
            return requestsPerHour;
        } else {
            return requestsPerDay;
        }
    }
}
