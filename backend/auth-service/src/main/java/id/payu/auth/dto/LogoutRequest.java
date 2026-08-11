package id.payu.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Logout request (LOGIN-002): the refresh token identifies the session to revoke
 * at the identity provider.
 */
public record LogoutRequest(
        @JsonProperty("refresh_token")
        @NotBlank(message = "Refresh token is required")
        String refreshToken) {
}
