package id.payu.auth.integration;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import id.payu.auth.dto.LoginRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Auth Service using Testcontainers with Keycloak.
 * 
 * These tests verify the complete authentication flow against a real Keycloak instance
 * running in a Docker container, ensuring proper OAuth2/OIDC integration.
 * 
 * Uses REST API directly to avoid Keycloak Admin Client SDK version mismatch issues.
 * 
 * @author PayU Backend Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = AuthIntegrationTest.KeycloakInitializer.class)
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Auth Service Integration Tests with Keycloak")
public class AuthIntegrationTest {

    private static final String REALM_NAME = "payu";
    private static final String CLIENT_ID = "auth-service";
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "TestPass123!";
    private static final String TEST_EMAIL = "testuser@payu.id";

    static KeycloakContainer keycloak;
    private static RestTemplate restTemplate = new RestTemplate();

    @LocalServerPort
    private int port;

    /**
     * ApplicationContextInitializer that starts Keycloak container BEFORE Spring context loads.
     * This ensures the container is running and port is available when properties are resolved.
     */
    static class KeycloakInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            // Start Keycloak container
            keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0")
                    .withAdminUsername("admin")
                    .withAdminPassword("admin");
            keycloak.start();

            String keycloakUrl = keycloak.getAuthServerUrl();
            
