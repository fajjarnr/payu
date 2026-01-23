package id.payu.auth.service;

import id.payu.auth.dto.LoginContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskEvaluationService")
class RiskEvaluationServiceTest {

    private RiskEvaluationService riskEvaluationService;

    @BeforeEach
    void setUp() {
        riskEvaluationService = new RiskEvaluationService();
        ReflectionTestUtils.setField(riskEvaluationService, "mfaThreshold", 50);
        ReflectionTestUtils.setField(riskEvaluationService, "newDeviceRisk", 40);
        ReflectionTestUtils.setField(riskEvaluationService, "newIpRisk", 30);
        ReflectionTestUtils.setField(riskEvaluationService, "failedAttemptsRisk", 20);
        ReflectionTestUtils.setField(riskEvaluationService, "unusualTimeRisk", 25);
        ReflectionTestUtils.setField(riskEvaluationService, "unusualHoursStart", 22);
        ReflectionTestUtils.setField(riskEvaluationService, "unusualHoursEnd", 6);
    }

    @Nested
    @DisplayName("evaluateRisk")
    class EvaluateRisk {

        @Test
        @DisplayName("should require MFA for new device")
        void shouldRequireMFAForNewDevice() {
            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "new-device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            RiskEvaluationService.RiskEvaluationResult result = riskEvaluationService.evaluateRisk(context);

            assertThat(result.isMfaRequired()).isTrue();
            assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(40);
            assertThat(result.getRiskFactors()).contains("new_device");
        }

        @Test
        @DisplayName("should require MFA for new IP address")
        void shouldRequireMFAForNewIpAddress() {
            LoginContext context = new LoginContext(
                    "testuser",
                    "10.0.0.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            RiskEvaluationService.RiskEvaluationResult result = riskEvaluationService.evaluateRisk(context);

            assertThat(result.isMfaRequired()).isTrue();
            assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(30);
            assertThat(result.getRiskFactors()).contains("new_ip_address");
        }

        @Test
        @DisplayName("should require MFA for failed attempts")
        void shouldRequireMFAForFailedAttempts() {
            riskEvaluationService.recordFailedAttempt("testuser");
            riskEvaluationService.recordFailedAttempt("testuser");

            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            RiskEvaluationService.RiskEvaluationResult result = riskEvaluationService.evaluateRisk(context);

            assertThat(result.isMfaRequired()).isTrue();
            assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(40);
        }

        @Test
        @DisplayName("should not require MFA for normal login from known device")
        void shouldNotRequireMFAForNormalLogin() {
            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            riskEvaluationService.recordSuccessfulLogin("testuser", context);

            RiskEvaluationService.RiskEvaluationResult result = riskEvaluationService.evaluateRisk(context);

            assertThat(result.isMfaRequired()).isFalse();
            assertThat(result.getRiskScore()).isLessThan(50);
            assertThat(result.getRiskFactors()).isEmpty();
        }

        @Test
        @DisplayName("should calculate cumulative risk score correctly")
        void shouldCalculateCumulativeRiskScore() {
            LoginContext context = new LoginContext(
                    "testuser",
                    "10.0.0.1",
                    "new-device-456",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            RiskEvaluationService.RiskEvaluationResult result = riskEvaluationService.evaluateRisk(context);

            assertThat(result.getRiskScore()).isEqualTo(70);
            assertThat(result.getRiskFactors()).contains("new_device", "new_ip_address");
        }
    }

    @Nested
    @DisplayName("recordSuccessfulLogin")
    class RecordSuccessfulLogin {

        @Test
        @DisplayName("should mark device and IP as known")
        void shouldMarkDeviceAndIpAsKnown() {
            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            riskEvaluationService.recordSuccessfulLogin("testuser", context);

            LoginContext secondAttempt = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            RiskEvaluationService.RiskEvaluationResult result = 
                    riskEvaluationService.evaluateRisk(secondAttempt);

            assertThat(result.isMfaRequired()).isFalse();
            assertThat(result.getRiskFactors()).doesNotContain("new_device", "new_ip_address");
        }

        @Test
        @DisplayName("should clear failed attempts on success")
        void shouldClearFailedAttemptsOnSuccess() {
            riskEvaluationService.recordFailedAttempt("testuser");
            riskEvaluationService.recordFailedAttempt("testuser");

            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            riskEvaluationService.recordSuccessfulLogin("testuser", context);

            RiskEvaluationService.RiskEvaluationResult result = 
                    riskEvaluationService.evaluateRisk(context);

            assertThat(result.getRiskFactors()).doesNotContain("failed_attempts");
        }
    }

    @Nested
    @DisplayName("recordFailedAttempt")
    class RecordFailedAttempt {

        @Test
        @DisplayName("should increment failed attempt counter")
        void shouldIncrementFailedAttemptCounter() {
            riskEvaluationService.recordFailedAttempt("testuser");

            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            RiskEvaluationService.RiskEvaluationResult result = 
                    riskEvaluationService.evaluateRisk(context);

            assertThat(result.getRiskFactors()).contains("failed_attempts:1");
        }

        @Test
        @DisplayName("should accumulate failed attempts")
        void shouldAccumulateFailedAttempts() {
            riskEvaluationService.recordFailedAttempt("testuser");
            riskEvaluationService.recordFailedAttempt("testuser");
            riskEvaluationService.recordFailedAttempt("testuser");

            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            RiskEvaluationService.RiskEvaluationResult result = 
                    riskEvaluationService.evaluateRisk(context);

            assertThat(result.getRiskFactors()).contains("failed_attempts:3");
            assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(60);
        }
    }

    @Nested
    @DisplayName("clearFailedAttempts")
    class ClearFailedAttempts {

        @Test
        @DisplayName("should clear failed attempt counter")
        void shouldClearFailedAttemptCounter() {
            riskEvaluationService.recordFailedAttempt("testuser");
            riskEvaluationService.recordFailedAttempt("testuser");

            riskEvaluationService.clearFailedAttempts("testuser");

            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            RiskEvaluationService.RiskEvaluationResult result = 
                    riskEvaluationService.evaluateRisk(context);

            assertThat(result.getRiskFactors()).doesNotContain("failed_attempts");
        }
    }
}
