package id.payu.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MFAResponse(
    @JsonProperty("mfa_required") boolean mfaRequired,
    @JsonProperty("mfa_token") String mfaToken,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("message") String message
) {}
