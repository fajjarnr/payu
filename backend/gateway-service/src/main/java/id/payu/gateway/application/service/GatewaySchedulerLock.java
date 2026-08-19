package id.payu.gateway.application.service;

import id.payu.gateway.adapter.cache.HotRodCacheClient;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;

/**
 * ADR-0042: lightweight distributed lock for Quarkus @Scheduled (gateway).
 * Uses HotRod tryLock (putIfAbsent) — ponytail: global lock per scheduler name,
 * TTL = lockAtMostFor; upgrade to ShedLock JdbcTemplate(usingDbTime) if DB lock needed
 */
@ApplicationScoped
public class GatewaySchedulerLock {

    @Inject
    HotRodCacheClient cache;

    public boolean tryAcquire(String name, Duration lockAtMostFor) {
        String key = "shedlock:" + name;
        try {
            Boolean locked = cache.tryLock(key, lockAtMostFor).await().atMost(Duration.ofSeconds(2));
            if (Boolean.TRUE.equals(locked)) {
                Log.debugf("Acquired scheduler lock %s", name);
                return true;
            }
            Log.debugf("Skipped scheduler %s — lock held elsewhere", name);
            return false;
        } catch (Exception e) {
            Log.warnf("Failed to acquire lock %s, running anyway: %s", name, e.getMessage());
            return true; // fail-open for now — ponytail: fail-closed if lock is safety-critical
        }
    }
}
