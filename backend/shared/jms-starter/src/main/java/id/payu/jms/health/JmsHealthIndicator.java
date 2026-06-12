package id.payu.jms.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;

/**
 * Health indicator for checking ActiveMQ Artemis JMS connection health.
 */
public class JmsHealthIndicator implements HealthIndicator {

    private final ConnectionFactory connectionFactory;

    public JmsHealthIndicator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try (Connection connection = connectionFactory.createConnection()) {
            connection.start();
            return Health.up()
                    .withDetail("provider", "ActiveMQ Artemis")
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("provider", "ActiveMQ Artemis")
                    .build();
        }
    }
}
