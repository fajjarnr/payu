package id.payu.auth.dto;

public record BiometricAuthenticationResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        String deviceId,
        String registrationId,
        String message
) {
}
