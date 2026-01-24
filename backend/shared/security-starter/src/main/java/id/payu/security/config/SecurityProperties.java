package id.payu.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for Security features
 */
@Data
@Component
@ConfigurationProperties(prefix = "payu.security")
public class SecurityProperties {

    /**
     * Enable field-level encryption
     */
    private boolean encryptionEnabled = true;

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

    @Data
    public static class Encryption {
        /**
         * Encryption algorithm
         */
        private String algorithm = "PBEWITHHMACSHA512ANDAES_256";

        /**
         * Encryption password (should be externalized)
         */
        private String password;

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
