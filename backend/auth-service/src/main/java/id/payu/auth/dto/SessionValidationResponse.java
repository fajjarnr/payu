package id.payu.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Response DTO for session validation endpoint.
 * Contains minimal user session data without exposing sensitive information.
 *
 * SECURITY NOTE: Only non-sensitive data is returned.
 * PII like full NIK, phone number, email are NOT included.
 */
public record SessionValidationResponse(
        @JsonProperty("valid") boolean valid,
        @JsonProperty("user_id") String userId,
        @JsonProperty("username") String username,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("roles") Set<String> roles,
        @JsonProperty("session_active") boolean sessionActive
) {
    /**
     * Creates a response for an invalid session.
     */
    public static SessionValidationResponse invalid() {
        return new SessionValidationResponse(false, null, null, 0L, Set.of(), false);
    }

    /**
     * Creates a response for a valid session.
     */
    public static SessionValidationResponse valid(String userId, String username, long expiresIn, Set<String> roles) {
        return new SessionValidationResponse(true, userId, username, expiresIn, roles, true);
    }
}
