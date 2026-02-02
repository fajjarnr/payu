package id.payu.auth.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BiometricRegistrationRequest(
        @NotBlank(message = "Username is required")
        @Sensitive
        String username,

        @NotBlank(message = "Public key is required")
        @Sensitive
        String publicKey,

        @NotBlank(message = "Device identifier is required")
        String deviceId,

        @NotBlank(message = "Device type is required")
        String deviceType,

        @NotBlank(message = "Challenge signature is required")
        @Sensitive
        String challengeSignature,

        @NotBlank(message = "Challenge string is required")
        @Sensitive
        String challenge
) {
}
