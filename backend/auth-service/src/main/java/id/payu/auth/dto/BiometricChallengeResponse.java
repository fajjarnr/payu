package id.payu.auth.dto;

public record BiometricChallengeResponse(
        String challenge,
        String challengeId,
        Long expiresAt,
        String message
) {
}
