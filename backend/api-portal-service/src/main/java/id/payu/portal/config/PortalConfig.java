package id.payu.portal.config;

import java.util.Map;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "portal")
public interface PortalConfig {

    Map<String, ServiceConfig> services();

    CacheConfig cache();

    interface ServiceConfig {
        String name();
        String url();
        String openapiPath();
    }

    interface CacheConfig {
        String ttl();
    }
}
