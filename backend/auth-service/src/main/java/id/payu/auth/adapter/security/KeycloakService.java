package id.payu.auth.adapter.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.auth.config.KeycloakConfig;
import id.payu.auth.interfaces.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import jakarta.ws.rs.core.Response;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final Keycloak keycloakAdmin;
    private final KeycloakConfig keycloakConfig;
    private final ObjectMapper objectMapper;
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    @Value("${payu.security.password-policy.min-length:12}")
    private int passwordMinLength;

    @Value("${payu.security.password-policy.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${payu.security.password-policy.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${payu.security.password-policy.require-digit:true}")
    private boolean requireDigit;

    @Value("${payu.security.password-policy.require-special-char:true}")
    private boolean requireSpecialChar;

    /**
     * LOGIN-003: exchange an OIDC authorization code (PKCE) at the Keycloak
     * token endpoint with the confidential web client (payu-web-app). The
     * browser never sends credentials to this service — Keycloak's
     * authorization endpoint authenticated the user and returned the code.
     *
     * @param code         the authorization code from the OIDC callback
     * @param codeVerifier the PKCE code_verifier held by the BFF
     * @param redirectUri  the exact redirect_uri used in the authorize request
     * @return token response
     * @throws IllegalArgumentException when Keycloak rejects the code (invalid/expired)
     * @throws ResourceAccessException  when the identity provider is unreachable
     */
    public LoginResponse exchangeAuthorizationCode(String code, String codeVerifier, String redirectUri) {
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakConfig.getServerUrl(), keycloakConfig.getRealm());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", keycloakConfig.getWebClientId());
        form.add("client_secret", keycloakConfig.getWebClientSecret());
        form.add("code", code);
        form.add("code_verifier", codeVerifier);
        form.add("redirect_uri", redirectUri);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenEndpoint, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String accessToken = root.get("access_token").asText();
            String refreshToken = root.has("refresh_token") ? root.get("refresh_token").asText() : null;
            long expiresIn = root.get("expires_in").asLong();
            String tokenType = root.get("token_type").asText();

            log.info("OIDC authorization code exchanged successfully");
            return new LoginResponse(accessToken, refreshToken, expiresIn, tokenType);
        } catch (HttpClientErrorException e) {
            log.warn("Authorization code exchange rejected by Keycloak: {}", e.getStatusCode());
            throw new IllegalArgumentException("Invalid or expired authorization code", e);
        } catch (ResourceAccessException e) {
            log.error("Keycloak unreachable during code exchange: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to exchange authorization code: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to exchange authorization code", e);
        }
    }

    /**
     * Revokes a session at Keycloak (LOGIN-002). The refresh token identifies
     * the session; Keycloak's end_session endpoint revokes it server-side so a
     * subsequent refresh with the same token is rejected. Uses the web client
     * because the session was issued through the OIDC authorization-code flow.
     *
     * @param refreshToken the refresh token to revoke
     * @throws IllegalArgumentException if the token cannot be parsed or is rejected
     */
    public void revokeSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        String endSessionEndpoint = String.format("%s/realms/%s/protocol/openid-connect/logout",
                keycloakConfig.getServerUrl(), keycloakConfig.getRealm());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakConfig.getWebClientId());
        if (keycloakConfig.getWebClientSecret() != null && !keycloakConfig.getWebClientSecret().isBlank()) {
            // Confidential client: Keycloak's end_session requires client
            // authentication (id_token_hint or client secret) to revoke.
            form.add("client_secret", keycloakConfig.getWebClientSecret());
        }
        form.add("refresh_token", refreshToken);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endSessionEndpoint, request, String.class);
            log.info("Keycloak session revocation returned status: {}", response.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.warn("Session revocation rejected by Keycloak: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid refresh token", e);
        } catch (Exception e) {
            log.error("Session revocation failed: {}", e.getMessage());
            throw new ResourceAccessException("Keycloak unreachable during logout: " + e.getMessage());
        }
    }

    /**
     * Blocking version of refresh for the web session (BFF refresh endpoint).
     * Uses the web client because the refresh token was issued to it.
     *
     * @param refreshToken the refresh token
     * @return LoginResponse containing new access tokens
     * @throws IllegalArgumentException if refresh fails
     */
    public LoginResponse refreshTokenBlocking(String refreshToken) {
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakConfig.getServerUrl(), keycloakConfig.getRealm());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(buildRefreshForm(refreshToken), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenEndpoint, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String accessToken = root.get("access_token").asText();
            String newRefreshToken = root.has("refresh_token") ? root.get("refresh_token").asText() : null;
            long expiresIn = root.get("expires_in").asLong();
            String tokenType = root.get("token_type").asText();

            log.info("Token refreshed successfully (blocking)");
            return new LoginResponse(accessToken, newRefreshToken, expiresIn, tokenType);
        } catch (Exception e) {
            log.error("Token refresh failed (blocking): {}", e.getMessage());
            throw new IllegalArgumentException("Failed to parse refresh response or server error: " + e.getMessage(), e);
        }
    }

    public String createUser(String username, String email, String password, String fullName) {
        validatePassword(password);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true);

        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            user.setFirstName(parts[0]);
            user.setLastName(parts.length > 1 ? parts[1] : parts[0]);
        }

        Response response = keycloakAdmin.realm(keycloakConfig.getRealm())
                .users().create(user);

        if (response.getStatus() != 201) {
            log.error("Failed to create user in Keycloak: Status {}", response.getStatus());
            throw new RuntimeException("Failed to register user in IAM");
        }

        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        keycloakAdmin.realm(keycloakConfig.getRealm())
                .users().get(userId).resetPassword(credential);

        log.info("Created user {} in Keycloak with ID {}", maskUsername(username), userId);
        return userId;
    }

    /**
     * ACCOUNT-005: remove a user from Keycloak (saga compensation for
     * provisioning). The admin client's {@code remove()} issues the REST
     * DELETE and throws on non-2xx; idempotent server-side, so double
     * compensation is harmless.
     *
     * @param userId the Keycloak user id to remove
     */
    public void deleteUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }
        keycloakAdmin.realm(keycloakConfig.getRealm())
                .users().get(userId).remove();
        log.info("Deleted user {} in Keycloak", userId);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < passwordMinLength) {
            throw new IllegalArgumentException("Password must be at least " + passwordMinLength + " characters long");
        }
        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (requireLowercase && !password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (requireDigit && !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        if (requireSpecialChar && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }

    private MultiValueMap<String, String> buildRefreshForm(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakConfig.getWebClientId());
        form.add("client_secret", keycloakConfig.getWebClientSecret());
        form.add("refresh_token", refreshToken);
        return form;
    }

    /**
     * Masks a username for safe logging (BUG-BE-016).
     * Shows first 2 and last 2 characters, masks the rest.
     * Example: "johndoe" → "jo***oe"
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 4) {
            return "****";
        }
        return username.substring(0, 2) + "***" + username.substring(username.length() - 2);
    }
}
