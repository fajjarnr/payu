package id.payu.auth.interfaces;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/auth/step-up")
public class StepUpController {

    private static final Logger log = LoggerFactory.getLogger(StepUpController.class);
    private final StringRedisTemplate redis;

    public StepUpController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public record ChallengeRequest(String userId, String payloadDigest) {}
    public record ChallengeResponse(String challengeId, long ttlSeconds) {}
    public record VerifyRequest(String challengeId, String pin, String payloadDigest) {}
    public record VerifyResponse(boolean verified) {}

    @PostMapping("/challenge")
    public ResponseEntity<ChallengeResponse> challenge(@RequestBody ChallengeRequest req) {
        if (req.userId() == null || req.payloadDigest() == null) return ResponseEntity.badRequest().build();
        String id = UUID.randomUUID().toString();
        // ponytail: Redis TTL 180s per ADR-0028, fallback to in-memory if Redis unavailable (local without payu-redis)
        try {
            redis.opsForValue().set("stepup:challenge:" + id, req.payloadDigest(), Duration.ofSeconds(180));
        } catch (Exception e) {
            log.warn("Redis unavailable, using in-memory fallback for step-up challenge {}", id);
            InMemoryFallback.put(id, req.payloadDigest());
        }
        log.info("StepUp challenge {} for user {}", id, req.userId());
        return ResponseEntity.ok(new ChallengeResponse(id, 180));
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyResponse> verify(@RequestBody VerifyRequest req) {
        String expected = null;
        try {
            expected = redis.opsForValue().get("stepup:challenge:" + req.challengeId());
        } catch (Exception e) {
            expected = InMemoryFallback.get(req.challengeId());
        }
        if (expected == null) expected = InMemoryFallback.get(req.challengeId());
        if (expected == null) return ResponseEntity.status(404).body(new VerifyResponse(false));
        boolean ok = expected.equals(req.payloadDigest());
        if (ok) {
            try { redis.delete("stepup:challenge:" + req.challengeId()); } catch (Exception ignored) {}
            InMemoryFallback.remove(req.challengeId());
        }
        log.info("StepUp verify {} {}", req.challengeId(), ok ? "ok" : "fail");
        return ResponseEntity.ok(new VerifyResponse(ok));
    }

    // ponytail: minimal in-memory fallback when payu-redis not running locally
    private static class InMemoryFallback {
        private static final java.util.Map<String, String> MAP = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<String, Long> EXPIRY = new java.util.concurrent.ConcurrentHashMap<>();
        static void put(String k, String v) { MAP.put(k, v); EXPIRY.put(k, System.currentTimeMillis() + 180_000); }
        static String get(String k) {
            Long exp = EXPIRY.get(k);
            if (exp != null && System.currentTimeMillis() > exp) { MAP.remove(k); EXPIRY.remove(k); return null; }
            return MAP.get(k);
        }
        static void remove(String k) { MAP.remove(k); EXPIRY.remove(k); }
    }
}
