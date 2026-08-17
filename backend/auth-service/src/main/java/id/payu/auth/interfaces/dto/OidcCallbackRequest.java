package id.payu.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * OIDC authorization-code callback payload (LOGIN-003).
 * The browser never sends credentials — Keycloak authenticated the user at
 * its own login page; this service only exchanges the code + PKCE verifier.
 */
public record OidcCallbackRequest(
    @NotBlank(message = "Authorization code is required")
    @Size(max = 4096, message = "Authorization code is too long")
    String code,

    @NotBlank(message = "PKCE code verifier is required")
    @Size(min = 43, max = 128, message = "Code verifier must be between 43 and 128 characters")
    String codeVerifier,

    @NotBlank(message = "redirectUri is required")
    @Size(max = 512, message = "redirectUri is too long")
    String redirectUri
) {
    @Override
    public String toString() {
        return "OidcCallbackRequest[code=***, codeVerifier=***, redirectUri=" + redirectUri + "]";
    }
}
