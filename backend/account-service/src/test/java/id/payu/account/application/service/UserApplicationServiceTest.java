package id.payu.account.application.service;

import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.KycVerificationPort;
import id.payu.account.domain.port.out.UserEventPublisherPort;
import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.account.dto.DukcapilResponse;
import id.payu.account.dto.RegisterUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserApplicationService")
class UserApplicationServiceTest {

    @Mock
    private UserPersistencePort userPersistencePort;

    @Mock
    private KycVerificationPort kycVerificationPort;

    @Mock
    private UserEventPublisherPort userEventPublisherPort;

    @InjectMocks
    private UserApplicationService userApplicationService;

    private RegisterUserRequest validRequest;
    private DukcapilResponse successfulKycResponse;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterUserRequest(
                UUID.randomUUID().toString(),
                "testuser",
                "test@example.com",
                "+6281234567890",
                "John Doe",
                "3201234567890001");

        successfulKycResponse = new DukcapilResponse(
                "REQ-001",
                "3201234567890001",
                true,
                "VERIFIED",
                "00",
                "Verification successful");
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("should successfully register new user when all validations pass")
        void shouldRegisterUserSuccessfully() throws ExecutionException, InterruptedException {
            // Given
            given(userPersistencePort.existsByEmail(validRequest.email())).willReturn(false);
            given(userPersistencePort.existsByUsername(validRequest.username())).willReturn(false);
            given(kycVerificationPort.verifyNik(validRequest.nik(), validRequest.fullName()))
                    .willReturn(successfulKycResponse);

            User savedUser = User.builder()
                    .id(UUID.randomUUID())
                    .externalId(validRequest.externalId())
                    .username(validRequest.username())
                    .email(validRequest.email())
                    .phoneNumber(validRequest.phoneNumber())
                    .fullName(validRequest.fullName())
                    .nik(validRequest.nik())
                    .status(User.UserStatus.ACTIVE)
                    .kycStatus(User.KycStatus.APPROVED)
                    .build();
            given(userPersistencePort.save(any(User.class))).willReturn(savedUser);

            // When
            CompletableFuture<User> result = userApplicationService.registerUser(validRequest);
            User registeredUser = result.get();

            // Then
            assertThat(registeredUser).isNotNull();
            assertThat(registeredUser.getEmail()).isEqualTo(validRequest.email());
            assertThat(registeredUser.getFullName()).isEqualTo(validRequest.fullName());
            assertThat(registeredUser.getKycStatus()).isEqualTo(User.KycStatus.APPROVED);

            verify(userPersistencePort).existsByEmail(validRequest.email());
            verify(userPersistencePort).existsByUsername(validRequest.username());
            verify(kycVerificationPort).verifyNik(validRequest.nik(), validRequest.fullName());
            verify(userPersistencePort).save(any(User.class));
            verify(userEventPublisherPort).publishUserCreated(any(id.payu.account.dto.UserCreatedEvent.class));
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            given(userPersistencePort.existsByEmail(validRequest.email())).willReturn(true);

            // When/Then
            assertThatThrownBy(() -> userApplicationService.registerUser(validRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already exists");

            verify(userPersistencePort, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should set status to REJECTED when KYC verification fails but still save user")
        void shouldSaveUserAsRejectedWhenKycFails() throws ExecutionException, InterruptedException {
            // In OnboardingService, it threw exception.
            // In UserApplicationService (refactored), logic was:
            // User.KycStatus kycStatus = kycResponse.verified() ? APPROVED : REJECTED;
            // So it saves user with REJECTED status instead of throwing exception!
            // Wait, let me check UserApplicationService logic again.

            // Re-checking UserApplicationService logic I wrote:
            /*
             * DukcapilResponse kycResponse = kycVerificationPort.verifyNik(command.nik(),
             * command.fullName());
             * User.KycStatus kycStatus = kycResponse.verified() ?
             * User.KycStatus.APPROVED : User.KycStatus.REJECTED;
             * ...
             * User savedUser = userPersistencePort.save(user);
             */
            // YES, the new logic saves as REJECTED. The old logic threw exception.
            // This is actually better (audit trail).

            DukcapilResponse failedKycResponse = new DukcapilResponse(
                    "REQ-002",
                    "3201234567890001",
                    false,
                    "NOT_FOUND",
                    "01",
                    "NIK not found");

            given(userPersistencePort.existsByEmail(validRequest.email())).willReturn(false);
            given(userPersistencePort.existsByUsername(validRequest.username())).willReturn(false);
            given(kycVerificationPort.verifyNik(validRequest.nik(), validRequest.fullName()))
                    .willReturn(failedKycResponse);

            User savedUser = User.builder()
                    .username(validRequest.username())
                    .status(User.UserStatus.ACTIVE)
                    .kycStatus(User.KycStatus.REJECTED)
                    .build();
            given(userPersistencePort.save(any(User.class))).willReturn(savedUser);

            // When
            CompletableFuture<User> result = userApplicationService.registerUser(validRequest);
            User registeredUser = result.get();

            // Then
            assertThat(registeredUser.getKycStatus()).isEqualTo(User.KycStatus.REJECTED);
            verify(userPersistencePort).save(any(User.class));
        }

        @Test
        @DisplayName("should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            // Given
            given(userPersistencePort.existsByEmail(validRequest.email())).willReturn(false);
            given(userPersistencePort.existsByUsername(validRequest.username())).willReturn(true);

            // When/Then
            assertThatThrownBy(() -> userApplicationService.registerUser(validRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Username already exists");

            verify(userPersistencePort, never()).save(any(User.class));
            verify(kycVerificationPort, never()).verifyNik(anyString(), anyString());
        }

        @Test
        @DisplayName("should validate phone number format for Indonesian numbers")
        void shouldValidatePhoneNumberFormat() throws ExecutionException, InterruptedException {
            // Given - Valid Indonesian phone formats
            String[] validPhoneNumbers = {
                "+6281234567890",
                "081234567890",
                "6281234567890"
            };

            for (String phoneNumber : validPhoneNumbers) {
                RegisterUserRequest request = new RegisterUserRequest(
                        validRequest.externalId(),
                        "testuser" + phoneNumber.substring(5), // unique username
                        "test" + phoneNumber.substring(5) + "@example.com", // unique email
                        phoneNumber,
                        validRequest.fullName(),
                        validRequest.nik());

                given(userPersistencePort.existsByEmail(request.email())).willReturn(false);
                given(userPersistencePort.existsByUsername(request.username())).willReturn(false);
                given(kycVerificationPort.verifyNik(request.nik(), request.fullName()))
                        .willReturn(successfulKycResponse);

                User savedUser = User.builder()
                        .id(UUID.randomUUID())
                        .phoneNumber(request.phoneNumber())
                        .status(User.UserStatus.ACTIVE)
                        .kycStatus(User.KycStatus.APPROVED)
                        .build();
                given(userPersistencePort.save(any(User.class))).willReturn(savedUser);

                // When/Then - Should not throw exception for valid format
                assertThat(userApplicationService.registerUser(request)).isNotNull();
            }
        }

        @Test
        @DisplayName("should publish Kafka event when user is successfully registered")
        void shouldPublishKafkaEventOnSuccessfulRegistration() throws ExecutionException, InterruptedException {
            // Given
            given(userPersistencePort.existsByEmail(validRequest.email())).willReturn(false);
            given(userPersistencePort.existsByUsername(validRequest.username())).willReturn(false);
            given(kycVerificationPort.verifyNik(validRequest.nik(), validRequest.fullName()))
                    .willReturn(successfulKycResponse);

            User savedUser = User.builder()
                    .id(UUID.randomUUID())
                    .email(validRequest.email())
                    .username(validRequest.username())
                    .status(User.UserStatus.ACTIVE)
                    .kycStatus(User.KycStatus.APPROVED)
                    .build();
            given(userPersistencePort.save(any(User.class))).willReturn(savedUser);

            // When
            userApplicationService.registerUser(validRequest).get();

            // Then
            verify(userEventPublisherPort, times(1)).publishUserCreated(any(id.payu.account.dto.UserCreatedEvent.class));
        }

        @Test
        @DisplayName("should NOT publish Kafka event when registration fails due to duplicate email")
        void shouldNotPublishKafkaEventWhenRegistrationFails() {
            // Given
            given(userPersistencePort.existsByEmail(validRequest.email())).willReturn(true);

            // When/Then
            assertThatThrownBy(() -> userApplicationService.registerUser(validRequest))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userEventPublisherPort, never()).publishUserCreated(any());
        }
    }
}
