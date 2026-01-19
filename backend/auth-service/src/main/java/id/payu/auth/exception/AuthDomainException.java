package id.payu.auth.exception;

/**
 * Base exception for Auth Service domain errors.
 * 
 * Error Code Structure: AUTH_[CATEGORY]_[SPECIFIC]
 * 
 * Categories:
 * - VAL: Validation errors (password policy, invalid input)
 * - BUS: Business rule violations (account locked, invalid credentials)
 * - EXT: External service errors (Keycloak unavailable)
 * - SYS: System/technical errors
 */
public abstract class AuthDomainException extends RuntimeException {

    private final String errorCode;
    private final String userMessage;

    protected AuthDomainException(String errorCode, String message, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    protected AuthDomainException(String errorCode, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }

    // === Validation Errors (4000-4099) ===
    
    public static class InvalidPasswordException extends AuthDomainException {
        public InvalidPasswordException(String reason) {
            super("AUTH_VAL_001", 
                  "Invalid password: " + reason,
                  "Password tidak memenuhi kriteria: " + reason);
        }
    }

    public static class InvalidUsernameException extends AuthDomainException {
        public InvalidUsernameException(String username) {
            super("AUTH_VAL_002", 
                  "Invalid username format: " + username,
                  "Format username tidak valid");
        }
    }

    public static class InvalidEmailException extends AuthDomainException {
        public InvalidEmailException(String email) {
            super("AUTH_VAL_003", 
                  "Invalid email format: " + email,
                  "Format email tidak valid");
        }
    }

    public static class MissingCredentialsException extends AuthDomainException {
        public MissingCredentialsException() {
            super("AUTH_VAL_004", 
                  "Username or password is missing",
                  "Username dan password harus diisi");
        }
    }

    // === Business Rule Errors (4100-4199) ===
    
    public static class InvalidCredentialsException extends AuthDomainException {
        public InvalidCredentialsException() {
            super("AUTH_BUS_001", 
                  "Invalid username or password",
                  "Username atau password salah");
        }
    }

    public static class AccountLockedException extends AuthDomainException {
        public AccountLockedException(int lockoutMinutes) {
            super("AUTH_BUS_002", 
                  "Account is temporarily locked due to too many failed attempts",
                  "Akun dikunci sementara, coba lagi dalam " + lockoutMinutes + " menit");
        }
    }

    public static class AccountDisabledException extends AuthDomainException {
        public AccountDisabledException(String username) {
            super("AUTH_BUS_003", 
                  "Account is disabled: " + username,
                  "Akun tidak aktif, silakan hubungi customer service");
        }
    }

    public static class TokenExpiredException extends AuthDomainException {
        public TokenExpiredException() {
            super("AUTH_BUS_004", 
                  "Token has expired",
                  "Sesi telah berakhir, silakan login kembali");
        }
    }

    public static class InvalidTokenException extends AuthDomainException {
        public InvalidTokenException() {
            super("AUTH_BUS_005", 
                  "Invalid or malformed token",
                  "Token tidak valid");
        }
    }

    public static class RefreshTokenExpiredException extends AuthDomainException {
        public RefreshTokenExpiredException() {
            super("AUTH_BUS_006", 
                  "Refresh token has expired",
                  "Sesi telah berakhir, silakan login kembali");
        }
    }

    public static class RateLimitExceededException extends AuthDomainException {
        public RateLimitExceededException() {
            super("AUTH_BUS_007", 
                  "Too many login attempts, rate limit exceeded",
                  "Terlalu banyak percobaan login, coba lagi nanti");
        }
    }

    public static class UserAlreadyExistsException extends AuthDomainException {
        public UserAlreadyExistsException(String identifier) {
            super("AUTH_BUS_008", 
                  "User already exists: " + identifier,
                  "User dengan email/username tersebut sudah terdaftar");
        }
    }

    // === External Service Errors (4200-4299) ===
    
    public static class KeycloakUnavailableException extends AuthDomainException {
        public KeycloakUnavailableException() {
            super("AUTH_EXT_001", 
                  "Identity provider (Keycloak) is unavailable",
                  "Layanan autentikasi sedang tidak tersedia, silakan coba lagi");
        }

        public KeycloakUnavailableException(Throwable cause) {
            super("AUTH_EXT_001", 
                  "Identity provider (Keycloak) is unavailable",
                  "Layanan autentikasi sedang tidak tersedia, silakan coba lagi",
                  cause);
        }
    }

    public static class KeycloakAuthenticationException extends AuthDomainException {
        public KeycloakAuthenticationException(String reason) {
            super("AUTH_EXT_002", 
                  "Keycloak authentication failed: " + reason,
                  "Autentikasi gagal");
        }

        public KeycloakAuthenticationException(String reason, Throwable cause) {
            super("AUTH_EXT_002", 
                  "Keycloak authentication failed: " + reason,
                  "Autentikasi gagal",
                  cause);
        }
    }

    public static class KeycloakUserCreationException extends AuthDomainException {
        public KeycloakUserCreationException(String reason) {
            super("AUTH_EXT_003", 
                  "Failed to create user in Keycloak: " + reason,
                  "Gagal mendaftarkan user");
        }

        public KeycloakUserCreationException(String reason, Throwable cause) {
            super("AUTH_EXT_003", 
                  "Failed to create user in Keycloak: " + reason,
                  "Gagal mendaftarkan user",
                  cause);
        }
    }

    // === System Errors (4900-4999) ===
    
    public static class AuthenticationSystemException extends AuthDomainException {
        public AuthenticationSystemException(String reason) {
            super("AUTH_SYS_001", 
                  "Authentication system error: " + reason,
                  "Terjadi kesalahan sistem, silakan coba lagi");
        }

        public AuthenticationSystemException(String reason, Throwable cause) {
            super("AUTH_SYS_001", 
                  "Authentication system error: " + reason,
                  "Terjadi kesalahan sistem, silakan coba lagi",
                  cause);
        }
    }

    public static class TokenGenerationException extends AuthDomainException {
        public TokenGenerationException(String reason) {
            super("AUTH_SYS_002", 
                  "Failed to generate token: " + reason,
                  "Terjadi kesalahan sistem, silakan coba lagi");
        }

        public TokenGenerationException(String reason, Throwable cause) {
            super("AUTH_SYS_002", 
                  "Failed to generate token: " + reason,
                  "Terjadi kesalahan sistem, silakan coba lagi",
                  cause);
        }
    }
}
