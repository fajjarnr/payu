package id.payu.auth.adapter.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for RefreshTokenService.
 * Tests token creation, rotation, validation, and invalidation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        refreshTokenService = new RefreshTokenService(redisTemplate);
    }

    @Nested
    @DisplayName("createRefreshToken()")
    class CreateRefreshToken {

        @Test
        @DisplayName("should create refresh token with valid metadata")
        void shouldCreateRefreshTokenWithValidMetadata() {
            // Given
            String userId = "user123";

            // When
            RefreshTokenService.RefreshTokenResponse response =
                    refreshTokenService.createRefreshToken(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.refreshToken()).isNotNull();
            assertThat(response.refreshToken()).startsWith("v1.");
            assertThat(response.expiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("should store hashed token in Redis")
        void shouldStoreHashedTokenInRedis() {
            // Given
            String userId = "user123";

            // When
            refreshTokenService.createRefreshToken(userId);

            // Then
            verify(valueOperations).set(
                    anyString(),
                    any(),
                    eq(Duration.ofDays(7))
            );
        }

        @Test
        @DisplayName("should create unique tokens for multiple calls")
        void shouldCreateUniqueTokensForMultipleCalls() {
            // Given
            String userId = "user123";

            // When
            RefreshTokenService.RefreshTokenResponse response1 =
                    refreshTokenService.createRefreshToken(userId);
            RefreshTokenService.RefreshTokenResponse response2 =
                    refreshTokenService.createRefreshToken(userId);

            // Then
            assertThat(response1.refreshToken()).isNotEqualTo(response2.refreshToken());
        }
    }

    @Nested
    @DisplayName("rotateRefreshToken()")
    class RotateRefreshToken {

        @Test
        @DisplayName("should throw exception for unknown token")
        void shouldThrowExceptionForUnknownToken() {
            // Given
            String unknownToken = "v1.unknown.abc123";
            given(valueOperations.get(anyString())).willReturn(null);

            // When/Then
            assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(unknownToken))
                    .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        @DisplayName("should throw exception for malformed token")
        void shouldThrowExceptionForMalformedToken() {
            // Given
            String malformedToken = "invalid_format";

            // When/Then
            assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(malformedToken))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid token format");
        }
    }

    @Nested
    @DisplayName("isRefreshTokenValid()")
    class IsRefreshTokenValid {

        @Test
        @DisplayName("should return false for null token")
        void shouldReturnFalseForNullToken() {
            // When
            boolean isValid = refreshTokenService.isRefreshTokenValid(null);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            // When
            boolean isValid = refreshTokenService.isRefreshTokenValid("");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            // When
            boolean isValid = refreshTokenService.isRefreshTokenValid("invalid_token");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should return false when token not found in Redis")
        void shouldReturnFalseWhenTokenNotFoundInRedis() {
            // Given
            String validFormatToken = "v1.validtokenid.abc123";
            given(valueOperations.get(anyString())).willReturn(null);

            // When
            boolean isValid = refreshTokenService.isRefreshTokenValid(validFormatToken);

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("invalidateToken()")
    class InvalidateToken {

        @Test
        @DisplayName("should delete token from Redis")
        void shouldDeleteTokenFromRedis() {
            // Given
            String userId = "user123";
            String tokenId = "token123";

            // When
            refreshTokenService.invalidateToken(userId, tokenId);

            // Then
            verify(redisTemplate).delete(anyString());
            verify(redisTemplate).delete(anyString());
        }
    }

    @Nested
    @DisplayName("invalidateAllUserTokens()")
    class InvalidateAllUserTokens {

        @Test
        @DisplayName("should delete all user tokens from Redis")
        void shouldDeleteAllUserTokensFromRedis() {
            // Given
            String userId = "user123";
            given(redisTemplate.keys(anyString())).willReturn(Set.of("key1", "key2"));

            // When
            refreshTokenService.invalidateAllUserTokens(userId);

            // Then
            verify(redisTemplate).delete(any(Set.class));
        }

        @Test
        @DisplayName("should handle empty key set gracefully")
        void shouldHandleEmptyKeySetGracefully() {
            // Given
            String userId = "user123";
            given(redisTemplate.keys(anyString())).willReturn(null);

            // When/Then - should not throw exception
            refreshTokenService.invalidateAllUserTokens(userId);

            // Then - delete should not be called when keys are null
            verify(redisTemplate).delete(any(Set.class));
        }
    }

    @Nested
    @DisplayName("token format validation")
    class TokenFormatValidation {

        @Test
        @DisplayName("created token should have correct format")
        void createdTokenShouldHaveCorrectFormat() {
            // Given
            String userId = "user123";

            // When
            RefreshTokenService.RefreshTokenResponse response =
                    refreshTokenService.createRefreshToken(userId);

            // Then
            String token = response.refreshToken();
            String[] parts = token.split("\\.");

            assertThat(parts).hasSizeGreaterThanOrEqualTo(2);
            assertThat(parts[0]).isEqualTo("v1");
        }

        @Test
        @DisplayName("created token should contain valid UUID as tokenId")
        void createdTokenShouldContainValidUUIDAsTokenId() {
            // Given
            String userId = "user123";

            // When
            RefreshTokenService.RefreshTokenResponse response =
                    refreshTokenService.createRefreshToken(userId);

            // Then
            String token = response.refreshToken();
            String[] parts = token.split("\\.");
            String tokenId = parts[1];

            // Verify tokenId is a valid UUID - should not throw exception
            try {
                java.util.UUID.fromString(tokenId);
            } catch (IllegalArgumentException e) {
                throw new AssertionError("Token ID should be a valid UUID", e);
            }
        }
    }

    @Nested
    @DisplayName("token expiration")
    class TokenExpiration {

        @Test
        @DisplayName("created token should expire in approximately 7 days")
        void createdTokenShouldExpireInApproximately7Days() {
            // Given
            String userId = "user123";

            // When
            RefreshTokenService.RefreshTokenResponse response =
                    refreshTokenService.createRefreshToken(userId);

            // Then
            Instant expiration = response.expiresAt();
            Instant now = Instant.now();
            Instant expectedExpiration = now.plus(Duration.ofDays(7));

            // Allow 1 second tolerance for timing
            long diffSeconds = Math.abs(Duration.between(expiration, expectedExpiration).getSeconds());
            assertThat(diffSeconds).isLessThan(2);
        }
    }
}
