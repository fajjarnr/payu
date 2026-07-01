package id.payu.jms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import jakarta.jms.ConnectionFactory;
import id.payu.jms.publisher.JmsMessagePublisher;

/**
 * Auto-configuration class for JMS with ActiveMQ Artemis.
 */
@AutoConfiguration
@ConditionalOnClass({ConnectionFactory.class, ActiveMQConnectionFactory.class})
@EnableConfigurationProperties(JmsProperties.class)
@ConditionalOnProperty(prefix = "payu.jms", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JmsAutoConfiguration {

    private final JmsProperties properties;

    public JmsAutoConfiguration(JmsProperties properties) {
        this.properties = properties;
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
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    public JmsMessagePublisher jmsMessagePublisher(JmsTemplate jmsTemplate) {
        return new JmsMessagePublisher(jmsTemplate);
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
