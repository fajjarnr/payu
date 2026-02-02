package id.payu.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import id.payu.security.annotation.Sensitive;

public record RefreshTokenRequest(
    @JsonProperty("refresh_token") @Sensitive(value = Sensitive.SensitivityLevel.CRITICAL) String refreshToken
) {}
