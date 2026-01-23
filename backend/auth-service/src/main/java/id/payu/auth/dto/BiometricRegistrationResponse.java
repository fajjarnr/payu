package id.payu.auth.dto;

import java.time.Instant;

public record BiometricRegistrationResponse(
        String registrationId,
        String username,
        String deviceId,
        String deviceType,
        String publicKey,
        Instant registeredAt,
        String message
) {
}
