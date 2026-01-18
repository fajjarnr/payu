package id.payu.auth.service;

import id.payu.auth.config.KeycloakConfig;
import id.payu.auth.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final Keycloak keycloakAdmin;
    private final KeycloakConfig keycloakConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public LoginResponse login(String username, String password) {
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token", 
                keycloakConfig.getServerUrl(), keycloakConfig.getRealm());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", keycloakConfig.getClientId());
        body.add("client_secret", keycloakConfig.getClientSecret());
        body.add("username", username);
        body.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                    tokenEndpoint, request, LoginResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Login failed for user {}: {}", username, e.getMessage());
            throw new IllegalArgumentException("Invalid credentials or login failed");
        }
    }

    public void createUser(String username, String email, String password) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true); // Auto-verify for simulation

        Response response = keycloakAdmin.realm(keycloakConfig.getRealm())
                .users().create(user);

        if (response.getStatus() != 201) {
            log.error("Failed to create user in Keycloak: Status {}", response.getStatus());
            throw new RuntimeException("Failed to register user in IAM");
        }

        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        // Set Password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        keycloakAdmin.realm(keycloakConfig.getRealm())
                .users().get(userId).resetPassword(credential);
        
        log.info("Created user {} in Keycloak with ID {}", username, userId);
    }
}
