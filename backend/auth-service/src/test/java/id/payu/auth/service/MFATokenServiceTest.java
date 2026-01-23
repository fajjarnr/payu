package id.payu.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MFATokenService")
class MFATokenServiceTest {

    private MFATokenService mfaTokenService;

    @BeforeEach
    void setUp() {
        mfaTokenService = new MFATokenService();
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
            MFATokenService.MFAToken token1 = mfaTokenService.generateMFAToken("user1");
            MFATokenService.MFAToken token2 = mfaTokenService.generateMFAToken("user2");

            assertThat(token1.mfaToken()).isNotNull();
            assertThat(token2.mfaToken()).isNotNull();
            assertThat(token1.mfaToken()).isNotEqualTo(token2.mfaToken());
        }

        @Test
        @DisplayName("should generate 6-digit OTP")
        void shouldGenerate6DigitOTP() {
            mfaTokenService.generateMFAToken("user1");

            String otp = "000000";
            boolean valid = mfaTokenService.validateOTP("user1", otp);
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should set correct expiry time")
        void shouldSetCorrectExpiryTime() {
            long before = System.currentTimeMillis();
            MFATokenService.MFAToken token = mfaTokenService.generateMFAToken("user1");
            long after = System.currentTimeMillis();

            long expectedExpiry = before + 300000;
            assertThat(token.expiresAt()).isGreaterThan(expectedExpiry - 1000);
            assertThat(token.expiresAt()).isLessThan(after + 300000 + 1000);
        }

        @Test
        @DisplayName("should mark token as active")
        void shouldMarkTokenAsActive() {
            MFATokenService.MFAToken token = mfaTokenService.generateMFAToken("user1");

            assertThat(token.active()).isTrue();
        }
    }

    @Nested
    @DisplayName("validateAndConsumeMFAToken")
    class ValidateAndConsumeMFAToken {

        @Test
        @DisplayName("should validate valid token")
        void shouldValidateValidToken() {
            MFATokenService.MFAToken token = mfaTokenService.generateMFAToken("user1");

            boolean valid = mfaTokenService.validateAndConsumeMFAToken(token.mfaToken(), "user1");

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("should reject invalid token")
        void shouldRejectInvalidToken() {
            boolean valid = mfaTokenService.validateAndConsumeMFAToken("invalid-token", "user1");

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject token for wrong username")
        void shouldRejectTokenForWrongUsername() {
            MFATokenService.MFAToken token = mfaTokenService.generateMFAToken("user1");

            boolean valid = mfaTokenService.validateAndConsumeMFAToken(token.mfaToken(), "user2");

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject already consumed token")
        void shouldRejectAlreadyConsumedToken() {
            MFATokenService.MFAToken token = mfaTokenService.generateMFAToken("user1");

            mfaTokenService.validateAndConsumeMFAToken(token.mfaToken(), "user1");
            boolean valid = mfaTokenService.validateAndConsumeMFAToken(token.mfaToken(), "user1");

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("validateOTP")
    class ValidateOTP {

        @Test
        @DisplayName("should validate correct OTP")
        void shouldValidateCorrectOTP() {
            mfaTokenService.generateMFAToken("user1");

            String otp = "123456";
            boolean valid = mfaTokenService.validateOTP("user1", otp);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject incorrect OTP")
        void shouldRejectIncorrectOTP() {
            mfaTokenService.generateMFAToken("user1");

            boolean valid = mfaTokenService.validateOTP("user1", "000000");

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject OTP for non-existent user")
        void shouldRejectOTPForNonExistentUser() {
            boolean valid = mfaTokenService.validateOTP("nonexistent", "123456");

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("consumeOTP")
    class ConsumeOTP {

        @Test
        @DisplayName("should consume OTP after validation")
        void shouldConsumeOTPAfterValidation() {
            mfaTokenService.generateMFAToken("user1");

            mfaTokenService.consumeOTP("user1");
            boolean valid = mfaTokenService.validateOTP("user1", "123456");

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("cleanupExpiredTokens")
    class CleanupExpiredTokens {

        @Test
        @DisplayName("should cleanup without errors")
        void shouldCleanupWithoutErrors() {
            mfaTokenService.generateMFAToken("user1");
            mfaTokenService.generateMFAToken("user2");

            mfaTokenService.cleanupExpiredTokens();
        }
    }
}
