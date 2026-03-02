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
import org.springframework.data.jpa.repository.JpaRepository;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
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

    public SecurityAutoConfiguration(SecurityProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security", name = "encryption-enabled", havingValue = "true", matchIfMissing = false)
    public EncryptionService encryptionService() {
        log.info("Initializing Encryption Service");

        if (properties.getEncryption().getPassword() == null ||
                properties.getEncryption().getPassword().isEmpty()) {
            // Generate a default encryption key (not recommended for production)
            log.warn("Using default encryption key. Please set payu.security.encryption.password for production!");
            return new EncryptionService(generateDefaultKey());
        }

        String salt = properties.getEncryption().getSalt();
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
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        log.info("Initializing Audit Log Publisher");
        return new AuditLogPublisher(properties, kafkaTemplate, objectMapper);
    }

    private String generateDefaultKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[32];
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
