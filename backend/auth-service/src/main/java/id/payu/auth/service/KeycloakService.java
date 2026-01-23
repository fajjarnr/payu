package id.payu.auth.service;

import id.payu.auth.config.KeycloakConfig;
import id.payu.auth.dto.LoginResponse;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final Keycloak keycloakAdmin;
    private final KeycloakConfig keycloakConfig;
    private final WebClient.Builder webClientBuilder;

    private final Map<String, FailedAttempt> failedAttempts = new ConcurrentHashMap<>();

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
                .bodyToMono(LoginResponse.class)
                .doOnSuccess(response -> {
                    clearFailedAttempts(username);
                    log.info("Successful login for user: {}", username);
                })
                .doOnError(error -> {
                    recordFailedAttempt(username);
                    log.error("Login failed for user {}: {}", username, error.getMessage());
                })
                .onErrorMap(error -> new IllegalArgumentException("Invalid credentials or login failed"));
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
                .bodyToMono(LoginResponse.class)
                .doOnSuccess(response -> log.info("Token refreshed successfully"))
                .doOnError(error -> log.error("Token refresh failed: {}", error.getMessage()))
                .onErrorMap(error -> new IllegalArgumentException("Failed to refresh token"));
    }

    public Mono<LoginResponse> rateLimitFallback(String username, String password, Throwable t) {
        log.warn("Rate limit exceeded for login attempts");
        return Mono.error(new IllegalArgumentException("Too many login attempts. Please try again later."));
    }

    public void createUser(String username, String email, String password) {
        validatePassword(password);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true);

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

        log.info("Created user {} in Keycloak with ID {}", username, userId);
    }

    private boolean isAccountLocked(String username) {
        FailedAttempt attempt = failedAttempts.get(username);
        if (attempt == null) {
            return false;
        }
        return attempt.getCount() >= maxLoginAttempts &&
                System.currentTimeMillis() < attempt.getLockUntil();
    }

    private void recordFailedAttempt(String username) {
        FailedAttempt attempt = failedAttempts.computeIfAbsent(username,
                k -> new FailedAttempt(0, 0L));
        attempt.increment();
        if (attempt.getCount() >= maxLoginAttempts) {
            attempt.setLockUntil(System.currentTimeMillis() + Duration.ofMinutes(lockoutDurationMinutes).toMillis());
            log.warn("Account locked: {} until {}", username, attempt.getLockUntil());
        }
        failedAttempts.put(username, attempt);
    }

    private void clearFailedAttempts(String username) {
        failedAttempts.remove(username);
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

    private static class FailedAttempt {
        private int count;
        private long lockUntil;

        public FailedAttempt(int count, long lockUntil) {
            this.count = count;
            this.lockUntil = lockUntil;
        }

        public int getCount() {
            return count;
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
}
