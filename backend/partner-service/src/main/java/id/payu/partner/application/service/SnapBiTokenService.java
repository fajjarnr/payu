package id.payu.partner.application.service;

import id.payu.cache.service.DistributedCache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
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

    private final DistributedCache distributedCache;

    public SnapBiTokenService(DistributedCache distributedCache) {
        this.distributedCache = distributedCache;
    }

    @PostConstruct
    public void init() {
        byte[] secretBytes = tokenSecret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        LOG.info("SnapBiTokenService initialized with JWT expiration={}ms (distributed-cache-backed)", expirationTimeMs);
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
        distributedCache.put(redisKey, tokenInfo, Duration.ofMillis(expirationTimeMs));

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
            TokenInfo tokenInfo = distributedCache.get(redisKey, TokenInfo.class);

            if (tokenInfo == null) {
                LOG.warn("Token not found in Redis store clientId={}", clientId);
                return null;
            }

            if (tokenInfo.expiration().before(new Date())) {
                distributedCache.evict(redisKey);
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
            distributedCache.evict(redisKey);
            LOG.info("Token revoked clientId={}", clientId);
        }
    }

    private String buildTokenKey(String clientId) {
        return TOKEN_KEY_PREFIX + clientId;
    }

    public record TokenInfo(String tokenId, String clientId, String partnerId, Date expiration) {
    }
}
