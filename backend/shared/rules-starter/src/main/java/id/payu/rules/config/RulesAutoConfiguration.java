package id.payu.rules.config;

import id.payu.rules.service.RulesEngineService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Spring Auto-Configuration for Rules Engine starter.
 */
@AutoConfiguration
public class RulesAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RulesEngineService rulesEngineService() {
        return new RulesEngineService();
    }
}
