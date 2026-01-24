package id.payu.security.config;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import id.payu.security.crypto.EncryptionService;
import id.payu.security.masking.DataMaskingAspect;
import id.payu.security.masking.LogbackMaskingFilter;
import id.payu.security.audit.AuditAspect;
import id.payu.security.audit.AuditLogPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(SecurityProperties.class)
@EnableAspectJAutoProxy
@ConditionalOnProperty(prefix = "payu.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {

    private final SecurityProperties properties;

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security.encryption-enabled", havingValue = "true", matchIfMissing = true)
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
    @ConditionalOnProperty(prefix = "payu.security.masking-enabled", havingValue = "true", matchIfMissing = true)
    public DataMaskingAspect dataMaskingAspect() {
        log.info("Initializing Data Masking Aspect");
        return new DataMaskingAspect(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security.audit-enabled", havingValue = "true", matchIfMissing = true)
    public AuditAspect auditAspect(AuditLogPublisher auditLogPublisher) {
        log.info("Initializing Audit Aspect");
        return new AuditAspect(properties, auditLogPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payu.security.audit-enabled", havingValue = "true", matchIfMissing = true)
    public AuditLogPublisher auditLogPublisher() {
        log.info("Initializing Audit Log Publisher");
        return new AuditLogPublisher(properties);
    }

    private String generateDefaultKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[32];
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
