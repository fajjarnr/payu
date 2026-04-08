package id.payu.auth.adapter.security;

import id.payu.auth.application.service.RiskEvaluationService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.auth.config.KeycloakConfig;
import id.payu.auth.domain.model.LoginContext;
import id.payu.auth.dto.LoginResponse;
import id.payu.cache.service.CacheService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.ws.rs.core.Response;
import java.time.Duration;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final Keycloak keycloakAdmin;
    private final KeycloakConfig keycloakConfig;
    private final WebClient.Builder webClientBuilder;
    private final RiskEvaluationService riskEvaluationService;
    private final ObjectMapper objectMapper;
    private final CacheService cacheService;
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    private static final String FAILED_ATTEMPTS_KEY_PREFIX = "auth:failedAttempts:";
    // BUG-SECURITY-008 FIX: TTL now derived from lockoutDurationMinutes (configurable) instead of hardcoded 15min

    @Value("${payu.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${payu.security.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    @Value("${payu.security.password-policy.min-length:8}")
    private int passwordMinLength;

    @Value("${payu.security.password-policy.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${payu.security.password-policy.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${payu.security.password-policy.require-digit:true}")
    private boolean requireDigit;

    @Value("${payu.security.password-policy.require-special-char:true}")
    private boolean requireSpecialChar;

    @RateLimiter(name = "loginRateLimiter", fallbackMethod = "rateLimitFallback")
    public Mono<LoginResponse> login(String username, String password) {
        if (isAccountLocked(username)) {
            log.warn("Login attempt for locked account: {}", maskUsername(username));
            return Mono.error(new IllegalArgumentException("Account temporarily locked due to too many failed attempts"));
        }

        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakConfig.getServerUrl(), keycloakConfig.getRealm());

        WebClient webClient = webClientBuilder
                .baseUrl(tokenEndpoint)
                .build();

        return webClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(buildLoginForm(username, password)))
                .retrieve()
                .bodyToMono(String.class)
                .map(jsonResponse -> {
                    try {
                        log.info("Keycloak token obtained successfully for user: {}", maskUsername(username));
                        JsonNode root = objectMapper.readTree(jsonResponse);
                        String accessToken = root.get("access_token").asText();
                        String refreshToken = root.has("refresh_token") ? root.get("refresh_token").asText() : null;
                        long expiresIn = root.get("expires_in").asLong();
                        String tokenType = root.get("token_type").asText();
                        return new LoginResponse(accessToken, refreshToken, expiresIn, tokenType);
                    } catch (Exception e) {
                        log.error("Failed to deserialize Keycloak response for user {}: {}",
                                username, e.getMessage(), e);
                        throw new IllegalArgumentException("Failed to parse login response: " + e.getMessage(), e);
                    }
                })
                .doOnSuccess(response -> {
                    clearFailedAttempts(username);
                    log.info("Successful login for user: {}", maskUsername(username));
                })
                .doOnError(error -> {
                    recordFailedAttemptInternal(username);
                    log.error("Login failed for user {}: {}", maskUsername(username), error.getMessage(), error);
                })
                .onErrorMap(error -> new IllegalArgumentException("Invalid credentials or login failed: " + error.getClass().getSimpleName() + " - " + error.getMessage()));
    }

    public Mono<Boolean> validateCredentials(String username, String password) {
        if (isAccountLocked(username)) {
            return Mono.just(false);
        }

        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakConfig.getServerUrl(), keycloakConfig.getRealm());

        WebClient webClient = webClientBuilder
                .baseUrl(tokenEndpoint)
                .build();

        return webClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(buildLoginForm(username, password)))
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> true)
                .onErrorResume(error -> {
                    recordFailedAttemptInternal(username);
                    return Mono.just(false);
                });
    }

    public Mono<LoginResponse> refreshToken(String refreshToken) {
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakConfig.getServerUrl(), keycloakConfig.getRealm());

        WebClient webClient = webClientBuilder
                .baseUrl(tokenEndpoint)
                .build();

        return webClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(buildRefreshForm(refreshToken)))
                .retrieve()
                .bodyToMono(String.class)
                .map(jsonResponse -> {
                    try {
                        JsonNode root = objectMapper.readTree(jsonResponse);
                        String accessToken = root.get("access_token").asText();
                        String newRefreshToken = root.has("refresh_token") ? root.get("refresh_token").asText() : null;
                        long expiresIn = root.get("expires_in").asLong();
                        String tokenType = root.get("token_type").asText();
                        return new LoginResponse(accessToken, newRefreshToken, expiresIn, tokenType);
                    } catch (Exception e) {
                        log.error("Failed to deserialize refresh token response: {}",
                                e.getMessage(), e);
                        throw new IllegalArgumentException("Failed to parse refresh response: " + e.getMessage(), e);
                    }
                })
                .doOnSuccess(response -> log.info("Token refreshed successfully"))
                .doOnError(error -> log.error("Token refresh failed: {}", error.getMessage()))
                .onErrorMap(error -> new IllegalArgumentException("Failed to refresh token"));
    }

    public Mono<LoginResponse> rateLimitFallback(String username, String password, Throwable t) {
        log.warn("Rate limit exceeded for login attempts");
        return Mono.error(new IllegalArgumentException("Too many login attempts. Please try again later."));
    }



    /**
     * Blocking version of validateCredentials for use in servlet (non-reactive) contexts.
     * This method blocks the thread until the validation completes.
     *
     * @param username the username
     * @param password the password
     * @return true if credentials are valid, false otherwise
     */
    public Boolean validateCredentialsBlocking(String username, String password) {
        if (isAccountLocked(username)) {
            return false;
        }

        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakConfig.getServerUrl(), keycloakConfig.getRealm());

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        org.springframework.http.HttpEntity<MultiValueMap<String, String>> request = 
                new org.springframework.http.HttpEntity<>(buildLoginForm(username, password), headers);

        try {
            org.springframework.http.ResponseEntity<String> response = restTemplate
                    .postForEntity(tokenEndpoint, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            recordFailedAttemptInternal(username);
            return false;
        } catch (Exception e) {
            log.error("Error validating credentials synchronously", e);
            recordFailedAttemptInternal(username);
            return false;
        }
    }

    /**
     * Blocking version of login for use in servlet (non-reactive) contexts.
     * This method blocks the thread until the login completes.
     *
     * @param username the username
     * @param password the password
     * @return LoginResponse containing access tokens
     * @throws IllegalArgumentException if login fails
     */
    @RateLimiter(name = "loginRateLimiter", fallbackMethod = "rateLimitFallbackBlocking")
    public LoginResponse loginBlocking(String username, String password) {
        if (isAccountLocked(username)) {
            log.warn("Login attempt for locked account: {}", maskUsername(username));
            throw new IllegalArgumentException("Account temporarily locked due to too many failed attempts");
        }

        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakConfig.getServerUrl(), keycloakConfig.getRealm());

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        org.springframework.http.HttpEntity<MultiValueMap<String, String>> request = 
                new org.springframework.http.HttpEntity<>(buildLoginForm(username, password), headers);

        try {
            org.springframework.http.ResponseEntity<String> response = restTemplate
                    .postForEntity(tokenEndpoint, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String accessToken = root.get("access_token").asText();
            String refreshToken = root.has("refresh_token") ? root.get("refresh_token").asText() : null;
            long expiresIn = root.get("expires_in").asLong();
            String tokenType = root.get("token_type").asText();

            clearFailedAttempts(username);
            log.info("Successful login for user: {}", maskUsername(username));
            return new LoginResponse(accessToken, refreshToken, expiresIn, tokenType);
        } catch (Exception e) {
            recordFailedAttemptInternal(username);
            log.error("Login failed for user {}: {}", maskUsername(username), e.getMessage());
            throw new IllegalArgumentException("Invalid credentials or login failed: " + e.getMessage());
        }
    }

    public LoginResponse rateLimitFallbackBlocking(String username, String password, Throwable t) {
        log.warn("Rate limit exceeded for login attempts (blocking)");
        throw new IllegalArgumentException("Too many login attempts. Please try again later.");
    }



    /**
     * Blocking version of refreshToken for use in servlet (non-reactive) contexts.
     * This method blocks the thread until the token refresh completes.
     *
     * @param refreshToken the refresh token
     * @return LoginResponse containing new access tokens
     * @throws IllegalArgumentException if refresh fails
     */
    public LoginResponse refreshTokenBlocking(String refreshToken) {
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakConfig.getServerUrl(), keycloakConfig.getRealm());

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        org.springframework.http.HttpEntity<MultiValueMap<String, String>> request = 
                new org.springframework.http.HttpEntity<>(buildRefreshForm(refreshToken), headers);

        try {
            org.springframework.http.ResponseEntity<String> response = restTemplate
                    .postForEntity(tokenEndpoint, request, String.class);

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

    private Mono<LoginResponse> loginInternal(String username, String password) {
        if (isAccountLocked(username)) {
            log.warn("Login attempt for locked account: {}", username);
            return Mono.error(new IllegalArgumentException("Account temporarily locked due to too many failed attempts"));
        }

        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakConfig.getServerUrl(), keycloakConfig.getRealm());

        WebClient webClient = webClientBuilder
                .baseUrl(tokenEndpoint)
                .build();

        return webClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(buildLoginForm(username, password)))
                .retrieve()
                .bodyToMono(String.class)
                .map(jsonResponse -> {
                    try {
                        JsonNode root = objectMapper.readTree(jsonResponse);
                        String accessToken = root.get("access_token").asText();
                        String refreshToken = root.has("refresh_token") ? root.get("refresh_token").asText() : null;
                        long expiresIn = root.get("expires_in").asLong();
                        String tokenType = root.get("token_type").asText();
                        return new LoginResponse(accessToken, refreshToken, expiresIn, tokenType);
                    } catch (Exception e) {
                        log.error("Failed to deserialize Keycloak response for user {}: {}",
                                username, e.getMessage(), e);
                        throw new IllegalArgumentException("Failed to parse login response: " + e.getMessage(), e);
                    }
                })
                .doOnSuccess(response -> {
                    clearFailedAttempts(username);
                    log.info("Successful login for user: {}", maskUsername(username));
                })
                .doOnError(error -> {
                    recordFailedAttemptInternal(username);
                    log.error("Login failed for user {}: {}", maskUsername(username), error.getMessage());
                })
                .onErrorMap(error -> new IllegalArgumentException("Invalid credentials or login failed"));
    }

    // BUG-SECURITY-009 FIX: Use synchronized block on interned key to prevent race condition
    private void recordFailedAttemptInternal(String username) {
        String key = FAILED_ATTEMPTS_KEY_PREFIX + username;
        Duration lockoutTtl = Duration.ofMinutes(lockoutDurationMinutes);
        synchronized (key.intern()) {
            FailedAttempt attempt = cacheService.get(key, FailedAttempt.class,
                    () -> new FailedAttempt(0, 0L));

            if (attempt == null) {
                attempt = new FailedAttempt(0, 0L);
            }

            attempt.increment();
            if (attempt.getCount() >= maxLoginAttempts) {
                attempt.setLockUntil(System.currentTimeMillis() + lockoutTtl.toMillis());
                log.warn("Account locked: {} until {}", maskUsername(username), attempt.getLockUntil());
            }
            // BUG-SECURITY-008 FIX: use lockoutTtl derived from config instead of hardcoded 15min
            cacheService.put(key, attempt, lockoutTtl);
        }
        riskEvaluationService.recordFailedAttempt(username);
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

    private boolean isAccountLocked(String username) {
        String key = FAILED_ATTEMPTS_KEY_PREFIX + username;
        FailedAttempt attempt = cacheService.get(key, FailedAttempt.class);
        if (attempt == null) {
            return false;
        }
        return attempt.getCount() >= maxLoginAttempts &&
                System.currentTimeMillis() < attempt.getLockUntil();
    }

    private void clearFailedAttempts(String username) {
        String key = FAILED_ATTEMPTS_KEY_PREFIX + username;
        cacheService.invalidate(key);
        riskEvaluationService.clearFailedAttempts(username);
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

    private MultiValueMap<String, String> buildLoginForm(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakConfig.getClientId());
        form.add("client_secret", keycloakConfig.getClientSecret());
        form.add("username", username);
        form.add("password", password);
        return form;
    }

    private MultiValueMap<String, String> buildRefreshForm(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakConfig.getClientId());
        form.add("client_secret", keycloakConfig.getClientSecret());
        form.add("refresh_token", refreshToken);
        return form;
    }

    /**
     * Serializable class for storing failed login attempts in Redis.
     * Must be public and have a default constructor for JSON serialization.
     */
    public static class FailedAttempt {
        private int count;
        private long lockUntil;

        // Default constructor for JSON deserialization
        public FailedAttempt() {
            this.count = 0;
            this.lockUntil = 0L;
        }

        public FailedAttempt(int count, long lockUntil) {
            this.count = count;
            this.lockUntil = lockUntil;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public void increment() {
            this.count++;
        }

        public long getLockUntil() {
            return lockUntil;
        }

        public void setLockUntil(long lockUntil) {
            this.lockUntil = lockUntil;
        }
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
