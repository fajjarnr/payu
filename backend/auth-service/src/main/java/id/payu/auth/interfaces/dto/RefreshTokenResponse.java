package id.payu.auth.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * OAuth 2.0 token response per RFC 6749 §5.1, with refresh rotation metadata.
 */
public record RefreshTokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("refresh_expires_in")
        long refreshExpiresIn,

        @JsonProperty("token_type")
        String tokenType
) {
    public RefreshTokenResponse {}
}
