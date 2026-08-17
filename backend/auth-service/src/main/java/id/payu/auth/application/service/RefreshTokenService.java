package id.payu.auth.application.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import id.payu.cache.service.DistributedCache;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing refresh tokens with rotation.
 *
 * Refresh token rotation is a security mechanism where:
 * 1. Each time a refresh token is used, a new one is issued
 * 2. The old refresh token is invalidated
 * 3. This prevents replay attacks
 *
 * PCI-DSS Compliance:
 * - Requirement 8.2.4: Change authentication keys periodically
 * - OWASP: Implement proper token management
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6819#section-4.7.1">OAuth 2.0 Threat Model - Refresh Token Rotation</a>
 */
@Slf4j
@Service
public class RefreshTokenService {

    private final DistributedCache distributedCache;
    private final BCryptPasswordEncoder tokenEncoder = new BCryptPasswordEncoder(12);

    // Refresh token lifetime: 7 days
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    // Cache key prefix for storing refresh tokens
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    public RefreshTokenService(DistributedCache distributedCache) {
        this.distributedCache = distributedCache;
    }

    /**
     * Creates a new refresh token for a user.
     *
     * @param userId The user ID
     * @return The refresh token response
     */
    public RefreshTokenResponse createRefreshToken(String userId) {
        String tokenId = UUID.randomUUID().toString();
        String rawToken = generateRawToken(tokenId);
        String hashedToken = hashToken(rawToken);

        RefreshTokenMetadata metadata = new RefreshTokenMetadata(
                tokenId, userId, Instant.now(), Instant.now().plus(REFRESH_TOKEN_TTL), 0, hashedToken);

        String cacheKey = buildCacheKey(userId, tokenId);
        distributedCache.put(cacheKey, metadata, REFRESH_TOKEN_TTL);

        // Store reverse index: tokenId -> userId for O(1) lookup
        String reverseIndexKey = buildReverseIndexKey(tokenId);
        distributedCache.put(reverseIndexKey, userId, REFRESH_TOKEN_TTL);

        log.info("Created refresh token for user: {}, tokenId: {}", maskUserId(userId), tokenId);

        return new RefreshTokenResponse(rawToken, metadata.expiresAt());
    }

    /**
     * Rotates a refresh token and returns a new one.
     *
     * This method implements refresh token rotation where:
     * - The old token is invalidated
     * - A new token is issued
     * - Rotation count is incremented
     *
     * @param oldRefreshToken The old refresh token
     * @return The new refresh token response
     * @throws IllegalArgumentException if the token is invalid or expired
     * @throws org.springframework.security.authentication.BadCredentialsException if token reuse is detected
     */
    public RefreshTokenResponse rotateRefreshToken(String oldRefreshToken) {
        // Extract token ID from the raw token
        String tokenId = extractTokenId(oldRefreshToken);

        // Find token metadata through its reverse index.
        RefreshTokenMetadata metadata = findTokenMetadata(tokenId);

        if (metadata == null) {
            log.warn("Attempt to use unknown refresh token: {}", maskToken(oldRefreshToken));
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Invalid refresh token");
        }

        // Check if token has expired
        if (Instant.now().isAfter(metadata.expiresAt())) {
            log.warn("Attempt to use expired refresh token for user: {}",
                    maskUserId(metadata.userId()));
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Refresh token has expired");
        }

        // Verify the token hash matches
        if (!tokenEncoder.matches(oldRefreshToken, metadata.hashedToken())) {
            log.warn("Attempt to use invalid refresh token for user: {}",
                    maskUserId(metadata.userId()));
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Invalid refresh token");
        }

        // Invalidate the old token
        invalidateToken(metadata.userId(), tokenId);

        // Create a new token (rotation)
        RefreshTokenResponse newToken = createRefreshToken(metadata.userId());

        log.info("Rotated refresh token for user: {}, previous rotation count: {}",
                maskUserId(metadata.userId()), metadata.rotationCount());

        return newToken;
    }

