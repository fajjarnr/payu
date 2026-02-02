package id.payu.auth.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.NotBlank;

public record BiometricAuthenticationRequest(
        @NotBlank(message = "Username is required")
        @Sensitive
        String username,

        @NotBlank(message = "Device identifier is required")
        String deviceId,

        @NotBlank(message = "Challenge signature is required")
        @Sensitive
        String challengeSignature,

        @NotBlank(message = "Challenge string is required")
        @Sensitive
        String challenge
) {
}
