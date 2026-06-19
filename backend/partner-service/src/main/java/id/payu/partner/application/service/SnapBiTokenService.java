package id.payu.partner.application.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SnapBiTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(SnapBiTokenService.class);
    private static final String TOKEN_KEY_PREFIX = "snapbi:token:";

    @Value("${partner.jwt.expiration-ms:900000}")
    private long expirationTimeMs;

    @Value("${partner.jwt.secret}")
    private String tokenSecret;

    private SecretKey signingKey;

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, TokenInfo> redisTemplate;
    private ValueOperations<String, TokenInfo> valueOps;

    public SnapBiTokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = (RedisTemplate<String, TokenInfo>) (RedisTemplate<?, ?>) redisTemplate;
    }

    @PostConstruct
    public void init() {
        byte[] secretBytes = tokenSecret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        this.valueOps = redisTemplate.opsForValue();
        LOG.info("SnapBiTokenService initialized with JWT expiration={}ms (Redis-backed)", expirationTimeMs);
    }

    public String generateAccessToken(String clientId, String partnerId, String partnerName) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTimeMs);
        String tokenId = java.util.UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("clientId", clientId);
        claims.put("partnerId", partnerId);
        claims.put("partnerName", partnerName);
        claims.put("tokenId", tokenId);

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(clientId)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(signingKey)
                .compact();

        TokenInfo tokenInfo = new TokenInfo(tokenId, clientId, partnerId, expiration);
        String redisKey = buildTokenKey(clientId);
        valueOps.set(redisKey, tokenInfo, Duration.ofMillis(expirationTimeMs));

        LOG.info("Generated access token for partner clientId={} partnerId={} tokenId={}", clientId, partnerId, tokenId);

        return token;
    }

    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String clientId = (String) claims.get("clientId");
            String redisKey = buildTokenKey(clientId);
            TokenInfo tokenInfo = valueOps.get(redisKey);

            if (tokenInfo == null) {
                LOG.warn("Token not found in Redis store clientId={}", clientId);
                return null;
            }

            if (tokenInfo.expiration.before(new Date())) {
                redisTemplate.delete(redisKey);
                LOG.warn("Token expired clientId={}", clientId);
                return null;
            }

            return claims;
        } catch (Exception e) {
            LOG.warn("Token validation failed", e);
            return null;
        }
    }

    public String getClientIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.get("clientId", String.class) : null;
    }

    public String getPartnerIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.get("partnerId", String.class) : null;
    }

    public void revokeToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            String clientId = (String) claims.get("clientId");
            String redisKey = buildTokenKey(clientId);
            redisTemplate.delete(redisKey);
            LOG.info("Token revoked clientId={}", clientId);
        }
    }

    @SchedulerLock(name = "SnapBiTokenService_cleanupExpiredTokens", lockAtLeastFor = "PT1S", lockAtMostFor = "PT1M")@Scheduled(fixedRate = 60000)
    public void cleanupExpiredTokens() {
        Date now = new Date();
        Set<String> keys = redisTemplate.keys(TOKEN_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            int removedCount = 0;
            for (String key : keys) {
                TokenInfo tokenInfo = valueOps.get(key);
                if (tokenInfo != null && tokenInfo.expiration.before(now)) {
                    redisTemplate.delete(key);
                    removedCount++;
                }
            }
            if (removedCount > 0) {
                LOG.debug("Cleaned up {} expired tokens from Redis", removedCount);
            }
        }
    }

    private String buildTokenKey(String clientId) {
        return TOKEN_KEY_PREFIX + clientId;
    }

    private static class TokenInfo {
        String tokenId;
        String clientId;
        String partnerId;
        Date expiration;

        TokenInfo(String tokenId, String clientId, String partnerId, Date expiration) {
            this.tokenId = tokenId;
            this.clientId = clientId;
            this.partnerId = partnerId;
            this.expiration = expiration;
        }
    }
}
