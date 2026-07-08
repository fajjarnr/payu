package id.payu.jms.config;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import jakarta.jms.ConnectionFactory;


import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Auto-configuration class for JMS with ActiveMQ Artemis.
 */
@AutoConfiguration
@ConditionalOnClass({ConnectionFactory.class, ActiveMQConnectionFactory.class})
@EnableConfigurationProperties(JmsProperties.class)
@ConditionalOnProperty(prefix = "payu.jms", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JmsAutoConfiguration {

    /**
     * Profiles where the default {@code "admin"} JMS password is forbidden.
     * Audit reference: AUDIT-059 (TODOS 2026-07-01 deep audit). Container pods
     * starting with {@code "admin"} would expose Artemis to the cluster network.
     */
    static final Set<String> PROD_LIKE_PROFILES =
        Set.of("container", "prod", "staging");

    private static final String WEAK_DEFAULT = "admin";

    private final JmsProperties properties;

    public JmsAutoConfiguration(JmsProperties properties, Environment environment) {
        this.properties = properties;
        validatePasswordForProfile(environment);
    }

    private void validatePasswordForProfile(Environment environment) {
        if (environment == null) {
            return;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles == null || activeProfiles.length == 0) {
            return;
        }
        boolean isProdLike = Arrays.stream(activeProfiles)
            .anyMatch(PROD_LIKE_PROFILES::contains);
        if (!isProdLike) {
            return;
        }
        String password = properties.getPassword();
        String reason;
        if (password == null || password.isBlank()) {
            reason = "must be set";
        } else if (WEAK_DEFAULT.equals(password)) {
            reason = "weak default 'admin' is forbidden in production profiles";
        } else {
            return;
        }
        String profile = String.join(",", activeProfiles);
        throw new IllegalStateException(
            "JMS password (payu.jms.password / ARTEMIS_PASSWORD) " + reason
                + ". Active profile: " + profile
                + ". Set ARTEMIS_PASSWORD environment variable to a strong secret.");
    }

    @Bean
    @ConditionalOnMissingBean
    public ConnectionFactory jmsConnectionFactory() {
        try {
            ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(
                    properties.getBrokerUrl(),
                    properties.getUsername(),
                    properties.getPassword()
            );
            // Use caching connection factory for performance
            CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMQConnectionFactory);
            cachingConnectionFactory.setSessionCacheSize(10);
            return cachingConnectionFactory;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure Artemis ConnectionFactory", e);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setPubSubDomain(properties.isPubSubDomain());
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                     MessageConverter messageConverter) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setPubSubDomain(properties.isPubSubDomain());
        factory.setSessionTransacted(true);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageConverter jacksonJmsMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }



    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(org.springframework.boot.health.contributor.HealthIndicator.class)
    static class JmsHealthConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "artemisJmsHealthIndicator")
        public org.springframework.boot.health.contributor.HealthIndicator artemisJmsHealthIndicator(ConnectionFactory connectionFactory) {
            return new id.payu.jms.health.JmsHealthIndicator(connectionFactory);
        }
    }
}
