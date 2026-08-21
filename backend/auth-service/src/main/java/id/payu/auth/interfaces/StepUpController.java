package id.payu.auth.interfaces;

import id.payu.auth.adapter.persistence.entity.UserPinEntity;
import id.payu.auth.adapter.persistence.repository.UserPinRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/auth/step-up")
public class StepUpController {

    private static final Logger log = LoggerFactory.getLogger(StepUpController.class);
    private final StringRedisTemplate redis;
    private final UserPinRepository pinRepo;
    private final Argon2PasswordEncoder encoder;

    public StepUpController(StringRedisTemplate redis, UserPinRepository pinRepo, Argon2PasswordEncoder encoder) {
        this.redis = redis;
        this.pinRepo = pinRepo;
        this.encoder = encoder;
    }

    public record ChallengeRequest(String userId, String payloadDigest) {}
    public record ChallengeResponse(String challengeId, long ttlSeconds) {}
    public record VerifyRequest(String challengeId, String pin, String payloadDigest) {}
    public record VerifyResponse(boolean verified) {}

    @PostMapping("/challenge")
    public ResponseEntity<ChallengeResponse> challenge(@RequestBody ChallengeRequest req) {
        if (req.userId() == null || req.payloadDigest() == null) return ResponseEntity.badRequest().build();
        String id = UUID.randomUUID().toString();
        String stored = req.userId() + "|" + req.payloadDigest();
        // ponytail: Redis TTL 180s per ADR-0028, fallback to in-memory if Redis unavailable (local without payu-redis)
        try {
            redis.opsForValue().set("stepup:challenge:" + id, stored, Duration.ofSeconds(180));
        } catch (Exception e) {
            log.warn("Redis unavailable, using in-memory fallback for step-up challenge {}", id);
            InMemoryFallback.put(id, stored);
        }
        log.info("StepUp challenge {} for user {}", id, req.userId());
        return ResponseEntity.ok(new ChallengeResponse(id, 180));
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyResponse> verify(@RequestBody VerifyRequest req) {
        if (req.challengeId() == null || req.pin() == null || req.payloadDigest() == null) return ResponseEntity.badRequest().body(new VerifyResponse(false));
        String stored = null;
        try {
            stored = redis.opsForValue().get("stepup:challenge:" + req.challengeId());
        } catch (Exception e) {
            stored = InMemoryFallback.get(req.challengeId());
        }
        if (stored == null) stored = InMemoryFallback.get(req.challengeId());
        if (stored == null) return ResponseEntity.status(404).body(new VerifyResponse(false));
        int sep = stored.indexOf('|');
        if (sep < 0) return ResponseEntity.status(404).body(new VerifyResponse(false));
        String userId = stored.substring(0, sep);
        String expectedDigest = stored.substring(sep + 1);
        // payload_digest tampering check per ADR-0028: SHA256(sender+recipient+amount+currency+nonce)
        if (!expectedDigest.equals(req.payloadDigest())) {
            log.warn("StepUp payload_digest mismatch challenge {}", req.challengeId());
            return ResponseEntity.ok(new VerifyResponse(false));
        }
        // ponytail: 3-strike lockout 15m per ADR-0028
        UserPinEntity pin = pinRepo.findById(userId).orElse(null);
        if (pin == null) return ResponseEntity.ok(new VerifyResponse(false));
        if (pin.getLockedUntil() != null && pin.getLockedUntil().isAfter(LocalDateTime.now())) {
            log.warn("StepUp locked user {} until {}", userId, pin.getLockedUntil());
            return ResponseEntity.status(423).body(new VerifyResponse(false));
        }
        boolean pinOk = encoder.matches(req.pin(), pin.getPinHash());
        if (!pinOk) {
            int fails = pin.getFailedAttempts() + 1;
            pin.setFailedAttempts(fails);
            if (fails >= 3) pin.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            pinRepo.save(pin);
            log.info("StepUp verify {} fail {}/3", req.challengeId(), fails);
            return ResponseEntity.ok(new VerifyResponse(false));
        }
        // success: reset counter, delete challenge
        pin.setFailedAttempts(0);
        pin.setLockedUntil(null);
        pinRepo.save(pin);
        try { redis.delete("stepup:challenge:" + req.challengeId()); } catch (Exception ignored) {}
        InMemoryFallback.remove(req.challengeId());
        log.info("StepUp verify {} ok", req.challengeId());
        return ResponseEntity.ok(new VerifyResponse(true));
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
