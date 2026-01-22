package id.payu.lending.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        return new KafkaAdmin(configs);
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic loanApprovedTopic() {
        return TopicBuilder.name("loan.approved")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic loanRejectedTopic() {
        return TopicBuilder.name("loan.rejected")
                .partitions(3)
                .replicas(1)
                .build();
    }
}