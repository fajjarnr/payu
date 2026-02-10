package id.payu.auth.application.service;

import id.payu.auth.domain.model.LoginContext;
import id.payu.auth.domain.model.UserRiskProfileEntity;
import id.payu.auth.adapter.persistence.repository.UserRiskProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("RiskEvaluationService")
@ExtendWith(MockitoExtension.class)
class RiskEvaluationServiceTest {

    @Mock
    private UserRiskProfileRepository riskProfileRepository;

    private RiskEvaluationService riskEvaluationService;

    @BeforeEach
    void setUp() {
        riskEvaluationService = new RiskEvaluationService(riskProfileRepository);
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
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(0);
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));

            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "new-device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            // When
            RiskEvaluationService.RiskEvaluationResult result = riskEvaluationService.evaluateRisk(context);

            // Then
            assertThat(result.isMfaRequired()).isTrue();
            assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(40);
            assertThat(result.getRiskFactors()).contains("new_device");
        }

        @Test
        @DisplayName("should require MFA for new IP address")
        void shouldRequireMFAForNewIpAddress() {
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(0);
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));

            LoginContext context = new LoginContext(
                    "testuser",
                    "10.0.0.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            // When
            RiskEvaluationService.RiskEvaluationResult result = riskEvaluationService.evaluateRisk(context);

            // Then
            assertThat(result.isMfaRequired()).isTrue();
            assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(30);
            assertThat(result.getRiskFactors()).contains("new_ip_address");
        }

        @Test
        @DisplayName("should require MFA for failed attempts")
        void shouldRequireMFAForFailedAttempts() {
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(3);
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));

            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            // When
            RiskEvaluationService.RiskEvaluationResult result = riskEvaluationService.evaluateRisk(context);

            // Then
            assertThat(result.isMfaRequired()).isTrue();
            assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(60);
        }

        @Test
        @DisplayName("should not require MFA for normal login from known device")
        void shouldNotRequireMFAForNormalLogin() {
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(0);
            profile.addKnownDevice("device-123");
            profile.addKnownIp("192.168.1.1");
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));

            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            // When
            RiskEvaluationService.RiskEvaluationResult result = riskEvaluationService.evaluateRisk(context);

            // Then
            // MFA should not be required for known device/IP
            assertThat(result.isMfaRequired()).isFalse();
            assertThat(result.getRiskScore()).isLessThan(50);
            // Note: unusual_time may still be included depending on time-based logic
            assertThat(result.getRiskFactors()).doesNotContain("new_device", "new_ip_address");
        }

        @Test
        @DisplayName("should calculate cumulative risk score correctly")
        void shouldCalculateCumulativeRiskScore() {
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(0);
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));

            LoginContext context = new LoginContext(
                    "testuser",
                    "10.0.0.1",
                    "new-device-456",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            // When
            RiskEvaluationService.RiskEvaluationResult result = riskEvaluationService.evaluateRisk(context);

            // Then
            // Score includes new_device, new_ip_address, and possibly unusual_time
            assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(70);
            assertThat(result.getRiskFactors()).contains("new_device", "new_ip_address");
        }
    }

    @Nested
    @DisplayName("recordSuccessfulLogin")
    class RecordSuccessfulLogin {

        @Test
        @DisplayName("should mark device and IP as known")
        void shouldMarkDeviceAndIpAsKnown() {
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(0);
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));
            given(riskProfileRepository.save(any(UserRiskProfileEntity.class))).willReturn(profile);

            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            // When
            riskEvaluationService.recordSuccessfulLogin("testuser", context);

            // Then
            assertThat(profile.getKnownDevices()).anyMatch(d -> d.getDeviceId().equals("device-123"));
            assertThat(profile.getKnownIps()).anyMatch(ip -> ip.getIpAddress().equals("192.168.1.1"));
            assertThat(profile.getFailedAttempts()).isEqualTo(0);
            verify(riskProfileRepository).save(profile);
        }

        @Test
        @DisplayName("should clear failed attempts on success")
        void shouldClearFailedAttemptsOnSuccess() {
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(5);
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));
            given(riskProfileRepository.save(any(UserRiskProfileEntity.class))).willReturn(profile);

            LoginContext context = new LoginContext(
                    "testuser",
                    "192.168.1.1",
                    "device-123",
                    "Mozilla/5.0",
                    System.currentTimeMillis()
            );

            // When
            riskEvaluationService.recordSuccessfulLogin("testuser", context);

            // Then
            assertThat(profile.getFailedAttempts()).isEqualTo(0);
            verify(riskProfileRepository).save(profile);
        }
    }

    @Nested
    @DisplayName("recordFailedAttempt")
    class RecordFailedAttempt {

        @Test
        @DisplayName("should increment failed attempt counter")
        void shouldIncrementFailedAttemptCounter() {
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(0);
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));
            given(riskProfileRepository.save(any(UserRiskProfileEntity.class))).willReturn(profile);

            // When
            riskEvaluationService.recordFailedAttempt("testuser");

            // Then
            assertThat(profile.getFailedAttempts()).isEqualTo(1);
            verify(riskProfileRepository).save(profile);
        }

        @Test
        @DisplayName("should accumulate failed attempts")
        void shouldAccumulateFailedAttempts() {
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(0);
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));
            given(riskProfileRepository.save(any(UserRiskProfileEntity.class))).willReturn(profile);

            // When
            riskEvaluationService.recordFailedAttempt("testuser");
            riskEvaluationService.recordFailedAttempt("testuser");
            riskEvaluationService.recordFailedAttempt("testuser");

            // Then
            assertThat(profile.getFailedAttempts()).isEqualTo(3);
            verify(riskProfileRepository).save(profile);
        }
    }

    @Nested
    @DisplayName("clearFailedAttempts")
    class ClearFailedAttempts {

        @Test
        @DisplayName("should clear failed attempt counter")
        void shouldClearFailedAttemptCounter() {
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(5);
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));
            given(riskProfileRepository.save(any(UserRiskProfileEntity.class))).willReturn(profile);

            // When
            riskEvaluationService.clearFailedAttempts("testuser");

            // Then
            assertThat(profile.getFailedAttempts()).isEqualTo(0);
            verify(riskProfileRepository).save(profile);
        }

        @Test
        @DisplayName("should do nothing when user not found")
        void shouldDoNothingWhenUserNotFound() {
            // Given
            given(riskProfileRepository.findById("nonexistent")).willReturn(Optional.empty());

            // When
            riskEvaluationService.clearFailedAttempts("nonexistent");

            // Then
            verify(riskProfileRepository, never()).save(any(UserRiskProfileEntity.class));
        }
    }

    @Nested
    @DisplayName("isAccountActive")
    class IsAccountActive {

        @Test
        @DisplayName("should return true when account has no failed attempts")
        void shouldReturnTrueWhenNoFailedAttempts() {
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(0);
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));

            // When
            boolean isActive = riskEvaluationService.isAccountActive("testuser");

            // Then
            assertThat(isActive).isTrue();
        }

        @Test
        @DisplayName("should return true when user does not exist (new user)")
        void shouldReturnTrueWhenUserDoesNotExist() {
            // Given
            given(riskProfileRepository.findById("newuser")).willReturn(Optional.empty());

            // When
            boolean isActive = riskEvaluationService.isAccountActive("newuser");

            // Then
            assertThat(isActive).isTrue();
        }

        @Test
        @DisplayName("should return false when failed attempts exceed threshold")
        void shouldReturnFalseWhenFailedAttemptsExceedThreshold() {
            // Given
            UserRiskProfileEntity profile = new UserRiskProfileEntity();
            profile.setUsername("testuser");
            profile.setFailedAttempts(60); // Exceeds mfaThreshold of 50
            given(riskProfileRepository.findById("testuser")).willReturn(Optional.of(profile));

            // When
            boolean isActive = riskEvaluationService.isAccountActive("testuser");

            // Then
            assertThat(isActive).isFalse();
        }
    }
}
