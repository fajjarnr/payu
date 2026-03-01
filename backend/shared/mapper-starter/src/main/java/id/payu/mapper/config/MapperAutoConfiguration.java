package id.payu.mapper.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Auto-configuration for mapper-starter.
 *
 * <p>Ensures MapStruct mappers are properly configured with the shared
 * {@link MappingConfig}.</p>
 *
 * @since IMP-069
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "payu.mapper", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MapperAutoConfiguration {

    public MapperAutoConfiguration() {
        log.info("MapStruct mapper starter auto-configured");
    }
}