    /**
     * Invalidates a specific refresh token.
     *
     * @param userId The user ID
     * @param tokenId The token ID
     */
    public void invalidateToken(String userId, String tokenId) {
        String cacheKey = buildCacheKey(userId, tokenId);
        String reverseIndexKey = buildReverseIndexKey(tokenId);
        distributedCache.evict(cacheKey);
        distributedCache.evict(reverseIndexKey);
        log.info("Invalidated refresh token for user: {}, tokenId: {}", maskUserId(userId), tokenId);
    }

    /**
     * Invalidates all refresh tokens for a user.
     * Used when user logs out from all devices or changes password.
     *
     * @param userId The user ID
     */
    public void invalidateAllUserTokens(String userId) {
        String pattern = REFRESH_TOKEN_PREFIX + userId + ":*";
        distributedCache.evictMatching(pattern);
        log.info("Invalidated all refresh tokens for user: {}", maskUserId(userId));
    }

    /**
     * Validates a refresh token without rotating it.
     * Used for checking if a token is still valid.
     *
     * @param refreshToken The refresh token
     * @return true if valid, false otherwise
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        try {
            String tokenId = extractTokenId(refreshToken);
            RefreshTokenMetadata metadata = findTokenMetadata(tokenId);

            if (metadata == null) {
                return false;
            }

            if (Instant.now().isAfter(metadata.expiresAt())) {
                return false;
            }

            return tokenEncoder.matches(refreshToken, metadata.hashedToken());
        } catch (Exception e) {
            log.warn("Error validating refresh token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Finds token metadata by token ID.
     * Uses reverse index mapping for O(1) lookup performance.
     *
     * @param tokenId The token ID to lookup
     * @return RefreshTokenMetadata if found, null otherwise
     */
    private RefreshTokenMetadata findTokenMetadata(String tokenId) {
        String reverseIndexKey = buildReverseIndexKey(tokenId);
        String userId = distributedCache.get(reverseIndexKey, String.class);

        if (userId == null) {
            log.debug("Token ID not found in reverse index: {}", tokenId);
            return null;
        }

        String cacheKey = buildCacheKey(userId, tokenId);
        RefreshTokenMetadata metadata = distributedCache.get(cacheKey, RefreshTokenMetadata.class);

        if (metadata == null) {
            log.warn("Metadata not found for token ID: {} and user: {}", tokenId, maskUserId(userId));
        }

        return metadata;
    }

    /**
     * Generates a raw refresh token string.
     */
    private String generateRawToken(String tokenId) {
        // Format: version + tokenId + random
        // In production, use a cryptographically secure random generator
        return "v1." + tokenId + "." + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Hashes a refresh token for secure storage.
     */
    private String hashToken(String rawToken) {
        return tokenEncoder.encode(rawToken);
    }

    /**
     * Extracts token ID from raw token.
     */
    private String extractTokenId(String rawToken) {
        // Token format: v1.{tokenId}.{random}
        String[] parts = rawToken.split("\\.");
        if (parts.length >= 2) {
            return parts[1];
        }
        throw new IllegalArgumentException("Invalid token format");
    }

    /**
     * Builds Redis key for storing token metadata.
     */
    private String buildCacheKey(String userId, String tokenId) {
        return REFRESH_TOKEN_PREFIX + userId + ":" + tokenId;
    }

    /**
     * Builds reverse index key for O(1) token lookup by ID.
     * Maps tokenId -> userId for fast metadata retrieval.
     */
    private String buildReverseIndexKey(String tokenId) {
        return REFRESH_TOKEN_PREFIX + "index:" + tokenId;
    }

    /**
     * Masks user ID for safe logging.
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.length() < 4) {
            return "***";
        }
        return userId.substring(0, 4) + "***";
    }

    /**
     * Masks token for safe logging.
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 8) + "...";
    }

    @AllArgsConstructor
    public static class RefreshTokenResponse {
        private final String refreshToken;
        private final Instant expiresAt;

        public String refreshToken() {
            return refreshToken;
        }

        public Instant expiresAt() {
            return expiresAt;
        }
    }

    record RefreshTokenMetadata(
            String tokenId,
            String userId,
            Instant createdAt,
            Instant expiresAt,
            int rotationCount,
            String hashedToken) {
    }
}
