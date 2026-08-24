package id.payu.transaction.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ADR-0030 velocity guard unit tests: lua-driven ZSET sliding window.
 * Policy per ADR-0030 "Mitigasi & Trade-Offs": Redis unavailable is fail-secure -> deny.
 */
@ExtendWith(MockitoExtension.class)
class VelocityGuardTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private VelocityGuard newGuard() {
        return new VelocityGuard(redisTemplate);
    }

    @Test
    void allowsWhenUnderVelocityAndDailyLimits() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn(List.of(200L, 1L, 1L, 0L));

        assertThat(newGuard().isAllowed("user-1", new BigDecimal("500000"))).isTrue();
    }

    @Test
    void blocksWhenBurstLimitExceeded() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn(List.of(429L, 5L, 12L, 1000000L));

        assertThat(newGuard().isAllowed("user-1", new BigDecimal("500000"))).isFalse();
    }

    @Test
    void blocksWhenDailyAmountLimitExceeded() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn(List.of(429L, 2L, 3L, 49000000L));

        assertThat(newGuard().isAllowed("user-1", new BigDecimal("2000000"))).isFalse();
    }

    @Test
    void failsClosedWhenRedisUnavailable() {
        // ADR-0030: circuit breaker with fail-secure mode — deny when Redis is down
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        assertThat(newGuard().isAllowed("user-1", new BigDecimal("500000"))).isFalse();
    }
}
