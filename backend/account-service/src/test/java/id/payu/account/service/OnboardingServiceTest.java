package id.payu.account.service;

import id.payu.account.dto.DukcapilResponse;
import id.payu.account.dto.RegisterUserRequest;
import id.payu.account.entity.User;
import id.payu.account.repository.ProfileRepository;
import id.payu.account.repository.UserRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OnboardingService
 * Following TDD approach - tests define expected behavior
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingService")
class OnboardingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private GatewayClient gatewayClient;

    @InjectMocks
    private OnboardingService onboardingService;

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
                "3201234567890001"
        );

        successfulKycResponse = new DukcapilResponse(
                "REQ-001",
                "3201234567890001",
                true,
                "VERIFIED",
                "00",
                "Verification successful"
        );
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("should successfully register new user when all validations pass")
        void shouldRegisterUserSuccessfully() throws ExecutionException, InterruptedException {
            // Given
            given(userRepository.existsByEmail(validRequest.email())).willReturn(false);
            given(userRepository.existsByUsername(validRequest.username())).willReturn(false);
            given(gatewayClient.verifyNik(any())).willReturn(successfulKycResponse);
            
            User savedUser = User.builder()
                    .id(UUID.randomUUID())
                    .externalId(validRequest.externalId())
                    .username(validRequest.username())
                    .email(validRequest.email())
                    .phoneNumber(validRequest.phoneNumber())
                    .status(User.UserStatus.ACTIVE)
                    .kycStatus(User.KycStatus.APPROVED)
                    .build();
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // When
            CompletableFuture<User> result = onboardingService.registerUser(validRequest);
            User registeredUser = result.get();

            // Then
            assertThat(registeredUser).isNotNull();
            assertThat(registeredUser.getEmail()).isEqualTo(validRequest.email());
            assertThat(registeredUser.getUsername()).isEqualTo(validRequest.username());
            assertThat(registeredUser.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
            assertThat(registeredUser.getKycStatus()).isEqualTo(User.KycStatus.APPROVED);

            verify(userRepository).existsByEmail(validRequest.email());
            verify(userRepository).existsByUsername(validRequest.username());
            verify(gatewayClient).verifyNik(any());
            verify(userRepository).save(any(User.class));
            verify(profileRepository).save(any());
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            given(userRepository.existsByEmail(validRequest.email())).willReturn(true);

            // When/Then
            CompletableFuture<User> result = onboardingService.registerUser(validRequest);
            
            assertThatThrownBy(result::get)
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository).existsByEmail(validRequest.email());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            // Given
            given(userRepository.existsByEmail(validRequest.email())).willReturn(false);
            given(userRepository.existsByUsername(validRequest.username())).willReturn(true);

            // When/Then
            CompletableFuture<User> result = onboardingService.registerUser(validRequest);
            
            assertThatThrownBy(result::get)
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Username already taken");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should throw exception when KYC verification fails")
        void shouldThrowExceptionWhenKycFails() {
            // Given
            DukcapilResponse failedKycResponse = new DukcapilResponse(
                    "REQ-002",
                    "3201234567890001",
                    false,
                    "NOT_FOUND",
                    "01",
                    "NIK not found in Dukcapil database"
            );
            
            given(userRepository.existsByEmail(validRequest.email())).willReturn(false);
            given(userRepository.existsByUsername(validRequest.username())).willReturn(false);
            given(gatewayClient.verifyNik(any())).willReturn(failedKycResponse);

            // When/Then
            CompletableFuture<User> result = onboardingService.registerUser(validRequest);
            
            assertThatThrownBy(result::get)
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("NIK Verification Failed");

            verify(userRepository, never()).save(any(User.class));
        }
    }
}
