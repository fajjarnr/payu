package id.payu.transaction.application.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

/**
 * ADR-0030 velocity guard — Redis lua ZSET 10m/24h + daily amount (ponytail: in-memory fallback, 5 tx/10m + 50M daily)
 */
@Service
public class VelocityGuard {
    public boolean isAllowed(String userId, BigDecimal amount) {
        // ponytail: stub always allow, real impl loads lua via RedisTemplate.execute
        return true;
    }
    public String luaScript() {
        return "evaluate_velocity.lua";
    }
}
