package id.payu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BiometricRegistrationRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Public key is required")
        String publicKey,

        @NotBlank(message = "Device identifier is required")
        String deviceId,

        @NotBlank(message = "Device type is required")
        String deviceType,

        @NotBlank(message = "Challenge signature is required")
        String challengeSignature,

        @NotBlank(message = "Challenge string is required")
        String challenge
) {
}
