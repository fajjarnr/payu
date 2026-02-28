package id.payu.gateway.domain.vo;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RateLimit value object.
 */
class RateLimitTest {

    @Test
    void shouldCreateRateLimit() {
        RateLimit limit = RateLimit.of(60, 1000, 10000);

        assertEquals(60, limit.requestsPerMinute());
        assertEquals(1000, limit.requestsPerHour());
        assertEquals(10000, limit.requestsPerDay());
    }

    @Test
    void shouldCreateDefaultLimits() {
        RateLimit limit = RateLimit.defaultLimits();

        assertEquals(60, limit.requestsPerMinute());
        assertEquals(1000, limit.requestsPerHour());
        assertEquals(10000, limit.requestsPerDay());
    }

    @Test
    void shouldCreateUnlimited() {
        RateLimit limit = RateLimit.unlimited();

        assertEquals(Integer.MAX_VALUE, limit.requestsPerMinute());
        assertEquals(Integer.MAX_VALUE, limit.requestsPerHour());
        assertEquals(Integer.MAX_VALUE, limit.requestsPerDay());
    }

    @Test
    void shouldCreateStrictLimits() {
        RateLimit limit = RateLimit.strict();

        assertEquals(10, limit.requestsPerMinute());
        assertEquals(100, limit.requestsPerHour());
        assertEquals(500, limit.requestsPerDay());
    }

    @Test
    void shouldNotAllowNegativeRequestsPerMinute() {
        assertThrows(IllegalArgumentException.class, () ->
            RateLimit.of(-1, 1000, 10000));
    }

    @Test
    void shouldNotAllowNegativeRequestsPerHour() {
        assertThrows(IllegalArgumentException.class, () ->
            RateLimit.of(60, -1, 10000));
    }

    @Test
    void shouldNotAllowNegativeRequestsPerDay() {
        assertThrows(IllegalArgumentException.class, () ->
            RateLimit.of(60, 1000, -1));
    }

    @Test
    void shouldCheckMinuteWindowExceeded() {
        RateLimit limit = RateLimit.of(60, 1000, 10000);

        assertTrue(limit.isExceeded(61, Duration.ofSeconds(30)));
        assertTrue(limit.isExceeded(61, Duration.ofMinutes(1)));
        assertFalse(limit.isExceeded(60, Duration.ofMinutes(1)));
    }

    @Test
    void shouldCheckHourWindowExceeded() {
        RateLimit limit = RateLimit.of(60, 1000, 10000);

        assertTrue(limit.isExceeded(1001, Duration.ofMinutes(30)));
        assertTrue(limit.isExceeded(1001, Duration.ofHours(1)));
        assertFalse(limit.isExceeded(1000, Duration.ofHours(1)));
    }

    @Test
    void shouldCheckDayWindowExceeded() {
        RateLimit limit = RateLimit.of(60, 1000, 10000);

        assertTrue(limit.isExceeded(10001, Duration.ofHours(12)));
        assertTrue(limit.isExceeded(10001, Duration.ofDays(1)));
        assertFalse(limit.isExceeded(10000, Duration.ofDays(1)));
    }

    @Test
    void shouldGetLimitForMinuteWindow() {
        RateLimit limit = RateLimit.of(60, 1000, 10000);

        assertEquals(60, limit.getLimitForWindow(Duration.ofSeconds(30)));
        assertEquals(60, limit.getLimitForWindow(Duration.ofMinutes(1)));
    }

    @Test
    void shouldGetLimitForHourWindow() {
        RateLimit limit = RateLimit.of(60, 1000, 10000);

        assertEquals(1000, limit.getLimitForWindow(Duration.ofMinutes(30)));
        assertEquals(1000, limit.getLimitForWindow(Duration.ofHours(1)));
    }

    @Test
    void shouldGetLimitForDayWindow() {
        RateLimit limit = RateLimit.of(60, 1000, 10000);

        assertEquals(10000, limit.getLimitForWindow(Duration.ofHours(12)));
        assertEquals(10000, limit.getLimitForWindow(Duration.ofDays(1)));
    }
}