            // Set properties after container is started
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx,
                    "payu.keycloak.server-url=" + keycloakUrl,
                    "payu.keycloak.realm=" + REALM_NAME,
                    "payu.keycloak.client-id=" + CLIENT_ID,
                    "payu.keycloak.client-secret=" + CLIENT_SECRET,
                    "payu.keycloak.admin.username=admin",
                    "payu.keycloak.admin.password=admin",
                    "spring.security.oauth2.resourceserver.jwt.issuer-uri=" + keycloakUrl + "/realms/" + REALM_NAME
            );
        }
    }

    @BeforeAll
    void setupRealm() throws Exception {
        String keycloakUrl = keycloak.getAuthServerUrl();
        
        // Get admin token
        String adminToken = getAdminToken(keycloakUrl);
        
        // Check if realm exists
        if (!realmExists(keycloakUrl, adminToken)) {
            // Create realm
            createRealm(keycloakUrl, adminToken);
            
            // Create client
            createClient(keycloakUrl, adminToken);
        }
        
        // Create test user if not exists
        createTestUserIfNotExists(keycloakUrl, adminToken);
    }

    private String getAdminToken(String keycloakUrl) {
        String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");
        body.add("username", "admin");
        body.add("password", "admin");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(tokenUrl, request, Map.class);
        
        return (String) response.get("access_token");
    }

    private boolean realmExists(String keycloakUrl, String adminToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            String url = keycloakUrl + "/admin/realms/" + REALM_NAME;
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void createRealm(String keycloakUrl, String adminToken) {
        String url = keycloakUrl + "/admin/realms";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        
        Map<String, Object> realmConfig = new HashMap<>();
        realmConfig.put("realm", REALM_NAME);
        realmConfig.put("enabled", true);
        realmConfig.put("registrationAllowed", false);
        // Disable verify profile required action for all users
        realmConfig.put("requiredActions", List.of());
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(realmConfig, headers);
        restTemplate.postForEntity(url, request, String.class);
    }

    private void createClient(String keycloakUrl, String adminToken) {
        String url = keycloakUrl + "/admin/realms/" + REALM_NAME + "/clients";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        
        Map<String, Object> clientConfig = new HashMap<>();
        clientConfig.put("clientId", CLIENT_ID);
        clientConfig.put("secret", CLIENT_SECRET);
        clientConfig.put("enabled", true);
        clientConfig.put("directAccessGrantsEnabled", true);
        clientConfig.put("serviceAccountsEnabled", true);
        clientConfig.put("publicClient", false);
        clientConfig.put("protocol", "openid-connect");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(clientConfig, headers);
        restTemplate.postForEntity(url, request, String.class);
    }

    @SuppressWarnings("unchecked")
    private void createTestUserIfNotExists(String keycloakUrl, String adminToken) {
        String searchUrl = keycloakUrl + "/admin/realms/" + REALM_NAME + "/users?username=" + TEST_USERNAME + "&exact=true";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<?> searchRequest = new HttpEntity<>(headers);
        
        List<?> users = restTemplate.exchange(searchUrl, HttpMethod.GET, searchRequest, List.class).getBody();
        
        if (users == null || users.isEmpty()) {
            // Create user without required actions
            String createUrl = keycloakUrl + "/admin/realms/" + REALM_NAME + "/users";
            
            HttpHeaders createHeaders = new HttpHeaders();
            createHeaders.setContentType(MediaType.APPLICATION_JSON);
            createHeaders.setBearerAuth(adminToken);
            
            Map<String, Object> userConfig = new HashMap<>();
            userConfig.put("username", TEST_USERNAME);
            userConfig.put("email", TEST_EMAIL);
            userConfig.put("enabled", true);
            userConfig.put("emailVerified", true);
            userConfig.put("requiredActions", List.of());  // Explicitly empty
            userConfig.put("credentials", List.of(Map.of(
                    "type", "password",
                    "value", TEST_PASSWORD,
                    "temporary", false
            )));
            
            HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(userConfig, createHeaders);
            restTemplate.postForEntity(createUrl, createRequest, String.class);
            
            // Get user ID and clear any required actions
            users = restTemplate.exchange(searchUrl, HttpMethod.GET, searchRequest, List.class).getBody();
            if (users != null && !users.isEmpty()) {
                Map<String, Object> user = (Map<String, Object>) users.get(0);
                String userId = (String) user.get("id");
                
                // Update user to remove any required actions
                String updateUrl = keycloakUrl + "/admin/realms/" + REALM_NAME + "/users/" + userId;
                Map<String, Object> updateConfig = new HashMap<>();
                updateConfig.put("requiredActions", List.of());
                
                HttpEntity<Map<String, Object>> updateRequest = new HttpEntity<>(updateConfig, createHeaders);
                restTemplate.exchange(updateUrl, HttpMethod.PUT, updateRequest, String.class);
            }
        }
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/auth";
    }

    @Test
    @DisplayName("Keycloak container should be running and accessible")
    void keycloakContainerShouldBeRunning() {
        org.assertj.core.api.Assertions.assertThat(keycloak.isRunning()).isTrue();
        org.assertj.core.api.Assertions.assertThat(keycloak.getAuthServerUrl())
                .isNotBlank()
                .contains("http");
    }

    @Test
    @DisplayName("Login endpoint should be accessible without authentication")
    void loginEndpointShouldBeAccessible() {
        LoginRequest loginRequest = new LoginRequest("anyuser", "anypassword");

        // Login endpoint should be public and return 400 for invalid credentials
        // (not 401/403 which would indicate security misconfiguration)
        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/login")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Should return 400 for invalid credentials")
    void shouldFailLoginWithInvalidCredentials() {
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, "wrongpassword");

        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/login")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Should return 400 for non-existent user")
    void shouldFailLoginForNonExistentUser() {
        LoginRequest loginRequest = new LoginRequest("nonexistent", "somepassword");

        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/login")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Should be able to get token directly from Keycloak")
    void shouldBeAbleToGetTokenDirectlyFromKeycloak() {
        // This test verifies Keycloak is set up correctly by getting admin token
        String keycloakUrl = keycloak.getAuthServerUrl();
        String adminToken = getAdminToken(keycloakUrl);
        
        org.assertj.core.api.Assertions.assertThat(adminToken)
                .isNotBlank()
                .contains(".");  // JWT format
    }

    @Test
    @DisplayName("Should lock account after multiple failed attempts")
    void shouldLockAccountAfterMultipleFailedAttempts() throws Exception {
        String lockedUsername = "locktest_" + System.currentTimeMillis();
        String keycloakUrl = keycloak.getAuthServerUrl();
        String adminToken = getAdminToken(keycloakUrl);
        
        // Create a temporary user for this test
        String createUrl = keycloakUrl + "/admin/realms/" + REALM_NAME + "/users";
        
        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.setContentType(MediaType.APPLICATION_JSON);
        createHeaders.setBearerAuth(adminToken);
        
        Map<String, Object> userConfig = new HashMap<>();
        userConfig.put("username", lockedUsername);
        userConfig.put("email", lockedUsername + "@payu.id");
        userConfig.put("enabled", true);
        userConfig.put("emailVerified", true);
        userConfig.put("requiredActions", List.of());
        userConfig.put("credentials", List.of(Map.of(
                "type", "password",
                "value", TEST_PASSWORD,
                "temporary", false
        )));
        
        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(userConfig, createHeaders);
        restTemplate.postForEntity(createUrl, createRequest, String.class);

        // Attempt 5 failed logins (max attempts configured in application)
        for (int i = 0; i < 5; i++) {
            given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(lockedUsername, "wrongpassword"))
            .when()
                .post("/login")
            .then()
                .statusCode(400);
        }

        // 6th attempt should still fail (account locked even with correct password)
        given()
            .contentType(ContentType.JSON)
            .body(new LoginRequest(lockedUsername, TEST_PASSWORD))
        .when()
            .post("/login")
        .then()
            .statusCode(400);
    }
}
