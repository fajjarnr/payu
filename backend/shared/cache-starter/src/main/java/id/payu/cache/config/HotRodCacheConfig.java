package id.payu.cache.config;

import id.payu.cache.properties.CacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.commons.marshall.UTF8StringMarshaller;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Configuration for Data Grid Hot Rod Native Client (ARCH-007).
 * Activated when {@code payu.cache.provider=hotrod}.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnClass(RemoteCacheManager.class)
@ConditionalOnProperty(prefix = "payu.cache", name = "provider", havingValue = "hotrod", matchIfMissing = true)
public class HotRodCacheConfig {

    private static final int NEAR_CACHE_MAX_ENTRIES = 10_000;

    private final CacheProperties properties;

    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    public RemoteCacheManager remoteCacheManager() {
        log.info("Initializing Data Grid Hot Rod RemoteCacheManager with servers: {}",
                properties.getHotrod().getServerList());

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServers(properties.getHotrod().getServerList());
        builder.marshaller(UTF8StringMarshaller.class);
        builder.clientIntelligence(ClientIntelligence.valueOf(properties.getHotrod().getClientIntelligence()));
        builder.remoteCache(properties.getHotrod().getCacheName())
                .nearCacheMode(NearCacheMode.INVALIDATED)
                .nearCacheMaxEntries(NEAR_CACHE_MAX_ENTRIES);

        if (properties.getHotrod().getAuthUsername() != null && !properties.getHotrod().getAuthUsername().isEmpty()) {
            builder.security()
                    .authentication()
                    .username(properties.getHotrod().getAuthUsername())
                    .password(properties.getHotrod().getAuthPassword())
                    .realm(properties.getHotrod().getAuthRealm())
                    .saslMechanism(properties.getHotrod().getSaslMechanism());
        }

        if (properties.getHotrod().isUseSsl()) {
            CacheProperties.HotRod hotrod = properties.getHotrod();
            var ssl = builder.security().ssl().enable()
                    .hostnameValidation(hotrod.isHostnameValidation());

            if (hasText(hotrod.getTrustStoreFileName())) {
                ssl.trustStoreFileName(hotrod.getTrustStoreFileName())
                        .trustStorePassword(hotrod.getTrustStorePassword().toCharArray())
                        .trustStoreType(hotrod.getTrustStoreType());
            }
            if (hasText(hotrod.getKeyStoreFileName())) {
                ssl.keyStoreFileName(hotrod.getKeyStoreFileName())
                        .keyStorePassword(hotrod.getKeyStorePassword().toCharArray())
                        .keyStoreType(hotrod.getKeyStoreType());
            }
            if (hasText(hotrod.getKeyAlias())) {
                ssl.keyAlias(hotrod.getKeyAlias());
            }
            if (hasText(hotrod.getSniHostName())) {
                ssl.sniHostName(hotrod.getSniHostName());
            }
        }

        return new RemoteCacheManager(builder.build(), false);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
