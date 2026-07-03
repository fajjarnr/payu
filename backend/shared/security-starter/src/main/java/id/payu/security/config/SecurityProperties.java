package id.payu.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for Security features
 */
@Data
@ConfigurationProperties(prefix = "payu.security")
public class SecurityProperties {

    /**
     * Enable field-level encryption (requires encryption.password config)
     */
    private boolean encryptionEnabled = false;

    /**
     * Enable data masking in logs
     */
    private boolean maskingEnabled = true;

    /**
     * Enable audit logging
     */
    private boolean auditEnabled = true;

    /**
     * Encryption configuration
     */
    private Encryption encryption = new Encryption();

    /**
     * Masking configuration
     */
    private Masking masking = new Masking();

    /**
     * Audit configuration
     */
    private Audit audit = new Audit();

    /**
     * CORS configuration
     */
    private Cors cors = new Cors();

    @Data
    public static class Cors {
        /**
         * Enable CORS configuration
         */
        private boolean enabled = false;

        /**
         * Allowed origin patterns (comma-separated or YAML list)
         */
        private List<String> allowedOrigins;

        /**
         * Allowed HTTP methods
         */
        private List<String> allowedMethods;

        /**
         * Allowed request headers
         */
        private List<String> allowedHeaders;

        /**
         * Exposed response headers
         */
        private List<String> exposedHeaders;

        /**
         * Allow credentials (cookies, authorization headers)
         */
        private boolean allowCredentials = false;

        /**
         * Preflight cache duration in seconds
         */
        private long maxAge = 3600L;
    }

    @Data
    public static class Encryption {
        /**
         * Encryption algorithm
         */
        private String algorithm = "PBEWITHHMACSHA512ANDAES_256";

        /**
         * Encryption password - MUST be externalized via Vault or environment variable.
         *
         * WARNING: Never store this in source control or use default values.
         * Use Spring Cloud Vault or environment variables for production.
         *
         * Configuration examples:
         * - Environment variable: ENCRYPTION_KEY
         * - Vault path: secret/payu/[service-name]/encryption-key
         * - Kubernetes secret: payu-encryption-key
         *
         * Key requirements:
         * - Minimum 32 characters for AES-256
         * - Use cryptographically secure random generation
         * - Rotate keys quarterly (document procedures)
         */
        private String password;

        /**
         * PBKDF2 salt for key derivation - MUST be externalized via Vault for production (BUG-BE-019).
         * If unset, a default salt is used. Override via: payu.security.encryption.salt
         */
        private String salt;

        /**
         * Fields to encrypt (regex patterns)
         */
        private List<String> fields = List.of(
                ".*password.*",
                ".*ssn.*",
                ".*creditCard.*",
                ".*accountNumber.*",
                ".*idCard.*",
                ".*nik.*",
                ".*secret.*"
        );
    }

    @Data
    public static class Masking {
        /**
         * Masking pattern for sensitive data
         */
        private String pattern = "(?<=.{4}).";

        /**
         * Mask character
         */
        private char maskChar = '*';

        /**
         * Fields to mask in logs
         */
        private List<String> fields = List.of(
                "password",
                "ssn",
                "creditCard",
                "accountNumber",
                "idCard",
                "nik",
                "email",
                "phoneNumber",
                "token",
                "secret"
        );
    }

    @Data
    public static class Audit {
        /**
         * Enable audit for sensitive operations
         */
        private boolean enabled = true;

        /**
         * Audit log retention days
         */
        private int retentionDays = 365;

        /**
         * Operations to audit
         */
        private List<String> operations = List.of(
                "CREATE",
                "UPDATE",
                "DELETE",
                "TRANSFER",
                "LOGIN",
                "LOGOUT",
                "KYC_APPROVE",
                "KYC_REJECT"
        );
    }
}
