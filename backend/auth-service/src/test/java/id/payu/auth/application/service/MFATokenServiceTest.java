package id.payu.auth.application.service;

import id.payu.cache.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("MFATokenService")
@ExtendWith(MockitoExtension.class)
class MFATokenServiceTest {

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private MFATokenService mfaTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mfaTokenService, "tokenExpirySeconds", 300);
        ReflectionTestUtils.setField(mfaTokenService, "otpLength", 6);
        ReflectionTestUtils.setField(mfaTokenService, "otpExpirySeconds", 300);
    }

    @Nested
    @DisplayName("generateMFAToken")
    class GenerateMFAToken {

        @Test
        @DisplayName("should generate unique MFA token")
        void shouldGenerateUniqueMFAToken() {
            // When
            MFATokenService.MFAToken token1 = mfaTokenService.generateMFAToken("user1");
            MFATokenService.MFAToken token2 = mfaTokenService.generateMFAToken("user2");

            // Then
            assertThat(token1.mfaToken()).isNotNull();
            assertThat(token2.mfaToken()).isNotNull();
            assertThat(token1.mfaToken()).isNotEqualTo(token2.mfaToken());
            verify(cacheService, times(2)).put(contains("auth:mfa:token:"), any(MFATokenService.MFAToken.class), eq(Duration.ofMinutes(5)));
            verify(cacheService, times(2)).put(contains("auth:mfa:otp:"), anyString(), eq(Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("should generate 6-digit OTP")
        void shouldGenerate6DigitOTP() {
            // When
            mfaTokenService.generateMFAToken("user1");

            // Then
            verify(cacheService).put(contains("auth:mfa:otp:"), argThat(otp -> otp != null && ((String) otp).length() == 6), eq(Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("should set correct expiry time")
        void shouldSetCorrectExpiryTime() {
            // Given
            long before = System.currentTimeMillis();

            // When
            MFATokenService.MFAToken token = mfaTokenService.generateMFAToken("user1");
            long after = System.currentTimeMillis();

            // Then
            long expectedExpiry = before + 300000;
            assertThat(token.expiresAt()).isGreaterThan(expectedExpiry - 1000);
            assertThat(token.expiresAt()).isLessThan(after + 300000 + 1000);
        }

        @Test
        @DisplayName("should mark token as active")
        void shouldMarkTokenAsActive() {
            // When
            MFATokenService.MFAToken token = mfaTokenService.generateMFAToken("user1");

            // Then
            assertThat(token.active()).isTrue();
        }
    }

    @Nested
    @DisplayName("validateAndConsumeMFAToken")
    class ValidateAndConsumeMFAToken {

        @Test
        @DisplayName("should validate valid token")
        void shouldValidateValidToken() {
            // Given
            String tokenId = "test-token-id";
            String username = "user1";
            long expiresAt = System.currentTimeMillis() + 300000;
            MFATokenService.MFAToken token = new MFATokenService.MFAToken(tokenId, username, expiresAt, true);

            when(cacheService.get("auth:mfa:token:" + tokenId, MFATokenService.MFAToken.class)).thenReturn(token);

            // When
            boolean valid = mfaTokenService.validateAndConsumeMFAToken(tokenId, username);

            // Then
            assertThat(valid).isTrue();
            verify(cacheService).put(eq("auth:mfa:token:" + tokenId), argThat(t -> !((MFATokenService.MFAToken) t).active()), any(Duration.class));
        }

        @Test
        @DisplayName("should reject invalid token")
        void shouldRejectInvalidToken() {
            // Given
            when(cacheService.get(anyString(), eq(MFATokenService.MFAToken.class))).thenReturn(null);

            // When
            boolean valid = mfaTokenService.validateAndConsumeMFAToken("invalid-token", "user1");

            // Then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject token for wrong username")
        void shouldRejectTokenForWrongUsername() {
            // Given
            String tokenId = "test-token-id";
            String username = "user1";
            long expiresAt = System.currentTimeMillis() + 300000;
            MFATokenService.MFAToken token = new MFATokenService.MFAToken(tokenId, username, expiresAt, true);

            when(cacheService.get("auth:mfa:token:" + tokenId, MFATokenService.MFAToken.class)).thenReturn(token);

            // When
            boolean valid = mfaTokenService.validateAndConsumeMFAToken(tokenId, "user2");

            // Then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject already consumed token")
        void shouldRejectAlreadyConsumedToken() {
            // Given
            String tokenId = "test-token-id";
            String username = "user1";
            long expiresAt = System.currentTimeMillis() + 300000;
            MFATokenService.MFAToken token = new MFATokenService.MFAToken(tokenId, username, expiresAt, false);

            when(cacheService.get("auth:mfa:token:" + tokenId, MFATokenService.MFAToken.class)).thenReturn(token);

            // When
            boolean valid = mfaTokenService.validateAndConsumeMFAToken(tokenId, username);

            // Then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject expired token")
        void shouldRejectExpiredToken() {
            // Given
            String tokenId = "test-token-id";
            String username = "user1";
            long expiresAt = System.currentTimeMillis() - 1000; // Expired
            MFATokenService.MFAToken token = new MFATokenService.MFAToken(tokenId, username, expiresAt, true);

            when(cacheService.get("auth:mfa:token:" + tokenId, MFATokenService.MFAToken.class)).thenReturn(token);

            // When
            boolean valid = mfaTokenService.validateAndConsumeMFAToken(tokenId, username);

            // Then
            assertThat(valid).isFalse();
            verify(cacheService).invalidate("auth:mfa:token:" + tokenId);
        }
    }

    @Nested
    @DisplayName("validateOTP")
    class ValidateOTP {

        @Test
        @DisplayName("should validate correct OTP")
        void shouldValidateCorrectOTP() {
            // Given
            String username = "user1";
            String otp = "123456";
            when(cacheService.get("auth:mfa:otp:" + username, String.class)).thenReturn(otp);

            // When
            boolean valid = mfaTokenService.validateOTP(username, otp);

            // Then
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("should reject incorrect OTP")
        void shouldRejectIncorrectOTP() {
            // Given
            String username = "user1";
            when(cacheService.get("auth:mfa:otp:" + username, String.class)).thenReturn("123456");

            // When
            boolean valid = mfaTokenService.validateOTP(username, "000000");

            // Then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject OTP for non-existent user")
        void shouldRejectOTPForNonExistentUser() {
            // Given
            when(cacheService.get(anyString(), eq(String.class))).thenReturn(null);

            // When
            boolean valid = mfaTokenService.validateOTP("nonexistent", "123456");

            // Then
            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("consumeOTP")
    class ConsumeOTP {

        @Test
        @DisplayName("should consume OTP after validation")
        void shouldConsumeOTPAfterValidation() {
            // Given
            String username = "user1";

            // When
            mfaTokenService.consumeOTP(username);

            // Then
            verify(cacheService).invalidate("auth:mfa:otp:" + username);
        }
    }

    @Nested
    @DisplayName("cleanupExpiredTokens")
    class CleanupExpiredTokens {

        @Test
        @DisplayName("should cleanup without errors")
        void shouldCleanupWithoutErrors() {
            // When - cleanup is now a no-op since Redis handles TTL
            mfaTokenService.cleanupExpiredTokens();

            // Then - no interactions with cache service needed
            verifyNoInteractions(cacheService);
        }
    }
}
