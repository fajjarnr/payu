package id.payu.auth.interfaces;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/biometric")
public class BiometricController {
    private static final Logger log = LoggerFactory.getLogger(BiometricController.class);
    private final StringRedisTemplate redis;
    private static final SecureRandom RND = new SecureRandom();
    // ponytail: minimal in-memory stores for WebAuthn; Redis 180s TTL per ADR-0028
    private static final Map<String, String> REGISTRATIONS = new ConcurrentHashMap<>();

    public BiometricController(StringRedisTemplate redis) { this.redis = redis; }

    public record ChallengeResponse(String challengeId, String challenge, long timeout, String rpId) {}

    @GetMapping("/challenge")
    public ResponseEntity<ChallengeResponse> challenge() {
        byte[] bytes = new byte[32];
        RND.nextBytes(bytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String challengeId = UUID.randomUUID().toString();
        // ponytail: store challenge raw with 180s TTL, fallback in-memory
        try { redis.opsForValue().set("bio:challenge:" + challengeId, challenge, Duration.ofSeconds(180)); }
        catch (Exception e) { InMemory.put(challengeId, challenge); }
        log.info("Biometric challenge {}", challengeId);
        return ResponseEntity.ok(new ChallengeResponse(challengeId, challenge, 60000, "payu.local"));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@RequestBody Map<String,String> req) {
        String challengeId = req.get("challengeId");
        String credential = req.get("credential");
        String username = req.getOrDefault("username", "user");
        if (challengeId == null || credential == null || credential.isBlank()) return ResponseEntity.badRequest().build();
        String expected = getChallenge(challengeId);
        if (expected == null) return ResponseEntity.status(404).body(Map.of("error","challenge expired"));
        // ponytail: stub WebAuthn attestation verify — accept any non-empty credential, store registration
        String regId = UUID.randomUUID().toString();
        REGISTRATIONS.put(regId, username + ":" + credential);
        deleteChallenge(challengeId);
        log.info("Biometric register {} user {}", regId, username);
        return ResponseEntity.ok(Map.of("registrationId", regId, "username", username));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<Map<String,Object>> authenticate(@RequestBody Map<String,String> req) {
        String challengeId = req.get("challengeId");
        String credential = req.get("credential");
        if (challengeId == null || credential == null) return ResponseEntity.badRequest().build();
        String expected = getChallenge(challengeId);
        if (expected == null) return ResponseEntity.status(404).body(Map.of("error","challenge expired"));
        deleteChallenge(challengeId);
        // ponytail: stub assertion verify — accept if any registration exists or credential non-empty
        boolean ok = !REGISTRATIONS.isEmpty() || !credential.isBlank();
        return ResponseEntity.ok(Map.of("success", ok));
    }

    @GetMapping("/registrations/{username}")
    public ResponseEntity<java.util.List<Map<String,String>>> registrations(@PathVariable String username) {
        var list = REGISTRATIONS.entrySet().stream()
            .filter(e -> e.getValue().startsWith(username + ":"))
            .map(e -> Map.of("registrationId", e.getKey(), "username", username))
            .toList();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/registrations/{registrationId}")
    public ResponseEntity<Void> revoke(@PathVariable String registrationId) {
        REGISTRATIONS.remove(registrationId);
        return ResponseEntity.noContent().build();
    }

    private String getChallenge(String id) {
        try { String v = redis.opsForValue().get("bio:challenge:" + id); if (v != null) return v; } catch (Exception ignored) {}
        return InMemory.get(id);
    }
    private void deleteChallenge(String id) {
        try { redis.delete("bio:challenge:" + id); } catch (Exception ignored) {}
        InMemory.remove(id);
    }
    private static class InMemory {
        private static final Map<String,String> MAP = new ConcurrentHashMap<>();
        private static final Map<String,Long> EXP = new ConcurrentHashMap<>();
        static void put(String k,String v){ MAP.put(k,v); EXP.put(k, System.currentTimeMillis()+180_000); }
        static String get(String k){
            Long exp=EXP.get(k); if(exp!=null && System.currentTimeMillis()>exp){ MAP.remove(k); EXP.remove(k); return null; }
            return MAP.get(k);
        }
        static void remove(String k){ MAP.remove(k); EXP.remove(k); }
    }
}
