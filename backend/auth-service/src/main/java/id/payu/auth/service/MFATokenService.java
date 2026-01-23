package id.payu.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MFATokenService {

    private final Map<String, MFAToken> tokenStore = new ConcurrentHashMap<>();
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();

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
        
        tokenStore.put(mfaToken, mfaTokenObj);
        otpStore.put(username, otp);
        
        log.info("Generated MFA token for user {}: token={}, otp_expires_at={}",
                username, mfaToken, otpExpiresAt);
        
        return mfaTokenObj;
    }

    public boolean validateAndConsumeMFAToken(String mfaToken, String username) {
        MFAToken token = tokenStore.get(mfaToken);
        
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
            tokenStore.remove(mfaToken);
            return false;
        }
        
        if (!token.username().equals(username)) {
            log.warn("MFA token username mismatch for user {}", username);
            return false;
        }
        
        MFAToken consumed = new MFAToken(
                token.mfaToken(),
                token.username(),
                token.expiresAt(),
                false
        );
        tokenStore.put(mfaToken, consumed);
        return true;
    }

    public boolean validateOTP(String username, String otpCode) {
        String storedOtp = otpStore.get(username);
        
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
        otpStore.remove(username);
    }

    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        int removed = 0;
        
        for (Map.Entry<String, MFAToken> entry : tokenStore.entrySet()) {
            if (entry.getValue().expiresAt() < now && entry.getValue().active()) {
                otpStore.remove(entry.getValue().username());
                tokenStore.remove(entry.getKey());
                removed++;
            }
        }
        
        if (removed > 0) {
            log.info("Cleaned up {} expired MFA tokens", removed);
        }
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
