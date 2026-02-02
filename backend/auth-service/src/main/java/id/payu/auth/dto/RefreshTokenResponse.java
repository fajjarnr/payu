package id.payu.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Response DTO for token refresh operation.
 * Contains the new access token and refresh token after rotation.
 *
 * Implements OAuth 2.0 Token Response specification with additional metadata.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-5.1">RFC 6749 - Access Token Response</a>
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
    /**
     * Creates a new RefreshTokenResponse.
     *
     * @param accessToken The new JWT access token
     * @param refreshToken The new refresh token (rotated)
     * @param expiresIn Access token expiry time in seconds
     * @param refreshExpiresIn Refresh token expiry time in seconds
     * @param tokenType Token type (typically "Bearer")
     */
    public RefreshTokenResponse {}
}
