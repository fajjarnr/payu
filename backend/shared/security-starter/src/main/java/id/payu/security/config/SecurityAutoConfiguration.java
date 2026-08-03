package id.payu.security.config;

// Jasypt integration disabled until compatible version is available
// import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import id.payu.security.crypto.EncryptionService;
import id.payu.security.masking.DataMaskingAspect;
import id.payu.security.masking.LogbackMaskingFilter;
import id.payu.security.audit.AuditAspect;
import id.payu.security.audit.AuditLogPublisher;
import id.payu.security.converter.EncryptedStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Collections;

/**
 * Auto-configuration for Security features
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@EnableAspectJAutoProxy
@ConditionalOnProperty(prefix = "payu.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    private final SecurityProperties properties;

    public SecurityAutoConfiguration(SecurityProperties properties, Environment environment) {
        this.properties = properties;
        validateProductionDefaults(environment);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security", name = "encryption-enabled", havingValue = "true", matchIfMissing = false)
    public EncryptionService encryptionService(
            org.springframework.core.env.Environment environment) {
        log.info("Initializing Encryption Service");

        String password = properties.getEncryption().getPassword();
        boolean isProdProfile = isProductionProfile(environment);

        String salt = properties.getEncryption().getSalt();
        if (password == null || password.isBlank()) {
            // GAP-30 fix: fail fast in production profiles instead of falling back
            // to a default key. The default key breaks multi-pod scaling (each pod
            // derives a different key after restart) and corrupts data after pod
            // rotation. ENCRYPTION_KEY env var MUST be injected via Vault.
            if (isProdProfile) {
                throw new IllegalStateException(
                    "GAP-30 enforcement: payu.security.encryption.password (ENCRYPTION_KEY env var) "
                    + "must be configured in production profiles [container, prod, staging]. "
                    + "Refusing to start with a default key — this would corrupt encrypted data "
                    + "after pod restart and break multi-pod encryption consistency.");
            }
            log.warn("Using default encryption key. Please set payu.security.encryption.password for production!");
            return new EncryptionService(generateDefaultKey());
        }

        return new EncryptionService(
                properties.getEncryption().getPassword(),
                Collections.emptyList(),
                salt
        );
    }

    /**
     * Configure the EncryptedStringConverter with the EncryptionService.
     * This enables field-level encryption for JPA entities.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security", name = "encryption-enabled", havingValue = "true", matchIfMissing = false)
    public EncryptedStringConverter encryptedStringConverter(EncryptionService encryptionService) {
        log.info("Initializing EncryptedStringConverter for field-level encryption");
        EncryptedStringConverter converter = new EncryptedStringConverter();
        converter.setEncryptionService(encryptionService);
        return converter;
    }

    /**
     * When encryption is NOT enabled (disabled or not configured), set EncryptedStringConverter
     * to pass-through mode so JPA entities with @Convert don't fail at runtime.
     */
    @Bean
    @ConditionalOnMissingBean(EncryptionService.class)
    public EncryptedStringConverter encryptedStringConverterPassThrough() {
        log.info("Encryption not enabled — EncryptedStringConverter will operate in pass-through mode");
        EncryptedStringConverter.setEncryptionDisabled(true);
        return new EncryptedStringConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security", name = "masking-enabled", havingValue = "true", matchIfMissing = true)
    public DataMaskingAspect dataMaskingAspect() {
        log.info("Initializing Data Masking Aspect");
        return new DataMaskingAspect(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security", name = "audit-enabled", havingValue = "true", matchIfMissing = true)
    public AuditAspect auditAspect(
            org.springframework.beans.factory.ObjectProvider<AuditLogPublisher> auditLogPublisherProvider) {
        AuditLogPublisher publisher = auditLogPublisherProvider.getIfAvailable();
        log.info("Initializing Audit Aspect (publisher {})", publisher != null ? "available" : "unavailable — fallback to SLF4J");
        return new AuditAspect(properties, publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security", name = "audit-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @ConditionalOnBean(name = "kafkaTemplate")
    public AuditLogPublisher auditLogPublisher(
            org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            org.springframework.beans.factory.ObjectProvider<id.payu.outbox.service.OutboxService> outboxServiceProvider) {
        log.info("Initializing Audit Log Publisher with optional OutboxService");
        return new AuditLogPublisher(
                properties,
                kafkaTemplate,
                objectMapper,
                outboxServiceProvider.getIfAvailable()
        );
    }

    private void validateProductionDefaults(Environment environment) {
        if (!isProductionProfile(environment)) {
            return;
        }
        if (!properties.isMaskingEnabled()) {
            throw new IllegalStateException(
                    "Production security requires payu.security.masking-enabled=true");
        }
        if (!properties.isAuditEnabled()) {
            throw new IllegalStateException(
                    "Production security requires payu.security.audit-enabled=true");
        }
        if (properties.isEncryptionEnabled()) {
            requireConfigured(properties.getEncryption().getPassword(),
                    "payu.security.encryption.password");
            requireConfigured(properties.getEncryption().getSalt(),
                    "payu.security.encryption.salt");
        }
    }

    private boolean isProductionProfile(Environment environment) {
        return java.util.Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> switch (profile) {
                    case "container", "prod", "staging", "sit", "uat", "preprod" -> true;
                    default -> false;
                });
    }

    private void requireConfigured(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Production security requires " + property + " to be configured");
        }
    }

    /**
     * Returns a deterministic default key for development/testing environments.
     * <p>
     * BUG-SHARED-002 FIX: Previously generated a random UUID key per invocation,
     * meaning each pod in a multi-pod deployment would derive a different AES key.
     * Data encrypted by pod-A was undecryptable by pod-B.
     * Now uses a fixed development-only key so all pods share the same key.
     * A loud WARNING is logged to ensure this is never used in production.
     */
    private String generateDefaultKey() {
        log.warn("╔══════════════════════════════════════════════════════════════════╗");
        log.warn("║  SECURITY WARNING: Using default encryption key!                ║");
        log.warn("║  Set payu.security.encryption.password in production!           ║");
        log.warn("║  Data encrypted with default key is NOT secure.                 ║");
        log.warn("╚══════════════════════════════════════════════════════════════════╝");
        return "CHANGE-ME-IN-PRODUCTION-payu-dev-key-2026";
    }
}
