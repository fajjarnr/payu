package id.payu.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MFAVerifyRequest(
    @NotBlank(message = "Username is required")
    String username,
    
    @NotBlank(message = "MFA token is required")
    String mfaToken,
    
    @NotBlank(message = "OTP code is required")
    String otpCode
) {}
