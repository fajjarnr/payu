package id.payu.auth.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Session validation response; excludes PII (NIK, phone, email).
 */
public record SessionValidationResponse(
        @JsonProperty("valid") boolean valid,
        @JsonProperty("user_id") String userId,
        @JsonProperty("username") String username,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("roles") Set<String> roles,
        @JsonProperty("session_active") boolean sessionActive
) {
    public static SessionValidationResponse invalid() {
        return new SessionValidationResponse(false, null, null, 0L, Set.of(), false);
    }

    public static SessionValidationResponse valid(String userId, String username, long expiresIn, Set<String> roles) {
        return new SessionValidationResponse(true, userId, username, expiresIn, roles, true);
    }
}
