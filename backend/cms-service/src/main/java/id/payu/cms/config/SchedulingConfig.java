package id.payu.cms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable scheduled tasks
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Scheduled tasks are enabled for content activation/archival
}
