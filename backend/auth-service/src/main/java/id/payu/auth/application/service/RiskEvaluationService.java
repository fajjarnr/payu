package id.payu.auth.application.service;

import id.payu.auth.domain.model.LoginContext;
import id.payu.auth.domain.model.UserRiskProfileEntity;
import id.payu.auth.domain.model.UserKnownDeviceEntity;
import id.payu.auth.domain.model.UserKnownIpEntity;
import id.payu.auth.adapter.persistence.repository.UserRiskProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RiskEvaluationService {

    private final UserRiskProfileRepository riskProfileRepository;

    @Value("${payu.security.risk.mfa-threshold:50}")
    private int mfaThreshold;

    @Value("${payu.security.risk.new-device-risk:40}")
    private int newDeviceRisk;

    @Value("${payu.security.risk.new-ip-risk:30}")
    private int newIpRisk;

    @Value("${payu.security.risk.failed-attempts-risk:20}")
    private int failedAttemptsRisk;

    @Value("${payu.security.risk.unusual-time-risk:25}")
    private int unusualTimeRisk;

    @Value("${payu.security.risk.unusual-hours-start:22}")
    private int unusualHoursStart;

    @Value("${payu.security.risk.unusual-hours-end:6}")
    private int unusualHoursEnd;

    public RiskEvaluationResult evaluateRisk(LoginContext context) {
        UserRiskProfileEntity profile = getUserRiskProfile(context.username());
        
        int riskScore = 0;
        List<String> riskFactors = new ArrayList<>();
        
        if (isNewDevice(profile, context.deviceId())) {
            riskScore += newDeviceRisk;
            riskFactors.add("new_device");
        }
        
        if (isNewIpAddress(profile, context.ipAddress())) {
            riskScore += newIpRisk;
            riskFactors.add("new_ip_address");
        }
        
        if (profile.getFailedAttempts() > 0) {
            riskScore += profile.getFailedAttempts() * failedAttemptsRisk;
            riskFactors.add("failed_attempts:" + profile.getFailedAttempts());
        }
        
        if (isUnusualLoginTime(context.timestamp())) {
            riskScore += unusualTimeRisk;
            riskFactors.add("unusual_time");
        }
        
        // Disabled for labs environment
        boolean mfaRequired = false; // riskScore >= mfaThreshold;
        
        log.info("Risk evaluation for user {}: score={}, mfa_required={}, factors={}",
                context.username(), riskScore, mfaRequired, riskFactors);
        
        return new RiskEvaluationResult(
                riskScore,
                mfaRequired,
                riskFactors,
                mfaRequired ? "MFA required due to suspicious login patterns" : "Login pattern normal"
        );
    }

    @Transactional
    public void recordSuccessfulLogin(String username, LoginContext context) {
        UserRiskProfileEntity profile = getUserRiskProfile(username);
        
        // Add Device if new
        if (context.deviceId() != null && isNewDevice(profile, context.deviceId())) {
            // profile.addKnownDevice(context.deviceId());
        }
        
        // Add IP if new
        if (context.ipAddress() != null && isNewIpAddress(profile, context.ipAddress())) {
            profile.addKnownIp(context.ipAddress());
        }
        
        profile.setFailedAttempts(0);
        riskProfileRepository.save(profile);
    }

    @Transactional
    public void recordFailedAttempt(String username) {
        UserRiskProfileEntity profile = getUserRiskProfile(username);
        profile.setFailedAttempts(profile.getFailedAttempts() + 1);
        riskProfileRepository.save(profile);
    }

    @Transactional
    public void clearFailedAttempts(String username) {
        riskProfileRepository.findById(username).ifPresent(profile -> {
            profile.setFailedAttempts(0);
            riskProfileRepository.save(profile);
        });
    }

    private UserRiskProfileEntity getUserRiskProfile(String username) {
        return riskProfileRepository.findById(username)
                .orElseGet(() -> {
                     UserRiskProfileEntity newProfile = new UserRiskProfileEntity();
                     newProfile.setUsername(username);
                     newProfile.setFailedAttempts(0);
                     return newProfile;
                });
    }

    private boolean isNewDevice(UserRiskProfileEntity profile, String deviceId) {
        if (deviceId == null) return false;
        if (profile.getKnownDevices() == null) return true;
        
        return profile.getKnownDevices().stream()
                .noneMatch(d -> d.getDeviceId().equals(deviceId));
    }

    private boolean isNewIpAddress(UserRiskProfileEntity profile, String ipAddress) {
        if (ipAddress == null) return false;
        if (profile.getKnownIps() == null) return true;
        
        return profile.getKnownIps().stream()
                .noneMatch(ip -> ip.getIpAddress().equals(ipAddress));
    }

    private boolean isUnusualLoginTime(Long timestamp) {
        if (timestamp == null) {
            return false;
        }
        LocalTime loginTime = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalTime();
        int hour = loginTime.getHour();
        return hour >= unusualHoursStart || hour < unusualHoursEnd;
    }

    /**
     * Checks if a user account is active (not locked due to too many failed attempts).
     *
     * @param userId the user ID to check
     * @return true if account is active, false if locked
     */
    public boolean isAccountActive(String userId) {
        return riskProfileRepository.findById(userId)
                .map(profile -> {
                    // Consider account inactive if failed attempts exceed threshold
                    boolean isActive = profile.getFailedAttempts() < mfaThreshold;
                    if (!isActive) {
                        log.warn("Account {} is inactive due to {} failed attempts",
                                userId, profile.getFailedAttempts());
                    }
                    return isActive;
                })
                .orElse(true); // New users are considered active
    }

    public static class RiskEvaluationResult {
        private final int riskScore;
        private final boolean mfaRequired;
        private final List<String> riskFactors;
        private final String message;

        public RiskEvaluationResult(int riskScore, boolean mfaRequired, 
                                   List<String> riskFactors, String message) {
            this.riskScore = riskScore;
            this.mfaRequired = mfaRequired;
            this.riskFactors = riskFactors;
            this.message = message;
        }

        public int getRiskScore() {
            return riskScore;
        }

        public boolean isMfaRequired() {
            return mfaRequired;
        }

        public List<String> getRiskFactors() {
            return riskFactors;
        }

        public String getMessage() {
            return message;
        }
    }
}
