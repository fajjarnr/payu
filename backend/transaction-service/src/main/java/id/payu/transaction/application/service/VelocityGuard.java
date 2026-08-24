package id.payu.transaction.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * ADR-0030 velocity guard — Redis lua ZSET sliding window (10m / 24h) + daily amount
 * accumulator, evaluated atomically by {@code redis/evaluate_velocity.lua}.
 *
 * <p>Keys (ADR-0030 §1): {@code aml:velocity:tx_count:{userId}:10m},
 * {@code aml:velocity:tx_count:{userId}:24h}, {@code aml:velocity:amount:{userId}:24h}.
 * Limits per ADR-0030 §2 Tier 2 defaults baked into the script:
 * 5 tx / 10 min, Rp 50.000.000 daily accumulation.
 *
 * <p>Policy (ADR-0030 Mitigasi & Trade-Offs): Redis unavailability is handled
 * <b>fail-secure</b> — the transfer is denied and a warning is logged, never silently allowed.
 */
@Slf4j
@Service
public class VelocityGuard {

    private static final RedisScript<List<Long>> EVALUATE_VELOCITY = velocityScript();

    @SuppressWarnings("unchecked")
    private static RedisScript<List<Long>> velocityScript() {
        DefaultRedisScript<List<Long>> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/evaluate_velocity.lua"));
        script.setResultType((Class<List<Long>>) (Class<?>) List.class);
        return script;
    }

    private final StringRedisTemplate redisTemplate;

    public VelocityGuard(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically checks and records the transfer against the user's sliding windows.
     *
     * @return true when the transfer is under all velocity/daily limits
     */
    public boolean isAllowed(String userId, BigDecimal amount) {
        try {
            List<Long> result = redisTemplate.execute(
                    EVALUATE_VELOCITY,
                    List.of(
                            "aml:velocity:tx_count:" + userId + ":10m",
                            "aml:velocity:tx_count:" + userId + ":24h",
                            "aml:velocity:amount:" + userId + ":24h"),
                    String.valueOf(Instant.now().getEpochSecond()),
                    amount.toPlainString());
            boolean allowed = result != null && !result.isEmpty()
                    && Long.valueOf(200L).equals(result.get(0));
            if (!allowed) {
                log.warn("AML velocity breach for user {}: {}", userId, result);
            }
            return allowed;
        } catch (RuntimeException e) {
            // ADR-0030 fail-secure: deny on Redis outage
            log.warn("Velocity check unavailable, failing secure (deny) for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    public String luaScript() {
        return "evaluate_velocity.lua";
    }
}
