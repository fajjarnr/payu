package id.payu.auth.service;

import id.payu.auth.dto.LoginContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RiskEvaluationService {

    private final Map<String, UserRiskProfile> userRiskProfiles = new ConcurrentHashMap<>();

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
        UserRiskProfile profile = getUserRiskProfile(context.username());
        
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
        
        boolean mfaRequired = riskScore >= mfaThreshold;
        
        log.info("Risk evaluation for user {}: score={}, mfa_required={}, factors={}",
                context.username(), riskScore, mfaRequired, riskFactors);
        
        return new RiskEvaluationResult(
                riskScore,
                mfaRequired,
                riskFactors,
                riskScore >= mfaThreshold ? "MFA required due to suspicious login patterns" : "Login pattern normal"
        );
    }

    public void recordSuccessfulLogin(String username, LoginContext context) {
        UserRiskProfile profile = getUserRiskProfile(username);
        profile.recordSuccessfulLogin(context);
        userRiskProfiles.put(username, profile);
    }

    public void recordFailedAttempt(String username) {
        UserRiskProfile profile = getUserRiskProfile(username);
        profile.recordFailedAttempt();
        userRiskProfiles.put(username, profile);
    }

    public void clearFailedAttempts(String username) {
        UserRiskProfile profile = userRiskProfiles.get(username);
        if (profile != null) {
            profile.clearFailedAttempts();
        }
    }

    private UserRiskProfile getUserRiskProfile(String username) {
        return userRiskProfiles.computeIfAbsent(username, k -> new UserRiskProfile(username));
    }

    private boolean isNewDevice(UserRiskProfile profile, String deviceId) {
        return deviceId != null && !profile.getKnownDevices().contains(deviceId);
    }

    private boolean isNewIpAddress(UserRiskProfile profile, String ipAddress) {
        return ipAddress != null && !profile.getKnownIpAddresses().contains(ipAddress);
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

    private static class UserRiskProfile {
        private final String username;
        private final Set<String> knownDevices;
        private final Set<String> knownIpAddresses;
        private int failedAttempts;

        public UserRiskProfile(String username) {
            this.username = username;
            this.knownDevices = ConcurrentHashMap.newKeySet();
            this.knownIpAddresses = ConcurrentHashMap.newKeySet();
            this.failedAttempts = 0;
        }

        public void recordSuccessfulLogin(LoginContext context) {
            if (context.deviceId() != null) {
                knownDevices.add(context.deviceId());
            }
            if (context.ipAddress() != null) {
                knownIpAddresses.add(context.ipAddress());
            }
            clearFailedAttempts();
        }

        public void recordFailedAttempt() {
            this.failedAttempts++;
        }

        public void clearFailedAttempts() {
            this.failedAttempts = 0;
        }

        public Set<String> getKnownDevices() {
            return Collections.unmodifiableSet(knownDevices);
        }

        public Set<String> getKnownIpAddresses() {
            return Collections.unmodifiableSet(knownIpAddresses);
        }

        public int getFailedAttempts() {
            return failedAttempts;
        }
    }
}
