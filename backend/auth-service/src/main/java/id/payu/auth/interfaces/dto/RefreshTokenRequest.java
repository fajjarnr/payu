package id.payu.auth.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import id.payu.security.annotation.Sensitive;
import id.payu.security.annotation.SensitivityLevel;

public record RefreshTokenRequest(
    @JsonProperty("refresh_token") @Sensitive(value = SensitivityLevel.CRITICAL) String refreshToken
) {}
