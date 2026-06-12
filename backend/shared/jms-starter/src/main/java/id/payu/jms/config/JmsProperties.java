package id.payu.jms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for ActiveMQ Artemis JMS integration.
 */
@Data
@ConfigurationProperties(prefix = "payu.jms")
public class JmsProperties {
    /**
     * Whether to enable JMS auto-configuration.
     */
    private boolean enabled = true;

    /**
     * Artemis broker connection URL.
     */
    private String brokerUrl = "tcp://localhost:61616";

    /**
     * Broker username.
     */
    private String username = "admin";

    /**
     * Broker password.
     */
    private String password = "admin";

    /**
     * Configure default destination type (true for topics, false for queues).
     */
    private boolean pubSubDomain = false;
}
