package id.payu.gateway.domain.vo;

import java.time.Duration;
import java.time.Instant;

/**
 * Value Object representing a time window for rate limiting.
 * Immutable and thread-safe.
 */
public record TimeWindow(Instant start, Instant end) {

    public TimeWindow {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end times cannot be null");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
    }

    public static TimeWindow of(Duration duration) {
        Instant now = Instant.now();
        return new TimeWindow(now.minus(duration), now);
    }

    public static TimeWindow ofMinutes(int minutes) {
        return of(Duration.ofMinutes(minutes));
    }

    public static TimeWindow ofHours(int hours) {
        return of(Duration.ofHours(hours));
    }

    public static TimeWindow ofDays(int days) {
        return of(Duration.ofDays(days));
    }

    public boolean contains(Instant instant) {
        return !instant.isBefore(start) && !instant.isAfter(end);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public long toSeconds() {
        return duration().getSeconds();
    }
}
