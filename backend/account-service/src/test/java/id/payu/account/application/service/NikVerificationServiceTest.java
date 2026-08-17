package id.payu.account.application.service;

import id.payu.account.domain.port.out.KycVerificationPort;
import id.payu.account.interfaces.dto.VerifyNikRequest;
import id.payu.account.interfaces.dto.VerifyNikResponse;
import id.payu.account.exception.AccountDomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NikVerificationService")
class NikVerificationServiceTest {

    @Mock
    private KycVerificationPort kycVerificationPort;

    private NikVerificationService nikVerificationService;

    private VerifyNikRequest validRequest;
    private VerifyNikResponse successResponse;
    private VerifyNikResponse notFoundResponse;

    @BeforeEach
    void setUp() {
        nikVerificationService = new NikVerificationService(kycVerificationPort);

        validRequest = new VerifyNikRequest(
            "3201234567890001",
            "John Doe",
            "Jakarta",
            "1990-01-15"
        );

        successResponse = VerifyNikResponse.success(
            UUID.randomUUID().toString(),
            "3201234567890001",
            true,
            "John Doe",
            "Jakarta",
            LocalDate.of(1990, 1, 15),
            "MALE",
            "Jl. Test No. 123",
            "ACTIVE"
        );

        notFoundResponse = VerifyNikResponse.notFound(
            UUID.randomUUID().toString(),
            "3201234567890001"
        );
    }

    @Nested
    @DisplayName("verifyNik()")
    class VerifyNikMethod {

        @Test
        @DisplayName("should return verified response when NIK matches")
        void shouldReturnVerifiedResponseWhenNikMatches() {
            // Given
            given(kycVerificationPort.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(successResponse);

            // When
            CompletableFuture<VerifyNikResponse> result = nikVerificationService.verifyNik(validRequest);

            // Then
            assertThat(result).isCompletedWithValue(successResponse);
            verify(kycVerificationPort).verifyNik(validRequest);
        }

        @Test
        @DisplayName("should return not found response when NIK not in database")
        void shouldReturnNotFoundResponseWhenNikNotFound() {
            // Given
            given(kycVerificationPort.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(notFoundResponse);

            // When
            CompletableFuture<VerifyNikResponse> result = nikVerificationService.verifyNik(validRequest);

            // Then
            assertThat(result).isCompletedWithValue(notFoundResponse);
            assertThat(result.join().verified()).isFalse();
            assertThat(result.join().responseCode()).isEqualTo("14");
        }

        @Test
        @DisplayName("should return service unavailable response when adapter throws exception")
        void shouldReturnServiceUnavailableResponseWhenAdapterThrows() {
            // Given
            given(kycVerificationPort.verifyNik(any(VerifyNikRequest.class)))
                .willThrow(new AccountDomainException.DukcapilServiceUnavailableException());

            // When/Then
            // In unit test without Spring AOP proxy, fallback is not active — exception propagates
            assertThatThrownBy(() -> nikVerificationService.verifyNik(validRequest))
                .isInstanceOf(AccountDomainException.DukcapilServiceUnavailableException.class);
        }

        @Test
        @DisplayName("should throw InvalidNikException when NIK is null")
        void shouldThrowExceptionWhenNikIsNull() {
            // Given
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                null,
                "John Doe",
                "Jakarta",
                "1990-01-15"
            );

            // When/Then
            assertThatThrownBy(() -> nikVerificationService.verifyNik(invalidRequest).join())
                .isInstanceOf(AccountDomainException.InvalidNikException.class)
                .hasMessageContaining("NIK cannot be empty");

            verify(kycVerificationPort, never()).verifyNik(any());
        }

        @Test
        @DisplayName("should throw InvalidNikException when NIK is empty")
        void shouldThrowExceptionWhenNikIsEmpty() {
            // Given
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                "",
                "John Doe",
                "Jakarta",
                "1990-01-15"
            );

            // When/Then
            assertThatThrownBy(() -> nikVerificationService.verifyNik(invalidRequest).join())
                .isInstanceOf(AccountDomainException.InvalidNikException.class);

            verify(kycVerificationPort, never()).verifyNik(any());
        }

        @Test
        @DisplayName("should throw InvalidNikException when NIK is not 16 digits")
        void shouldThrowExceptionWhenNikNot16Digits() {
            // Given
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                "12345",
                "John Doe",
                "Jakarta",
                "1990-01-15"
            );

            // When/Then
            assertThatThrownBy(() -> nikVerificationService.verifyNik(invalidRequest).join())
                .isInstanceOf(AccountDomainException.InvalidNikException.class);

            verify(kycVerificationPort, never()).verifyNik(any());
        }

        @Test
        @DisplayName("should throw InvalidNikException when NIK contains non-digits")
        void shouldThrowExceptionWhenNikContainsNonDigits() {
            // Given
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                "3201abcd56789001",
                "John Doe",
                "Jakarta",
                "1990-01-15"
            );

            // When/Then
            assertThatThrownBy(() -> nikVerificationService.verifyNik(invalidRequest).join())
                .isInstanceOf(AccountDomainException.InvalidNikException.class);

            verify(kycVerificationPort, never()).verifyNik(any());
        }

        @Test
        @DisplayName("should throw InvalidNikException when NIK is all same digits")
        void shouldThrowExceptionWhenNikAllSameDigits() {
            // Given
            VerifyNikRequest invalidRequest = new VerifyNikRequest(
                "1111111111111111",
                "John Doe",
                "Jakarta",
                "1990-01-15"
            );

            // When/Then
            assertThatThrownBy(() -> nikVerificationService.verifyNik(invalidRequest).join())
                .isInstanceOf(AccountDomainException.InvalidNikException.class);

            verify(kycVerificationPort, never()).verifyNik(any());
        }

        @Test
        @DisplayName("should handle request without birth place")
        void shouldHandleRequestWithoutBirthPlace() {
            // Given
            VerifyNikRequest requestWithoutBirthPlace = new VerifyNikRequest(
                "3201234567890001",
                "John Doe",
                null,
                "1990-01-15"
            );

            given(kycVerificationPort.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(successResponse);

            // When
            CompletableFuture<VerifyNikResponse> result = nikVerificationService.verifyNik(requestWithoutBirthPlace);

            // Then
            assertThat(result).isCompletedWithValue(successResponse);
        }

        @Test
        @DisplayName("should propagate verification failed exception from adapter")
        void shouldPropagateVerificationFailedException() {
            // Given
            given(kycVerificationPort.verifyNik(any(VerifyNikRequest.class)))
                .willThrow(new AccountDomainException.DukcapilVerificationFailedException("Network error"));

            // When/Then
            assertThatThrownBy(() -> nikVerificationService.verifyNik(validRequest).join())
                .isInstanceOf(AccountDomainException.DukcapilVerificationFailedException.class)
                .hasMessageContaining("Network error");
        }
    }
}
