package id.payu.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record BiometricAuthenticationRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Device identifier is required")
        String deviceId,

        @NotBlank(message = "Challenge signature is required")
        String challengeSignature,

        @NotBlank(message = "Challenge string is required")
        String challenge
) {
}
