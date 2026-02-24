package id.payu.auth.application.service;

import id.payu.cache.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MFATokenService {

    private final CacheService cacheService;

    private static final String TOKEN_KEY_PREFIX = "auth:mfa:token:";
    private static final String OTP_KEY_PREFIX = "auth:mfa:otp:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    @Value("${payu.security.mfa.token-expiry-seconds:300}")
    private long tokenExpirySeconds;

    @Value("${payu.security.mfa.otp-length:6}")
    private int otpLength;

    @Value("${payu.security.mfa.otp-expiry-seconds:300}")
    private long otpExpirySeconds;

    public MFAToken generateMFAToken(String username) {
        String mfaToken = UUID.randomUUID().toString();
        String otp = generateOTP();

        long expiresAt = Instant.now().plusSeconds(tokenExpirySeconds).toEpochMilli();
        long otpExpiresAt = Instant.now().plusSeconds(otpExpirySeconds).toEpochMilli();

        MFAToken mfaTokenObj = new MFAToken(
                mfaToken,
                username,
                expiresAt,
                true
        );

        // Store in Redis with TTL
        String tokenKey = TOKEN_KEY_PREFIX + mfaToken;
        String otpKey = OTP_KEY_PREFIX + username;

        cacheService.put(tokenKey, mfaTokenObj, TOKEN_TTL);
        cacheService.put(otpKey, otp, OTP_TTL);

        log.info("Generated MFA token for user {}: token={}, otp_expires_at={}",
                username, mfaToken, otpExpiresAt);

        return mfaTokenObj;
    }

    public boolean validateAndConsumeMFAToken(String mfaToken, String username) {
        String tokenKey = TOKEN_KEY_PREFIX + mfaToken;
        MFAToken token = cacheService.get(tokenKey, MFAToken.class);

        if (token == null) {
            log.warn("MFA token not found for user {}", username);
            return false;
        }

        if (!token.active()) {
            log.warn("MFA token already consumed for user {}", username);
            return false;
        }

        if (System.currentTimeMillis() > token.expiresAt()) {
            log.warn("MFA token expired for user {}", username);
            cacheService.invalidate(tokenKey);
            return false;
        }

        if (!token.username().equals(username)) {
            log.warn("MFA token username mismatch for user {}", username);
            return false;
        }

        // Mark token as consumed by storing with remaining TTL
        long remainingTtl = Math.max(1, (token.expiresAt() - System.currentTimeMillis()) / 1000);
        MFAToken consumed = new MFAToken(
                token.mfaToken(),
                token.username(),
                token.expiresAt(),
                false
        );
        cacheService.put(tokenKey, consumed, Duration.ofSeconds(remainingTtl));
        return true;
    }

    public boolean validateOTP(String username, String otpCode) {
        String otpKey = OTP_KEY_PREFIX + username;
        String storedOtp = cacheService.get(otpKey, String.class);

        if (storedOtp == null) {
            log.warn("No OTP found for user {}", username);
            return false;
        }

        if (!storedOtp.equals(otpCode)) {
            log.warn("Invalid OTP for user {}", username);
            return false;
        }

        return true;
    }

    public void consumeOTP(String username) {
        String otpKey = OTP_KEY_PREFIX + username;
        cacheService.invalidate(otpKey);
    }

    /**
     * Cleanup is now handled automatically by Redis TTL.
     * This method is kept for backward compatibility but is now a no-op.
     */
    public void cleanupExpiredTokens() {
        // Redis automatically expires keys based on TTL
        log.debug("Cleanup called - tokens are automatically expired by Redis TTL");
    }

    private String generateOTP() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    public record MFAToken(
            String mfaToken,
            String username,
            long expiresAt,
            boolean active
    ) {}
}
