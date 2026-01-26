package id.payu.security.config;

// Jasypt integration disabled until compatible version is available
// import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import id.payu.security.crypto.EncryptionService;
import id.payu.security.masking.DataMaskingAspect;
import id.payu.security.masking.LogbackMaskingFilter;
import id.payu.security.audit.AuditAspect;
import id.payu.security.audit.AuditLogPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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

/**
 * Auto-configuration for Security features
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@EnableAspectJAutoProxy
@ConditionalOnProperty(prefix = "payu.security", name = "enabled", havingValue = "true", matchIfMissing = false)
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

        return new EncryptionService(properties.getEncryption().getPassword());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security", name = "masking-enabled", havingValue = "true", matchIfMissing = false)
    public DataMaskingAspect dataMaskingAspect() {
        log.info("Initializing Data Masking Aspect");
        return new DataMaskingAspect(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security", name = "audit-enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    public AuditAspect auditAspect(
            AuditLogPublisher auditLogPublisher) {
        log.info("Initializing Audit Aspect");
        return new AuditAspect(properties, auditLogPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security", name = "audit-enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
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
