package id.payu.partner.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SnapBiTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(SnapBiTokenService.class);

    @Value("${partner.jwt.expiration-ms:900000}")
    private long expirationTimeMs;

    @Value("${partner.jwt.secret}")
    private String tokenSecret;

    private SecretKey signingKey;

    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        byte[] secretBytes = tokenSecret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        LOG.info("SnapBiTokenService initialized with JWT expiration={}ms", expirationTimeMs);
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
        tokenStore.put(tokenId, tokenInfo);

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

            String tokenId = (String) claims.get("tokenId");
            TokenInfo tokenInfo = tokenStore.get(tokenId);

            if (tokenInfo == null) {
                LOG.warn("Token not found in store tokenId={}", tokenId);
                return null;
            }

            if (tokenInfo.expiration.before(new Date())) {
                tokenStore.remove(tokenId);
                LOG.warn("Token expired tokenId={}", tokenId);
                return null;
            }

            return claims;
        } catch (Exception e) {
            LOG.warn("Token validation failed error={}", e.getMessage());
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
            String tokenId = (String) claims.get("tokenId");
            tokenStore.remove(tokenId);
            LOG.info("Token revoked tokenId={}", tokenId);
        }
    }

    public void cleanupExpiredTokens() {
        Date now = new Date();
        tokenStore.entrySet().removeIf(entry -> entry.getValue().expiration.before(now));
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
