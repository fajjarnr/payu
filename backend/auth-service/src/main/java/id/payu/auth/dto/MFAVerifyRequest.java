package id.payu.auth.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.NotBlank;

public record MFAVerifyRequest(
    @NotBlank(message = "Username is required")
    @Sensitive
    String username,

    @NotBlank(message = "MFA token is required")
    @Sensitive(value = Sensitive.SensitivityLevel.CRITICAL)
    String mfaToken,

    @NotBlank(message = "OTP code is required")
    @Sensitive(value = Sensitive.SensitivityLevel.CRITICAL)
    String otpCode
) {}
