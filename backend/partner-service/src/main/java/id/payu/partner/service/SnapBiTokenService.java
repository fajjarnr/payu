package id.payu.partner.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@ApplicationScoped
public class SnapBiTokenService {

    private static final Logger LOG = Logger.getLogger(SnapBiTokenService.class);
    
    private static final long EXPIRATION_TIME_MS = 15 * 60 * 1000;
    private static final String TOKEN_SECRET = "payu-snap-bi-secret-key-for-jwt-token-generation-validation-2024-payu-payu";
    
    private static final byte[] SECRET_BYTES = TOKEN_SECRET.getBytes(StandardCharsets.UTF_8);
    private static final SecretKey SIGNING_KEY = new SecretKeySpec(SECRET_BYTES, "HmacSHA256");

    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();

    @Inject
    SnapBiSignatureService signatureService;

    public String generateAccessToken(String clientId, String partnerId, String partnerName) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME_MS);
        String tokenId = java.util.UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("clientId", clientId);
        claims.put("partnerId", partnerId);
        claims.put("partnerName", partnerName);
        claims.put("tokenId", tokenId);

        String token = Jwts.builder()
                .claims(claims)
                .subject(clientId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(SIGNING_KEY)
                .compact();

        TokenInfo tokenInfo = new TokenInfo(tokenId, clientId, partnerId, expiration);
        tokenStore.put(tokenId, tokenInfo);

        LOG.infof("Generated access token for partner clientId=%s partnerId=%s tokenId=%s", clientId, partnerId, tokenId);

        return token;
    }

    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) SIGNING_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenId = (String) claims.get("tokenId");
            TokenInfo tokenInfo = tokenStore.get(tokenId);

            if (tokenInfo == null) {
                LOG.warnf("Token not found in store tokenId=%s", tokenId);
                return null;
            }

            if (tokenInfo.expiration.before(new Date())) {
                tokenStore.remove(tokenId);
                LOG.warnf("Token expired tokenId=%s", tokenId);
                return null;
            }

            return claims;
        } catch (Exception e) {
            LOG.warnf("Token validation failed error=%s", e.getMessage());
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
            LOG.infof("Token revoked tokenId=%s", tokenId);
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
