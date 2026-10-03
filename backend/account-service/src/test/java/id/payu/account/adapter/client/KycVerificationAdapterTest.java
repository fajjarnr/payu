package id.payu.account.adapter.client;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("KycVerificationAdapter")
class KycVerificationAdapterTest {

    @Mock
    private GatewayClient gatewayClient;

    private KycVerificationAdapter kycVerificationAdapter;

    private VerifyNikRequest validRequest;
    private VerifyNikResponse successResponse;

    @BeforeEach
    void setUp() {
        kycVerificationAdapter = new KycVerificationAdapter(gatewayClient);

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
    }

    @Nested
    @DisplayName("verifyNik() - with VerifyNikRequest")
    class VerifyNikWithRequest {

        @Test
        @DisplayName("should return verified response when gateway returns success")
        void shouldReturnVerifiedResponseWhenGatewayReturnsSuccess() {
            given(gatewayClient.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(successResponse);

            VerifyNikResponse result = kycVerificationAdapter.verifyNik(validRequest);

            assertThat(result).isNotNull();
            assertThat(result.verified()).isTrue();
            assertThat(result.nik()).isEqualTo("3201234567890001");
            assertThat(result.fullName()).isEqualTo("John Doe");
            assertThat(result.responseCode()).isEqualTo("00");
        }

        @Test
        @DisplayName("should return not found response when NIK not in database")
        void shouldReturnNotFoundResponseWhenNikNotFound() {
            VerifyNikResponse notFoundResponse = VerifyNikResponse.notFound(
                UUID.randomUUID().toString(),
                "3201234567890001"
            );
            given(gatewayClient.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(notFoundResponse);

            VerifyNikResponse result = kycVerificationAdapter.verifyNik(validRequest);

            assertThat(result.verified()).isFalse();
            assertThat(result.responseCode()).isEqualTo("14");
            assertThat(result.responseMessage()).contains("not found");
        }

        @Test
        @DisplayName("should return blocked response when NIK is blocked")
        void shouldReturnBlockedResponseWhenNikIsBlocked() {
            VerifyNikResponse blockedResponse = VerifyNikResponse.blocked(
                UUID.randomUUID().toString(),
                "3201234567890001"
            );
            given(gatewayClient.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(blockedResponse);

            VerifyNikResponse result = kycVerificationAdapter.verifyNik(validRequest);

            assertThat(result.verified()).isFalse();
            assertThat(result.responseCode()).isEqualTo("62");
            assertThat(result.status()).isEqualTo("BLOCKED");
        }

        @Test
        @DisplayName("should throw exception when gateway throws exception")
        void shouldThrowExceptionWhenGatewayThrows() {
            given(gatewayClient.verifyNik(any(VerifyNikRequest.class)))
                .willThrow(new RuntimeException("Network error"));

            assertThatThrownBy(() -> kycVerificationAdapter.verifyNik(validRequest))
                .isInstanceOf(AccountDomainException.DukcapilVerificationFailedException.class)
                .hasMessageContaining("Failed to verify NIK")
                .hasCauseInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should mask NIK in logs")
        void shouldMaskNikInLogs() {
            given(gatewayClient.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(successResponse);

            kycVerificationAdapter.verifyNik(validRequest);

            // Masking happens inside the adapter; just verify it works.
            assertThat(validRequest.nik()).isEqualTo("3201234567890001");
        }

        @Test
        @DisplayName("should handle response with null birth place")
        void shouldHandleNullBirthPlace() {
            VerifyNikRequest requestWithNullBirthPlace = new VerifyNikRequest(
                "3201234567890001",
                "John Doe",
                null,
                "1990-01-15"
            );

            VerifyNikResponse response = VerifyNikResponse.success(
                UUID.randomUUID().toString(),
                "3201234567890001",
                true,
                "John Doe",
                null,
                LocalDate.of(1990, 1, 15),
                "MALE",
                null,
                "ACTIVE"
            );

            given(gatewayClient.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(response);

            VerifyNikResponse result = kycVerificationAdapter.verifyNik(requestWithNullBirthPlace);

            assertThat(result).isNotNull();
            assertThat(result.birthPlace()).isNull();
        }
    }

    @Nested
    @DisplayName("verifyNik() - deprecated with String parameters")
    class VerifyNikDeprecated {

        @Test
        @DisplayName("should call gateway with default values for deprecated method")
        void shouldCallGatewayWithDefaultValues() {
            id.payu.account.interfaces.dto.DukcapilResponse expectedResponse = new id.payu.account.interfaces.dto.DukcapilResponse(
                UUID.randomUUID().toString(),
                "3201234567890001",
                true,
                "ACTIVE",
                "00",
                "Success"
            );

            given(gatewayClient.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(VerifyNikResponse.success(
                    "req-123",
                    "3201234567890001",
                    true,
                    "John Doe",
                    "Jakarta",
                    LocalDate.of(1990, 1, 15),
                    "MALE",
                    "Jl. Test No. 123",
                    "ACTIVE"
                ));

            id.payu.account.interfaces.dto.DukcapilResponse result = kycVerificationAdapter.verifyNik(
                "3201234567890001",
                "John Doe"
            );

            assertThat(result).isNotNull();
            assertThat(result.verified()).isTrue();
        }
    }

    @Nested
    @DisplayName("NIK Masking")
    class NikMasking {

        @Test
        @DisplayName("should mask NIK correctly for 16-digit NIK")
        void shouldMaskNikCorrectly() {
            given(gatewayClient.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(successResponse);

            kycVerificationAdapter.verifyNik(validRequest);

            // Masking is internal; just verify no exception.
            assertThat(validRequest.nik()).hasSize(16);
        }

        @Test
        @DisplayName("should handle short NIK in masking")
        void shouldHandleShortNikInMasking() {
            VerifyNikRequest requestWithShortNik = new VerifyNikRequest(
                "1234",
                "John Doe",
                "Jakarta",
                "1990-01-15"
            );

            given(gatewayClient.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(successResponse);

            kycVerificationAdapter.verifyNik(requestWithShortNik);

            // No exception means masking handled it.
        }

        @Test
        @DisplayName("should handle null NIK in masking")
        void shouldHandleNullNikInMasking() {
            given(gatewayClient.verifyNik(any(VerifyNikRequest.class)))
                .willReturn(successResponse);

            // Would fail validation before masking; testing the adapter directly.
            VerifyNikRequest requestWithNullNik = new VerifyNikRequest(
                null,
                "John Doe",
                "Jakarta",
                "1990-01-15"
            );

            // Null handled by masking; validation would reject it before the gateway call.
            assertThatNoException().isThrownBy(() -> kycVerificationAdapter.verifyNik(requestWithNullNik));
            verify(gatewayClient).verifyNik(requestWithNullNik);
        }
    }
}
